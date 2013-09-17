/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.handlers.gml;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerBase;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.sparql.SparqlEndpoint.QueryType;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.UriPolicy;

import static org.datalift.fwk.MediaTypes.UTF8_ENCODED;
import static org.datalift.fwk.rdf.RdfNamespace.OGC;


/**
 * A {@link UriPolicy} implementation that handles content
 * for {@link #APPLICATION_GML_XML GML} if the requested RDF object
 * contains a <code>http://www.opengis.net/rdf#asGML</code> attribute
 * and serves this GML content.
 *
 * @author lbihanic
 */
public class GmlResourceHandler implements UriPolicy
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** "application/gml+xml" */
    public final static String APPLICATION_GML_XML = "application/gml+xml";
    /** "application/gml+xml" */
    public final static MediaType APPLICATION_GML_XML_TYPE =
                                        MediaType.valueOf(APPLICATION_GML_XML);
    /**
     * "application/vnd.ogc.gml", a deprecated alias for
     * {@link #APPLICATION_GML_XML}.
     */
    public final static String APPLICATION_VND_OGC_GML =
                                                    "application/vnd.ogc.gml";
    /**
     * "application/vnd.ogc.gml", a deprecated alias for
     * {@link #APPLICATION_GML_XML_TYPE}.
     */
    public final static MediaType APPLICATION_VND_OGC_GML_TYPE =
                                    MediaType.valueOf(APPLICATION_VND_OGC_GML);

    private final static String GML_AVAILABILITY_QUERY =
            "PREFIX ogc: <" + OGC.uri + ">\n" +
            "SELECT ?gml WHERE { { ?uri ogc:asGML ?gml . } UNION " +
                                "{ ?uri ?p ?o . ?o ogc:asGML ?gml . } } LIMIT 1";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The supported response MIME types. */
    private List<Variant> supportedResponseTypes = null;

    //-------------------------------------------------------------------------
    // UriPolicy contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override public void postInit(Configuration configuration) {
        // Gather default response types from SPARQL endpoint.
        List<Variant> defTypes =
                        configuration.getBean(SparqlEndpoint.class)
                                     .getResponseMimeTypes(QueryType.DESCRIBE);
        // Build response type selection list, inserting the GML types
        // after the RDF types (which must have priority).
        List<Variant> l = new ArrayList<Variant>(defTypes.size() + 2);
        boolean inRdfTypes = true;
        for (Variant  v : defTypes) {
            if ((inRdfTypes) && (! MediaTypes.isRdf(v.getMediaType()))) {
                l.add(new Variant(APPLICATION_GML_XML_TYPE, null, null));
                l.add(new Variant(APPLICATION_VND_OGC_GML_TYPE, null, null));
                inRdfTypes = false;
            }
            l.add(v);
        }
        this.supportedResponseTypes = Collections.unmodifiableList(l);
    }

    /** {@inheritDoc} */
    @Override
    public ResourceHandler canHandle(URI uri, UriInfo uriInfo,
                                     Request request, String acceptHdr) {
        ResourceHandler h = null;
        // 1. Check that GML is the preferred content type.
        final Variant v = request.selectVariant(this.supportedResponseTypes);
        if ((v != null) &&
            ((APPLICATION_GML_XML_TYPE.equals(v.getMediaType())) ||
             (APPLICATION_VND_OGC_GML_TYPE.equals(v.getMediaType())))) {
            // 2. Check this request resource holds a GML field.
            final String gml = this.getGmlData(uri);
            if (StringUtils.isSet(gml)) {
                h = new ResourceHandler() {
                        @Override
                        public URI resolve() {
                            return null;    // No redirect.
                        }

                        @Override
                        public Response getRepresentation() {
                            return Response.ok(gml, v.getMediaType() + UTF8_ENCODED)
                                           .build();
                        }
                    };
            }
            // Else: No GML content found. => Not for me!
        }
        // Else: GML is not the preferred content type. => Not for me!

        return h;
    }

    /** {@inheritDoc} */
    @Override public void init(Configuration configuration)     { /* NOP */ }
    /** {@inheritDoc} */
    @Override public void shutdown(Configuration configuration) { /* NOP */ }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Extracts the GML data from the specified RDF resource.
     * @param  uri   the URI of the RDF resource.
     *
     * @return the GML data as a string or <code>null</code> if no such
     *         data exist or an error occurred retrieving them.
     */
    private String getGmlData(URI uri) {
        GmlDataExtractor x = new GmlDataExtractor();
        try {
            Repository r = Configuration.getDefault().getDataRepository();
            Map<String,Object> bindings = new HashMap<String,Object>();
            bindings.put("uri", uri);
            r.select(GML_AVAILABILITY_QUERY, bindings, x);
        }
        catch (Exception e) { /* Ignore... */ }

        return x.gml;
    }

    //-------------------------------------------------------------------------
    // GmlDataExtractor nested class
    //-------------------------------------------------------------------------

    /**
     * A {@link TupleQueryResultHandler} that extracts the GML data
     * associated to a RDF resource.
     */
    private final static class GmlDataExtractor
                                            extends TupleQueryResultHandlerBase
    {
        public String gml = null;

        @Override
        public void handleSolution(BindingSet bindingSet) {
            Value v = bindingSet.getValue("gml");
            if (v != null) {
                gml = v.stringValue();
            }
        }
    }
}

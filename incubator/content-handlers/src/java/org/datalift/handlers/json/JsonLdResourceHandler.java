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

package org.datalift.handlers.json;


import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;

import de.dfki.km.json.jsonld.impl.SesameJSONLDSerializer;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.rdf.RdfException;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.sparql.SparqlEndpoint.QueryType;
import org.datalift.fwk.util.UriPolicy;


public class JsonLdResourceHandler implements UriPolicy
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** "application/ld+json" */
    public final static String APPLICATION_JSON_LD = "application/ld+json";
    /** "application/ld+json" */
    public final static MediaType APPLICATION_JSON_LD_TYPE =
                                        MediaType.valueOf(APPLICATION_JSON_LD);

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
        // Build response type selection list, inserting the JSON-LD type
        // after the RDF types (which must have priority).
        List<Variant> l = new ArrayList<Variant>(defTypes.size() + 1);
        boolean inRdfTypes = true;
        for (Variant  v : defTypes) {
            if ((inRdfTypes) && (! MediaTypes.isRdf(v.getMediaType()))) {
                l.add(new Variant(APPLICATION_JSON_LD_TYPE, null, null));
                inRdfTypes = false;
            }
            l.add(v);
        }
        this.supportedResponseTypes = Collections.unmodifiableList(l);
    }

    /** {@inheritDoc} */
    @Override
    public ResourceHandler canHandle(final URI uri, UriInfo uriInfo,
                                     Request request, String acceptHdr) {
        ResourceHandler h = null;
        // Check that JSON-LD is the preferred content type.
        final Variant v = request.selectVariant(this.supportedResponseTypes);
        if ((v != null) && (APPLICATION_JSON_LD_TYPE.equals(v.getMediaType()))) {
            h = new ResourceHandler() {
                    @Override
                    public URI resolve() {
                        return null;    // No redirect.
                    }

                    @Override
                    public Response getRepresentation() {
                        StreamingOutput out = new JsonLdSerializer(uri,
                                            Configuration.getDefault()
                                                         .getDataRepository());
                        return Response.ok(out, APPLICATION_JSON_LD
                                            + MediaTypes.UTF8_ENCODED).build();
                    }
                };
        }
        // Else: Not for me!

        return h;
    }

    /** {@inheritDoc} */
    @Override public void init(Configuration configuration)     { /* NOP */ }
    /** {@inheritDoc} */
    @Override public void shutdown(Configuration configuration) { /* NOP */ }


    //-------------------------------------------------------------------------
    // JsonLdSerializer nested class
    //-------------------------------------------------------------------------

    private final static class JsonLdSerializer extends SesameJSONLDSerializer
                                                implements StreamingOutput
    {
        private final URI uri;
        private final Repository repository;
        private final Map<String,RdfNamespace> namespaces =
                                            new HashMap<String,RdfNamespace>();

        public JsonLdSerializer(URI uri, Repository repository) {
            super();
            this.uri = uri;
            this.repository = repository;
            for (RdfNamespace ns : RdfNamespace.values()) {
                this.namespaces.put(ns.uri, ns);
            }
        }

        @Override
        public void write(OutputStream output) throws IOException,
                                                      WebApplicationException {
            try {
                this.repository.construct("DESCRIBE <"+ this.uri + '>', this);
                this.writeTo(output, false);
                output.flush();
            }
            catch (RdfException e) {
                throw new IOException(e);
            }
        }

        @Override
        public void handleStatement(Statement statement) {
            this.registerPrefix(statement.getSubject());
            this.registerPrefix(statement.getPredicate());
            this.registerPrefix(statement.getObject());

            super.handleStatement(statement);
        }

        private void registerPrefix(Value v) {
            if (v instanceof org.openrdf.model.URI) {
                this.registerPrefix((org.openrdf.model.URI)v);
            }
        }

        private void registerPrefix(org.openrdf.model.URI uri) {
            if (uri != null) {
                RdfNamespace ns = this.namespaces.get(uri.getNamespace());
                if (ns != null) {
                    this.setPrefix(ns.uri, ns.prefix);
                }
            }
        }
    }
}

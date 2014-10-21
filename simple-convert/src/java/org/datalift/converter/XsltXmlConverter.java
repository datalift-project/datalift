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

package org.datalift.converter;


import java.io.InputStream;
import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.rdfxml.RDFXMLParser;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.XmlSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.BatchStatementAppender;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.web.UriParam;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A {@link ProjectModule project module} that performs XML to RDF
 * conversion by applying a generic XSL transformation stylesheet.
 * <p>
 * The default XSLT stylesheet is the
 * <a href="http://www.gac-grid.de/project-products/Software/XML2RDF.html">XML2RDF
 * stylesheet</a> provided by the AstroGrid-D project of the German
 * Astronomy Community Grid (GACG).</p>
 *
 * @author lbihanic
 */
@Path(XsltXmlConverter.MODULE_NAME)
public class XsltXmlConverter extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "xsltxmlmapper";

    private final static String DEFAULT_STYLESHEET =
                                        "stylesheets/xml2rdf3-noOrdering.xsl";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private static TransformerFactory transformerFactory;
    private static Templates defaultTemplates;

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public XsltXmlConverter() {
        super(MODULE_NAME, 800, SourceType.XmlSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam(PROJECT_ID_PARAM) URI projectId) {
        return this.newProjectView("xsltXmlMapper.vm", projectId);
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response mapXmlData(
                        @FormParam(PROJECT_ID_PARAM) UriParam projectId,
                        @FormParam(SOURCE_ID_PARAM)  UriParam sourceId,
                        @FormParam(TARGET_SRC_NAME)  String destTitle,
                        @FormParam(GRAPH_URI_PARAM)  UriParam targetGraphParam,
                        @FormParam(BASE_URI_PARAM)   UriParam baseUriParam)
                                                throws WebApplicationException {
        if (projectId == null) {
            this.throwInvalidParamError(PROJECT_ID_PARAM, null);
        }
        if (sourceId == null) {
            this.throwInvalidParamError(SOURCE_ID_PARAM, null);
        }
        if (targetGraphParam == null) {
            this.throwInvalidParamError(GRAPH_URI_PARAM, null);
        }
        if (baseUriParam == null) {
            this.throwInvalidParamError(BASE_URI_PARAM, null);
        }
        Response response = null;

        try {
            // Retrieve project.
            Project p = this.getProject(projectId.toUri(PROJECT_ID_PARAM));
            // Load input source.
            XmlSource in = (XmlSource)
                            (p.getSource(sourceId.toUri(SOURCE_ID_PARAM)));
            if (in == null) {
                throw new ObjectNotFoundException("project.source.not.found",
                                                  projectId, sourceId);
            }
            // Extract target named graph. It shall NOT conflict with
            // existing objects (sources, projects) otherwise it would not
            // be accessible afterwards (e.g. display, removal...).
            URI targetGraph = targetGraphParam.toUri(GRAPH_URI_PARAM);
            this.checkUriConflict(targetGraph, GRAPH_URI_PARAM);
            // Convert XML data to RDF and load generated triples.
            URI baseUri = UriParam.valueOf(baseUriParam, BASE_URI_PARAM);
            log.debug("Mapping XML data from \"{}\" into graph \"{}\"",
                                                        sourceId, targetGraph);
            this.convert(in, Configuration.getDefault().getInternalRepository(),
                             targetGraph, baseUri);
            // Register new transformed RDF source.
            Source out = this.addResultSource(p, in, destTitle, targetGraph);
            // Display project source tab, including the newly created source.
            response = this.created(out).build();

            log.info("XML data from \"{}\" successfully mapped to \"{}\"",
                                                        sourceId, targetGraph);
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private void convert(XmlSource src, Repository target,
                                        URI targetGraph, URI baseUri) {
        if (baseUri == null) {
            baseUri = targetGraph;
        }
        org.openrdf.model.URI ctx = null;
        RepositoryConnection cnx  = target.newConnection();
        try {
            // Clear target named graph, if any.
            if (targetGraph != null) {
                ctx = cnx.getValueFactory().createURI(targetGraph.toString());
            }
            // Compute 2 base URIs: one without ending separator, for the
            // XSLT stylesheet to append ad-hoc separator ('/' for elements,
            // '#' for attributes) and another for the RDF parser.
            String xmlBaseUri = this.getBaseUri(baseUri);
            String rdfBaseUri = RdfUtils.getBaseUri(
                            (baseUri != null)? baseUri.toString(): null, '/');
            // Create XML RDF parser and content handler, to insert triples
            // in RDF store in stream mode, without intermediate file storage.
            BatchStatementAppender appender =
                                new BatchStatementAppender(cnx, ctx);
            RDFXMLParser rdfParser = (RDFXMLParser)
                                (RdfUtils.newRdfParser(RdfFormat.RDF_XML));
            rdfParser.setRDFHandler(appender);
            // Input XML document.
            StreamSource xmlSrc = new StreamSource(src.getInputStream());
            if (! isBlank(src.getSourceUrl())) {
                // Set the source URL to resolve DTDs, if any.
                xmlSrc.setSystemId(src.getSourceUrl());
            }
            // Apply XSL transformation to build RDF XML from XML data.
            Transformer t = this.newTransformer(null);
            t.setParameter("BaseURI", xmlBaseUri);
            t.transform(xmlSrc, rdfParser.getSAXResult(rdfBaseUri));

            log.debug("Inserted {} RDF triples into <{}> in {} seconds",
                      wrap(appender.getStatementCount()), targetGraph,
                      wrap(appender.getDuration() / 1000.0));
        }
        catch (Exception e) {
            throw new TechnicalException("xml.conversion.failed", e);
        }
        finally {
            Repository.closeQuietly(cnx);
        }
    }

    /**
     * Returns a {@link Transformer} object to apply the specified XSLT
     * stylesheet.
     * @param  xslt   the XSLT stylesheet to use for transforming
     *                source XML data into RDF or <code>null</code> to
     *                use the default stylesheet.
     * @return a new Transformer object.
     */
    private Transformer newTransformer(InputStream xslt) {
        if (transformerFactory == null) {
            // set thread content classloader to ensure proper resource
            // (JAR service provider configuration file and stylesheets)
            // resolution.
            ClassLoader ctx = Thread.currentThread().getContextClassLoader();
            Thread.currentThread().setContextClassLoader(
                                            this.getClass().getClassLoader());
            // Load embedded XLST transformation engine implementation.
            transformerFactory = TransformerFactory.newInstance();
            // Restore application server classloader.
            Thread.currentThread().setContextClassLoader(ctx);
        }
        Transformer t = null;
        try {
            if (xslt == null) {
                // Use default XML to RDF transformation stylesheet.
                if (defaultTemplates == null) {
                    defaultTemplates = transformerFactory.newTemplates(
                            new StreamSource(
                                this.getClass().getClassLoader()
                                    .getResourceAsStream(DEFAULT_STYLESHEET)));
                }
                t = defaultTemplates.newTransformer();
            }
            else {
                t = transformerFactory.newTransformer(new StreamSource(xslt));
            }
        }
        catch (Exception e) {
            throw new TechnicalException("xml.stylesheet.parse.failed", e);
        }
        return t;
    }

    private String getBaseUri(URI uri) {
        String baseUri = "";
        if (uri != null) {
            baseUri = uri.toString();
            int n = baseUri.length() - 1;
            char c = baseUri.charAt(n);
            if ((c == '/') || (c == '#')) {
                baseUri = baseUri.substring(0, n);
            }
        }
        return baseUri;
    }
}

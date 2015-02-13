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


import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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
import org.datalift.fwk.rdf.rio.rdfxml.RDFXMLParser;
import org.datalift.fwk.util.web.UriParam;
import org.datalift.fwk.view.TemplateModel;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.util.TimeUtils.asSeconds;


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
    public enum DefaultStylesheet
    {
        DOCUMENT        ("xml.xslt.whole.document",
                         "stylesheets/xml2rdf-WholeDocument-noOrdering.xsl"),
        ROOT_ELEMENT    ("xml.xslt.root.element",
                         "stylesheets/xml2rdf-RootElement-noOrdering.xsl"),
        FIRST_LEVEL_CHILDREN("xml.xslt.first.level.children",
                         "stylesheets/xml2rdf-ChildElements-noOrdering.xsl");

        public final String label;
        private final String path;

        DefaultStylesheet(String label, String path) {
            this.label = label;
            this.path = path;
        }

        public URL getUrl() {
            return this.getClass().getClassLoader().getResource(this.path);
        }

        @Override
        public String toString() {
            return this.name() + '(' + this.path + ')';
        }

        /**
         * Return the URL of the stylesheet associated to the
         * enumeration value corresponding to the specified label.
         * @param  s   the enumeration value label.
         *
         * @return the stylesheet URL or <code>null</code> if the
         *         specified label was not recognized.
         */
        public static URL fromString(String s) {
            DefaultStylesheet def = null;
            if (isSet(s)) {
                for (DefaultStylesheet e : values()) {
                    if (e.name().equalsIgnoreCase(s)) {
                        def = e;
                        break;
                    }
                }
            }
            else {
                def = values()[0];
            }
            return (def != null)? def.getUrl(): null;
        }
    }

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "xsltxmlmapper";

    /* Web service parameter names. */
    private final static String STYLESHEET_PARAM = "stylesheet";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private static TransformerFactory transformerFactory;
    private static Map<String,Templates> cachedTemplates =
                                    new ConcurrentHashMap<String,Templates>();

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

    /**
     * <i>[Resource method]</i> Displays the module welcome page.
     * @param  projectId   the URI of the data-lifting project.
     *
     * @return a JAX-RS response with the page template and parameters.
     */
    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam(PROJECT_ID_PARAM) URI projectId) {
        TemplateModel view = this.getProjectView("xsltXmlMapper.vm", projectId);
        view.put("stylesheets", this.getStylesheets(projectId));
        return Response.ok(view, TEXT_HTML_UTF8).build();
    }

    /**
     * <i>[Resource method]</i> Converts the data of the specified XML
     * source into RDF triples by applying a predefined XSLT stylesheet,
     * loads them in the internal store and creates a new associated
     * RDF source.
     * @param  projectId          the URI of the data-lifting project.
     * @param  sourceId           the URI of the source to convert.
     * @param  destTitle          the name of the RDF source to hold the
     *                            converted data.
     * @param  targetGraphParam   the URI of the named graph to hold the
     *                            converted data, which will also be the
     *                            URI of the created RDF source.
     * @param  baseUriParam       the base URI to build the RDF
     *                            identifiers from the XML data.
     *
     * @return a JAX-RS response redirecting the user browser to the
     *         created RDF source.
     * @throws WebApplicationException if any error occurred during the
     *         data conversion from SQL to RDF.
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response mapXmlData(
                        @FormParam(PROJECT_ID_PARAM) UriParam projectId,
                        @FormParam(SOURCE_ID_PARAM)  UriParam sourceId,
                        @FormParam(TARGET_SRC_NAME)  String destTitle,
                        @FormParam(GRAPH_URI_PARAM)  UriParam targetGraphParam,
                        @FormParam(BASE_URI_PARAM)   UriParam baseUriParam,
                        @FormParam(STYLESHEET_PARAM) String stylesheet)
                                                throws WebApplicationException {
        if (! UriParam.isSet(projectId)) {
            this.throwInvalidParamError(PROJECT_ID_PARAM, null);
        }
        if (! UriParam.isSet(sourceId)) {
            this.throwInvalidParamError(SOURCE_ID_PARAM, null);
        }
        if (! UriParam.isSet(targetGraphParam)) {
            this.throwInvalidParamError(GRAPH_URI_PARAM, null);
        }
        if (! UriParam.isSet(baseUriParam)) {
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
            // Resolve stylesheet.
            StreamSource xslt = null;
            URL u = DefaultStylesheet.fromString(stylesheet);
            if (u != null) {
                // Default stylesheet matched.
                xslt = new StreamSource(u.openStream(), u.toString());
            }
            else {
                // Not a default stylesheet. => Assume project file source.
                XmlSource xslSrc = (XmlSource)(p.getSource(stylesheet));
                if (xslSrc != null) {
                    xslt = new StreamSource(xslSrc.getInputStream());
                }
            }
            if (xslt == null) {
                this.throwInvalidParamError(STYLESHEET_PARAM, stylesheet);
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
                             targetGraph, baseUri, xslt);
            // Register new transformed RDF source.
            Source out = this.addResultSource(p, in, destTitle,
                                                     targetGraph, baseUri);
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

    private Collection<StylesheetDesc> getStylesheets(URI projectId) {
        Collection<StylesheetDesc> l =
                        new LinkedList<XsltXmlConverter.StylesheetDesc>();
        // Add default stylesheets.
        for (DefaultStylesheet def : DefaultStylesheet.values()) {
            l.add(new StylesheetDesc(def));
        }
        // Add project XML sources associated to a .xsl[t] file.
        try {
            Project p = this.getProject(projectId);
            for (Source s : p.getSources()) {
                if (s instanceof XmlSource) {
                    String path = ((XmlSource)s).getFilePath();
                    if (path.toLowerCase().lastIndexOf(".xsl") != -1) {
                        l.add(new StylesheetDesc(s.getUri(), path));
                    }
                }
            }
        }
        catch (Exception e) {
            log.warn("Failed to list project XSLT stylesheets.", e);
        }
        return l;
    }

    private void convert(XmlSource src, Repository target, URI targetGraph,
                                        URI baseUri, StreamSource stylesheet) {
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
            Transformer t = this.newTransformer(stylesheet);
            t.setParameter("BaseURI", xmlBaseUri);
            t.transform(xmlSrc, rdfParser.getSAXResult(rdfBaseUri));

            log.debug("Inserted {} RDF triples into <{}> in {} seconds",
                      wrap(appender.getStatementCount()), targetGraph,
                      wrap(asSeconds(appender.getDuration())));
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
    private Transformer newTransformer(StreamSource xslt) {
        if (xslt == null) {
            throw new IllegalArgumentException("xslt");
        }
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
            String path = xslt.getSystemId();
            if (isSet(path)) {
                Templates templates = cachedTemplates.get(path);
                if (templates == null) {
                    templates = transformerFactory.newTemplates(xslt);
                    cachedTemplates.put(path, templates);
                }
                t = templates.newTransformer();
            }
            else {
                t = transformerFactory.newTransformer(xslt);
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


    public final static class StylesheetDesc
    {
        private final String id;
        private final String label;

        public StylesheetDesc(DefaultStylesheet def) {
            this.id    = def.name();
            this.label = def.label;
        }

        public StylesheetDesc(String id, String path) {
            this.id = id;
            int n = path.lastIndexOf('/');
            this.label = (n != -1)? path.substring(n + 1): path;
        }

        public String getId() {
            return this.id;
        }

        public String getLabel() {
            return this.label;
        }

        @Override
        public String toString() {
            return this.id;
        }
    }
}

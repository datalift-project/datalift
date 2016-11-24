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

package org.datalift.skos;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeSet;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.Response.Status.*;

import org.openrdf.model.Value;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerBase;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfException;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.rdf.ElementType.Resource;
import static org.datalift.fwk.rdf.RdfFormat.VARIANTS;
import static org.datalift.fwk.rdf.RdfNamespace.SKOS;
import static org.datalift.fwk.sparql.SparqlEndpoint.QueryType.DESCRIBE;


/**
 * A Datalift module that provides HTML rendering of SKOS Concepts and
 * Concept Schemes.
 *
 * @author Fatima Aoullag
 * @author lbihanic
 */
@Path(SkosViewer.MODULE_NAME + '/')
public class SkosViewer extends BaseModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The module name. */
    public static final String MODULE_NAME = "skos";

    // URIs of SKOS Concept and ConceptScheme RDF types.
    private final static String CONCEPT_TYPE            = "Concept";
    private final static String CONCEPT_SCHEME_TYPE     = "ConceptScheme";
    private final static String CONCEPT_URI     = SKOS.uri + CONCEPT_TYPE;

    private final static String MAPPER_KEY      = "uriMapper";
    private final static long   SCHEME_UPD_FREQ = 30 * 60 * 1000L; // 30 min.
    private final static long   FIRST_UPD_DELAY = 3 * 1000L;       // 3 sec.

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    // private long lastSchemeRootsUpdate = 0L;
    private Set<String> knownSchemeRoots = new TreeSet<>();
    private Timer schemeUpdateTimer;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new ThesaurusW module instance.
     */
    public SkosViewer() {
        super(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * Returns the index page for the module ThesaurusW.
     * @param  uriInfo   the requested URI data (injected).
     *
     * @return the index page.
     */
    @GET
    @Produces({TEXT_HTML, APPLICATION_XHTML_XML, APPLICATION_XML, TEXT_XML })
    public Response getIndex(@Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            TemplateModel m = this.newViewable("index.vm", null);
            m.put(MAPPER_KEY, new UriMapper(uriInfo));
            response = Response.ok(m).build();
        }
        catch (Exception e) {
            this.handleError(e);
        }
        return response;
    }

    /**
     * Resolves the resource representation using contents negotiation:
     * "data" for RDF representation, "page" for HTML representation
     * (it can be expanded to more resource representation).
     * @param  id        the id of the resource being accessed.
     * @param  uriInfo   the requested URI data (injected).
     * @param  request   the JAX-RS request object (injected).
     *
     * @return the resource representation Velocity page
     */
    @GET
    @Path("{id: .+$}")
    public Response getResource(@PathParam("id") String id,
                                @QueryParam("rdf") boolean asRdf,
                                @Context UriInfo uriInfo,
                                @Context Request request,
                                @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Check the requested path maps to an existing SKOS resource.
            String rscPath = uriInfo.getPath()
                                    .replaceFirst(MODULE_NAME + '/', "");
            Map<String,Value> rscDesc = this.resolveResource(rscPath);
            if (rscDesc == null) {
                throw new WebApplicationException(NOT_FOUND);
            }
            Value uri  = rscDesc.get("uri");
            String type = rscDesc.get("type").stringValue();
            // Perform content negotiation.
            SparqlEndpoint sparql = Configuration.getDefault()
                                                 .getBean(SparqlEndpoint.class);
            List<Variant> mimesTypes = sparql.getResponseMimeTypes(DESCRIBE);
            Variant v = request.selectVariant(mimesTypes);
            if (v == null) {
                // No matching MIME types.
                response = Response.notAcceptable(mimesTypes).build();
            }
            else {
                MediaType contentType = v.getMediaType();
                if ((! asRdf) &&
                    (TEXT_HTML_TYPE.equals(contentType) ||
                     APPLICATION_XHTML_XML_TYPE.equals(contentType))) {
                    // Select page template according to RDF type.
                    String page = (type.equals(CONCEPT_URI))?
                                            "concept.vm": "conceptScheme.vm";
                    TemplateModel m = this.newViewable(page, uri);
                    m.put(MAPPER_KEY, new UriMapper(uriInfo, uri));
                    response = Response.ok(m).build();
                }
                else {
                    // Raw RDF data requested.
                    // => Delegate request handling to SPARQL endpoint.
                    response = sparql.describe(uri.stringValue(),
                                        Resource, uriInfo,
                                        request, acceptHdr, VARIANTS).build();
                }
            }
        }
        catch (Exception e) {
            this.handleError(e);
        }
        return response;
    }

    /**
     * Traps accesses to module static resources and redirect them
     * toward the default {@link ResourceResolver} for resolution.
     * @param  path        the relative path of the module static
     *                     resource being accessed.
     * @param  uriInfo     the requested URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response JAX-RS response} to download the
     *         content of the specified public resource.
     */
    @GET
    @Path("{path: .+\\.(css|js|jpg|png|html)$}")
    public Object getStaticResource(@PathParam("path") String path,
                                    @Context UriInfo uriInfo,
                                    @Context Request request,
                                    @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        ResourceResolver r = Configuration.getDefault()
                                          .getBean(ResourceResolver.class);
        return r.resolveModuleResource(this.getName(),
                                       uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);
        // Start timer to keep known schemes list up-to-date.
        // Note: In case Datalift and Sesame are in the same app. server,
        //       we need to wait for a while (e.g. 3 sec.) for Sesame to
        //       complete start-up prior making the first SPARQL query.
        this.schemeUpdateTimer = new Timer(true);
        this.schemeUpdateTimer.schedule(
                new TimerTask() {
                        @Override
                        public void run() {
                            try {
                                updateKnownSchemes();
                            }
                            catch (Exception e) { /* Ignore... */ }
                        }
                }, FIRST_UPD_DELAY, SCHEME_UPD_FREQ);
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown(Configuration configuration) {
        this.schemeUpdateTimer.cancel();
        super.shutdown(configuration);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Return a viewable for the specified template, populated with the
     * specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     * @return a populated viewable.
     */
    private TemplateModel newViewable(String templateName, Object it) {
        return ViewFactory.newView(
                                "/" + this.getName() + '/' + templateName, it);
    }

    /**
     * Resolve the specified RDF resource ending path into URI.
     * @param  path   the resource path.
     * @throws RdfException if an error occurred while querying the
     *         RDF store.
     */
    private Map<String,Value> resolveResource(String path)
                                                        throws RdfException {
        Map<String,Object> bindings = new HashMap<String, Object>();
        bindings.put("path", path);
        final String query =
                    "PREFIX skos: <" + SKOS.uri + ">\n" +
                    "SELECT ?uri ?type WHERE {\n" +
                        "?uri a ?type .\n" +
                        "FILTER(?type = skos:" + CONCEPT_TYPE + " || " +
                               "?type = skos:" + CONCEPT_SCHEME_TYPE + ")\n" +
                        "FILTER(STRENDS(STR(?uri), ?path))\n" +
                    "} LIMIT 1";
        final Map<String,Value> results = new HashMap<>();
        TupleQueryResultHandlerBase handler =
                new TupleQueryResultHandlerBase() {
                    @Override
                    public void handleSolution(BindingSet bindingSet) {
                        for (String s : bindingSet.getBindingNames()) {
                            results.put(s, bindingSet.getValue(s));
                        }
                    }
                };
        Configuration.getDefault().getDefaultRepository()
                                  .select(query, bindings, handler);
        return (results.isEmpty())? null: results;
    }

    private void updateKnownSchemes() {
        try {
            final String query =
                    "PREFIX skos: <" + SKOS.uri + ">\n" +
                    "SELECT DISTINCT ?s WHERE { ?s a skos:ConceptScheme . }";
            final Set<String> schemeRoots = new TreeSet<>();
            TupleQueryResultHandlerBase handler =
                    new TupleQueryResultHandlerBase() {
                        @Override
                        public void handleSolution(BindingSet bs) {
                            String scheme = bs.getValue("s").stringValue();
                            schemeRoots.add(getSchemeRootUri(scheme));
                        }
                    };
            Configuration.getDefault().getDefaultRepository()
                                      .select(query, handler);
            this.knownSchemeRoots = schemeRoots;
            // this.lastSchemeRootsUpdate = System.currentTimeMillis();
            log.info("Published SKOS concept schemes list updated.");
        }
        catch (RdfException e) {
            log.error("Published SKOS concept schemes listing failed.", e);
            throw new RuntimeException(e);
        }
    }

    private String getSchemeRootUri(String schemeUri) {
        return URI.create(schemeUri).resolve("..").toString();
    }
    /**
     * Generic error handler.
     * @param  e   the caught exception.
     */
    private void handleError(Exception e) throws WebApplicationException {
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        else {
            log.error("Request processing failed: \"{}\"",
                                                e, e.getLocalizedMessage());
            this.sendError(INTERNAL_SERVER_ERROR, e.getLocalizedMessage());
        }
    }

    public final class UriMapper
    {
        private final String baseUri;
        private final String refUri;

        private UriMapper(UriInfo uriInfo) {
            this(uriInfo, (String)null);
        }

        private UriMapper(UriInfo uriInfo, Value refUri) {
            this(uriInfo, refUri.stringValue());
        }

        private UriMapper(UriInfo uriInfo, String refUri) {
            this.baseUri = uriInfo.getBaseUri().toString() + MODULE_NAME;
            String ref = null;
            if (refUri != null) {
                ref = getSchemeRootUri(refUri);
            }
            this.refUri = ref;
        }

        public String toUrl(Value v) {
            return this.toUrl(v.stringValue());
        }

        public String toUrl(String uri) {
            // Map all URIs related to the current concept scheme or any
            // locally-published concept schemes.
            if (((this.refUri != null) && (uri.startsWith(this.refUri))) ||
                (this.canMap(uri))) {
                try {
                    URI u = URI.create(uri);
                    uri = this.baseUri + u.getPath();
                    if (u.getQuery() != null) {
                        uri += "?" + u.getQuery();
                    }
                }
                catch (Exception e) { /* Ignore and do not map. */ }
            }
            return uri;
        }

        private boolean canMap(String uri) {
            boolean canMap = false;
            for (String s : knownSchemeRoots) {
                if (uri.startsWith(s)) {
                    canMap = true;
                    break;
                }
            }
            return canMap;
        }
    }
}

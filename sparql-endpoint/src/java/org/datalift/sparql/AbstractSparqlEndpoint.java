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

package org.datalift.sparql;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.sun.jersey.api.view.Viewable;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;


abstract public class AbstractSparqlEndpoint extends BaseModule
                                             implements SparqlEndpoint
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static Pattern QUERY_START_PATTERN = Pattern.compile(
                    "SELECT|CONSTRUCT|ASK|DESCRIBE", Pattern.CASE_INSENSITIVE);
    private final static int MAX_QUERY_DESC = 128;

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    protected final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift configuration. */
    protected Configuration configuration = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    protected AbstractSparqlEndpoint() {
        super(MODULE_NAME, true);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration cfg) {
        super.init(cfg);
        this.configuration = cfg;
    }

    //-------------------------------------------------------------------------
    // SparqlEndpoint contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(String query, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                                throws WebApplicationException {
        return this.executeQuery(null, null, query, -1, -1, false,
                                 uriInfo, request, acceptHdr);
    }
    
    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                                        List<String> namedGraphUris,
                                        String query, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                                throws WebApplicationException {
        return this.executeQuery(defaultGraphUris, namedGraphUris, query,
                                 -1, -1, false, uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            int startOffset, int endOffset, boolean gridJson,
                            UriInfo uriInfo, Request request, String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            response = this.doExecute(defaultGraphUris, namedGraphUris, query,
                                      startOffset, endOffset, gridJson,
                                      uriInfo, request, acceptHdr);
        }
        catch (Exception e) {
            this.handleError(query, e);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Process a SPARQL query sent as an HTTP
     * GET request.
     * @param  defaultGraphUris   the <code>default-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  namedGraphUris     the <code>named-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  query              the <code>query</code>
     *                            parameter of the SPARQL query.
     * @param  uriInfo            the request URI data.
     * @param  request            the JAX-RS Request object, for content
     *                            negotiation.
     * @param  acceptHdr          the HTTP Accept header, for content
     *                            negotiation.
     *
     * @return the SPARQL query result, formatted according to the
     *         negotiated content type.
     * @throws WebApplicationException if the SPARQL request is invalid
     *         or a processing error occurred or the user is not allowed
     *         to execute the specified query.
     */
    @GET
    public Response getQuery(
                @QueryParam("default-graph-uri") List<String> defaultGraphUris,
                @QueryParam("named-graph-uri") List<String> namedGraphUris,
                @QueryParam("query") String query,
                @QueryParam("min") @DefaultValue("-1") int startOffset,
                @QueryParam("max") @DefaultValue("-1") int endOffset,
                @QueryParam("grid") @DefaultValue("false") boolean gridJson,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, gridJson,
                                  uriInfo, request, acceptHdr);
    }
    
    /**
     * <i>[Resource method]</i> Process a SPARQL query sent as an HTTP
     * POST request.
     * @param  defaultGraphUris   the <code>default-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  namedGraphUris     the <code>named-graph-uri</code>
     *                            parameter of the SPARQL query.
     * @param  query              the <code>query</code>
     *                            parameter of the SPARQL query.
     * @param  uriInfo            the request URI data.
     * @param  request            the JAX-RS Request object, for content
     *                            negotiation.
     * @param  acceptHdr          the HTTP Accept header, for content
     *                            negotiation.
     *
     * @return the SPARQL query result, formatted according to the
     *         negotiated content type.
     * @throws WebApplicationException if the SPARQL request is invalid
     *         or a processing error occurred or the user is not allowed
     *         to execute the specified query.
     */
    @POST
    public final Response postQuery(
                @QueryParam("default-graph-uri") List<String> defaultGraphUris,
                @QueryParam("named-graph-uri") List<String> namedGraphUris,
                @QueryParam("query") String query,
                @QueryParam("min") @DefaultValue("-1") int startOffset,
                @QueryParam("max") @DefaultValue("-1") int endOffset,
                @QueryParam("grid") @DefaultValue("false") boolean gridJson,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  startOffset, endOffset, gridJson,
                                  uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private final Response dispatchQuery(List<String> defaultGraphUris,
                                List<String> namedGraphUris, String query,
                                int startOffset, int endOffset,
                                boolean gridJson, UriInfo uriInfo,
                                Request request, String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;

        // Check for empty query and render HTML query input form.
        if ((query == null) || (query.trim().length() == 0)) {
            response = Response.ok(this.newViewable("/sparqlEndpoint.vm",
                                        this.configuration.getRepositories()),
                                   MediaType.TEXT_HTML);
            return response.build();
        }
        else {
            try {
                response = this.executeQuery(defaultGraphUris, namedGraphUris,
                                        query, startOffset, endOffset, gridJson,
                                        uriInfo, request, acceptHdr);
            }
            catch (Exception e) {
                this.handleError(query, e);
            }
        }
        return response.build();
    }
    
    abstract protected ResponseBuilder doExecute(
                                          List<String> defaultGraphUris,
                                          List<String> namedGraphUris,
                                          String query,
                                          int startOffset, int endOffset,
                                          boolean gridJson, UriInfo uriInfo,
                                          Request request, String acceptHdr)
                                                            throws Exception;

    protected final Repository getTargetRepository(
                                            List<String> defaultGraphUris)
                                                    throws SecurityException {
        String targetRepo = null;
        if ((defaultGraphUris != null) && (! defaultGraphUris.isEmpty())) {
            targetRepo = defaultGraphUris.remove(0);
        }
        Repository repo = this.configuration.getRepository(targetRepo);
        if (repo == null) {
            // No repository found for first default graph.
            // => Use default repository: public data.
            defaultGraphUris.add(0, targetRepo);
            repo = this.configuration.getDataRepository();
        }
        else {
            if ((repo != this.configuration.getDefaultRepository()) &&
                (! SecurityContext.isUserAuthenticated())) {
                // Unauthenticated user are not allowed to access
                // any repository but the public one.
                throw new java.lang.SecurityException();
            }
        }
        return repo;
    }

    protected final Variant getResponseType(Request request, String accept,
                                            List<Variant> supportedTypes)
                                                throws WebApplicationException {
        Variant responseType = request.selectVariant(supportedTypes);
        if (responseType == null) {
            // No matching type found.
            String msg = new StringBuilder()
                .append("No matching content type found: requested types: [")
                .append(accept)
                .append("], Supported content types for query type: [")
                .append(StringUtils.join(supportedTypes, ", "))
                .append("]").toString();
            log.error(msg);

            throw new WebApplicationException(
                                Response.status(Status.NOT_ACCEPTABLE)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(msg).build());
        }
        return responseType;
    }

    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }

    protected final void handleError(String query,
                                     String message, Status status)
                                                throws WebApplicationException {
        if (status == null) {
            status = Status.INTERNAL_SERVER_ERROR;
        }
        log.error("Query processing failed: \"{}\" for: \"{}\"", message, query);
        throw new WebApplicationException(
                                Response.status(status)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(message).build());
    }

    protected final void handleError(String query, Exception e)
                                                throws WebApplicationException {
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        else if (e instanceof SecurityException) {
            throw new WebApplicationException(Status.FORBIDDEN);
        }
        else if (e.getCause() instanceof QueryDoneException) {
            // End of requested range (start/end offset) successfully reached.
        }
        else {
            log.error("Query processing failed: \"{}\" for: \"{}\"",
                                                    e, e.getMessage(), query);
            throw new WebApplicationException(e,
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(e.getMessage()).build());
        }
    }

    protected final static String getQueryDesc(String query) {
        String desc = query;
        if (query != null) {
            // Strip prefix declarations.
            Matcher m = QUERY_START_PATTERN.matcher(query);
            if (m.find()) {
                int i = m.start();
                // Get the 100 first chars of the query string, minus prefixes.
                desc = (query.length() - i > MAX_QUERY_DESC)?
                                query.substring(i, MAX_QUERY_DESC + i) + "...":
                                query.substring(i);
            }
        }
        return desc;
    }

    protected final static class QueryDescription
    {
        public final String query;
        private String desc = null;

        public QueryDescription(String query) {
            this.query = query;
        }

        @Override
        public String toString() {
            if (this.desc == null) {
                this.desc = getQueryDesc(this.query);
            }
            return this.desc;
        }
    }
}

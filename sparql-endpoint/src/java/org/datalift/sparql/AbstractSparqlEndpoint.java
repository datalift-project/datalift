package org.datalift.sparql;


import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.datalift.fwk.sparql.SparqlEndpoint;


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
        return this.executeQuery(null, null, query,
                                             uriInfo, request, acceptHdr);
    }

    /** {@inheritDoc} */
    @Override
    public ResponseBuilder executeQuery(List<String> defaultGraphUris,
                                        List<String> namedGraphUris,
                                        String query, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            response = this.doExecute(defaultGraphUris, namedGraphUris,
                                      query, uriInfo, request, acceptHdr);
        }
        catch (Exception e) {
            this.handleError(query, e);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // JAX-RS root resource contract support
    //-------------------------------------------------------------------------

    @GET
    public Response getQuery(
                @QueryParam("default-graph-uri") List<String> defaultGraphUris,
                @QueryParam("named-graph-uri") List<String> namedGraphUris,
                @QueryParam("query") String query,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  uriInfo, request, acceptHdr);
    }

    @POST
    public final Response postQuery(
                @QueryParam("default-graph-uri") List<String> defaultGraphUris,
                @QueryParam("named-graph-uri") List<String> namedGraphUris,
                @QueryParam("query") String query,
                @Context UriInfo uriInfo,
                @Context Request request,
                @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        return this.dispatchQuery(defaultGraphUris, namedGraphUris, query,
                                  uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private final Response dispatchQuery(List<String> defaultGraphUris,
                            List<String> namedGraphUris, String query,
                            UriInfo uriInfo, Request request, String acceptHdr)
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
                                            query, uriInfo, request, acceptHdr);
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
                                          String query, UriInfo uriInfo,
                                          Request request, String acceptHdr)
                                                            throws Exception;

    protected final Repository getTargetRepository(List<String> defaultGraphUris) {
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
        return repo;
    }

    protected final Variant getResponseType(Request request,
                                            List<Variant> supportedTypes)
                                                throws WebApplicationException {
        Variant responseType = request.selectVariant(supportedTypes);
        if (responseType == null) {
            // No matching type found.
            StringBuilder buf = new StringBuilder();
            buf.append("No matching content type found. ")
               .append("Supported content types for query type: ");
            for (Variant v : supportedTypes) {
                buf.append(v.getMediaType()).append(", ");
            }
            buf.setLength(buf.length() - 2);
            String msg = buf.toString();
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

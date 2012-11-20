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


import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Variant;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.Dataset;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryResult;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandler;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.impl.DatasetImpl;
import org.openrdf.query.parser.ParsedBooleanQuery;
import org.openrdf.query.parser.ParsedGraphQuery;
import org.openrdf.query.parser.ParsedQuery;
import org.openrdf.query.parser.ParsedTupleQuery;
import org.openrdf.query.parser.sparql.SPARQLParserFactory;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.rio.n3.N3Writer;
import org.openrdf.rio.ntriples.NTriplesWriter;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.trig.TriGWriter;
import org.openrdf.rio.trix.TriXWriter;
import org.openrdf.rio.turtle.TurtleWriter;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.web.json.GridJsonWriter;
import org.datalift.fwk.util.web.json.JsonRdfHandler;
import org.datalift.fwk.util.web.json.SparqlResultsJsonWriter;
import org.datalift.fwk.util.web.json.AbstractJsonWriter.ResourceType;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.*;


/**
 * The JAS-RS root resource exposing a SPARQL endpoint that performs
 * queries using the
 * <a href="http://www.openrdf.org/">Open RDF Sesame 2</a> API.
 *
 * @author lbihanic
 */
@Path(AbstractSparqlEndpoint.MODULE_NAME)
public class SesameSparqlEndpoint extends AbstractSparqlEndpoint
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /**
     * The configuration property defining the default base URI
     * for SPARQL queries.
     */
    public final static String BASE_URI_PROPERTY = "datalift.rdf.base.uri";
    /**
     * The configuration property defining the maximum query duration
     * for SPARQL queries.
     */
    public final static String MAX_QUERY_DURATION_PROPERTY =
                                                "sparql.max.query.duration";

    /** The supported MIME types for SELECT query responses. */
    protected final static List<Variant> SELECT_RESPONSE_TYPES =
            Collections.unmodifiableList(Arrays.asList(
                    new Variant(APPLICATION_SPARQL_RESULT_XML_TYPE, null, null),
                    new Variant(APPLICATION_SPARQL_RESULT_JSON_TYPE, null, null),
                    new Variant(APPLICATION_JSON_TYPE, null, null),
                    new Variant(TEXT_HTML_TYPE, null, null),
                    new Variant(APPLICATION_XHTML_XML_TYPE, null, null),
                    new Variant(APPLICATION_XML_TYPE, null, null),
                    new Variant(TEXT_XML_TYPE, null, null),
                    new Variant(TEXT_CSV_TYPE, null, null),
                    new Variant(APPLICATION_CSV_TYPE, null, null),
                    new Variant(TEXT_COMMA_SEPARATED_VALUES_TYPE, null, null)));
    /** The supported MIME types for CONSTRUCT and DESCRIBE query responses. */
    protected final static List<Variant> CONSTRUCT_RESPONSE_TYPES =
            Collections.unmodifiableList(Arrays.asList(
                    new Variant(APPLICATION_RDF_XML_TYPE, null, null),
                    new Variant(APPLICATION_SPARQL_RESULT_JSON_TYPE, null, null),
                    new Variant(APPLICATION_JSON_TYPE, null, null),
                    new Variant(TEXT_TURTLE_TYPE, null, null),
                    new Variant(APPLICATION_TURTLE_TYPE, null, null),
                    new Variant(TEXT_N3_TYPE, null, null),
                    new Variant(TEXT_RDF_N3_TYPE, null, null),
                    new Variant(APPLICATION_N3_TYPE, null, null),
                    new Variant(APPLICATION_NTRIPLES_TYPE, null, null),
                    new Variant(APPLICATION_TRIG_TYPE, null, null),
                    new Variant(APPLICATION_TRIX_TYPE, null, null),
                    new Variant(TEXT_HTML_TYPE, null, null),
                    new Variant(APPLICATION_XHTML_XML_TYPE, null, null),
                    new Variant(APPLICATION_XML_TYPE, null, null),
                    new Variant(TEXT_XML_TYPE, null, null)));
    /** The supported MIME types for ASK query responses. */
    protected final static List<Variant> ASK_RESPONSE_TYPES =
            Collections.unmodifiableList(Arrays.asList(
                    new Variant(APPLICATION_SPARQL_RESULT_JSON_TYPE, null, null),
                    new Variant(APPLICATION_JSON_TYPE, null, null),
                    new Variant(TEXT_PLAIN_TYPE, null, null)));

    private final static String STD_JSON_SINGLE_VALUE_FMT =
            "{ \"head\":{ \"vars\":[ \"value\" ] }, " +
              "\"results\":{ \"bindings\":[ { " +
                "\"value\":{ \"type\":\"literal\", \"value\":\"%s\" } } ] } }";
    private final static String GRID_JSON_SINGLE_VALUE_FMT =
            "{ \"head\":[ \"value\" ], " +
              "\"rows\":[ { \"value\":\"%s\" } ] }";

    private final static String DESCRIBE_URL_PATTERN =
        "sparql/describe?uri={0}{1,choice," +
            ResourceType.Object.value    + "#&type=" + DescribeType.Object    + "|" +
            ResourceType.Predicate.value + "#&type=" + DescribeType.Predicate + "|" +
            ResourceType.Graph.value     + "#&type=" + DescribeType.Graph     + "|" +
            ResourceType.Graph.value     + "<}&default-graph={2}";

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public SesameSparqlEndpoint() {
        this(null);
    }

    /**
     * Creates a new SPARQL endpoint resource.
     * @param  welcomeTemplate   the Velocity template to display as
     *                           welcome page.
     */
    protected SesameSparqlEndpoint(String welcomeTemplate) {
        this(MODULE_NAME, welcomeTemplate);
    }

    /**
     * Creates a new SPARQL endpoint resource.
     * @param  name              the module name.
     * @param  welcomeTemplate   the Velocity template to display as
     *                           welcome page.
     */
    protected SesameSparqlEndpoint(String name, String welcomeTemplate) {
        super(name, welcomeTemplate);
    }

    //-------------------------------------------------------------------------
    // AbstractSparqlEndpoint contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public List<Variant> getResponseMimeTypes(QueryType queryType) {
        List<Variant> types = null;
        switch (queryType) {
            case SELECT:
                types = SELECT_RESPONSE_TYPES;
                break;
            case CONSTRUCT:
            case DESCRIBE:
                types = CONSTRUCT_RESPONSE_TYPES;
                break;
            case ASK:
                types = ASK_RESPONSE_TYPES;
                break;
            default:
                throw new IllegalArgumentException("queryType");
        }
        return types;
    }

    /** {@inheritDoc} */
    @Override
    protected ResponseBuilder doExecute(List<String> defaultGraphUris,
                                        List<String> namedGraphUris,
                                        String query, int startOffset,
                                        int endOffset, boolean gridJson,
                                        String format, String jsonCallback,
                                        UriInfo uriInfo, Request request,
                                        String acceptHdr,
                                        Map<String,Object> viewData)
                                                throws WebApplicationException {
        log.trace("Processing SPARQL query: \"{}\"", query);
        // Build base URI from request if none was enforced in configuration.
        String baseUri = this.getQueryBaseUri(uriInfo);
        // Parse SPARQL query to make sure it's valid.
        ParsedQuery parsedQuery = null;
        try {
           parsedQuery = new SPARQLParserFactory().getParser()
                                                  .parseQuery(query, baseUri);
        }
        catch (MalformedQueryException e) {
            this.handleError(query, "Syntax error: " + e.getMessage(),
                             Status.BAD_REQUEST);
        }
        // Make defaultGraphUri list mutable.
        List<String> defGraphUris = (defaultGraphUris != null) ?
                                new LinkedList<String>(defaultGraphUris): null;
        // Extract target RDF repository.
        Repository repo = this.getTargetRepository(defGraphUris);
        // Build query dataset from specified graphs, if any.
        Dataset dataset = this.buildDataset(defGraphUris, namedGraphUris);
        // Prepare HTML view parameters.
        Map<String,Object> model = new HashMap<String,Object>();
        model.put("default-graph-uri", defaultGraphUris);
        model.put("named-graph-uri", namedGraphUris);
        model.put("repository", repo.name);
        model.put("query", query);
        model.put("min",  Integer.valueOf(startOffset));
        model.put("max",  Integer.valueOf(endOffset));
        model.put("grid", Boolean.valueOf(gridJson));
        model.put("format", format);
        if (viewData != null) {
            model.putAll(viewData);
        }
        // Execute query.
        ResponseBuilder response = null;
        Variant responseType = null;
        if (parsedQuery instanceof ParsedBooleanQuery) {
            // ASK query.
            responseType = this.getResponseType(request, format,
                                                         ASK_RESPONSE_TYPES);
            MediaType mediaType = responseType.getMediaType();

            String result = Boolean.toString(this.executeAskQuery(
                                                repo, query, baseUri, dataset));
            if ((mediaType.isCompatible(APPLICATION_JSON_TYPE)) ||
                (mediaType.isCompatible(APPLICATION_SPARQL_RESULT_JSON_TYPE))) {
                String fmt = (gridJson)? GRID_JSON_SINGLE_VALUE_FMT:
                                         STD_JSON_SINGLE_VALUE_FMT;
                result = String.format(fmt, result);
                // Check for JSONP results.
                if (isSet(jsonCallback)) {
                    result = jsonCallback + '(' + result + ')';
                }
            }
            response = Response.ok(result, responseType);
            log.debug("ASK query result: {} for \"{}\"", result,
                                                new QueryDescription(query));
        }
        else if (parsedQuery instanceof ParsedGraphQuery) {
            // CONSTRUCT query.
            responseType = this.getResponseType(request, format,
                                                CONSTRUCT_RESPONSE_TYPES);
            MediaType mediaType = responseType.getMediaType();

            if (mediaType.isCompatible(TEXT_HTML_TYPE) ||
                mediaType.isCompatible(APPLICATION_XHTML_XML_TYPE)) {
                // Execute query and provide iterator to Velocity template.
                model.put("it", this.executeConstructQuery(repo, query,
                                                        startOffset, endOffset,
                                                        baseUri, dataset));
                response = Response.ok(this.newView("constructResult.vm", model));
            }
            else {
                StreamingOutput out = this.getConstructHandlerOutput(repo, query,
                                            startOffset, endOffset,
                                            gridJson, jsonCallback,
                                            baseUri, dataset, responseType);
                response = Response.ok(out, responseType);
            }
        }
        else if (parsedQuery instanceof ParsedTupleQuery) {
            // SELECT query.
            responseType = this.getResponseType(request, format,
                                                         SELECT_RESPONSE_TYPES);
            MediaType mediaType = responseType.getMediaType();

            if (mediaType.isCompatible(TEXT_HTML_TYPE) ||
                mediaType.isCompatible(APPLICATION_XHTML_XML_TYPE)) {
                // Execute query and provide iterator to Velocity template.
                model.put("it", this.executeSelectQuery(repo, query,
                                                        startOffset, endOffset,
                                                        baseUri, dataset));
                response = Response.ok(this.newView("selectResult.vm", model));
            }
            else {
                StreamingOutput out = this.getSelectHandlerOutput(repo, query,
                                            startOffset, endOffset,
                                            gridJson, jsonCallback,
                                            baseUri, dataset, responseType);
                response = Response.ok(out, responseType);
            }
        }
        else {
            this.handleError(query, "Unsupported query type",
                                                         Status.BAD_REQUEST);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private Dataset buildDataset(List<String> defaultGraphUris,
                                 List<String> namedGraphUris) {
        DatasetImpl dataset = new DatasetImpl();
        if (defaultGraphUris != null) {
            for (String g : defaultGraphUris) {
                dataset.addDefaultGraph(new URIImpl(g));
            }
        }
        if (namedGraphUris != null) {
            for (String g : namedGraphUris) {
                dataset.addNamedGraph(new URIImpl(g));
            }
        }
        return dataset;
    }

    private boolean executeAskQuery(Repository repository, String query,
                                    String baseUri, Dataset dataset) {
        boolean result = false;
        RepositoryConnection cnx = repository.newConnection();
        try {
            BooleanQuery q = cnx.prepareBooleanQuery(SPARQL, query, baseUri);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            q.setMaxQueryTime(this.getMaxQueryDuration());
            result = q.evaluate();
        }
        catch (OpenRDFException e) {
            this.handleError(query, e);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
        return result;
    }

    private QueryResultIterator<BindingSet> executeSelectQuery(
                                        Repository repository, String query,
                                        int startOffset, int endOffset,
                                        String baseUri, Dataset dataset) {
        QueryResultIterator<BindingSet> result = null;
        RepositoryConnection cnx = repository.newConnection();
        try {
            TupleQuery q = cnx.prepareTupleQuery(SPARQL, query, baseUri);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            q.setMaxQueryTime(this.getMaxQueryDuration());
            TupleQueryResult r = q.evaluate();
            result = new QueryResultIterator<BindingSet>(query,
                                                startOffset, endOffset,
                                                r, cnx, r.getBindingNames());
        }
        catch (OpenRDFException e) {
            // Close repository connection.
            try { cnx.close(); } catch (Exception e2) { /* Ignore... */ }
            // Build plain text error response.
            this.handleError(query, e);
        }
        return result;
    }

    private QueryResultIterator<Statement> executeConstructQuery(
                                        Repository repository, String query,
                                        int startOffset, int endOffset,
                                        String baseUri, Dataset dataset) {
        QueryResultIterator<Statement> result = null;
        RepositoryConnection cnx = repository.newConnection();
        try {
            GraphQuery q = cnx.prepareGraphQuery(SPARQL, query, baseUri);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            q.setMaxQueryTime(this.getMaxQueryDuration());
            result = new QueryResultIterator<Statement>(query,
                                                startOffset, endOffset,
                                                q.evaluate(), cnx, null);
        }
        catch (OpenRDFException e) {
            // Close repository connection.
            try { cnx.close(); } catch (Exception e2) { /* Ignore... */ }
            // Build plain text error response.
            this.handleError(query, e);
        }
        return result;
    }

    private StreamingOutput getConstructHandlerOutput(
                            final Repository repository, final String query,
                            final int startOffset, final int endOffset,
                            final boolean gridJson, final String jsonCallback,
                            final String baseUri,
                            final Dataset dataset, final Variant v) {
        StreamingOutput handler = null;

        MediaType mediaType = v.getMediaType();
        if ((mediaType.isCompatible(APPLICATION_JSON_TYPE)) ||
            (mediaType.isCompatible(APPLICATION_SPARQL_RESULT_JSON_TYPE))) {
            if (gridJson) {
                handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                    {
                        @Override
                        protected RDFHandler newHandler(OutputStream out) {
                            return new GridJsonWriter(out,
                                            this.baseUri + DESCRIBE_URL_PATTERN,
                                            this.repository.name, jsonCallback);
                        }
                    };
            }
            else {
                handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                    {
                        @Override
                        protected RDFHandler newHandler(OutputStream out) {
                            return new JsonRdfHandler(out);
                        }
                    };
            }
        }
        else if ((mediaType.isCompatible(TEXT_TURTLE_TYPE)) ||
                 (mediaType.isCompatible(APPLICATION_TURTLE_TYPE))) {
            handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected RDFHandler newHandler(OutputStream out) {
                        return new TurtleWriter(out);
                    }
                };
        }
        else if ((mediaType.isCompatible(TEXT_N3_TYPE)) ||
                 (mediaType.isCompatible(TEXT_RDF_N3_TYPE)) ||
                 (mediaType.isCompatible(APPLICATION_N3_TYPE))) {
            handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected RDFHandler newHandler(OutputStream out) {
                        return new N3Writer(out);
                    }
                };
        }
        else if (mediaType.isCompatible(APPLICATION_NTRIPLES_TYPE)) {
            handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected RDFHandler newHandler(OutputStream out) {
                        return new NTriplesWriter(out);
                    }
                };
        }
        else if (mediaType.isCompatible(APPLICATION_TRIG_TYPE)) {
            handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected RDFHandler newHandler(OutputStream out) {
                        return new TriGWriter(out);
                    }
                };
        }
        else if (mediaType.isCompatible(APPLICATION_TRIX_TYPE)) {
            handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected RDFHandler newHandler(OutputStream out) {
                        return new TriXWriter(out);
                    }
                };
        }
        else {
            // Assume RDF/XML...
            handler = new ConstructStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected RDFHandler newHandler(OutputStream out) {
                        return new RDFXMLWriter(out);
                    }
                };
        }
        return handler;
    }

    private StreamingOutput getSelectHandlerOutput(
                            final Repository repository, final String query,
                            final int startOffset, final int endOffset,
                            final boolean gridJson, final String jsonCallback,
                            final String baseUri,
                            final Dataset dataset, final Variant v) {
        StreamingOutput handler = null;

        MediaType mediaType = v.getMediaType();
        if ((mediaType.isCompatible(APPLICATION_JSON_TYPE)) ||
            (mediaType.isCompatible(APPLICATION_SPARQL_RESULT_JSON_TYPE))) {
            if (gridJson) {
                handler = new SelectStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                    {
                        @Override
                        protected TupleQueryResultHandler newHandler(OutputStream out) {
                            return new GridJsonWriter(out,
                                            this.baseUri + DESCRIBE_URL_PATTERN,
                                            this.repository.name, jsonCallback);
                        }
                    };
            }
            else {
                handler = new SelectStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                    {
                        @Override
                        protected TupleQueryResultHandler newHandler(OutputStream out) {
                            return new SparqlResultsJsonWriter(out,
                                            this.baseUri + DESCRIBE_URL_PATTERN,
                                            this.repository.name, jsonCallback);
                        }
                    };
            }
        }
        else if ((mediaType.isCompatible(TEXT_CSV_TYPE)) ||
                 (mediaType.isCompatible(APPLICATION_CSV_TYPE)) ||
                 (mediaType.isCompatible(TEXT_COMMA_SEPARATED_VALUES_TYPE))) {
            handler = new SelectStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected TupleQueryResultHandler newHandler(OutputStream out) {
                        return new SPARQLResultsCSVWriter(out);
                    }
                };
        }
        else {
            // Assume SPARQL Results/XML...
            handler = new SelectStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    @Override
                    protected TupleQueryResultHandler newHandler(OutputStream out) {
                        return new SPARQLResultsXMLWriter(out);
                    }
                };
        }
        return handler;
    }

    /**
     * Returns the base URI for evaluating a SPARQL query, retrieving it
     * from request path if none was enforced in configuration.
     * @param  uriInfo   the requested URI.
     *
     * @return the base URI for evaluating a SPARQL query.
     */
    private String getQueryBaseUri(UriInfo uriInfo) {
        String baseUri = Configuration.getDefault()
                                      .getProperty(BASE_URI_PROPERTY);
        return (baseUri != null)? baseUri: uriInfo.getBaseUri().toString();
    }

    private int getMaxQueryDuration() {
        int maxDuration = -1;
        String v = Configuration.getDefault()
                                .getProperty(MAX_QUERY_DURATION_PROPERTY, "-1");
        try {
            maxDuration = Integer.parseInt(v);
        }
        catch (Exception e) {
            log.warn("Invalid value for configuration parameter \"{}\": " +
                     "\"{}\". Integer value expected.",
                     MAX_QUERY_DURATION_PROPERTY, v);
        }
        return maxDuration;
    }

    private abstract class ConstructStreamingOutput implements StreamingOutput
    {
        protected final Repository repository;
        protected final String query;
        protected final int startOffset;
        protected final int endOffset;
        protected final String baseUri;
        protected final Dataset dataset;

        public ConstructStreamingOutput(Repository repository, String query,
                                        int startOffset, int endOffset,
                                        String baseUri, Dataset dataset) {
            this.repository  = repository;
            this.query       = query;
            this.startOffset = startOffset;
            this.endOffset   = endOffset;
            this.baseUri     = baseUri;
            this.dataset     = dataset;
        }

        @Override
        public void write(OutputStream out)
                                throws IOException, WebApplicationException {
            RepositoryConnection cnx = this.repository.newConnection();
            try {
                GraphQuery q = cnx.prepareGraphQuery(SPARQL,
                                                     this.query, this.baseUri);
                if (this.dataset != null) {
                    q.setDataset(this.dataset);
                }
                q.evaluate(this.getHandler(out));
            }
            catch (OpenRDFException e) {
                handleError(query, e);
            }
            finally {
                try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }

        private RDFHandler getHandler(OutputStream out) {
            final RDFHandler handler = this.newHandler(out);
            return new RDFHandlerWrapper(handler) {
                    private int count = 0;
                    @Override
                    public void handleStatement(Statement st)
                                                throws RDFHandlerException {
                        this.count++;
                        if ((endOffset != -1) && (this.count >= endOffset)) {
                            // Last request result reached. => Abort!
                            this.endRDF();
                            throw new RDFHandlerException(
                                                    new QueryDoneException());
                        }
                        if (this.count >= startOffset) {
                            // Result in request range. => Process...
                            super.handleStatement(st);
                        }
                    }
                    @Override
                    public void endRDF() throws RDFHandlerException {
                        super.endRDF();
                        if (startOffset != -1) {
                            this.count -= startOffset;
                        }
                        log.debug("Processed {} statements from <{}> for: {}",
                                  Integer.valueOf(this.count), repository.name,
                                  new QueryDescription(query));
                    }
                };
        }

        abstract protected RDFHandler newHandler(OutputStream out);
    }

    private abstract class SelectStreamingOutput implements StreamingOutput
    {
        protected final Repository repository;
        protected final String query;
        protected final int startOffset;
        protected final int endOffset;
        protected final String baseUri;
        protected final Dataset dataset;

        public SelectStreamingOutput(Repository repository, String query,
                                     int startOffset, int endOffset,
                                     String baseUri, Dataset dataset) {
            this.repository  = repository;
            this.query       = query;
            this.startOffset = startOffset;
            this.endOffset   = endOffset;
            this.baseUri     = baseUri;
            this.dataset     = dataset;
        }

        @Override
        public void write(OutputStream out)
                                throws IOException, WebApplicationException {
            RepositoryConnection cnx = this.repository.newConnection();
            try {
                TupleQuery q = cnx.prepareTupleQuery(SPARQL,
                                                     this.query, this.baseUri);
                if (this.dataset != null) {
                    q.setDataset(this.dataset);
                }
                q.evaluate(this.getHandler(out));
            }
            catch (OpenRDFException e) {
                handleError(query, e);
            }
            finally {
                try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }

        private TupleQueryResultHandler getHandler(OutputStream out) {
            final TupleQueryResultHandler handler = this.newHandler(out);
            return new TupleQueryResultHandler() {
                    private int count = 0;
                    @Override
                    public void startQueryResult(List<String> bindingNames)
                                    throws TupleQueryResultHandlerException {
                        handler.startQueryResult(bindingNames);
                    }
                    @Override
                    public void handleSolution(BindingSet b)
                                    throws TupleQueryResultHandlerException {
                        this.count++;
                        if ((endOffset != -1) && (this.count >= endOffset)) {
                            // Last request result reached. => Abort!
                            this.endQueryResult();
                            throw new TupleQueryResultHandlerException(
                                                    new QueryDoneException());
                        }
                        if (this.count >= startOffset) {
                            // Result in request range. => Process...
                            handler.handleSolution(b);
                        }
                    }
                    @Override
                    public void endQueryResult()
                                    throws TupleQueryResultHandlerException {
                        handler.endQueryResult();
                        if (startOffset != -1) {
                            this.count -= startOffset;
                        }
                        log.debug("Processed {} binding sets for: {}",
                                  Integer.valueOf(this.count),
                                  new QueryDescription(query));
                    }
                };
        }

        protected abstract TupleQueryResultHandler newHandler(OutputStream out);
    }

    //-------------------------------------------------------------------------
    // QueryResultIterator nested class
    //-------------------------------------------------------------------------

    /**
     * An iterator of the results of a SPARQL query, to support direct
     * consumption of the query results by the view (HTML template, JSP
     * page...) in streaming mode.
     *
     * @param  <T>   the expected type of query results, e.g.
     *               {@link Statement} for CONSTRUCT queries or
     *               {@link BindingSet} for SELECT queries.
     */
    public static class QueryResultIterator<T> implements CloseableIterator<T> {
        public final String query;
        public final int startOffset;
        public final int endOffset;
        public final QueryResult<T> result;
        private RepositoryConnection cnx = null;
        private final List<String> columns;
        private int count = 0;

        public QueryResultIterator(String query, int startOffset, int endOffset,
                            QueryResult<T> result, RepositoryConnection cnx,
                            List<String> columns) {
            if (result == null) {
                throw new IllegalArgumentException("result");
            }
            if (cnx == null) {
                throw new IllegalArgumentException("cnx");
            }
            this.query       = query;
            this.startOffset = startOffset;
            this.endOffset   = (endOffset < 0)? Integer.MAX_VALUE: endOffset;
            this.result      = result;
            this.cnx         = cnx;
            this.columns     = columns;
            // Skip first entries to reach requested start offset.
            while ((this.count < this.startOffset) && (this.hasNext())) {
                this.next();
            }
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = false;
            try {
                hasNext = ((this.result.hasNext())
                                            && (this.count < this.endOffset));
                if (hasNext == false) {
                    this.close();
                }
            }
            catch (OpenRDFException e) {
                this.close(e);
            }
            return hasNext;
        }

        @Override
        public T next() {
            T t = null;
            try {
                this.count++;
                t = this.result.next();
            }
            catch (OpenRDFException e) {
                this.close(e);
            }
            return t;
        }

        @Override
        public void remove() {
            try {
                this.result.remove();
            }
            catch (OpenRDFException e) {
                this.close();
            }
        }

        @Override
        public void close() {
            this.close(null);
        }

        private void close(Throwable t) {
            if (this.cnx != null) {
                // Close result and repository connection.
                try {
                    this.result.close();
                } catch (Exception e) { /* Ignore... */ }
                try {
                    this.cnx.close();
                } catch (Exception e) { /* Ignore... */ }
                this.cnx = null;

                // Check outcome.
                if (t != null) {
                    log.error("Query processing failed: \"{}\" for: {}",
                                                        t.getMessage(), query);
                    throw new RuntimeException(t.getMessage(), t);
                }
                else {
                    if (this.startOffset != -1) {
                        this.count -= this.startOffset;
                    }
                    log.debug("Processed {} results for: {}",
                                            Integer.valueOf(this.count),
                                            new QueryDescription(this.query));
                }
            }
        }

        public List<String> getColumns() {
            return this.columns;
        }
    }
}

/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.sparql;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

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
import org.openrdf.query.parser.sparql.SPARQLParserFactory;
import org.openrdf.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;
import org.openrdf.rio.rdfxml.RDFXMLWriter;
import org.openrdf.rio.turtle.TurtleWriter;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.rdf.Repository;

import static org.datalift.fwk.MediaTypes.*;


/**
 * The JAS-RS root resource exposing a SPARQL endpoint that performs
 * queries using the
 * <a href="http://www.openrdf.org/">Open RDF Sesame 2</a> API.
 *
 * @author lbihanic
 */
@Path("/sparql")
public class SesameSparqlEndpoint extends AbstractSparqlEndpoint
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The default base URI for SPARQL requests. */
    public final static String BASE_URI_PROPERTY = "datalift.rdf.base.uri";

    private final static List<Variant> SELECT_RESPONSE_TYPES = Arrays.asList(
                    new Variant(APPLICATION_SPARQL_RESULT_XML_TYPE, null, null),
                    new Variant(APPLICATION_SPARQL_RESULT_JSON_TYPE, null, null),
                    new Variant(APPLICATION_JSON_TYPE, null, null),
                    new Variant(TEXT_HTML_TYPE, null, null),
                    new Variant(APPLICATION_XHTML_XML_TYPE, null, null),
                    new Variant(APPLICATION_XML_TYPE, null, null));
    private final static List<Variant> CONSTRUCT_RESPONSE_TYPES = Arrays.asList(
                    new Variant(APPLICATION_RDF_XML_TYPE, null, null),
                    new Variant(TEXT_TURTLE_TYPE, null, null),
                    new Variant(APPLICATION_TURTLE_TYPE, null, null),
                    new Variant(TEXT_N3_TYPE, null, null),
                    new Variant(TEXT_RDF_N3_TYPE, null, null),
                    new Variant(APPLICATION_N3_TYPE, null, null),
                    new Variant(APPLICATION_NTRIPLES_TYPE, null, null),
                    new Variant(TEXT_HTML_TYPE, null, null),
                    new Variant(APPLICATION_XHTML_XML_TYPE, null, null),
                    new Variant(APPLICATION_XML_TYPE, null, null));
    private final static List<Variant> ASK_RESPONSE_TYPES = Arrays.asList(
                    new Variant(TEXT_PLAIN_TYPE, null, null));

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private String cfgBaseUri;

    //-------------------------------------------------------------------------
    // LifeCycle contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration cfg) {
        super.init(cfg);
        this.cfgBaseUri = cfg.getProperty(BASE_URI_PROPERTY);
    }

    //-------------------------------------------------------------------------
    // AbstractSparqlEndpoint contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected ResponseBuilder doExecute(List<String> defaultGraphUris,
                                        List<String> namedGraphUris,
                                        String query,
                                        int startOffset, int endOffset,
                                        boolean gridJson, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                                throws WebApplicationException {
        log.trace("Processing SPARQL query: \"{}\"", query);
        // Build base URI from request if none was enforced in configuration.
        String baseUri = (this.cfgBaseUri != null)?
                            this.cfgBaseUri: uriInfo.getBaseUri().toString();
        // Parse SPARQL query to make sure it's valid.
        ParsedQuery parsedQuery = null;
        try {
           parsedQuery = new SPARQLParserFactory()
                                       .getParser().parseQuery(query, baseUri);
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
        // Execute query.
        ResponseBuilder response = null;
        Variant responseType = null;
        if (parsedQuery instanceof ParsedBooleanQuery) {
            responseType = this.getResponseType(request, ASK_RESPONSE_TYPES);
            String result = Boolean.toString(this.executeAskQuery(
                                                repo, query, baseUri, dataset));
            response = Response.ok(result, responseType);
            log.debug("ASK query result: {} for \"{}\"", result,
                                                new QueryDescription(query));
        }
        else if (parsedQuery instanceof ParsedGraphQuery) {
            responseType = this.getResponseType(request,
                                                CONSTRUCT_RESPONSE_TYPES);
            MediaType mediaType = responseType.getMediaType();

            if (mediaType.isCompatible(TEXT_HTML_TYPE) ||
                mediaType.isCompatible(APPLICATION_XHTML_XML_TYPE)) {
                // Execute query and provide iterator to Velocity template.
                response = Response.ok(this.newViewable("/constructResult.vm",
                            this.executeConstructQuery(repo, query,
                                                       baseUri, dataset)));
            }
            else {
                StreamingOutput out = this.getRdfHandlerOutput(repo, query,
                                                baseUri, dataset, responseType);
                response = Response.ok(out, responseType);
            }
        }
        else {
            // ParsedTupleQuery
            responseType = this.getResponseType(request, SELECT_RESPONSE_TYPES);
            MediaType mediaType = responseType.getMediaType();

            if (mediaType.isCompatible(TEXT_HTML_TYPE) ||
                mediaType.isCompatible(APPLICATION_XHTML_XML_TYPE)) {
                // Execute query and provide iterator to Velocity template.
                response = Response.ok(this.newViewable("/selectResult.vm",
                    this.executeSelectQuery(repo, query, baseUri, dataset)));
            }
            else {
                StreamingOutput out = this.getResultHandlerOutput(repo, query,
                                            startOffset, endOffset, gridJson,
                                            baseUri, dataset, responseType);
                response = Response.ok(out, responseType);
            }
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
                                        String baseUri, Dataset dataset) {
        QueryResultIterator<BindingSet> result = null;
        RepositoryConnection cnx = repository.newConnection();
        try {
            TupleQuery q = cnx.prepareTupleQuery(SPARQL, query, baseUri);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            TupleQueryResult r = q.evaluate();
            result = new QueryResultIterator<BindingSet>(query,
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
                                        String baseUri, Dataset dataset) {
        QueryResultIterator<Statement> result = null;
        RepositoryConnection cnx = repository.newConnection();
        try {
            GraphQuery q = cnx.prepareGraphQuery(SPARQL, query, baseUri);
            if (dataset != null) {
                q.setDataset(dataset);
            }
            result = new QueryResultIterator<Statement>(
                                                query, q.evaluate(), cnx, null);
        }
        catch (OpenRDFException e) {
            // Close repository connection.
            try { cnx.close(); } catch (Exception e2) { /* Ignore... */ }
            // Build plain text error response.
            this.handleError(query, e);
        }
        return result;
    }

    private StreamingOutput getRdfHandlerOutput(Repository repository,
                                            final String query, String baseUri,
                                            Dataset dataset, Variant v) {
        StreamingOutput handler = null;

        MediaType mediaType = v.getMediaType();
        if ((mediaType.isCompatible(TEXT_TURTLE_TYPE)) ||
            (mediaType.isCompatible(APPLICATION_TURTLE_TYPE)) ||
            (mediaType.isCompatible(TEXT_N3_TYPE)) ||
            (mediaType.isCompatible(TEXT_RDF_N3_TYPE)) ||
            (mediaType.isCompatible(APPLICATION_N3_TYPE)) ||
            (mediaType.isCompatible(APPLICATION_NTRIPLES_TYPE))) {
            handler = new ConstructStreamingOutput(repository, query,
                                                            baseUri, dataset)
                {
                    protected RDFHandler newHandler(OutputStream out) {
                        return new TurtleWriter(out);
                    }
                };
        }
        else {
            // Assume RDF/XML...
            handler = new ConstructStreamingOutput(repository, query,
                                                            baseUri, dataset)
                {
                    protected RDFHandler newHandler(OutputStream out) {
                        return new RDFXMLWriter(out);
                    }
                };
        }
        return handler;
    }

    private StreamingOutput getResultHandlerOutput(Repository repository,
                                            final String query,
                                            int startOffset, int endOffset,
                                            boolean gridJson, String baseUri,
                                            Dataset dataset, Variant v) {
        StreamingOutput handler = null;

        MediaType mediaType = v.getMediaType();
        if ((mediaType.isCompatible(APPLICATION_JSON_TYPE)) ||
            (mediaType.isCompatible(APPLICATION_SPARQL_RESULT_JSON_TYPE))) {
            if (gridJson) {
                handler = new SelectStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                    {
                        protected TupleQueryResultHandler newHandler(OutputStream out) {
                            return new GridJsonWriter(out);
                        }
                		
                    };
            }
            else {
                handler = new SelectStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                    {
                        protected TupleQueryResultHandler newHandler(OutputStream out) {
                            return new SPARQLResultsJSONWriter(out);
                        }
                    };
            }
        }
        else {
            // Assume SPARQL Results/XML...
            handler = new SelectStreamingOutput(repository, query,
                                    startOffset, endOffset, baseUri, dataset)
                {
                    protected TupleQueryResultHandler newHandler(OutputStream out) {
                        return new SPARQLResultsXMLWriter(out);
                    }
                };
        }
        return handler;
    }

    private abstract class ConstructStreamingOutput implements StreamingOutput
    {
        private final Repository repository;
        private final String query;
        private final String baseUri;
        private final Dataset dataset;

        public ConstructStreamingOutput(Repository repository, String query,
                                        String baseUri, Dataset dataset) {
            this.repository = repository;
            this.query      = query;
            this.baseUri    = baseUri;
            this.dataset    = dataset;
        }

        @Override
        public void write(OutputStream out)
                                throws IOException, WebApplicationException {
            RepositoryConnection cnx = this.repository.newConnection();
            try {
                GraphQuery q = cnx.prepareGraphQuery(SPARQL,
                                                     this.query, this.baseUri);
                if (dataset != null) {
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
            return new RDFHandlerWrapper(this.newHandler(out)) {
                    private int consumed = 0;
                    @Override
                    public void handleStatement(Statement st)
                                            throws RDFHandlerException {
                        super.handleStatement(st);
                        this.consumed++;
                    }
                    @Override
                    public void endRDF() throws RDFHandlerException {
                        super.endRDF();
                        log.debug("Processed {} results for: {}",
                                                Integer.valueOf(this.consumed),
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
        protected final	int startOffset;
        protected final int endOffset;
        protected final String 	baseUri;
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
                if (dataset != null) {
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
                            handler.endQueryResult();
                            throw new TupleQueryResultHandlerException(
                                                    new QueryDoneException());
                        }
                        if (this.count >= endOffset) {
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

    public static class QueryResultIterator<T> implements Iterator<T> {
        public final String query;
        public final QueryResult<T> result;
        private RepositoryConnection cnx = null;
        private final List<String> columns;
        private int consumed = 0;

        public QueryResultIterator(String query, QueryResult<T> result,
                            RepositoryConnection cnx, List<String> columns) {
            if (result == null) {
                throw new IllegalArgumentException("result");
            }
            if (cnx == null) {
                throw new IllegalArgumentException("cnx");
            }
            this.query   = query;
            this.result  = result;
            this.cnx     = cnx;
            this.columns = columns;
        }

        @Override
        public boolean hasNext() {
            boolean hasNext = false;
            try {
                hasNext = this.result.hasNext();
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
                this.consumed++;
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
                    log.debug("Processed {} results for: {}",
                                            Integer.valueOf(this.consumed),
                                            new QueryDescription(this.query));
                }
            }
        }

        public List<String> getColumns() {
            return this.columns;
        }
    }
}

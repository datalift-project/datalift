package org.datalift.s4ac.services;


import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerBase;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryConnection;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.AccessContextProvider;
import org.datalift.s4ac.TechnicalException;

import static org.datalift.fwk.rdf.RdfNamespace.*;


public class SparqlAccessContextProvider extends BaseModule
                                         implements AccessContextProvider
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The SPARQL query to extract context-populating queries. */
    private final static String CONTEXT_QUERIES_QUERY =
            "PREFIX rdf: <" + RDF.uri + ">\n" +
            "PREFIX rdfs: <" + RDFS.uri + ">\n" +
            "PREFIX datalift: <" + DataLift.uri + ">\n" +
            "SELECT ?q ?r WHERE { " +
                "?s a datalift:SparqlQuery ; rdf:value ?q . " +
                "OPTIONAL { ?s datalift:defGraphUri ?r . } }";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The SPARQL queries to populate the access context. */
    private final Collection<QueryDesc> queries = new LinkedList<QueryDesc>();
    /** The S4AC access controller instance. */
    private S4acAccessController s4ac = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public SparqlAccessContextProvider() {
        super(SparqlAccessContextProvider.class.getSimpleName());
    }

    //-------------------------------------------------------------------------
    // BaseModule contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        // Retrieve the security repository
        this.s4ac = configuration.getBean(S4acAccessController.class);
        Repository securityRepository = s4ac.getSecurityRepository();
        // Extract access control context building queries.
        RepositoryConnection cnx = null;
        try {
            this.queries.clear();
            cnx = securityRepository.newConnection();
            TupleQueryResult result =
                        cnx.prepareTupleQuery(SPARQL, CONTEXT_QUERIES_QUERY)
                           .evaluate();
            while (result.hasNext()) {
                BindingSet bs = result.next();
                Value r = bs.getValue("r");
                this.queries.add(
                        new QueryDesc(bs.getValue("q").stringValue(),
                                      (r != null)? r.stringValue(): null));
            }
        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    //-------------------------------------------------------------------------
    // AccessContextProvider contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void populateContext(Map<String,Object> context,
                                Repository target) {
        final Map<String,Object> ctx = new HashMap<String,Object>();
        for (QueryDesc q : this.queries) {
            ctx.clear();
            try {
                Repository r = null;
                if (q.repository != null) {
                    r = Configuration.getDefault().getRepository(q.repository);
                }
                else {
                    r = this.s4ac.getPolicyEvaluationRepository(target);
                }
                r.select(q.query, context,
                    new TupleQueryResultHandlerBase() {
                        @Override
                        public void handleSolution(BindingSet bs)
                                    throws TupleQueryResultHandlerException {
                            for (String b : bs.getBindingNames()) {
                                Binding v = bs.getBinding(b);
                                if (b != null) {
                                    ctx.put(b, v.getValue());
                                }
                            }
                        }
                    });
                log.debug("Added context data from query \"{}\": {}",
                          q.query, ctx);
                context.putAll(ctx);
            }
            catch (Exception e) {
                // Ignore...
            }
        }
    }

    //-------------------------------------------------------------------------
    // QueryDesc nested class
    //-------------------------------------------------------------------------

    /**
     * Description of a SPARQL query.
     */
    private final static class QueryDesc
    {
        public final String query;
        public final String repository;

        public QueryDesc(String query, String repository) {
            this.query      = query;
            this.repository = repository;
        }
    }
}

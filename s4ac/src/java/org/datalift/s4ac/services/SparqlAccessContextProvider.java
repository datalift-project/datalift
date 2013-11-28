package org.datalift.s4ac.services;


import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.query.TupleQueryResultHandlerBase;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.RepositoryConnection;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.AccessContextProvider;
import org.datalift.s4ac.TechnicalException;

import static org.datalift.fwk.rdf.RdfNamespace.*;


public class SparqlAccessContextProvider extends BaseModule
                                         implements AccessContextProvider
{
    private final static String CONTEXT_QUERIES_QUERY =
            "PREFIX rdf: <" + RDF.uri + ">\n" +
            "PREFIX rdfs: <" + RDFS.uri + ">\n" +
            "PREFIX datalift: <" + DataLift.uri + ">\n" +
            "SELECT ?q WHERE { ?s a datalift:SparqlQuery ; rdf:value ?q . }";

    private final Collection<String> queries = new LinkedList<String>();
    private Repository securityRepository = null;

    public SparqlAccessContextProvider() {
        super(SparqlAccessContextProvider.class.getSimpleName());
    }

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        // Retrieve the security repository
        S4acAccessController s4ac =
                            configuration.getBean(S4acAccessController.class);
        this.securityRepository = s4ac.getSecurityRepository();
        // Extract access control context building queries.
        RepositoryConnection cnx = null;
        try {
            this.queries.clear();
            cnx = this.securityRepository.newConnection();
            TupleQueryResult result =
                        cnx.prepareTupleQuery(SPARQL, CONTEXT_QUERIES_QUERY)
                           .evaluate();
            String p = result.getBindingNames().get(0);
            while (result.hasNext()) {
                this.queries.add(
                        result.next().getBinding(p).getValue().stringValue());
            }
        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    @Override
    public void populateContext(final Map<String,Object> context) {
        for (String q : this.queries) {
            try {
                this.securityRepository.select(q, context,
                    new TupleQueryResultHandlerBase() {
                        @Override
                        public void handleSolution(BindingSet bs)
                                    throws TupleQueryResultHandlerException {
                            for (String b : bs.getBindingNames()) {
                                Binding v = bs.getBinding(b);
                                if (b != null) {
                                    context.put(b, v.getValue());
                                }
                            }
                        }
                    });
            }
            catch (Exception e) {
                // Ignore...
            }
        }
    }
}

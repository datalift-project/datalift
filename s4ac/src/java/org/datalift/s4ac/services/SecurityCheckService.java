package org.datalift.s4ac.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.s4ac.utils.CRUDType;

import org.openrdf.OpenRDFException;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;


public class SecurityCheckService extends BaseModule {
	private Repository securedRepository;
	private Repository liftedRepository;	
	
	private static final Log log = LogFactory.getLog(SecurityCheckService.class);
	
	String queryCtx = "PREFIX s4ac:<http://ns.inria.fr/s4ac/v2#> " +
    		"PREFIX rdf:<http://www.w3.org/1999/02/22-rdf-syntax-ns#> " +
			"SELECT * " +
			"WHERE{ " +
			"?ap a s4ac:AccessPolicy . " +
			"?ap s4ac:appliesTo ?graph . " +
			"?ap s4ac:hasAccessConditionSet ?acs . " +
			"?ap s4ac:hasAccessPrivilege ?privilege . " +
			"?acs rdf:type ?acstype . " +
			"FILTER(?acstype = <http://ns.inria.fr/s4ac/v2#ConjunctiveAccessConditionSet> || ?acstype=<http://ns.inria.fr/s4ac/v2#DisjunctiveAccessConditionSet>) . " +
			"?acs s4ac:hasAccessCondition ?ac . " +
			"?ac s4ac:hasQueryAsk ?ask } "; 
//			"GROUP BY ?ap ?graph ?privilege ?acs ?acstype ?ac";

	private Map<String, AccessPolicy> aps = new HashMap<String, AccessPolicy>();
	private Map<CRUDType, Set<String>> apindex = new HashMap<CRUDType, Set<String>>();

	@SuppressWarnings("deprecation")
	public SecurityCheckService(){
		super("s4ac-securityCheckService",true);
	}
	
	public void init() {
		try {
			this.liftedRepository = Configuration.getDefault().getDataRepository();
    		this.securedRepository = Configuration.getDefault().getRepository("secured");
    		
//    		this.securedRepository = Configuration.getDefault().newRepository("secured", null, false);
		} catch (Exception e) {
			log.fatal("Unable to get repositories", e);
			throw new RuntimeException(e);
		}
		
    	try {
            RepositoryConnection conn = this.securedRepository.newConnection();
            log.info("Opened connection to Secured repo");
            try {
                TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, queryCtx);
                TupleQueryResult result = tupleQuery.evaluate();
                try {
                	while (result.hasNext()) {
                		BindingSet bindingSet = result.next();
                		String apuri = bindingSet.getValue("ap").stringValue();
                		String acstype = bindingSet.getValue("acstype").stringValue();
                		String g = bindingSet.getValue("graph").stringValue();
                		
                		AccessPolicy ap;
                		if(aps.containsKey(apuri)) 
            				ap = aps.get(apuri);
            			else {
            				ap = new AccessPolicy(apuri, acstype, new HashSet<String>());
            				aps.put(apuri, ap);
            			}
                		
                		ap.addGraph(g);
            			String ask = bindingSet.getValue("ask").stringValue();
            			ap.addAsk(ask);
            			
                		String privilege = bindingSet.getValue("privilege").stringValue();
                		
                		CRUDType type = CRUDType.UNKNOWN;
            			if(privilege.equals("http://ns.inria.fr/s4ac/v2/Create")) type = CRUDType.CREATE;
            			else if(privilege.equals("http://ns.inria.fr/s4ac/v2/Read")) type = CRUDType.READ;
            			else if(privilege.equals("http://ns.inria.fr/s4ac/v2/Update")) type = CRUDType.UPDATE;
            			else if(privilege.equals("http://ns.inria.fr/s4ac/v2/Delete")) type = CRUDType.DELETE;
            			
            			ap.addPrivilege(type);
            			
            			Set<String> index;
            			if(apindex.containsKey(type)) index = apindex.get(type);
            			else{
            				index = new HashSet<String>();
            				apindex.put(type, index);
            			}
            			index.add(apuri);
            			
                	}
               
                	log.info("Got " + aps.size() + " access policies");
                }
                finally {
                   result.close();
                }
             }
             finally {
                conn.close();
             }
          }
          catch (OpenRDFException e) {
             log.fatal("Failed to get Access Policies");
             throw new RuntimeException(e);
          }
	}

	public Map<String, AccessPolicy> getAps() {
		return aps;
	}

	public Map<CRUDType, Set<String>> getApindex() {
		return apindex;
	}

	public Set<String> check(String logged_user) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		
		Set<String> graphs = new HashSet<String>();
		graphs.add("http://example.com/default"); // on ajoute le graphe par défaut
		for(AccessPolicy ap : aps.values()){
			boolean isok = true;
			boolean isokFalse=false;
			
			if(ap.getAcstype()== ACSType.CONJUNCTIVE){
				// set conjonctif, tous les asks doivent etre valides
				for(String askQuery : ap.getAsks()){
					isok = isok && ask(askQuery, logged_user);
				}
				if(isok==true){
					graphs.addAll(ap.getGraph());
				}
			}
			else if(ap.getAcstype()== ACSType.DISJUNCTIVE){
				// set disjonjonctif, au moins un ask doit etre valide
				for(String askQuery : ap.getAsks()){
					isokFalse = ask(askQuery, logged_user);
					if(isokFalse==true){
						graphs.addAll(ap.getGraph());
						break;
					}
				}
			}
		}
		
		if (graphs.size() >= 1 ) {
			graphs.remove("http://example.com/default");
		}
		log.info("Graphs " + graphs);
		return graphs;
	}	

	public boolean ask(String queryContent, String sessid) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		
		if(sessid != null && !sessid.equals("")){
			queryContent += "\nBINDINGS ?context {(<"+ ContextURI.get(sessid) +"#ctx>)}";
		}		
		RepositoryConnection conn = this.securedRepository.newConnection();
		BooleanQuery bquery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryContent);
		boolean res = bquery.evaluate();
		
        log.debug("RESULT ASK : "+String.valueOf(res));		
        
        return res;
	}

	public Set<String> getAccessibleGraphs(List<String> namedGraphUris) {
		
		String logged_user = SecurityContext.getUserPrincipal();
		
		log.info("logged user = " + logged_user);
		
		Set<String> ng = null;
		Set<String> tot = null;
		try {
			ng = this.check(logged_user);
			log.debug("Named graph checking done. Found " +ng.size() +" named graphs" );
		} catch (RepositoryException e) {
			e.printStackTrace();
		} catch (MalformedQueryException e) {
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			e.printStackTrace();
		}
		
		if (ng.size() == 0) {
			log.debug("NAMED GRAPH SIZE = 0... Computing difference...");
			Set<String> prot = this.getProtectedNamedGraphs();
			tot = this.getAllGraphsFromLifted();
			tot.removeAll(prot);
			return tot;
		} else 
			return ng;
	}

	private Set<String> getAllGraphsFromLifted() {
		Set<String> all = new HashSet<String>();
		try {
			RepositoryConnection conn = this.liftedRepository.newConnection();
			String query = "SELECT DISTINCT ?g WHERE {GRAPH ?g {?s ?p ?o}}";
            try {
                TupleQuery tupleQuery = conn.prepareTupleQuery(QueryLanguage.SPARQL, query);
                TupleQueryResult result = tupleQuery.evaluate();
                try {
                	while (result.hasNext()) {
                		BindingSet bindingSet = result.next();
                		String g = bindingSet.getValue("g").stringValue();
                		all.add(g);
                		}
                }
                finally {
                   result.close();
                }
             }
             finally {
                conn.close();
             }
          }
          catch (OpenRDFException e) {
             log.fatal("Failed to get graphs from lifted");
             throw new RuntimeException(e);
          }
		return all;
	}
	
	private Set<String> getProtectedNamedGraphs() {		
		Set<String> graphs = new HashSet<String>();
		graphs.add("http://example.com/default"); // on ajoute le graphe par défaut
		for(AccessPolicy ap : aps.values()){
			graphs.addAll(ap.getGraph());
		}

		if (graphs.size() >= 1 ) {
			graphs.remove("http://example.com/default");
		}
		return graphs;
	}
	
/*
	public Set<String> checkByPrivilege(SecuredSparqlQuery sqry, String sessid) throws RepositoryException, MalformedQueryException, QueryEvaluationException  {
		
		Set<String> graphs = new HashSet<String>();
				
		// select AP depending on the query CRUDType
		CRUDType type = sqry.getCrudType();
		if(apindex.get(type)!=null){
			
			Set<String> apByPrivilege = apindex.get(type);
			
			for(String apuri : apByPrivilege){
				AccessPolicy ap = aps.get(apuri);
				boolean isok = true;
				boolean isokFalse=false;
				
				if(ap.getAcstype()== ACSType.CONJUNCTIVE){
					// set conjonctif, tous les asks doivent etre valides
					for(String askQuery : ap.getAsks()){
						isok = isok && ask(askQuery, sessid);
					}
					if(isok==true){
						graphs.addAll(ap.getGraph());
					}
				}
				else if(ap.getAcstype()== ACSType.DISJUNCTIVE){
					// set disjonjonctif, au moins un ask doit etre valide
					for(String askQuery : ap.getAsks()){
						isokFalse = ask(askQuery, sessid);
						if(isokFalse==true){
							graphs.addAll(ap.getGraph());
							protected final Viewable newViewable(String templateName, Object it) {
								return new Viewable("/" + this.getName() + templateName, it);
							}		break;
						}
					}
				}
			}
		}

		return graphs;
	}
*/
}

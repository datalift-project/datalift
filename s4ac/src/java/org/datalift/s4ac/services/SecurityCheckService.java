package org.datalift.s4ac.services;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.datalift.fwk.BaseModule;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.s4ac.Config;
import org.datalift.s4ac.utils.CRUDType;

import org.openrdf.OpenRDFException;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;


public class SecurityCheckService extends BaseModule {
	private Repository securedRepository;
	private Repository liftedRepository;	
	
	private SecurityContext ctx;
	
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

	public SecurityCheckService(){
		super("s4ac-securityCheckService",true);
	}
	
	public void init() {
				
		try {
			this.liftedRepository = new HTTPRepository(Config.getSesameServer(), Config.getLiftedRep());
			this.securedRepository = new HTTPRepository(Config.getSesameServer(), Config.getSecureRep());
		} catch (Exception e) {
			log.fatal("Unable to get repositories", e);
			throw new RuntimeException(e);
		}
		
    	try {
            RepositoryConnection conn = this.securedRepository.getConnection();
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
//                		String graph = bindingSet.getValue("graph").stringValue();
                		
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

	public Set<String> check(String sessid) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		
		Set<String> graphs = new HashSet<String>();
		graphs.add("http://example.com/default"); // on ajoute le graphe par défaut
		for(AccessPolicy ap : aps.values()){
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
	
//	public Set<String> checkByPrivilege(SecuredSparqlQuery sqry, String sessid) throws RepositoryException, MalformedQueryException, QueryEvaluationException  {
//		
//		Set<String> graphs = new HashSet<String>();
//				
//		// select AP depending on the query CRUDType
//		CRUDType type = sqry.getCrudType();
//		if(apindex.get(type)!=null){
//			
//			Set<String> apByPrivilege = apindex.get(type);
//			
//			for(String apuri : apByPrivilege){
//				AccessPolicy ap = aps.get(apuri);
//				boolean isok = true;
//				boolean isokFalse=false;
//				
//				if(ap.getAcstype()== ACSType.CONJUNCTIVE){
//					// set conjonctif, tous les asks doivent etre valides
//					for(String askQuery : ap.getAsks()){
//						isok = isok && ask(askQuery, sessid);
//					}
//					if(isok==true){
//						graphs.addAll(ap.getGraph());
//					}
//				}
//				else if(ap.getAcstype()== ACSType.DISJUNCTIVE){
//					// set disjonjonctif, au moins un ask doit etre valide
//					for(String askQuery : ap.getAsks()){
//						isokFalse = ask(askQuery, sessid);
//						if(isokFalse==true){
//							graphs.addAll(ap.getGraph());
//							protected final Viewable newViewable(String templateName, Object it) {
//								return new Viewable("/" + this.getName() + templateName, it);
//							}		break;
//						}
//					}
//				}
//			}
//		}
//
//		return graphs;
//	}

	public boolean ask(String queryContent, String sessid) throws RepositoryException, MalformedQueryException, QueryEvaluationException {
		
		if(sessid != null && !sessid.equals("")){
			queryContent += "\nBINDINGS ?context {(<"+ ContextURI.get(sessid) +"#ctx>)}";
		}
		
//		String ee = queryContent.replaceAll("\\?context", "<"+ ContextURI.get(sessid) +"#ctx>");
//		log.info("Ask eseguita:  " + ee);
		
		RepositoryConnection conn = this.securedRepository.getConnection();
		BooleanQuery bquery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, queryContent);
//		BooleanQuery bquery = conn.prepareBooleanQuery(QueryLanguage.SPARQL, ee);
		boolean res = bquery.evaluate();
		
        log.debug("RESULT ASK : "+String.valueOf(res));		
        
        return res;
	}
	

}

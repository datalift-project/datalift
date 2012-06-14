package org.datalift.s4ac;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.s4ac.resources.SecuredSparqlQuery;
import org.datalift.s4ac.services.SecurityCheckService;

import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;


import com.sun.jersey.api.view.Viewable;






public class S4acModule extends BaseModule implements ProjectModule
{
    private final static Logger log = Logger.getLogger();
    
    private Configuration c = Configuration.getDefault();
        
	protected ProjectManager projectManager;
    protected SparqlEndpoint sparqlEndpoint;
	private Repository liftedRepository;
	@SuppressWarnings("unused")
	private Repository securedRepository;
	
	private SecurityCheckService sec;
	private List<String> resultForVm = new ArrayList<String>();
	
	public S4acModule() {
        super("s4ac", true);
        log.info("S4ac module created", this);
    }
    
    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
    	
    	super.postInit(this.c);
      
        try {
    		this.liftedRepository = new HTTPRepository(Config.getSesameServer(), Config.getLiftedRep());
    		this.securedRepository = new HTTPRepository(Config.getSesameServer(), Config.getSecureRep());
    	} catch (Exception e) {
    		log.fatal("Unable to get repositories", e);
    		throw new RuntimeException(e);
    	}

        this.projectManager = this.c.getBean(ProjectManager.class);
        if (this.projectManager == null) {
            throw new RuntimeException("project.manager.not.available");
        }
//        this.sparqlEndpoint = this.c.getBean(SparqlEndpoint.class);
//        if (this.sparqlEndpoint == null) {
//            throw new RuntimeException("sparql.endpoint.not.available");
//        }List<String> resultForVm = new ArrayList<String>();
//        
        
    }
        	

	@Override
    public UriDesc canHandle(Project p) {       
		// TODO Auto-generated method stub
		return null;
    }


	@GET
	public Viewable getIndex() {
		return this.newViewable("/index.vm", null);
	}

	protected final Viewable newViewable(String templateName, Object it) { 
		return new Viewable("/" + this.getName() + templateName, it);
	}
	
	@POST
/*	public Response doPost(@FormParam("query") String qrycontent, @FormParam("graph") String usergraph, @FormParam("ctx") String construct,  
			@FormParam("uid") String sessid, @Context HttpServletRequest req ){
*/		
	public Response doPost(@FormParam("query") String qrycontent, @FormParam("graph") String usergraph, @FormParam("ctx") String construct,  
		@Context HttpServletRequest req ){
		
		HttpSession session = req.getSession(true);
		String sessid = SecurityContext.getUserPrincipal();
    	if ((sessid == null) || sessid.isEmpty())
    		sessid = session.getId();
        	
    	log.info("Authenticated user id : " + sessid);
    	
    	this.sec = new SecurityCheckService();
        if (this.sec == null) {
        	throw new RuntimeException("security-check-service.not.available");
        } else 
        	this.sec.init();
         
        
        try {
			Set<String> ng = this.sec.check(sessid);
			log.debug("Named graph checking done. Found " +ng.size() +" named graphs" );
			if (ng.size() > 0) {
				SecuredSparqlQuery sqry = new SecuredSparqlQuery(qrycontent, ng);
				RepositoryConnection con = this.liftedRepository.getConnection();
				TupleQuery tupleQuery = con.prepareTupleQuery(QueryLanguage.SPARQL, sqry.getQrycontent());
				TupleQueryResult result = tupleQuery.evaluate();
				List<String> bn = result.getBindingNames();
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
	        		String first = bindingSet.getValue(bn.get(0)).stringValue();
	        		resultForVm.add(first); 
				}	
				log.info("Found " + resultForVm.size() + "results.");
			}
			else {
				log.info("No named graphs matching user " + sessid +": no results found.");
			}
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedQueryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (QueryEvaluationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
   
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("result", resultForVm);
        args.put("converter", this);
        return Response.ok(this.newViewable("/data.vm", args)).build();
        
	}    
	

}


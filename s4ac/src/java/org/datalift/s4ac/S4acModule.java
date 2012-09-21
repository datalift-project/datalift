package org.datalift.s4ac;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;
import org.datalift.s4ac.resources.SecuredSparqlQuery;
import org.datalift.s4ac.services.SecurityCheckService;

import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;


@Path(S4acModule.MODULE_NAME)
public class S4acModule extends BaseModule
{
    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "s4ac";

    private final static Logger log = Logger.getLogger();
	
	private SecurityCheckService sec;
	private List<String> resultForVm = new ArrayList<String>();
	
    public S4acModule() {
        super(MODULE_NAME);
        log.info("S4ac module created");
    }

	@GET
	public TemplateModel getIndex() {
		return this.newViewable("/index.vm", null);
	}

	protected final TemplateModel newViewable(String templateName, Object it) { 
		return ViewFactory.newView("/" + this.getName() + templateName, it);
	}
	
	@POST
	public Response doPost(@FormParam("query") String qrycontent, @FormParam("graph") String usergraph, @FormParam("ctx") String construct) {
    	String sessid = SecurityContext.getUserPrincipal();
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
				Configuration cfg = Configuration.getDefault();
				RepositoryConnection con = cfg.getDefaultRepository().newConnection();
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


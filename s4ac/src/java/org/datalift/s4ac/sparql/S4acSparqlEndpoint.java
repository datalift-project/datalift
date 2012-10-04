package org.datalift.s4ac.sparql;


import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.Configuration;
import org.datalift.s4ac.resources.SecuredSparqlQuery;
import org.datalift.s4ac.services.SecurityCheckService;
import org.datalift.sparql.SesameSparqlEndpoint;


@Path(SesameSparqlEndpoint.MODULE_NAME)
public class S4acSparqlEndpoint extends SesameSparqlEndpoint
{
    private SecurityCheckService scs;

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        super.init(configuration);
        this.scs = new SecurityCheckService();
    }

    @Override
    public boolean allowsAnonymousAccess() {
        return false;
    }

    //-------------------------------------------------------------------------
    // AbstractSparqlEndpoint contract support
    //-------------------------------------------------------------------------

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
        if (this.scs == null) {
            throw new RuntimeException("SecurityCheckService.not.available");
        } else {
            this.scs.init();
        }
       
        if (this.scs.getAps().size() > 0) {
            Set<String> targetNamedGraphs = this.scs.getAccessibleGraphs(namedGraphUris); 
            String newQuery;
            if (targetNamedGraphs.size() == 0) {
            	log.debug("User cannot access... ");
            	//FIXME ...not so good......
            	newQuery = query + " Limit 0";
            } else {
            	SecuredSparqlQuery sqry = new SecuredSparqlQuery(query, targetNamedGraphs);
                log.debug("Secured Query : " + sqry);	
                newQuery = sqry.getQrycontent();
            }
            log.debug("newQuery : " + newQuery);
            List<String> targetNamedGraphsList = new ArrayList<String>(targetNamedGraphs);
                    
            
            return super.doExecute(defaultGraphUris, targetNamedGraphsList, 
            		newQuery, startOffset, endOffset, gridJson,
                                   format, jsonCallback, uriInfo, request,
                                   acceptHdr, viewData);
        }
        else {
        	log.info("Not secured query execution: SPARQL endpoint not protected (no AP)");
        	return super.doExecute(defaultGraphUris, namedGraphUris, query, startOffset,
                    endOffset, gridJson,format, jsonCallback,uriInfo, request,
                    acceptHdr,viewData);
        }
    }
}

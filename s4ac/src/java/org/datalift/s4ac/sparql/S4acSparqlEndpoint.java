package org.datalift.s4ac.sparql;

import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.Configuration;
import org.datalift.s4ac.services.SecurityCheckService;
import org.datalift.sparql.AbstractSparqlEndpoint;
import org.datalift.sparql.SesameSparqlEndpoint;

@Path("/" + AbstractSparqlEndpoint.MODULE_NAME)
public class S4acSparqlEndpoint extends SesameSparqlEndpoint {
	
	private String cfgBaseUri;
	private SecurityCheckService scs;

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        super.init(configuration);
        this.cfgBaseUri = configuration.getProperty(BASE_URI_PROPERTY);
        this.scs = new SecurityCheckService();
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
                                        String format, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                                throws WebApplicationException {
    	
    	if (this.scs == null) {
        	throw new RuntimeException("SecurityCheckService.not.available");
        } else 
        	this.scs.init();
    	
    	List<String> targetNamedGraphs = this.scs.getAccessibleGraphs(namedGraphUris); 
    	
        return super.doExecute(defaultGraphUris, targetNamedGraphs, 
        		query,startOffset, endOffset, gridJson, format, uriInfo, request, acceptHdr);
        
      }

}

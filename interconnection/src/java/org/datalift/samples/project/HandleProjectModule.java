/*
 * Copyright / INRIA 2011-2012
 * Contributor(s) : Z. Fan, J. Euzenat, F. Scharffe
 *
 * Contact: zhengjie.fan@inria.fr
 */

package org.datalift.samples.project;

import java.io.File;
import java.io.ObjectStreamException;
import java.util.HashMap;
import java.util.Map;

import javassist.bytecode.Descriptor.Iterator;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;

import de.fuberlin.wiwiss.silk.Silk;

@Path("/" + HandleProjectModule.MODULE_NAME)
public class HandleProjectModule extends BaseInterconnectionModule
{
	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    public final static String MODULE_NAME = "silk";
	
    private final static Logger log = Logger.getLogger();

    public HandleProjectModule() {
        super(MODULE_NAME);
    }

    //@Override
    public UriDesc canHandle(Project p) {
    	
    	UriDesc projectURL = null;
    	
    	try {   	  	
    		  	
            if (p.getSources().size()>1) {
            // The URI should be a URI for running the interconnection
            projectURL = new UriDesc(this.getName() + "?project=" + p.getUri(),
                               "Interconnection");     	
            
            //if (this.position > 0) {
                projectURL.setPosition(100000);
            //}
            }
            
        }
        catch (Exception e) {
            log.fatal("Uh?", e);
            throw new RuntimeException(e);
        }
        return projectURL;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------
    
    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getIndexPage(@QueryParam("project") java.net.URI projectId) 
    		                                            throws ObjectStreamException
    {
        // Retrieve project.
        Project p = this.getProject(projectId);		
        // Display conversion configuration page.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p);
        args.put("linking", this);
        return Response.ok(this.newViewable("/silk-webpage.vm", args))
                       .build();
    }
    
    @POST
    @Path("run-silk")
    @Produces(MediaTypes.TEXT_PLAIN)
    public String doRun(@QueryParam("project") java.net.URI projectId,
                        @FormParam("configFile") File configFile,
                        @FormParam("linkSpecId") String linkSpecId,
                        @FormParam("numThreads") int numThreads,
                        @FormParam("reload") boolean reload)
                        		throws ObjectStreamException
    {    	
        //link the data sets
        Silk.executeFile(configFile, linkSpecId, numThreads, reload);
        
        return "OK~~";
        
    }
    
}

/*
 * Copyright / INRIA 2011-2012
 * Contributor(s) : Z. Fan, J. Euzenat, F. Scharffe
 *
 * Contact: zhengjie.fan@inria.fr
 */

package org.datalift.samples.project;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

import javassist.bytecode.Descriptor.Iterator;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.openrdf.model.URI;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.fuberlin.wiwiss.silk.Silk;

@Path("/" + HandleProjectModule.MODULE_NAME)
public class HandleProjectModule extends BaseInterconnectionModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    public final static String MODULE_NAME = "silk";
    
    public String place=null;
	
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
    
    @GET
    @Path("build-silkscript")
    @Produces(MediaType.TEXT_HTML)
    public Response getIndexPage2(@QueryParam("project") java.net.URI projectId) 
    		                                            throws ObjectStreamException
    {
        // Retrieve project.
        Project p2 = this.getProject(projectId);		
        // Display conversion configuration page.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p2);
        args.put("linking", this);
        return Response.ok(this.newViewable("/script-webpage.vm", args))
                       .build();
    }
    
    @POST
    @Path("build-silkscript/fillin")
    @Produces(MediaTypes.TEXT_HTML)
    public Response doCreate(@QueryParam("project") java.net.URI projectId,
    	            	@FormParam("sourcename") String source_name,
    		            @FormParam("prefixsource") String prefix_source,
    		            @FormParam("targetname") String target_name,
    		            @FormParam("prefixtarget") String prefix_target,    		           
    		            @FormParam("sourcetype") String source_type,
    		            @FormParam("sourceaddressf") String source_address_file,
    		            @FormParam("sourceaddresss") String source_address_sparql,
    		            @FormParam("sourceaddressso") String source_address_sparql_other,
    		            @FormParam("targettype") String target_type,
    		            @FormParam("targetaddressf") String target_address_file,
    		            @FormParam("targetaddresss") String target_address_sparql,
    		            @FormParam("targetaddressso") String target_address_sparql_other,
    		            @FormParam("interlinkid") String interlink_id,   		            
    		            @FormParam("sourcequery") String source_query,
    		            @FormParam("variable1") String v1,
    		            @FormParam("targetquery") String target_query,
    		            @FormParam("variable2") String v2,
    		            @FormParam("aggregatetype") String aggregate_type,
    		            @FormParam("metric0") String metric0,    	
    		            @FormParam("threshold0") float threshold0,
    		            @FormParam("required0") boolean required0,
    		            @FormParam("weight0") int weight0,
    		            @FormParam("minValue0") float minValue0,
    		            @FormParam("maxValue0") float maxValue0,
    		            @FormParam("unit0") String unit0,
    		            @FormParam("curveStyle0") String curveStyle0,
    		            @FormParam("sourceproperty0") String source_property0,
    		            @FormParam("transformInputs0") String transformInput_s0,
    		            @FormParam("functions0") String function_s0,
    		            @FormParam("allWordss0") boolean allWords_s0,
    		            @FormParam("searchs0") String search_s0,
    		            @FormParam("replaces10") String replace_s1_0,
    		            @FormParam("regexs10") String regex_s1_0,
    		            @FormParam("replaces20") String replace_s2_0,
    		            @FormParam("bases0") int base_s0,
    		            @FormParam("sourceCharsets0") String sourceCharset_s0,
    		            @FormParam("targetCharsets0") String targetCharset_s0,
    		            @FormParam("regexs20") String regex_s2_0,
    		            @FormParam("blacklists0") String blacklist_s0,    		            
    		            @FormParam("targetproperty0") String target_property0,
    		            @FormParam("transformInputt0") String transformInput_t0,
    		            @FormParam("functiont0") String function_t0,
    		            @FormParam("allWords_t0") boolean allWords_t0,
    		            @FormParam("search_t0") String search_t0,
    		            @FormParam("replace_t10") String replace_t1_0,
    		            @FormParam("regext10") String regex_t1_0,
    		            @FormParam("replacet20") String replace_t2_0,
    		            @FormParam("baset0") int base_t0,
    		            @FormParam("sourceCharsett0") String sourceCharset_t0,
    		            @FormParam("targetCharsett0") String targetCharset_t0,
    		            @FormParam("regext20") String regex_t2_0,
    		            @FormParam("blacklistt0") String blacklist_t0,  		                		        
    		            @FormParam("filterlimit") String filter_limit)
                        		throws IOException
    {    	    	  	    	
    	    //set the default value for each variables
    	    required0 = false;
    	    filter_limit=null;
    	
            //create the silk script
        	int i=0;
            File script = new File("script.xml");
        	//File script = new File("C://Zhengjie//study//datalift_6.13//configFile.xml");
        	PrintStream out = null;
    		try {
    			out = new PrintStream(new FileOutputStream(script));
    		} catch (FileNotFoundException e) {
    			// TODO Auto-generated catch block
    			e.printStackTrace();
    		}
        	System.setOut(out);
        	
        	System.out.println("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
        	System.out.println("<Silk>");
        	System.out.println("<Prefixes>");
        	System.out.println("<Prefix id=\"rdf\" namespace=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" />");
        	System.out.println("<Prefix id=\"rdfs\" namespace=\"http://www.w3.org/2000/01/rdf-schema#\" />");
        	System.out.println("<Prefix id=\"xsd\" namespace=\"http://www.w3.org/2001/XMLSchema#\" />");
        	System.out.println("<Prefix id=\"dc\" namespace=\"http://purl.org/dc/elements/1.1/\" />");
        	System.out.println("<Prefix id=\"cc\" namespace=\"http://creativecommons.org/ns#\" />");
        	System.out.println("<Prefix id=\"owl\" namespace=\"http://www.w3.org/2002/07/owl#\"/>");
        	System.out.println("<Prefix id=\"dcterms\" namespace=\"http://purl.org/dc/terms/\" />");
        	System.out.println("<Prefix id=\"xmlns\" namespace=\"http://ec.europa.eu/eurostat/ramon/ontologies/geographic.rdf#\" />");
        	System.out.println("<Prefix id=\""+ source_name +"\" namespace=\""+ prefix_source +"\" />");
        	System.out.println("<Prefix id=\""+ target_name +"\" namespace=\""+ prefix_target +"\" />");
        	System.out.println("</Prefixes>");
        	System.out.println("\n");
        	System.out.println("<DataSources>");
        	//the datasets should specify the sparqlendpoint or file format
        	if (source_type.equals("file"))
        	{
            	System.out.println("<DataSource id=\""+ source_name +"\" type=\"file\">");
            	System.out.println("<Param name=\"file\" value=\""+ source_address_file +"\"/>");
            	System.out.println("<Param name=\"format\" value=\"RDF/XML\"/>");
            	System.out.println("</DataSource>");
        	}
        	else if (source_type.equals("sparqlEndpoint"))
        	{
            	System.out.println("<DataSource id=\""+ source_name +"\" type=\"sparqlEndpoint\">");
            	if (source_address_sparql.equals("other"))
            	    System.out.println("<Param name=\"endpointURI\" value=\""+ source_address_sparql_other +"\"/>");
            	else 
            		System.out.println("<Param name=\"endpointURI\" value=\""+ source_address_sparql +"\"/>");
            	//System.out.println("<Param name=\"graph\" value=\""+ source_graph_value +"\"/>");
            	System.out.println("</DataSource>");
        	}
        	System.out.println("\n");
        	if (target_type.equals("file"))
        	{
            	System.out.println("<DataSource id=\""+ target_name +"\" type=\"file\">");
            	System.out.println("<Param name=\"file\" value=\""+ target_address_file +"\"/>");
            	System.out.println("<Param name=\"format\" value=\"RDF/XML\"/>");
            	System.out.println("</DataSource>");
        	}
        	else if (target_type.equals("sparqlEndpoint"))
        	{
            	System.out.println("<DataSource id=\""+ target_name +"\" type=\"sparqlEndpoint\">");
            	if (target_address_sparql.equals("other"))
            	    System.out.println("<Param name=\"endpointURI\" value=\""+ target_address_sparql_other +"\"/>");
            	else 
            		System.out.println("<Param name=\"endpointURI\" value=\""+ target_address_sparql +"\"/>");
            	//System.out.println("<Param name=\"graph\" value=\""+ target_graph_value +"\"/>");
            	System.out.println("</DataSource>");
        	}
        	System.out.println("</DataSources>");
        	System.out.println("\n");
        	System.out.println("<Interlinks>");
        	//if there are several interlink
        	//for (i=0; i<num_interlink; i++)
        	//{
    	    	System.out.println("<Interlink id=\""+ interlink_id +"\">");
    	    	System.out.println("<LinkType>owl:sameAs</LinkType>");
    	    	System.out.println("\n");
    	    	System.out.println("<SourceDataset dataSource=\""+ source_name +"\" var=\""+v1+"\">");
    	    	System.out.println("<RestrictTo>");
    	    	System.out.println(source_query);
    	    	System.out.println("</RestrictTo>");
    	    	System.out.println("</SourceDataset>");
    	    	System.out.println("\n");
    	    	System.out.println("<TargetDataset dataSource=\""+ target_name +"\" var=\""+v2+"\">");
    	    	System.out.println("<RestrictTo>");	    
    	    	System.out.println(target_query);
    		    System.out.println("</RestrictTo>");
    		    System.out.println("</TargetDataset>");
    		    System.out.println("\n");
    		    System.out.println("<LinkageRule>");
    		    System.out.println("<Aggregate type=\""+ aggregate_type +"\">");
				
    		    System.out.println("<Compare metric=\""+ metric0 +"\" threshold=\""+threshold0+"\" required=\""+required0+"\">");		    
    		    if (transformInput_s0.equals("Yes"))
    		    {
        		    System.out.println("<TransformInput function=\""+function_s0+"\">");
    		    }
    		    System.out.println("<Input path=\"?"+v1+"/"+ source_property0 +"\" />");
    		    if (transformInput_s0.equals("Yes"))
    		    {
    		    	if (function_s0.equals("capitalize"))
    		    		System.out.println("<Param name=\"allWords\" value=\""+allWords_s0+"\"/>");
    		    	if (function_s0.equals("replace"))
    		    	{
    		    		System.out.println("<Param name=\"search\" value=\""+search_s0+"\"/>");
    		    		System.out.println("<Param name=\"replace\" value=\""+replace_s1_0+"\"/>");
    		    	}   		    		
    		    	if (function_s0.equals("regexReplace"))
    		    	{
    		    		System.out.println("<Param name=\"regex\" value=\""+regex_s1_0+"\"/>");
    		    		System.out.println("<Param name=\"replace\" value=\""+replace_s2_0+"\"/>");
    		    	} 
    		    	if (function_s0.equals("logarithm"))
    		    		System.out.println("<Param name=\"base\" value=\""+base_s0+"\"/>");
    		    	if (function_s0.equals("convert"))
        		    	{
        		    		System.out.println("<Param name=\"sourceCharset\" value=\""+sourceCharset_s0+"\"/>");
        		    		System.out.println("<Param name=\"targetCharset\" value=\""+targetCharset_s0+"\"/>");
        		    	}
        		    if (function_s0.equals("tokenize"))
        		    	System.out.println("<Param name=\"regex\" value=\""+regex_s2_0+"\"/>");
        		    if (function_s0.equals("removeValues"))
        		    	System.out.println("<Param name=\"blacklist\" value=\""+blacklist_s0+"\"/>");
        		    System.out.println("</TransformInput>");
    		    }
    		    if (transformInput_t0.equals("Yes"))
    		    {
        		    System.out.println("<TransformInput function=\""+function_t0+"\">");
    		    }
    		    System.out.println("<Input path=\"?"+v2+"/"+ target_property0 +"\" />");
    		    if (transformInput_t0.equals("Yes"))
    		    {
    		    	if (function_t0.equals("capitalize"))
    		    		System.out.println("<Param name=\"allWords\" value=\""+allWords_t0+"\"/>");
    		    	if (function_t0.equals("replace"))
    		    	{
    		    		System.out.println("<Param name=\"search\" value=\""+search_t0+"\"/>");
    		    		System.out.println("<Param name=\"replace\" value=\""+replace_t1_0+"\"/>");
    		    	}   		    		
    		    	if (function_t0.equals("regexReplace"))
    		    	{
    		    		System.out.println("<Param name=\"regex\" value=\""+regex_t1_0+"\"/>");
    		    		System.out.println("<Param name=\"replace\" value=\""+replace_t2_0+"\"/>");
    		    	} 
    		    	if (function_t0.equals("logarithm"))
    		    		System.out.println("<Param name=\"base\" value=\""+base_t0+"\"/>");
    		    	if (function_t0.equals("convert"))
        		    	{
        		    		System.out.println("<Param name=\"sourceCharset\" value=\""+sourceCharset_t0+"\"/>");
        		    		System.out.println("<Param name=\"targetCharset\" value=\""+targetCharset_t0+"\"/>");
        		    	}
        		    if (function_t0.equals("tokenize"))
        		    	System.out.println("<Param name=\"regex\" value=\""+regex_t2_0+"\"/>");
        		    if (function_t0.equals("removeValues"))
        		    	System.out.println("<Param name=\"blacklist\" value=\""+blacklist_t0+"\"/>");
        		    System.out.println("</TransformInput>");
    		    }  		    
    		    if (metric0.equals("num"))  	
    		    {
    		    	System.out.println("<Param name=\"minValue\" value=\""+minValue0+"\"/>");
    		    	System.out.println("<Param name=\"maxValue\" value=\""+maxValue0+"\"/>");
    		    }    		    	
    		    if (metric0.equals("wgs84"))  	
    		    {
    		    	System.out.println("<Param name=\"unit\" value=\""+unit0+"\"/>"); 
    		    	System.out.println("<Param name=\"unit\" value=\""+curveStyle0+"\"/>"); 
    		    }   		    			    		    
    		    System.out.println("</Compare>");    		    				
				System.out.println("</Aggregate>");
    	    	System.out.println("</LinkageRule>");
    	    	System.out.println("\n");
    	    	if (filter_limit!=null)
        		    System.out.println("<Filter limit=\""+ filter_limit +"\"/>");
    	    	else
    	    		System.out.println("<Filter />");
        		System.out.println("\n");
        		System.out.println("<Outputs>");
        		System.out.println("<Output type=\"sparul\" >");
        		System.out.println("<Param name=\"uri\" value=\"http://localhost:8080/openrdf-sesame/repositories/lifted/statements\"/>");
        		System.out.println("<Param name=\"parameter\" value=\"update\"/>");
        		System.out.println("</Output>");
    	    	System.out.println("</Outputs>");
    	    	System.out.println("</Interlink>");
        	//}
        	//end of the script
        	System.out.println("</Interlinks>");
        	System.out.println("</Silk>");
    	System.out.close();
    	
    	place = System.getProperty("user.dir")+"/script.xml";
        // Retrieve project.
        Project p3 = this.getProject(projectId);		
        // Display conversion configuration page.  	
    	Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p3);
        args.put("linking", this);  
    	return Response.ok(this.newViewable("/silk-webpage_2.vm", args))
                .build();
        
    }
 
    @POST
    @Path("run-silk")
    @Consumes(MediaTypes.MULTIPART_FORM_DATA)
    @Produces(MediaTypes.TEXT_HTML)
    public Response doRun(@QueryParam("project") java.net.URI projectId,
                        @FormDataParam("configFile") InputStream data,
                        @FormDataParam("configFile") FormDataContentDisposition disposition,
                        @FormDataParam("linkSpecId") String linkSpecId,
                        @FormDataParam("numThreads") int numThreads,
                        @FormDataParam("reload") boolean reload)
                                        throws ObjectStreamException
    {    	   	
        // Retrieve project.
        Project p4 = this.getProject(projectId);		
        // Display conversion configuration page.  	
    	Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p4);
        args.put("linking", this);
    	
        String filename = disposition.getFileName();
        try {
			FileOutputStream fos = null;
	        BufferedInputStream bis = null;
	        int BUFFER_SIZE = 1024;
	        byte[] buf = new byte[BUFFER_SIZE];
	        int size = 0;
	        bis = new BufferedInputStream(data);
	        try {
	        	    File configFile = null;
					try {
						configFile = File.createTempFile("configFile",".xml");
						fos = new FileOutputStream(configFile);
					} catch (IOException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} finally {
	                
	                try {
	                                while ( (size = bis.read(buf)) != -1)
	                                    fos.write(buf, 0, size);
	                                fos.close();
	                                bis.close();
	                        } catch (IOException e) {
	                        	 // Includes FileNotFoundException
	                             log.fatal("File upload error for {}", e, configFile);
	                             throw new WebApplicationException(
	                                       Response.status(Status.INTERNAL_SERVER_ERROR)
	                                                .entity(e.getMessage())
	                                                .type(MediaType.TEXT_PLAIN).build());
	                        }
        	        Silk.executeFile(configFile, linkSpecId, numThreads, reload);
        	        configFile.deleteOnExit();
	                } 
					
		} finally {}      
		} finally {}
		
        return Response.ok(this.newViewable("/ok.vm", args)).build();
        
    }
    
    @POST
    @Path("run-silk_2")
    @Produces(MediaTypes.TEXT_HTML)
    public Response doRun(@QueryParam("project") java.net.URI projectId,
                        @FormParam("linkSpecId") String linkSpecId,
                        @FormParam("numThreads") int numThreads,
                        @FormParam("reload") boolean reload)
                        		throws ObjectStreamException
    {    	
        // Retrieve project.
        Project p5 = this.getProject(projectId);		
        // Display conversion configuration page.  	
    	Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p5);
        args.put("linking", this);
        File configFile = null;
		//link the data sets
        if (place!=null)
    	    configFile = new File(place);
    	Silk.executeFile(configFile, linkSpecId, numThreads, reload);
        
    	return Response.ok(this.newViewable("/ok.vm", args)).build();
    }
}

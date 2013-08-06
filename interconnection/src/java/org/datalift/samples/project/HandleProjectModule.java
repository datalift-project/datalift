/*
 * Copyright / INRIA 2011-2012
 * Contributor(s) : Z. Fan, J. Euzenat, F. Scharffe
 *
 * Contact: zhengjie.fan@inria.fr
 */

package org.datalift.samples.project;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectStreamException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
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
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.StringUtils;

import org.semanticweb.owl.align.AlignmentException;

import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import de.fuberlin.wiwiss.silk.Silk;
import fr.inrialpes.exmo.align.impl.renderer.CopyOfSilkRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;


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
                        		throws IOException {
        //set the default value for each variables
        // required0 = false;
        // filter_limit=null;
        if (StringUtils.isBlank(filter_limit)) {
            filter_limit = null;
        }
        //SPARQL endpoint URI
        Repository r = Configuration.getDefault().getDataRepository();
        String url = r.getEndpointUrl();
        source_address_sparql = url.substring(0,21);
        target_address_sparql = url.substring(0,21);
        //create the silk script
        File script = new File("script.xml");
        PrintStream out = null;
        try {
                out = new PrintStream(new FileOutputStream(script));
        	out.println("<?xml version=\"1.0\" encoding=\"utf-8\" ?>");
        	out.println("<Silk>");
        	out.println("<Prefixes>");
        	out.println("<Prefix id=\"rdf\" namespace=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" />");
        	out.println("<Prefix id=\"rdfs\" namespace=\"http://www.w3.org/2000/01/rdf-schema#\" />");
        	out.println("<Prefix id=\"xsd\" namespace=\"http://www.w3.org/2001/XMLSchema#\" />");
        	out.println("<Prefix id=\"dc\" namespace=\"http://purl.org/dc/elements/1.1/\" />");
        	out.println("<Prefix id=\"cc\" namespace=\"http://creativecommons.org/ns#\" />");
        	out.println("<Prefix id=\"owl\" namespace=\"http://www.w3.org/2002/07/owl#\"/>");
        	out.println("<Prefix id=\"dcterms\" namespace=\"http://purl.org/dc/terms/\" />");
        	out.println("<Prefix id=\"xmlns\" namespace=\"http://ec.europa.eu/eurostat/ramon/ontologies/geographic.rdf#\" />");
        	out.println("<Prefix id=\""+ source_name +"\" namespace=\""+ prefix_source +"\" />");
        	out.println("<Prefix id=\""+ target_name +"\" namespace=\""+ prefix_target +"\" />");
        	out.println("</Prefixes>");
        	out.println("\n");
        	out.println("<DataSources>");
        	//the datasets should specify the sparqlendpoint or file format
        	if (source_type.equals("file")) {
            	    out.println("<DataSource id=\""+ source_name +"\" type=\"file\">");
            	    out.println("<Param name=\"file\" value=\""+ source_address_file +"\"/>");
            	    out.println("<Param name=\"format\" value=\"RDF/XML\"/>");
            	    out.println("</DataSource>");
        	}
        	else if (source_type.equals("sparqlEndpoint")) {
            	    out.println("<DataSource id=\""+ source_name +"\" type=\"sparqlEndpoint\">");
            	    if (source_address_sparql.equals("other")) {
            	        out.println("<Param name=\"endpointURI\" value=\""+ source_address_sparql_other +"\"/>");
            	    }
            	    else {
            		out.println("<Param name=\"endpointURI\" value=\""+ source_address_sparql +"/datalift/sparql\"/>");
            	    }
            	    //out.println("<Param name=\"graph\" value=\""+ source_graph_value +"\"/>");
            	    out.println("</DataSource>");
        	}
        	out.println("\n");
        	if (target_type.equals("file")) {
            	    out.println("<DataSource id=\""+ target_name +"\" type=\"file\">");
            	    out.println("<Param name=\"file\" value=\""+ target_address_file +"\"/>");
            	    out.println("<Param name=\"format\" value=\"RDF/XML\"/>");
            	    out.println("</DataSource>");
        	}
        	else if (target_type.equals("sparqlEndpoint")) {
            	    out.println("<DataSource id=\""+ target_name +"\" type=\"sparqlEndpoint\">");
            	    if (target_address_sparql.equals("other")) {
            	        out.println("<Param name=\"endpointURI\" value=\""+ target_address_sparql_other +"\"/>");
            	    }
            	    else {
            		out.println("<Param name=\"endpointURI\" value=\""+ target_address_sparql +"/datalift/sparql\"/>");
            	    }
            	    //out.println("<Param name=\"graph\" value=\""+ target_graph_value +"\"/>");
            	    out.println("</DataSource>");
        	}
        	out.println("</DataSources>");
        	out.println("\n");
        	out.println("<Interlinks>");
        	//if there are several interlink
        	//for (i=0; i<num_interlink; i++)
        	//{
        	out.println("<Interlink id=\""+ interlink_id +"\">");
        	out.println("<LinkType>owl:sameAs</LinkType>");
        	out.println("\n");
        	out.println("<SourceDataset dataSource=\""+ source_name +"\" var=\""+v1+"\">");
        	out.println("<RestrictTo>");
        	out.println(source_query);
        	out.println("</RestrictTo>");
        	out.println("</SourceDataset>");
        	out.println("\n");
        	out.println("<TargetDataset dataSource=\""+ target_name +"\" var=\""+v2+"\">");
        	out.println("<RestrictTo>");	    
        	out.println(target_query);
        	out.println("</RestrictTo>");
        	out.println("</TargetDataset>");
        	out.println("\n");
        	out.println("<LinkageRule>");
        	out.println("<Aggregate type=\""+ aggregate_type +"\">");
				
        	out.println("<Compare metric=\""+ metric0 +"\" threshold=\""+threshold0+"\" required=\""+required0+"\">");		    
        	if (transformInput_s0.equals("Yes")) {
        	    out.println("<TransformInput function=\""+function_s0+"\">");
        	}
        	out.println("<Input path=\"?"+v1+"/"+ source_property0 +"\" />");
        	if (transformInput_s0.equals("Yes")) {
    		    	if (function_s0.equals("capitalize")) {
    		    		out.println("<Param name=\"allWords\" value=\""+allWords_s0+"\"/>");
    		    	}
    		    	if (function_s0.equals("replace")) {
    		    		out.println("<Param name=\"search\" value=\""+search_s0+"\"/>");
    		    		out.println("<Param name=\"replace\" value=\""+replace_s1_0+"\"/>");
    		    	}   		    		
    		    	if (function_s0.equals("regexReplace")) {
    		    		out.println("<Param name=\"regex\" value=\""+regex_s1_0+"\"/>");
    		    		out.println("<Param name=\"replace\" value=\""+replace_s2_0+"\"/>");
    		    	} 
    		    	if (function_s0.equals("logarithm")) {
    		    		out.println("<Param name=\"base\" value=\""+base_s0+"\"/>");
    		    	}
    		    	if (function_s0.equals("convert")) {
        			out.println("<Param name=\"sourceCharset\" value=\""+sourceCharset_s0+"\"/>");
        			out.println("<Param name=\"targetCharset\" value=\""+targetCharset_s0+"\"/>");
        		}
        		if (function_s0.equals("tokenize")) {
        			out.println("<Param name=\"regex\" value=\""+regex_s2_0+"\"/>");
        		}
        		if (function_s0.equals("removeValues")) {
        			out.println("<Param name=\"blacklist\" value=\""+blacklist_s0+"\"/>");
        		}
        		out.println("</TransformInput>");
        	}
        	if (transformInput_t0.equals("Yes")) {
        	    out.println("<TransformInput function=\""+function_t0+"\">");
        	}
        	out.println("<Input path=\"?"+v2+"/"+ target_property0 +"\" />");
        	if (transformInput_t0.equals("Yes")) {
    		    	if (function_t0.equals("capitalize")) {
    		    		out.println("<Param name=\"allWords\" value=\""+allWords_t0+"\"/>");
    		    	}
    		    	if (function_t0.equals("replace")) {
    		    		out.println("<Param name=\"search\" value=\""+search_t0+"\"/>");
    		    		out.println("<Param name=\"replace\" value=\""+replace_t1_0+"\"/>");
    		    	}   		    		
    		    	if (function_t0.equals("regexReplace")) {
    		    		out.println("<Param name=\"regex\" value=\""+regex_t1_0+"\"/>");
    		    		out.println("<Param name=\"replace\" value=\""+replace_t2_0+"\"/>");
    		    	} 
    		    	if (function_t0.equals("logarithm")) {
    		    		out.println("<Param name=\"base\" value=\""+base_t0+"\"/>");
    		    	}
    		    	if (function_t0.equals("convert")) {
        			out.println("<Param name=\"sourceCharset\" value=\""+sourceCharset_t0+"\"/>");
        			out.println("<Param name=\"targetCharset\" value=\""+targetCharset_t0+"\"/>");
        		}
        		if (function_t0.equals("tokenize")) {
        		    	out.println("<Param name=\"regex\" value=\""+regex_t2_0+"\"/>");
        		}
        		if (function_t0.equals("removeValues")) {
        		    	out.println("<Param name=\"blacklist\" value=\""+blacklist_t0+"\"/>");
        		}
        		out.println("</TransformInput>");
        	}
        	if (metric0.equals("num")) {
        	    out.println("<Param name=\"minValue\" value=\""+minValue0+"\"/>");
        	    out.println("<Param name=\"maxValue\" value=\""+maxValue0+"\"/>");
        	}    		    	
        	if (metric0.equals("wgs84")) {
        	    out.println("<Param name=\"unit\" value=\""+unit0+"\"/>"); 
        	    out.println("<Param name=\"unit\" value=\""+curveStyle0+"\"/>"); 
        	}   		    			    		    
        	out.println("</Compare>");
        	out.println("</Aggregate>");
        	out.println("</LinkageRule>");
        	out.println("\n");
        	if (filter_limit!=null) {
        	    out.println("<Filter limit=\""+ filter_limit +"\"/>");
        	}
        	else {
        	    out.println("<Filter />");
        	}
        	out.println("\n");
        	out.println("<Outputs>");
        	out.println("<Output type=\"sparul\" >");
        	out.println("<Param name=\"uri\" value=\"" + url + "/statements\"/>");	
        	out.println("<Param name=\"parameter\" value=\"update\"/>");
        	out.println("</Output>");
        	out.println("</Outputs>");
        	out.println("</Interlink>");
        	//}
        	//end of the script
        	out.println("</Interlinks>");
        	out.println("</Silk>");
        	out.flush();
        	out.close();
        	out = null;
        }
        finally {
            if (out != null) {
                try { out.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
    	place = System.getProperty("user.dir")+"/script.xml";
        // Retrieve project.
        Project p3 = this.getProject(projectId);		
        // Display conversion configuration page.  	
    	Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p3);
        args.put("linking", this);  
    	return Response.ok(this.newViewable("/silk-webpage.vm", args))
                .build();        
    }
 
    @POST
    @Path("run-silk")
    @Consumes(MediaTypes.MULTIPART_FORM_DATA)
    @Produces(MediaTypes.TEXT_HTML)
    public Response doRun(@QueryParam("project") java.net.URI projectId,
                        @FormDataParam("silkScript") InputStream data,
                        @FormDataParam("silkScript") FormDataContentDisposition disposition)
                                        throws ObjectStreamException
    {    	   	
        // Retrieve project.
        Project p4 = this.getProject(projectId);		
        // Display conversion configuration page.  	
    	Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p4);
        args.put("linking", this);
    	
	//link the data sets                
        File silkScript = null;
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
            bis = new BufferedInputStream(data);
            silkScript = File.createTempFile("silkScript",".xml");
            fos = new FileOutputStream(silkScript);
            byte[] buf = new byte[1024];
            int size = 0;
            while ( (size = bis.read(buf)) != -1) {
                fos.write(buf, 0, size);
            }
            fos.close();
            fos = null;
            bis.close();
            bis = null;

            Silk.executeFile(silkScript, null, 1, true);
	}
        catch (IOException e) {
            log.fatal("File upload error for {}", e, silkScript);
            throw new WebApplicationException(
                            Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .entity(e.getMessage())
                                    .type(MediaType.TEXT_PLAIN).build());
        }
	finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception e) { /* Ignore... */ }
            }
            if (bis != null) {
                try { bis.close(); } catch (Exception e) { /* Ignore... */ }
            }
	    if (silkScript != null) {
                silkScript.delete();
	    }
	}
        return Response.ok(this.newViewable("/ok.vm", args)).build();
        
    }
    
    @POST
    @Path("run-edoal")
    @Consumes(MediaTypes.MULTIPART_FORM_DATA)
    @Produces(MediaTypes.TEXT_HTML)
    public Response doRun_edoal(@QueryParam("project") java.net.URI projectId,
                        @FormDataParam("EDOALFile") InputStream data,
                        @FormDataParam("EDOALFile") FormDataContentDisposition disposition,
                        @FormDataParam("sourcedataset") String sourcedataset,
                        @FormDataParam("targetdataset") String targetdataset)
                                        throws ObjectStreamException {
        // Retrieve project.
        Project p4 = this.getProject(projectId);		
        // Display conversion configuration page.  	
    	Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p4);
        args.put("linking", this);
    	
        FileOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
	        bis = new BufferedInputStream(data);
	        File configFile = File.createTempFile("configFile",".xml");
	        configFile.deleteOnExit();
	        fos = new FileOutputStream(configFile);
	        byte[] buf = new byte[1024];
	        int size = 0;
	        while ( (size = bis.read(buf)) != -1) {
	            fos.write(buf, 0, size);
	        }
	        fos.close();
	        fos = null;
	        bis.close();
	        bis = null;

	                if (data!=null)
	                {
	                	//create SILK script
	                	AlignmentParser aparser = new AlignmentParser(0);
	                	URI file = configFile.toURI();
	                	
	                	//create Hashtable for init
	                	Properties params = new Properties();
	                	if (!sourcedataset.equals(""))
	                		params.setProperty( "source", sourcedataset);
	                	if (!targetdataset.equals(""))
	                		params.setProperty( "target", targetdataset);
	                	
	        			try {
							aparser.parse(file);
							PrintWriter writer = new PrintWriter(
										 new BufferedWriter(
								         new OutputStreamWriter( System.out, "UTF-8" )), true);
							
							CopyOfSilkRendererVisitor renderer = new CopyOfSilkRendererVisitor(writer);
		        				renderer.run(params, "SILKscript.xml", file.toString());
		                    writer.flush();
		                    writer.close();

		                    //Run SILK script
		                    File fspec = new File(System.getProperty("user.dir")+"\\SILKscript.xml");		                    
                  
		                    //replace the sparql endpoint of target data set:
		                    BufferedReader br = null;  
		                    BufferedWriter bw = null; 
		                    String line = null;  
		                    StringBuffer buff = new StringBuffer();  		                     
		                    try {  
		                        // create input stream buffer
		                        br = new BufferedReader(new FileReader(fspec));		                        
		                        // read each line, and put into the buffer	                        
		                        while ((line = br.readLine()) != null) {  
		                            // revise the content
		                        	if (line.contains("<Outputs>")) {   
		                        			line = br.readLine();
		                            		buff.append("			<Outputs>");
	                                    	buff.append(System.getProperty("line.separator"));
		                                    line = br.readLine();
			                            	buff.append("				<Output type=\"sparul\">");
		                                    buff.append(System.getProperty("line.separator"));
		                                    line = br.readLine();
			                            	buff.append("					<Param name=\"uri\" value=\"http://localhost:8080/openrdf-sesame/repositories/lifted/statements\"/>");
			                            	buff.append(System.getProperty("line.separator"));
		                                    line = br.readLine();
			                            	buff.append("					<Param name=\"parameter\" value=\"update\"/>");
			                            	buff.append(System.getProperty("line.separator"));
			                            	line = br.readLine();
		                                    buff.append("				</Output>");
		                                    buff.append(System.getProperty("line.separator"));
		                                    line = br.readLine();
		                                    line = br.readLine();
		                                    line = br.readLine();
		                        		}		                        	
		                            // if it don't need to be revised, copy as the same
		                            else {  
		                                buff.append(line);
		                                buff.append(System.getProperty("line.separator"));
		                            }		                            
		                        }
		                    } finally {  
		                        if (br != null) {  
		                            try { br.close(); } catch (Exception e) { /* Ignore... */ }  
		                        }  
		                    } 
		                    try {  
		                        bw = new BufferedWriter(new FileWriter(fspec));  
		                        bw.write(buff.toString());  
		                    } finally {  
		                        if (bw != null) {  
		                            try { bw.close(); } catch (Exception e) { /* Ignore... */ }
		                        }  
		                    }
		                    
		                    Silk.executeFile(fspec, null, 1, true);
		                    
						} catch (AlignmentException e) {
							log.fatal("Cannot create EDOAL file", e);
							throw new IOException("Cannot create EDOAL file", e);
						}	        			
	                }
	}
        catch (Exception e) {
            log.fatal("Processing error for {}", e, disposition.getFileName());
            throw new WebApplicationException(
                            Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .entity(e.getMessage())
                                    .type(MediaType.TEXT_PLAIN).build());
        }
	finally {
	    if (fos != null) {
	        try { fos.close(); } catch (Exception e) { /* Ignore... */ }
	    }
	    if (bis != null) {
	        try { bis.close(); } catch (Exception e) { /* Ignore... */ }
	    }
	}
        return Response.ok(this.newViewable("/ok.vm", args)).build();
        
    }
    
    @POST
    @Path("run-silk_2")
    @Produces(MediaTypes.TEXT_HTML)
    public Response doRun(@QueryParam("project") java.net.URI projectId)
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
        if (configFile.length()>100)
        {
        	Silk.executeFile(configFile, null, 1, true); 
        	configFile.delete();
        }    		
        
    	return Response.ok(this.newViewable("/ok.vm", args)).build();
    }
}

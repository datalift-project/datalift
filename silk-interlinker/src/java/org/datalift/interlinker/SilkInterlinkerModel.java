package org.datalift.interlinker;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

//import org.apache.commons.io.IOUtils;
import org.codehaus.jackson.JsonEncoding;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.Update;
import org.openrdf.query.UpdateExecutionException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.fuberlin.wiwiss.silk.Silk;
//import fr.inrialpes.exmo.align.impl.renderer.CopyOfSilkRendererVisitor;

public class SilkInterlinkerModel extends Model{
	
	/** The default fallback max number of links created per RDF entity. */
    public static final int DEFAULT_MAX_LINKS = 0;

    /** The default fallback number of threads to use when executing Silk. */
    public static final int DEFAULT_NB_THREADS = 1;
    /** The default fallback way to manage cache to use when executing Silk. */
    public static final boolean DEFAULT_RELOAD_CACHE = true;
    
    //-------------------------------------------------------------------------
    // Sources management.
    //-------------------------------------------------------------------------
    
    /**
     * Checks if a given {@link Source} contains valid RDF-structured data.
     * @param src The source to check.
     * @return True if src is {@link TransformedRdfSource} or {@link SparqlSource}.
     */
    protected boolean isValidSource(Source src) {
    	//TODO Lower it further more ?
    	return src.getType().equals(SourceType.TransformedRdfSource) 
        	|| src.getType().equals(SourceType.SparqlSource);
    }
	
    public String getJsonSources(Project proj){
    	HashMap<String, String> mapSources = this.getSources(proj);
		Set<String> sourcesUrl = mapSources.keySet();
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
	    JsonFactory jsonFactory = new JsonFactory();
	    try{
			JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(outputStream, JsonEncoding.UTF8);
			jsonGenerator.writeStartArray();
			for(String url : sourcesUrl){		    	
				jsonGenerator.writeStartObject();
			    jsonGenerator.writeStringField("uri", url);
			    jsonGenerator.writeStringField("name", mapSources.get(url));
			    jsonGenerator.writeEndObject();
		    }
			jsonGenerator.writeEndArray();
			jsonGenerator.close();
			String jsonSources = outputStream.toString();
			return jsonSources;
		} catch (JsonGenerationException e) {
			throw new TechnicalException(e);
		} catch (IOException e) {
			throw new TechnicalException(e);
		}
	    
    }
    
    public String getSimpleJsonArray(List<String> array){
    	ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    	JsonFactory jsonFactory = new JsonFactory();
    	try {
    		JsonGenerator jsonGenerator = jsonFactory.createJsonGenerator(outputStream, JsonEncoding.UTF8);
			jsonGenerator.writeStartArray();
			for(String item: array){
				jsonGenerator.writeString(item);
			}
			jsonGenerator.writeEndArray();
			jsonGenerator.close();
			return outputStream.toString();
		} catch (JsonGenerationException e) {
			throw new TechnicalException(e);
		} catch (IOException e) {
			throw new TechnicalException(e);
		}

    }
    
    /**
     * Launches the Silk engine with a given configuration.
     * @param proj current project;
     * @param config the Silk configuration file.
     * @param linkID the identifier of our interlink.
     * @param threads number of threads to allocate to Silk.
     * @param reload tells if the Silk cache has to be reloaded before exec.
     * @param validateFile tells if we have to check if the parameters are usable.
     * @return A list which contains the newly created triples.
     */
    public final LinkedList<LinkedList<String>> launchSilk(Project proj, File config, String targetContext, String newSourceContext, String newSourceName, 
    		String linkID, int threads, boolean reload, boolean validateFile) {
    	LinkedList<LinkedList<String>> ret = new LinkedList<LinkedList<String>>();
    	if (!validateFile || getErrorMessages(config, linkID).isEmpty()) {
    		
    		LOG.info("Launching Silk on " + config.getAbsolutePath() + " - " + linkID);
	    	
			try {
					if(targetContext!=null){
						Source parent = proj.getSource(targetContext);
						addResultSource(proj, parent, newSourceName, new URI(newSourceContext));
						
						RepositoryConnection cnx = INTERNAL_REPO.newConnection();
						// Copy all of the data to the new graph.
						String updateQy = "COPY <" + targetContext + "> TO <" + newSourceContext + ">";
						Update up = cnx.prepareUpdate(QueryLanguage.SPARQL, updateQy);
						up.execute();
					}
			} 
			catch (IOException e) { LOG.fatal("Silk Configuration file execution failed - " + e); } 
			catch (URISyntaxException e) { LOG.fatal("Silk Configuration file execution failed - " + e); }
			catch (UpdateExecutionException e) { LOG.fatal("Silk Configuration file execution failed - " + e); } 
			catch (RepositoryException e) { LOG.fatal("Silk Configuration file execution failed - " + e); } 
			catch (MalformedQueryException e) { LOG.fatal("Silk Configuration file execution failed - " + e); }
	    	
			Silk.executeFile(config, linkID, threads, reload);
    	}else {
    		// Should never happen.
    		LinkedList<String> error = new LinkedList<String>();
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		ret.add(error);
    		
    		LOG.info("Silk interlinking KO.");
    	}
    	
    	return ret;
    }
    
    /**
     * Launches the Silk engine with a given configuration.
     * @param proj current project
     * @param targetContext the target url where to put the new triples
     * @param config the Silk configuration file.
     * @param the name of the new source
     * @param threads number of threads to allocate to Silk.
     * @param reload tells if the Silk cache has to be reloaded before exec.
     * @param validateFile tells if we have to check if the parameters are usable.
     * @return A list which contains the newly created triples.
     */
    public final LinkedList<LinkedList<String>> launchSilk(Project proj, File config,String targetContext, String newSourceContext, String newSourceName, int threads, boolean reload, boolean validateFile) {
    	// Here we'll extract the interlink identifier from the file.
    	String interlinkId = null;
    	try {
	    	DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
			Document doc = dBuilder.parse(config);
			doc.getDocumentElement().normalize();
			
			interlinkId = doc.getElementsByTagName("Interlink").item(0).getAttributes().getNamedItem("id").getTextContent();
    	}
    	catch (IOException e) {
			LOG.fatal("Silk Configuration file DOM parsing failed - " + e);
		} catch (SAXException e) {
			LOG.fatal("Silk Configuration file DOM parsing failed - " + e);
		} catch (ParserConfigurationException e) {
			LOG.fatal("Silk Configuration file DOM parsing failed - " + e);
		}
    	
    	return interlinkId != null ? launchSilk(proj, config,targetContext, newSourceContext, newSourceName,  interlinkId, 
    			threads, reload, validateFile) : null;
    }
    /**
     * Tries to find errors before launching the interlink process with an uploaded Silk script.
     * @param configFile The external Silk script.
     * @param linkSpecId The given interlink identifier to use.
     * @return A list of errors to be shown to the end user.
     */
    public final LinkedList<String> getErrorMessages(File configFile, String linkSpecId) {
		LinkedList<String> errors = new LinkedList<String>();
				
		if (configFile == null || !configFile.exists() || !configFile.canRead()) {
			errors.add(getTranslatedResource("error.filenotfound"));
		}
		else {
			try {
				DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
				DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
				Document doc = dBuilder.parse(configFile);
				doc.getDocumentElement().normalize();
				
				// Retrieve all the <Interlink> elements.
				NodeList interlinks = doc.getElementsByTagName("Interlink");
				if (interlinks.item(0) == null) {
					errors.add(getTranslatedResource("error.filenotvalid"));
				}
				else {
					String tmp;
					String availableIds = "";
					boolean found = false;
					int i = 0;
					while (i < interlinks.getLength() && !found) {
						tmp = ((Element) interlinks.item(i)).getAttribute("id").trim();
						found = linkSpecId.equals(tmp);
						availableIds += ", \"" + tmp + "\"";
						i++;
					}
					// Add a list of available identifiers to help the user.
					if (!found) {
						errors.add(getTranslatedResource("error.idnotfound") + " : \"" + linkSpecId + "\".");
						errors.add(getTranslatedResource("error.availableids") + " : " + availableIds.substring(1) + ".");
					}
				}
			}
			catch (IOException e) {
				LOG.fatal("Silk Configuration file DOM parsing failed - " + e);
			} catch (SAXException e) {
				LOG.fatal("Silk Configuration file DOM parsing failed - " + e);
			} catch (ParserConfigurationException e) {
				LOG.fatal("Silk Configuration file DOM parsing failed - " + e);
			}
		}
		
		return errors;
	}
    
    /**
     * Imports a configuration file from the data fetched by a form.
     * @param name Name of the temp file to write on disc.
     * @param data File content.
     * @return A new File which contains a Silk configuration.
     */
    public final File importConfigFile(String name, InputStream data) {
    	File ret = null;
    	
    	LOG.debug("Uploading new Silk config file - " + name);
	    
		try {
			ret = File.createTempFile(name, ".xml");
			ret.deleteOnExit();
			// To write on the new file.
			FileOutputStream fos = new FileOutputStream(ret);
			// To read from our form file data.
			BufferedInputStream bis = new BufferedInputStream(data);
			
			int size = 0;
			byte[] buf = new byte[1024];
			while ((size = bis.read(buf)) != -1) {
				fos.write(buf, 0, size);
			}
			
			fos.close();
			bis.close();
			
		} catch (IOException e) {
			LOG.fatal("Error while uploading Silk file - " + e);
		}
		
		return ret;
    }
    
    /*public final void convertEdoalScript(InputStream scriptStream, String edoalSource, String edoalTarget){
    	FileOutputStream fos = null;
        BufferedInputStream bis = null;
        try {
	        File edoalFile = File.createTempFile("uploaded_edoal",".xml");
	        edoalFile.deleteOnExit();
	        fos = new FileOutputStream(edoalFile);
	        //upload the edoal script and put it into a temporary file
	        IOUtils.copy(scriptStream, fos);
	        fos.close();
	        fos = null;
//	        if (scriptStream!=null){
	        	//create SILK script
	        	//AlignmentParser aparser = new AlignmentParser(0);
	          
	            //create Hashtable for init
	            Properties params = new Properties();
	            if (!edoalSource.isEmpty()) params.setProperty( "source", edoalSource);
	            if (!edoalTarget.isEmpty()) params.setProperty( "target", edoalTarget);
	     //   	try {
	        		//Alignment a = aparser.parse(edoalFile.toURI());
					PrintWriter writer = new PrintWriter(
							new BufferedWriter(
							new OutputStreamWriter( System.out, "UTF-8" )), true);
							
					CopyOfSilkRendererVisitor renderer = new CopyOfSilkRendererVisitor(writer);
					
					File tmpDir = new File(Configuration.getDefault().getPublicStorage().getFile("project"), "edoal");
		   			tmpDir.mkdirs();
					File convertedSilk = new File(tmpDir, "converted_silk_script.xml");
		        	renderer.run(params, convertedSilk.getAbsolutePath(), edoalFile.toURI().toString());
		        //	PropertyId prop = new PropertyId(edoalSource);
		        	
		            writer.flush();
		            writer.close();

		            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		        	DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		        	Document doc = dBuilder.parse(convertedSilk);
		        	NodeList outputNodeList = doc.getElementsByTagName("Outputs");
		        	
		        	Element outputNode = doc.createElement("Output");
		        	outputNode.setAttribute("type", "sparul");
		        	
		        	Element paramUri = doc.createElement("param");
		        	paramUri.setAttribute("name", "uri");
		        	String liftedPointUrl = Configuration.getDefault().getRepositories().iterator().next().getEndpointUrl();
		        	paramUri.setAttribute("value", liftedPointUrl);
		        	outputNode.appendChild(paramUri);
		        	
		        	Element paramParams = doc.createElement("param");
		        	paramParams.setAttribute("name", "parameter");
		        	paramParams.setAttribute("value", "update");
		        	outputNode.appendChild(paramParams);
		        	
		        	for(int i=0;i<outputNodeList.getLength();i++){
		        		outputNodeList.item(i).appendChild(outputNode);
		        	}
		        	
		        	TransformerFactory transformerFactory = TransformerFactory.newInstance();
		   			Transformer transformer = transformerFactory.newTransformer();
		   			DOMSource source = new DOMSource(doc);
		   			
		   			//save the updated script
		   			StreamResult result = new StreamResult(convertedSilk);
		   			transformer.transform(source, result);
		            //replace the sparql endpoint of target data set:
		           /* BufferedReader br = null;  
		            BufferedWriter bw = null; 
		            String line = null;  
		            StringBuffer buff = new StringBuffer();  		                     
		            try {  
		            	// create input stream buffer
		                br = new BufferedReader(new FileReader(convertedSilk));		                        
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
			                   	buff.append("					<Param name=\"uri\" value=\"http://localhost:9091/openrdf-sesame/repositories/lifted/statements\"/>");
			                    buff.append(System.getProperty("line.separator"));
		                        line = br.readLine();
			                    buff.append("					<Param name=\"parameter\" value=\"update\"/>");
			                    buff.append(System.getProperty("line.separator"));
			                    line = br.readLine();
		                        buff.append("				</Output>");
		                        buff.append(System.getProperty("line.separator"));
		                        buff.append("				</Outputs>\n");
		                    } else {  
		                        buff.append(line);
		                        buff.append(System.getProperty("line.separator"));
		                    }		                            
		                }
		                    } finally {  
		                        if (br != null) {  
		                            try { br.close(); } catch (Exception e) { /* Ignore... */ //}  
		                       // }  
		                    //} 
		                    /*try {  
		                        bw = new BufferedWriter(new FileWriter(convertedSilk));  
		                        bw.write(buff.toString());  
		                    } finally {  
		                        if (bw != null) {  
		                            try { bw.close(); } catch (Exception e) { /* Ignore... */ //}
		                      //  }  
		                    //}
		                    
		                  //  Silk.executeFile(convertedSilk, null, 1, true);
		                    
//						} catch (AlignmentException e) {
//							LOG.fatal("Cannot create EDOAL file", e);
//							throw new IOException("Cannot create EDOAL file", e);
//						}	        			
//	                }
/*	}
        catch (Exception e) {
            LOG.fatal("Processing error for {}", e);
            throw new WebApplicationException(
                            Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .entity(e.getMessage())
                                    .type(MediaType.TEXT_PLAIN).build());
        }
	//finally {
	  //  if (fos != null) {
	    //    try { fos.close(); } catch (Exception e) { /* Ignore... */ //}
	   // }
	    //if (bis != null) {
	      //  try { bis.close(); } catch (Exception e) { /* Ignore... */ }
	    //}
	//}
   // }
    
}



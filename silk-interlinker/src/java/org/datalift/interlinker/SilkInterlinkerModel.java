package org.datalift.interlinker;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;


import org.apache.commons.io.IOUtils;

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
import org.semanticweb.owl.align.Alignment;
import org.semanticweb.owl.align.AlignmentException;

import org.w3c.dom.Node;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


import de.fuberlin.wiwiss.silk.Silk;
import fr.inrialpes.exmo.align.impl.renderer.SILKRendererVisitor;
//import fr.inrialpes.exmo.align.impl.renderer.CopyOfSilkRendererVisitor;
import fr.inrialpes.exmo.align.parser.AlignmentParser;

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
    
    public final void convertEdoalScript(InputStream edoalStream, String source, String target, String measure, String thresold){
		LOG.debug("Convert and EDOAL File to a Silk Script and run it");
    	try {
			Random randomizer = new Random();
			int randomNameMax=10000;
			File edoalFile = File.createTempFile("uploaded_edoal"+ randomizer.nextInt(randomNameMax),".xml");
			edoalFile.deleteOnExit();
	        FileOutputStream edoalFos = new FileOutputStream(edoalFile);
	        //upload the edoal script and put it into a temporary file
	        IOUtils.copy(edoalStream, edoalFos);
	        LOG.debug("Copied an edoal uploaded file to {}", edoalFile.getAbsolutePath());
	        edoalFos.close();
	        
	        AlignmentParser alParser = new AlignmentParser(0);
			alParser.setEmbedded(false);
			Alignment aligner = alParser.parse(edoalFile.toURI());
			
			File silkScript = File.createTempFile("converted_silk" + randomizer.nextInt(randomNameMax), ".xml");
			silkScript.deleteOnExit();
			PrintWriter silkWriter = new PrintWriter(silkScript);
			SILKRendererVisitor visitor = new SILKRendererVisitor(silkWriter);
			Properties renderProp = new Properties();
			visitor.init(renderProp);
			
			visitor.visit(aligner);
			LOG.debug("Converted the edoal file {} to a silk Script in {}",edoalFile.getName(), silkScript.getAbsolutePath());
			silkWriter.close();
			
			//------------------------------------------------------------------------------------------------
			//NOW WE START TO EDIT THE XML FILE
			LOG.debug("The silk script will be now updated with the entered parameters.");
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
			Document silkDoc = docBuilder.parse(silkScript);
			
			//rename the main node
			silkDoc.renameNode(silkDoc.getFirstChild(), null, "Silk");
			
			//rename the source and target dataset:
			renameNodes(silkDoc,"SourceDataSet", "SourceDataset");
			
			renameNodes(silkDoc, "TargetDataSet", "TargetDataset");
			
			Element prefixesElement = (Element) silkDoc.getElementsByTagName("Prefixes").item(0); 
			//add the prefix:
			List<String> prefixNamespaces = getPrefixes(edoalFile);
			for(int i = 0;i<prefixNamespaces.size();i++){
				Element prefixElem = silkDoc.createElement("Prefix");
				prefixElem.setAttribute("id", "ns"+i);
				prefixElem.setAttribute("namespace", prefixNamespaces.get(i));
				prefixesElement.appendChild(prefixElem);
			}
			
			//add the right datasource:
			Node datasourcesNode = silkDoc.getElementsByTagName("DataSources").item(0);
			cleanNode(datasourcesNode);
			
			//add the proper nodes:
			addDataSource(datasourcesNode, silkDoc, "source", source);
			addDataSource(datasourcesNode, silkDoc, "target", target);
			
			//edit the comparison metrics and threshold
			NodeList comparisons = silkDoc.getElementsByTagName("Compare");
			for(int index = 0; index<comparisons.getLength(); index++){
				Element compElem = (Element) comparisons.item(index);
				Node metricNode = compElem.getAttributeNode("metric");
				metricNode.setNodeValue(measure);
				Node thresholdNode = compElem.getAttributeNode("threshold");
				thresholdNode.setNodeValue(thresold);
			}
			
			//change the first iterlink id:
			Element interlinkNode = (Element) silkDoc.getElementsByTagName("Interlink").item(0);
			interlinkNode.getAttributeNode("id").setNodeValue("silk-interlink");
			
			//change the output:
			NodeList outputsNodes = silkDoc.getElementsByTagName("Outputs");
			for(int i = 0;i<outputsNodes.getLength();i++){
				Node outputsNode = outputsNodes.item(i); 
				cleanNode(outputsNode);
				Element outputNode = silkDoc.createElement("Output");
				outputNode.setAttribute("type", "sparul");
				Element uriParam = silkDoc.createElement("Param");
				uriParam.setAttribute("name", "uri");
				uriParam.setAttribute("value", Configuration.getDefault().getInternalRepository().getUrl());
				
				Element paramParam = silkDoc.createElement("Param");
				paramParam.setAttribute("name", "parameter");
				paramParam.setAttribute("value", "update");
				
				outputNode.appendChild(uriParam);
				outputNode.appendChild(paramParam);
				((Element)outputsNode).appendChild(outputNode);
				
			}
			TransformerFactory transformerFactory = TransformerFactory.newInstance();
			Transformer transformer = transformerFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			DOMSource domSource = new DOMSource(silkDoc);
			File updatedSilkScript = File.createTempFile("updated_silk" + randomizer.nextInt(randomNameMax), ".xml");
			updatedSilkScript.deleteOnExit();
			StreamResult result = new StreamResult(updatedSilkScript);
			transformer.transform(domSource, result);
			
			LOG.debug("The silk script file in {} has been updated to run properly. The new file is located in {} and will now run", silkScript.getAbsolutePath(), updatedSilkScript.getAbsolutePath());
			Silk.executeFile(updatedSilkScript,"silk-interlink" , 1, true); 
			
		} catch (IOException e) {
			LOG.fatal("Edoal file copy failed - " + e);
		} catch (AlignmentException e) {
			LOG.fatal("Edoal file alignment failed - " + e);
		} catch (ParserConfigurationException e) {
			LOG.fatal("Edoal file parsing failed - " + e);
		} catch (SAXException e) {
			LOG.fatal("Xml file parsing failed - " + e);
		} catch (TransformerConfigurationException e) {
			LOG.fatal("Edoal file convertion to Silk failed - " + e);
		} catch (TransformerException e) {
			LOG.fatal("Edoal file convertion to Silk failed - " + e);
		}
        
    }
    
    private void cleanNode(Node node){
    	NodeList oldSourceList = node.getChildNodes();
		//clean the old node
		for(int i=0;i<oldSourceList.getLength();i++){
			Node childNode = oldSourceList.item(i);
			if(childNode.getNodeType() == Node.ELEMENT_NODE){
				node.removeChild(childNode);
			}
		}
    }
    
    private void addDataSource(Node datasourcesNode, Document doc, String id, String graphUri){
    	Element dataSource = doc.createElement("DataSource");
		dataSource.setAttribute("type", "sparqlEndpoint");
		dataSource.setAttribute("id", id);
		
		Element paramEndPoint = doc.createElement("Param");
		paramEndPoint.setAttribute("name", "endpointURI");
		paramEndPoint.setAttribute("value", Configuration.getDefault().getInternalRepository().getUrl());
		
		Element paramGraph = doc.createElement("Param");
		paramGraph.setAttribute("name", "graph");
		paramGraph.setAttribute("value", graphUri);
		
		dataSource.appendChild(paramEndPoint);
		dataSource.appendChild(paramGraph);
		
		datasourcesNode.appendChild(dataSource);
    }

    private void renameNodes(Document doc, String oldName, String newName){
    	NodeList sourceDatasetNodes = doc.getElementsByTagName(oldName);
		for(int index = 0;index<sourceDatasetNodes.getLength();index++){
			Node nodeSource = sourceDatasetNodes.item(index);
			doc.renameNode(nodeSource, null, newName);
		}
    }
    
    private List<String> getPrefixes(File edoalFile) throws ParserConfigurationException, SAXException, IOException{
    	DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		docBuilder = docFactory.newDocumentBuilder();
		Document silkDoc = docBuilder.parse(edoalFile);
		NodeList edoalProperties = silkDoc.getElementsByTagName("edoal:Property");
		Set<String> entityIds = new HashSet<String>();
		for(int i = 0;i<edoalProperties.getLength();i++){
			Element edoalProp = (Element) edoalProperties.item(i);
			String aboutProp = edoalProp.getAttribute("rdf:about");
			if(aboutProp.contains("#")){
				entityIds.add(aboutProp.substring(0, aboutProp.lastIndexOf("#")+1));
			}else{
				entityIds.add(aboutProp.substring(0, aboutProp.lastIndexOf("/")));
			}
				
		}
		return new ArrayList<String>(entityIds);
    }
}



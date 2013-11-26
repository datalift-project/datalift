package org.silk.interlinker.script;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.interlinker.Model;
import org.datalift.interlinker.SilkInterlinkerModel;
import org.datalift.interlinker.sparql.DataSource;
import org.silk.interlinker.script.InterlinkedSourcesInfo.ComparisonParameters;
import org.silk.interlinker.script.InterlinkedSourcesInfo.ComparisonSettings;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Create, handle and manage the script
 * @author carlo
 *
 */
public final class ScriptFileWriter {
	private static final int DEFAULT_MAX_LINKS = SilkInterlinkerModel.DEFAULT_MAX_LINKS;
	
    /** Datalift's logging system. */
    private static final Logger LOG = Logger.getLogger();

	private static String baseURISub(String URI) {
		return URI.substring(0, Math.max(URI.lastIndexOf("/"), URI.lastIndexOf("#")) + 1);
	}
	    
	
	 /**
     *  Creates multiple Prefix tags for the given pairs of ID,URIs.
     * @param doc The doc where to create the tag.
     * @param namespaces Prefix,URI pairs.
     * @return A DOM Element.
     */
    private static Element getPrefixesTag(Document doc, HashMap<String, String> namespaces) {
    	Element prefixes = doc.createElement("Prefixes");
    	for (String id : namespaces.keySet()) {
    		prefixes.appendChild(getPrefixTag(doc, id, namespaces.get(id)));
    	}
    	
    	return prefixes;
    }
	
    
    /**
     * Creates a single Prefix tag.
     * @param doc The doc where to create the tag.
     * @param id Prefix identifier.
     * @param namespace Namespace URI.
     * @return A DOM Element.
     */
    private static Element getPrefixTag(Document doc, String id, String namespace) {
    	Element prefixTag = doc.createElement("Prefix");
    	prefixTag.setAttribute("id", id);
    	prefixTag.setAttribute("namespace", namespace);
    	
    	return prefixTag;
    }
    
    /**
     * Clean the string removing every inappropriate character
     * @param str the dirty string
     * @return the same string without invalid characters
     */
    private static String getCleanString(String str){
    	return str.replaceAll("[^a-zA-Z0-9-]", "_");
    }
    
	public static File createSilkScript(InterlinkedSourcesInfo interlkingSources){
		File ret = null;
		try {
   			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
   			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
   			
   			String sourceId=getCleanString(interlkingSources.getSourceId());
   			String targetId=getCleanString(interlkingSources.getTargetId());
   			String interlinkId=sourceId +"-and-"+targetId;
   			
   			ComparisonParameters[] sourceComparisonParameters = interlkingSources.getSourceComparisonParameters();
   	    	ComparisonParameters[] targetComparisonParameters = interlkingSources.getTargetComparisonParameters();
   			
   			Document doc = docBuilder.newDocument();
   			Element root = doc.createElement("Silk");
   			
   			//first we put the most common prefixes
   			HashMap<String, String> tmpPrefixes = new HashMap<String, String>();
   			tmpPrefixes.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
   			tmpPrefixes.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
   			tmpPrefixes.put("xsd", "http://www.w3.org/2001/XMLSchema#");
   			tmpPrefixes.put("geo", "http://rdf.insee.fr/geo/");
   			tmpPrefixes.put("dc", "http://purl.org/dc/elements/1.1/");
   			tmpPrefixes.put("cc", "http://creativecommons.org/ns#");
   			tmpPrefixes.put("owl", "http://www.w3.org/2002/07/owl#");
   			tmpPrefixes.put("dcterms", "http://purl.org/dc/terms/");
   			tmpPrefixes.put("insee", "http://rdf.insee.fr/geo/");
   			
   			//then we put a prefix for every predicate the user selected and update the sources object
   			putPredicatePrefix("t", sourceComparisonParameters,tmpPrefixes);
   			putPredicatePrefix("s", targetComparisonParameters, tmpPrefixes);
   			
   			//put the prefixes to the xml document
   			root.appendChild(getPrefixesTag(doc, tmpPrefixes));
   			
   			Element dataSources = doc.createElement("DataSources");
   			dataSources.appendChild(getDataSourceTag(doc, sourceId, interlkingSources.getSourceUrl(), interlkingSources.getIsSourceLocal()));
   			dataSources.appendChild(getDataSourceTag(doc,targetId, interlkingSources.getTargetUrl(), interlkingSources.getIsTargetLocal()));
   			root.appendChild(dataSources);
   			
   			Element interlinks = doc.createElement("Interlinks");
   			Element interlink = doc.createElement("Interlink");
   			interlink.setAttribute("id",interlinkId );
   			
   			String sourceVariable = DataSource.SOURCE_SUBJ_BINDER;
   			String targetVariable = DataSource.TARGET_SUBJ_BINDER;
   			
   			interlink.appendChild(getLinkTypeTag(doc, "owl:sameAs"));
   	    	interlink.appendChild(getSourceDatasetTag(doc, sourceId, sourceVariable, interlkingSources.getSourceQuery()));
   	    	interlink.appendChild(getTargetDatasetTag(doc, targetId, targetVariable, interlkingSources.getTargetQuery()));

   	    	Element linkageRule = doc.createElement("LinkageRule");
   	    	Element aggregate = doc.createElement("Aggregate");
   	    	aggregate.setAttribute("type", interlkingSources.getAggregationSetting());
   	       	    	
   	    	for(int i=0;i<sourceComparisonParameters.length;i++){
   	    		ComparisonSettings setting = interlkingSources.getComparisonSettings()[i];
   	    		
   	    		Element sourceTransform = getTransformInputTag(doc, sourceVariable, sourceComparisonParameters[i].getProperty(), sourceComparisonParameters[i].getTransformation(), sourceComparisonParameters[i].getStringParam());
   	   	    	Element targetTransform = getTransformInputTag(doc, targetVariable, targetComparisonParameters[i].getProperty(), targetComparisonParameters[i].getTransformation(), targetComparisonParameters[i].getStringParam());
   	   	    	Element comparison = getCompareTag(doc, setting.getMetric(), setting.getThreshold(), setting.getWeight(), sourceTransform, targetTransform, setting.getMetricParams());
   	   	    	aggregate.appendChild(comparison);
   	    	}
   	    	
   	    	linkageRule.appendChild(aggregate);
   	    	interlink.appendChild(linkageRule);

   	    	interlink.appendChild(getFilterTag(doc, DEFAULT_MAX_LINKS));
   	    	interlink.appendChild(getOutputsTag(doc, Model.INTERNAL_URL, interlkingSources.getNewSourceUrl()));
   			
   			interlinks.appendChild(interlink);
   			root.appendChild(interlinks);
   			
   			doc.appendChild(root);
   			//save to file
   			TransformerFactory transformerFactory = TransformerFactory.newInstance();
   			Transformer transformer = transformerFactory.newTransformer();
   			DOMSource source = new DOMSource(doc);
   			File tmpDir = new File(Configuration.getDefault().getPublicStorage().getFile("project"), "interlink");
   			tmpDir.mkdirs();
   			String scriptFileName = interlinkId + "-config.xml"; 
   			ret = new File(tmpDir, scriptFileName);
   			ret.createNewFile();
   			ret.deleteOnExit();

   			StreamResult result = new StreamResult(ret);
   			transformer.transform(source, result);
   			
   			if (LOG.isInfoEnabled()) {
   				LOG.info("Created Silk configuration file at " + ret.getName());
   			}
		} catch (ParserConfigurationException e) {
			LOG.fatal("Error while creating Interlink configuration file - " + e);
		} catch (TransformerConfigurationException e) {
			LOG.fatal("Error while creating Interlink configuration file - " + e);
		} catch (TransformerException e) {
			LOG.fatal("Error while creating Interlink configuration file - " + e);
		} catch (IOException e) { 
			LOG.fatal("Error while creating Interlink configuration file - " + e);
		}
   		
   		return ret;
	}
	
	 /**
     * Creates an Input / TransformInput tag if necessary.
     * @param doc The doc where to create the tag.
     * @param variable Variable representing a dataset.
     * @param property A property inside the same dataset.
     * @param function A function to transform the data with.
     * @param stringFunctionParam parameters used when we want to run a string function
     * @return A DOM Element.
     */
    private static Element getTransformInputTag(Document doc, String variable, String property, String function, String stringFunctionParam[]) {
    	Element ret;
    	Element input = doc.createElement("Input");
    	input.setAttribute("path", "?" + variable + "/" + property);
    	
    	// We don't always need a TransformInput.
    	if (function!=null && !function.equals("none")) {
	    	ret = doc.createElement("TransformInput");
	    	
	    	String effectiveFunction = function;
	    	ret.appendChild(input);
	    	
	    	// Special case : capitalize first letters.
	    	if (function.startsWith("capitalize")) {
	    		effectiveFunction = "capitalize";
	    		
	    		Element allWords = doc.createElement("Param");
	    		allWords.setAttribute("name", "allWords");
	    		allWords.setAttribute("value", Boolean.toString(function.endsWith("All")));
	    		
	    		ret.appendChild(allWords);
	    	}
	    	// Special case : encoding convertion functions.
	    	else if (function.startsWith("convert")) {
	    		effectiveFunction = "convert";
	    		
	    		Element sourceCharset = doc.createElement("Param");
	    		Element targetCharset = doc.createElement("Param");
	    		sourceCharset.setAttribute("name", "sourceCharset");
	    		targetCharset.setAttribute("name", "targetCharset");
	    		if (function.endsWith("UTFISO")) {
	    			sourceCharset.setAttribute("value", "UTF-8");
	    			targetCharset.setAttribute("value", "ISO-8859-1");
	    		}
	    		else if (function.endsWith("ISOUTF")) {
	    			sourceCharset.setAttribute("value", "ISO-8859-1");
	    			targetCharset.setAttribute("value", "UTF-8");
	    		}
	    		ret.appendChild(sourceCharset);
	    		ret.appendChild(targetCharset);
	    	}
	    	// Special case : numerical transformation with logarithms.
	    	else if (function.endsWith("Logarithm")) {
	    		effectiveFunction = "logarithm";
	    		
	    		Element base = doc.createElement("Param");
	    		base.setAttribute("name", "base");
	    		if (function.startsWith("common")) {
	    			base.setAttribute("value", "10");
	    		} 
	    		else if (function.startsWith("natural")) {
	    			base.setAttribute("value", Double.toString(Math.E));
	    		} 
	    		else if (function.startsWith("binary"))  {
	    			base.setAttribute("value", "2");
	    		}
	    		ret.appendChild(base);
	    	}
	    	// Special case : search and replace.
	    	else if (function.toLowerCase().endsWith("replace")) {
	    		Element searchTag = doc.createElement("Param");
	    		searchTag.setAttribute("name", function.startsWith("regex") ? "regex" : "search");
	    		searchTag.setAttribute("value", stringFunctionParam[0]);
	    		
	    		Element replaceTag = doc.createElement("Param");
	    		replaceTag.setAttribute("name", "replace");
	    		replaceTag.setAttribute("value", stringFunctionParam[1]);
	    		
	    		ret.appendChild(searchTag);
	    		ret.appendChild(replaceTag);
	    	}
	    	// Special case : tokenize the value.
	    	else if (function.equals("tokenize")) {
	    		Element regex = doc.createElement("Param");
	    		regex.setAttribute("name", "regex");
	    		regex.setAttribute("value", stringFunctionParam[0]);
	    		
	    		ret.appendChild(regex);
	    	}
	    	// Special case : remove stopWords / blacklist.
	    	else if (function.equals("tokenize")) {
	    		Element stopWordsTag = doc.createElement("Param");
	    		stopWordsTag.setAttribute("name", "blacklist");
	    		stopWordsTag.setAttribute("value", stringFunctionParam[0].replace("\"", "'"));
	    		
	    		ret.appendChild(stopWordsTag);
	    	}

	    	ret.setAttribute("function", effectiveFunction);
    	}
    	else {
    		ret = input;
    	}
    	
    	return ret;
    }
    

	
	 /**
     * Usual use : owl:sameAs.
     * @param doc The doc where to create the tag.
     * @param type owl:sameAs.
     * @return A DOM Element.
     */
    private static Element getLinkTypeTag(Document doc, String type) {
    	Element linkType = doc.createElement("LinkType");
    	linkType.setTextContent(type);
    	
    	return linkType;
    }
    
	
	  /**
     * Creates a DataSource tag for a given id, address, deciding whether it is
     * a file, an endpoint URL or a Datalift source.
     * @param doc The doc where to create the tag.
     * @param id Source identifier.
     * @param address Source address.
     * @return A DOM Element.
     */
	private static Element getDataSourceTag(Document doc, String id, String address, boolean isLocal) {
    	Element dataSourceTag = doc.createElement("DataSource");
    	Element param = doc.createElement("Param");
    	
    	String type	= "sparqlEndpoint";
    	String name = "endpointURI";
    		
    	dataSourceTag.setAttribute("id", id);
    	dataSourceTag.setAttribute("type", type);
    	
    	param.setAttribute("name", name);
    	String sparqlEndpoint = isLocal? Model.INTERNAL_URL :address;
    	param.setAttribute("value", sparqlEndpoint);
    	
    	dataSourceTag.appendChild(param);
    	
    	if(isLocal){
	    	Element paramTer = doc.createElement("Param");
			paramTer.setAttribute("name", "graph");
			paramTer.setAttribute("value", address);
				
			dataSourceTag.appendChild(paramTer);
    	}
    	
    	return dataSourceTag;
    }
	
		
	/**
	 * Create the namespaces for the comparison predicates
	 * @param prefix
	 * @param compParam
	 * @param prefixMap
	 */
	private static void putPredicatePrefix(String prefix, ComparisonParameters[] compParam,HashMap<String, String> prefixMap){
		for(int i=0;i<compParam.length;i++){
			String property = compParam[i].getProperty();
			String baseUri = baseURISub(property);
			String fullPrefix = prefix+i;
			prefixMap.put(fullPrefix, baseUri);
			compParam[i].setProperty(property.replace(baseUri, fullPrefix+":"));
		}
	}
	
	
	 /**
     * Creates a SourceDataset tag for the given id, variable and optional query.
     * @param doc The doc where to create the tag.
     * @param dataSource The Dataset id defined in a DataSource tag.
     * @param var Variable representing this dataset.
     * @param query An optional SPARQL Query to restrain the dataset's reach.
     * @return A DOM Element.
     */
    private static Element getSourceDatasetTag(Document doc, String dataSource, String var, String query) {
    	return getDatasetTag(doc, "Source", dataSource, var, query);
    }
    
    /**
     * Creates a TargetDataset tag for the given id, variable and optional query.
     * @param doc The doc where to create the tag.
     * @param dataSource The Dataset id defined in a DataSource tag.
     * @param var Variable representing this dataset.
     * @param query An optional SPARQL Query to restrain the dataset's reach.
     * @return A DOM Element.
     */
    private static Element getTargetDatasetTag(Document doc, String dataSource, String var, String query) {
    	return getDatasetTag(doc, "Target", dataSource, var, query);
    }
    
    /**
     * Creates a Dataset tag for the given id, variable and optional query.
     * @param doc The doc where to create the tag.
     * @param tag Either Source or Target.
     * @param dataSource The Dataset id defined in a DataSource tag.
     * @param var Variable representing this dataset.
     * @param query An optional SPARQL Query to restrain the dataset's reach.
     * @return A DOM Element.
     */
    private static Element getDatasetTag(Document doc, String tag, String dataSource, String var, String query) {
    	Element dataset = doc.createElement(tag + "Dataset");
    	dataset.setAttribute("dataSource", dataSource);
    	dataset.setAttribute("var", var);
    	
    	if (query==null) {
	    	Element restrictTo = doc.createElement("RestrictTo");
	    	restrictTo.setTextContent(query);
	    	
	    	dataset.appendChild(restrictTo);
    	}
    	
    	return dataset;
    }
    
    /**
     * Creates a comparison tag, sometimes with additional parameters.
     * @param doc The doc where to create the tag.
     * @param metric The distance measure to run the comparison with.
     * @param threshold A threshold to define what is valuable.
     * @param weight Useful when multiple comparisons are aggregated with means.
     * @param transformSource Source input tag.
     * @param transformTarget Target input tag.
     * @param metricParams parameters needed for for the metrics num and wgs84
     * @return A DOM Element.
     */
    private static Element getCompareTag(Document doc, String metric, String threshold, String weight, Element transformSource, Element transformTarget, String[] metricParams) {
    	Element compare = doc.createElement("Compare");
    	compare.setAttribute("metric", metric);
    	compare.setAttribute("threshold", threshold);
    	if (weight!=null && !weight.isEmpty()) {
    		compare.setAttribute("weight", weight);
    	}
    	compare.appendChild(transformSource);
    	compare.appendChild(transformTarget);
    	
    	// Special case : num comparison with two parameters.
    	if (metric.equals("num")) {
    		Element minValue = doc.createElement("Param");
    		minValue.setAttribute("name", "minValue");
    		minValue.setAttribute("value", metricParams[0]);
    		Element maxValue = doc.createElement("Param");
    		maxValue.setAttribute("name", "maxValue");
    		maxValue.setAttribute("value",  metricParams[1]);
    		
    		compare.appendChild(minValue);
    		compare.appendChild(maxValue);
    	}
    	// Special case : geographical comparison with two parameters.
    	else if (metric.equals("wgs84")) {
    		Element unitTag = doc.createElement("Param");
    		unitTag.setAttribute("name", "unit");
    		unitTag.setAttribute("value",  metricParams[0]);
    		Element curveStyle = doc.createElement("Param");
    		curveStyle.setAttribute("name", "curveStyle");
    		curveStyle.setAttribute("value",  metricParams[1]);
    		
    		compare.appendChild(unitTag);
    		compare.appendChild(curveStyle);
    	}
    	
    	return compare;
    }
    
    /**
     * Creates a Filter tag, either empty or complete according to the limit.
     * @param doc The doc where to create the tag.
     * @param limit Maximum number of links to create per RDF entity.
     * @return A DOM Element.
     */
    private static Element getFilterTag(Document doc, int limit) {
    	Element filter = doc.createElement("Filter");
    	if (limit > 0) {
    		filter.setAttribute("limit", Integer.toString(limit)); 
    	}
    	return filter;
    }
    
    /**
     * Creates a Output tag where the output is SPARQL updates.
     * @param doc The doc where to create the tag.
     * @param endpointURL Where to do the update.
     * @param graphURI The URI of the graph to put the links 
     * @return A DOM Element.
     */
    private static Element getOutputsTag(Document doc, String endpointURL, String graphURI) {
    	Element outputs = doc.createElement("Outputs");
    	Element output = doc.createElement("Output");
    	output.setAttribute("type", "sparul");
  
    	Element param = doc.createElement("Param");
    	param.setAttribute("name", "uri");
    	param.setAttribute("value", endpointURL);
    	Element paramBis = doc.createElement("Param");
    	paramBis.setAttribute("name", "parameter");
    	paramBis.setAttribute("value", "update");
    	Element paramTer = doc.createElement("Param");
    	paramTer.setAttribute("name", "graphUri");
    	paramTer.setAttribute("value", graphURI);
    	
    	output.appendChild(param);
    	output.appendChild(paramBis);
    	output.appendChild(paramTer);
    	outputs.appendChild(output);
    	
    	return outputs;
    }
}

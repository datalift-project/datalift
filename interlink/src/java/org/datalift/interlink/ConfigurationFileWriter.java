/*
 * Copyright / LIRMM 2011-2012
 * Contributor(s) : T. Colas, F. Scharffe
 *
 * Contact: thibaud.colas@etud.univ-montp2.fr
 */

package org.datalift.interlink;

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
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Helps writing Silk configuration files using DOM manipulation.
 * @author tcolas
 * @version 03102012
 */
public final class ConfigurationFileWriter {
	
	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The default fallback max number of links created per RDF entity. */
    public static final int DEFAULT_MAX_LINKS = SilkModel.DEFAULT_MAX_LINKS;
    
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------
    
    /** Base name of the resource bundle for converter GUI. */
    private static final String GUI_RESOURCES_BUNDLE = SilkController.GUI_RESOURCES_BUNDLE;
    /** Datalift's internal Sesame {@link Repository repository}. **/
    private static final Repository INTERNAL_REPO = Configuration.getDefault().getInternalRepository();
    /** Datalift's internal Sesame {@link Repository repository} URL. */
    private static final String INTERNAL_URL = INTERNAL_REPO.getEndpointUrl();
    /** Datalift's logging system. */
    private static final Logger LOG = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new ConfigurationFileWriter instance.
     */
    private ConfigurationFileWriter() {
    	// Private : Not supposed to be used.
    }
    
    /**
     * Resource getter.
     * @param key The key to retrieve.
     * @return The value of key.
     */
    private static String getTranslatedResource(String key) {
    	return PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, SilkController.class).getString(key);
    }
    
    /**
     * Checks whether a value is empty, eg. "", "Aucune" or "None". The value 
     * must be trimmed first.
     * @param val Value to check.
     * @return True if val is empty.
     */
    public static boolean isEmptyValue(String val) {
    	return val.isEmpty() 
    		|| val.equals(getTranslatedResource("field.none")) 
    		|| val.startsWith(getTranslatedResource("field.optional")) 
    		|| val.startsWith(getTranslatedResource("field.example"))
    		|| val.startsWith(getTranslatedResource("field.mandatory"));
    }
    
	//-------------------------------------------------------------------------
    // XML writer management.
    //-------------------------------------------------------------------------
    
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
     * Creates a DataSource tag for a given id, address, deciding whether it is
     * a file, an endpoint URL or a Datalift source.
     * @param doc The doc where to create the tag.
     * @param id Source identifier.
     * @param address Source address.
     * @param context graph to use if sparqlENdpoint.
     * @return A DOM Element.
     */
	private static Element getDataSourceTag(Document doc, String id, String address, String context) {
    	Element dataSourceTag = doc.createElement("DataSource");
    	Element param = doc.createElement("Param");
    	
    	String type;
    	String name;
    	// Datalift sources will have address = internal repo address.
    	if (!address.startsWith("http")) {
    		type = "file";
    		name = "file";
    	}
    	else {
    		type = "sparqlEndpoint";
    		name = "endpointURI";
    		
    	}
    	dataSourceTag.setAttribute("id", id);
    	dataSourceTag.setAttribute("type", type);
    	param.setAttribute("name", name);
    	param.setAttribute("value", address);
    	
    	dataSourceTag.appendChild(param);
    	
    	if (!address.startsWith("http")) {
    		Element paramBis = doc.createElement("Param");
    		paramBis.setAttribute("name", "format");
    		paramBis.setAttribute("value", "RDF/XML");
        	
        	dataSourceTag.appendChild(paramBis);
    	}
    	// Essentially for Datalift sources.
    	else if (!context.isEmpty()) {
			Element paramTer = doc.createElement("Param");
			paramTer.setAttribute("name", "graph");
			paramTer.setAttribute("value", context);
			
			dataSourceTag.appendChild(paramTer);
		}
    	
    	return dataSourceTag;
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
    	
    	if (!isEmptyValue(query)) {
	    	Element restrictTo = doc.createElement("RestrictTo");
	    	restrictTo.setTextContent(query);
	    	
	    	dataset.appendChild(restrictTo);
    	}
    	
    	return dataset;
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
     * Creates a comparison tag, sometimes with additional parameters.
     * @param doc The doc where to create the tag.
     * @param metric The distance measure to run the comparison with.
     * @param threshold A threshold to define what is valuable.
     * @param weight Useful when multiple comparisons are aggregated with means.
     * @param transformOne First input tag.
     * @param transformTwo Secund input tag.
     * @param min Additional field for the "num" distance measure.
     * @param max Additional field for the "num" distance measure.
     * @param unit Additional field for the "wgs84" distance measure.
     * @param curve Additional field for the "wgs84" distance measure.
     * @return A DOM Element.
     */
    private static Element getCompareTag(Document doc, String metric, String threshold, String weight, Element transformOne, Element transformTwo, String min, String max, String unit, String curve) {
    	Element compare = doc.createElement("Compare");
    	compare.setAttribute("metric", metric);
    	compare.setAttribute("threshold", threshold);
    	if (!isEmptyValue(weight)) {
    		compare.setAttribute("weight", weight);
    	}
    	compare.appendChild(transformOne);
    	compare.appendChild(transformTwo);
    	
    	// Special case : num comparison with two parameters.
    	if (metric.equals("num")) {
    		Element minValue = doc.createElement("Param");
    		minValue.setAttribute("name", "minValue");
    		minValue.setAttribute("value", min);
    		Element maxValue = doc.createElement("Param");
    		maxValue.setAttribute("name", "maxValue");
    		maxValue.setAttribute("value", max);
    		
    		compare.appendChild(minValue);
    		compare.appendChild(maxValue);
    	}
    	// Special case : geographical comparison with two parameters.
    	else if (metric.equals("wgs84")) {
    		Element unitTag = doc.createElement("Param");
    		unitTag.setAttribute("name", "unit");
    		unitTag.setAttribute("value", unit);
    		Element curveStyle = doc.createElement("Param");
    		curveStyle.setAttribute("name", "curveStyle");
    		curveStyle.setAttribute("value", curve);
    		
    		compare.appendChild(unitTag);
    		compare.appendChild(curveStyle);
    	}
    	
    	return compare;
    }
    
    /**
     * Creates an Input / TransformInput tag if necessary.
     * @param doc The doc where to create the tag.
     * @param variable Variable representing a dataset.
     * @param property A property inside the same dataset.
     * @param function A function to transform the data with.
     * @param regexpToken Additional field for the "tokenize" function.
     * @param stopWords Additional field for the "removeValues" function.
     * @param search Additional field for the "replace" and "regexpReplace" functions.
     * @param replace Additional field for the "replace" and "regexpReplace" functions.
     * @return A DOM Element.
     */
    private static Element getTransformInputTag(Document doc, String variable, String property, String function, String regexpToken, String stopWords, String search, String replace) {
    	Element ret;
    	Element input = doc.createElement("Input");
    	input.setAttribute("path", "?" + variable + "/" + property);
    	
    	// We don't always need a TransformInput.
    	if (!isEmptyValue(function)) {
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
	    		searchTag.setAttribute("value", search);
	    		
	    		Element replaceTag = doc.createElement("Param");
	    		replaceTag.setAttribute("name", "replace");
	    		replaceTag.setAttribute("value", replace);
	    		
	    		ret.appendChild(searchTag);
	    		ret.appendChild(replaceTag);
	    	}
	    	// Special case : tokenize the value.
	    	else if (function.equals("tokenize")) {
	    		Element regex = doc.createElement("Param");
	    		regex.setAttribute("name", "regex");
	    		regex.setAttribute("value", regexpToken);
	    		
	    		ret.appendChild(regex);
	    	}
	    	// Special case : remove stopWords / blacklist.
	    	else if (function.equals("tokenize")) {
	    		Element stopWordsTag = doc.createElement("Param");
	    		stopWordsTag.setAttribute("name", "blacklist");
	    		stopWordsTag.setAttribute("value", stopWords.replace("\"", "'"));
	    		
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
    
    private static String baseURISub(String URI) {
    	return URI.substring(0, Math.max(URI.lastIndexOf("/"), URI.lastIndexOf("#")) + 1);
    }
    
    /**
     * Creates a Silk configuration file with the given data. Some of the parameters
     * are from fields that are optional and thus might be empty.
     * @return The configuration file.
     */
    public static File createConfigFile(String interlinkId,
    		// Source fields.
    		String sourceId,
    		String sourceAddress,
    		String sourceContext,
    		String sourceQuery,
    		String sourcePropertyFirst,
    		String sourceTransformationFirst,
    		String sourceRegexpTokenFirst,
    		String sourceStopWordsFirst,
    		String sourceSearchFirst,
    		String sourceReplaceFirst,
    		// Optional fields for surnumerous comparisons.
    		String sourcePropertySecund,
    		String sourceTransformationSecund,
    		String sourceRegexpTokenSecund,
    		String sourceStopWordsSecund,
    		String sourceSearchSecund,
    		String sourceReplaceSecund,
    		String sourcePropertyThird,
    		String sourceTransformationThird,
    		String sourceRegexpTokenThird,
    		String sourceStopWordsThird,
    		String sourceSearchThird,
    		String sourceReplaceThird,
    		// Target fields.
    		String targetId,
    		String targetAddress,
    		String targetContext,
    		String targetQuery,
    		String targetPropertyFirst,
    		String targetTransformationFirst,
    		String targetRegexpTokenFirst,
    		String targetStopWordsFirst,
    		String targetSearchFirst,
    		String targetReplaceFirst,
    		String targetPropertySecund,
    		String targetTransformationSecund,
    		String targetRegexpTokenSecund,
    		String targetStopWordsSecund,
    		String targetSearchSecund,
    		String targetReplaceSecund,
    		String targetPropertyThird,
    		String targetTransformationThird,
    		String targetRegexpTokenThird,
    		String targetStopWordsThird,
    		String targetSearchThird,
    		String targetReplaceThird,
    		// Common comparison & aggregation fields.
    		String metricFirst,
    		String minFirst,
    		String maxFirst,
    		String unitFirst,
    		String curveFirst,
    		String weightFirst,
    		String thresholdFirst,
    		// Optional fields for surnumerous comparisons.
    		String metricSecund,
    		String minSecund,
    		String maxSecund,
    		String unitSecund,
    		String curveSecund,
    		String weightSecund,
    		String thresholdSecund,
    		String metricThird,
    		String minThird,
    		String maxThird,
    		String unitThird,
    		String curveThird,
    		String weightThird,
    		String thresholdThird,
    		// Aggregation method.
    		String aggregation,
    		// New graph URI
    		String newGraphURI) {
    	File ret = null;
    	
   		try {
   			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
   			DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
   			
   			Document doc = docBuilder.newDocument();
   			Element root = doc.createElement("Silk");
   			
   			//TODO Do not use hard coded prefixes.
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
   			tmpPrefixes.put("eurostat", "http://ec.europa.eu/eurostat/ramon/ontologies/geographic.rdf#");
   			tmpPrefixes.put("geo", "http://www.telegraphis.net/ontology/geography/geography#");
   			tmpPrefixes.put("gn", "http://www.geonames.org/ontology#");
   			
   			String tmpBaseURI = baseURISub(targetPropertyFirst);
   			tmpPrefixes.put("x", tmpBaseURI);
   			targetPropertyFirst = targetPropertyFirst.replace(tmpBaseURI, "x:");
   			if (!targetPropertySecund.equals("")) {
	   			tmpBaseURI = baseURISub(targetPropertySecund);
	   			tmpPrefixes.put("y", tmpBaseURI);
	   			targetPropertySecund = targetPropertySecund.replace(tmpBaseURI, "y:");
	   			if (!targetPropertyThird.equals("")) {
		   			tmpBaseURI = baseURISub(targetPropertyThird);
		   			tmpPrefixes.put("z", tmpBaseURI);
		   			targetPropertyThird = targetPropertyThird.replace(tmpBaseURI, "z:");
	   			}
   			}
   			
   			tmpBaseURI = baseURISub(sourcePropertyFirst);
   			tmpPrefixes.put("xx", tmpBaseURI);
   			sourcePropertyFirst = sourcePropertyFirst.replace(tmpBaseURI, "xx:");
   			if (!sourcePropertySecund.equals("")) {
	   			tmpBaseURI = baseURISub(sourcePropertySecund);
	   			tmpPrefixes.put("yy", tmpBaseURI);
	   			sourcePropertySecund = sourcePropertySecund.replace(tmpBaseURI, "yy:");
	   			if (!sourcePropertyThird.equals("")) {
		   			tmpBaseURI = baseURISub(sourcePropertyThird);
		   			tmpPrefixes.put("zz", tmpBaseURI);
		   			sourcePropertyThird = sourcePropertyThird.replace(tmpBaseURI, "zz:");
	   			}
   			}
   			

   			root.appendChild(getPrefixesTag(doc, tmpPrefixes));
   			
   			if (isEmptyValue(sourceId) || isEmptyValue(targetId) || isEmptyValue(sourceAddress) || isEmptyValue(targetAddress)) {
   				throw new ParserConfigurationException("Required DataSource fields can't be empty");
   			}
   			
   			Element dataSources = doc.createElement("DataSources");
   			dataSources.appendChild(getDataSourceTag(doc, sourceId, sourceAddress, sourceContext));
   			dataSources.appendChild(getDataSourceTag(doc, targetId, targetAddress, targetContext));
   			root.appendChild(dataSources);
   			
   			Element interlinks = doc.createElement("Interlinks");
   			Element interlink = doc.createElement("Interlink");
   			interlink.setAttribute("id", interlinkId);
   			
   			
   			//TODO Set it elsewhere ?
   			String sourceVariable = SilkModel.SB;
   			String targetVariable = SilkModel.SB;
   			
   	    	interlink.appendChild(getLinkTypeTag(doc, "owl:sameAs"));
   	    	interlink.appendChild(getSourceDatasetTag(doc, sourceId, sourceVariable, sourceQuery));
   	    	interlink.appendChild(getTargetDatasetTag(doc, targetId, targetVariable, targetQuery));
   	    	
   	    	Element linkageRule = doc.createElement("LinkageRule");
   	    	Element aggregate = doc.createElement("Aggregate");
   	    	aggregate.setAttribute("type", aggregation);
   	    	
   	    	Element firstSourceTransform = getTransformInputTag(doc, sourceVariable, sourcePropertyFirst, sourceTransformationFirst, sourceRegexpTokenFirst, sourceStopWordsFirst, sourceSearchFirst, sourceReplaceFirst);
   	    	Element firstTargetTransform = getTransformInputTag(doc, targetVariable, targetPropertyFirst, targetTransformationFirst, targetRegexpTokenFirst, targetStopWordsFirst, targetSearchFirst, targetReplaceFirst);
   	    	Element firstComparison = getCompareTag(doc, metricFirst, thresholdFirst, weightFirst, firstSourceTransform, firstTargetTransform, minFirst, maxFirst, unitFirst, curveFirst);
   	    	aggregate.appendChild(firstComparison);
   	    	
   	    	// If targetProperty is empty, we don't need the new transformations / comparions.
   	    	if (!isEmptyValue(sourcePropertySecund) && !isEmptyValue(targetPropertySecund)) {
   	    		Element secundSourceTransform = getTransformInputTag(doc, sourceVariable, sourcePropertySecund, sourceTransformationSecund, sourceRegexpTokenSecund, sourceStopWordsSecund, sourceSearchSecund, sourceReplaceSecund);
   	    		Element secundTargetTransform = getTransformInputTag(doc, targetVariable, targetPropertySecund, targetTransformationSecund, targetRegexpTokenSecund, targetStopWordsSecund, targetSearchSecund, targetReplaceSecund);
   	    		Element secundComparison = getCompareTag(doc, metricSecund, thresholdSecund, weightSecund, secundSourceTransform, secundTargetTransform, minSecund, maxSecund, unitSecund, curveSecund);
   	    		aggregate.appendChild(secundComparison);
   	    		
   	    		if (!isEmptyValue(sourcePropertyThird) && !isEmptyValue(targetPropertyThird)) {
   	    			Element thirdSourceTransform = getTransformInputTag(doc, sourceVariable, sourcePropertyThird, sourceTransformationThird, sourceRegexpTokenThird, sourceStopWordsThird, sourceSearchThird, sourceReplaceThird);
   	    			Element thirdTargetTransform = getTransformInputTag(doc, targetVariable, targetPropertyThird, targetTransformationThird, targetRegexpTokenThird, targetStopWordsThird, targetSearchThird, targetReplaceThird);
   	    			Element thirdComparison = getCompareTag(doc, metricThird, thresholdThird, weightThird, thirdSourceTransform, thirdTargetTransform, minThird, maxThird, unitThird, curveThird);
   	    			aggregate.appendChild(thirdComparison);
   	    		}
   	    	}
   	    	
   	    	linkageRule.appendChild(aggregate);
   	    	interlink.appendChild(linkageRule);

   	    	interlink.appendChild(getFilterTag(doc, DEFAULT_MAX_LINKS));
   	    	interlink.appendChild(getOutputsTag(doc, INTERNAL_URL, newGraphURI));
   			
   			interlinks.appendChild(interlink);
   			root.appendChild(interlinks);
   			
   			doc.appendChild(root);
   			
   			TransformerFactory transformerFactory = TransformerFactory.newInstance();
   			Transformer transformer = transformerFactory.newTransformer();
   			DOMSource source = new DOMSource(doc);
   			ret = new File(Configuration.getDefault().getPublicStorage().getAbsoluteFile() + File.separator + "interlink-config.xml");
   			ret.createNewFile();
   			ret.deleteOnExit();

   			StreamResult result = new StreamResult(ret);
   			
   			// Save to file.
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
}

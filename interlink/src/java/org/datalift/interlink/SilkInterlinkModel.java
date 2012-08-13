/*
 * Copyright / LIRMM 2011-2012
 * Contributor(s) : T. Colas, F. Scharffe
 *
 * Contact: thibaud.colas@etud.univ-montp2.fr
 */

package org.datalift.interlink;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.Repository;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import de.fuberlin.wiwiss.silk.Silk;

/**
 * A {@link ProjectModule project module} that uses the Silk link generation
 * framework to generate links between two datasets.
 * This class handles Silk interlink constraints.
 * TODO Configuration file.
 * TODO Might not work well if used with external files / endpoints. Is it supposed to ?
 * TODO Add namespace prefixing management
 * TODO Refactoring : InterconnectionController, InterconnectionModel, SilkManager, ConfigurationWriter, SilkInterface
 *
 * @author tcolas
 * @version 12082012
 */
public class SilkInterlinkModel {
	
	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

	/** The module's name. */
    public static final String MODULE_NAME = SilkInterlinkController.MODULE_NAME;
    
    /** The default fallback number of threads to use when executing Silk. */
    public static final int DEFAULT_NB_THREADS = 1;
    /** The default fallback way to manage cache to use when executing Silk. */
    public static final boolean DEFAULT_RELOAD_CACHE = true;
    /** The default fallback max number of links created per RDF entity. */
    public static final int DEFAULT_MAX_LINKS = 0;
    
    /** Binding for the default subject var in SPARQL. */
    private static final String SB = "s";
    /** Binding for the default predicate var in SPARQL. */
    private static final String PB = "p";
    /** Binding for the default object var in SPARQL. */
    private static final String OB = "o";
    
    /** Base name of the resource bundle for converter GUI. */
    private static final String GUI_RESOURCES_BUNDLE = SilkInterlinkController.GUI_RESOURCES_BUNDLE;
    
    /** Default WHERE SPARQL clause to retrieve all classes. */
    private static final String CLASS_WHERE = "{?" + SB + " a ?" + OB + "}";
    /** Default WHERE SPARQL clause to retrieve all predicates. */
    private static final String PREDICATE_WHERE = "{?" + SB + " ?" + PB + " ?" + OB + "}";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** Datalift's internal Sesame {@link Repository repository}. **/
    private static final Repository INTERNAL_REPO = Configuration.getDefault().getInternalRepository();
    /** Datalift's internal Sesame {@link Repository repository} URL. */
    private static final String INTERNAL_URL = INTERNAL_REPO.getEndpointUrl();
    /** Datalift's logging system. */
    private static final Logger LOG = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Connection to the internal Sesame {@link Repository repository}. **/
    private RepositoryConnection internal;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new SilkInterlinkModel instance.
     */
    public SilkInterlinkModel() {
    	internal =  INTERNAL_REPO.newConnection();
    }
    
    
    /**
     * Resource getter.
     * @param key The key to retrieve.
     * @return The value of key.
     */
    private String getTranslatedResource(String key) {
    	return PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, this).getString(key);
    }
    
    //-------------------------------------------------------------------------
    // Sources management.
    //-------------------------------------------------------------------------
    
    /**
     * Checks if a given {@link Source} contains valid RDF-structured data.
     * @param src The source to check.
     * @return True if src is {@link TransformedRdfSource} or {@link SparqlSource}.
     */
    private boolean isValidSource(Source src) {
    	return src.getType().equals(SourceType.TransformedRdfSource) 
        	|| src.getType().equals(SourceType.SparqlSource);
    }
    
    /**
     * Checks if a {@link Project proj} contains valid RDF sources.
     * @param proj The project to check.
     * @param minvalid The number of RDF sources we want to have.
     * @return True if there are more than number valid sources.
     */
    public final boolean hasMultipleRDFSources(Project proj, int minvalid) {
    	int cpt = 0;
    	Iterator<Source> sources = proj.getSources().iterator();
    	
    	while (sources.hasNext() && cpt < minvalid) {
    		if (isValidSource(sources.next())) {
    			cpt++;
    		}
    	}
    	return cpt >= minvalid;
    }
    
    /**
     * Returns all of the URIs (as strings) from the {@link Project project}.
     * @param proj The project to use.
     * @return A LinkedList containing source file's URIs as strings.
     */
    public final LinkedList<String> getSourcesURIs(Project proj) {
    	LinkedList<String> ret = new LinkedList<String>();
    	
    	for (Source src : proj.getSources()) {
    		if (isValidSource(src)) {
    			ret.add(src.getUri());
    		}
    	}
    	return ret;
    }
    
    public final boolean isDataliftSource(Project proj, String src) {
    	return getSourcesURIs(proj).contains(src);
    }
    
    public static final boolean isFileSource(String src) {
    	return !src.startsWith("http");
    }
    
    public static final String extractFileSourceName(String src) {
    	return src.substring(src.lastIndexOf("/")).replace(".", "");
    }
    
    public static final String extractEndpointSourceName(String src) {
    	// Strip http://www.
    	String ret = src.substring(7).replace("www.", "");
    	return ret.replace("/", ".");
    }
    
    //-------------------------------------------------------------------------
    // Value validation.
    //-------------------------------------------------------------------------
    
    /**
     * Checks whether a value is empty, eg. "", "Aucune" or "None". The value 
     * must be trimmed first.
     * @param val Value to check.
     * @return True if val is empty.
     */
    public final boolean isEmptyValue(String val) {
    	return val.isEmpty() 
    		|| val.equals(getTranslatedResource("field.none")) 
    		|| val.startsWith(getTranslatedResource("field.optional")) 
    		|| val.startsWith(getTranslatedResource("field.example"))
    		|| val.startsWith(getTranslatedResource("field.mandatory"));
    }
    
    /**
     * Checks whether a query seems to have a valid syntax.
     * @param query SPARQL Query WHERE statement to check.
     * @param var Return variable.
     * @return True if query is valid.
     */
    private boolean isValidQuery(String query, String var) {
    	// Might make use of a better validation.
    	return query.contains("?" + var);
    }
    
    /**
     * Checks that if a special transformation function is used, the required
     * additional fields are submitted too.
     * @param transformation A transformation to check.
     * @param specialCase Its special case.
     * @param additionalField The required additional field.
     * @return True if transf != special case OR transf = special case && additionalField not empty.
     */
    private boolean validTransformation(String transformation, String specialCase, String additionalField) {
    	// OK if transf != special case OR transf = special case && additionalField not empty.
    	return !transformation.equals(specialCase)
    		|| !isEmptyValue(additionalField);
    }
    
    //TODO Refactor those two functions and give them a better name.
    
    /**
     * Checks that if a special distance measure is used, the required
     * additional fields are submitted too.
     * @param measure A distance measure to check.
     * @param specialCase Its special case.
     * @param additionalField The required additional field.
     * @return True if meas != special case OR meas = special case && additionalField not empty.
     */
    private boolean validDistanceMeasure(String measure, String specialCase, String additionalField) {
    	// OK if meas != special case OR meas = special case && additionalField not empty.
    	return !measure.equals(specialCase)
    		|| !isEmptyValue(additionalField);
    }
    
    /**
     * Checks if the given string is numeric. Relies on exceptions being thrown,
     * thus not very effective performance-wise.
     * @param str A possibly numerical string.
     * @return True if str is numeric.
     */
    public static boolean isNumeric(String str) { 
    	boolean ret = true;
    	try {  
    		Double.parseDouble(str);  
    	}  
    	catch (NumberFormatException nfe) {  
    		ret = false;  
    	}  
    	return ret;  
    }

    
    //-------------------------------------------------------------------------
    // Queries management.
    //-------------------------------------------------------------------------
    
    /**
	 * Tels if the bindings of the results are wel-formed.
	 * @param tqr The result of a SPARQL query.
	 * @param bind The result one and only binding.
	 * @return True if the results contains only bind.
	 * @throws QueryEvaluationException Error while closing the result.
	 */
	private boolean hasCorrectBindingNames(TupleQueryResult tqr, String bind) throws QueryEvaluationException {
		return tqr.getBindingNames().size() == 1 && tqr.getBindingNames().contains(bind);
	}

	/**
	 * Sends and evaluates a SPARQL select query on the data set, then returns
	 * the results (which must be one-column only) as a list of Strings.
	 * @param query The SPARQL query without its prefixes.
	 * @param bind The result one and only binding.
	 * @return The query's result as a list of Strings.
	 */
    private LinkedList<String> selectQuery(String query, String bind) {
		TupleQuery tq;
		TupleQueryResult tqr;
		LinkedList<String> ret = new LinkedList<String>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug(MODULE_NAME + " " + query);
		}
		
		try {
			tq = internal.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			
			if (!hasCorrectBindingNames(tqr, bind)) {
				throw new MalformedQueryException("Wrong query result bindings - " + query);
			}
			
			while (tqr.hasNext()) {
				ret.add(tqr.next().getValue(bind).stringValue());
			}
		}
		catch (MalformedQueryException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		} catch (QueryEvaluationException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		} catch (RepositoryException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		}
	    return ret;
	}
    
    /**
	 * Sends and evaluates a SPARQL select query on the data set, then returns
	 * the results (which must be two-column only) as a list of lists of Strings.
	 * @param query The SPARQL query without its prefixes.
	 * @param stub The predicate used in the query.
	 * @return The query's result as a list of Strings.
	 */
    private LinkedList<LinkedList<String>> pairSelectQuery(String query, String stub) {
		TupleQuery tq;
		TupleQueryResult tqr;
		BindingSet bs;
		LinkedList<LinkedList<String>> ret = new LinkedList<LinkedList<String>>();
		
		if (LOG.isDebugEnabled()) {
			LOG.debug(MODULE_NAME + " " + query);
		}
		
		try {
			tq = internal.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			LinkedList<String> tmp;
			while (tqr.hasNext()) {
				bs = tqr.next();
				tmp = new LinkedList<String>();
				tmp.add(bs.getValue(SB).stringValue());
				tmp.add(stub);
				tmp.add(bs.getValue(OB).stringValue());
				ret.add(tmp);
			}
		}
		catch (MalformedQueryException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		} catch (QueryEvaluationException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		} catch (RepositoryException e) {
			LOG.fatal(MODULE_NAME + " " + query + " - " + e);
		}
	    return ret;
	}
    
    /**
     * Writes a query given a bind to retrieve, a context and a WHERE clause.
     * @param context Context on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return A full query.
     */
    private String writeQuery(String context, String where, String bind) {
    	String ret = "SELECT DISTINCT ?" + bind
    			+ (context.isEmpty() ? "" : " FROM <" + context + ">")
    			+ " WHERE " + where;
    	return ret;
    }
    
    /**
     * Retrieves multiple queries results based on a query pattern executed on
     * multiple contexts.
     * @param contexts Contexts on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return Results as a LinkedList of Strings.
     */
    private LinkedList<String> getMultipleResults(LinkedList<String> contexts, String where, String bind) {
    	LinkedList<String> ret = new LinkedList<String>();
    	
    	for (String context : contexts) {
    		ret.addAll(selectQuery(writeQuery(context, where, bind), bind));
    	}
    	return ret;
    }
    
    /**
     * Retrieves all of the classes used inside given contexts.
     * @param contexts The contexts to use.
     * @return A LinkedList of all of the classes used inside the contexts.
     */
	public final LinkedList<String> getAllClasses(LinkedList<String> contexts) {
		return getMultipleResults(contexts, CLASS_WHERE, OB);
	}
	
	/**
     * Retrieves all of the predicates used inside given contexts.
     * @param contexts The contexts to use.
     * @return A LinkedList of all of the predicates used inside the contexts.
     */
	public final LinkedList<String> getAllPredicates(LinkedList<String> contexts) {
		return getMultipleResults(contexts, PREDICATE_WHERE, PB);
	}
    
    //-------------------------------------------------------------------------
    // Namespaces management.
    //-------------------------------------------------------------------------
    
	//TODO Manage prefixes.
	
    //-------------------------------------------------------------------------
    // Launching management.
    //-------------------------------------------------------------------------
    
    /**
     * Imports a configuration file from the data fetched by a form.
     * @param name Name of the temp file to write on disc.
     * @param data File content.
     * @return A new File which contains a Silk configuration.
     */
    public final File importConfigFile(String name, InputStream data) {
    	File ret = null;
    	
    	if (LOG.isDebugEnabled()) {
    		LOG.debug(MODULE_NAME + " - Uploading new config file - " + name);
    	}
	    
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
			LOG.fatal(MODULE_NAME + " - Error while uploading file - " + e);
		}
		
		return ret;
    }
    
    public final File createConfigFile(Project proj,
    		// Source fields.
    		String sourceAddress,
    		String sourceQuery,
    		String sourceVariable,
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
    		String targetAddress,
    		String targetQuery,
    		String targetVariable,
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
    		String aggregation) {
    	// First we extract the three identifiers (source, target, interlink) from the sources' addresses.
    	String sourceId;
    	if (isFileSource(sourceAddress)) {
    		sourceId = extractFileSourceName(sourceAddress);
    	}
    	else if (isDataliftSource(proj, sourceAddress)) {
    		sourceId = proj.getSource(URI.create(sourceAddress)).getTitle();
    		sourceAddress = INTERNAL_URL;
    	}
    	else {
    		sourceId = extractEndpointSourceName(sourceAddress);
    	}
    	
    	String targetId;
    	if (isFileSource(targetAddress)) {
    		targetId = extractFileSourceName(targetAddress);
    	}
    	else if (isDataliftSource(proj, targetAddress)) {
    		//TODO Looks like a great idea but it might be better to filter it / use something else.
    		targetId = proj.getSource(URI.create(targetAddress)).getTitle();
    		targetAddress = INTERNAL_URL;
    	}
    	else {
    		targetId = extractEndpointSourceName(targetAddress);
    	}

    	String interlinkId = sourceId + "-and-" + targetId;
    	 	
    	File ret = ConfigurationFileWriter.createConfigFile(interlinkId, sourceId, sourceAddress, sourceQuery, sourceVariable, sourcePropertyFirst, sourceTransformationFirst, sourceRegexpTokenFirst, sourceStopWordsFirst, sourceSearchFirst, sourceReplaceFirst, sourcePropertySecund, sourceTransformationSecund, sourceRegexpTokenSecund, sourceStopWordsSecund, sourceSearchSecund, sourceReplaceSecund, sourcePropertyThird, sourceTransformationThird, sourceRegexpTokenThird, sourceStopWordsThird, sourceSearchThird, sourceReplaceThird, targetId, targetAddress, targetQuery, targetVariable, targetPropertyFirst, targetTransformationFirst, targetRegexpTokenFirst, targetStopWordsFirst, targetSearchFirst, targetReplaceFirst, targetPropertySecund, targetTransformationSecund, targetRegexpTokenSecund, targetStopWordsSecund, targetSearchSecund, targetReplaceSecund, targetPropertyThird, targetTransformationThird, targetRegexpTokenThird, targetStopWordsThird, targetSearchThird, targetReplaceThird, metricFirst, minFirst, maxFirst, unitFirst, curveFirst, weightFirst, thresholdFirst, metricSecund, minSecund, maxSecund, unitSecund, curveSecund, weightSecund, thresholdSecund, metricThird, minThird, maxThird, unitThird, curveThird, weightThird, thresholdThird, aggregation);
		
    	
    	if (LOG.isDebugEnabled()) {
    		LOG.debug(MODULE_NAME + " - Created new config file.");
    	}
    	
    	return ret;
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
				LOG.fatal(MODULE_NAME + " - Configuration file DOM parsing failed - " + e);
			} catch (SAXException e) {
				LOG.fatal(MODULE_NAME + " - Configuration file DOM parsing failed - " + e);
			} catch (ParserConfigurationException e) {
				LOG.fatal(MODULE_NAME + " - Configuration file DOM parsing failed - " + e);
			}
		}
		
		return errors;
	}
    
    /**
     * Tries to find errors in the script creation field before creating a configuration file.
     * TODO NPath of 611'545'806'144 ( 1'000'000'000 times too big).
     * TODO 250 lines instead of the maximum 150.
     * @return A list of errors in the given fields.
     */
    public final LinkedList<String> getErrorMessages(String sourceAddress, String sourceQuery, String sourceVariable, String sourcePropertyFirst, String sourceTransformationFirst, String sourceRegexpTokenFirst, String sourceStopWordsFirst, String sourceSearchFirst, String sourceReplaceFirst, String sourcePropertySecund, String sourceTransformationSecund, String sourceRegexpTokenSecund, String sourceStopWordsSecund, String sourceSearchSecund, String sourceReplaceSecund, String sourcePropertyThird, String sourceTransformationThird, String sourceRegexpTokenThird, String sourceStopWordsThird, String sourceSearchThird, String sourceReplaceThird, String targetAddress, String targetQuery, String targetVariable, String targetPropertyFirst, String targetTransformationFirst, String targetRegexpTokenFirst, String targetStopWordsFirst, String targetSearchFirst, String targetReplaceFirst, String targetPropertySecund, String targetTransformationSecund, String targetRegexpTokenSecund, String targetStopWordsSecund, String targetSearchSecund, String targetReplaceSecund, String targetPropertyThird, String targetTransformationThird, String targetRegexpTokenThird, String targetStopWordsThird, String targetSearchThird, String targetReplaceThird, String metricFirst, String minFirst, String maxFirst, String unitFirst, String curveFirst, String weightFirst, String thresholdFirst, String metricSecund, String minSecund, String maxSecund, String unitSecund, String curveSecund, String weightSecund, String thresholdSecund, String metricThird, String minThird, String maxThird, String unitThird, String curveThird, String weightThird, String thresholdThird, String aggregation) {
    	LinkedList<String> errors = new LinkedList<String>();
    	//TODO Might use a refactoring, but how to handle so many fields ?
    	
    	// String sourcePropertySecund, String sourceTransformationSecund, String sourceRegexpTokenSecund, String sourceStopWordsSecund, String sourceSearchSecund, String sourceReplaceSecund
    	// String sourcePropertyThird, String sourceTransformationThird, String sourceRegexpTokenThird, String sourceStopWordsThird, String sourceSearchThird, String sourceReplaceThird
    	// String targetPropertySecund, String targetTransformationSecund, String targetRegexpTokenSecund, String targetStopWordsSecund, String targetSearchSecund, String targetReplaceSecund
    	// String targetPropertyThird, String targetTransformationThird, String targetRegexpTokenThird, String targetStopWordsThird, String targetSearchThird, String targetReplaceThird

    	// String metricSecund, String minSecund, String maxSecund, String unitSecund, String curveSecund, String thresholdSecund
    	// String metricThird, String minThird, String maxThird, String unitThird, String curveThird, String thresholdThird
    	
    	// Addresses, variables, queries.
    	
    	if (isEmptyValue(sourceAddress)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.address.label") + "\" (" + getTranslatedResource("source.label") + ").");
    	}
    	if (isEmptyValue(targetAddress)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.address.label") + "\" (" + getTranslatedResource("target.label") + ").");
    	}
    	if (isEmptyValue(sourceVariable)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.variable.label") + "\" (" + getTranslatedResource("source.label") + ").");
    	}
    	else {
    		if (!isEmptyValue(sourceQuery) && !isValidQuery(sourceQuery, sourceVariable)) {
    			errors.add(getTranslatedResource("error.invalidquery") + " : \"" + sourceQuery + "\" (" + getTranslatedResource("source.label") + ") .");
    		}
    	}
    	if (isEmptyValue(targetVariable)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.variable.label") + "\" (" + getTranslatedResource("target.label") + ").");
    	} else {
    		if (!isEmptyValue(targetQuery) && !isValidQuery(targetQuery, targetVariable)) {
    			errors.add(getTranslatedResource("error.invalidquery") + " : \"" + targetQuery + "\" (" + getTranslatedResource("target.label") + ") .");
    		}
    	}
    	
    	// Comparison properties, transformations and additional fields for certain transformations only.
    	
    	if (isEmptyValue(sourcePropertyFirst)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.property.label") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    	}
    	else if (!isEmptyValue(sourceTransformationFirst)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!validTransformation(sourceTransformationFirst, "tokenize", sourceRegexpTokenFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    		else if (!validTransformation(sourceTransformationFirst, "removeValues", sourceStopWordsFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    		else if (!validTransformation(sourceTransformationFirst, "replace", sourceSearchFirst) || !validTransformation(sourceTransformationFirst, "replace", sourceReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    		else if (!validTransformation(sourceTransformationFirst, "regexReplace", sourceSearchFirst) || !validTransformation(sourceTransformationFirst, "regexReplace", sourceReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    	}
    	if (isEmptyValue(targetPropertyFirst)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.property.label") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    	} 
    	else if (!isEmptyValue(targetTransformationFirst)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!validTransformation(targetTransformationFirst, "tokenize", targetRegexpTokenFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    		else if (!validTransformation(targetTransformationFirst, "removeValues", targetStopWordsFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    		else if (!validTransformation(targetTransformationFirst, "replace", targetSearchFirst) || !validTransformation(targetTransformationFirst, "replace", targetReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    		else if (!validTransformation(targetTransformationFirst, "regexReplace", targetSearchFirst) || !validTransformation(targetTransformationFirst, "regexReplace", targetReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    	}
    	
    	// The first distance measure, which is mandatory.
    	
    	if (isEmptyValue(metricFirst)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("compare.measure.title") + "\" #1.");
    	}
    	else {
    		// Special distance measures with additional fields.
    		if (!validDistanceMeasure(metricFirst, "num", minFirst) || !validDistanceMeasure(metricFirst, "num", maxFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.num") + "\" #1.");
    		}
    		else if (!isEmptyValue(minFirst) && !isNumeric(minFirst) ||  !isEmptyValue(maxFirst) &&  !isNumeric(maxFirst)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("metric.title.num") + "\" #1.");
			}
    		
    		if (!validDistanceMeasure(metricFirst, "wgs84", curveFirst) || !validDistanceMeasure(metricFirst, "wgs84", unitFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.wgs84") + "\" #1.");
    		}
    		
    		if (isEmptyValue(thresholdFirst)) {
        		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("compare.measure.threshold") + "\" #1.");
        	}
    		else if (!isNumeric(thresholdFirst)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("compare.measure.threshold") + "\" #1.");
			}
    	}
    	
    	// We enter the realm of The-Optional-Fields With-Dependencies-If-Submitted.
    	// Secund comparison.
    	
    	if (!isEmptyValue(sourcePropertySecund) && !isEmptyValue(sourceTransformationSecund)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!validTransformation(sourceTransformationSecund, "tokenize", sourceRegexpTokenSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    		else if (!validTransformation(sourceTransformationSecund, "removeValues", sourceStopWordsSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    		else if (!validTransformation(sourceTransformationSecund, "replace", sourceSearchSecund) || !validTransformation(sourceTransformationSecund, "replace", sourceReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    		else if (!validTransformation(sourceTransformationSecund, "regexReplace", sourceSearchSecund) || !validTransformation(sourceTransformationSecund, "regexReplace", sourceReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    	}
    	
    	if (!isEmptyValue(targetPropertySecund) && !isEmptyValue(sourceTransformationSecund)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!validTransformation(targetTransformationSecund, "tokenize", targetRegexpTokenSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    		else if (!validTransformation(targetTransformationSecund, "removeValues", targetStopWordsSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    		else if (!validTransformation(targetTransformationSecund, "replace", targetSearchSecund) || !validTransformation(targetTransformationSecund, "replace", targetReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    		else if (!validTransformation(targetTransformationSecund, "regexReplace", targetSearchSecund) || !validTransformation(targetTransformationSecund, "regexReplace", targetReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    	}
    	
    	// The secund distance measure, which is optional.
    	
    	if (!isEmptyValue(metricSecund)) {
    		// Special distance measures with additional fields.
    		if (!validDistanceMeasure(metricSecund, "num", minSecund) || !validDistanceMeasure(metricSecund, "num", maxSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.num") + "\" #2.");
    		}
    		else if (!isEmptyValue(minSecund) && !isNumeric(minSecund) ||  !isEmptyValue(maxSecund) && !isNumeric(maxSecund)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("metric.title.num") + "\" #2.");
			}
    		
    		if (!validDistanceMeasure(metricSecund, "wgs84", curveSecund) || !validDistanceMeasure(metricSecund, "wgs84", unitSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.wgs84") + "\" #2.");
    		}
    		
    		if (isEmptyValue(thresholdSecund)) {
        		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("compare.measure.threshold") + "\" #2.");
        	}
    		else if (!isNumeric(thresholdSecund)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("compare.measure.threshold") + "\" #2.");
			}
    	}
    	
    	// Third comparison.
    	
    	if (!isEmptyValue(sourcePropertyThird) && !isEmptyValue(sourceTransformationThird)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!validTransformation(sourceTransformationThird, "tokenize", sourceRegexpTokenThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    		else if (!validTransformation(sourceTransformationThird, "removeValues", sourceStopWordsThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    		else if (!validTransformation(sourceTransformationThird, "replace", sourceSearchThird) || !validTransformation(sourceTransformationThird, "replace", sourceReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    		else if (!validTransformation(sourceTransformationThird, "regexReplace", sourceSearchThird) || !validTransformation(sourceTransformationThird, "regexReplace", sourceReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    	}
    	
    	if (!isEmptyValue(targetPropertyThird) && !isEmptyValue(sourceTransformationThird)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!validTransformation(targetTransformationThird, "tokenize", targetRegexpTokenThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    		else if (!validTransformation(targetTransformationThird, "removeValues", targetStopWordsThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    		else if (!validTransformation(targetTransformationThird, "replace", targetSearchThird) || !validTransformation(targetTransformationThird, "replace", targetReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    		else if (!validTransformation(targetTransformationThird, "regexReplace", targetSearchThird) || !validTransformation(targetTransformationThird, "regexReplace", targetReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    	}
    	
    	// The Third distance measure, which is optional.
    	
    	if (!isEmptyValue(metricThird)) {
    		// Special distance measures with additional fields.
    		if (!validDistanceMeasure(metricThird, "num", minThird) || !validDistanceMeasure(metricThird, "num", maxThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.num") + "\" #3.");
    		}
    		else if (!isEmptyValue(minThird) && !isNumeric(minThird) ||  !isEmptyValue(maxThird) &&  !isNumeric(maxThird)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("metric.title.num") + "\" #3.");
			}
    		
    		if (!validDistanceMeasure(metricThird, "wgs84", curveThird) || !validDistanceMeasure(metricThird, "wgs84", unitThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.wgs84") + "\" #3.");
    		}
    		
    		if (isEmptyValue(thresholdThird)) {
        		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("compare.measure.threshold") + "\" #3.");
        	}
    		else if (!isNumeric(thresholdThird)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("compare.measure.threshold") + "\" #3.");
			}
    	}
    	
    	
    	
    	if (!isEmptyValue(sourcePropertySecund) && !isEmptyValue(targetPropertySecund)) {
    		if (isEmptyValue(aggregation)) {
    			errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("aggregation.title") + "\".");
    		}
    		else {
    			// If aggregation is some kind of mean, we need weights to be used.
    			if (aggregation.toLowerCase().equals("average") || aggregation.toLowerCase().equals("geometricMean") || aggregation.toLowerCase().equals("quadraticMean")) {
    				if (isEmptyValue(weightFirst)) {
    					errors.add(getTranslatedResource("error.requiredfields.aggregate") + " : \"" + getTranslatedResource("compare.measure.weight") + "\" #1.");
    				}
    				else if (!isNumeric(weightFirst)) {
    					errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("compare.measure.weight") + "\" #1.");
    				}
    				if (isEmptyValue(weightSecund)) {
    					errors.add(getTranslatedResource("error.requiredfields.aggregate") + " : \"" + getTranslatedResource("compare.measure.weight") + "\" #2.");
    				}
    				else if (!isNumeric(weightSecund)) {
    					errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("compare.measure.weight") + "\" #2.");
    				}
    				if (!isEmptyValue(sourcePropertyThird) && !isEmptyValue(targetPropertyThird)) {
    					if (isEmptyValue(weightThird)) {
    						errors.add(getTranslatedResource("error.requiredfields.aggregate") + " : \"" + getTranslatedResource("compare.measure.weight") + "\" #3.");
    					}
    					else if (!isNumeric(weightThird)) {
        					errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("compare.measure.weight") + "\" #3.");
        				}
    				}
    			}
    		}
    	}
    	return errors;
    }
    
    /**
     * Launches the Silk engine with a given configuration.
     * @param config the Silk configuration file.
     * @param linkID the identifier of our interlink.
     * @param threads number of threads to allocate to Silk.
     * @param reload tells if the Silk cache has to be reloaded before exec.
     * @param validateFile tells if we have to check if the parameters are usable.
     * @return A list which contains the newly created triples.
     */
    public final LinkedList<LinkedList<String>> launchSilk(File config, String linkID, int threads, boolean reload, boolean validateFile) {
    	LinkedList<LinkedList<String>> ret = new LinkedList<LinkedList<String>>();
    	String resultPredicate = "<http://www.w3.org/2002/07/owl#sameAs>";
    	String resultQuery = "SELECT DISTINCT ?" + SB + " ?" + OB + " WHERE { ?" + SB + " " + resultPredicate + " ?" + OB + "}";
    	if (!validateFile || getErrorMessages(config, linkID).isEmpty()) {
	    	if (LOG.isInfoEnabled()) {
	    		LOG.info(MODULE_NAME + " - Launching Silk on " + config.getAbsolutePath() + " - " + linkID);
	    	}
	    	
	    	LinkedList<LinkedList<String>> before = pairSelectQuery(resultQuery, resultPredicate);
	    	
			Silk.executeFile(config, linkID, threads, reload);
			
	    	ret = pairSelectQuery(resultQuery, resultPredicate);
			// We'll only display new sameAs links.
	    	ret.removeAll(before);
    	}
    	else {
    		// Should never happen.
    		LinkedList<String> error = new LinkedList<String>();
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		error.add(getTranslatedResource("error.label"));
    		ret.add(error);
    		
    		if (LOG.isInfoEnabled()) {
            	LOG.info(MODULE_NAME + " interconnection KO.");
            }
    	}
    	
    	return ret;
    }
    
    /**
     * Launches the Silk engine with a given configuration.
     * @param config the Silk configuration file.
     * @param threads number of threads to allocate to Silk.
     * @param reload tells if the Silk cache has to be reloaded before exec.
     * @param validateFile tells if we have to check if the parameters are usable.
     * @return A list which contains the newly created triples.
     */
    public final LinkedList<LinkedList<String>> launchSilk(File config, int threads, boolean reload, boolean validateFile) {
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
			LOG.fatal(MODULE_NAME + " - Configuration file DOM parsing failed - " + e);
		} catch (SAXException e) {
			LOG.fatal(MODULE_NAME + " - Configuration file DOM parsing failed - " + e);
		} catch (ParserConfigurationException e) {
			LOG.fatal(MODULE_NAME + " - Configuration file DOM parsing failed - " + e);
		}
    	
    	return interlinkId != null ? launchSilk(config, interlinkId, threads, reload, validateFile) : null;
    }
}

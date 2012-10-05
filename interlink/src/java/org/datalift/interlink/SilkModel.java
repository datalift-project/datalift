/*
 * Copyright / LIRMM 2012
 * Contributor(s) : T. Colas, F. Scharffe
 *
 * Contact: thibaud.colas@etud.univ-montp2.fr
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.interlink;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.LinkedList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.TransformedRdfSource;
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

/**
 * A {@link ProjectModule project module} that uses the Silk link generation
 * framework to generate links between two datasets.
 * This class handles Silk interlink constraints.
 *
 * @author tcolas
 * @version 03102012
 */
public class SilkModel extends InterlinkingModel {
	
	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------
    
    /** The default fallback number of threads to use when executing Silk. */
    public static final int DEFAULT_NB_THREADS = 1;
    /** The default fallback way to manage cache to use when executing Silk. */
    public static final boolean DEFAULT_RELOAD_CACHE = true;
    /** The default fallback max number of links created per RDF entity. */
    public static final int DEFAULT_MAX_LINKS = 0;
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new SilkModel instance.
     * @param name Name of the module.
     */
    public SilkModel(String name) {
    	super(name);
    }
    
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
    
    public final boolean isDataliftSource(Project proj, String src) {
    	return getSourcesURIs(proj).contains(src);
    }
    
    public static final boolean isFileSource(String src) {
    	return !src.startsWith("http");
    }
    
    public static final String extractFileSourceName(String src) {
    	return removeSpecialChars(src.substring(src.lastIndexOf("/")));
    }
    
    public static final String extractEndpointSourceName(String src) {
    	// Strip http://www.
    	String ret = src.substring(7).replace("www.", "");
    	return removeSpecialChars(ret);
    }
    
    public static final String extractDataliftSourceName(Project proj, String src) {
    	//TODO Looks like a great idea but it might be better to filter it / use something else.
       return removeSpecialChars(proj.getSource(URI.create(src)).getTitle());
    }
    
    //-------------------------------------------------------------------------
    // Value validation.
    //-------------------------------------------------------------------------
    
    /**
     * Checks whether a query seems to have a valid syntax.
     * @param query SPARQL Query WHERE statement to check.
     * @param var Return variable.
     * @return True if query is valid.
     */
    public boolean isValidQuery(String query, String var) {
    	// Might make use of a better validation.
    	return query.contains("?" + var);
    }
    
    /**
     * Checks that if a special transformation function is used, the required
     * additional fields are submitted too.
     * @param transformation A transformation to check.
     * @param specialCase Its special case.
     * @param parameter The required additional field.
     * @return True if transf != special case OR transf = special case && additionalField not empty.
     */
    private boolean isValidUseCase(String givenCase, String specialCase, String parameter) {
    	// OK if case not special OR case = special && parameter not empty.
    	return !givenCase.equals(specialCase)
    		|| !isEmptyValue(parameter);
    }
	
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
    
    public final File createConfigFile(Project proj,
    		// Source fields.
    		String sourceAddress,
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
    		String targetAddress,
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
    		String aggregation) {
    	// First we extract the three identifiers (source, target, interlink) from the sources' addresses.
    	String sourceId;
    	String sourceContext = "";
    	if (isFileSource(sourceAddress)) {
    		sourceId = extractFileSourceName(sourceAddress);
    	}
    	else if (isDataliftSource(proj, sourceAddress)) {
    		sourceContext = sourceAddress;
    		sourceId = extractDataliftSourceName(proj, sourceAddress);
    		sourceAddress = INTERNAL_URL;
    	}
    	else {
    		sourceId = extractEndpointSourceName(sourceAddress);
    	}
    	
    	String targetId;
    	String targetContext = "";
    	if (isFileSource(targetAddress)) {
    		targetId = extractFileSourceName(targetAddress);
    	}
    	else if (isDataliftSource(proj, targetAddress)) {
    		targetContext = targetAddress;
    		targetId = extractDataliftSourceName(proj, targetAddress);
    		targetAddress = INTERNAL_URL;
    	}
    	else {
    		targetId = extractEndpointSourceName(targetAddress);
    	}

    	String interlinkId = sourceId + "-and-" + targetId;
    	
    	String newGraphURI = targetContext + "-silk";
    	 	
    	File ret = ConfigurationFileWriter.createConfigFile(interlinkId, sourceId, sourceAddress, sourceContext, sourceQuery, sourcePropertyFirst, sourceTransformationFirst, sourceRegexpTokenFirst, sourceStopWordsFirst, sourceSearchFirst, sourceReplaceFirst, sourcePropertySecund, sourceTransformationSecund, sourceRegexpTokenSecund, sourceStopWordsSecund, sourceSearchSecund, sourceReplaceSecund, sourcePropertyThird, sourceTransformationThird, sourceRegexpTokenThird, sourceStopWordsThird, sourceSearchThird, sourceReplaceThird, 
    														targetId, targetAddress, targetContext, targetQuery, targetPropertyFirst, targetTransformationFirst, targetRegexpTokenFirst, targetStopWordsFirst, targetSearchFirst, targetReplaceFirst, targetPropertySecund, targetTransformationSecund, targetRegexpTokenSecund, targetStopWordsSecund, targetSearchSecund, targetReplaceSecund, targetPropertyThird, targetTransformationThird, targetRegexpTokenThird, targetStopWordsThird, targetSearchThird, targetReplaceThird, 
    														metricFirst, minFirst, maxFirst, unitFirst, curveFirst, weightFirst, thresholdFirst, metricSecund, minSecund, maxSecund, unitSecund, curveSecund, weightSecund, thresholdSecund, metricThird, minThird, maxThird, unitThird, curveThird, weightThird, thresholdThird, aggregation, newGraphURI);
		
    	
    	LOG.debug("Created new Silk config file.");
    	
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
     * Tries to find errors in the script creation field before creating a configuration file.
     * TODO NPath of 611'545'806'144 ( 1'000'000'000 times too big).
     * TODO 250 lines instead of the maximum 150.
     * @return A list of errors in the given fields.
     */
    public final LinkedList<String> getErrorMessages(String sourceAddress, String sourceQuery, String sourcePropertyFirst, String sourceTransformationFirst, String sourceRegexpTokenFirst, String sourceStopWordsFirst, String sourceSearchFirst, String sourceReplaceFirst, String sourcePropertySecund, String sourceTransformationSecund, String sourceRegexpTokenSecund, String sourceStopWordsSecund, String sourceSearchSecund, String sourceReplaceSecund, String sourcePropertyThird, String sourceTransformationThird, String sourceRegexpTokenThird, String sourceStopWordsThird, String sourceSearchThird, String sourceReplaceThird, String targetAddress, String targetQuery, String targetPropertyFirst, String targetTransformationFirst, String targetRegexpTokenFirst, String targetStopWordsFirst, String targetSearchFirst, String targetReplaceFirst, String targetPropertySecund, String targetTransformationSecund, String targetRegexpTokenSecund, String targetStopWordsSecund, String targetSearchSecund, String targetReplaceSecund, String targetPropertyThird, String targetTransformationThird, String targetRegexpTokenThird, String targetStopWordsThird, String targetSearchThird, String targetReplaceThird, String metricFirst, String minFirst, String maxFirst, String unitFirst, String curveFirst, String weightFirst, String thresholdFirst, String metricSecund, String minSecund, String maxSecund, String unitSecund, String curveSecund, String weightSecund, String thresholdSecund, String metricThird, String minThird, String maxThird, String unitThird, String curveThird, String weightThird, String thresholdThird, String aggregation) {
    	LinkedList<String> errors = new LinkedList<String>();
    	//TODO Might use a refactoring, but how to handle so many fields ?
    	//TODO For now we only check for empty values / dependencies, but we also
    	// need to check values, types and logic (i.e numerical transformations are for numbers).
    	
    	// Addresses, variables, queries.
    	
    	if (isEmptyValue(sourceAddress)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.address.label") + "\" (" + getTranslatedResource("source.label") + ").");
    	}
    	if (isEmptyValue(targetAddress)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.address.label") + "\" (" + getTranslatedResource("target.label") + ").");
    	}
    	
    	// Comparison properties, transformations and additional fields for certain transformations only.
    	
    	if (isEmptyValue(sourcePropertyFirst)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.property.label") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    	}
    	else if (!isEmptyValue(sourceTransformationFirst)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!isValidUseCase(sourceTransformationFirst, "tokenize", sourceRegexpTokenFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    		else if (!isValidUseCase(sourceTransformationFirst, "removeValues", sourceStopWordsFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    		else if (!isValidUseCase(sourceTransformationFirst, "replace", sourceSearchFirst) || !isValidUseCase(sourceTransformationFirst, "replace", sourceReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    		else if (!isValidUseCase(sourceTransformationFirst, "regexReplace", sourceSearchFirst) || !isValidUseCase(sourceTransformationFirst, "regexReplace", sourceReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("source.label") + ") #1.");
    		}
    	}
    	if (isEmptyValue(targetPropertyFirst)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("dataset.property.label") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    	} 
    	else if (!isEmptyValue(targetTransformationFirst)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!isValidUseCase(targetTransformationFirst, "tokenize", targetRegexpTokenFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    		else if (!isValidUseCase(targetTransformationFirst, "removeValues", targetStopWordsFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    		else if (!isValidUseCase(targetTransformationFirst, "replace", targetSearchFirst) || !isValidUseCase(targetTransformationFirst, "replace", targetReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    		else if (!isValidUseCase(targetTransformationFirst, "regexReplace", targetSearchFirst) || !isValidUseCase(targetTransformationFirst, "regexReplace", targetReplaceFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("target.label") + ") #1.");
    		}
    	}
    	
    	// The first distance measure, which is mandatory.
    	
    	if (isEmptyValue(metricFirst)) {
    		errors.add(getTranslatedResource("error.emptyfield") + " : \"" + getTranslatedResource("compare.measure.title") + "\" #1.");
    	}
    	else {
    		// Special distance measures with additional fields.
    		if (!isValidUseCase(metricFirst, "num", minFirst) || !isValidUseCase(metricFirst, "num", maxFirst)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.num") + "\" #1.");
    		}
    		else if (!isEmptyValue(minFirst) && !isNumeric(minFirst) ||  !isEmptyValue(maxFirst) &&  !isNumeric(maxFirst)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("metric.title.num") + "\" #1.");
			}
    		
    		if (!isValidUseCase(metricFirst, "wgs84", curveFirst) || !isValidUseCase(metricFirst, "wgs84", unitFirst)) {
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
    		if (!isValidUseCase(sourceTransformationSecund, "tokenize", sourceRegexpTokenSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    		else if (!isValidUseCase(sourceTransformationSecund, "removeValues", sourceStopWordsSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    		else if (!isValidUseCase(sourceTransformationSecund, "replace", sourceSearchSecund) || !isValidUseCase(sourceTransformationSecund, "replace", sourceReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    		else if (!isValidUseCase(sourceTransformationSecund, "regexReplace", sourceSearchSecund) || !isValidUseCase(sourceTransformationSecund, "regexReplace", sourceReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("source.label") + ") #2.");
    		}
    	}
    	
    	if (!isEmptyValue(targetPropertySecund) && !isEmptyValue(sourceTransformationSecund)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!isValidUseCase(targetTransformationSecund, "tokenize", targetRegexpTokenSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    		else if (!isValidUseCase(targetTransformationSecund, "removeValues", targetStopWordsSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    		else if (!isValidUseCase(targetTransformationSecund, "replace", targetSearchSecund) || !isValidUseCase(targetTransformationSecund, "replace", targetReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    		else if (!isValidUseCase(targetTransformationSecund, "regexReplace", targetSearchSecund) || !isValidUseCase(targetTransformationSecund, "regexReplace", targetReplaceSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("target.label") + ") #2.");
    		}
    	}
    	
    	// The secund distance measure, which is optional.
    	
    	if (!isEmptyValue(metricSecund)) {
    		// Special distance measures with additional fields.
    		if (!isValidUseCase(metricSecund, "num", minSecund) || !isValidUseCase(metricSecund, "num", maxSecund)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.num") + "\" #2.");
    		}
    		else if (!isEmptyValue(minSecund) && !isNumeric(minSecund) ||  !isEmptyValue(maxSecund) && !isNumeric(maxSecund)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("metric.title.num") + "\" #2.");
			}
    		
    		if (!isValidUseCase(metricSecund, "wgs84", curveSecund) || !isValidUseCase(metricSecund, "wgs84", unitSecund)) {
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
    		if (!isValidUseCase(sourceTransformationThird, "tokenize", sourceRegexpTokenThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    		else if (!isValidUseCase(sourceTransformationThird, "removeValues", sourceStopWordsThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    		else if (!isValidUseCase(sourceTransformationThird, "replace", sourceSearchThird) || !isValidUseCase(sourceTransformationThird, "replace", sourceReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    		else if (!isValidUseCase(sourceTransformationThird, "regexReplace", sourceSearchThird) || !isValidUseCase(sourceTransformationThird, "regexReplace", sourceReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("source.label") + ") #3.");
    		}
    	}
    	
    	if (!isEmptyValue(targetPropertyThird) && !isEmptyValue(sourceTransformationThird)) {
    		// Special transformations with additional fields which are not marked as required using the HTML5 required attribute.
    		if (!isValidUseCase(targetTransformationThird, "tokenize", targetRegexpTokenThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.tokenize") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    		else if (!isValidUseCase(targetTransformationThird, "removeValues", targetStopWordsThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.removeValues") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    		else if (!isValidUseCase(targetTransformationThird, "replace", targetSearchThird) || !isValidUseCase(targetTransformationThird, "replace", targetReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.replace") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    		else if (!isValidUseCase(targetTransformationThird, "regexReplace", targetSearchThird) || !isValidUseCase(targetTransformationThird, "regexReplace", targetReplaceThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.transform") + " : \"" + getTranslatedResource("function.title.regexReplace") + "\" (" + getTranslatedResource("target.label") + ") #3.");
    		}
    	}
    	
    	// The Third distance measure, which is optional.
    	
    	if (!isEmptyValue(metricThird)) {
    		// Special distance measures with additional fields.
    		if (!isValidUseCase(metricThird, "num", minThird) || !isValidUseCase(metricThird, "num", maxThird)) {
    			errors.add(getTranslatedResource("error.requiredfields.compare") + " : \"" + getTranslatedResource("metric.title.num") + "\" #3.");
    		}
    		else if (!isEmptyValue(minThird) && !isNumeric(minThird) ||  !isEmptyValue(maxThird) &&  !isNumeric(maxThird)) {
				errors.add(getTranslatedResource("error.notnumericfield") + " : \"" + getTranslatedResource("metric.title.num") + "\" #3.");
			}
    		
    		if (!isValidUseCase(metricThird, "wgs84", curveThird) || !isValidUseCase(metricThird, "wgs84", unitThird)) {
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
     * @param proj current project;
     * @param config the Silk configuration file.
     * @param linkID the identifier of our interlink.
     * @param threads number of threads to allocate to Silk.
     * @param reload tells if the Silk cache has to be reloaded before exec.
     * @param validateFile tells if we have to check if the parameters are usable.
     * @return A list which contains the newly created triples.
     */
    public final LinkedList<LinkedList<String>> launchSilk(Project proj, String targetContext, File config, String linkID, int threads, boolean reload, boolean validateFile) {
    	LinkedList<LinkedList<String>> ret = new LinkedList<LinkedList<String>>();
//    	String resultPredicate = "<http://www.w3.org/2002/07/owl#sameAs>";
//    	String resultQuery = "SELECT DISTINCT ?" + SB + " ?" + OB + " WHERE { ?" + SB + " " + resultPredicate + " ?" + OB + "}";
    	if (!validateFile || getErrorMessages(config, linkID).isEmpty()) {
    		
    		LOG.info("Launching Silk on " + config.getAbsolutePath() + " - " + linkID);
	    	
			try {
				String newSourceURI = "";
				if (targetContext != null) {
					newSourceURI = targetContext + "-silk";
					Source parent = proj.getSource(targetContext);
					addResultSource(proj, parent, parent.getTitle() + "-silk", new URI(newSourceURI));
					
					RepositoryConnection cnx = INTERNAL_REPO.newConnection();
					// Copy all of the data to the new graph.
					String updateQy = "INSERT {GRAPH <" + newSourceURI + "> {?s ?p ?o}} WHERE {GRAPH <" + targetContext + "> {?s ?p ?o}}";
					Update up = cnx.prepareUpdate(QueryLanguage.SPARQL, updateQy);
					up.execute();
				}
				else {
					String tmpURI = proj.getSources().iterator().next().getUri();
					newSourceURI = tmpURI.substring(0, tmpURI.lastIndexOf("/") + 1) + "custom-silk-result";
					addResultSource(proj, proj.getSources().iterator().next(), "custom-silk-result", new URI(newSourceURI));
				}
			} 
			catch (IOException e) { LOG.fatal("Silk Configuration file execution failed - " + e); } 
			catch (URISyntaxException e) { LOG.fatal("Silk Configuration file execution failed - " + e); }
			catch (UpdateExecutionException e) { LOG.fatal("Silk Configuration file execution failed - " + e); } 
			catch (RepositoryException e) { LOG.fatal("Silk Configuration file execution failed - " + e); } 
			catch (MalformedQueryException e) { LOG.fatal("Silk Configuration file execution failed - " + e); }
	    	
			Silk.executeFile(config, linkID, threads, reload);
    	}
    	else {
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
     * @param config the Silk configuration file.
     * @param threads number of threads to allocate to Silk.
     * @param reload tells if the Silk cache has to be reloaded before exec.
     * @param validateFile tells if we have to check if the parameters are usable.
     * @return A list which contains the newly created triples.
     */
    public final LinkedList<LinkedList<String>> launchSilk(Project proj, String targetContext, File config, int threads, boolean reload, boolean validateFile) {
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
    	
    	return interlinkId != null ? launchSilk(proj, targetContext, config, interlinkId, threads, reload, validateFile) : null;
    }
}

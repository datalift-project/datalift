package org.datalift.geoconverter.usgs.gml.parsers;



import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Set;


import org.datalift.geoconverter.usgs.gml2rdf.gui.ConfigurationEditor;
import org.datalift.geoconverter.usgs.gml2rdf.gui.Filters;
import org.datalift.geoconverter.usgs.rdf.util.Config;
import org.datalift.geoconverter.usgs.rdf.util.FeatureType;
import org.geotools.xml.StreamingParser;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.vocabulary.RDF;
import com.vividsolutions.jts.geom.Geometry;

/**
 * Class for parsing through GML files to retrieve the attributes contained in
 * the file.
 * Loads a GML file using the streaming Parser. If the file contains a
 * feature type with no configuration the parser creates a default
 * configuration and displays the configuration editor for the feature type.
 * The configuration associated with the feature is used to extract all
 * attributes in the GML simple feature's property collection and convert
 * to an RDF model.
 * @author Andrew Bulen
 */
public class FeatureParser {
	/** stores all feature configurations used for the conversion. */
	private Hashtable<String, FeatureType> m_featureConfigs =
		new Hashtable<String, FeatureType>();
	/** folder in which the configuration files are saved. */
	public static final String defaultConfigPath = "config/";
	/** default CEGIS RDF URI. */
	private String rdfNameSpace = "http://cegis.usgs.gov/rdf/";
	/** default URI for RDF data, updated depending on feature type. */
	private String defaultNameSpace = rdfNameSpace;
	/**
	 * retrieves the string representing the default namespace.
	 * @return URI of default NS
	 */
	public final String defaultNS() {
		return defaultNameSpace;
	}
    /**
     * loads any stored feature configurations from the input directory.
     * @param configPath folder containing configuration files
     * @throws Exception exception from featureType loadFromFile
     */
    public final void loadFeatureConfigurations(final String configPath)
    throws Exception {
		File cPath = new File(configPath);
		if (cPath.exists()) {
			String[] files = cPath.list(Filters.configFilter);
			if (files.length > 0) {
				for (String iFile : files) {
					File cFile = new File(cPath.getPath() + "/" + iFile);
					FeatureType ft = new FeatureType();
					ft.loadFromFile(cFile.getPath());
					String cName = cFile.getName();
					this.m_featureConfigs.put(cName.substring(0,
							cName.indexOf(".conf")), ft);
				}
			}
		}
    }
    /**
     * retrieves the table of configurations.
     * @return table of configurations
     */
    public final Hashtable<String, FeatureType> getConfigs() {
    	return m_featureConfigs;
    }
    /**
     * saves all feature configurations to the output directory.
     * @param configPath output folder for the configuration files
     * @throws IOException exception from DataOutputStream error
     */
    public final void saveFeatureConfigurations(final String configPath)
    throws IOException {
    	// check for configuration file location and create if one doesn't exist
    	File cPath = new File(configPath);
		if (!cPath.exists()) {
			cPath.mkdirs();
		}
		Set<String> keys = this.m_featureConfigs.keySet();
		DataOutputStream cout = null;
		// loop through set of feature configurations and save to file
		for (String featureKey : keys) {
			try {
				cout = new DataOutputStream(new BufferedOutputStream(
					new FileOutputStream(configPath + featureKey + ".conf")));
				FeatureType ft = this.m_featureConfigs.get(featureKey);
				cout.writeBytes(ft.name() + "\n");
				HashMap<String, Config> atts = ft.getAttributes();
				Set<String> atKeys = atts.keySet();
				for (String atName : atKeys) {
					Config atConf = atts.get(atName);
					String rl = "L";
					if (atConf.getIsResource()) {
						rl = "R";
					}
					cout.writeBytes(atName + "|" + rl + "|" + 
						atConf.getNamespace() + "|" + atConf.getParent() +
							"|" + atConf.getPredicate() + "\n");
				}
			} finally {
				cout.close();
			}
		}
    }

    /**
     * parses all GML files contained within an input directory.
     * @param directory path to the folder of GML files
     * @throws Exception exception from streamParseToRDF error
     */
    public final Model parseDirectory(final String directory) throws Exception {
    	File path = new File(directory);
		File[] files;
	// create an array containing all the filenames in the input directory
		files = path.listFiles();
		// loop through each file and convert valid N3 files
		for (File f : files) {
			if (f.isFile()) {
				String fName = f.getName();
				if (fName.contains(".gml") || fName.contains(".xml")) {
					Model m = this.streamParseToRDF(f.getPath());
					return m;
				}
			}
		}
		return null;
    }
    /**
     * parses through a single GML file and adds the data to a model.
     * @param gmlFile input GML file
     * @return model containing the data from the GML
     * @throws Exception exception from IO, or Parser errors
     */
    public final Model streamParseToRDF(final String gmlFile) throws Exception {
    	Model model = ModelFactory.createMemModelMaker().createFreshModel();

    	// update namespace
    	File file = new File(gmlFile);
    	String fName = file.getName().substring(0, file.getName().indexOf("."));
    	fName = fName.split("_")[0].toLowerCase();
   		this.defaultNameSpace = this.rdfNameSpace + fName + "#";

    	// load any saved configurations
    	this.loadFeatureConfigurations(defaultConfigPath);

    	// set up GML parser
    	InputStream in = null;
    	try {
	    	in = new FileInputStream(gmlFile);
	    	org.geotools.xml.Configuration gml =
	    		new org.geotools.gml2.GMLConfiguration();
	    	StreamingParser parser
	    		= new StreamingParser(gml, in, SimpleFeature.class);
	    	SimpleFeature f = null;
		    // parse through each feature and add to model

		    while ((f = (SimpleFeature) parser.parse()) != null) {
		    	String fname = f.getFeatureType().getName().getLocalPart();
		    	// check for feature configurations
		    	if (!this.m_featureConfigs.containsKey(fname)) {
		    		this.addFeatureConfig(f);
		    	}
		    	// copy new data and NS prefixes into model
		    	Model fmodel = this.featureToModel(f);
		    	model.setNsPrefixes(fmodel.getNsPrefixMap());
		    	model.add(fmodel);
		    }
    	} finally {
    		in.close();
    	}
    	return model;
    }
    /**
     * creates a new feature configuration for any feature types not loaded.
     * @param f the simple feature of the type the configuration is for
     * @throws Exception IO exception
     */
    private void addFeatureConfig(final SimpleFeature f)
    throws Exception {
    	String propType = f.getType().getName().getLocalPart();
		String typeName = null;
		if (propType.split("_").length > 1) {
			typeName = propType.split("_")[1];
		} else {
			typeName = propType;
		}

       	Collection<? extends Property> properties = f.getValue();
       	FeatureType featureProps = new FeatureType();
       	featureProps.setName(typeName);
       	featureProps.addAttribute(typeName, new Config(true,
       			defaultNameSpace.replace("#", "/featureID#"), " ", " "));
       	featureProps.addAttribute("RDFType", new Config(false,
       		" ", typeName, defaultNameSpace.replace("#", "/" + typeName + "#")));
       	// iterate through all attributes of the feature
       	for (Property property : properties) {
       		if (property.getValue() != null) {
       			Config c = new Config();
       			c.setPredicate(defaultNameSpace);
       			c.setParent(featureProps.name());
       			featureProps.addAttribute(property.getName().toString(), c);
           	}
       	}
       	// Open and run the configuration editor
       	ConfigurationEditor ce = new ConfigurationEditor(propType, featureProps);
       	Thread ceThread = new Thread(ce);
       	ceThread.start();
       	try {
       		ceThread.join();
       	} catch (Exception e) {
       		System.out.println(propType + " configuration finished");
       	}
       	// load the edited configuration and add to set
       	featureProps.loadFromFile(defaultConfigPath + propType + ".conf");
       	this.m_featureConfigs.put(propType, featureProps);
    }
	/**
     * searches the simple feature for the property containing the desired ID.
     * @param sf simple feature being processed
     * @return the ID representing the feature, gml:id if none found
     */
    public final String getID(final SimpleFeature sf) {
    	String id = null;
    	String ft = sf.getFeatureType().getName().getLocalPart();
    	String idf = this.m_featureConfigs.get(ft).uidField();
    	if (!idf.isEmpty()) {
			id = sf.getAttribute(idf).toString();
		}
    	if (id == null) {
    		id = sf.getID();
    	}
    	return id;
    }
    /**
     * loads data from a single feature into a RDF model.
     * @param f Simple feature containing the data to be loaded
     * @return RDF model of the feature
     * @throws Exception missing configuration error
     */
    public final Model featureToModel(final SimpleFeature f) throws Exception {
    	Model model = ModelFactory.createMemModelMaker().createFreshModel();

    	String fID = getID(f);
    	String featureType = f.getType().getName().getLocalPart();
    	FeatureType ft = this.m_featureConfigs.get(featureType);

    	if (!model.getNsPrefixMap().containsKey("Feature")) {
    		model.setNsPrefix("Feature",
    				ft.getAttributeConfig(ft.name()).getNamespace());
		}
    	HashMap<String, Resource> resourceMap = new HashMap<String, Resource>();
    	// add feature resource to model
		try { //parse as integer to remove leading 0s
			resourceMap.put(ft.name(), model.createResource(
					ft.getAttributeConfig(ft.name()).getNamespace()
					+ "_" + Integer.parseInt(fID.replace(" ", "_"))));
		} catch (Exception e) {
			resourceMap.put(ft.name(), model.createResource(
					ft.getAttributeConfig(ft.name()).getNamespace()
					+ "_" + fID.trim().replace(" ", "_")));
		}
		// add RDF type to feature
		if (ft.getAttributes().containsKey("RDFType")) {
			Config conf = ft.getAttributeConfig("RDFType");
			if (conf.getParent() != null
					&& resourceMap.containsKey(conf.getParent())) {
				model.add(resourceMap.get(conf.getParent()), RDF.type,
						model.createResource(conf.getPredicate()));
			}
		}
		
    	Collection<?extends Property> properties = f.getValue();
    	for (Property property : properties) {
    		String propName = property.getName().toString();
    		Set<String> fConfigs = ft.getAttributes().keySet();
    		if (fConfigs.contains(propName)) {
    			if (property.getValue() != null) {
    				/// add property to model
    				Object att = property.getValue();

    				if (att == null || att.toString().equalsIgnoreCase("")
    						|| att.toString().equalsIgnoreCase("{}")) {
    					continue;
    				} //do not add empty nodes
    				Config c = ft.getAttributeConfig(propName);
    				
    				if (c == null) {
    					throw new Exception(propName
    							+ " not found in config file.");
    				}
    				if (c.getIsResource()) {
    					//error checking
    					if (c.getParent() == null
    							|| c.getParent().equalsIgnoreCase("")) {
    						//has no parent
    						try { //parse as integer to remove leading 0s
    							resourceMap.put(propName, model.createResource(
    								c.getNamespace() + "_"
    								+ Integer.parseInt(att.toString().replace(" ", "_"))));
    						} catch (Exception e) {
    							resourceMap.put(propName,
    								model.createResource(c.getNamespace()
    									+ att.toString().replace(" ", "_")));
    						}
    					}
    					else { //if this resource has a parent
    						//requires a predicate and that the parent exists
    						if (c.getPredicate() == null
    							|| c.getPredicate().equalsIgnoreCase("")) {
    							//System.out.println(schema[i]);
    							continue;
    						}
    						if (resourceMap.get(c.getParent()) == null) {
    							//System.out.println(schema[i]);
    							continue;
    						}
    						try { //parse as integer to remove leading 0s
    							model.add(resourceMap.get(c.getParent()),
    								model.createProperty(c.getPredicate()),
    								model.createResource(c.getNamespace() + "_"
    									+ Integer.parseInt(
    										att.toString().replace(" ", "_"))));
    						} catch (Exception e) {
    							model.add(resourceMap.get(c.getParent()),
    								model.createProperty(c.getPredicate()),
    								model.createResource(c.getNamespace()
    										+ att.toString().replace(" ", "_")));
    						}
    					}
    					String ns = c.getPredicate().substring(
    							c.getPredicate().indexOf("#") + "#".length());
    					if (!model.getNsPrefixMap().containsKey(ns)) {
    						if (!model.getNsPrefixMap().containsValue(
    								c.getNamespace())) {
    								model.setNsPrefix(ns, c.getNamespace());
    						}
    					}
    				}
    				else {
    					//if current column is a literal
    					//needs a parent and predicate
    					if (resourceMap.get(c.getParent()) == null) {
    						//System.out.println(schema[i]);
    						continue;
    					}
    					if (c.getPredicate() == null
    							|| c.getPredicate().equalsIgnoreCase("")) {
    						//System.out.println(schema[i]);
    						continue;
    					}
    					model.add(resourceMap.get(c.getParent()),
    						model.createProperty(c.getPredicate()),
    						model.createTypedLiteral(att));
    				}
    			}
    		}
    	}
    	return model;
    }
}

/**
 * @file FeatureType.java
 * @author Andrew Bulen
 * @brief stores all attribute names and configurations for a given feature type
 */
package org.datalift.geoconverter.usgs.rdf.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Container for storing the configurations for each attribute of a
 * given feature type as well as which field represents a unique
 * identifier for the feature and the feature name.
 * @author Andrew Bulen
 */
public class FeatureType {
	/** name of the feature type. */
	private String m_name = null;
	/** field used to determine the Unique ID of the feature. */
	private String m_idField = null;
	/** table of attribute configurations. */
	private HashMap<String, Config> m_attributes = null;
	
	/** default constructor. */
	public FeatureType() {
		this.m_name = "";
		this.m_idField = "";
		this.m_attributes = new HashMap<String, Config>();
	}
	/** initialize featureType with a name.
	 * @param name name of the feature type
	 */
	public FeatureType(final String name) {
		this.m_name = name;
		this.m_idField = "OBJECTID";
		this.m_attributes = new HashMap<String, Config>();
	}
	/** copy constructor.
	 * @param ft feature type that is copied
	 */
	public FeatureType(final FeatureType ft) {
		this.m_name = ft.m_name;
		this.m_idField = ft.m_idField;
		this.m_attributes = ft.m_attributes;
	}
	/**
	 * initialize the feature type with a name and a set of properties.
	 * @param name name of the feature type
	 * @param attributes map of attribute configurations
	 */
	public FeatureType(final String name, final Map<? extends String,
			? extends Config> attributes) {
		this.m_attributes = new HashMap<String, Config>(attributes);
		this.m_name = name;
		this.m_idField = "";
	}
	/** initializes the feature type using the input from a
	 * configuration file.
	 * @param cFile configuration file loaded
	 * @throws Exception loadFromFile IO exception
	 */
	public FeatureType(final File cFile) throws Exception {
		this.loadFromFile(cFile.getPath());
	}
	/**
	 * sets the name of the feature type.
	 * @param name input string to change name to
	 */
	public final void setName(final String name) {
		m_name = name;
	}
	/** retrieves the name of the feature type.
	 * @return name of the feature type
	 */
	public final String name() { return m_name; }
	/** sets the unique identifying field for the feature
	 *  (ComID, HUC, etc. ).
	 *  @param id field containing the unique ID
	 *  */
	public final void setIDField(final String id) { this.m_idField = id; }
	/** retrieves the attribute name that has the unique id
	 * for the feature.
	 * @return field containing the unique ID
	 */
	public final String uidField() { return this.m_idField; }
	/**
	 * adds a table of attribute configurations to the set.
	 * @param attributes configuration table
	 */
	public final void addAttributes(final Map<? extends String,
			? extends Config> attributes) {
		this.m_attributes.putAll(attributes);
	}
	/**
	 * retrieves the attribute configurations table.
	 * @return table of attribute configurations
	 */
	public final HashMap<String, Config> getAttributes() {
		return this.m_attributes;
	}
	/**
	 * adds a single attribute configuration to the set.
	 * @param name key value extracted from the GML
	 * @param config configuration for converting to RDF
	 */
	public final void addAttribute(final String name, final Config config) {
		if (config.getPredicate().endsWith("#")) {
			config.setPredicate(config.getPredicate() + name);
		}
		this.m_attributes.put(name, config);
	}
	/**
	 * retrieves a single attribute configuration.
	 * @param key attribute value as retrieved from the GML
	 * @return Configuration for the input attribute
	 */
	public final Config getAttributeConfig(final String key) {
		return this.m_attributes.get(key);
	}
	/**
	 * formats the configuration into a pipe delineated String.
	 * @return configuration in the conf file format
	 */
	public final String toText() {
		StringBuilder config = new StringBuilder();
		config.append(this.m_name + "|" + this.m_idField + "\n");
		Set<String> atKeys = this.m_attributes.keySet();
		for (String atName : atKeys) {
			Config atConf = this.m_attributes.get(atName);
			String rl = "L";
			if (atConf.getIsResource()) {
				rl = "R";
			}
			config.append(atName + "|" + rl + "|" + atConf.getNamespace()
					+ "|" + atConf.getParent() + "|" + atConf.getPredicate()
					+ "\n");
		}
		return config.toString();
	}
	/**
	 * loads a string representing a configuration file into
	 * the feature type.
	 * @param configuration pipe delineated string containing
	 *  each attribute configuration
	 */
	public final void load(final String configuration) {
		String[] cArray = configuration.split("\n");
		String[] ids = cArray[0].split("\\|");
		this.m_name = ids[0].trim();
		if (ids.length > 1) {
			this.m_idField = ids[1].trim();
		} else {
			this.m_idField = "null";
		}
		for (int i = 1; i < cArray.length; i++) {
			String[] config = cArray[i].split("\\|");
			Config c = new Config(config[1].equals("R"), config[2],
					config[3], config[4]);
			this.addAttribute(config[0], c);
		}
	}
	/**
	 * saves the Feature type configurations to the specified file.
	 * @param cFile filename matching the GML Feature type
	 * @throws Exception error with output file
	 */
	public final void toFile(final String cFile) throws Exception {
		DataOutputStream cout = new DataOutputStream(
			new BufferedOutputStream(new FileOutputStream(cFile)));
		cout.writeBytes(this.m_name + "|" + this.m_idField + "\n");
		Set<String> atKeys = this.m_attributes.keySet();
		for (String atName : atKeys) {
			Config atConf = this.m_attributes.get(atName);
			String rl = "L";
			if (atConf.getIsResource()) {
				rl = "R";
			}
			cout.writeBytes(atName + "|" + rl + "|"
					+ atConf.getNamespace() + "|"
					+ atConf.getParent()
					+ "|" + atConf.getPredicate()
					+ "\n");
		}
		cout.close();
	}
	/**
	 * loads a feature configuration into the current featureType.
	 * @param cFile configuration file loaded into current featureType
	 * @throws Exception input file error
	 */
	public final void loadFromFile(final String cFile) throws Exception {
		BufferedReader cin = null;
		try {
			cin = new BufferedReader(new FileReader(cFile));
			String line = null;
			String[] lineArray = null;
			if (cin.ready()) {
				line = cin.readLine();
				lineArray = line.split("\\|");
				this.m_name = lineArray[0].trim();
				if (lineArray.length > 1) {
					this.m_idField = lineArray[1].trim();
				} else {
					this.m_idField = "null";
				}
			}
			while (cin.ready()) {
				line = cin.readLine();
				lineArray = line.split("\\|");
				Config c = new Config(lineArray[1].equals("R"),
					lineArray[2], lineArray[3], lineArray[4]);
				this.addAttribute(lineArray[0], c);
			}
		} catch (Exception e) {
			throw e;
		} finally {
			if (cin != null) { cin.close(); }
		}
	}
}

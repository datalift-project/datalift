package org.datalift.geoconverter.usgs.gml.parsers;

import java.io.BufferedReader;

import org.datalift.geoconverter.usgs.rdf.util.Config;
import org.datalift.geoconverter.usgs.rdf.util.FeatureType;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
/**
 * Class for parsing through GML files to extract copies of the GML text.
 * Reads through a GML document and adds the exact text from each
 * feature member.
 * to a string that is added to the RDF feature and associated geometry
 * @author Andrew Bulen
 */
public class GMLParser {
	/** type of feature contained in the GML. */
	private String m_featureType = "null";
	/** the type of features contained in the GML file. */
	private FeatureType m_ft = null, m_geoft = new FeatureType();
	/** the default location of the configuration files. */
	public static final String defaultConfigPath = "config/";
	/** URN of the WGS1984 Spatial Reference System. */
	private static final String WGS84 = "EPSG:4326";
	/**
	 * reads through the GML file and copies each feature member into
	 * a string containing the entire GML stored in the feature resource
	 * and the Geometry string stored in the geometry resource.
	 * @param in BufferedReader used to read in GML
	 * @return Model model containing string literals of the GML
	 * @throws Exception load configuration or buffered reader error
	 */
	public final Model getFeatureMembers(final BufferedReader in)
	throws Exception {
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		Resource parent = model.createResource();
		m_geoft.loadFromFile("config/Geometry.conf");
		Config c = null;
		m_ft = new FeatureType();
		StringBuilder sb = new StringBuilder();
		String line;
		Boolean inTag = false;
		Boolean closed = true;
		while ((line = in.readLine()) != null) {
			line = line.trim();
			if (line.contains("<gml:featureMember>")
					|| line.contains("</gml:featureMember>")) {
				inTag =! inTag;
				closed = inTag;
			}
			if (inTag) {
				// add Spatial Reference to GML type
				if (line.contains("<gml:Box>")) {
					line = line.replace("<gml:Box>",
						"<gml:Box srsName=\"" + WGS84 + "\">");
				} else if (line.contains("<gml:Point>")) {
					line = line.replace("<gml:Point>",
						"<gml:Point srsName=\"" + WGS84 + "\">");
				} else if (line.contains("<gml:LineString>")) {
					line = line.replace("<gml:LineString>",
						"<gml:LineString srsName=\"" + WGS84 + "\">");
				} else if (line.contains("<gml:Polygon>")) {
					line = line.replace("<gml:Polygon>",
						"<gml:Polygon srsName=\"" + WGS84 + "\">");
				}
				// add line to the GML String builder
				sb.append(line);
				// check for line containing feature type
				if (line.contains("fid=") && !line.contains(m_featureType)) {
					m_featureType = line.substring(line.indexOf("<ogr:")
						+ "<ogr:".length(), line.indexOf(" fid="));
					if (!m_ft.name().equals(m_featureType)) {
						this.loadFeatureConfig(m_featureType);
					}
				}
			}
			if (!closed) {
				sb.append(line);
				String gmlString = sb.toString();

				sb = new StringBuilder();

				String tag = null;
				String closeTag = null;

				String ogrTag = "<ogr:" + m_ft.uidField() + ">";
				if (gmlString.contains(ogrTag)) {
					tag = ogrTag;
					closeTag = tag.replace("<", "</");
					// if data value is empty reset tag and continue
					if ((gmlString.indexOf(tag) + tag.length())
							== gmlString.indexOf(closeTag)) {
						tag = null;
					}
				}
				if (tag == null) {
					tag = "fid=\"";
					closeTag = "\">";
				}
				if (tag != null) {
					String id = gmlString.substring(
							gmlString.indexOf(tag) + tag.length(),
							gmlString.indexOf(closeTag));
					// add whole GML to feature
					/*
					c = m_ft.getAttributeConfig(m_ft.name());
					try { //parse as integer to remove leading 0s
						parent = model.createResource(c.getNamespace()
							+ "_" + Integer.parseInt(id.replace(" ", "_")));
					} catch (Exception e) {
						parent = model.createResource(c.getNamespace()
							+ "_" + id.trim().replace(" ", "_"));
					}
					c = m_ft.getAttributeConfig("asGML");
					model.add(parent, model.createProperty(
						c.getPredicate()), gmlString);
					*/

					// add GML geometry
					int start = gmlString.indexOf("<gml:",
						"<gml:featureMember>".length());
					int end = gmlString.indexOf("</gml:");
					int index = end;
					while (index > 0) {
						index = gmlString.indexOf("</gml:", index + 1);
						if (index > 0 && index < (gmlString.length()
								- "</gml:featureMember>".length())) {
							end = index;
						}
					}
					end = gmlString.indexOf("<", end+1);
					c = m_geoft.getAttributeConfig(m_geoft.name());
					try { //parse as integer to remove leading 0s
						parent = model.createResource(c.getNamespace()
							+ "_" + Integer.parseInt(id.replace(" ", "_")));
					} catch (Exception e) {
						parent = model.createResource(c.getNamespace()
							+ "_" + id.trim().replace(" ", "_"));
					}
					c = m_geoft.getAttributeConfig("asGML");
					model.add(parent, model.createProperty(c.getPredicate()),
						gmlString.substring(start, end));
				}
				closed = true;
			}
		}
		return model;
	}
	/**
     * loads the configuration for the feature type matching the input file.
     * @param featureType type of feature for which configuration is loaded
     * @throws Exception IO exception from feature loadFromFile
     */
    private void loadFeatureConfig(final String featureType) throws Exception {
    	m_ft = new FeatureType();
    	String configFile = defaultConfigPath + featureType + ".conf";
    	m_ft.loadFromFile(configFile);
    }
}

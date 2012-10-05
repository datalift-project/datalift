/**
 * @file GeometryParser.java
 * @author Andrew Bulen
 * @brief parses through a GML file for geometries and uses them to extract data about
 * the feature
 */

package org.datalift.geoconverter.usgs.gml.parsers;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;

import org.datalift.geoconverter.usgs.rdf.util.Config;
import org.datalift.geoconverter.usgs.rdf.util.ConfigFinder;
import org.datalift.geoconverter.usgs.rdf.util.FeatureType;
import org.geotools.gml2.GMLConfiguration;
import org.geotools.xml.StreamingParser;
import org.opengis.feature.simple.SimpleFeature;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.vividsolutions.jts.geom.Geometry;

/**
 * class for parsing GML files to retrieve the geometric data.
 * stream parses through a GML file and adds all simple feature geometries to a
 * set. Then processes the set to retrieve the geometric properties associated
 * with the simple feature geometry and/or the spatial relations between all of
 * the features contained in the set.
 * @author Andrew Bulen
 */
public class GeometryParser {
	/** List of all ID's used to represent the geometry and associated feature. */
	private Vector<String> m_IDs = new Vector<String>();
	/** List containing all of the simple feature geometries. */
	private Vector<Geometry> m_geoms = new Vector<Geometry>();
	/** List containing the feature type associated with the geometries. */
	private Vector<String> m_ftypes = new Vector<String>();
	/** configurations for any feature type that is being processed. */
	private Hashtable<String, FeatureType> m_configs
		= new Hashtable<String, FeatureType>();
	/** the type of features contained in the GML file. */
	private FeatureType m_ft = null;
	/** Geometry feature type configuration. */
	private FeatureType m_geoft = null;
	/** the default location of the configuration files. */
        // public static final String defaultConfigPath = "config/";

	/** configurations for the 8 relational properties. */
	private Config contains, covers, crosses, equals, intersects, overlaps,
		touches, within;

	/** default constructor. */
	public GeometryParser() {
	    // NOP
	}

//	public static void setDefaultConfigPath(File cfgPath) throws IOException {
//	    if (! (cfgPath.isDirectory() && cfgPath.canRead())) {
//	        throw new FileNotFoundException(cfgPath.getPath());
//	    }
//	    defaultConfigPath = cfgPath;
//	}

	/**
	 * parses through the GML file and stores the geometry, ID and feature
	 * type of each simple feature.
	 * @param gmlFile GML file data is extracted from
	 * @return table containing the geometries
	 * @throws Exception IO or parser error
	 */
	public final Vector<Geometry> getGeometries(final String gmlFile)
		throws Exception {
	    File src = new File(gmlFile);
	    InputStream in = new FileInputStream(src);
    	GMLConfiguration gml = new GMLConfiguration();
    	StreamingParser parser = new StreamingParser(gml, in,
    			SimpleFeature.class);
    	SimpleFeature f = null;
    	while ((f = (SimpleFeature) parser.parse()) != null) {
    		Geometry g = (Geometry) f.getDefaultGeometry();
    		if (g != null) {
    			this.m_geoms.add(g);
    			String ft = f.getFeatureType().getName().getLocalPart();
    			this.m_ftypes.add(ft);
    			if (!this.m_configs.containsKey(ft)) {
    			    this.m_ft = this.loadFeatureConfig(ft);
    			    this.m_configs.put(ft, this.m_ft);
        		}
    			this.m_IDs.add(this.getID(f));
    		}
    	}
    	m_geoft = this.loadFeatureConfig("Geometry");
    	return this.m_geoms;
	}
	/**
	 * initializes the relational configurations.
	 */
	private void setRelationConfig() {
		contains = m_geoft.getAttributeConfig("contains");
		covers = m_geoft.getAttributeConfig("covers");
		crosses = m_geoft.getAttributeConfig("crosses");
		equals = m_geoft.getAttributeConfig("equals");
		intersects = m_geoft.getAttributeConfig("intersects");
		overlaps = m_geoft.getAttributeConfig("overlaps");
		touches = m_geoft.getAttributeConfig("touches");
		within = m_geoft.getAttributeConfig("within");
	}
	/**
	 * extracts the geometric properties from each feature's geometry and
	 * adds to a RDF model.
	 * @return model containing the geometric data
	 */
	public final Model getGeoProperties() {
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		Iterator<String> idIter = this.m_IDs.iterator();
		Iterator<Geometry> geoIter = this.m_geoms.iterator();
		Iterator<String> ftypeIter = this.m_ftypes.iterator();

		while (idIter.hasNext() && geoIter.hasNext() && ftypeIter.hasNext()) {
			String id = idIter.next();
			Geometry geo = geoIter.next();
			String ftype = ftypeIter.next();
			FeatureType sft = m_configs.get(ftype);
			Config sfc = sft.getAttributeConfig(sft.name());
			//create the feature to which the geometry is linked
			Resource parent = null, feature = null;
			try { //parse as integer to remove leading 0s
				feature = model.createResource(sfc.getNamespace() + "_"
						+ Integer.parseInt(id.replace(" ", "_")));
			} catch (Exception e) {
				feature = model.createResource(sfc.getNamespace()
						+ "_" + id.trim().replace(" ", "_"));
			}
			// create the parent geometry to which all attributes are linked
			try { //parse as integer to remove leading 0s
				parent = model.createResource(m_geoft.getAttributeConfig(
					m_geoft.name()).getNamespace() + "_"
					+ Integer.parseInt(id.replace(" ", "_")));
			} catch (Exception e) {
				parent = model.createResource(m_geoft.getAttributeConfig(
					m_geoft.name()).getNamespace() + "_" + id.trim().replace(" ", "_"));
			}
			Config c = sft.getAttributeConfig("hasGeometry");
			model.add(feature, model.createProperty(c.getPredicate()), parent);
			c = m_geoft.getAttributeConfig("dimension");
			model.add(parent, model.createProperty(c.getPredicate()),
					model.createTypedLiteral(geo.getDimension()));
			c = m_geoft.getAttributeConfig("coordinateDimension");
			model.add(parent, model.createProperty(c.getPredicate()),
				model.createTypedLiteral(geo.getDimension()));
			c = m_geoft.getAttributeConfig("spatialDimension");
			model.add(parent, model.createProperty(c.getPredicate()),
				model.createTypedLiteral(geo.getDimension()));
			c = m_geoft.getAttributeConfig("isEmpty");
			model.add(parent, model.createProperty(c.getPredicate()),
				model.createTypedLiteral(geo.isEmpty()));
			c = m_geoft.getAttributeConfig("isSimple");
			model.add(parent, model.createProperty(c.getPredicate()),
				model.createTypedLiteral(geo.isSimple()));
			c = m_geoft.getAttributeConfig("is3D");		
			model.add(parent, model.createProperty(c.getPredicate()),
				model.createTypedLiteral(geo.getDimension() == 3));
			c = m_geoft.getAttributeConfig("srid");
			model.add(parent, model.createProperty(c.getPredicate()),
				model.createTypedLiteral(geo.getSRID()));
			c = m_geoft.getAttributeConfig("asWKT");
			model.add(parent, model.createProperty(c.getPredicate()),
				geo.toText());
		}
		return model;
	}
	/**
	 * compares a single geometry with all others contained in the
	 * geometry list.
	 * @param geo the geometry being compared to all others
	 * @param geoID the Unique ID of the current geometry
	 * @param ftype the feature type associated with the geometry
	 * @return Model containing any relations discovered with input geometry
	 */
	private Model compareGeometry(final Geometry geo, final String geoID,
			final String ftype) {
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		Iterator<String> compIdIter = this.m_IDs.iterator();
		Iterator<Geometry> compGeoIter = this.m_geoms.iterator();
		while (!compIdIter.next().equals(geoID)) {
			compGeoIter.next();
		}
		compGeoIter.next();
		String compID;
		while (compIdIter.hasNext() && compGeoIter.hasNext()) {
			compID = compIdIter.next();
			Geometry compGeo = compGeoIter.next();
			// don't compare with itself
			if (geoID!=compID) {
				try {
					// contained inside current feature
					if (geo.contains(compGeo)) {
						model.add(this.addRelation(geoID, compID, contains));
					}
					// covered by current feature
					if (geo.covers(compGeo)) {
						model.add(this.addRelation(geoID, compID, covers));
					}
					// crosses current feature
					if (geo.crosses(compGeo)) {
						model.add(this.addRelation(geoID, compID, crosses));
					}
					// equal to current feature
					if (geo.equals(compGeo)) {
						model.add(this.addRelation(geoID, compID, equals));
					}
					// intersects current feature
					if (geo.intersects(compGeo)) {
						model.add(this.addRelation(geoID, compID, intersects));
					}
					// overlaps current feature
					if (geo.overlaps(compGeo)) {
						model.add(this.addRelation(geoID, compID, overlaps));
					}
					// touches current feature
					if (geo.touches(compGeo)) {
						model.add(this.addRelation(geoID, compID, touches));
					}
					// within current feature
					if (geo.within(compGeo)) {
						model.add(this.addRelation(geoID, compID, within));
					}
				} catch(Exception e) {
					System.out.println("Error comparing feature: " + geoID
							+ " with feature: " + compID);
				}
			}
		}
		return model;
	}
	/**
	 * compares the geometries to determine any spatial relations
	 * between each geometry.
	 * @return model containing all the spatial relations
	 */
	public final Model getRelations() {
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		this.setRelationConfig();

		Iterator<String> idIter = this.m_IDs.iterator();
		Iterator<Geometry> geoIter = this.m_geoms.iterator();
		Iterator<String> ftypeIter = this.m_ftypes.iterator();
		String staticID;
		while (idIter.hasNext() && geoIter.hasNext() && ftypeIter.hasNext()) {
			staticID = idIter.next();
			Geometry staticGeo = geoIter.next();
			String staticFtype = ftypeIter.next();
			try {
				model.add(this.compareGeometry(staticGeo, staticID,
						staticFtype));
			} catch (Exception e) {
				System.out.println("Error comparing geometry for feature: "
						+ staticID);
			}
		}
		return model;
	}
	/**
	 * adds any input relation to a RDF model.
	 * @param parentID the ID of the parent feature
	 * @param relID ID of the feature with the input relation to the parent
	 * @param relc configuration for the relation between features
	 * @return model containing the relation
	 * @throws Exception faulty configuration error
	 */
	private Model addRelation(final String parentID, final String relID,
			final Config relc) throws Exception {
		Model model = ModelFactory.createMemModelMaker().createFreshModel();
		Resource parent, child;

		Config gc = m_geoft.getAttributeConfig(m_geoft.name());
		//requires a predicate and that the parent exists
		if (relc.getPredicate() == null
				|| relc.getPredicate().equalsIgnoreCase("")) {
			//System.out.println(schema[i]);
			throw new Exception("no predicate found for relation");
		}
		try { //parse as integer to remove leading 0s
			parent = model.createResource(gc.getNamespace() + "_"
				+ Integer.parseInt(parentID.replace(" ", "_")));
		} catch (Exception e) {
			parent = model.createResource(gc.getNamespace() + "_"
				+ parentID.trim());
		}
		try { //parse as integer to remove leading 0s
			child = model.createResource(gc.getNamespace() + "_"
				+ Integer.parseInt(relID.replace(" ", "_")));
		} catch (Exception e) {
			child = model.createResource(gc.getNamespace() + "_"
				+ relID.trim());
		}
		try { //parse as integer to remove leading 0s
			model.add(parent, model.createProperty(relc.getPredicate()),
				child);
		} catch (Exception e) {
			model.add(parent, model.createProperty(relc.getPredicate()),
				child);
		}
		return model;
	}
	/**
     * searches the simple feature for the property containing the desired ID.
     * @param sf simple feature being processed
     * @return the ID representing the feature, gml:id if none found
     */
    public final String getID(final SimpleFeature sf) {
    	String id = null;
    	String ft = sf.getFeatureType().getName().getLocalPart();
    	String idf = this.m_configs.get(ft).uidField();
    	if (!idf.isEmpty()) {
			Object o = sf.getAttribute(idf);
			if (o != null) {
			    id = o.toString();
			}
		}
    	if (id == null) {
    		id = sf.getID();
    	}
    	return id;
    }
    /**
     * loads the configuration for the feature type matching the input file.
     * @param featureType name of feature type configuration to load
     * @throws Exception load configuration IO error
     */
    private FeatureType loadFeatureConfig(final String featureType)
    throws Exception {
        File cfg = ConfigFinder.findFile(featureType + ".conf");
//        File cfg = new File(path, featureType + ".conf");
//        if (! (cfg.isFile() && cfg.canRead())) {
//            cfg = new File(defaultConfigPath, featureType + ".conf");
//        }
        if (! (cfg.isFile() && cfg.canRead())) {
            throw new FileNotFoundException(cfg.getPath());
        }
        return this.loadFeatureConfig(cfg);
    }
    private FeatureType loadFeatureConfig(final File configFile)
    throws Exception {
        FeatureType ft = new FeatureType();
        ft.loadFromFile(configFile.getCanonicalPath());
        return ft;
    }
}

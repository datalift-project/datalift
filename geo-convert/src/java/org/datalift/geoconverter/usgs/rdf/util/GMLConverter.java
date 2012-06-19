package org.datalift.geoconverter.usgs.rdf.util;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;

import org.datalift.geoconverter.usgs.gml.parsers.FeatureParser;
import org.datalift.geoconverter.usgs.gml.parsers.GMLParser;
import org.datalift.geoconverter.usgs.gml.parsers.GeometryParser;


import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.vocabulary.OWL;
import com.hp.hpl.jena.vocabulary.RDF;
/**
 * GML conversion class that runs each of the GML parsers
 * and combines the output from each into a single RDF model
 * and writes the entire model to a N3 file.
 * -implements the Runnable interface to allow multiple instances
 * of the converter to be run in parallel
 * @author Andrew Bulen
 */
public class GMLConverter implements Runnable {
	/** model to which all data is added. */
	private Model m_model;
	/** extracts the feature data from the GML. */
	private FeatureParser m_fParser;
	/** extracts the geometric data from the GML. */
	private GeometryParser m_geoParser;
	/** extracts the GML strings from the file. */
	private GMLParser m_gmlParser;
	/** input GML file. */
	private File m_inputFile;
	/** output Directory for N3 files. */
	private File m_outputDir;
	/** booleans representing which functions to run. */
	private boolean m_features = false, m_geometry = false,
		m_gml = true, m_relations = false;

	/**
	 * determines if input file is single file or directory and
	 * converts accordingly.
	 */
	public final void run() {
		System.out.println("Beginning Conversion of: \n\t"
				+ m_inputFile.getPath());
		Timer timer = new Timer();
		try {
	    	if (m_inputFile.isFile()) {
	    		convertFile(m_inputFile.getPath(),
	    				m_outputDir.getPath());
	    	} else if (m_inputFile.isDirectory()) {
	    		convertDirectory(m_inputFile.getPath(),
	    				m_outputDir.getPath());
	    	}
			System.out.println("Total Running Time");
			timer.printElapsedTime();
			System.out.println();
    	} catch (Exception e) {
    		e.printStackTrace();
    		System.out.println("Error");
    		timer.printElapsedTime();
    		System.out.println();
    	}
	}
	/**
	 * converts a single input file to RDF.
	 * @param input input file location
	 * @param output output directory location
	 * @throws Exception IO or model generation error
	 */
	public final void convertFile(final String input, final String output)
	throws Exception {
    	File inFile = new File(input);
    	Timer fileTimer = new Timer();
    	// make sure all parsers are initialized to default values
    	m_fParser = new FeatureParser();
    	m_geoParser = new GeometryParser();
    	m_gmlParser = new GMLParser();

		generateModel(inFile.getPath());

		// output to file
		DataOutputStream out = new DataOutputStream(
			new BufferedOutputStream(new FileOutputStream(
			output + "/" + inFile.getName().replace(".gml", ".rdf"))));

		//m_model.getWriter("N3").write(m_model, out, null);
		m_model.getWriter("RDF/XML").write(m_model, out, null);
		out.close();
		System.out.println("Finished converting " + inFile.getName());
		fileTimer.printElapsedTime();
	}
	/**
	 * loads the geometries for a directory of GML files
	 * and determines spatial.
	 * relations between all input Features
	 * @param inpath directory containing input GML files
	 * @param outpath directory for saving output RDF
	 * @throws Exception IO or model generation error
	 */
	public final void convertRelations(final String inpath,
			final String outpath) throws Exception {
		m_geoParser = new GeometryParser();
		File path = new File(inpath);
		File[] files = path.listFiles(
				org.datalift.geoconverter.usgs.gml2rdf.gui.Filters.gmlFileFilter);
		Timer loadTimer = new Timer();
		try {
			for (File f : files) {
				m_geoParser.getGeometries(f.getPath());
				System.out.println("Geometries loaded from "
						+ f.getName());
			}
			loadTimer.printElapsedTime();
		} catch (Exception e) {
			System.out.println("could not load all geometries");
			throw e;
		}
		loadTimer.reset();
		try {
			m_model = m_geoParser.getRelations();
			if (!m_model.getNsPrefixMap().containsKey("geom")) {
				m_model.setNsPrefix("geom",
					"http://cegis.usgs.gov/rdf/geometry#");
			}
			System.out.println("Model Created");
			loadTimer.printElapsedTime();
		} catch (Exception e) {
			System.out.println("Error adding relations to model");
			throw e;
		}

		// output to file
		DataOutputStream out = null;
		try {
		out = new DataOutputStream(
			new BufferedOutputStream(new FileOutputStream(
				outpath + "/Relations.n3")));

		m_model.getWriter("N3").write(m_model, out, null);
		} catch (Exception e) {
			throw e;
		} finally {
			out.close();
		}
	}
	/**
	 * either finds relations between all files in the input
	 * directory or finds all GML files in the directory and
	 * converts them to RDF individually.
	 * @param inpath input directory containing GML files
	 * @param outpath output directory for RDF files
	 * @throws Exception file selection or convertFile error
	 */
	public final void convertDirectory(final String inpath,
			final String outpath) throws Exception {
		// if just finding relations compare entire directory
		if (this.m_relations && !this.m_features
				&& !this.m_geometry && !this.m_gml) {
			this.convertRelations(inpath, outpath);
		} else {
	    	File path = new File(inpath);
			File[] files;
			// create an array containing all the filenames in the
			// input directory
			files = path.listFiles(
					org.datalift.geoconverter.usgs.gml2rdf.gui.Filters.gmlFileFilter);
			// loop through each file and convert valid N3 files
			for (File f : files) {
				if (f.isFile()) {
					this.convertFile(f.getPath(), outpath);
				}
			}
		}
	}
	/**
	 * default constructor.
	 */
	public GMLConverter() {
		m_model = ModelFactory.createMemModelMaker().createFreshModel();
		m_fParser = new FeatureParser();
		m_geoParser = new GeometryParser();
		m_gmlParser = new GMLParser();
	}
	/**
	 * file initialization constructor.
	 * @param input input file
	 * @param output output directory
	 */
	public GMLConverter(final File input, final File output) {
		this.m_inputFile = input;
		this.m_outputDir = output;
		m_model = ModelFactory.createMemModelMaker().createFreshModel();
		m_fParser = new FeatureParser();
		m_geoParser = new GeometryParser();
		m_gmlParser = new GMLParser();
	}
	/**
	 * initialization constructor.
	 * @param input input file
	 * @param output output directory
	 * @param features run feature parser
	 * @param gml run GML parser
	 * @param geometry run geometry parser
	 * @param relations run geometric relations function
	 */
	public GMLConverter(final File input, final File output,
		final boolean features, final boolean gml,
		final boolean geometry,	final boolean relations) {

		this.m_inputFile = input;
		this.m_outputDir = output;
		m_model = ModelFactory.createMemModelMaker().createFreshModel();
		m_fParser = new FeatureParser();
		m_geoParser = new GeometryParser();
		m_gmlParser = new GMLParser();
		this.m_features = features;
		this.m_gml = gml;
		this.m_geometry = geometry;
		this.m_relations = relations;
	}
	/**
	 * runs each parser selected and adds data from each to the RDF model.
	 * @param path location of file
	 * @return model contains all data from each parser combined into a
	 *  single RDF model
	 * @throws Exception parser or IO error
	 */
	public final Model generateModel(final String path) throws Exception {
		m_model = ModelFactory.createMemModelMaker().createFreshModel();
		if (this.m_features) {
			m_model = m_fParser.streamParseToRDF(path);
			String prefix = m_fParser.defaultNS().split("/")[(
				m_fParser.defaultNS().split("/").length - 1)];
			prefix = prefix.substring(0, prefix.indexOf("#"));
			m_model.setNsPrefix(prefix, m_fParser.defaultNS());
		}
		if (this.m_geometry || this.m_relations) {
			m_geoParser.getGeometries(path);
			if (this.m_relations) {
				m_model.add(m_geoParser.getRelations());
			}
			if (this.m_geometry) {
				m_model.add(m_geoParser.getGeoProperties());
			}
			m_geoParser = new GeometryParser();
		}
		if (this.m_gml) {
			BufferedReader in = null;
			try {
				in = new BufferedReader(new FileReader(path));
				m_model.add(m_gmlParser.getFeatureMembers(in));
			} finally {
				in.close();
			}
		}

		m_model.setNsPrefix("owl", OWL.NS);
		m_model.setNsPrefix("rdf", RDF.getURI());
		m_model.setNsPrefix("Geometry",
				"http://www.opengis.net/rdf/Geometry#");
		m_model.setNsPrefix("ogc", "http://www.opengis.net/rdf#");

		return m_model;
	}
	/**
	 * @return model storing RDF data
	 */
	public final Model getModel() {
		return m_model;
	}

}

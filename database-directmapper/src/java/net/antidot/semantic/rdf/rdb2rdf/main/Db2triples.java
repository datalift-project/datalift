/* 
 * Copyright 2011 Antidot opensource@antidot.net
 * https://github.com/antidot/db2triples
 * 
 * DB2Triples is free software; you can redistribute it and/or 
 * modify it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation; either version 2 of 
 * the License, or (at your option) any later version.
 * 
 * DB2Triples is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * 
 * Direct Mapping Main
 *
 * Interface between user and console.
 * 
 *
 */
package net.antidot.semantic.rdf.rdb2rdf.main;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import net.antidot.semantic.rdf.model.impl.sesame.SesameDataSet;
import net.antidot.semantic.rdf.rdb2rdf.dm.core.DirectMapper;
import net.antidot.semantic.rdf.rdb2rdf.dm.core.DirectMappingEngine.Version;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.core.R2RMLProcessor;
import net.antidot.sql.model.core.DriverType;
import net.antidot.sql.model.core.SQLConnector;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.rio.RDFFormat;

@SuppressWarnings("static-access")
public class Db2triples {

	// Log
	private static Log log = LogFactory.getLog(Db2triples.class);
	
	private static final DriverType defaultDriver = DriverType.MysqlDriver;

	private static Option modeOpt = OptionBuilder
			.withArgName("mode")
			.hasArg()
			.withDescription(
					"Mandatory conversion mode, 'r2rml' for R2RML and 'dm' for Direct Mapping")
			.withLongOpt("mode").create("m");

	private static Option userNameOpt = OptionBuilder.withArgName("user_name")
			.hasArg().withDescription("Database user name").withLongOpt("user")
			.create("u");

	private static Option passwordOpt = OptionBuilder.withArgName("password")
			.hasArg().withDescription("Database password").withLongOpt("pass")
			.create("p");

	private static Option URLOpt = OptionBuilder
			.withArgName("url")
			.hasArg()
			.withDescription("Database URL (default : jdbc:mysql://localhost/)")
			.withLongOpt("url").create("l");

	private static Option driverOpt = OptionBuilder.withArgName("driver")
			.hasArg().withDescription(
					"Driver to use (default : " + defaultDriver.getDriverName() + " )")
			.withLongOpt("driver").create("d");

	private static Option versionOpt = OptionBuilder
			.withArgName("version")
			.hasArg()
			.withDescription(
					"Version of norm to use (1 = Working Draft 20 September 2011 (default), 2 = Working Draft 23 March 2011)")
			.withLongOpt("version").create("v");

	private static Option dbOpt = OptionBuilder.withArgName("database_name")
			.hasArg().withDescription("database name").withLongOpt("database")
			.create("b");

	private static Option baseURIOpt = OptionBuilder.withArgName("base_uri")
			.hasArg().withDescription(
					"Base URI (default : http://foo.example/DB/)").withLongOpt(
					"base_uri").create("i");

	private static Option nativeOpt = new Option("n",
			"Use native store (store in output directory path)");

	private static Option nativeStoreNameOpt = OptionBuilder.withArgName(
			"nativeOutput").hasArg().withDescription(
			"Native store output directory").withLongOpt("native_output")
			.create("n");

	private static Option forceOpt = new Option("f",
			"Force loading of existing repository (without remove data)");

	private static Option outputOpt = OptionBuilder.withArgName("output")
			.hasArg().withDescription("Output RDF filename (default : output)")
			.withLongOpt("output").create("o");

	private static Option transformSPARQLFile = OptionBuilder.withArgName(
			"sparql").hasArg().withDescription(
			"Sparql transform request file (optionnal)").withLongOpt("sparql")
			.create("s");

	private static Option transformOutputFile = OptionBuilder
			.withArgName("sparql_output")
			.hasArg()
			.withDescription(
					"Transformed graph output file (optionnal if sparql option is not specified, default : sparql_output otherwise)")
			.withLongOpt("sparql_output").create("q");

	private static Option rdfFormat = OptionBuilder
			.withArgName("format")
			.hasArg()
			.withDescription(
					"RDF syntax output format ('RDFXML', 'N3', 'NTRIPLES' or 'TURTLE')")
			.withLongOpt("format").create("t");
	private static Option r2rmlFileOpt = OptionBuilder
			.withArgName("r2rml_file")
			.hasArg()
			.withDescription(
					"R2RML config file used to convert relationnal database into RDF terms.")
			.withLongOpt("r2rml_file").create("r");

	private static String projectName = "db2triples v1.0 - See https://github.com/antidot/db2triples for more informations.";
	private static String projectNameR2RMLMode = "db2triples v1.0 - R2RML mode - See https://github.com/antidot/db2triples for more informations.";
	private static String projectNameDirectMappingMode = "db2triples v1.0 - Direct Mapping mode - See https://github.com/antidot/db2triples for more informations.";

	public static void main(String[] args) {
		// Get all options
		Options options = new Options();
		Options r2rmlOptions = new Options();
		Options dmOptions = new Options();
		options.addOption(modeOpt);
		options.addOption(userNameOpt);
		r2rmlOptions.addOption(userNameOpt);
		dmOptions.addOption(userNameOpt);
		options.addOption(passwordOpt);
		r2rmlOptions.addOption(passwordOpt);
		dmOptions.addOption(passwordOpt);
		options.addOption(URLOpt);
		r2rmlOptions.addOption(URLOpt);
		dmOptions.addOption(URLOpt);
		options.addOption(driverOpt);
		r2rmlOptions.addOption(driverOpt);
		dmOptions.addOption(driverOpt);
		options.addOption(dbOpt);
		r2rmlOptions.addOption(dbOpt);
		dmOptions.addOption(dbOpt);
		options.addOption(baseURIOpt);
		r2rmlOptions.addOption(baseURIOpt);
		dmOptions.addOption(baseURIOpt);
		options.addOption(forceOpt);
		r2rmlOptions.addOption(forceOpt);
		dmOptions.addOption(forceOpt);
		options.addOption(nativeOpt);
		r2rmlOptions.addOption(nativeOpt);
		dmOptions.addOption(nativeOpt);
		options.addOption(nativeStoreNameOpt);
		r2rmlOptions.addOption(nativeStoreNameOpt);
		dmOptions.addOption(nativeStoreNameOpt);
		options.addOption(outputOpt);
		r2rmlOptions.addOption(outputOpt);
		dmOptions.addOption(outputOpt);
		options.addOption(transformSPARQLFile);
		dmOptions.addOption(transformSPARQLFile);
		options.addOption(transformOutputFile);
		dmOptions.addOption(transformOutputFile);
		options.addOption(rdfFormat);
		r2rmlOptions.addOption(rdfFormat);
		dmOptions.addOption(rdfFormat);
		options.addOption(versionOpt);
		dmOptions.addOption(versionOpt);
		options.addOption(r2rmlFileOpt);
		r2rmlOptions.addOption(r2rmlFileOpt);

		// Init parameters
		String mode = null;
		String userName = null;
		String password = null;
		String url = null;
		DriverType driver = null;
		String dbName = null;
		String baseURI = null;
		boolean useNativeStore = false;
		boolean forceExistingRep = false;
		String nativeOutput = null;
		String output = null;
		String sparql = null;
		String sparqlOutput = null;
		String format = null;
		String r2rmlFile = null;
		int int_version = 1;

		// RDF Format output
		RDFFormat rdfFormat = RDFFormat.TURTLE; // Turtle by default
		// Norm version
		Version version = Version.WD_20120529;

		// Option parsing
		// Create the parser
		CommandLineParser parser = new GnuParser();
		try {
			// parse the command line arguments
			CommandLine line = parser.parse(options, args);
			// Database settings
			// Mode
			if (!line.hasOption("mode")) {
				// automatically generate the help statement
				log.error("Mode is required. Use -m option to set it.");
				HelpFormatter formatter = new HelpFormatter();
				formatter.printHelp(projectName, options);
				System.exit(-1);
			} else {
				mode = line.getOptionValue("mode");
				if (!mode.equals("r2rml") && !mode.equals("dm")) {
					log
							.error("Unkonw mode. Please select 'r2rml' or 'dm' mode.");
					HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp(projectName, options);
					System.exit(-1);
				}
			}
			// user name
			if (!line.hasOption("user")) {
				// automatically generate the help statement
				log.error("User name is required. Use -u option to set it.");
				HelpFormatter formatter = new HelpFormatter();
				if (mode.equals("r2rml")) {
					formatter.printHelp(projectNameR2RMLMode, r2rmlOptions);
				} else {
					formatter
							.printHelp(projectNameDirectMappingMode, dmOptions);
				}
				System.exit(-1);
			} else {
				userName = line.getOptionValue("user");
			}
			// password
			if (!line.hasOption("pass")) {
				// automatically generate the help statement
				log.error("Password is required. Use -p option to set it.");
				HelpFormatter formatter = new HelpFormatter();
				if (mode.equals("r2rml")) {
					formatter.printHelp(projectNameR2RMLMode, r2rmlOptions);
				} else {
					formatter
							.printHelp(projectNameDirectMappingMode, dmOptions);
				}
				System.exit(-1);
			} else {
				password = line.getOptionValue("pass");
			}
			// Database URL
			url = line.getOptionValue("url", "jdbc:mysql://localhost/");
			// driver
			driver = new DriverType(line.getOptionValue("driver", defaultDriver.getDriverName()));
			// Database name
			if (!line.hasOption("database")) {
				// automatically generate the help statement
					log
							.error("Database name is required. Use -b option to set it.");
				HelpFormatter formatter = new HelpFormatter();
				if (mode.equals("r2rml")) {
					formatter.printHelp(projectNameR2RMLMode, r2rmlOptions);
				} else {
					formatter
							.printHelp(projectNameDirectMappingMode, dmOptions);
				}
				System.exit(-1);
			} else {
				dbName = line.getOptionValue("database");
			}
			// Base URI
			baseURI = line.getOptionValue("base_uri", "http://foo.example/DB/");
			// Use of native store ?
			useNativeStore = line.hasOption("n");
			// Name of native store
			if (useNativeStore && !line.hasOption("native_output")) {
				// automatically generate the help statement
				log
						.error("Native triplestore path is required. Use -n option to set it.");
				HelpFormatter formatter = new HelpFormatter();
				if (mode.equals("r2rml")) {
					formatter.printHelp(projectNameR2RMLMode, r2rmlOptions);
				} else {
					formatter
							.printHelp(projectNameDirectMappingMode, dmOptions);
				}
				System.exit(-1);
			} else {
				nativeOutput = line.getOptionValue("native_output");
			}
			// Force loading of repository
			forceExistingRep = line.hasOption("f");
			// Output
			output = line.getOptionValue("output", "output.ttl");
			// SPARQL transformation
			if (line.hasOption("sparql")) {
				if (!mode.equals("dm")) {
					log
							.warn("sparql option is required only for 'dm' mode : it will be ignored...");
				} else {
					sparql = line.getOptionValue("sparql");
					sparqlOutput = line.getOptionValue("sparql_output",
							"output_sparql.ttl");
				}
			}
			// RDF Format
			if (line.hasOption("format")) {
				format = line.getOptionValue("format");
				if (format.equals("TURTLE"))
					rdfFormat = RDFFormat.TURTLE;
				else if (format.equals("RDFXML"))
					rdfFormat = RDFFormat.RDFXML;
				else if (format.equals("NTRIPLES"))
					rdfFormat = RDFFormat.NTRIPLES;
				else if (!format.equals("N3")) {
					log.error("Unknown RDF format. Please use RDFXML, TURTLE, N3 or NTRIPLES.");
					HelpFormatter formatter = new HelpFormatter();
					if (mode.equals("r2rml")) {
						formatter.printHelp(projectNameR2RMLMode, r2rmlOptions);
					} else {
						formatter.printHelp(projectNameDirectMappingMode,
								dmOptions);
					}
					System.exit(-1);
				}
			}
			// Norm version
			if (line.hasOption("version")) {
				if (!mode.equals("dm")) {
					log.warn("version option is required only for 'dm' mode : it will be ignored...");
				}
				switch (int_version) {
				case 1:
					version = Version.WD_20120529;
					break;
				case 2:
					version = Version.WD_20110324;
					// Check DB compatibilities
					if (!(driver.equals(DriverType.MysqlDriver) || driver
							.equals(DriverType.PostgreSQL))) {
						log.error("Db2triples in Direct Mapping mode does'nt support this driver for the Working Draft"
										+ " of 23 March 2011 (only MySQL and PostGreSQL for this time). "
										+ "You can set the version option to select Working Draft of 20 September 2011.");
						System.exit(-1);
					}
					break;
				default:
					break;
				}
			}
			// r2rml instance
			if (mode.equals("r2rml")) {
				if (!line.hasOption("r2rml_file")) {
					log
					.error("R2RML config file is required. Use -r option to set it.");
					// automatically generate the help statement
					HelpFormatter formatter = new HelpFormatter();
					formatter.printHelp(projectNameR2RMLMode, r2rmlOptions);
					System.exit(-1);
				} else {
					r2rmlFile = line.getOptionValue("r2rml_file");
					File r2rmlFileTest = new File(r2rmlFile);
					if (!r2rmlFileTest.exists()) {
						log.error("R2RML file does not exists.");
						System.exit(-1);
					}
				}
			}
		} catch (ParseException exp) {
			// oops, something went wrong
			log.error("Parsing failed. Reason : " + exp.getMessage());
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp(projectName, options);
			System.exit(-1);
		}

		// Open test database
		Connection conn = null;
		try {
			// Connect database
			conn = SQLConnector
					.connect(userName, password, url, driver, dbName);

			// Generate RDF graph
			SesameDataSet g = null;
			// Check nature of storage (memory by default)
			if (useNativeStore) {
				File pathToNativeOutputDir = new File(nativeOutput);
				if (pathToNativeOutputDir.exists() && !forceExistingRep) {
						log
								.error("Directory "
										+ pathToNativeOutputDir
										+ "  already exists. Use -f option to force loading of existing repository.");
					System.exit(-1);
				}
				// Extract database model according to convert mode
				if (mode.equals("r2rml")) {
					g = R2RMLProcessor.convertDatabase(conn, r2rmlFile, baseURI, nativeOutput, driver);
				} else {
					g = DirectMapper.generateDirectMapping(conn, version,
							driver, baseURI, null, nativeOutput);
				}
			} else {
				File outputFile = new File(output);
				if (outputFile.exists() && !forceExistingRep) {
						log
								.error("Output file "
										+ outputFile.getAbsolutePath()
										+ " already exists. Please remove it or modify ouput name option.");
					System.exit(-1);
				}
				// Extract database model
				if (mode.equals("r2rml")){
					g = R2RMLProcessor.convertDatabase(conn, r2rmlFile, baseURI, driver);
				} else {
					g = DirectMapper.generateDirectMapping(conn, version, driver,
							baseURI, null, null);
				}
				// Dump graph
				log.info("Serialize RDF graph...");
				g.dumpRDF(output, rdfFormat);
				log.info("RDF graph serialized into " + outputFile.getAbsolutePath());
			}
			if (sparql != null && mode.equals("dm")) {
				log.info("Execute SPARQL transformation...");
				Long start = System.currentTimeMillis();
				String result = g.runSPARQLFromFile(sparql, rdfFormat);
				SesameDataSet gResult = new SesameDataSet();
				gResult.addString(result, rdfFormat);
				gResult.dumpRDF(sparqlOutput, rdfFormat);

				Float stop = Float.valueOf(System.currentTimeMillis() - start) / 1000;
				log.info("Direct Mapping SPARQL query executed in "
						+ stop + " seconds.");
				log.info("[DirectMapping:main] Number of triples after transformation : "
								+ gResult.getSize());
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				// Close db connection
				conn.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}

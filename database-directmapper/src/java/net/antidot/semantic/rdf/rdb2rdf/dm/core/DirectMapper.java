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
/***************************************************************************
 *
 * Direct Mapping : Direct Mapper
 *
 * The Direct Mapper is the main composant which starts the mapping.
 * Its goal consists to extracts generic tuples of a given database in cursor
 * mode and to convert every of them. 
 *
 ****************************************************************************/
package net.antidot.semantic.rdf.rdb2rdf.dm.core;

import java.io.UnsupportedEncodingException;
import org.datalift.fwk.log.Logger;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;

import net.antidot.semantic.rdf.model.impl.sesame.DataSet;
import net.antidot.semantic.rdf.model.impl.sesame.SesameDataSet;
import net.antidot.semantic.rdf.rdb2rdf.dm.core.DirectMappingEngine.Version;
import net.antidot.sql.model.core.DriverType;
import net.antidot.sql.model.db.Key;
import net.antidot.sql.model.db.Tuple;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.datalift.converter.dbdirectmapper.StoreMappedDataSet;
import org.datalift.fwk.rdf.Repository;
import org.openrdf.model.Statement;


public abstract class DirectMapper extends Thread {
	
	// Log
	private static Log log = LogFactory.getLog(DirectMapper.class);
	
	// Namespaces
	public static HashMap<String, String> prefix = new HashMap<String, String>();
	
	// Load prefix
	static {
		prefix.put("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		prefix.put("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
		prefix.put("xsd", "http://www.w3.org/2001/XMLSchema#");
	}
	
	// Metrics 
	private static int nbTriples;
	private static int lastModulo;
	
	
	/**
	 * Datalift-oriented overloaded method to perform direct mapping on a database. Despite the internal logic of this method
	 * is really similar to the canonical one the biggest difference is in the fact that the triples are saved, one by one into an
	 * object that streams them directly to the Datalift RDF Store 
	 * @param conn Database connection
	 * @param driver Kind of database driver
	 * @param targetGraph 
	 * @param repository the datalift RDF Store
	 * @return a dataset object that alreadt put the new triples to the repository
	 * @throws UnsupportedEncodingException
	 */
	public static StoreMappedDataSet generateDirectMapping(Connection conn, DriverType driver, String targetGraph, Repository repository) throws UnsupportedEncodingException{
		StoreMappedDataSet result = new StoreMappedDataSet(repository, targetGraph);
		DirectMappingEngine dme = new DirectMappingEngineWD20120529();
		TupleExtractor te = new TupleExtractor(conn, dme, driver, null);
		long start = System.currentTimeMillis();
		Logger dlLog = Logger.getLogger();
		nbTriples = 0;
		lastModulo = 0;
		while (te.next()){
			convertNextTuple(result, te, dme, driver, targetGraph);
		}
		// commit the triples out that didn't make it during the last batch
		result.flush();
		long delay = (System.currentTimeMillis() - start) / 1000;
		try {
	        dlLog.debug("Inserted {} RDF triples into <{}> from {} Database in {} seconds",
	                nbTriples, targetGraph,conn.getCatalog(),
	                Double.valueOf(delay / 1000.0));
		} catch (SQLException e) {
			 dlLog.debug("Inserted {} RDF triples into <{}> from Database in {} seconds",
					nbTriples, targetGraph,
		            Double.valueOf(delay / 1000.0));
		}
		return result;
	}
	
	
	public static SesameDataSet generateDirectMapping(Connection conn, Version version, DriverType driver, String baseURI, String timeZone, String fileToNativeStore) throws UnsupportedEncodingException{
		log.info("Generate Direct Mapping...");
		Long start = System.currentTimeMillis();
		SesameDataSet result = null;
		// Check if use of native store is required
		if (fileToNativeStore != null) {
			result = new SesameDataSet(fileToNativeStore, false);
		} else {
			result = new SesameDataSet();
		}
		DirectMappingEngine dme = null;
		switch (version) {
		case WD_20110324:
			dme = new DirectMappingEngineWD20110324();
			break;
		case WD_20120529:
			dme = new DirectMappingEngineWD20120529();
			break;
		default:
			// Working draft Mars 2011 by default
			dme = new DirectMappingEngineWD20110324();
			break;
		}
		TupleExtractor te = new TupleExtractor(conn, dme, driver, timeZone);
		nbTriples = 0;
		lastModulo = 0;
		while (te.next()){
			convertNextTuple(result, te, dme, driver, baseURI);
		}
		Float stop = Float.valueOf(System.currentTimeMillis() - start) / 1000;
		log.info("Database extracted in "
					+ stop + " seconds.");
		log.info(nbTriples + " triples has been extracted.");
		return result;
	}
	
	private static void convertNextTuple(DataSet result, TupleExtractor te, DirectMappingEngine dme, DriverType driver, String baseURI) throws UnsupportedEncodingException{
		Tuple tuple = te.getCurrentTuple();
		//Tuple tuple = null;
		log.debug("[DirectMapper:convertNextTuple] Tuple extracted : " + tuple);
		try {
			// Extract referenced rows
			HashMap<Key, Tuple> referencedTuples = te.getReferencedTuples(driver, tuple);
			// Extract primary-is-foreign Key 
			Key primaryIsForeignKey = te.getCurrentPrimaryIsForeignKey(referencedTuples.keySet(), tuple);
			log.debug("[DirectMapper:convertNextTuple] Number of referenced tuples generated : " + referencedTuples.values().size());
			
			HashSet<Statement> statements = dme.extractTriplesFrom(tuple, referencedTuples, primaryIsForeignKey, baseURI);
			for (Statement triple : statements) {
				log.debug("[DirectMapper:convertNextTuple] Triple generated : " + triple);
				result.add(triple.getSubject(), triple.getPredicate(), triple
						.getObject());
				nbTriples++;
			}
			if (nbTriples / 50000 > lastModulo){
				log.info(nbTriples + " triples has already been extracted.");
				lastModulo = nbTriples / 50000;
			}
		} catch (UnsupportedEncodingException e) {
			log.error("[DirectMapper:generateDirectMapping] Encoding not supported.");
			e.printStackTrace();
		}
	}
	

}

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
 * Direct Mapping : Tuple extractor
 * 
 * It's the genreic mecanism which extract generic tuple from a given database
 * using JDBC. This object depends on DirectMappingEngine instance used.
 * 
 *
 ****************************************************************************/
package net.antidot.semantic.rdf.rdb2rdf.dm.core;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import net.antidot.sql.model.core.DriverType;
import net.antidot.sql.model.core.SQLConnector;
import net.antidot.sql.model.db.Key;
import net.antidot.sql.model.db.Tuple;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


public class TupleExtractor {

	// Log
	private static Log log = LogFactory.getLog(TupleExtractor.class);

	// Metrics
	private static int moduloValueCheck = 1;

	// Database values data
	private ResultSet currentResultSet;
	private ResultSet currentHeaderSet;
	private ResultSet currentPrimaryKeySet;
	private ResultSet currentImportedKeySet;
	private ResultSet tablesSet;
	private String currentTableName;
	private String currentReferencedTableName;
	private HashMap<String, ResultSet> referencedHeaderMap;
	private HashMap<String, ResultSet> referencedPrimaryKeyMap;
	private HashMap<String, ResultSet> referencedForeignKeyMap;
	private ResultSet currentReferencedHeaderSet;
	private ResultSet currentReferencedPrimaryKeySet;
	private ResultSet currentReferencedImportedKeySet;
	private Key currentPrimaryIsForeignKey;
 
	// Database connection data
	private Statement currentStatement;
	private DatabaseMetaData metas;
	private Connection conn;
	private DirectMappingEngine engine;
	private DriverType driver;
	private String timeZone;

	// Metrics data
	private int currentNbTuplesInTable;
	private int currentNbTuplesExtractedInTable;
	private int lastModuloValue;

	public TupleExtractor(Connection conn, DirectMappingEngine engine,
			DriverType driver, String timeZone) {
		if (conn == null)
			throw new IllegalArgumentException(
					"[TupleExtractor:TupleExtractor] A SQL connection is required !");
		if (engine == null)
			throw new IllegalArgumentException(
					"[TupleExtractor:TupleExtractor] A Direct Mapping engine is required !");
		if (driver == null)
			throw new IllegalArgumentException(
					"[TupleExtractor:TupleExtractor] A driver is required !");

		initConnections(engine, conn, driver);
		initTimeZone(timeZone);
		initMetrics();
		initContextualSets();
		initReferencedContextualSets();
		initReferencedContextualMaps();
		initPrimaryIsForeignKey();
		initExtractor();
	}

	private void initTimeZone(String timeZone) {
		// Check timezone (only for MySQL)
		if ((driver != null) && driver.equals("com.mysql.jdbc.Driver")
				&& (timeZone == null)) {
			if (log.isWarnEnabled())
				log.warn("[TupleExtractor:TupleExtractor] No time zone specified. Use database time zone by default.");
			try {
				this.timeZone = SQLConnector.getTimeZone(conn);
			} catch (SQLException e) {
				log.error("[TupleExtractor:TupleExtractor] SQL error during time zone extraction.");
				e.printStackTrace();
			}
		} else {
			this.timeZone = timeZone;
		}
	}

	private void initPrimaryIsForeignKey() {
		this.currentPrimaryIsForeignKey = null;
	}

	private void initConnections(DirectMappingEngine engine, Connection conn,
			DriverType driver) {
		this.engine = engine;
		this.conn = conn;
		this.driver = driver;
	}

	private void initMetrics() {
		this.currentNbTuplesInTable = -1;
		this.currentNbTuplesExtractedInTable = -1;
		this.lastModuloValue = 0;
	}

	private void initContextualSets() {
		this.currentResultSet = null;
		this.currentHeaderSet = null;
		this.currentPrimaryKeySet = null;
		this.currentImportedKeySet = null;
		this.tablesSet = null;
		this.currentTableName = null;
	}
	
	private void initReferencedContextualSets() {
		this.currentResultSet = null;
		this.currentReferencedHeaderSet = null;
		this.currentReferencedPrimaryKeySet = null;
		this.currentReferencedImportedKeySet = null;
	}

	private void initReferencedContextualMaps() {
		this.currentReferencedTableName = null;
		this.referencedHeaderMap = new HashMap<String, ResultSet>();
		this.referencedPrimaryKeyMap = new HashMap<String, ResultSet>();
		this.referencedForeignKeyMap = new HashMap<String, ResultSet>();
	}

	private void initExtractor() {
		try {
			// Extract metadata from DB
			metas = conn.getMetaData();
			// Generates tables
			tablesSet = metas.getTables(conn.getCatalog(), null, "%", null);
			// Make sure autocommit is off (required for cursor mode)
			conn.setAutoCommit(false);
			currentStatement = conn.createStatement();
			// Turn use of the cursor on.
			currentStatement.setFetchSize(50);
			// Execute first query
			nextTable();

		} catch (SQLException e) {
			log.error("[TupleExtractor:initExtractor] Error SQL during extractor initialization.");
			e.printStackTrace();
		}
	}

	private boolean nextTable() {
		boolean hasTables = false;
		// Move tables cursor
		try {
			hasTables = tablesSet.next();
			if (!hasTables)
				return false;
			
			while (hasTables && (tablesSet.getString("TABLE_TYPE") == null || !tablesSet
					.getString("TABLE_TYPE").equals("TABLE"))) {
				hasTables = tablesSet.next();
			}
			if (!hasTables)
				return false;
			currentTableName = tablesSet.getString("TABLE_NAME");
			log.info("Next table : " + currentTableName);

			extractMetrics();
			extractSets();
		} catch (SQLException e) {
			log.error("[TupleExtractor:nextTable] Error SQL during extracting next table.");
			e.printStackTrace();
		}
		return hasTables;
	}

	private void extractMetrics() {
		// Extract metrics
		String metricsSQLQuery = null;
		if (driver.equals(DriverType.MysqlDriver))
			metricsSQLQuery = "SELECT COUNT(*) FROM `" + currentTableName + "`";
		else
			metricsSQLQuery = "SELECT COUNT(*) FROM \"" + currentTableName + "\"";
		log.debug("[TupleExtractor:extractMetrics] Execute query : " + metricsSQLQuery);
		Statement metricsStatement;
		try {
			metricsStatement = conn.createStatement();
			ResultSet metricsSet = metricsStatement
					.executeQuery(metricsSQLQuery);
			if (metricsSet.next()) {
				int nbTuples = 0;
				nbTuples = metricsSet.getInt(1);
				currentNbTuplesInTable = nbTuples;
				currentNbTuplesExtractedInTable = 0;
				lastModuloValue = 0;
				log.info("Number of tuples in this table : "
								+ nbTuples);
			} else {
				currentNbTuplesInTable = -1;
				currentNbTuplesExtractedInTable = -1;
				lastModuloValue = 0;
				log.warn("[TupleExtractor:extractMetrics] Can not extract number of tuples in this table.");
			}
		} catch (SQLException e) {
			log.error("[TupleExtractor:extractMetrics] Error SQL during extracting metrics sets.");
			e.printStackTrace();
		}
	}
		
	private void extractSets() {
		// Extract header
		try {
			currentHeaderSet = metas.getColumns(null, null, currentTableName, null);
			// Extract values
			String SQLQuery = engine.constructSQLQuery(driver,
					currentHeaderSet, currentTableName);
			log.debug("[TupleExtractor:nextTable] Execute query : " + SQLQuery);
			currentResultSet = currentStatement.executeQuery(SQLQuery);
			
			// Extract primary keys
			currentPrimaryKeySet = metas.getPrimaryKeys(conn.getCatalog(),
					null, currentTableName);
						
			// Extract foreign key
			currentImportedKeySet = metas.getImportedKeys(conn.getCatalog(),
					null, currentTableName);
		} catch (SQLException e) {
			log.error("[TupleExtractor:extractSets] Error SQL during extracting context sets.");
			e.printStackTrace();
		}
	}

	public boolean next() {
		boolean hasNext = false;
		try {
			hasNext = currentResultSet.next();
			while (!hasNext){
				// Check next table
				boolean hasTable = nextTable();
				if (!hasTable)
					return false;
				else {
					hasNext = currentResultSet.next();
				}
				if (!hasNext) log.info("This table is empty.");
			}
			reinitCurrentCursors();
			updateMetrics();
		} catch (SQLException e) {
			log.error("[TupleExtractor:next] Error SQL during extracting of next tuple.");
			e.printStackTrace();
		}
		return hasNext;
	}

	private void reinitCurrentCursors() {
		// Re-init cursors
		try {
			if (!currentHeaderSet.isFirst()) {
				currentHeaderSet.beforeFirst();
			}
			if (!currentPrimaryKeySet.isFirst()) {
				currentPrimaryKeySet.beforeFirst();
			}
			if (!currentImportedKeySet.isFirst()) {
				currentImportedKeySet.beforeFirst();
			}
		} catch (Exception e) {
			log.error("[TupleExtractor:reinitCurrentCursors] Error SQL during reinitilization of cursors.");
			e.printStackTrace();
		}
	}
	
	private void reinitCurrentReferencedCursors() {
		// Re-init cursors
		try {
			if (!currentReferencedHeaderSet.isFirst()) {
				currentReferencedHeaderSet.beforeFirst();
			}
			if (!currentReferencedPrimaryKeySet.isFirst()) {
				currentPrimaryKeySet.beforeFirst();
			}
			if (!currentReferencedImportedKeySet.isFirst()) {
				currentImportedKeySet.beforeFirst();
			}
		} catch (Exception e) {
			log.error("[TupleExtractor:reinitCurrentCursors] Error SQL during reinitilization of referenced cursors.");
			e.printStackTrace();
		}
	}

	private void updateMetrics() {
		// Up metrics
		if (currentNbTuplesInTable != -1 && currentNbTuplesInTable != 0) {
			// Only if number of tuples is known
			currentNbTuplesExtractedInTable++;
			int ratio = (int) Math
					.floor(((double) currentNbTuplesExtractedInTable / currentNbTuplesInTable) * 100.);
			int modulo = ratio / moduloValueCheck;
			if (modulo > lastModuloValue) {
				lastModuloValue = modulo;
				log.info("Extracted tuples : " + ratio + "%");
			}
		}
	}

	public Tuple getCurrentTuple() throws UnsupportedEncodingException {
		// This method depends on Direct Mapping norm used
		return engine.extractTupleFrom(currentResultSet, currentHeaderSet,
				currentPrimaryKeySet, currentImportedKeySet, currentTableName,
				driver, timeZone, currentNbTuplesExtractedInTable);
	}

	public Key getCurrentPrimaryIsForeignKey(Set<Key> referencedKeys,
			Tuple tuple) throws UnsupportedEncodingException {
		if (currentPrimaryIsForeignKey != null)
			return currentPrimaryIsForeignKey;
		else {
			Key primaryIsForeignKey = null;
			for (Key key : referencedKeys) {
				if (engine.isPrimaryKey(key, tuple)) {
					primaryIsForeignKey = getPrimaryIsForeignKey(key, tuple);
					if (primaryIsForeignKey == null)
						log.warn("[TupleExtractor:getReferencedTuples] No primary-is-foreign key extracted whereas it's has been detected.");
					else {
						log.debug("[TupleExtractor:getReferencedTuples] Primary-is-foreign key detected : "
										+ primaryIsForeignKey);
						break;
					}
				}
			}
			return primaryIsForeignKey;
		}
	}

	private void extractReferencedSets(String referencedTableName) {
		
		try {
			if (currentReferencedTableName != referencedTableName) {
				initReferencedContextualMaps();
				this.currentReferencedTableName = referencedTableName;
				// Extract target headers from target table
				ResultSet referencedHeaderSet = metas.getColumns(null, null,
						referencedTableName, null);
				this.referencedHeaderMap.put(referencedTableName, referencedHeaderSet);
				// Extract primary keys
				ResultSet referencedPrimaryKeySet = metas.getPrimaryKeys(conn
						.getCatalog(), null, referencedTableName);
				this.referencedPrimaryKeyMap.put(referencedTableName, referencedPrimaryKeySet);
				// Extract foreign key
				ResultSet referencedForeignKeySet = metas.getImportedKeys(conn
						.getCatalog(), null, referencedTableName);
				this.referencedForeignKeyMap.put(referencedTableName, referencedForeignKeySet);
			}
			currentReferencedHeaderSet = referencedHeaderMap.get(referencedTableName);
			currentReferencedPrimaryKeySet = referencedPrimaryKeyMap.get(referencedTableName);
			currentReferencedImportedKeySet = referencedForeignKeyMap.get(referencedTableName);
		} catch (SQLException e) {
			log.error("[TupleExtractor:extractSets] Error SQL during extracting context sets.");
			e.printStackTrace();
		}
	}

	public Key getPrimaryIsForeignKey(Key key, Tuple tuple) throws UnsupportedEncodingException {
		Key result = key;
		try {
			Statement referencedStatement = conn.createStatement();
			boolean primaryIsForeignKeyException = true;
			while (primaryIsForeignKeyException) {

				String referencedTableName = engine.getReferencedTableName(key);
				extractReferencedSets(referencedTableName);
				String sqlQuery = engine.constructReferencedSQLQuery(driver,
						currentReferencedHeaderSet, referencedTableName, key, tuple);
				reinitCurrentReferencedCursors();
				log.debug("[TupleExtractor:getReferencedTuples] Execute query : "
								+ sqlQuery);
				ResultSet referencedValueSet = referencedStatement
						.executeQuery(sqlQuery);

				// Extract values in database
				if (referencedValueSet.next()) {
					Tuple referencedTuple = engine.extractReferencedTupleFrom(
							referencedValueSet, currentReferencedHeaderSet,
							currentReferencedPrimaryKeySet, currentReferencedImportedKeySet,
							engine.getReferencedTableName(key), driver, null, currentNbTuplesExtractedInTable);
					if (referencedValueSet.next())
						throw new IllegalStateException(
								"[TupleExtractor:getReferencedTuples] Foreign key matches with one element and more, it's unconsistent.");
					HashSet<Key> referencedKeys = engine
							.getReferencedKeys(referencedTuple);
					primaryIsForeignKeyException = false;
					for (Key fk : referencedKeys) {
						if (engine.isPrimaryKey(key, referencedTuple)) {
							primaryIsForeignKeyException = true;
							key = fk;
							result = key;
							tuple = referencedTuple;
							break;
						}
					}
				}
			}
		} catch (SQLException e) {
			log.error("[TupleExtractor:getPrimaryIsForeignKey] Error SQL during extracting primary-is-foreign key.");
			e.printStackTrace();
		}
		return result;

	}

	public HashMap<Key, Tuple> getReferencedTuples(DriverType driver, Tuple tuple) throws UnsupportedEncodingException {
		HashMap<Key, Tuple> result = new HashMap<Key, Tuple>();
		try {
			// Extract target name table
			HashSet<Key> referencedKeys = engine.getReferencedKeys(tuple);
			log.debug("[TupleExtractor:getReferencedTuples] Referenced keys : " + referencedKeys);
			Statement referencedStatement = conn.createStatement();
			referencedStatement.setFetchSize(0); // Default behaviour = no cursor mode
			Key primaryIsForeignKey = null;
			for (Key key : referencedKeys) {
				if (engine.isPrimaryKey(key, tuple)) {
					currentPrimaryIsForeignKey = getPrimaryIsForeignKey(key,
							tuple);
					if (currentPrimaryIsForeignKey == null)
						throw new IllegalStateException(
								"[TupleExtractor:getReferencedTuples] No primary-is-foreign key extracted whereas it's has been detected.");
					else
						log.debug("[TupleExtractor:getReferencedTuples] Primary-is-foreign key detected : "
										+ primaryIsForeignKey);
				} else {
					
					String referencedTableName = engine
							.getReferencedTableName(key);
					extractReferencedSets(referencedTableName);
					String sqlQuery = engine.constructReferencedSQLQuery(
							driver, currentReferencedHeaderSet, referencedTableName,
							key, tuple);
					reinitCurrentReferencedCursors();
					log.debug("[TupleExtractor:getReferencedTuples] Execute query : "
									+ sqlQuery);
					ResultSet referencedValueSet = referencedStatement
							.executeQuery(sqlQuery);

					// Extract values in database
					if (referencedValueSet.next()) {
						Tuple referencedTuple = engine
								.extractReferencedTupleFrom(referencedValueSet,
										currentReferencedHeaderSet,
										currentReferencedPrimaryKeySet,
										currentReferencedImportedKeySet, engine
												.getReferencedTableName(key),
										driver, null, currentNbTuplesExtractedInTable);
						if (referencedValueSet.next())
							throw new IllegalStateException(
									"[TupleExtractor:getReferencedTuples] Foreign key matches with one element and more, it's unconsistent.");
						result.put(key, referencedTuple);
					}
				}
			}
		} catch (SQLException e) {
			log.error("[TupleExtractor:getReferencedTuples] Error SQL during extracting referenced tuples.");
			e.printStackTrace();
		}
		return result;
	}

}

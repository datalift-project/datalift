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
 * Direct Mapping : Direct Mapper Engine of Working Draft 20 September 2011
 *
 * This Direct Mapper Engine implements functions and mecanisms of this working draft.
 * 
 *
 ****************************************************************************/
package net.antidot.semantic.rdf.rdb2rdf.dm.core;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.TreeMap;

import net.antidot.semantic.rdf.model.impl.sesame.SemiStatement;
import net.antidot.semantic.rdf.model.tools.RDFDataValidator;
import net.antidot.semantic.rdf.rdb2rdf.commons.RDFPrefixes;
import net.antidot.semantic.rdf.rdb2rdf.commons.SQLToXMLS;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.core.R2RMLProcessor;
import net.antidot.semantic.rdf.rdb2rdf.r2rml.tools.R2RMLToolkit;
import net.antidot.semantic.xmls.xsd.XSDLexicalTransformation;
import net.antidot.semantic.xmls.xsd.XSDType;
import net.antidot.sql.model.core.DriverType;
import net.antidot.sql.model.db.CandidateKey;
import net.antidot.sql.model.db.ForeignKey;
import net.antidot.sql.model.db.Key;
import net.antidot.sql.model.db.Row;
import net.antidot.sql.model.db.StdBody;
import net.antidot.sql.model.db.StdHeader;
import net.antidot.sql.model.db.StdTable;
import net.antidot.sql.model.db.Tuple;
import net.antidot.sql.model.type.SQLType;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

public class DirectMappingEngineWD20120529 implements DirectMappingEngine {

	// Log
	private static Log log = LogFactory
			.getLog(DirectMappingEngineWD20120529.class);

	// Database values
	private String currentTableName;
	private LinkedHashMap<String, String> datatypes;
	private StdHeader header;
	private ArrayList<CandidateKey> primaryKeys;
	private HashSet<ForeignKey> foreignKeys;

	// Sesame valueFactory which generates values like litterals or URI
	private ValueFactory vf;

	// IRI characters used in rules
	private static char solidus = '/';
	private static char semicolon = ';';
	private static char hash = '#';
	private static char hyphenEquals = '=';
	private static String refInfix = "ref-";

	public DirectMappingEngineWD20120529() {
		initDatabaseValues();
	}

	private void initDatabaseValues() {
		currentTableName = null;
		datatypes = new LinkedHashMap<String, String>();
		header = null;
		primaryKeys = new ArrayList<CandidateKey>();
		foreignKeys = new HashSet<ForeignKey>();
		vf = new ValueFactoryImpl();
	}

	/*
	 * Extract a tuple from database extracted sets.
	 */
	public Tuple extractTupleFrom(ResultSet valueSet, ResultSet headerSet,
			ResultSet primaryKeysSet, ResultSet foreignKeysSet,
			String tableName, DriverType driver, String timeZone, int index)
			throws UnsupportedEncodingException {
		if (tableName != currentTableName)
			// First table treatment
			// Get datatypes
			updateTableContext(headerSet, primaryKeysSet, foreignKeysSet,
					tableName);
		if (datatypes == null || header == null || primaryKeys == null
				|| foreignKeys == null)
			throw new IllegalStateException(
					"[DirectMappingEngine:extractTupleFrom] One of mandatory elements for tuple's building is missing.");
		// Extract row
		Row result = extractRow(driver, header, tableName, valueSet, timeZone,
				index);
		// Attach context to this row
		buildTmpModel(result, tableName, header, primaryKeys, foreignKeys);
		return result;
	}

	/*
	 * Update data associated with current table.
	 */
	private void updateTableContext(ResultSet headerSet,
			ResultSet primaryKeysSet, ResultSet foreignKeysSet, String tableName) {
		// First table treatment
		// Get datatypes
		datatypes = extractDatatypes(headerSet, tableName);
		header = new StdHeader(datatypes);
		// Extract candidate keys
		primaryKeys = extractPrimaryKeys(primaryKeysSet, header, tableName);
		// Extract foreign key
		if (foreignKeysSet != null)
			foreignKeys = extractForeignKeys(foreignKeysSet, tableName);
	}

	/*
	 * Extract a referenced tuple (tuple pointed by a foreign key) from database
	 * extracted sets.
	 */
	public Tuple extractReferencedTupleFrom(ResultSet valueSet,
			ResultSet headerSet, ResultSet primaryKeysSet,
			ResultSet foreignKeysSet, String tableName, DriverType driver,
			String timeZone, int index) throws UnsupportedEncodingException {
		// Get datatypes
		LinkedHashMap<String, String> referencedDatatypes = extractDatatypes(
				headerSet, tableName);
		StdHeader referencedHeader = new StdHeader(referencedDatatypes);
		// Extract candidate keys
		ArrayList<CandidateKey> referencedPrimaryKeys = extractPrimaryKeys(
				primaryKeysSet, referencedHeader, tableName);
		// Extract foreign key
		HashSet<ForeignKey> referencedForeignKeys = extractForeignKeys(
				foreignKeysSet, tableName);
		if (datatypes == null || header == null || primaryKeys == null)
			throw new IllegalStateException(
					"[DirectMappingEngine:extractTupleFrom] One of mandatory elements for tuple's building is missing.");
		// Extract row
		Row result = extractRow(driver, referencedHeader, tableName, valueSet,
				timeZone, index);
		// Attach context to this row
		buildTmpModel(result, tableName, referencedHeader,
				referencedPrimaryKeys, referencedForeignKeys);
		return result;
	}

	/*
	 * Build objects associated with a row from database extracted sets.
	 */
	private void buildTmpModel(Row row, String tableName, StdHeader header,
			ArrayList<CandidateKey> primaryKeys, HashSet<ForeignKey> foreignKeys) {
		// Create body
		HashSet<Row> rows = new HashSet<Row>();
		rows.add(row);
		StdBody body = new StdBody(rows, null);
		// Create table
		StdTable table = new StdTable(tableName, header, primaryKeys,
				foreignKeys, body);
		// Link objects
		body.setParentTable(table);
		row.setParentBody(body);
	}

	/*
	 * Extract a row from values datasets and its model.
	 */
	private Row extractRow(DriverType driver, StdHeader header, String tableName,
			ResultSet valueSet, String timeZone, int index)
			throws UnsupportedEncodingException {
		TreeMap<String, byte[]> values = new TreeMap<String, byte[]>();
		for (String columnName : header.getColumnNames()) {
			try {
				byte[] value = null;
				// SQLType type =
				// SQLType.toSQLType(Integer.valueOf(header.getDatatypes().get(columnName)));
				value = valueSet.getBytes(columnName);

				// http://bugs.mysql.com/bug.php?id=65943
				if(value != null && 
					driver.equals(DriverType.MysqlDriver) &&
					SQLType.toSQLType(valueSet.getMetaData().getColumnType(valueSet.findColumn(columnName))) == SQLType.CHAR) {
				    value = valueSet.getString(columnName).getBytes();
				}

				values.put(columnName, value);
			} catch (SQLException e) {
				log.error("[DirectMappingEngine:extractRow] SQL Error during row extraction");
				e.printStackTrace();
			}
		}
		Row row = new Row(values, null, index);
		return row;
	}

	/*
	 * Extract datatypes from a table and its header set.
	 */
	private LinkedHashMap<String, String> extractDatatypes(
			ResultSet headersSet, String tableName) {
		LinkedHashMap<String, String> datatypes = new LinkedHashMap<String, String>();
		try {
			while (headersSet.next()) {
				String type = headersSet.getString("DATA_TYPE");
				String column = headersSet.getString("COLUMN_NAME");
				checkBlobType(type, tableName, column);
				datatypes.put(column, type);
			}
		} catch (SQLException e) {
			log.error("[DirectMappingEngine:extractDatatypes] SQL Error during datatype's tuples extraction");
			e.printStackTrace();
		}
		return datatypes;
	}

	/*
	 * Check blob types. This type is not supported in this working draft.
	 */
	private void checkBlobType(String type, String tableName, String column) {
		if (SQLType.toSQLType(Integer.valueOf(type)).isBlobType())
			log.warn("[DirectMapper:checkBlobType] WARNING Table "
					+ tableName
					+ ", column "
					+ column
					+ " Forbidden BLOB type (binary stream not supported in XSD)"
					+ " => this column will be ignored.");
	}

	/*
	 * Extract primary keys from database sets.
	 */
	private ArrayList<CandidateKey> extractPrimaryKeys(
			ResultSet primaryKeysSet, StdHeader header, String tableName) {
		ArrayList<CandidateKey> primaryKeys = new ArrayList<CandidateKey>();
		// In particular : primary key

		ArrayList<String> columnNames = new ArrayList<String>();
		int size = 0;
		// Extract columns names
		try {
			while (primaryKeysSet.next()) {
				size++;
				String columnName = primaryKeysSet.getString("COLUMN_NAME");
				columnNames.add(columnName);
			}
		} catch (SQLException e) {
			log.error("[DirectMappingEngine:extractPrimaryKeys] SQL Error during primary key of tuples extraction");
			e.printStackTrace();
		}
		// Sort columns
		ArrayList<String> sortedColumnNames = new ArrayList<String>();
		for (String columnName : header.getColumnNames()) {
			if (columnNames.contains(columnName))
				sortedColumnNames.add(columnName);
		}
		// Create object
		if (size != 0) {
			CandidateKey primaryKey = new CandidateKey(sortedColumnNames,
					tableName, CandidateKey.KeyType.PRIMARY);
			primaryKeys.add(primaryKey);
		}
		if (primaryKeys.size() > 1)
			throw new IllegalStateException(
					"[DirectMappingEngine:extractPrimaryKeys] Table "
							+ tableName + " has more primary keys.");
		return primaryKeys;
	}

	/*
	 * Extract foreign keys from database sets.
	 */
	private HashSet<ForeignKey> extractForeignKeys(ResultSet foreignKeysSet,
			String tableName) {
		// Extract foreign key
	    	log.error("[DirectMappingEngine:extractForeignKeys] Extract Foreign Keys for : " + tableName);	    
		HashSet<ForeignKey> foreignKeys = new HashSet<ForeignKey>();
		String currentPkTableName = null;
		ArrayList<String> pkColumnNames = new ArrayList<String>();
		ArrayList<String> fkColumnNames = new ArrayList<String>();
		try {
			while (foreignKeysSet.next()) {
				String pkTableName;
				// Foreign key infos
				pkTableName = foreignKeysSet.getString("PKTABLE_NAME");

				String pkColumnName = foreignKeysSet.getString("PKCOLUMN_NAME");
				String fkTableName = foreignKeysSet.getString("FKTABLE_NAME");
				String fkColumnName = foreignKeysSet.getString("FKCOLUMN_NAME");
				int fkSequence = foreignKeysSet.getInt("KEY_SEQ");
				// Consistency test
				if (!fkTableName.equals(tableName))
					throw new IllegalStateException(
							"[DirectMappingEngine:extractForeignKeys] Unconsistency between source "
									+ "table of foreign key and current table : "
									+ tableName + " != " + fkTableName);

				if (fkSequence == 1) { // Sequence == order of column in
					// Multi-column foreign key
					// New foreign key => store last key
					storeForeignKey(foreignKeys, fkColumnNames, pkColumnNames,
							tableName, currentPkTableName);
					fkColumnNames = new ArrayList<String>();
					pkColumnNames = new ArrayList<String>();
				}
				currentPkTableName = pkTableName;
				pkColumnNames.add(pkColumnName);
				fkColumnNames.add(fkColumnName);
			}
			log.error("[DirectMappingEngine:extractForeignKeys] Store now last foreign key");
			// Store last key
			storeForeignKey(foreignKeys, fkColumnNames, pkColumnNames,
					tableName, currentPkTableName);

		} catch (SQLException e) {
		    	log.error("[DirectMappingEngine:extractForeignKeys] SQL Error during foreign keys of tuples extraction");
			e.printStackTrace();
		}
		return foreignKeys;
	}

	/*
	 * Create foreign key object.
	 */
	private void storeForeignKey(HashSet<ForeignKey> foreignKeys,
			ArrayList<String> fkColumnNames, ArrayList<String> pkColumnNames,
			String tableName, String currentPkTableName) {
		log.debug("[DirectMappingEngine:storeForeignKey] Store foreign key : "
				+ pkColumnNames);
		if (fkColumnNames.size() != 0)
			foreignKeys.add(new ForeignKey(fkColumnNames, tableName,
					new CandidateKey(pkColumnNames, currentPkTableName,
							CandidateKey.KeyType.REFERENCE)));
	}

	/*
	 * Construct SQL Query from database sets in order to extract row from its
	 * table.
	 */
	public String constructSQLQuery(DriverType driver, ResultSet headersSet,
			String tableName) {
		LinkedHashMap<String, String> datatypes = extractDatatypes(headersSet,
				tableName);
		StdHeader header = new StdHeader(datatypes);
		// Construct SQL query
		String SQLQuery = "SELECT ";
		int i = 0;
		for (String columnName : header.getColumnNames()) {
			i++;
			// Extract SQL date format in a ISO 8601 format
			SQLType type = SQLType.toSQLType(Integer.valueOf(header
					.getDatatypes().get(columnName)));
			if (type == null) {
				throw new IllegalStateException(
						"[DirectMappingEngine:constructSQLQuery] Unknown SQL type : "
								+ header.getDatatypes().get(columnName)
								+ " from column : " + columnName);
			}
			if (type == SQLType.UNKNOWN) {
				log.warn("[DirectMappingEngine:constructSQLQuery] Unknown SQL type : "
						+ header.getDatatypes().get(columnName)
						+ " from column : " + columnName);
			}
			if (driver.equals(DriverType.MysqlDriver))
				SQLQuery += "`" + columnName + "`";
			else
			    	SQLQuery += "\"" + columnName + "\"";
			if (i < header.getColumnNames().size())
				SQLQuery += ", ";
		}
		if (driver.equals(DriverType.MysqlDriver))
			SQLQuery += " FROM `" + tableName + "`;";
		else
		    	SQLQuery += " FROM \"" + tableName + "\";";
		return SQLQuery;
	}

	/*
	 * Construct SQL Query from database sets in order to extract row from its
	 * table.
	 */
	public String constructReferencedSQLQuery(DriverType driver,
			ResultSet headersSet, String tableName, Key key, Tuple tuple) {
		// Explicit conversion
		ForeignKey fk = (ForeignKey) key;
		Row r = (Row) tuple;
		LinkedHashMap<String, String> datatypes = extractDatatypes(headersSet,
				tableName);
		StdHeader header = new StdHeader(datatypes);
		// Construct SQL query
		// SELECT clause
		String SQLQuery = "SELECT ";
		int i = 0;
		for (String columnName : header.getColumnNames()) {
			i++;
			// Extract SQL date format in a ISO 8601 format
			SQLType type = SQLType.toSQLType(Integer.valueOf(header
					.getDatatypes().get(columnName)));
			if (type == null) {
				throw new IllegalStateException(
						"[DirectMappingEngine:constructSQLQuery] Unknown SQL type : "
								+ header.getDatatypes().get(columnName)
								+ " from column : " + columnName);
			}
			if (type == SQLType.UNKNOWN) {
				if (log.isWarnEnabled())
					log.warn("[DirectMappingEngine:constructSQLQuery] Unknown SQL type : "
							+ header.getDatatypes().get(columnName)
							+ " from column : " + columnName);
			}
			if (driver.equals(DriverType.MysqlDriver))
				SQLQuery += "`" + columnName + "`";
			else
			    	SQLQuery += "\"" + columnName + "\"";
			if (i < header.getColumnNames().size())
				SQLQuery += ", ";
		}
		if (driver.equals(DriverType.MysqlDriver))
			SQLQuery += " FROM `" + tableName + "`";
		else
		    	SQLQuery += " FROM \"" + tableName + "\"";
		// WHERE clause
		SQLQuery += " WHERE ";
		int j = 0;
		ArrayList<String> columnNames = fk.getReferenceKey().getColumnNames();
		for (String columnName : columnNames) {
		    	String finalColumnName = "\"" + columnName + "\"";
			if (driver.equals(DriverType.MysqlDriver)) {
			    finalColumnName = "`" + columnName + "`";
			}

			final byte[] bs = r.getValues().get(fk.getColumnNames().get(j));
			if (bs == null) {
				// Always use IS NULL to look for NULL values.
			    SQLQuery += finalColumnName + " IS NULL";
			} else {
			    String value = new String(bs);
			    SQLQuery += finalColumnName + " = '" + value + "'";
			}
				
			j++;
			if (j < columnNames.size())
				SQLQuery += " AND ";
			else
				SQLQuery += ";";

		}
		return SQLQuery;
	}

	/*
	 * Most of the functions defining the Direct Mapping are higher-order
	 * functions parameterized by a function φ (phi) : Row → Node. This function
	 * maps any row to a unique node IRI or Blank Node [35]/[36].
	 * 
	 * @throws UnsupportedEncodingException
	 */
	private Resource phi(StdTable t, Row r, String baseURI)
			throws UnsupportedEncodingException {
		if (log.isDebugEnabled())
			log.debug("[DirectMappingEngine:phi] Table : " + t);
		CandidateKey primaryKey = t.getPrimaryKey();
		if (primaryKey != null) {
			// Unique Node IRI
			String stringURI = generateUniqNodeIRI(r, t, primaryKey, baseURI);
			URI uri = vf.createURI(baseURI, stringURI);
			return uri;
		} else {
			// Blank node
			BNode bnode = vf.createBNode(generateUniqBlankNodeName(r));
			return bnode;
		}
	}

	/*
	 * Phi function for referenced rows (in particular for blank node mangement)
	 */
	private Resource phi(StdTable t, Row row, Row referencedRow, String baseURI)
			throws UnsupportedEncodingException {
     	log.debug("[DirectMappingEngine:phi] Table : " + t);
		
		CandidateKey primaryKey = t.getPrimaryKey();
		if (primaryKey != null) {
			// Unique Node IRI
			String stringURI = generateUniqNodeIRI(referencedRow, t,
					primaryKey, baseURI);
					
			URI uri = vf.createURI(baseURI, stringURI);
			// URIs.put(r, uri);
			return uri;
		} else {
			// Blank node
			BNode bnode = vf.createBNode(generateUniqBlankNodeName(referencedRow));
			return bnode;
		}
	}

	/*
	 * Percent encode
	 */
	private String percentEncode(String value, boolean isAttributeName,
			boolean isAttributeValue) {
		// Replace the string with the IRI-safe form of that character per
		// section 7.3 of [R2RML].
		String result = R2RMLToolkit.getIRISafeVersion(value);
		// For attribute names, replace each HYPHEN-MINUS character ('-',
		// U+003d) with the string "%3D".
		if (isAttributeName)
			result = result.replaceAll("\\-", "%3D");
		// For attribute values, replace each FULL STOP character ('.', U+002e)
		// with the string "%2E".
		if (isAttributeValue)
			result = result.replaceAll("\\-", "%2E");
		return result;
	}

	/*
	 * Generate IRI name from database information.
	 */
	private String generateUniqNodeIRI(Row r, StdTable t,
			CandidateKey primaryKey, String baseURI)
			throws UnsupportedEncodingException {
		String stringURI = percentEncode(t.getTableName(), false, false)
				+ solidus;
		int i = 0;
		for (String columnName : primaryKey.getColumnNames()) {
			i++;
			final byte[] bs = r.getValues().get(columnName);
			stringURI += percentEncode(columnName, true, false) + hyphenEquals
					+ percentEncode(new String(bs), false, true);
			if (i < primaryKey.getColumnNames().size())
				stringURI += semicolon;
		}
		// Check URI syntax
		if (!RDFDataValidator.isValidURI(baseURI + stringURI)) {
			if (log.isWarnEnabled())
				log.warn("[DirectMappingEngine:phi] This URI is not valid : "
						+ baseURI + stringURI);
		}
		return stringURI;
	}

	/*
	 * Generate blank name (useful for link row and referenced key in case of
	 * empty primary key).
	 */
	private String generateUniqBlankNodeName(Row r)
			throws UnsupportedEncodingException {
		String blankNodeUniqName = r.getIndex() + "-";
		int i = 1;
		for (String columnName : r.getValues().keySet()) {
			final byte[] bs = r.getValues().get(columnName);
			blankNodeUniqName += percentEncode(columnName, true, false)
					+ hyphenEquals
					+ percentEncode(new String(bs), false, true);
			if (i < r.getValues().size())
				blankNodeUniqName += semicolon;
			i++;
		}
		// Bug of Jena......
		return blankNodeUniqName.replace("%", "P").replace(";", "S").replace("=", "-");
	}

	/*
	 * Primary-is-Candidate-Key Exception If the primary key is also a candidate
	 * key K to table R : - The shared subject is the subject of the referenced
	 * row in R. - The foreign key K generates no reference triple. - Even if K
	 * is a single-column foreign key, it generates a literal triple.
	 */
	private HashSet<Statement> convertPrimaryIsCandidateKey(Row row,
			Row referencedRow, ForeignKey fk, String baseURI)
			throws UnsupportedEncodingException {
		HashSet<Statement> result = new HashSet<Statement>();
		// Generate URI subject

		String saveTableName = referencedRow.getParentBody().getParentTable()
				.getTableName();
		StdTable referencedTable = referencedRow.getParentBody()
				.getParentTable();
		referencedTable.setTableName(fk.getTargetTableName());

		Resource s = phi(referencedTable, referencedRow, baseURI);
		referencedTable.setTableName(saveTableName);
		// Generate predicates and objects
		for (String columnName : fk.getColumnNames()) {
			// For each column in candidate key, a literal triple is generated
			Statement t = convertLex(s, row.getParentBody().getParentTable()
					.getHeader(), row, columnName, baseURI);
			result.add(t);
		}
		return result;
	}

	/*
	 * Denotational semantics function : convert foreign key columns into a
	 * triple with mapped (predicate, object).
	 */
	private HashSet<SemiStatement> convertRef(Row row, Row referencedRow,
			ForeignKey fk, String baseURI) throws UnsupportedEncodingException {
		log.debug("[DirectMappingEngine:convertRef] Row : " + row + " Referenced row : " + referencedRow);
		HashSet<SemiStatement> result = new HashSet<SemiStatement>();
		ArrayList<String> columnNames = new ArrayList<String>();
		columnNames.addAll(fk.getColumnNames());
		URI p = convertCol(row, columnNames, baseURI, true);

		// Do NOT build if ANY ref is "null"
		for(String colName : fk.getReferenceKey().getColumnNames())
		{
		    if(referencedRow.getValues().get(colName) == null)
		    {
			log.debug("[DirectMappingEngine:convertRef] Return since " + colName + " is null");
			return result;
		    }
		}
		// Get URI of target table
		Resource o = phi(referencedRow.getParentBody().getParentTable(), row,
				referencedRow, baseURI);
		SemiStatement semiTriple = new SemiStatement(p, o);
		result.add(semiTriple);

		return result;
	}

	/*
	 * Denotational semantics function : convert row into triples.
	 */
	public HashSet<Statement> extractTriplesFrom(Tuple t,
			HashMap<Key, Tuple> referencedTuples, Key primaryIsForeignKey,
			String baseURI) throws UnsupportedEncodingException {
		log.debug("[DirectMappingEngine:extractTriplesFrom] Tuple : " + t
				+ ", referencedTuples : " + referencedTuples);
		// Explicit conversion
		Row r = (Row) t;
		HashMap<ForeignKey, Row> referencedRows = new HashMap<ForeignKey, Row>();
		for (Key key : referencedTuples.keySet()) {
			referencedRows.put((ForeignKey) key,
					(Row) referencedTuples.get(key));
		}
		ForeignKey primaryIsFk = (ForeignKey) primaryIsForeignKey;
		HashSet<Statement> result = new HashSet<Statement>();
		StdTable currentTable = r.getParentBody().getParentTable();

		Resource s = phi(currentTable, r, baseURI);
		// Temporary set of triple used to store triples
		// before attribute them their subject
		// It's necessary for manage the Primary-is-Candidate-Key Exception
		// which can modify subject
		HashSet<SemiStatement> tmpResult = new HashSet<SemiStatement>();
		if (primaryIsForeignKey != null) {
			// Primary-is-Candidate-Key Exception
			HashSet<Statement> primaryIsCandidateKeyTriples = convertPrimaryIsCandidateKey(
					r, r, primaryIsFk, baseURI);
			// Add these triples to result
			boolean firstTriple = true;
			for (Statement primaryIsCandidateKeyTriple : primaryIsCandidateKeyTriples) {
				// Modify subject
				if (firstTriple) {
					s = primaryIsCandidateKeyTriple.getSubject();
					firstTriple = false;
				}
				result.add(primaryIsCandidateKeyTriple);
			}
		}
		for (ForeignKey fk : referencedRows.keySet()) {
			HashSet<SemiStatement> refTriples = convertRef(r,
					referencedRows.get(fk), fk, baseURI);
			for (SemiStatement refTriple : refTriples) {
				tmpResult.add(refTriple);
			}
		}
		// Construct ref triples with correct subject
		for (SemiStatement refSemiTriple : tmpResult) {
			Statement triple = vf.createStatement(s,
					refSemiTriple.getPredicate(), refSemiTriple.getObject());
			if (triple != null)
				result.add(triple);
		}
		// Literal Triples
		// Difference with preceding Working Draft : even unary foreign key are
		// converted.
		for (String columnName : currentTable.getHeader().getColumnNames()) {
			Statement triple = convertLex(s, currentTable.getHeader(), r,
					columnName, baseURI);
			if (triple != null) {
				result.add(triple);
			}
		}
		// Update current table
		if (currentTableName != currentTable.getTableName()) {
			// currentTableName = currentTable.getTableName();
			Statement typeStatement = convertType(s, baseURI, currentTable);
			result.add(typeStatement);
		}
		return result;
	}

	/*
	 * Convert type predicate from a row.
	 */
	private Statement convertType(Resource s, String baseURI,
			StdTable currentTable) {
		// Table Triples
		URI typePredicate = vf.createURI(RDFPrefixes.prefix.get("rdf"), "type");
		URI typeObject = vf.createURI(baseURI,
				percentEncode(currentTable.getTableName(), false, false));
		Statement typeTriple = vf.createStatement(s, typePredicate, typeObject);
		return typeTriple;
	}

	/*
	 * Denotational semantics function : convert lexical columns into a triple
	 * with mapped (predicate, object).
	 * 
	 * @throws UnsupportedEncodingException
	 */
	
	/**
	 * Denotational semantics function : convert lexical columns into a triple
	 * with mapped (predicate, object).
	 * 
	 * @param subject The subject to add for this triple.
	 * @param header
	 * @param r
	 * @param columnName
	 * @param baseURI
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	private Statement convertLex(
		Resource subject,
		StdHeader header,
		Row r,
		String columnName,
		String baseURI) throws UnsupportedEncodingException {
	    
	    	log.debug("[DirectMappingEngine:convertLex] Table "
					+ r.getParentBody().getParentTable().getTableName()
					+ ", column : " + columnName);
		Statement result = null;
		Literal l = null;
		ArrayList<String> columnNames = new ArrayList<String>();
		columnNames.add(columnName);
		URI p = convertCol(r, columnNames, baseURI, false);
		byte[] v = r.getValues().get(columnName);
		String d = header.getDatatypes().get(columnName);
		if (v == null || v.equals("null")) {
			// Don't keep triple with null value
			return null;
		}
		XSDType type = null;
		SQLType sqlType = SQLType.toSQLType(Integer.valueOf(d));
		if (sqlType.isBlobType()) {
			if (log.isDebugEnabled())
				log.debug("[DirectMappingEngine:convertLex] Table "
						+ r.getParentBody().getParentTable().getTableName()
						+ ", column "
						+ columnName
						+ " Forbidden BLOB type (binary stream not supported in XSD)"
						+ " => this triple will be ignored.");
			return null;
		} else {
			type = SQLToXMLS.getEquivalentType(Integer.valueOf(d));
			if (type == null)
				throw new IllegalStateException(
						"[DirectMappingEngine:convertLex] Unknown XSD equivalent type of : "
								+ SQLType.toSQLType(Integer.valueOf(d))
								+ " in column : "
								+ columnName
								+ " in table : "
								+ r.getParentBody().getParentTable()
										.getTableName());
		}
		// Canonical lexical form
		String v_str = XSDLexicalTransformation.extractNaturalRDFFormFrom(type, v);
		if (type.toString().equals(XSDType.STRING.toString())) {
			l = vf.createLiteral(v_str);
		} else {
			URI datatype_iri = convertDatatype(d);
			if (datatype_iri == null) {
				l = vf.createLiteral(v_str);
			} else {
				l = vf.createLiteral(v_str, datatype_iri);
			}
		}
		result = vf.createStatement(subject, p, (Value) l);
		return result;
	}

	/*
	 * Denotational semantics function : convert row and columnNames into
	 * predicate URI.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	private URI convertCol(Row row, ArrayList<String> columnNames,
			String baseURI, boolean isRef) throws UnsupportedEncodingException {
		String label = percentEncode(row.getParentBody().getParentTable()
				.getTableName(), false, false)
				+ hash;
		if (isRef)
			label += refInfix;
		int i = 0;
		for (String columnName : columnNames) {
			i++;
			label += percentEncode(columnName, true, false);
			if (i < columnNames.size())
				label += semicolon;
		}
		// Check URI syntax
		if (!RDFDataValidator.isValidURI(baseURI + label)) {
			log.warn("[DirectMappingEngine:convertCol] This URI is not valid : "
					+ baseURI + label);
			log.warn("[DirectMappingEngine:convertCol] This URI has been converted into : "
					+ baseURI + label);
		}
		// Create value factory which build URI
		return vf.createURI(baseURI, label);
	}

	/*
	 * Denotational semantics function : convert datatype from SQL into xsd
	 * datatype.
	 */
	private URI convertDatatype(String datatype) {
		String upDatatype = datatype.toUpperCase();
		if (!SQLToXMLS.isValidSQLDatatype(Integer.valueOf(upDatatype)))
			log.debug("[DirectMappingEngine:convertDatatype] Unknown datatype : "
					+ datatype);
		String xsdDatatype = SQLToXMLS.getEquivalentType(
				Integer.valueOf(upDatatype)).toString();
		return vf.createURI(RDFPrefixes.prefix.get("xsd"), xsdDatatype);
	}

	/*
	 * Objects accessors with adapted explicit conversion.
	 */
	public String getReferencedTableName(Tuple tuple) {
		// Explicit conversion
		Row r = (Row) tuple;
		return r.getParentBody().getParentTable().getTableName();
	}

	public String getReferencedTableName(Key key) {
		// Explicit conversion
		ForeignKey referenceKey = (ForeignKey) key;
		return referenceKey.getTargetTableName();
	}

	public HashSet<Key> getReferencedKeys(Tuple tuple) {
		// Explicit conversion
		Row r = (Row) tuple;
		HashSet<Key> keys = new HashSet<Key>();
		for (ForeignKey fk : r.getParentBody().getParentTable()
				.getForeignKeys())
			keys.add(fk);
		return keys;
	}

	public boolean isPrimaryKey(Key key, Tuple tuple) {
		// Explicit conversion
		ForeignKey fk = (ForeignKey) key;
		Row r = (Row) tuple;
		CandidateKey pk = r.getParentBody().getParentTable().getPrimaryKey();
		if (pk == null)
			return false;
		else
			return fk.matchSameColumns(pk);
	}
}

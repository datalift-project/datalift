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
 * Direct Mapping : Direct Mapper Engine of Working Draft 24 March 2011
 *
 * This Direct Mapper Engine implements functions and mecanisms of this working draft.
 * 
 *
 ****************************************************************************/
package net.antidot.semantic.rdf.rdb2rdf.dm.core;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import net.antidot.semantic.rdf.rdb2rdf.commons.SpecificSQLToXMLS;
import net.antidot.semantic.xmls.xsd.XSDType;
import net.antidot.sql.model.core.DriverType;
import net.antidot.sql.model.core.SQLConnector;
import net.antidot.sql.model.db.CandidateKey;
import net.antidot.sql.model.db.ForeignKey;
import net.antidot.sql.model.db.Key;
import net.antidot.sql.model.db.Row;
import net.antidot.sql.model.db.StdBody;
import net.antidot.sql.model.db.StdHeader;
import net.antidot.sql.model.db.StdTable;
import net.antidot.sql.model.db.Tuple;
import net.antidot.sql.model.type.SQLSpecificType;

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


public class DirectMappingEngineWD20110324 implements DirectMappingEngine {

	// Log
	private static Log log = LogFactory
			.getLog(DirectMappingEngineWD20110324.class);

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
	private static char fullStop = ',';
	private static char hash = '#';
	private static char hyphenMinus = '=';

	public DirectMappingEngineWD20110324() {
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
			String tableName, DriverType driver, String timeZone, int index) throws UnsupportedEncodingException {
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
		Row result = extractRow(driver, header, tableName, valueSet, timeZone, index);
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
		StdTable table = new StdTable(tableName, header, primaryKeys, foreignKeys,
				body);
		// Link objects
		body.setParentTable(table);
		row.setParentBody(body);
	}

	/*
	 * Extract a row from values datasets and its model.
	 */
	private Row extractRow(DriverType driver, StdHeader header, String tableName,
			ResultSet valueSet, String timeZone, int index) throws UnsupportedEncodingException {
		TreeMap<String, byte[]> values = new TreeMap<String, byte[]>();
		for (String columnName : header.getColumnNames()) {
			try {
				byte[] value = null;
				SQLSpecificType type = SQLSpecificType.toSQLType(header.getDatatypes().get(
						columnName));

				if ((driver != null) && driver.equals("com.mysql.jdbc.Driver")
						&& type.isDateType()) {
					// Particular treatment for MySQL dates
					extractMySQLDate(columnName, valueSet, tableName, type, timeZone, values);
				} else {
					value = valueSet.getBytes(columnName);
					values.put(columnName, value);
				}
			} catch (SQLException e) {
				log.error("[TupleExtractor:extractRow] SQL Error during row extraction");
				e.printStackTrace();
			}
		}
		Row row = new Row(values, null, index);
		return row;
	}

	/*
	 * Special treatment for MySQL date.
	 */
	private void extractMySQLDate(String columnName, ResultSet valueSet,
			String tableName, SQLSpecificType type, String timeZone,
			TreeMap<String, byte[]> values) {
		// Optimization of datatype
		try {
			SQLSpecificType.toSQLType(header.getDatatypes().get(columnName));
			// Convert date into timestamp
			String value;
			value = valueSet.getString("UNIX_TIMESTAMP(`" + columnName + "`)");

			if (value == null) {
					log
							.debug("[SQLConnection:extractDatabase] Null timestamp for type "
									+ header.getDatatypes().get(columnName)
									+ " from column "
									+ columnName
									+ " in table " + tableName);
				values.put(columnName, (new String("null")).getBytes());
			} else {
					log
							.debug("[SQLConnection:extractDatabase] Timestamp value : "
									+ value);
				// Store date values in appropriate date format
				values.put(columnName, SQLConnector.dateFormatToDate(type, Long
						.valueOf(value), timeZone).getBytes());
			}
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/*
	 * Extract datatypes from a table and its header set.
	 */
	private LinkedHashMap<String, String> extractDatatypes(
			ResultSet headersSet, String tableName) {
		LinkedHashMap<String, String> datatypes = new LinkedHashMap<String, String>();
		try {
			while (headersSet.next()) {
				String type = headersSet.getString("TYPE_NAME");
				String column = headersSet.getString("COLUMN_NAME");
				checkBlobType(type, tableName, column);
				datatypes.put(column, type);
			}
		} catch (SQLException e) {
				log.error("[TupleExtractor:extractDatatypes] SQL Error during datatype's tuples extraction");
			e.printStackTrace();
		}
		return datatypes;
	}
	
	/*
	 * Check blob types. This type is not supported in this working draft.
	 */
	private void checkBlobType(String type, String tableName, String column){
		if (SQLSpecificType.toSQLType(type).isBlobType())
			log
					.warn("[DirectMapper:checkBlobType] WARNING Table "
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
			log.error("[DirectMappingEngineWD20110324:extractTupleFrom] SQL Error during primary key of tuples extraction");
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
							"[SQLConnection:extractDatabase] Unconsistency between source "
									+ "table of foreign key and current table : "
									+ tableName + " != " + fkTableName);

				if (fkSequence == 1) { // Sequence == order of column in
					// Multi-column foreign key
					// New foreign key => store last key
					storeForeignKey(foreignKeys, fkColumnNames, pkColumnNames,
							tableName, currentPkTableName);
					// TODO : check if this value is the same for another
					// SGBD than MySQL
					fkColumnNames = new ArrayList<String>();
					pkColumnNames = new ArrayList<String>();
				}
				currentPkTableName = pkTableName;
				pkColumnNames.add(pkColumnName);
				fkColumnNames.add(fkColumnName);
			}
			// Store last key
			storeForeignKey(foreignKeys, fkColumnNames, pkColumnNames,
					tableName, currentPkTableName);

		} catch (SQLException e) {
			if (log.isErrorEnabled())
				log
						.error("[DirectMappingEngineWD20110324:extractTupleFrom] SQL Error during foreign keys of tuples extraction");
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
		if (fkColumnNames.size() != 0)
			foreignKeys.add(new ForeignKey(fkColumnNames, tableName,
					new CandidateKey(pkColumnNames, currentPkTableName,
							CandidateKey.KeyType.REFERENCE)));
	}

	/*
	 * Construct SQL Query from database sets in order to extract row from its table.
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
			SQLSpecificType type = SQLSpecificType.toSQLType(header.getDatatypes().get(
					columnName));
			if (type == null) {
				throw new IllegalStateException(
						"[DirectMappingEngineWD20110324:constructSQLQuery] Unknown SQL type : "
								+ header.getDatatypes().get(columnName)
								+ " from column : " + columnName);
			}
			if (type == SQLSpecificType.UNKNOW) {
					log.warn("[DirectMappingEngineWD20110324:constructSQLQuery] Unknown SQL type : "
									+ header.getDatatypes().get(columnName)
									+ " from column : " + columnName);
			}
			// MySQL date special treatment
			if ((driver != null) && driver.equals("com.mysql.jdbc.Driver")
					&& type.isDateType()) {
				SQLQuery += "UNIX_TIMESTAMP(`" + columnName + "`)";
			} else {
				SQLQuery += "`" + columnName + "`";
			}
			if (i < header.getColumnNames().size())
				SQLQuery += ", ";
		}
		SQLQuery += " FROM `" + tableName + "`;";
		return SQLQuery;
	}

	/*
	 * Construct SQL Query from database sets in order to extract row from its table.
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
			SQLSpecificType type = SQLSpecificType.toSQLType(header.getDatatypes().get(
					columnName));
			if (type == null) {
				throw new IllegalStateException(
						"[DirectMappingEngineWD20110324:constructSQLQuery] Unknown SQL type : "
								+ header.getDatatypes().get(columnName)
								+ " from column : " + columnName);
			}
			if (type == SQLSpecificType.UNKNOW) {
				if (log.isWarnEnabled())
					log
							.warn("[DirectMappingEngineWD20110324:constructSQLQuery] Unknown SQL type : "
									+ header.getDatatypes().get(columnName)
									+ " from column : " + columnName);
			}
			if ((driver != null) && driver.equals("com.mysql.jdbc.Driver")
					&& type.isDateType()) {
				SQLQuery += "UNIX_TIMESTAMP(`" + columnName + "`)";
			} else {
				SQLQuery += "`" + columnName + "`";
			}
			if (i < header.getColumnNames().size())
				SQLQuery += ", ";
		}
		SQLQuery += " FROM `" + tableName + "`";
		// WHERE clause
		SQLQuery += " WHERE ";
		int j = 0;
		ArrayList<String> columnNames = fk.getReferenceKey().getColumnNames();
		for (String columnName : columnNames) {
			SQLQuery += "`" + columnName + "` = '"
					+ r.getValues().get(fk.getColumnNames().get(j)) + "'";
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
			log.debug("[DirectMapper:phi] Table : " + t);
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
		if (log.isDebugEnabled())
			log.debug("[DirectMapper:phi] Table : " + t);
		CandidateKey primaryKey = t.getPrimaryKey();
		if (primaryKey != null) {
			// Unique Node IRI
			String stringURI = generateUniqNodeIRI(referencedRow, t, primaryKey, baseURI);
			URI uri = vf.createURI(baseURI, stringURI);
			// URIs.put(r, uri);
			return uri;
		} else {
			// Blank node
			BNode bnode = vf.createBNode(generateUniqBlankNodeName(row));
			return bnode;
		}
	}
	
	/*
	 * Generate IRI name from databse information.
	 */
	private String generateUniqNodeIRI(Row r, StdTable t, CandidateKey primaryKey, String baseURI){
		String stringURI = t.getTableName() + solidus;
		int i = 0;
		for (String columnName : primaryKey.getColumnNames()) {
			i++;
			stringURI += columnName + hyphenMinus
					+ r.getValues().get(columnName);
			if (i < primaryKey.getColumnNames().size())
				stringURI += fullStop;
		}
		// Check URI syntax
		if (!RDFDataValidator.isValidURI(baseURI + stringURI)) {
			if (log.isWarnEnabled())
				log.warn("[DirectMapper:phi] This URI is not valid : "
						+ baseURI + stringURI);
		}
		return stringURI;
	}
	
	/*
	 * Generate blank name (useful for link row and referenced key in case of empty primary key).
	 */
	private  String generateUniqBlankNodeName(Row r) throws UnsupportedEncodingException{
		String blankNodeUniqName = r.getIndex() + "-";
		int i = 1;
		for (String columnName : r.getValues().keySet()) {
			final byte[] bs = r.getValues().get(columnName);
			blankNodeUniqName += URLEncoder.encode(columnName, DirectMappingEngine.encoding)
					+ hyphenMinus
					+ URLEncoder.encode(new String(bs),
							DirectMappingEngine.encoding);
			if (i < r.getValues().size())
				blankNodeUniqName += fullStop;
			i++;
		}
		return blankNodeUniqName;
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
		StdTable referencedTable = referencedRow.getParentBody().getParentTable();
		referencedTable.setTableName(fk.getTargetTableName());

		Resource s = phi(referencedTable, referencedRow, baseURI);
		referencedTable.setTableName(saveTableName);
		// Generate predicates and objects
		for (String columnName : fk.getColumnNames()) {
			// For each column in candidate key, a literal triple is generated
			Statement t = convertLex(row.getParentBody().getParentTable()
					.getHeader(), row, columnName, baseURI);
			result.add(vf.createStatement(s, t.getPredicate(), t.getObject()));
		}
		return result;
	}

	/*
	 * Denotational semantics function : convert foreign key columns
	 * into a triple with mapped (predicate, object).
	 */
	private HashSet<SemiStatement> convertRef(Row row, Row referencedRow,
			ForeignKey fk, String baseURI) throws UnsupportedEncodingException {
		HashSet<SemiStatement> result = new HashSet<SemiStatement>();
		ArrayList<String> columnNames = new ArrayList<String>();
		columnNames.addAll(fk.getColumnNames());
		URI p = convertCol(row, columnNames, baseURI);

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
			log.debug("[DirectMapper:convertRow] Tuple : " + t
					+ ", referencedTuples : " + referencedTuples);
		// Explicit conversion
		Row r = (Row) t;
		HashMap<ForeignKey, Row> referencedRows = new HashMap<ForeignKey, Row>();
		for (Key key : referencedTuples.keySet()) {
			referencedRows.put((ForeignKey) key, (Row) referencedTuples
					.get(key));
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
			HashSet<SemiStatement> refTriples = convertRef(r, referencedRows
					.get(fk), fk, baseURI);
			for (SemiStatement refTriple : refTriples) {
				tmpResult.add(refTriple);
			}
		}
		// Construct ref triples with correct subject
		for (SemiStatement refSemiTriple : tmpResult) {
			Statement triple = vf.createStatement(s, refSemiTriple
					.getPredicate(), refSemiTriple.getObject());
			if (triple != null)
				result.add(triple);
		}
		// Literal Triples
		for (String columnName : currentTable.getLexicals()) {
			Statement triple = convertLex(currentTable.getHeader(), r,
					columnName, baseURI);
			if (triple != null) {
				result.add(vf.createStatement(s, triple.getPredicate(), triple
						.getObject()));
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
	private Statement convertType(Resource s, String baseURI, StdTable currentTable) {
		// Table Triples
		URI typePredicate = vf.createURI(RDFPrefixes.prefix.get("rdf"), "type");
		URI typeObject = vf.createURI(baseURI, currentTable.getTableName());
		Statement typeTriple = vf.createStatement(s, typePredicate, typeObject);
		return typeTriple;
	}

	/*
	 * Denotational semantics function : convert lexical columns into
	 * a triple with mapped (predicate, object).
	 * 
	 * @throws UnsupportedEncodingException
	 */
	private Statement convertLex(StdHeader header, Row r, String columnName,
			String baseURI) throws UnsupportedEncodingException {
		if (log.isDebugEnabled())
			log.debug("[DirectMapper:convertLex] Table "
					+ r.getParentBody().getParentTable().getTableName()
					+ ", column : " + columnName);
		Statement result = null;
		Literal l = null;
		ArrayList<String> columnNames = new ArrayList<String>();
		columnNames.add(columnName);
		URI p = convertCol(r, columnNames, baseURI);
		final byte[] bs = r.getValues().get(columnName);
		String v = new String(bs);
		String d = header.getDatatypes().get(columnName);
		if (v == null || v.equals("null")) {
			// Don't keep triple with null value
			return null;
		}
		XSDType type = null;
		SQLSpecificType sqlType = SQLSpecificType.toSQLType(d);
		if (sqlType.isBlobType()) {
			if (log.isDebugEnabled())
				log
						.debug("[DirectMapper:convertLex] Table "
								+ r.getParentBody().getParentTable()
										.getTableName()
								+ ", column "
								+ columnName
								+ " Forbidden BLOB type (binary stream not supported in XSD)"
								+ " => this triple will be ignored.");
			return null;
		} else {
			type = SpecificSQLToXMLS.getEquivalentSpecificType(d);
			if (type == null)
				throw new IllegalStateException(
						"[DirectMapper:convertLex] Unknown XSD equivalent type of : "
								+ SQLSpecificType.toSQLType(d)
								+ " in column : "
								+ columnName
								+ " in table : "
								+ r.getParentBody().getParentTable()
										.getTableName());
		}
		if (type.toString().equals(XSDType.STRING.toString())) {
			l = vf.createLiteral(v);
		} else {

			URI datatype_iri = convertDatatype(d);
			if (datatype_iri == null) {
				l = vf.createLiteral(v);
			} else {
				l = vf.createLiteral(v, datatype_iri);
			}
		}
		result = vf.createStatement(null, p, (Value) l);
		return result;
	}

	/*
	 * Denotational semantics function : convert row and columnNames
	 * into predicate URI.
	 * 
	 * @throws UnsupportedEncodingException
	 */
	private URI convertCol(Row row, ArrayList<String> columnNames,
			String baseURI) throws UnsupportedEncodingException {
		String label = URLEncoder.encode(row.getParentBody().getParentTable()
				.getTableName(), DirectMappingEngine.encoding)
				+ hash;
		int i = 0;
		for (String columnName : columnNames) {
			i++;
			label += URLEncoder.encode(columnName, DirectMappingEngine.encoding);
			if (i < columnNames.size())
				label += fullStop;
		}
		// Check URI syntax
		if (!RDFDataValidator.isValidURI(baseURI + label)) {
				log.warn("[DirectMapper:convertCol] This URI is not valid : "
						+ baseURI + label);
				log.warn("[DirectMapper:convertCol] This URI has been converted into : " + baseURI + label);
		}
		// Create value factory which build URI
		return vf.createURI(baseURI, label);
	}

	/*
	 * Denotational semantics function : convert datatype from SQL
	 * into xsd datatype.
	 */
	private URI convertDatatype(String datatype) {
		String upDatatype = datatype.toUpperCase();
		if (!SpecificSQLToXMLS.isValidSQLSpecificDatatype(upDatatype))	
	    log.debug("[DirectMapper:convertDatatype] Unknown datatype : "
						+ datatype);
		String xsdDatatype = SpecificSQLToXMLS.getEquivalentSpecificType(upDatatype)
				.toString();
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

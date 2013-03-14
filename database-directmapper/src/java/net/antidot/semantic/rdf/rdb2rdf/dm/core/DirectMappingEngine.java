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
 * Direct Mapping : Direct Mapper Engine
 *
 * The Direct Mapper Engine corresponds to an interface of a norm of W3C.
 * With such as interface, it's possible to swich from a norm to another and deploy
 * a cursor mode.
 * 
 *
 ****************************************************************************/
package net.antidot.semantic.rdf.rdb2rdf.dm.core;

import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.HashSet;

import net.antidot.sql.model.core.DriverType;
import net.antidot.sql.model.db.Key;
import net.antidot.sql.model.db.Tuple;

import org.openrdf.model.Statement;


public interface DirectMappingEngine {

	/**
	 * Version of norm of the W3C organisation.
	 */
	public enum Version {
		/**
		 * A Direct Mapping of Relational Data to RDF, W3C Working Draft 24 March 2011 
		 */
		WD_20110324, 
		/**
		 * A Direct Mapping of Relational Data to RDF, W3C Working Draft 29 May 2012 
		 */
		WD_20120529;
	}
	
	/**
	 * Encoding used for URI creating.
	 */
	public static String encoding = "UTF-8";

	/**
	 * Extract a generic tuple from generic JDBC sets. 
	 * @param values
	 * @param headers
	 * @param primaryKeys
	 * @param foreignKeys
	 * @param tableName 
	 * @param driver
	 * @param timeZone
	 * @param index 
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public Tuple extractTupleFrom(ResultSet values, ResultSet headers,
			ResultSet primaryKeys, ResultSet foreignKeys, String tableName,
			DriverType driver, String timeZone, int index) throws UnsupportedEncodingException;

	/**
	 * Extract a generic referenced tuple from generic JDBC sets.
	 * A referenced tuple corresponds to a tuple which is the target of a foreign key.
	 * @param values
	 * @param headers
	 * @param primaryKeys
	 * @param foreignKeys
	 * @param tableName
	 * @param driver
	 * @param timeZone
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public Tuple extractReferencedTupleFrom(ResultSet values,
			ResultSet headers, ResultSet primaryKeys, ResultSet foreignKeys,
			String tableName, DriverType driver, String timeZone, int index) throws UnsupportedEncodingException;

	/**
	 * Construct the SQL Query depending on current norm used for construct tuples.
	 * @param driver 
	 * @param headersSet
	 * @param tableName
	 * @return
	 */
	public String constructSQLQuery(DriverType driver, ResultSet headersSet,
			String tableName);
	
	/**
	 * Construct the SQL Query depending on current norm used for construct referenced tuples.
	 * @param driver
	 * @param headersSet
	 * @param tableName
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public String constructReferencedSQLQuery(DriverType driver,
			ResultSet headersSet, String tableName, Key key, Tuple tuple) throws UnsupportedEncodingException;

	/**
	 * Extract Sesame triples from generic tuples depends on used norm.
	 * @param tuple
	 * @param referencedTuples
	 * @param primaryIsForeignKey
	 * @param baseURI
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public HashSet<Statement> extractTriplesFrom(Tuple tuple,
			HashMap<Key, Tuple> referencedTuples, Key primaryIsForeignKey,
			String baseURI) throws UnsupportedEncodingException;

	/**
	 * Methods which convert generic tuples to corresponding model in used norm.
	 * @param tuple
	 * @return
	 */
	public String getReferencedTableName(Tuple tuple);
	public String getReferencedTableName(Key key);
	public HashSet<Key> getReferencedKeys(Tuple tuple);
	public boolean isPrimaryKey(Key key, Tuple tuple);

}

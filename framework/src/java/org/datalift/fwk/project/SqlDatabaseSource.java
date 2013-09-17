package org.datalift.fwk.project;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;

import org.datalift.fwk.util.CloseableIterator;
/**
 * A source object reading tables from a SQL database.
 *
 * @author csuglia
 */
public interface SqlDatabaseSource extends SqlSource {
	/**
     * @return the Database name basing on the jdbc url
     */
	public String getDatabaseName();	
	
	/**
	 * @return the number of tables included in the database
	 */
	public int getTableCount();
    /**
     * @return the Database Path
     */
    public String getDatabasePath();
    /**
     * @return the list of the names of the database's tables 
     * @throws SQLException if any error occurred accessing the
     *         database.
     */
    public List<String> getTableNames() throws SQLException;
    /**
     * Get the iterator of a table that belongs to the database, in order to navigate it
     * @param tableName
     * @return the Iterator of the table which name was passed as parameter, based on the iterator that
     * a {@link SqlQuerySource} would return
     */
    public CloseableIterator<Row<Object>> getTableIterator(String tableName);
    
    
    
    /**
     * Get the column names of a table that belongs to the database
     * @param tableName 
     * @throws SQLException if any error occurred accessing the
     *         database.
     * @return the column names of the table passed as parameter
     */
    public HashMap<String,List<String>> getTablesColumnNames() throws SQLException;
}

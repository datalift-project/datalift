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
     * Returns the Database name based on the JDBC URL.
     * @return the Database name based on the JDBC URL.
     */
    public String getDatabaseName();	

    /**
     * Returns the number of tables included in the database
     * @return the number of tables included in the database
     */
    public int getTableCount();

    /**
     * Returns the database path
     * @return the database path
     */
    public String getDatabasePath();

    /**
     * Returns the list of the names of the database's tables
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
     * @return the column names of the table passed as parameter
     * @throws SQLException if any error occurred accessing the
     *         database.
     */
    public HashMap<String,List<String>> getTablesColumnNames() throws SQLException;
}

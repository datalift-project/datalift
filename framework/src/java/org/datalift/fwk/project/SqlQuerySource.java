package org.datalift.fwk.project;

import java.sql.SQLException;
import java.util.List;

import org.datalift.fwk.util.CloseableIterable;

/**
 * A source object reading data from a SQL database by executing a
 * user-provided JDBC query.
 *
 * @author hdevos
 */
public interface SqlQuerySource extends SqlSource, CloseableIterable<Row<Object>>
{
    public String getQuery();

    public void setQuery(String query);

    /**
     * Returns the number of data columns returned by the SQL query.
     * @return the number of data columns.
     * @throws SQLException if any error occurred accessing the
     *         database.
     */
    public int getColumnCount() throws SQLException;

    /**
     * Returns the names of data columns returned by the SQL query.
     * @return the names of data columns.
     * @throws SQLException if any error occurred accessing the
     *         database.
     */
    public List<String> getColumnNames() throws SQLException;
}

/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.core.project;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.persistence.Entity;
import javax.sql.rowset.WebRowSet;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.sun.rowset.WebRowSetImpl;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.SqlQuerySource;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.io.FileUtils;

import static org.datalift.fwk.util.StringUtils.isSet;
import static org.datalift.fwk.util.web.Charsets.UTF_8;


/**
 * Default implementation of the {@link SqlQuerySource} interface.
 *
 * @author hdevos
 */
@SuppressWarnings("restriction")
@Entity
@RdfsClass("datalift:sqlSource")
public class SqlQuerySourceImpl extends CachingSourceImpl implements SqlQuerySource
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The scheme for JDBC URLs. */
    public final static String JDBC_URL_SCHEME  = "jdbc:";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:user")
    private String user;
    @RdfProperty("datalift:password")
    private String password;
    @RdfProperty("datalift:request")
    private String query;

    private transient WebRowSet rowSet = null;
    private transient List<String> columns = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new SQL source.
     */
    public SqlQuerySourceImpl() {
        super(SourceType.SqlQuerySource);
    }

    /**
     * Creates a new SQL source with the specified identifier and
     * owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    public SqlQuerySourceImpl(String uri, Project project) {
        super(SourceType.SqlQuerySource, uri, project);
    }

    //-------------------------------------------------------------------------
    // SqlSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getConnectionUrl() {
        return this.getSourceUrl();
    }

    /** {@inheritDoc} */
    @Override
    public void setConnectionUrl(String connectionUrl) {
        // Check URL.
        getDatabaseType(connectionUrl);
        // Store URL.
        this.setSourceUrl(connectionUrl);
        // Invalidate cache to force data reload.
        this.invalidateCache();
    }

    /** {@inheritDoc} */
    @Override
    public String getUser() {
        return this.user;
    }

    /** {@inheritDoc} */
    @Override
    public void setUser(String user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public String getPassword() {
        return this.password;
    }

    /** {@inheritDoc} */
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /** {@inheritDoc} */
    @Override
    public String getQuery() {
        return this.query;
    }

    /** {@inheritDoc} */
    @Override
    public void setQuery(String query) {
        this.query = query;
        // Invalidate cache to force data reload.
        this.invalidateCache();
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnCount() {
        this.init();
        return this.columns.size();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getColumnNames() throws SQLException {
        this.init();
        return this.columns;
    }

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Row<Object>> iterator() {
        CloseableIterator<Row<Object>> i = null;

        this.init();
        if (this.columns != null) {
            try {
                // Get a cursor on the shared copy of locally cached data.
                return new RowIterator(this.rowSet.createShared());
            }
            catch (Exception e) {
                throw new TechnicalException(null, e);
            }
        }
        // Else: No data available.

        return i;
    }

    /** {@inheritDoc} */
    @Override
    public String getDatabaseType() {
        return getDatabaseType(this.getConnectionUrl());
    }

    //-------------------------------------------------------------------------
    // CachingSourceImpl contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected synchronized void reloadCache() throws IOException {
        // Release any data held in memory.
        if (this.rowSet != null) {
            this.closeQuietly(this.rowSet);
            this.rowSet = null;
        }
        // Force re-computation of column names on next access.
        this.columns = null;

        WebRowSet webRowSet = null;
        String databaseType = null;
        try {
            // Force loading of database driver.
            String cnxUrl = this.getConnectionUrl();
            databaseType = this.getDatabaseType();
            Class.forName(DatabaseType.valueOf(databaseType).getDriver());
            log.debug("Database driver loaded for {}", databaseType);
            // Execute SQL query to retrieve data.
            webRowSet = new WebRowSetImpl();
            webRowSet.setUrl(cnxUrl);
            webRowSet.setCommand(this.getQuery());
            webRowSet.setUsername(this.getUser());
            webRowSet.setPassword(this.getPassword());
            webRowSet.execute();
            log.debug("Successfully executed query: {}", this.getQuery());
            // Save query results into local cache file.
            File localCache = this.getCacheFile();
            webRowSet.writeXml(new OutputStreamWriter(
                                    new FileOutputStream(localCache), UTF_8));
            // Do not close WebRowSet to use it as a shared memory cache.
            // Do not close the output stream: WebRowSet takes care of it.
            log.debug("Query results saved to {}", localCache);
            // Keep cached data in memory.
            webRowSet.setReadOnly(true);
            this.rowSet = webRowSet;
        }
        catch (ClassNotFoundException e) {
            throw new IOException(
                    new TechnicalException("jdbc.driver.not.found",
                                           databaseType));
        }
        catch (SQLException e) {
            this.closeQuietly(webRowSet);
            throw new IOException(e);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected String getCacheFileExtension() {
        return "xml";           // Sun's WebRowSet stores ResultSets as XML.
    }

    /** {@inheritDoc} */
    @Override
    protected StringBuilder toString(StringBuilder b) {
        b.append(this.getConnectionUrl());
        b.append(", \"").append(this.getQuery()).append('"');
        return super.toString(b);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    protected final void closeQuietly(ResultSet rs) {
        if (rs != null) {
            try {
                rs.close();
            }
            catch (Exception e) { /* Ignore... */ }
        }
    }

    /**
     * Ensures resources are released even when object gets
     * garbage-collected.
     */
    @Override
    protected void finalize() {
        if (this.rowSet != null) {
            this.closeQuietly(this.rowSet);
            this.rowSet = null;
        }
    }

    private void init() {
        if (this.columns == null) {
            Reader in = null;
            try {
                if (this.rowSet == null) {
                    // Read data from local cache file.
                    in = new InputStreamReader(this.getInputStream(), UTF_8);
                    WebRowSet webRowSet = new WebRowSetImpl();
                    webRowSet.readXml(in);
                    // Keep cached data in memory.
                    webRowSet.setReadOnly(true);
                    this.rowSet = webRowSet;
                    log.debug("Loaded RowSet from local cache file: {}",
                                                        this.getCacheFile());
                }
                // Extract column names from cached data.
                ResultSetMetaData metadata = this.rowSet.getMetaData();
                String[] cols = new String[metadata.getColumnCount()];
                for (int i=0, max=cols.length; i<max; i++) {
                    cols[i] = metadata.getColumnName(i+1);
                }
                this.columns = Collections.unmodifiableList(
                                                        Arrays.asList(cols));
                log.debug("Extracted query result columns: {}", this.columns);
            }
            catch (Exception e) {
                throw new TechnicalException(null, e);
            }
            finally {
                if (this.rowSet == null) {
                    // Only close the input stream if it is not being taken
                    // care of by the WebRowSet.
                    FileUtils.closeQuietly(in);
                }
            }
        }
        // Else: Already initialized.
    }

    private static String getDatabaseType(String connectionUrl) {
        if ((isSet(connectionUrl)) &&
            (connectionUrl.startsWith(JDBC_URL_SCHEME))) {
            String[] urlElts = connectionUrl.split(":");
            if ((urlElts.length > 1) && (urlElts[1] != null)) {
                return urlElts[1];
            }
        }
        throw new TechnicalException("invalid.jdbc.url", connectionUrl);
    }

    //-------------------------------------------------------------------------
    // RowIterator nested class
    //-------------------------------------------------------------------------

    /**
     * An {@link Iterator} over the data rows read from a CSV file.
     */
    private final class RowIterator implements CloseableIterator<Row<Object>>
    {
        private final ResultSet rs;
        private boolean closed = false;

        public RowIterator(ResultSet rs) {
            this.rs = rs;
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            boolean hasNext = false;
            try {
                hasNext = ! ((this.closed) || (this.rs.isLast()));
            }
            catch (SQLException e) {
                this.rethrow(e);
            }
            return hasNext;
        }

        /** {@inheritDoc} */
        @Override
        public Row<Object> next() {
            if (! this.hasNext()) {
                throw new NoSuchElementException();
            }
            Row<Object> row = null;
            try {
                this.rs.next();
                row = new ResultSetRow(this.rs, columns);
            }
            catch (SQLException e) {
                this.rethrow(e);
            }
            return row;
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            if (! this.closed) {
                this.closed = true;
                closeQuietly(this.rs);
            }
            // Else: Already closed.
        }

        /**
         * Ensures resources are released even when
         * {@link #close()} has not been invoked by user class.
         */
        @Override
        protected void finalize() {
            this.close();
        }

        /**
         * Translates a {@link SQLException} into a
         * {@link TechnicalException.
         * @param  e   an <code>SQLException</code>.
         *
         * @throws TechnicalException always.
         */
        private void rethrow(SQLException e) {
            // Release resources.
            this.close();
            // Rethrow exception.
            throw new TechnicalException(null, e);
        }
    }

    //-------------------------------------------------------------------------
    // ResultSetRow nested class
    //-------------------------------------------------------------------------

    /**
     * A Row implementation wrapping a row in a JDBC result set.
     * <p>
     * <i>Implementation notes</i>: this class is marked as public on
     * purpose. Otherwise the Velocity template engine fails to access
     * methods of this class.</p>
     */
    public final static class ResultSetRow implements Row<Object>
    {
        private final ResultSet rs;
        private final List<String> columns;

        /**
         * Creates a new result set row object for the current row
         * of the result set.
         * @param  rs        the result set the current row of which
         *                   shall be wrapped.
         * @param  columns   the columns available from the result set.
         */
        public ResultSetRow(ResultSet rs, List<String> columns) {
            this.rs = rs;
            this.columns = columns;
        }

        /** {@inheritDoc} */
        @Override
        public int size() {
            return this.columns.size();
        }

        /** {@inheritDoc} */
        @Override
        public List<String> keys() {
            return this.columns;
        }

        /** {@inheritDoc} */
        @Override
        public Object get(String key) {
            try {
                return this.rs.getObject(key);
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getString(String key) {
            try {
                return this.rs.getString(key);
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public Object get(int index) {
            try {
                return this.rs.getObject(index + 1); // SQL index is 1-based.
            }
            catch (SQLException e) {
                throw new TechnicalException(null, e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getString(int index) {
            try {
                return this.rs.getString(index + 1); // SQL index is 1-based.
            }
            catch (SQLException e) {
                throw new TechnicalException(null, e);
            }
        }

        /** {@inheritDoc} */
        @Override
        public String getKey(int index) {
            return this.columns.get(index);
        }

        /** {@inheritDoc} */
        @Override
        public Iterator<Object> iterator() {
            return new Iterator<Object>() {
                private int curPos = 0;

                @Override
                public boolean hasNext() {
                    return (this.curPos < size());
                }

                @Override
                public Object next() {
                    if (! this.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return get(this.curPos++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}

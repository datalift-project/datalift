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


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

import javax.persistence.Entity;
import javax.sql.rowset.WebRowSet;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.sun.rowset.WebRowSetImpl;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.SqlSource;
import org.datalift.fwk.util.CloseableIterator;


/**
 * Default implementation of the {@link SqlSource} interface.
 *
 * @author hdevos
 */
@Entity
@RdfsClass("datalift:dbSource")
public class SqlSourceImpl extends CachingSourceImpl implements SqlSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:user")
    private String user;
    @RdfProperty("datalift:password")
    private String password;
    @RdfProperty("datalift:database")
    private String database;
    @RdfProperty("datalift:request")
    private String query;

    private transient Collection<String> columns = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new SQL source.
     */
    public SqlSourceImpl() {
        super(SourceType.SqlSource);
    }

    /**
     * Creates a new SQL source with the specified identifier.
     * @param  uri    the source unique identifier (URI) or
     *                <code>null</code> if not known at this stage.
     */
    public SqlSourceImpl(String uri) {
        super(SourceType.SqlSource, uri);
    }

    //-------------------------------------------------------------------------
    // SqlSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getConnectionUrl() {
        return this.getSource();
    }

    /** {@inheritDoc} */
    @Override
    public void setConnectionUrl(String connectionUrl) {
        // Check URL.
        this.getDatabaseType(connectionUrl);
        this.setSource(connectionUrl);
    }

    /** {@inheritDoc} */
    @Override
    public String getUser() {
        return user;
    }

    /** {@inheritDoc} */
    @Override
    public void setUser(String user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public String getPassword() {
        return password;
    }

    /** {@inheritDoc} */
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /** {@inheritDoc} */
    @Override
    public String getDatabase() {
        return database;
    }

    /** {@inheritDoc} */
    @Override
    public void setDatabase(String database) {
        this.database = database;
    }

    /** {@inheritDoc} */
    @Override
    public String getQuery() {
        return query;
    }

    /** {@inheritDoc} */
    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnCount() {
        this.init();
        return this.columns.size();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getColumnNames() throws SQLException {
        this.init();
        return this.columns;
    }

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Row<Object>> iterator() {
        this.init();
        try {
            WebRowSet rowSet = new WebRowSetImpl();
            rowSet.readXml(this.getInputStream());
            return new RowIterator(rowSet);
        }
        catch (Exception e) {
            throw new TechnicalException(null, e);
        }
    }

    //-------------------------------------------------------------------------
    // CachingSourceImpl contract support
    //-------------------------------------------------------------------------

    @Override
    protected synchronized void reloadCache() throws IOException {
        WebRowSet rowSet = null;
        String databaseType = null;
        try {
            rowSet = new WebRowSetImpl();
            // Force loading of database driver.
            String cnxUrl = this.getConnectionUrl();
            databaseType = this.getDatabaseType(cnxUrl);
            Class.forName(DatabaseType.valueOf(databaseType).getDriver());
            // Get table to grid
            rowSet.setUrl(cnxUrl);
            rowSet.setCommand(this.getQuery());
            rowSet.setUsername(this.getUser());
            rowSet.setPassword(this.getPassword());
            rowSet.execute();
            // Force recomputation of column names on next access.
            this.columns = null;

            OutputStream out = new FileOutputStream(this.getCacheFile());
            try {
                rowSet.writeXml(out);
                rowSet.close();
            }
            finally {
                out.close();
            }
        }
        catch (ClassNotFoundException e) {
            throw new IOException(
                    new TechnicalException("jdbc.driver.not.found",
                                           databaseType));
        }
        catch (SQLException e) {
            throw new IOException(e);
        }
        finally {
            if (rowSet != null) {
                try { rowSet.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private void init() {
        if (this.columns == null) {
            WebRowSet rowSet = null;
            InputStream in = null;
            try {
                in = this.getInputStream();
                rowSet = new WebRowSetImpl();
                rowSet.readXml(in);

                ResultSetMetaData metadata = rowSet.getMetaData();
                String[] cols = new String[metadata.getColumnCount()];
                for (int i=0, max=cols.length; i<max; i++) {
                    cols[i] = metadata.getColumnName(i+1);
                }
                this.columns = Collections.unmodifiableCollection(
                                                        Arrays.asList(cols));
            }
            catch (Exception e) {
                throw new TechnicalException(null, e);
            }
            finally {
                if (rowSet != null) {
                    try {
                        rowSet.close();
                    } catch (Exception e) { /* Ignore... */ }
                }
                if (in != null) {
                    try { in.close(); } catch (Exception e) { /* Ignore... */ }
                }
            }
        }
        // Else: Already initialized.
    }

    private String getDatabaseType(String connectionUrl) {
        if (connectionUrl.startsWith("jdbc:")) {
            String[] arrayUrl = connectionUrl.split(":");
            if (arrayUrl[1] != null) {
                return arrayUrl[1];
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
        private final WebRowSet rowSet;
        private boolean hasNext = false;
        private boolean closed = false;

        public RowIterator(WebRowSet rowSet) {
            this.rowSet  = rowSet;
            this.hasNext = this.getNextRow();
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return this.hasNext;
        }

        /** {@inheritDoc} */
        @Override
        public Row<Object> next() {
            Row<Object> row = null;
            if (this.hasNext) {
                row = new ResultSetRow(this.rowSet, columns);
                this.hasNext = this.getNextRow();
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
                try {
                    this.rowSet.close();
                }
                catch (SQLException e) { /* Ignore... */ }
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

        private boolean getNextRow() {
            boolean hasNext = false;
            try {
                hasNext = this.rowSet.next();
            }
            catch (SQLException e) {
                throw new TechnicalException(null, e);
            }
            finally {
                if (! hasNext) {
                    this.close();
                }
            }
            return hasNext;
        }
    }

    //-------------------------------------------------------------------------
    // ResultSetRow nested class
    //-------------------------------------------------------------------------

    /**
     * A Row implementation wrapping an array of strings.
     * <p>
     * <i>Implementation notes</i>: this class is marked as public on
     * purpose. Otherwise the Velocity template engine fails to access
     * methods of this class.</p>
     */
    public final static class ResultSetRow implements Row<Object>
    {
        private final ResultSet rs;
        private final Collection<String> columns;

        public ResultSetRow(ResultSet rs, Collection<String> columns) {
            this.rs = rs;
            this.columns = columns;
        }

        @Override
        public int size() {
            return this.columns.size();
        }

        @Override
        public Collection<String> keys() {
            return this.columns;
        }

        @Override
        public Object get(String key) {
            try {
                return this.rs.getObject(key);
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public String getString(String key) {
            try {
                return this.rs.getString(key);
            }
            catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Object get(int index) {
            try {
                return this.rs.getObject(index);
            }
            catch (SQLException e) {
                throw new TechnicalException(null, e);
            }
        }

        @Override
        public String getString(int index) {
            try {
                return this.rs.getString(index);
            }
            catch (SQLException e) {
                throw new TechnicalException(null, e);
            }
        }

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

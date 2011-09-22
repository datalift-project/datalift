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
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;
import javax.sql.rowset.WebRowSet;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.sun.rowset.WebRowSetImpl;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.SqlSource;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.StringUtils;


/**
 * Default implementation of the {@link SqlSource} interface.
 *
 * @author hdevos
 */
@Entity
@RdfsClass("datalift:dbSource")
public class SqlSourceImpl extends BaseSource implements SqlSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:connectionUrl")
    private String connectionUrl;
    @RdfProperty("datalift:user")
    private String user;
    @RdfProperty("datalift:password")
    private String password;
    @RdfProperty("datalift:database")
    private String database;
    @RdfProperty("datalift:request")
    private String request;
    @RdfProperty("datalift:cacheDuration")
    private int cacheDuration;

    private transient File cacheFile = null;
    private transient String[] columns = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new SQL source.
     */
    public SqlSourceImpl() {
        super(SourceType.DbSource);
    }

    /**
     * Creates a new SQL source with the specified identifier.
     * @param  uri    the source unique identifier (URI) or
     *                <code>null</code> if not known at this stage.
     */
    public SqlSourceImpl(String uri) {
        super(SourceType.DbSource, uri);
    }

    //-------------------------------------------------------------------------
    // DbSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration, URI baseUri)
                                                            throws IOException {
        super.init(configuration, baseUri);

        WebRowSet rowSet = null;
        try {
            String fileName = this.getClass().getSimpleName() + '-' +
                                            StringUtils.urlify(this.getTitle());
            this.cacheFile = new File(configuration.getPrivateStorage(),
                                      fileName);
            rowSet = new WebRowSetImpl();
            InputStream in = this.getCacheStream(this.cacheFile);
            if (in == null) {
                // Force loading of database driver.
                Class.forName(DatabaseType.valueOf(this.getDatabaseType())
                                          .getDriver());
                // Get table to grid
                rowSet.setCommand(request);
                rowSet.setUrl(connectionUrl);
                rowSet.setUsername(user);
                rowSet.setPassword(password);
                rowSet.execute();
                rowSet.writeXml(new FileOutputStream(cacheFile));
                cacheFile.deleteOnExit();
            }
            else {
                rowSet.readXml(in);
            }
            ResultSetMetaData metadata = rowSet.getMetaData();
            String[] cols = new String[metadata.getColumnCount()];
            for (int i=0, max=cols.length; i<max; i++) {
                cols[i] = metadata.getColumnName(i+1);
            }
            this.columns = cols;
        }
        catch (Exception e) {
            throw new IOException(e);
        }
        finally {
            if (rowSet != null) {
                try { rowSet.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
    }

    /** {@inheritDoc} */
    @Override
    public String getConnectionUrl() {
        return connectionUrl;
    }

    /** {@inheritDoc} */
    @Override
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
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
    public int getCacheDuration() {
        return cacheDuration;
    }

    /** {@inheritDoc} */
    @Override
    public void setCacheDuration(int cacheDuration) {
        this.cacheDuration = cacheDuration;
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
    public String getRequest() {
        return request;
    }

    /** {@inheritDoc} */
    @Override
    public void setRequest(String request) {
        this.request = request;
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnCount() {
        if (this.columns == null) {
            throw new IllegalStateException("Not initialized");
        }
        return this.columns.length;
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getColumnNames() throws SQLException {
        if (this.columns == null) {
            throw new IllegalStateException("Not initialized");
        }
        return Arrays.asList(this.columns);
    }

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Row<Object>> iterator() {
        if (this.columns == null) {
            throw new IllegalStateException("Not initialized");
        }
        try {
            WebRowSet rowSet = new WebRowSetImpl();
            rowSet.readXml(this.getCacheStream(this.cacheFile));
            return new RowIterator(rowSet);
        }
        catch (Exception e) {
            throw new TechnicalException(null, e);
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private String getDatabaseType() {
        if (connectionUrl.startsWith("jdbc:")) {
            String[] arrayUrl = connectionUrl.split(":");
            if (arrayUrl[1] != null) {
                return arrayUrl[1];
            }
            return null;
        }
        else {
            throw new RuntimeException();
        }
    }

    /**
     * Get the data of the database form the cache file, if it has
     * been populated.
     * @param  cacheFile   the cache file path.
     *
     * @return an input stream on the cache file, if it exists.
     * @throws IOException if any error occurred accessing the cache
     *         file.
     */
    private InputStream getCacheStream(File cacheFile) throws IOException {
        if (this.cacheDuration > 0) {
            long now = System.currentTimeMillis();
            if ((cacheFile.exists()) &&
                (cacheFile.lastModified() < (now + this.cacheDuration))) {
                return new FileInputStream(cacheFile);
            }
        }
        return null;
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
                row = new Row<Object>()
                    {
                        @Override
                        public int size() {
                            return columns.length;
                        }

                        @Override
                        public Collection<String> keys() {
                            return Arrays.asList(columns);
                        }
    
                        @Override
                        public Object get(String key) {
                            try {
                                return rowSet.getObject(key);
                            }
                            catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
    
                        @Override
                        public String getString(String key) {
                            try {
                                return rowSet.getString(key);
                            }
                            catch (SQLException e) {
                                throw new RuntimeException(e);
                            }
                        }
    
                        @Override
                        public Object get(int index) {
                            try {
                                return rowSet.getObject(index);
                            }
                            catch (SQLException e) {
                                throw new TechnicalException(null, e);
                            }
                        }
    
                        @Override
                        public String getString(int index) {
                            try {
                                return rowSet.getString(index);
                            }
                            catch (SQLException e) {
                                throw new TechnicalException(null, e);
                            }
                        }
                    };
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
}

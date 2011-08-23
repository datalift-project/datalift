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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.sql.rowset.WebRowSet;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.sun.rowset.WebRowSetImpl;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.DbSource;
import org.datalift.fwk.util.StringUtils;


@Entity
@RdfsClass("datalift:dbSource")
public class DbSourceImpl extends BaseSource implements DbSource
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

    private transient WebRowSet wrset;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public DbSourceImpl() {
        super(SourceType.DbSource);
    }

    public DbSourceImpl(String uri) {
        super(SourceType.DbSource, uri);
    }

    //-------------------------------------------------------------------------
    // BaseSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration, URI baseUri)
                                                            throws IOException {
        super.init(configuration, baseUri);

        try {
            String fileName = this.getClass().getSimpleName() + '-' +
                                            StringUtils.urlify(this.getTitle());
            File cacheFile = new File(configuration.getPrivateStorage(),
                                      fileName);
            wrset = new WebRowSetImpl();
            InputStream in = this.getCacheStream(cacheFile);
            if (in == null) {
                // Force loading of database driver.
                Class.forName(DatabaseType.valueOf(this.getDatabaseType())
                                          .getDriver());
                // Get table to grid
                wrset.setCommand(request);
                wrset.setUrl(connectionUrl);
                wrset.setUsername(user);
                wrset.setPassword(password);
                wrset.execute();
                wrset.writeXml(new FileOutputStream(cacheFile));
                cacheFile.deleteOnExit();
            }
            else {
                wrset.readXml(in);
            }
        }
        catch (Exception e) {
            throw new IOException(e);
        }
    }

    //-------------------------------------------------------------------------
    // DbSource contract support
    //-------------------------------------------------------------------------

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
        try {
            return wrset.getMetaData().getColumnCount();
        }
        catch (SQLException e) {
            throw new TechnicalException(null, e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getColumnNames() {
        List<String> names = new LinkedList<String>();
        try {
            for (int i=0, max=this.getColumnCount(); i<max; i++) {
                names.add(wrset.getMetaData().getColumnName(i+1));
            }
        }
        catch (SQLException e) {
            throw new TechnicalException(null, e);
        }
        return Collections.unmodifiableList(names);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<Object> iterator() {
        try {
            return Collections.unmodifiableCollection(wrset.toCollection())
                              .iterator();
        } catch (SQLException e) {
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
     * Get the data of the database in cache if it had to be used.
     * @param cacheFile
     * @return File in cache if it's valid
     * @throws FileNotFoundException
     */
    private InputStream getCacheStream(File cacheFile)
                                                throws FileNotFoundException {
        if (this.cacheDuration > 0) {
            long now = System.currentTimeMillis();
            if ((cacheFile.exists()) &&
                (cacheFile.lastModified() < (now + this.cacheDuration))) {
                return new FileInputStream(cacheFile);
            }
        }
        return null;
    }
}

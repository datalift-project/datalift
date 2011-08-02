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
import java.util.Date;
import java.util.Iterator;

import javax.persistence.Entity;
import javax.sql.rowset.WebRowSet;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.sun.rowset.WebRowSetImpl;

import org.datalift.fwk.project.DbSource;


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
    private int	cacheDuration;

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

    /** {@inheritDoc} */
	@Override
	public void init(File cacheFile) {
		try {
			wrset = new WebRowSetImpl();
			InputStream in;
			if ((in = this.getCacheStream(cacheFile)) == null) {
				// Get table to grid
				Class.forName(DatabaseType.valueOf(this.getDatabaseType()).getValue()).newInstance();
				
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
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		
	}
	
	public void init(File docRoot, URI baseUri) throws IOException {
		// NOP		
	}
	
    //-------------------------------------------------------------------------
    // DbSource contract support
    //-------------------------------------------------------------------------

	@Override
	public String getConnectionUrl() {
		return connectionUrl;
	}

	@Override
	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}

	@Override
	public String getUser() {
		return user;
	}
	
	@Override
	public void setUser(String user) {
		this.user = user;
	}

	@Override
	public String getPassword() {
		return password;
	}
	
	@Override
	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public int getCacheDuration() {
		return cacheDuration;
	}
	
	@Override
	public void setCacheDuration(int cacheDuration) {
		this.cacheDuration = cacheDuration;
	}
	
	/**
	 * Get the data of the database in cache if it had to be used.  
	 * @param cacheFile
	 * @return File in cache if it's valid 
	 * @throws FileNotFoundException
	 */
	private InputStream getCacheStream(File cacheFile) throws FileNotFoundException {
		if (this.cacheDuration > 0) {
			Date cur = new Date();
			if (cacheFile.exists() && cacheFile.lastModified() < cur.getTime() + cacheDuration) {
				return new FileInputStream(cacheFile);
			}
		} 
		return null;
	}

	@Override
	public String getDatabase() {
		return database;
	}
	
	@Override
	public void setDatabase(String database) {
		this.database = database;
	}
	
	public String getDatabaseType() {
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

	@Override
	public String getRequest() {
		return request;
	}
	
	@Override
	public void setRequest(String request) {
		this.request = request;
	}

	@Override
	public int getColumnCount(){
		try {
			return wrset.getMetaData().getColumnCount();
		} catch (SQLException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	
	@Override
	public String[] getColumnsName() {
	   String[] noms = new String[this.getColumnCount()];
	   try {
		   for(int i = 0; i < noms.length; i++){
		      String nomColonne = wrset.getMetaData().getColumnName(i+1);
		      noms[i] = nomColonne;
		   }
	   } catch (SQLException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
	   }
	   return noms;
	}
	
	@Override
	public Iterator<Object> iterator() {
		try {
			return Collections.unmodifiableCollection(wrset.toCollection()).iterator();
		} catch (SQLException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
}

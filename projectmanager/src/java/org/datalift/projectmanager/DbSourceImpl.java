package org.datalift.projectmanager;


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

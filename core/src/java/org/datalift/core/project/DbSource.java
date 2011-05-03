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

@Entity
@RdfsClass("datalift:dbSource")
public class DbSource extends BaseSource implements Iterable<Object>
{
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

	public DbSource() {
		// NOP
	}
        
	public DbSource(String uri) {
		super(uri);
	}
       
	public String getConnectionUrl() {
		return connectionUrl;
	}

	public void setConnectionUrl(String connectionUrl) {
		this.connectionUrl = connectionUrl;
	}
	
	@Override
	public Iterator<Object> iterator() {
		try {
			return Collections.unmodifiableCollection(wrset.toCollection()).iterator();
		} catch (SQLException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getUser() {
		return user;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public String getPassword() {
		return password;
	}
	
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
	
	public InputStream		getCacheStream(File cacheFile) throws FileNotFoundException {
		if (this.cacheDuration > 0) {
			Date cur = new Date();
			if (cacheFile.exists() && cacheFile.lastModified() < cur.getTime() + cacheDuration) {
				return new FileInputStream(cacheFile);
			}
		} 
		return null;
	}
	
	public int getCacheDuration() {
		return cacheDuration;
	}

	public void setCacheDuration(int cacheDuration) {
		this.cacheDuration = cacheDuration;
	}
	
	public String	getDatabaseType() {
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
	
	public void setDatabase(String database) {
		this.database = database;
	}

	public String getDatabase() {
		return database;
	}

	public void setRequest(String request) {
		this.request = request;
	}

	public String getRequest() {
		return request;
	}

	public int getColumnCount(){
		try {
			return wrset.getMetaData().getColumnCount();
		} catch (SQLException e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
	}
	

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

	public enum DatabaseType {
		mysql("com.mysql.jdbc.Driver");
		
		protected String value;
		
		DatabaseType(String val) {
			this.value = val;
		}
		
		public String getValue() {
			return value;
		}
	}

	@Override
	public void init(File docRoot, URI baseUri) throws IOException {
		// NOP		
	}
}

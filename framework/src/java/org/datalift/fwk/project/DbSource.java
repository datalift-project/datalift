package org.datalift.fwk.project;

import java.io.File;

public interface DbSource extends Source, Iterable<Object>  {
	
	/**
	 * Init the connection to the database and execute the request
	 * @param cacheFile
	 */
	public void init(File cacheFile);
	
	public String getConnectionUrl();
	public void setConnectionUrl(String connectionUrl);
	
	public String getUser();
	public void setUser(String user);
	
	public String getPassword();
	public void setPassword(String password);
	
	public int getCacheDuration();
	public void setCacheDuration(int cacheDuration);

	public String getDatabase();
	public void setDatabase(String database);
	
	public String getRequest();
	public void setRequest(String request);

	public int getColumnCount();
	public String[] getColumnsName();
	
	/**
	 * List of the different databases supported and their java class
	 *
	 */
	public enum DatabaseType {
		mysql("com.mysql.jdbc.Driver"),
		postgresql("org.postgresql.Driver"),
		oracle("oracle.jdbc.driver.OracleDriver");
		
		protected String value;
		
		DatabaseType(String val) {
			this.value = val;
		}
		
		public String getValue() {
			return value;
		}
	}

}

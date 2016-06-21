package org.datalift.fwk.project;

public interface ServiceSource extends Source{
	
	
	/**
	 * 
	 * @return
	 */
	public String getVersion();
	
	/**
	 * 
	 * @return
	 */
	public String getPublisher();
	
	/**
	 * 
	 * @param
	 */
	public void setVersion(String version);
	
	/**
	 * 
	 * @param
	 */
	public void setserverTypeStrategy(String s);
	
	public String getserverTypeStrategy();
				  
	/**
	 * 
	 * @param
	 */
	public void setPublisher(String publisher);
}

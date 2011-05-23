package org.datalift.fwk.project;

public interface Source
{
	
    public String getUri();
    public void setTitle(String title);
    public String getTitle();
    
    public TypeSource getTypeSource();
    
    public enum TypeSource {
    	RdfSource, 
    	CsvSource,
    	DbSource;
        
    }
}

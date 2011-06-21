package org.datalift.fwk.project;


public interface Source
{
    public String getUri();
    public void setTitle(String title);
    public String getTitle();
    
    public SourceType getType();
    
    public enum SourceType {
    	RdfSource, 
    	CsvSource,
    	DbSource,
    	TransformedRdfSource;
    }
}

package org.datalift.fwk.project;


import java.util.List;


public interface CsvSource extends FileSource<String[]>
{
	public enum Separator {
	    comma(','), semicolon(';'), tab('\t');

	    protected final char value;

	    Separator(char s) {
	        this.value = s;
	    }

	    public char getValue() {
	        return value;
	    }
	}
	
    public boolean hasTitleRow();
    public String getSeparator();
    public List<String> getColumnsHeader();
}

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
    public void	setTitleRow(boolean titleRow);
    public String getSeparator();
    public void setSeparator(String sep);
    public List<String> getColumnsHeader();
}

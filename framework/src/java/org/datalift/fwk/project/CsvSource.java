package org.datalift.fwk.project;


import java.util.List;


public interface CsvSource extends FileSource<String[]>
{
    public boolean hasTitleRow();
    public String getSeparator();
    public List<String> getColumnsHeader();
}

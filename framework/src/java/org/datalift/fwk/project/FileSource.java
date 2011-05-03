package org.datalift.fwk.project;


import java.io.IOException;
import java.io.InputStream;


public interface FileSource<T> extends Source, Iterable<T>
{
    public String getMimeType();
    public String getFilePath();
    public InputStream getInputStream() throws IOException;
}

package org.datalift.fwk.project;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;


public interface FileSource<T> extends Source, Iterable<T>
{
    public String getMimeType();
    public void setMimeType(String mimeType);
    public String getFilePath();
    public InputStream getInputStream() throws IOException;
    public void init(File docRoot, URI baseUri) throws IOException;
}

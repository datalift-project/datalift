package org.datalift.core.project;

import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;


@Entity
@RdfsClass("datalift:fileSource")
public class FileSource extends BaseSource
{

    @RdfProperty("datalift:url")
    private String url;
    @RdfProperty("datalift:mimeType")
    private String mimeType;

    private FileReader freader;

    public void	init(String storagePath) throws FileNotFoundException {
    	try {
			freader = new FileReader(storagePath);
		} catch (FileNotFoundException e) {
			throw e;
		}
    }

    public String getMimeType()
    {
        return mimeType;
    }

    public void setMimeType(String mimeType)
    {
        this.mimeType = mimeType;
    }

    public void setUrl(String url)
    {
        this.url = url;
    }

    public String getUrl()
    {
        return url;
    }
    
    public FileReader getReader() {
    	return freader;
    }
}

package org.datalift.fwk.project;

public interface ServiceSource extends Source
{
    public String getVersion();
    public void setVersion(String version);

    public String getPublisher();
    public void setPublisher(String publisher);

    public String getserverTypeStrategy();
    public void setserverTypeStrategy(String s);
}

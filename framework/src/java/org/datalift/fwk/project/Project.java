package org.datalift.fwk.project;

import java.util.Collection;

public interface Project
{
    public String getUri();

    public String getTitle();
    public void setTitle(String t);

    public String getOwner();
    public void setOwner(String o);

    public String getDescription();
    public void setDescription(String d);

    public void addSource(Source s);

    public Collection<Source> getSources();
}

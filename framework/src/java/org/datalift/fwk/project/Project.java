package org.datalift.fwk.project;

import java.net.URI;
import java.util.Collection;
import java.util.Date;


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
    
    public Date getDateCreation();
    public void setDateCreation(Date date);

    public Date getDateModification();
    public void setDateModification(Date date);

    public URI getLicense();
    public void setLicense(URI license);
    
    public Collection<Ontology> getOntologies();
    public void addOntology(Ontology vocabulary);
    
    public URI getExecution();
    public void setExecution(URI execution);
  
}

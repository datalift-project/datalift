package org.datalift.fwk.project;

import java.net.URI;
import java.util.Date;

public interface Ontology
{
    public String getUri();
    
    public String getTitle();
    public void setTitle(String title);

    public URI getSource();
    public void setSource(URI source);
    
    public Date getDateSubmitted();
    public void setDateSubmitted(Date dateSubmitted);
    
    public String getOperator();
    public void setOperator(String operator);
}

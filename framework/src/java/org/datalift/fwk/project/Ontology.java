package org.datalift.fwk.project;

import java.util.Date;

public interface Ontology
{
    public String getTitle();
    public void setTitle(String title);

    public Date getDateSubmitted();
    public void setDateSubmitted(Date dateSubmitted);
    
    public String getOperator();
    public void setOperator(String operator);
}

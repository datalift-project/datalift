package org.datalift.fwk.project;


public interface TransformedRdfSource extends Source
{
    public void setTargetGraph(String targetGraph);
    public String getTargetGraph();
}

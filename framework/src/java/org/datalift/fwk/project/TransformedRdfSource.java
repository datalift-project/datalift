package org.datalift.fwk.project;


public interface TransformedRdfSource extends Source
{
    public String getTargetGraph();
    public Source getParent();
}

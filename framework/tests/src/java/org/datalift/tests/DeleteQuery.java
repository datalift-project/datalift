package org.datalift.tests;


import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;


public class DeleteQuery extends UpdateQuery
{
    public DeleteQuery() {
        super("DELETE");
    }

    public DeleteQuery(String targetGraph) {
        this(new URIImpl(targetGraph));
    }

    public DeleteQuery(URI targetGraph) {
        super("DELETE", targetGraph);
    }
}

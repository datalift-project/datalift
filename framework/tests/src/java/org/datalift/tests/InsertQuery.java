package org.datalift.tests;


import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;


public class InsertQuery extends UpdateQuery
{
    public InsertQuery() {
        super("INSERT");
    }

    public InsertQuery(String targetGraph) {
        this(new URIImpl(targetGraph));
    }

    public InsertQuery(URI targetGraph) {
        super("INSERT", targetGraph);
    }
}

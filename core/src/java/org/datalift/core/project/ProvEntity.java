package org.datalift.core.project;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfsClass;


/**
 * Just a marker class to append prov:Entity RDF type.
 */
@Entity
@RdfsClass("prov:Entity")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="datalift:datalift")
public class ProvEntity extends BaseRdfEntity
{
    @RdfId
    private String uri;

    public ProvEntity() {
        // NOP
    }

    public ProvEntity(String uri) {
        this.setId(uri);
    }

    @Override
    protected void setId(String id) {
        this.uri = id;
    }

    @Override
    public String toString() {
        return "{ " + this.getClass().getSimpleName() + ": " + this.uri + " }";
    }
}

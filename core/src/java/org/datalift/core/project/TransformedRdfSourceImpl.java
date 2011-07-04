package org.datalift.core.project;


import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;


@Entity
@RdfsClass("datalift:TransformedRdfSource")
public class TransformedRdfSourceImpl extends BaseSource
                                      implements TransformedRdfSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:targetGraph")
    private String targetGraph;
    @RdfProperty("datalift:parentSource")
    private Source parent;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public TransformedRdfSourceImpl() {
        super(SourceType.TransformedRdfSource);
    }

    public TransformedRdfSourceImpl(String uri) {
        super(SourceType.TransformedRdfSource, uri);
    }

    //-------------------------------------------------------------------------
    // BaseSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(File docRoot, URI baseUri) throws IOException {
        // Nothing to initialize.
    }

    //-------------------------------------------------------------------------
    // TransformedRdfSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getTargetGraph() {
        return this.targetGraph;
    }

    /** {@inheritDoc} */
    @Override
    public Source getParent() {
        return this.parent;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void setTargetGraph(String targetGraph) {
        this.targetGraph = targetGraph;
    }

    public void setParent(Source parent) {
        this.parent = parent;
    }
}

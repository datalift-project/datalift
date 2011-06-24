package org.datalift.projectmanager;


import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

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
    public void setTargetGraph(String targetGraph) {
        this.targetGraph = targetGraph;
    }

    /** {@inheritDoc} */
    @Override
    public String getTargetGraph() {
        return targetGraph;
    }
}

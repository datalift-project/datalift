package org.datalift.projectmanager;


import java.io.File;
import java.io.IOException;
import java.net.URI;

import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Source;

import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;


@MappedSuperclass
public abstract class BaseSource extends BaseRdfEntity implements Source
{
    @RdfId
    private String uri;
    @RdfProperty("dc:title")
    private String title;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    protected BaseSource() {
        // NOP
    }

    protected BaseSource(String uri) {
        this.uri = uri;
    }

    //-------------------------------------------------------------------------
    // Source contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getTitle(){
        return title;
    }
    /** {@inheritDoc} */
    @Override
    public void setTitle(String t) {
        title = t;
    }

    //-------------------------------------------------------------------------
    // BaseRdfEntity contract support
    //-------------------------------------------------------------------------

    protected void setId(String id) {
        this.uri = id;
    }

    //-------------------------------------------------------------------------
    // BaseSource contract definition
    //-------------------------------------------------------------------------

    public String getUri() {
        return this.uri;
    }

    abstract public void init(File docRoot, URI baseUri) throws IOException;
}

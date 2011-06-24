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
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfId
    private String uri;
    @RdfProperty("dc:title")
    private String title;

    private final SourceType type;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    protected BaseSource(SourceType type) {
        this(type, null);
    }

    protected BaseSource(SourceType type, String uri) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        this.type = type;
        this.uri  = uri;
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

    /** {@inheritDoc} */
    @Override
    public final SourceType getType() {
        return this.type;
    }

    //-------------------------------------------------------------------------
    // BaseRdfEntity contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
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

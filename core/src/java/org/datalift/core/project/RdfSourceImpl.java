package org.datalift.core.project;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.persistence.Entity;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;

import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.rdf.RdfUtils;

import static org.datalift.fwk.rdf.RdfUtils.*;


@Entity
@RdfsClass("datalift:rdfSource")
public class RdfSourceImpl extends BaseFileSource<Statement>
                           implements RdfSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private transient Collection<Statement> content = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public RdfSourceImpl() {
        super(SourceType.RdfSource);
    }

    public RdfSourceImpl(String uri) {
        super(SourceType.RdfSource, uri);
    }

    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    @Override
    public void init(File docRoot, URI baseUri) throws IOException {
        super.init(docRoot, baseUri);
        InputStream in = this.getInputStream();
        if (in != null) {
            RDFParser parser = RdfUtils.newRdfParser(this.getMimeType());
            Collection<Statement> l = new LinkedList<Statement>();
            if (parser != null) {
                try {
                    StatementCollector collector = new StatementCollector(l);
                    parser.setRDFHandler(collector);
                    parser.parse(in, (baseUri != null)? baseUri.toString(): "");
                } catch (Exception e) {
                    throw new IOException("Error while parsing RDF source");
                }
            }
            this.content = Collections.unmodifiableCollection(l);
        }
    }

    @Override
    public void setMimeType(String mimeType) {
        super.setMimeType(parseMimeType(mimeType).toString());
    }

    //-------------------------------------------------------------------------
    // RdfSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Iterator<Statement> iterator() {
        if (this.content == null) {
            throw new IllegalStateException();
        }
        return this.content.iterator();
    }
}

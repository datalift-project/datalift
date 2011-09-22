/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.core.project;


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

import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.util.CloseableIterator;

import static org.datalift.fwk.rdf.RdfUtils.*;


/**
 * Default implementation of the {@link RdfFileSource} interface.
 *
 * @author hdevos
 */
@Entity
@RdfsClass("datalift:rdfSource")
public class RdfSourceImpl extends BaseFileSource<Statement>
                           implements RdfFileSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private transient Collection<Statement> content = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new RDF source.
     */
    public RdfSourceImpl() {
        super(SourceType.RdfSource);
    }

    /**
     * Creates a new RDF source with the specified identifier.
     * @param  uri    the source unique identifier (URI) or
     *                <code>null</code> if not known at this stage.
     */
    public RdfSourceImpl(String uri) {
        super(SourceType.RdfSource, uri);
    }

    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration, URI baseUri)
                                                            throws IOException {
        super.init(configuration, baseUri);

        InputStream in = this.getInputStream();
        if (in != null) {
            RDFParser parser = RdfUtils.newRdfParser(this.getMimeType());
            Collection<Statement> l = new LinkedList<Statement>();
            if (parser != null) {
                try {
                    StatementCollector collector = new StatementCollector(l);
                    parser.setRDFHandler(collector);
                    parser.parse(in, (baseUri != null)? baseUri.toString(): "");
                }
                catch (Exception e) {
                    throw new IOException("Error while parsing RDF source", e);
                }
            }
            this.content = Collections.unmodifiableCollection(l);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setMimeType(String mimeType) {
        super.setMimeType(parseMimeType(mimeType).toString());
    }

    //-------------------------------------------------------------------------
    // RdfSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Statement> iterator() {
        if (this.content == null) {
            throw new IllegalStateException("Not initialized");
        }
        final Iterator<Statement> i = this.content.iterator();
        return new CloseableIterator<Statement>() {
                @Override
                public boolean hasNext() {
                    return i.hasNext();
                }
    
                @Override
                public Statement next() {
                    return i.next();
                }
    
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void close() {
                    // NOP
                }
            };
    }
}

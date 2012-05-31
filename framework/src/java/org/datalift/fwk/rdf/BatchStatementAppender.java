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

package org.datalift.fwk.rdf;


import java.net.URISyntaxException;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.helpers.RDFHandlerBase;

import org.datalift.fwk.util.UriMapper;

import static org.datalift.fwk.rdf.RdfUtils.*;
import static org.datalift.fwk.util.Env.*;


/**
 * An {@link RDFHandler} implementation that inserts triples into an
 * RDF store in batches of configurable size, allowing to
 * {@link UriMapper translate URIs} on the fly. It also sanitizes the
 * string literal values by
 * {@link RdfUtils#removeInvalidDataCharacter(String) removing invalid
 * characters} that may be present in N3 or Turtle RDF files.
 *
 * @author lbihanic
 */
public final class BatchStatementAppender extends RDFHandlerBase
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final RepositoryConnection cnx;
    private final ValueFactory valueFactory;
    private final URI targetGraph;
    private final UriMapper mapper;
    private final int batchSize;

    private long statementCount = -1L;
    private long startTime = -1L;
    private long duration = -1L;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new statement appender with the default batch size.
     * @param  cnx           the connection to the RDF store.
     * @param  targetGraph   the named graph to which the inserted
     *                       triples shall belong or <code>null</code>
     *                       to insert the triples in the default graph.
     * @param  mapper        an optional URI mapper to translate URIs
     *                       while processing triples.
     */
    public BatchStatementAppender(RepositoryConnection cnx,
                                  URI targetGraph,
                                  UriMapper mapper) {
        this(cnx, targetGraph, mapper, getRdfBatchSize());
    }

    /**
     * Creates a new statement appender with the specified batch size.
     * @param  cnx           the connection to the RDF store.
     * @param  targetGraph   the named graph to which the inserted
     *                       triples shall belong or <code>null</code>
     *                       to insert the triples in the default graph.
     * @param  mapper        an optional URI mapper to translate URIs
     *                       while processing triples.
     * @param  batchSize     the size of triple batches, as a number of
     *                       triples.
     */
    public BatchStatementAppender(RepositoryConnection cnx,
                                  URI targetGraph,
                                  UriMapper mapper, int batchSize) {
        super();

        if (cnx == null) {
            throw new IllegalArgumentException("cnx");
        }
        if (batchSize < 0) {
            throw new IllegalArgumentException("batchSize");
        }
        this.cnx = cnx;
        this.valueFactory = cnx.getValueFactory();
        this.targetGraph = targetGraph;
        this.mapper = mapper;
        // Batches can't be too small.
        this.batchSize = (batchSize < MIN_RDF_IO_BATCH_SIZE)?
                                            MIN_RDF_IO_BATCH_SIZE: batchSize;
    }

    //-------------------------------------------------------------------------
    // RDFHandler contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void startRDF() {
        this.startTime = System.currentTimeMillis();
        this.statementCount = 0L;
        try {
            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
        }
        catch (RepositoryException e) {
            throw new RuntimeException("RDF triple insertion failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void handleStatement(Statement stmt) {
        try {
            Resource s = stmt.getSubject();
            org.openrdf.model.URI p = stmt.getPredicate();
            Value o = checkStringLitteral(stmt.getObject());

            if (mapper != null) {
                // Map URIs.
                s = (Resource)(this.mapValue(s));
                p = this.mapUri(p);
                o = this.mapValue(o);
            }
            this.cnx.add(s, p, o, this.targetGraph);

            // Commit transaction according to the configured batch size.
            this.statementCount++;
            if ((this.statementCount % this.batchSize) == 0) {
                this.cnx.commit();
            }
        }
        catch (RepositoryException e) {
            throw new RuntimeException("RDF triple insertion failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void endRDF() {
        try {
            this.cnx.commit();
            this.duration = System.currentTimeMillis() - this.startTime;
        }
        catch (RepositoryException e) {
            throw new RuntimeException("RDF triple insertion failed", e);
        }
    }

    //-------------------------------------------------------------------------
    // BatchStatementAppender contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the number of RDF triples that were inserted.
     * @return the number of RDF triples that were inserted.
     */
    public long getStatementCount() {
        return this.statementCount;
    }

    /**
     * Returns the duration of the triples processing, including the
     * amount of time taken by the underlying RDF parser and RDF store.
     * @return the duration of the triples processing.
     */
    public long getDuration() {
        return this.duration;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private Value checkStringLitteral(Value v) {
        if (v instanceof Literal) {
            Literal l = (Literal)v;
            if (l.getDatatype() == null) {
                String s = l.stringValue();
                if (! isValidStringLiteral(s)) {
                    v = valueFactory.createLiteral(
                                removeInvalidDataCharacter(s), l.getLanguage());
                }
            }
        }
        return v;
    }

    private Value mapValue(Value v) {
        return (v instanceof org.openrdf.model.URI)?
                                    this.mapUri((org.openrdf.model.URI)v): v;
    }

    private org.openrdf.model.URI mapUri(org.openrdf.model.URI u) {
        try {
            return this.valueFactory.createURI(
                        this.mapper.map(new java.net.URI(u.stringValue()))
                                   .toString());
        }
        catch (URISyntaxException e) {
            // Should never happen.
            throw new RuntimeException(e);
        }
    }
}

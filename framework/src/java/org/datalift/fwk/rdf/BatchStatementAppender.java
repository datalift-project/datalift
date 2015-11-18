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


import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.helpers.RDFHandlerBase;

import static org.datalift.fwk.util.Env.*;


/**
 * An {@link RDFHandler} implementation that inserts triples into an
 * RDF store in batches of configurable size.
 *
 * @author lbihanic
 */
public class BatchStatementAppender extends RDFHandlerBase
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    protected final RepositoryConnection cnx;
    protected final ValueFactory valueFactory;
    protected final URI targetGraph;
    protected final int batchSize;

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
     */
    public BatchStatementAppender(RepositoryConnection cnx,
                                  URI targetGraph) {
        this(cnx, targetGraph, getRdfBatchSize());
    }

    /**
     * Creates a new statement appender with the specified batch size.
     * @param  cnx           the connection to the RDF store.
     * @param  targetGraph   the named graph to which the inserted
     *                       triples shall belong or <code>null</code>
     *                       to insert the triples in the default graph.
     * @param  batchSize     the size of triple batches, as a number of
     *                       triples.
     */
    public BatchStatementAppender(RepositoryConnection cnx,
                                  URI targetGraph, int batchSize) {
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
        // Batches can't be too small.
        this.batchSize = (batchSize < MIN_RDF_IO_BATCH_SIZE)?
                                            MIN_RDF_IO_BATCH_SIZE: batchSize;
    }

    //-------------------------------------------------------------------------
    // RDFHandler contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final void startRDF() {
        this.startTime = System.currentTimeMillis();
        this.statementCount = 0L;
        try {
            // Prevent transaction commit for each triple inserted.
            cnx.begin();
        }
        catch (RepositoryException e) {
            throw new RuntimeException("RDF triple insertion failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void handleStatement(Statement stmt) {
        try {
            // Add statement.
            this.addStatement(stmt);
            // Commit transaction according to the configured batch size.
            this.statementCount++;
            if ((this.statementCount % this.batchSize) == 0) {
                this.cnx.commit();
                this.cnx.begin();
            }
        }
        catch (RepositoryException e) {
            throw new RuntimeException("RDF triple insertion failed", e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final void endRDF() {
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
    public final long getStatementCount() {
        return this.statementCount;
    }

    /**
     * Returns the duration of the triples processing, including the
     * amount of time taken by the underlying RDF parser and RDF store.
     * @return the duration of the triples processing as a number of
     *         milliseconds.
     */
    public final long getDuration() {
        return this.duration;
    }

    /**
     * Appends the specified RDF triple to the repository.
     * @param  stmt   the RDF triple.
     *
     * @throws RepositoryException if any error occurred while adding
     *         the triple through the repository connection.
     */
    protected void addStatement(Statement stmt) throws RepositoryException {
        if (this.targetGraph != null) {
            this.cnx.add(stmt, this.targetGraph);
        }
        else {
            this.cnx.add(stmt);
        }
    }
}

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

package org.datalift.core.rdf;


import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.util.CloseableIterator;


/**
 * A utility class for performing an asynchronous RDF parse populating
 * a {@link Statement statement} iterator but limiting the number of
 * statements stored in memory. The iterator client speed controls the
 * parser speed.
 *
 * @author lbihanic
 */
public final class BoundedAsyncRdfParser
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The default buffer size for parsed RDF statements. */
    public final static int DEFAULT_BUFFER_SIZE = 1000;
    /** The minimum buffer size for parsed RDF statements. */
    public final static int MIN_BUFFER_SIZE = 100;

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** Worker threads for async. parsing of RDF data. */
    private final static ExecutorService threadPool =
                                            Executors.newCachedThreadPool();

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor, private on purpose. */
    private BoundedAsyncRdfParser() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Parses the specified RDF data stream.
     * @param  in         the RDF data stream to parse.
     * @param  mimeType   the expected type of the data.
     * @param  baseUri    the base URI to translate relative URIs.
     *
     * @return an iterator on the parsed RDF statements.
     *
     * @see    #parse(InputStream, String, String, int)
     */
    public static CloseableIterator<Statement> parse(InputStream in,
                                        String mimeType, String baseUri) {
        return parse(in, mimeType, baseUri, DEFAULT_BUFFER_SIZE);
    }

    /**
     * Parses the specified RDF data stream.
     * @param  in           the RDF data stream to parse.
     * @param  mimeType     the expected type of the data.
     * @param  baseUri      the base URI to translate relative URIs.
     * @param  bufferSize   the number of RDF statements the iterator
     *                      can buffer.
     *
     * @return an iterator on the parsed RDF statements.
     *
     * @see    #parse(InputStream, String, String, int)
     */
    public static CloseableIterator<Statement> parse(final InputStream in,
                                        String mimeType, final String baseUri,
                                        int bufferSize) {
        if (bufferSize < MIN_BUFFER_SIZE) {
            bufferSize = MIN_BUFFER_SIZE;
        }
        // Validate that provided MIME type is supported.
        final RDFParser parser = RdfUtils.newRdfParser(mimeType);
        // Use a blocking queue to control the memory alloted to the
        // being-read RDF statements. Let the producer (RDF parser) be
        // ahead of the consumer (iterator client) by bufferSize statements.
        final BlockingQueue<Statement> statements =
                                new ArrayBlockingQueue<Statement>(bufferSize);
        // Parse RDF data in a separate thread, queuing the read statements.
        final Future<Void> f = threadPool.submit(new Callable<Void>()
            {            
                @Override
                public Void call() {
                    try {
                        parser.setRDFHandler(new RDFHandlerBase()
                            {
                                @Override
                                public void handleStatement(Statement stmt)
                                                    throws RDFHandlerException {
                                    publish(stmt);
                                }
                            });
                        parser.parse(in, RdfUtils.getBaseUri(baseUri));
                    }
                    catch (RuntimeException e) {
                        throw e;
                    }
                    catch (Exception e) {
                        throw new TechnicalException(
                                        "rdf.parse.error", e, e.getMessage());
                    }
                    finally {
                        this.publish(new EndOfParse());
                        try {
                            in.close();
                        }
                        catch (Exception e) { /* Ignore... */ }
                    }
                    return null;
                }

                private void publish(Statement stmt) {
                    try {
                        statements.put(stmt);
                    }
                    catch (InterruptedException e) {
                        throw new TechnicalException(null, e);
                    }
                }
            });
        // Return an iterator consuming the statement queue.
        return new CloseableIterator<Statement>()
            {
                private Statement current = this.getNextStatement();

                @Override
                public boolean hasNext() {
                    return (this.current != null);
                }

                @Override
                public Statement next() {
                    Statement next = this.current;
                    this.current = this.getNextStatement();
                    return next;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                @Override
                public void close() {
                    if (! f.isDone()) {
                        // Let some time to the parse thread to terminate.
                        Thread.yield();
                        // Abort RDF parse.
                        f.cancel(true);
                    }
                    // Wait for task termination.
                    try {
                        f.get();
                    }
                    catch (ExecutionException e) {
                        log.warn("RDF parse failed", e);
                        throw (RuntimeException)(e.getCause());
                    }
                    catch (Exception e) {
                        TechnicalException error = new TechnicalException(
                                    "Unexpected error thrown by RDF parse", e);
                        log.fatal(error.getLocalizedMessage(), e);
                        throw error;
                    }
                    finally {
                        // Make sure all resources are properly released.
                        try {
                            in.close();
                        }
                        catch (Exception e) { /* Ignore... */ }
                    }
                }

                /**
                 * Ensures resources are released even when
                 * {@link #close()} has not been invoked by user class.
                 */
                @Override
                protected void finalize() {
                    this.close();
                }

                /**
                 * Retrieves the next available statement from the
                 * queue, blocking until one is available.
                 * @return the next parsed {@link Statement} or
                 *         <code>null</code> if the parse is complete.
                 */
                private Statement getNextStatement() {
                    Statement stmt = null;
                    // Consume next statement from queue.
                    try {
                        stmt = statements.take();
                        if (stmt instanceof EndOfParse) {
                            // Parse complete.
                            stmt = null;
                        }
                    }
                    catch (InterruptedException e) {
                        // Thread interrupted.
                        throw new RuntimeException(e);
                    }
                    finally {
                        if (stmt == null) {
                            this.close();
                        }
                    }
                    return stmt;
                }
            };
    }

    //-------------------------------------------------------------------------
    // NullStatement nested class
    //-------------------------------------------------------------------------

    /**
     * A specific {@link Statement} implementation to denote the end of
     * the RDF parse.
     */
    private final static class EndOfParse implements Statement
    {
        /** Default constructor. */
        public EndOfParse() {
            super();
        }

        @Override public Resource getSubject() { return null; }
        @Override public Value getObject()     { return null; }
        @Override public URI getPredicate()    { return null; }
        @Override public Resource getContext() { return null; }
    }
}

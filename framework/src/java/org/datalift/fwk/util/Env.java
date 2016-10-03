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

package org.datalift.fwk.util;


import org.datalift.fwk.Configuration;


/**
 * Provides access to the DataLift runtime environment configuration
 * parameters.
 *
 * @author lbihanic
 */
public final class Env
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The configuration property defining the file I/O buffer size. */
    public final static String FILE_BUFFER_SIZE_PROP =
                                            "datalift.file.buffer.size";
    /**
     * The configuration property defining the RDF I/O batch size,
     * i.e. the maximum number of uncommitted triples in a transaction.
     */
    public final static String RDF_IO_BATCH_SIZE_PROP =
                                            "datalift.rdf.batch.size";

    /** The default buffer size for file access: 32 KB. */
    public final static int DEFAULT_FILE_BUFFER_SIZE = 32768;
    /** The minimum buffer size for file access: 8 KB. */
    public final static int MIN_FILE_BUFFER_SIZE = 8192;

    /** The default batch size for RDF I/Os: 10000 uncommitted triples. */
    public final static int DEFAULT_RDF_IO_BATCH_SIZE = 10000;

    /** The minimum batch size for RDF I/Os: 100 uncommitted triples. */
    public final static int MIN_RDF_IO_BATCH_SIZE = 100;


    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private static int fileBufferSize = -1;
    private static int rdfBatchSize = -1;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor, private on purpose. */
    private Env() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // Environment configuration accessors
    //-------------------------------------------------------------------------

    /**
     * Returns the configured file I/O buffer size.
     * @return the configured file I/O buffer size, as a number of
     *         bytes.
     */
    public static int getFileBufferSize() {
        if (fileBufferSize <= 0) {
            int n = 0;
            try {
                n = Integer.parseInt(
                        Configuration.getDefault()
                                     .getProperty(FILE_BUFFER_SIZE_PROP, "0"));
            }
            catch (Exception e) { /* Ignore... */ }

            fileBufferSize = (n <= MIN_FILE_BUFFER_SIZE)?
                                                DEFAULT_FILE_BUFFER_SIZE: n;
        }
        return fileBufferSize;
    }

    /**
     * Returns the configured RDF I/O batch size.
     * @return the configured RDF I/O batch size, as a number of
     *         triples.
     */
    public static int getRdfBatchSize() {
        if (rdfBatchSize <= 0) {
            int n = 0;
            try {
                n = Integer.parseInt(
                        Configuration.getDefault()
                                     .getProperty(RDF_IO_BATCH_SIZE_PROP, "0"));
            }
            catch (Exception e) { /* Ignore... */ }

            rdfBatchSize = (n <= MIN_RDF_IO_BATCH_SIZE)?
                                                DEFAULT_RDF_IO_BATCH_SIZE: n;
        }
        return rdfBatchSize;
    }
}

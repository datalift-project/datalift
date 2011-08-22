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

package org.datalift.fwk.project;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;


/**
 * A file-based source object.
 *
 * @param  <T>   The type of data the source contains and that can be
 *               iterated over when reading through this source object.
 *
 * @author hdevos
 */
public interface FileSource<T> extends Source, Iterable<T>
{
    /**
     * Returns the declared type of data this source contains.
     * @return the declared MIME type for the source content.
     */
    public String getMimeType();

    /**
     * Sets the data type of the source content.
     * @param  mimeType   type of data this source contains. 
     */
    public void setMimeType(String mimeType);

    /**
     * Returns the path (relative to the DataLift public storage
     * directory) of the file containing the source data.
     * @return the data file relative path.
     */
    public String getFilePath();

    /**
     * Returns an input stream for reading the source content.
     * @return an input stream
     * @throws IOException if any error occurred accessing the source
     *         data file.
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Initializes this source to make the source data accessible.
     * This method shall be invoke prior any
     * {@link #getInputStream() attempt to access} the file content.
     * @param docRoot   the DataLift public storage directory.
     * @param baseUri   the base URI for this DataLift deployment.
     *
     * @throws IOException if any error occurred accessing the source
     *         data file.
     */
    public void init(File docRoot, URI baseUri) throws IOException;
}

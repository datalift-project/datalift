/*
 * Copyright / Copr. IGN
 * Contributor(s) : F. Hamdi
 *
 * Contact: hamdi.faycal@gmail.com
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

import java.io.IOException;
import java.io.InputStream;

/**
 * A Shapefile source.
 *
 * @author Fayçal Hamdi
 */
public interface ShpSource extends FileSource
{
    /**
     * Returns the path of the Shape file (SHP) for this Shapefile
     * source.
     * @return the Shape file path, relative to the DataLift public
     *         storage directory.
     */
    public String getShapeFilePath();

    /**
     * Returns the path of the index file (SHX) for this Shapefile
     * source.
     * @return the index file path, relative to the DataLift public
     *         storage directory.
     */
    public String getIndexFilePath();

    /**
     * Returns the path of the attribute file (DBF) for this Shapefile
     * source.
     * @return the attribute file path, relative to the DataLift public
     *         storage directory.
     */
    public String getAttributeFilePath();

    /**
     * Returns the path of the projection file (PRJ) for this Shapefile
     * source.
     * @return the projection file path, relative to the DataLift public
     *         storage directory.
     */
    public String getProjectionFilePath();

    /**
     * Returns an input stream for reading the Shape file (SHP)
     * content.
     * @return an input stream
     * @throws IOException if any error occurred accessing the file.
     */
    public InputStream getShapeFileInputStream() throws IOException;

    /**
     * Returns an input stream for reading the index file (SHX)
     * content.
     * @return an input stream
     * @throws IOException if any error occurred accessing the file.
     */
    public InputStream getIndexFileInputStream() throws IOException;

    /**
     * Returns an input stream for reading the attribute file (DBF)
     * content.
     * @return an input stream
     * @throws IOException if any error occurred accessing the file.
     */
    public InputStream getAttributeFileInputStream() throws IOException;

    /**
     * Returns an input stream for reading the projection file (PRJ)
     * content.
     * @return an input stream
     * @throws IOException if any error occurred accessing the file.
     */
    public InputStream getProjectionFileInputStream() throws IOException;
}

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


import java.util.List;


/**
 * A interface modeling access to a row in matrix-like data.
 * @param  <V>   the type of objects the row contains.
 *
 * @author lbihanic
 */
public interface Row<V> extends Iterable<V>
{
    /**
     * Returns the number of columns this row contains.
     * @return the number of columns this row contains.
     */
    public int size();

    /**
     * Returns the column name for accessing row data.
     * @return the column name.
     */
    public List<String> keys();

    /**
     * Return the row data for the specified key, in native format.
     * @param  key   the column name.
     *
     * @return the data associated to the column identified by
     *         <code>key</code>.
     */
    public V get(String key);

    /**
     * Return the row data for the specified key as a string.
     * @param  key   the column name.
     *
     * @return the string representation of the data associated to
     *         the column identified by <code>key</code>.
     */
    public String getString(String key);

    /**
     * Return the row data at the specified index, in native format.
     * @param  index   the (zero-based) column index.
     *
     * @return the data associated to the column at index
     *         <code>index</code>.
     */
    public V get(int index);

    /**
     * Return the row data at the specified index as a string.
     * @param  index   the (zero-based) column index.
     *
     * @return the string representation of the data associated to
     *         the column at index <code>index</code>.
     */
    public String getString(int index);

    /**
     * Returns the column name at the specified index.
     * @param  index  the (zero-based) column index.
     *
     * @return the column name at index <code>index</code>.
     */
    public String getKey(int index);
}

/*
 * Copyright / Copr. 2010-2015 Atos - Public Sector France -
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

package org.datalift.fwk.util.web;


import java.util.Collection;
import java.util.Iterator;
import java.util.TreeSet;


/**
 * A Datalift GUI menu.
 *
 * @author lbihanic
 */
public class Menu implements Collection<MenuEntry>
{
    // ------------------------------------------------------------------------
    // Instance members
    // ------------------------------------------------------------------------

    /** The menu entries. */
    private final Collection<MenuEntry> entries = new TreeSet<MenuEntry>();

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    /**
     * Creates a new menu.
     */
    public Menu() {
        super();
    }

    // ------------------------------------------------------------------------
    // Collection contract support
    // ------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean add(MenuEntry e) {
        return this.entries.add(e);
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends MenuEntry> c) {
        return this.entries.addAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean remove(Object o) {
        return this.entries.remove(o);
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeAll(Collection<?> c) {
        return this.entries.removeAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        this.entries.clear();
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return this.entries.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isEmpty() {
        return this.entries.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public boolean contains(Object o) {
        return this.entries.contains(o);
    }

    /** {@inheritDoc} */
    @Override
    public boolean containsAll(Collection<?> c) {
        return this.entries.containsAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public boolean retainAll(Collection<?> c) {
        return this.entries.retainAll(c);
    }

    /** {@inheritDoc} */
    @Override
    public Iterator<MenuEntry> iterator() {
        return this.entries.iterator();
    }

    /** {@inheritDoc} */
    @Override
    public Object[] toArray() {
        return this.entries.toArray();
    }

    /** {@inheritDoc} */
    @Override
    public <T> T[] toArray(T[] a) {
        return this.entries.toArray(a);
    }
}

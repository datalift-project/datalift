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

package org.datalift.tests;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

import org.openrdf.model.Statement;


public final class WhereClauses implements Iterable<Statement>
{
    public enum WhereType {
        DEFAULT         (""),
        OPTIONAL        ("OPTIONAL"),
        UNION           ("UNION"),
        MINUS           ("MINIUS");

        public final String label;

        WhereType(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return this.label;
        }
    }

    // public final String name;
    public final WhereType type;
    public final Collection<Statement> clauses = new LinkedList<Statement>();
    public Collection<WhereClauses> children = new LinkedList<WhereClauses>();

    public WhereClauses() {
        this(WhereType.DEFAULT, null, null);
    }

    public WhereClauses(String name, WhereClauses parent) {
        this(WhereType.DEFAULT, name, parent);
    }

    public WhereClauses(WhereType type, String name, WhereClauses parent) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        // this.name = name;
        this.type = type;
        if (parent != null) {
            parent.add(this);
        }
    }

    public WhereClauses add(Statement s) {
        this.clauses.add(s);
        return this;
    }

    public boolean isEmpty() {
        return (this.clauses.isEmpty() && this.children.isEmpty());
    }

    private WhereClauses add(WhereClauses child) {
        this.children.add(child);
        return this;
    }

    @Override
    public Iterator<Statement> iterator() {
        return this.clauses.iterator();
    }
}

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


import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;


abstract public class GraphPattern implements Statement
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** An empty URI: no namespace, no name, nothing! */
    private final static URI EMPTY_URI = new URI() {
        @Override public String getLocalName() { return ""; }
        @Override public String getNamespace() { return ""; }
        @Override public String stringValue()  { return ""; }
        @Override public String toString()     { return this.stringValue(); }
    };

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The named graph associated to this pattern. */
    public final URI graph;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new graph pattern.
     */
    protected GraphPattern() {
        this(null);
    }

    /**
     * Creates a new graph pattern for the specified named graph.
     * @param  graph   the named graph associated to the pattern or
     *                 <code>null</code> if the pattern is not bound
     *                 to any specific named graph.
     */
    protected GraphPattern(URI graph) {
        this.graph = graph;
    }

    //-------------------------------------------------------------------------
    // GraphPattern contract definition
    //-------------------------------------------------------------------------

    abstract String stringValue();

    //-------------------------------------------------------------------------
    // Statement contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final String toString() {
        return this.stringValue();
    }

    /** {@inheritDoc} */
    @Override
    public final Resource getContext() {
        return this.graph;
    }

    /** {@inheritDoc} */
    @Override
    public final Resource getSubject() {
        return EMPTY_URI;
    }

    /** {@inheritDoc} */
    @Override
    public final URI getPredicate() {
        return EMPTY_URI;
    }

    /** {@inheritDoc} */
    @Override
    public final Value getObject() {
        final String v = this.stringValue();
        return new Value() {
                @Override public String stringValue() { return v; }
                @Override public String toString()    { return v; }
        };
    }
}

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

package org.datalift.sparql.query.impl;


import static org.datalift.fwk.util.StringUtils.isSet;

import java.util.regex.Pattern;

import org.datalift.sparql.query.Variable;


/**
 * Default implementation for SPARQL query variable.
 *
 * @author lbihanic
 */
public final class VariableImpl implements Variable
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Regular expr. for matching punctuation, control and blank characters. */
    private final static Pattern NON_BASE_CHARS =
                    Pattern.compile("[\\p{Punct}\\p{Space}\\p{Cntrl}]+");

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The SPARQL variable name, without the '?' prefix. */
    public final String name;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new variable with the specified name.
     * @param  name   the variable name, with or without the '?' prefix.
     *
     * @throws IllegalArgumentException if <code>name</code> is
     *         <code>null</code> or empty.
     */
    public VariableImpl(String name) {
        if (! isSet(name)) {
            throw new IllegalArgumentException("name");
        }
        if (name.charAt(0) == '?') {
            name = name.substring(1);
        }
        this.name = NON_BASE_CHARS.matcher(name).replaceAll("_");
    }

    //-------------------------------------------------------------------------
    // Variable contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String stringValue()  {
        return "?" + this.name;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.stringValue();
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return this.stringValue().hashCode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        return (o instanceof VariableImpl)?
            this.stringValue().equals(((VariableImpl)o).stringValue()): false;
    }
}

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

package org.datalift.core.project;


import java.util.Date;

import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.SupportsRdfId;
import com.clarkparsia.empire.annotation.SupportsRdfIdImpl;

import org.datalift.fwk.util.Iso8601DateFormat;


/**
 * An abstract superclass for all Java objects to be persisted in RDF.
 * <p>
 * <strong>Warning</strong>: No implementation class or method shall
 * be marked <code>final</code> as Empire RDF JPA provider relies on
 * runtime-generated proxies to manipulate objects. Final methods
 * can not be overridden; hence leading to unexpected behaviors.</p>
 *
 * @author lbihanic
 */
@MappedSuperclass
public abstract class BaseRdfEntity implements SupportsRdfId
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private SupportsRdfId rdfId = new SupportsRdfIdImpl();

    //-------------------------------------------------------------------------
    // BaseRdfEntity contract definition
    //-------------------------------------------------------------------------

    /**
     * Set the RDF identifier (URI) for this entity.
     * @param  id   the URI identifying this entity.
     */
    abstract protected void setId(String id);

    //-------------------------------------------------------------------------
    // SupportsRdfId contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public RdfKey<?> getRdfId() {
        return this.rdfId.getRdfId();
    }

    /** {@inheritDoc} */
    @Override @SuppressWarnings("unchecked")
    public void setRdfId(RdfKey id) {
        this.rdfId.setRdfId(id);
        this.setId(String.valueOf(id));
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean equals(Object o) {
        return ((o != null) &&
                o.getClass().equals(this.getClass()) &&
                String.valueOf(this.getRdfId()).equals(
                                String.valueOf(((BaseRdfEntity)o).getRdfId())));
    }

    /** {@inheritDoc} */
    @Override
    public int hashCode() {
        return (this.getRdfId() != null)? this.getRdfId().hashCode():
                                          System.identityHashCode(this);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return String.valueOf(this.getRdfId());
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Utility method to perform a defensive copy of a user-provided
     * date, as {@link Date dates} are mutable objects.
     * @param  date   the {@link Date} object to copy.
     *
     * @return a clone of the specified {@link Date} object.
     */
    protected final Date copy(final Date date) {
        return (date != null)? new Date(date.getTime()): null;
    }

    /**
     * Formats a date in {@link Iso8601DateFormat ISO-8601 format}.
     * @param  date   the date to format.
     * @return the ISO-8601 representation of the specified date.
     */
    protected final String toString(final Date date) {
        return (date != null)? Iso8601DateFormat.DATETIME_UTC.format(date):
                               "null";
    }
}

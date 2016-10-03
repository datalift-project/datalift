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

package org.datalift.core.velocity;


import java.util.Calendar;
import java.util.Date;

import javax.xml.datatype.DatatypeFactory;

import org.openrdf.model.Literal;


/**
 * An extended version to Velocity DateTool.
 *
 * @author lbihanic
 */
public class DateTool extends org.apache.velocity.tools.generic.DateTool
{
    // ------------------------------------------------------------------------
    // Class members
    // ------------------------------------------------------------------------

    /**
     * A factory to create XML schema datatype objects such as dateTime
     * or duration objects.
     */
    protected final static DatatypeFactory dt;

    // ------------------------------------------------------------------------
    // Class initializer
    // ------------------------------------------------------------------------

    static {
        try {
            dt = DatatypeFactory.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ------------------------------------------------------------------------
    // Constructors
    // ------------------------------------------------------------------------

    public DateTool() {
        super();
    }

    // ------------------------------------------------------------------------
    // DateTool contract support
    // ------------------------------------------------------------------------

    /** {@inheritDoc} */ 
    @Override
    public Date toDate(Object obj) {
        Date d = null;
        if (obj instanceof Literal) {
            Calendar c = this.toCalendar((Literal)obj);
            if (c != null) {
                d = c.getTime();
            }
        }
        else {
            d = super.toDate(obj);
        }
        return d;
    }

    /** {@inheritDoc} */ 
    @Override
    public Calendar toCalendar(Object obj) {
        return (obj instanceof Literal)?
                        this.toCalendar((Literal)obj): super.toCalendar(obj);
    }

    // ------------------------------------------------------------------------
    // Specific implementation
    // ------------------------------------------------------------------------

    private Calendar toCalendar(Literal l) {
        Calendar c = null;
        try {
            // Literal.calendarValue() sometimes returns incorrect values
            // while the literal text (label) is correct. => Parse label.
            c = dt.newXMLGregorianCalendar(l.getLabel()).toGregorianCalendar();
        }
        catch (Exception e) {
            // Not a date.
        }
        return c;
    }
}

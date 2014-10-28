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

package org.datalift.fwk.util.web;


import java.net.URI;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.datalift.fwk.MediaTypes;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * A helper class to improve error reporting when receiving a URI
 * as parameter of a REST resource method.
 * 
 * @author lbihanic
 */
public class UriParam
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final String v;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new UriParam wrapping the specified URI value.
     * @param  v   the URI as a string.
     */
    public UriParam(String v) {
        super();
//        // Ensure all escape sequences are properly decoded back to Unicode.
//        if ((v != null) && (v.indexOf('%') != -1)) {
//            try {
//                v = URLDecoder.decode(v, Charsets.UTF8_CHARSET);
//            }
//            catch (Exception e) {
//                // Can't happen: UTF-8 charset is supported by all JVMs.
//            }
//        }
        this.v = v;
    }

    //-------------------------------------------------------------------------
    // UriParam contract definition
    //-------------------------------------------------------------------------

    /**
     * Parses the wrapped URI value into a URI object.
     * @return the URI, normalized.
     * @throws WebApplicationException if any error occurred parsing
     *         the URI value.
     */
    public URI toUri() throws WebApplicationException {
        return this.toUri(null);
    }

    /**
     * Parses the wrapped URI value into a URI object.
     * @param  parameterName   the name of the URI parameter for the
     *                         resource method to be included in the
     *                         error message in case the URI value is
     *                         invalid.
     *
     * @return the URI, normalized, or <code>null</code> if the URI
     *         value is <code>null</code>, empty or only contains
     *         space characters.
     * @throws WebApplicationException if any error occurred parsing
     *         the URI value.
     */
    public URI toUri(String parameterName) throws WebApplicationException {
        URI u = null;

        String s = trimToNull(this.v);
        if (s != null) {
            try {
                return new URI(s).normalize();
            }
            catch (Exception e) {
                StringBuilder b = new StringBuilder(128);
                if (! isBlank(parameterName)) {
                    b.append(parameterName).append(": ");
                }
                b.append("Invalid URI: \"").append(this.v).append('"');

                throw new WebApplicationException(e,
                            Response.status(Status.BAD_REQUEST)
                                    .entity(b.toString())
                                    .type(MediaTypes.TEXT_PLAIN)
                                    .build());
            }
        }
        return u;
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.v;
    }

    /**
     * Parses the URI wrapped in the specified URI parameter.
     * @param  p   the URI parameter object or <code>null</code>.
     *
     * @return the URI, normalized, or <code>null</code> if the URI
     *         parameter is <code>null</code> or its value is empty or
     *         only contains space characters.
     * @throws WebApplicationException if any error occurred parsing
     *         the URI value.
     * @see    #toUri()
     */
    public static URI valueOf(UriParam p) {
        return valueOf(p, null);
    }

    /**
     * Parses the URI wrapped in the specified URI parameter.
     * @param  p               the URI parameter object or
     *                         <code>null</code>.
     * @param  parameterName   the name of the URI parameter for the
     *                         resource method to be included in the
     *                         error message in case the URI value is
     *                         invalid.
     *
     * @return the URI, normalized, or <code>null</code> if the URI
     *         parameter is <code>null</code> or its value is empty or
     *         only contains space characters.
     * @throws WebApplicationException if any error occurred parsing
     *         the URI value.
     * @see    #toUri(String)
     */
    public static URI valueOf(UriParam p, String parameterName) {
        return (p != null)? p.toUri(parameterName): null;
    }

    /**
     * Checks if a URI parameter is neither <code>null</code> nor
     * contains a <code>null</code> or empty value. The value only
     * made of space characters is deemed empty.
     * @param  u   the URI parameter to check, may be <code>null</code>.
     *
     * @return <code>true</code> if the URI parameter is neither
     *         <code>null</code> nor contains a <code>null</code> or
     *         empty value.
     */
    public static boolean isSet(UriParam u) {
        return ((u != null) && (trimToNull(u.v) != null));
    }
}

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

package org.datalift.fwk.security;


import java.util.Iterator;
import java.util.ServiceLoader;

import javax.servlet.http.HttpServletRequest;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.web.RequestContext;

import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A factory class that abstracts access to the user's security
 * information.
 *
 * @author lbihanic
 */
public abstract class SecurityContext
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** User delegation HTTP header. */
    public final static String ACTING_AS_HEADER = "X-ActingAs";
    /** User delegation role. */
    public final static String DELEGATION_ROLE = "delegation";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /**
     * Cached instance of the configured <code>SecurityContext</code>
     * implementation.
     */
    private static SecurityContext instance = null;

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor.
     */
    protected SecurityContext() {
        super();
    }

    //-------------------------------------------------------------------------
    // SecurityContext contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the login name of the user performing the request.
     * @return the login name or <code>null</code> if no user is
     *         authenticated or identified.
     */
    abstract public String getPrincipal();

    /**
     * Returns whether the user performing the request has been
     * authenticated.
     * <p>
     * Note that this method may return <code>false</code> while a
     * {@link #getPrincipal() user principal} is available in case a
     * Remember-Me protocol is being used.</p>
     * 
     * @return <code>true</code> if the user performing the request has
     *         successfully authenticated; <code>false</code> otherwise.
     */
    public boolean isAuthenticated() {
        return (this.getPrincipal() != null);
    }

    /**
     * Returns whether to current user has the specified role.
     * @param  role   the role name.
     * @return <code>true</code> if the user performing the request has
     *         the specified role; <code>false</code> otherwise.
     */
    abstract public boolean hasRole(String role);

    /**
     * Returns the being-processed HTTP Servlet request
     * @return the being-processed HTTP Servlet request or
     *         <code>null</code> if not available.
     */
    protected final HttpServletRequest getRequestContext() {
        RequestContext ctx = RequestContext.get();
        return (ctx != null)? ctx.request: null;
    }

    /**
     * Checks whether the current user is allowed to
     * {@link #DELEGATION_ROLE act on behalf} of
     * {@link #ACTING_AS_HEADER another user} and, if it does,
     * returns the surrogate user.
     * @param  principal   the login name of the user performing
     *                     the request.
     * @return the surrogate user, if allowed and specified,
     *         otherwise the current user.
     */
    protected final String checkSurrogateUser(String principal) {
        if (this.hasRole(DELEGATION_ROLE)) {
            // User is allowed to delegate access to a surrogate user.
            HttpServletRequest req = this.getRequestContext();
            if (req != null) {
                String surrogateUser = req.getHeader(ACTING_AS_HEADER);
                if (! isBlank(surrogateUser)) {
                    principal = surrogateUser.trim();
                }
            }
        }
        return principal;
    }

    //-------------------------------------------------------------------------
    // Static shortcut methods to SecurityContext singleton method
    //-------------------------------------------------------------------------

    /**
     * A shortcut to
     * <code>{@link #getContext() SecurityContext.getContext().}{@link #getPrincipal()}</code>.
     * @return the login name or <code>null</code> if no user is
     *         authenticated or identified.
     */
    public static String getUserPrincipal() {
        return getContext().getPrincipal();
    }

    /**
     * A shortcut to
     * <code>{@link #getContext() SecurityContext.getContext().}{@link #isAuthenticated()}</code>.
     * <p>
     * Note that this method may return <code>false</code> while a
     * {@link #getPrincipal() user principal} is available in case a
     * Remember-Me protocol is being used.</p>
     *
     * @return <code>true</code> if the user performing the request has
     *         successfully authenticated; <code>false</code> otherwise.
     */
    public static boolean isUserAuthenticated() {
        return getContext().isAuthenticated();
    }

    /**
     * A shortcut to
     * <code>{@link #getContext() SecurityContext.getContext().}{@link #hasRole(String)}</code>.
     * @param  role   the role name.
     * @return <code>true</code> if the user performing the request has
     *         the specified role; <code>false</code> otherwise.
     */
    public static boolean isUserInRole(String role) {
        return getContext().hasRole(role);
    }

    //-------------------------------------------------------------------------
    // Factory methods
    //-------------------------------------------------------------------------

    /**
     * Returns an instance of the configured
     * <code>SecurityContext</code>.
     * <p>
     * This method uses the {@link ServiceLoader JAR service provider}
     * mechanism to discover the SecurityContext implementation. The
     * implementation selected is the first listed in the first file
     * named
     * <code>META-INF/services/org.datalift.fwk.security.SecurityContext</code>
     * found in the classpath.</p>
     *
     * @return an instance of <code>SecurityContext</code>.
     */
    public static SecurityContext getContext() {
        if (instance == null) {
            Iterator<SecurityContext> i =
                        ServiceLoader.load(SecurityContext.class).iterator();
            if (i.hasNext()) {
                instance = i.next();
                log.trace("Using security context implementation: {}",
                          instance.getClass().getSimpleName());
            }
            else {
                throw new IllegalStateException(
                    "No provider found for " + SecurityContext.class.getName());
            }
        }
        return instance;
    }
}

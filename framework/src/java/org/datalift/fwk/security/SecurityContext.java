package org.datalift.fwk.security;

import java.util.Iterator;
import java.util.ServiceLoader;


/**
 * An factory class that abstracts the access to the user's security
 * information.
 *
 * @author lbihanic
 */
public abstract class SecurityContext
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /**
     * Cached instance of the configured <code>SecurityContext</code>
     * implementation.
     */
    private static SecurityContext instance = null;

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
     *         authenticated.
     */
    abstract public String getPrincipal();

    /**
     * Returns whether the user performing the request is authenticated.
     * @return <code>true</code> if the user performing the request has
     *         successfully authenticated; <code>false</code> otherwise.
     */
    public boolean isAuthenticated() {
        return (this.getPrincipal() != null);
    }

    /**
     * Returns whether to current user is authenticated and has the
     * specified role.
     * @param  role   the role name.
     * @return <code>true</code> if the user performing the request has
     *         successfully authenticated and has the specified role;
     *         <code>false</code> otherwise.
     */
    abstract public boolean hasRole(String role);

    //-------------------------------------------------------------------------
    // Static shortcut methods to SecurityContext singleton method
    //-------------------------------------------------------------------------

    /**
     * A shortcut to
     * <code>{@link #getContext() SecurityContext.getContext().}{@link #getPrincipal()}</code>.
     * @return the login name or <code>null</code> if no user is
     *         authenticated.
     */
    public static String getUserPrincipal() {
        return getContext().getPrincipal();
    }

    /**
     * A shortcut to
     * <code>{@link #getContext() SecurityContext.getContext().}{@link #isAuthenticated()}</code>.
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
     *         successfully authenticated and has the specified role;
     *         <code>false</code> otherwise.
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
            }
            else {
                throw new IllegalStateException(
                    "No provider found for " + SecurityContext.class.getName());
            }
        }
        return instance;
    }
}

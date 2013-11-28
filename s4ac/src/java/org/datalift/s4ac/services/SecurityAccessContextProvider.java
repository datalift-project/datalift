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

package org.datalift.s4ac.services;


import java.net.URI;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.sparql.AccessContextProvider;
import org.datalift.fwk.util.StringUtils;


/**
 * {@link AccessContextProvider} that populates the context with
 * user-related information extracted from the Datalift
 * {@link SecurityContext security context}.
 *
 * @author lbihanic
 */
public class SecurityAccessContextProvider extends BaseModule
                                           implements AccessContextProvider
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The format to build the user URI context. */
    public final static String USER_URI_FORMAT_PROPERTY    =
                                        "sparql.security.user.uri";
    /** The security roles to be checked for addition to the access context. */
    public final static String ROLE_LIST_PROPERTY   =
                                        "sparql.security.user.roles";
    /** The format to build the role URI context. */
    public final static String ROLE_URI_FORMAT_PROPERTY    =
                                        "sparql.security.role.uri";

    /** Name of the context variable for the user login. */
    public final static String USER_LOGIN_CONTEXT = "principal";
    /** Name of the context variable for the user URI. */
    public final static String USER_URI_CONTEXT = "user";
    /** Name of the context variable for the user role names. */
    public final static String ROLE_NAME_CONTEXT = "rolename";
    /** Name of the context variable for the user role URIs. */
    public final static String ROLE_URI_CONTEXT = "role";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The message format to build the user URI. */
    private MessageFormat userUriFormat = null;
    /** The list of roles to check. */
    private Collection<String> userRoles = new LinkedList<String>();
    /** The message format to build the user URI. */
    private MessageFormat roleUriFormat = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public SecurityAccessContextProvider() {
        super(SecurityAccessContextProvider.class.getSimpleName());
    }

    //-------------------------------------------------------------------------
    // BaseModule contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        // Retrieve the format for the user URI context.
        String s = configuration.getProperty(USER_URI_FORMAT_PROPERTY);
        if (StringUtils.isSet(s)) {
            this.userUriFormat = new MessageFormat(s);
        }
        // Retrieve the list of user roles to check.
        s = configuration.getProperty(ROLE_LIST_PROPERTY);
        if (StringUtils.isSet(s)) {
            this.userRoles = Arrays.asList(s.split("\\s*,\\s*"));
        }
        // Retrieve the format for the role URIs context.
        s = configuration.getProperty(ROLE_URI_FORMAT_PROPERTY);
        if (StringUtils.isSet(s)) {
            this.roleUriFormat = new MessageFormat(s);
        }
    }

    //-------------------------------------------------------------------------
    // AccessContextProvider contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void populateContext(Map<String,Object> context) {
        try {
            SecurityContext securityCtx = SecurityContext.getContext();
            String user = securityCtx.getPrincipal();
            if (user != null) {
                // Append user context.
                context.put(USER_LOGIN_CONTEXT, user);
                if (this.userUriFormat != null) {
                    context.put(USER_URI_CONTEXT, URI.create(
                            this.userUriFormat.format(new Object[] { user })));
                }
                // Append user role context.
                if (! this.userRoles.isEmpty()) {
                    Collection<String> roles = new LinkedList<String>();
                    Collection<URI> roleUris = null;
                    if (this.roleUriFormat != null) {
                        roleUris = new LinkedList<URI>();
                    }
                    for (String role : this.userRoles) {
                        if (securityCtx.hasRole(role)) {
                            roles.add(role);
                            if (roleUris != null) {
                                roleUris.add(URI.create(
                                            this.roleUriFormat.format(
                                                    new Object[] { role })));
                            }
                        }
                    }
                    if (! roles.isEmpty()) {
                        context.put(ROLE_NAME_CONTEXT, roles);
                    }
                    if ((roleUris != null) && (! roleUris.isEmpty())) {
                        context.put(ROLE_URI_CONTEXT, roleUris);
                    }
                }
            }
        }
        catch (Exception e) { /* Ignore... */ }
    }
}

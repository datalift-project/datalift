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

package org.datalift.core.security.shiro;


import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.security.SecurityContext;


/**
 * An implementation of {@link SecurityContext} that relies on Apache
 * <a href="http://shiro.apache.org/">Shiro</a> security framework.
 *
 * @author lbihanic
 */
public class ShiroSecurityContext extends SecurityContext
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // SecurityContext contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getPrincipal() {
        String principal = null;
        Subject s = this.getSubject();
        if (s != null) {
            Object o = s.getPrincipal();
            if (o != null) {
                principal = String.valueOf(o);
            }
        }
        return principal;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAuthenticated() {
        boolean authenticated = false;
        Subject s = this.getSubject();
        if (s != null) {
            authenticated = s.isAuthenticated();
        }
        return authenticated;
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasRole(String role) {
        boolean hasRole = false;
        Subject s = this.getSubject();
        if (s != null) {
            hasRole = s.hasRole(role);
            if (! hasRole) {
                hasRole = s.isPermitted(role);
            }
        }
        if (log.isDebugEnabled()) {
            log.debug("{} has role {}: {}", s.getPrincipal(), role,
                                                    Boolean.valueOf(hasRole));
        }
        return hasRole;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private Subject getSubject() {
        Subject s = null;
        try {
            s = SecurityUtils.getSubject();
        }
        catch (Exception e) {
            // No security context available. => Ignore...
        }
        return s;
    }
}

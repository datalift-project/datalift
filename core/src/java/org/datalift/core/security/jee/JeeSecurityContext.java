package org.datalift.core.security.jee;


import javax.servlet.http.HttpServletRequest;

import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.util.web.RequestContext;


/**
 * An implementation of {@link SecurityContext} that relies on JEE
 * security data provided by the {@link RequestContext being-processed}
 * request: {@link HttpServletRequest#getUserPrincipal() principal}
 * and {@link HttpServletRequest#isUserInRole(String) roles}.
 *
 * @author lbihanic
 */
public class JeeSecurityContext extends SecurityContext
{
    //-------------------------------------------------------------------------
    // SecurityContext contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getPrincipal() {
        String principal = null;
        HttpServletRequest req = this.getRequestContext();
        if ((req != null) && (req.getUserPrincipal() != null)) {
            principal = req.getUserPrincipal().getName();
        }
        return this.checkSurrogateUser(principal);
    }

    /** {@inheritDoc} */
    @Override
    public boolean hasRole(String role) {
        boolean hasRole = false;
        HttpServletRequest req = this.getRequestContext();
        if (req != null) {
            hasRole = req.isUserInRole(role);
        }
        return hasRole;
    }
}

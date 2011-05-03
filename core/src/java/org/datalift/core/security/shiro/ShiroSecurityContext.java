package org.datalift.core.security.shiro;


import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

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
    public boolean hasRole(String role) {
        Subject s = this.getSubject();
        return (s != null)? s.hasRole(role): false;
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

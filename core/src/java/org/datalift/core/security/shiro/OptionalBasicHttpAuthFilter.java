package org.datalift.core.security.shiro;


import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.shiro.web.filter.authc.BasicHttpAuthenticationFilter;

import org.datalift.fwk.log.Logger;


/**
 * An extension to Shiro's BasicHttpAuthenticationFilter to support
 * optional HTTP Basic authentication, i.e. cases where user credentials
 * shall be taken into account when present but not prevent user access
 * if absent.
 *
 * @author lbihanic
 */
public class OptionalBasicHttpAuthFilter extends BasicHttpAuthenticationFilter
{
    private final static Logger log = Logger.getLogger();

    /**
     * {@inheritDoc}
     * @return <code>true</code> always to grant access to anonymous
     *         users.
     */
    @Override
    protected boolean onAccessDenied(ServletRequest request,
                                    ServletResponse response) throws Exception {
        super.onAccessDenied(request, response);
        // Force return code to true to grant user access.
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation is empty: no challenge shall be set when
     * not authentication header is present as anonymous accesses are
     * allowed.</p>
     */
    @Override
    protected boolean sendChallenge(ServletRequest request,
                                    ServletResponse response) {
        // Skip sending challenge to accept anonymous access.
        log.debug("User not authenticated. Skipping sending challenge response");
        return false;
    }
}

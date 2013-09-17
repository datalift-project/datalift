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

package org.datalift.fwk;


import java.util.Collection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.datalift.fwk.util.StringUtils;


/**
 * A default implementation of the {@link Module} interface to act
 * as a superclass for actual application modules. Most method
 * implementations are empty.
 *
 * @author lbihanic
 */
public abstract class BaseModule implements Module
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The module name. */
    private final String name;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new module.
     * <p>
     * This constructor is a shortcut to
     * <code>new BaseModule(name, false)</code>.
     * @param  name   the module name.
     *
     * @throws IllegalArgumentException if <code>name</code> is
     *         <code>null</code>.
     */
    protected BaseModule(String name) {
        if ((name == null) || (name.length() == 0)) {
            throw new IllegalArgumentException("name");
        }
        this.name = name;
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * <p>This implementation returns the module name provided as
     * constructor argument.</p>
     */
    @Override
    public final String getName() {
        return this.name;
    }

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        // NOP
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown(Configuration configuration) {
        // NOP
    }

    /**
     * {@inheritDoc}
     * <p>This implementation returns <code>false</code>
     * (authentication required).</p>
     */
    @Override
    public boolean allowsAnonymousAccess() {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>This implementation returns <code>null</code> (no
     * roles required).</p>
     */
    @Override
    public Collection<String> getAuthorizedRoles() {
        return null;
    }

    //-------------------------------------------------------------------------
    // BaseModule contract support
    //-------------------------------------------------------------------------

    /**
     * Helper method to build a JAX-RS web service error response
     * @param  status    the {@link Status HTTP status code}.
     * @param  message   can optional error message to return
     *                   to the service user.
     *
     * @throws WebApplicationException always.
     */
    public final void sendError(int status, String message)
                                            throws WebApplicationException {
        ResponseBuilder r = Response.status(status);
        if (StringUtils.isSet(message)) {
            r.entity(message).type(MediaTypes.TEXT_PLAIN);
        }
        throw new WebApplicationException(r.build());
    }

    /**
     * Helper method to build a JAX-RS web service error response
     * @param  status    the {@link Status HTTP status code}.
     * @param  message   an optional error message to return
     *                   to the service user.
     *
     * @throws WebApplicationException always.
     */
    public final void sendError(Status status, String message)
                                            throws WebApplicationException {
        this.sendError(status.getStatusCode(), message);
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getName();
    }
}

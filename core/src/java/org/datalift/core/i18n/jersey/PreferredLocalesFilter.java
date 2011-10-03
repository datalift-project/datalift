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

package org.datalift.core.i18n.jersey;


import com.sun.jersey.spi.container.ContainerRequest;
import com.sun.jersey.spi.container.ContainerRequestFilter;
import com.sun.jersey.spi.container.ContainerResponse;
import com.sun.jersey.spi.container.ContainerResponseFilter;

import org.datalift.fwk.i18n.PreferredLocales;


/**
 * A Jersey filter to extract the user's preferred locales from the
 * HTTP request <code>Accept-Language</code> header and make them
 * available through the {@link PreferredLocales} class.
 * <p>
 * When an application is deployed as a Servlet or Filter this Jersey
 * filter can be registered using the following initialization
 * parameters:</p>
 * <blockquote><pre>
 *   &lt;init-param&gt;
 *     &lt;param-name&gt;com.sun.jersey.spi.container.ContainerRequestFilters&lt;/param-name&gt;
 *     &lt;param-value&gt;org.datalift.core.i18n.jersey.PreferredLocalesFilter&lt;/param-value&gt;
 *   &lt;/init-param&gt
 *   &lt;init-param&gt
 *     &lt;param-name&gt;com.sun.jersey.spi.container.ContainerResponseFilters&lt;/param-name&gt;
 *     &lt;param-value&gt;org.datalift.core.i18n.jersey.PreferredLocalesFilter&lt;/param-value&gt;
 *   &lt;/init-param&gt;
 * </pre></blockquote>
 *
 * @author lbihanic
 */
public class PreferredLocalesFilter implements ContainerRequestFilter,
                                               ContainerResponseFilter
{
    /** {@inheritDoc} */
    @Override
    public ContainerRequest filter(ContainerRequest request) {
        PreferredLocales.set(request.getAcceptableLanguages());
        return request;
    }

    /** {@inheritDoc} */
    @Override
    public ContainerResponse filter(ContainerRequest request,
                                    ContainerResponse response) {
        PreferredLocales.reset();
        return response;
    }
}

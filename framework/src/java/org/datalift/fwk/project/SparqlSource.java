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

package org.datalift.fwk.project;


/**
 * A source object reading data from a SPARQL endpoint by executing a
 * user-provided SPARQL query.
 *
 * @author hdevos
 */
public interface SparqlSource extends RdfSource
{
    /**
     * Returns the URL of the SPARQL endpoint this source reads data
     * from.
     * @return the URL of the SPARQL endpoint.
     */
    public String getEndpointUrl();

    /**
     * Sets the URL of the SPARQL endpoint this source reads data from.
     * @param  url   the URL of the SPARQL endpoint.
     */
    public void setEndpointUrl(String url);

    /**
     * Returns the SPARQL query (CONSTRUCT or DESCRIBE) this source
     * shall execute to retrieve data.
     * @return the SPARQL query.
     */
    public String getQuery();

    /**
     * Sets the SPARQL query (CONSTRUCT or DESCRIBE) this source
     * shall execute to retrieve data.
     * @param  query   the SPARQL query.
     */
    public void setQuery(String query);

    /**
     * Returns the default graph URI to insert in the HTTP request to
     * the SPARQL endpoint. Only one default graph URI is supported for
     * the time being.
     * @return the default graph URI or <code>null</code> if none was
     *         specified.
     */
    public String getDefaultGraphUri();

    /**
     * Sets the (single) default graph URI to insert in the HTTP request to
     * the SPARQL endpoint. Only one default graph URI is supported for
     * the time being.
     * @param  uri   the default graph URI or <code>null</code> if no
     *               value shall be set for the
     *               <code>default-graph-uri</code> HTTP query
     *               parameter.
     */
    public void setDefaultGraphUri(String uri);

    /**
     * Returns the user name used to perform HTTP basic authentication
     * when accessing the SPARQL endpoint.
     * @return the user name for HTTP basic authentication.
     */
    public String getUser();

    /**
     * Sets the user name used to perform HTTP basic authentication
     * (see <a href="http://www.ietf.org/rfc/rfc2617.txt">RFC 2617</a>)
     * when accessing the SPARQL endpoint.
     * @param  user   the user name or <code>null</code> if no HTTP
     *                basic authentication shall take place.
     */
    public void setUser(String user);

    /**
     * Sets the password associated to the
     * {@link #getUser() specified user name} for HTTP basic
     * authentication.
     * @return the user password.
     */
    public String getPassword();

    /**
     * Sets the password associated to the
     * {@link #setUser(String) specified user name} for HTTP basic
     * authentication.
     * @param  password   the user password.
     */
    public void setPassword(String password);
}

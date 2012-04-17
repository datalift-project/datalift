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

package org.datalift.sparql;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.datalift.fwk.MediaTypes;

import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * The JAS-RS root resource exposing a SPARQL endpoint that routes
 * queries to the actual repositories over HTTP.
 *
 * @author lbihanic
 */
@Path(AbstractSparqlEndpoint.MODULE_NAME)
public class HttpSparqlEndpoint extends AbstractSparqlEndpoint
{
    //-------------------------------------------------------------------------
    // AbstractSparqlEndpoint contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected ResponseBuilder doExecute(List<String> defaultGraphUris,
                                        List<String> namedGraphUris,
                                        String query, int startOffset,
                                        int endOffset, boolean gridJson,
                                        String format, UriInfo uriInfo,
                                        Request request, String acceptHdr,
                                        Map<String,Object> viewData)
                                        throws IOException, URISyntaxException {
        log.trace("Processing SPARQL query: \"{}\"", query);
        // Forward query to the SPARQL endpoint.
        URL u = new URL(this.getTargetRepository(defaultGraphUris)
                            .getEndpointUrl());
        // Use URI multi-argument constructor to escape query string.
        u = new URI(u.getProtocol(), null,
                    u.getHost(), u.getPort(),
                    u.getPath(), "query=" + query, null).toURL();
        // Build HTTP request.
        HttpURLConnection cnx = (HttpURLConnection)(u.openConnection());
        cnx.setRequestMethod(request.getMethod());
        cnx.setConnectTimeout(2000);    // 2 sec.
        cnx.setReadTimeout(30000);      // 30 sec.
        cnx.setRequestProperty("Accept", isBlank(format)? acceptHdr: format);
        // Force server connection.
        cnx.connect();
        int status = cnx.getResponseCode();
        // Check for error data.
        InputStream data = cnx.getErrorStream();
        if (data == null) {
            // No error data available. => get response data.
            data = cnx.getInputStream();
        }
        // Forward server response to client.
        // Force content type to "application/xml" as Sesame does not
        // provide any valid (RDF/XML, SPARQL Results) content type
        // in the SPARQL endpoint HTTP responses.
        return Response.status(status)
                       .type(MediaTypes.APPLICATION_XML)
                       .entity(data);
    }
}

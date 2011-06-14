package org.datalift.sparql;


import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.HttpURLConnection;
import java.util.List;

import javax.ws.rs.Path;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.datalift.fwk.MediaTypes;


/**
 * The JAS-RS root resource exposing a SPARQL endpoint that routes
 * queries to the actual repositories over HTTP.
 *
 * @author lbihanic
 */
@Path("/sparql")
public class HttpSparqlEndpoint extends AbstractSparqlEndpoint
{
    //-------------------------------------------------------------------------
    // AbstractSparqlEndpoint contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected ResponseBuilder doExecute(List<String> defaultGraphUris,
                                        List<String> namedGraphUris,
                                        String query,
                                        int startOffset, int endOffset,
                                        boolean gridJson, UriInfo uriInfo,
                                        Request request, String acceptHdr)
                                        throws IOException, URISyntaxException {
        log.trace("Processing SPARQL query: \"{}\"", query);
        // Forward query to the SPARQL endpoint.
        URL u = this.getTargetRepository(defaultGraphUris).url;
        // Use URI multi-argument constructor to escape query string.
        u = new URI(u.getProtocol(), null,
                    u.getHost(), u.getPort(),
                    u.getPath(), "query=" + query, null).toURL();
        // Build HTTP request.
        HttpURLConnection cnx = (HttpURLConnection)(u.openConnection());
        cnx.setRequestMethod(request.getMethod());
        cnx.setConnectTimeout(2000);    // 2 sec.
        cnx.setReadTimeout(30000);      // 30 sec.
        cnx.setRequestProperty("Accept", acceptHdr);
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

	@Override
	public void postInit() {
		// TODO Auto-generated method stub
		
	}
}

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

package org.datalift.core.project;


import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import javax.persistence.Entity;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.core.MediaType;

import static javax.ws.rs.core.HttpHeaders.*;

import org.openrdf.model.Statement;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParser.DatatypeHandling;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.core.TechnicalException;
import org.datalift.core.rdf.BoundedAsyncRdfParser;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.util.Base64;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.web.Charsets;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.*;


/**
 * Default implementation of the {@link SparqlSource} interface.
 *
 * @author hdevos
 */
@Entity
@RdfsClass("datalift:sparqlSource")
public class SparqlSourceImpl extends CachingSourceImpl implements SparqlSource
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The scheme name for HTTP Basic authentication. */
    public final static String BASIC_AUTH_SCHEME = "Basic";
    /** The character encoding for HTTP Basic authentication: ISO-8859-1. */
    public final static String BASIC_AUTH_ENCODING = "ISO-8859-1";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:request")
    private String query;
    @RdfProperty("datalift:default-graph-uri")
    private String defaultGraphUri;
    @RdfProperty("datalift:user")
    private String user;
    @RdfProperty("datalift:password")
    private String password;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new SPARQL source.
     */
    protected SparqlSourceImpl() {
        super(SourceType.SparqlSource);
    }

    /**
     * Creates a new SPARQL source with the specified identifier and
     * owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    protected SparqlSourceImpl(String uri, Project project) {
        super(SourceType.SparqlSource, uri, project);
    }

    //-------------------------------------------------------------------------
    // SparqlSource contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @return <code>null</code> always as SPARQL-originating data do
     *         not share any common base URI.
     */
    @Override
    public String getBaseUri() {
        return null;
    }

    /** {@inheritDoc} */
    @Override
    public String getEndpointUrl() {
        return this.getSourceUrl();
    }

    /** {@inheritDoc} */
    @Override
    public void setEndpointUrl(String url) {
        this.setSourceUrl(url);
        // Invalidate cache to force data reload.
        this.invalidateCache();
    }

    /** {@inheritDoc} */
    @Override
    public String getQuery() {
        return this.query;
    }

    /** {@inheritDoc} */
    @Override
    public void setQuery(String query) {
        if ((query == null) ||
            (! CONSTRUCT_VALIDATION_PATTERN.matcher(query).find())) {
            throw new IllegalArgumentException("query");
        }
        this.query = query;
        // Invalidate cache to force data reload.
        this.invalidateCache();
    }

    /** {@inheritDoc} */
    @Override
    public String getDefaultGraphUri() {
        return this.defaultGraphUri;
    }

    /** {@inheritDoc} */
    @Override
    public void setDefaultGraphUri(String uri) {
        this.defaultGraphUri = uri;
        // Invalidate cache to force data reload.
        this.invalidateCache();
    }

    /** {@inheritDoc} */
    @Override
    public String getUser() {
        return this.user;
    }

    /** {@inheritDoc} */
    @Override
    public void setUser(String user) {
        this.user = user;
    }

    /** {@inheritDoc} */
    @Override
    public String getPassword() {
        return this.password;
    }

    /** {@inheritDoc} */
    @Override
    public void setPassword(String password) {
        this.password = password;
    }

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Statement> iterator() {
        try {
            ParserConfig config = new ParserConfig(
                                        true,       // Assume data are valid.
                                        false,      // Report all errors.
                                        false,      // Don't preserve BNode ids.
                                        DatatypeHandling.VERIFY);
            RDFParser parser = RdfUtils.newRdfParser(APPLICATION_RDF_XML);
            parser.setParserConfig(config);
            return BoundedAsyncRdfParser.parse(this.getInputStream(),
                                               parser, this.getEndpointUrl(),
                                               Env.getRdfBatchSize());
        }
        catch (IOException e) {
            throw new TechnicalException(e.getMessage(), e);
        }
    }

    //-------------------------------------------------------------------------
    // CachingSourceImpl contract support
    //-------------------------------------------------------------------------

    @Override
    protected void reloadCache() throws IOException {
        // Build HTTP SPARQL request.
        URL u = null;
        String query = null;
        HttpURLConnection cnx = null;
        log.debug("Reloading cache from {}, query: {}",
                                        this.getEndpointUrl(), this.getQuery());
        try {
            // Build HTTP query string.
            StringBuilder buf = new StringBuilder(2048);
            // Extract query data from endpoint URL, if any.
            u = new URL(this.getEndpointUrl());
            String q = u.getQuery();
            if (isSet(q)) {
                buf.append(u.getQuery()).append('&');
                u = new URI(u.getProtocol(), null,
                            u.getHost(), u.getPort(), u.getPath(),
                            null, null).toURL();
            }
            if (! isBlank(this.getDefaultGraphUri())) {
                buf.append("default-graph-uri=")
                     .append(this.getDefaultGraphUri())
                     .append('&');
            }
            query = buf.append("query=").append(this.getQuery()).toString();
            // Use HTTP GET or POST depending on query length: use HTTP POST
            // to bypass URL length limitations of GET method.
            if ((u.toString() + '?' + query).length() > 2048) {
                // Use HTTP POST.
                cnx = (HttpURLConnection)(u.openConnection());
                cnx.setRequestMethod(HttpMethod.POST);
            }
            else {
                // HTTP GET will do...
                // Use URI multi-argument constructor to escape query string.
                u = new URI(u.getProtocol(), null,
                            u.getHost(), u.getPort(), u.getPath(),
                            query, null).toURL();
                cnx = (HttpURLConnection)(u.openConnection());
                cnx.setRequestMethod(HttpMethod.GET);
                query = null;           // Mark query as consumed.
            }
        }
        catch (Exception e) {
            throw new IOException(
                    new TechnicalException("invalid.endpoint.url", e,
                                           this.getEndpointUrl()));
        }
        cnx.setRequestProperty(ACCEPT, APPLICATION_RDF_XML);
        if (! isBlank(this.getUser())) {
            String token = this.getUser() + ':' +
                    ((isBlank(this.getPassword()))? "" : this.getPassword());
            cnx.setRequestProperty(AUTHORIZATION,
                    BASIC_AUTH_SCHEME + ' ' +
                    Base64.encode(token.getBytes(BASIC_AUTH_ENCODING), null));
        }
        cnx.setUseCaches(false);
        cnx.setDoInput(true);
        log.debug("Connecting to: {}", u);
        // Append query string in case of HTTP POST.
        if (query != null) {
            cnx.setDoOutput(true);
            OutputStream out = cnx.getOutputStream();
            out.write(query.getBytes(Charsets.UTF_8));
            out.flush();
            out.close();
        }
        // Force server connection.
        cnx.connect();
        // Check for error data.
        InputStream in = cnx.getErrorStream();
        if (in == null) {
            // No error data found. => save response data to cache.
            this.save(cnx.getInputStream());
            log.debug("Query results saved to {}", this.getCacheFile());
        }
        else {
            char[] buf = new char[1024];
            int l = 0;
            Reader r = null;
            try {
                String cs = getCharset(this.parseContentType(
                                                        cnx.getContentType()));
                in = new BufferedInputStream(in, this.getBufferSize());
                r = (cs == null)? new InputStreamReader(in):
                                  new InputStreamReader(in, cs);
                l = r.read(buf);
            }
            catch (Exception e) { /* Ignore... */ }
            finally {
                try { r.close(); } catch (Exception e) { /* Ignore... */ }
            }
            throw new IOException(
                    new TechnicalException("endpoint.access.error",
                                        this.getEndpointUrl(),
                                        Integer.valueOf(cnx.getResponseCode()),
                                        new String(buf, 0, l)));
        }
    }

    /** {@inheritDoc} */
    @Override
    protected String getCacheFileExtension() {
        return "xml";           // Saving SPARQL results as RDF+XML.
    }

    /** {@inheritDoc} */
    @Override
    protected StringBuilder toString(StringBuilder b) {
        b.append(this.getEndpointUrl());
        if (isSet(this.getDefaultGraphUri())) {
            b.append(", ").append(this.getDefaultGraphUri());
        }
        b.append(", \"").append(this.getQuery()).append('"');
        return super.toString(b);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private void save(InputStream in) throws IOException {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(this.getCacheFile());
            byte[] buf = new byte[8192];
            int l;
            while ((l = in.read(buf)) != -1) {
                fos.write(buf, 0, l);
            }
        }
        finally {
            if (fos != null) {
                try { fos.close(); } catch (Exception e) { /* Ignore... */ }
            }
            try { in.close();  } catch (Exception e) { /* Ignore... */ }
        }
    }

    /**
     * Parses the value of the Content-Type HTTP header to extract
     * the content type (MIME type) and character encoding information.
     * @param  contentType   the value of the HTTP Content-Type header.
     *
     * @return the content type and character encoding as an array of
     *         strings.
     */
    private MediaType parseContentType(String contentType) {
        return MediaType.valueOf(
                    isSet(contentType)? contentType: APPLICATION_OCTET_STREAM);
    }
}

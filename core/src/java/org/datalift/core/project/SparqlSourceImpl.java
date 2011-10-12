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


import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import javax.persistence.Entity;
import javax.ws.rs.HttpMethod;

import org.openrdf.model.Statement;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import static javax.ws.rs.core.HttpHeaders.*;

import org.datalift.core.TechnicalException;
import org.datalift.core.rdf.BoundedAsyncRdfParser;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.util.CloseableIterator;

import static org.datalift.fwk.MediaTypes.*;


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
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:request")
    private String query;

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

    /** {@inheritDoc} */
    @Override
    public String getEndpointUrl() {
        return this.getSourceUrl();
    }

    /** {@inheritDoc} */
    @Override
    public void setEndpointUrl(String url) {
        this.setSourceUrl(url);
    }

    /** {@inheritDoc} */
    @Override
    public String getQuery() {
        return this.query;
    }

    /** {@inheritDoc} */
    @Override
    public void setQuery(String query) {
        this.query = query;
    }

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Statement> iterator() {
        try {
            return BoundedAsyncRdfParser.parse(this.getInputStream(),
                                    APPLICATION_RDF_XML, this.getEndpointUrl());
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
        try {
            u = new URL(this.getEndpointUrl());
            // Use URI multi-argument constructor to escape query string.
            u = new URI(u.getProtocol(), null,
                        u.getHost(), u.getPort(), u.getPath(),
                        "query=" + this.getQuery(), null).toURL();
        }
        catch (Exception e) {
            throw new IOException(
                    new TechnicalException("invalid.endpoint.url", e,
                                           this.getEndpointUrl()));
        }
        HttpURLConnection cnx = (HttpURLConnection)(u.openConnection());
        cnx.setRequestProperty(ACCEPT, APPLICATION_RDF_XML);
        cnx.setRequestProperty(CACHE_CONTROL, "no-cache");
        // Set HTTP method. For large requests, use HTTP POST
        // to bypass URL length limitations of GET method.
        cnx.setRequestMethod((u.toString().length() > 2048)?
                                            HttpMethod.POST: HttpMethod.GET);
        // Force server connection.
        cnx.connect();
        // Check for error data.
        InputStream in = cnx.getErrorStream();
        if (in == null) {
            // No error data found. => save response data to cache.
            this.save(cnx.getInputStream());
        }
        else {
            char[] buf = new char[1024];
            int l = 0;
            Reader r = null;
            try {
                String[] contentType = this.parseContentType(
                                                        cnx.getContentType());
                r = (contentType[1] == null)?
                                    new InputStreamReader(in):
                                    new InputStreamReader(in, contentType[1]);
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

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private void save(InputStream in) throws IOException {
        FileOutputStream fos = new FileOutputStream(this.getCacheFile());
        try {
            byte[] buf = new byte[8192];
            int l;
            while ((l = in.read(buf)) != -1) {
                fos.write(buf, 0, l);
            }
        }
        finally {
            try { fos.close(); } catch (Exception e) { /* Ignore... */ }
            try { in.close();  } catch (Exception e) { /* Ignore... */ }
        }
    }

    private String[] parseContentType(String contentType) {
        String[] elts = new String[2];

        final String CHARSET_TAG = "charset=";
        if ((contentType != null) && (contentType.length() != 0)) {
            String[] s = contentType.split("\\s;\\s");
            elts[0] = s[0];
            if ((s.length > 1) && (s[1].startsWith(CHARSET_TAG))) {
                elts[1] = s[1].substring(CHARSET_TAG.length());
            }
        }
        return elts;
    }
}

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


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.persistence.Entity;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.StringUtils;


/**
 * Default implementation of the {@link SparqlSource} interface.
 *
 * @author hdevos
 */
@Entity
@RdfsClass("datalift:sparqlSource")
public class SparqlSourceImpl extends CachingSourceImpl implements SparqlSource {
    @RdfProperty("datalift:connectionUrl")
    private String connectionUrl;
    @RdfProperty("datalift:request")
    private String request;

    private transient Collection<Statement> content = null;

    protected SparqlSourceImpl() {
        super(SourceType.SparqlSource);
    }

    protected SparqlSourceImpl(String uri) {
        super(SourceType.SparqlSource, uri);
    }

    /** {@inheritDoc} */
    @Override
    public String getConnectionUrl() {
        return connectionUrl;
    }

    /** {@inheritDoc} */
    @Override
    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    /** {@inheritDoc} */
    @Override
    public String getRequest() {
        return request;
    }

    /** {@inheritDoc} */
    @Override
    public void setRequest(String request) {
        this.request = request;
    }

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration, URI baseUri)
                                                            throws IOException {
        super.init(configuration, baseUri);

        String fileName = this.getClass().getSimpleName() + '-' +
                                        StringUtils.urlify(this.getTitle());
        File cacheFile = new File(configuration.getPrivateStorage(),
                fileName);
        InputStream in = this.getCacheStream(cacheFile);
        if (in == null) {
            URL u = new URL(this.connectionUrl);
               // Use URI multi-argument constructor to escape query string.
               try {
                u = new URI(u.getProtocol(), null,
                               u.getHost(), u.getPort(),
                               u.getPath(), "query=" + this.request, null).toURL();
            } catch (URISyntaxException e) {
                //NOP
            }
            HttpURLConnection cnx = (HttpURLConnection)(u.openConnection());
            cnx.setRequestProperty("Accept", MediaTypes.APPLICATION_RDF_XML);
             // Force server connection.
            cnx.connect();
            // Check for error data.
            in = cnx.getErrorStream();
            if (in == null) {
                // No error data available. => get response data.
                in = cnx.getInputStream();
            }
            else
                throw new TechnicalException("Could not retrieve repository data");
            in = this.saveFile(cacheFile, in);
            cacheFile.deleteOnExit();
        }
        RDFParser parser = RdfUtils.newRdfParser(MediaTypes.APPLICATION_RDF_XML);
        Collection<Statement> l = new LinkedList<Statement>();
        if (parser != null) {
            try {
                StatementCollector collector = new StatementCollector(l);
                parser.setRDFHandler(collector);
                parser.parse(in, (baseUri != null)? baseUri.toString(): "");
            }
            catch (Exception e) {
                throw new IOException("Error while parsing SPARQL source", e);
            }
        }
        this.content = Collections.unmodifiableCollection(l);
    }

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Statement> iterator() {
        if (this.content == null) {
            throw new IllegalStateException("Not initialized");
        }
        final Iterator<Statement> i = this.content.iterator();

        return new CloseableIterator<Statement>() {
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }

            @Override
            public Statement next() {
                return i.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void close() {
                // NOP
            }
        };
    }

    private InputStream saveFile(File file, InputStream in) throws IOException {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            try {
                byte[] buf = new byte[8192];
                int len;
                while ( ( len = in.read(buf)) >= 0 ) {
                    fos.write(buf, 0, len);
                }
            } finally {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new FileInputStream(file);
    }
}

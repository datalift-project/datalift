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

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.util.StringUtils;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.openrdf.model.Statement;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;



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
    public Iterator<Statement> iterator() {
        if (this.content == null) {
            throw new IllegalStateException("Not initialized");
        }
        return this.content.iterator();
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

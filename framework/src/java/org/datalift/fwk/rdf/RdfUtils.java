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

package org.datalift.fwk.rdf;


import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.rdfxml.RDFXMLParser;
import org.openrdf.rio.trig.TriGParser;
import org.openrdf.rio.trix.TriXParser;
import org.openrdf.rio.turtle.TurtleParser;

import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.UriMapper;

import static org.datalift.fwk.MediaTypes.*;


public class RdfUtils
{
    public static void upload(File source, Repository target, URI namedGraph)
                                                        throws RdfException {
        if ((source == null) || (! source.isFile())) {
            throw new IllegalArgumentException("source");
        }
        upload(source, guessRdfTypeFromExtension(source),
                                                    target, namedGraph, null);
    }

    public static void upload(File source, Repository target, URI namedGraph,
                              UriMapper mapper) throws RdfException {
        if ((source == null) || (! source.isFile())) {
            throw new IllegalArgumentException("source");
        }
        upload(source, guessRdfTypeFromExtension(source),
                                                    target, namedGraph, mapper);
    }

    public static void upload(File source, MediaType mimeType,
                              Repository target, URI namedGraph,
                              UriMapper mapper) throws RdfException {
        upload(source, mimeType, target, namedGraph, mapper,
                            (namedGraph != null)? namedGraph.toString(): null);
    }

    public static void upload(File source, MediaType mimeType,
                              Repository target, URI namedGraph,
                              final UriMapper mapper, String baseUri)
                                                        throws RdfException {
        if ((source == null) || (! source.isFile())) {
            throw new IllegalArgumentException("source");
        }
        if (target == null) {
            throw new IllegalArgumentException("target");
        }

        final RepositoryConnection cnx = target.newConnection();
        try {
            RDFParser parser = newRdfParser(mimeType);
            final ValueFactory valueFactory = cnx.getValueFactory();

            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
            // Clear target named graph, if any.
            org.openrdf.model.URI u = null;
            if (namedGraph != null) {
                u = valueFactory.createURI(namedGraph.toString());
                cnx.clear(u);
            }
            final org.openrdf.model.URI ctx = u;
            // Load triples, mapping URIs on the fly.
            parser.setRDFHandler(new RDFHandlerBase()
                {
                    @Override
                    public void handleStatement(Statement stmt) {
                        try {
                            if (mapper != null) {
                                // Map subject and object URIs
                                stmt = valueFactory.createStatement(
                                            (Resource)(mapUri(stmt.getSubject())),
                                            stmt.getPredicate(),
                                            mapUri(stmt.getObject()));
                            }
                            cnx.add(stmt, ctx);
                        }
                        catch (RepositoryException e) {
                            throw new RuntimeException(
                                            "RDF triple insertion failed", e);
                        }
                    }

                    private Value mapUri(Value v) {
                        if (v instanceof org.openrdf.model.URI) {
                            try {
                                return valueFactory.createURI(
                                        mapper.map(new URI(v.stringValue()))
                                              .toString());
                            }
                            catch (URISyntaxException e) {
                                // Should never happen.
                                throw new RuntimeException(e);
                            }
                        }
                        else {
                            return v;
                        }
                    }
                });
            parser.parse(new FileInputStream(source),
                         (baseUri != null)? baseUri.toString(): "");
            cnx.commit();
        }
        catch (Exception e) {
            try {
                cnx.rollback();
            }
            catch (Exception e2) { /* Ignore... */ }

            throw new RdfException("Failed to upload RDF triples from "
                                   + source.getPath(), e);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }

    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target) throws RdfException {
        convert(source, constructQueries, target, null);
    }

    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target, URI namedGraph)
                                                        throws RdfException {
        convert(source, constructQueries, target, namedGraph,
                            (namedGraph != null)? namedGraph.toString(): null);
    }

    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target,
                               URI namedGraph, String baseUri)
                                                        throws RdfException {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        if (target == null) {
            throw new IllegalArgumentException("target");
        }
        if (constructQueries == null) {
            throw new IllegalArgumentException("constructQueries");
        }
        if (baseUri == null) {
            baseUri = "";
        }

        RepositoryConnection in  = null;
        RepositoryConnection out = null;
        String query = null;
        try {
            in  = source.newConnection();
            out = target.newConnection();

            final ValueFactory valueFactory = out.getValueFactory();
            // Prevent transaction commit for each triple inserted.
            out.setAutoCommit(false);
            // Clear target named graph, if any.
            org.openrdf.model.URI u = null;
            if (namedGraph != null) {
                u = valueFactory.createURI(namedGraph.toString());
                out.clear(u);
            }

            for (String s : constructQueries) {
                query = s;
                GraphQuery q = in.prepareGraphQuery(QueryLanguage.SPARQL,
                                                    query, baseUri);
                out.add(q.evaluate(), u);
            }
            out.commit();
            query = null;       // No query in error.
        }
        catch (Exception e) {
            try {
                out.rollback();
            }
            catch (Exception e2) { /* Ignore... */ }

            throw new RdfException((query != null)? query: e.getMessage(), e);
        }
        finally {
            if (in != null) {
                try { in.close();  } catch (Exception e) { /* Ignore... */ }
            }
            if (in != null) {
                try { out.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
    }

    public static RDFParser newRdfParser(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            throw new IllegalArgumentException("mimeType");
        }
        return newRdfParser(parseMimeType(mimeType));
    }

    public static RDFParser newRdfParser(MediaType mimeType) {
        if (mimeType == null) {
            throw new IllegalArgumentException("mimeType");
        }
        RDFParser parser = null;
        if ((TEXT_TURTLE_TYPE.equals(mimeType)) ||
            (TEXT_N3_TYPE.equals(mimeType))) {
            parser = new TurtleParser();
        }
        else if (APPLICATION_NTRIPLES_TYPE.equals(mimeType)) {
            parser = new NTriplesParser();
        }
        else if (APPLICATION_TRIG_TYPE.equals(mimeType)) {
            parser = new TriGParser();
        }
        else if (APPLICATION_TRIX_TYPE.equals(mimeType)) {
            parser = new TriXParser();
        }
        else if ((APPLICATION_RDF_XML_TYPE.equals(mimeType)) ||
                 (APPLICATION_XML_TYPE.equals(mimeType))) {
            parser = new RDFXMLParser();
        }
        else {
            throw new IllegalArgumentException(
                            "Unsupported MIME type for RDF data: " + mimeType);
        }
        return parser;
    }

    public static MediaType guessRdfTypeFromExtension(File f) {
        String fileName = f.getName();
        String ext = "";
        int i = fileName.lastIndexOf('.');
        if ((i > 0) && (i < fileName.length() - 1)) {
            ext = fileName.substring(i+1);
        }
        MediaType mimeType = null;
        if (("rdf".equalsIgnoreCase(ext)) || ("rdfs".equalsIgnoreCase(ext)) ||
            ("owl".equalsIgnoreCase(ext)) || ("xml".equalsIgnoreCase(ext))) {
            mimeType = APPLICATION_RDF_XML_TYPE;
        }
        else if ("ttl".equalsIgnoreCase(ext)) {
            mimeType = TEXT_TURTLE_TYPE;
        }
        else if ("n3".equalsIgnoreCase(ext)) {
            mimeType = TEXT_N3_TYPE;
        }
        else if ("nt".equalsIgnoreCase(ext)) {
            mimeType = APPLICATION_NTRIPLES_TYPE;
        }
        else if ("trig".equalsIgnoreCase(ext)) {
            mimeType = APPLICATION_TRIG_TYPE;
        }
        else if ("trix".equalsIgnoreCase(ext)) {
            mimeType = APPLICATION_TRIX_TYPE;
        }
        return mimeType;
    }

    public static MediaType parseMimeType(String mimeType) {
        MediaType mappedType = null;
        if (! StringUtils.isBlank(mimeType)) {
            mimeType = mimeType.trim().toLowerCase();
            if ((TEXT_TURTLE.equals(mimeType)) ||
                (APPLICATION_TURTLE.equals(mimeType))) {
                mappedType = TEXT_TURTLE_TYPE;
            }
            else if ((TEXT_N3.equals(mimeType)) ||
                     (TEXT_RDF_N3.equals(mimeType)) ||
                     (APPLICATION_N3.equals(mimeType))) {
                mappedType = TEXT_N3_TYPE;
            }
            else if ((APPLICATION_RDF_XML.equals(mimeType)) ||
                     (APPLICATION_XML.equals(mimeType))) {
                mappedType = APPLICATION_RDF_XML_TYPE;
            }
            else if (APPLICATION_TRIG.equals(mimeType)) {
                mappedType = APPLICATION_TRIG_TYPE;
            }
            else if (APPLICATION_TRIX.equals(mimeType)) {
                mappedType = APPLICATION_TRIX_TYPE;
            }
            else if (APPLICATION_NTRIPLES.equals(mimeType)) {
                mappedType = APPLICATION_NTRIPLES_TYPE;
            }
        }
        if (mappedType == null) {
            throw new IllegalArgumentException(
                            "Unsupported MIME type for RDF data: " + mimeType);
        }
        return mappedType;
    }
}

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

import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.UriMapper;

import static org.datalift.fwk.MediaTypes.*;


/**
 * A set of utility methods to manipulate RDF triples and files.
 * <p>
 * This class offers methods to:</p>
 * <ul>
 *  <li>Determine the type of RDF data (RDF/XML, Turtle, N3...) a file
 *  contains from its extension:
 *  {@link #guessRdfTypeFromExtension(File)}</li>
 *  <li>Get an RDF parser: {@link #newRdfParser(MediaType)}</li>
 *  <li>Parse RDF files and load the resulting triples into an RDF
 *   store: {@link #upload(File, Repository, URI)}</li>
 *  <li>Apply a set of SPARQL CONTRUCT queries to build a new set of
 *   RDF triples and save them into an RDF store:
 *   {@link #convert(Repository, List, Repository, URI)}</li>
 * </ul>
 *
 * @author lbihanic
 */
public class RdfUtils
{
    /**
     * Parses the specified file, guessing the RDF data type (RDF/XML,
     * Turtle, N3...) from the file extension and loads the resulting
     * triples into the specified RDF store, optionally placing them
     * in the specified named graph.
     * @param  source       the RDF file to load.
     * @param  target       the RDF store to persist triples into.
     * @param  namedGraph   the named graph to use as context for the
     *                      triples or <code>null</code>. If the named
     *                      graph exists, it will be cleared prior
     *                      loading the triples.
     *
     * @throws IllegalArgumentException if no source file or target
     *         RDF store are provided.
     * @throws RdfException if any error occurred parsing the file or
     *         accessing the RDF store.
     *
     * @see    #upload(File, MediaType, Repository, URI, UriMapper, String)
     */
    public static void upload(File source, Repository target, URI namedGraph)
                                                        throws RdfException {
        if ((source == null) || (! source.isFile())) {
            throw new IllegalArgumentException("source");
        }
        upload(source, guessRdfTypeFromExtension(source),
                                                    target, namedGraph, null);
    }

    /**
     * Parses the specified file, guessing the RDF data type (RDF/XML,
     * Turtle, N3...) from the file extension and loads the resulting
     * triples into the specified RDF store, optionally placing them
     * in the specified named graph.
     * @param  source       the RDF file to load.
     * @param  target       the RDF store to persist triples into.
     * @param  namedGraph   the named graph to use as context for the
     *                      triples or <code>null</code>. If the named
     *                      graph exists, it will be cleared prior
     *                      loading the triples.
     * @param  mapper       an optional {@link UriMapper mapper} to
     *                      translate URIs as triples are loaded or
     *                      <code>null</code> if no mapping is needed.
     *
     * @throws IllegalArgumentException if no source file or target
     *         RDF store are provided.
     * @throws RdfException if any error occurred parsing the file or
     *         accessing the RDF store.
     *
     * @see    #upload(File, MediaType, Repository, URI, UriMapper, String)
     */
    public static void upload(File source, Repository target, URI namedGraph,
                              UriMapper mapper) throws RdfException {
        if ((source == null) || (! source.isFile())) {
            throw new IllegalArgumentException("source");
        }
        upload(source, guessRdfTypeFromExtension(source),
                                                    target, namedGraph, mapper);
    }

    /**
     * Parses the specified file and loads the resulting triples
     * into the specified RDF store, optionally placing them in the
     * specified named graph.
     * @param  source       the RDF file to load.
     * @param  mimeType     the type of RDF data present in the file.
     * @param  target       the RDF store to persist triples into.
     * @param  namedGraph   the named graph to use as context for the
     *                      triples or <code>null</code>. If the named
     *                      graph exists, it will be cleared prior
     *                      loading the triples.
     * @param  mapper       an optional {@link UriMapper mapper} to
     *                      translate URIs as triples are loaded or
     *                      <code>null</code> if no mapping is needed.
     *
     * @throws IllegalArgumentException if no source file, MIME type
     *         or target RDF store are provided.
     * @throws RdfException if any error occurred parsing the file or
     *         accessing the RDF store.
     *
     * @see    #upload(File, MediaType, Repository, URI, UriMapper, String)
     */
    public static void upload(File source, MediaType mimeType,
                              Repository target, URI namedGraph,
                              UriMapper mapper) throws RdfException {
        upload(source, mimeType, target, namedGraph, mapper,
                            (namedGraph != null)? namedGraph.toString(): null);
    }

    /**
     * Parses the specified file and loads the resulting triples
     * into the specified RDF store, optionally placing them in the
     * specified named graph.
     * @param  source       the RDF file to load.
     * @param  mimeType     the type of RDF data present in the file.
     * @param  target       the RDF store to persist triples into.
     * @param  namedGraph   the named graph to use as context for the
     *                      triples or <code>null</code>. If the named
     *                      graph exists, it will be cleared prior
     *                      loading the triples.
     * @param  mapper       an optional {@link UriMapper mapper} to
     *                      translate URIs as triples are loaded or
     *                      <code>null</code> if no mapping is needed.
     * @param  baseUri      the (optional) base URI to resolve relative
     *                      URIs.
     *
     * @throws IllegalArgumentException if no source file, MIME type
     *         or target RDF store are provided.
     * @throws RdfException if any error occurred parsing the file or
     *         accessing the RDF store.
     */
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
        RDFParser parser = newRdfParser(mimeType);

        final RepositoryConnection cnx = target.newConnection();
        try {
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

    /**
     * Applies a set of SPARQL CONTRUCT queries on an RDF store to
     * build a set of RDF triples and save them into another RDF store.
     * @param  source             the RDF store to query.
     * @param  constructQueries   the SPARQL CONSTRUCT queries to
     *                            execute to extract triples.
     * @param  target             the RDF store to persist the
     *                            generated triple to or
     *                            <code>null</code> to persist the
     *                            triples into the source RDF store.
     *
     * @throws IllegalArgumentException if no source RDF store or
     *         CONSTRUCT query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         stores or executing the CONSTRUCT queries.
     *
     * @see    #convert(Repository, List, Repository, URI, String)
     */
    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target) throws RdfException {
        convert(source, constructQueries, target, null);
    }

    /**
     * Applies a set of SPARQL CONTRUCT queries on an RDF store to
     * build a set of RDF triples and save them into another RDF store.
     * @param  source             the RDF store to query.
     * @param  constructQueries   the SPARQL CONSTRUCT queries to
     *                            execute to extract triples.
     * @param  target             the RDF store to persist the
     *                            generated triple to or
     *                            <code>null</code> to persist the
     *                            triples into the source RDF store.
     * @param  namedGraph         the named graph to use as context
     *                            when persisting the generated triples
     *                            or <code>null</code>. If the named
     *                            graph exists, it will be cleared
     *                            prior inserting the new triples.
     *
     * @throws IllegalArgumentException if no source RDF store or
     *         CONSTRUCT query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         stores or executing the CONSTRUCT queries.
     *
     * @see    #convert(Repository, List, Repository, URI, String)
     */
    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target, URI namedGraph)
                                                        throws RdfException {
        convert(source, constructQueries, target, namedGraph,
                            (namedGraph != null)? namedGraph.toString(): null);
    }

    /**
     * Applies a set of SPARQL CONTRUCT queries on an RDF store to
     * build a set of RDF triples and save them into another RDF store.
     * @param  source             the RDF store to query.
     * @param  constructQueries   the SPARQL CONSTRUCT queries to
     *                            execute to extract triples.
     * @param  target             the RDF store to persist the
     *                            generated triple to or
     *                            <code>null</code> to persist the
     *                            triples into the source RDF store.
     * @param  namedGraph         the named graph to use as context
     *                            when persisting the generated triples
     *                            or <code>null</code>. If the named
     *                            graph exists, it will be cleared
     *                            prior inserting the new triples.
     * @param  baseUri            the (optional) base URI to resolve
     *                            relative URIs.
     *
     * @throws IllegalArgumentException if no source RDF store or
     *         CONSTRUCT query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         stores or executing the CONSTRUCT queries.
     */
    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target,
                               URI namedGraph, String baseUri)
                                                        throws RdfException {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        if (target == null) {
            target = source;
        }
        if ((constructQueries == null) || (constructQueries.isEmpty())) {
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

    /**
     * Returns a RDF parser suitable for parsing files of the
     * specified MIME type.
     * @param  mimeType   the MIME type of the data to be parsed.
     *
     * @return a RDF parser.
     * @throws IllegalArgumentException if mimeType is
     *         <code>null</code> or not a valid MIME type for RDF
     *         data.
     *
     * @see    #newRdfParser(MediaType)
     */
    public static RDFParser newRdfParser(String mimeType) {
        if (StringUtils.isBlank(mimeType)) {
            throw new IllegalArgumentException("mimeType");
        }
        return newRdfParser(parseMimeType(mimeType));
    }

    /**
     * Returns a RDF parser suitable for parsing files of the
     * specified MIME type.
     * @param  mimeType   the MIME type of the data to be parsed.
     *
     * @return a RDF parser.
     * @throws IllegalArgumentException if mimeType is
     *         <code>null</code> or not a valid MIME type for RDF
     *         data.
     */
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

    /**
     * Attempts to guess the type of RDF data a file contains by
     * examining the file extension.
     * @param  f   the file the type of which is to determine.
     *
     * @return the MIME type of the file content or <code>null</code>
     *         if the extension was not recognized.
     * @throws IllegalArgumentException if <code>f</code> is
     *         <code>null</code> or is not a regular file.
     */
    public static MediaType guessRdfTypeFromExtension(File f) {
        if ((f == null) || (! f.isFile())) {
            throw new IllegalArgumentException("f");
        }
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

    /**
     * Parses the specified RDF MIME type definition and returns the
     * closest official MIME type, e.g. parsing
     * "<code>text/rdf+n3</.code>" will return
     * <code>{@link MediaTypes#TEXT_N3_TYPE text/n3}</code>.
     * @param  mimeType   the MIME type definition.
     *
     * @return the official MIME type for the specified type.
     * @throws IllegalArgumentException if mimeType is
     *         <code>null</code> or not a valid MIME type for RDF
     *         data.
     */
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

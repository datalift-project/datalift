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
import java.io.InputStream;
import java.net.URI;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.openrdf.model.Statement;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFParser;

import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.CloseableIterable;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.UriMapper;
import org.datalift.fwk.util.io.FileUtils;


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
 *   {@link #convert(Repository, List, Repository, URI, boolean)}</li>
 * </ul>
 *
 * @author lbihanic
 */
public final class RdfUtils
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor, private on purpose. */
    private RdfUtils() {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------------------
    // RdfUtils contract definition
    //-------------------------------------------------------------------------

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
        upload(source, target, namedGraph, null);
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
        upload(source, guessRdfTypeFromExtension(source.getName()),
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
     * @param  source       the RDF data to load.
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
     * @see    #upload(InputStream, MediaType, Repository, URI, UriMapper, String)
     */
    public static void upload(InputStream source, MediaType mimeType,
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
        try {
            upload(FileUtils.getInputStream(source), mimeType, target,
                                                namedGraph, mapper, baseUri);
        }
        catch (Exception e) {
            throw new RdfException("Failed to upload RDF triples from "
                                   + source.getPath(), e);
        }
    }

    /**
     * Parses the specified file and loads the resulting triples
     * into the specified RDF store, optionally placing them in the
     * specified named graph.
     * @param  source       the RDF data to load.
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
    public static void upload(InputStream source, MediaType mimeType,
                              Repository target, URI namedGraph,
                              final UriMapper mapper, String baseUri)
                                                        throws RdfException {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        if (target == null) {
            throw new IllegalArgumentException("target");
        }
        baseUri = getBaseUri(baseUri);

        org.openrdf.model.URI targetGraph = null;
        RepositoryConnection cnx = target.newConnection();
        try {
            // Clear target named graph, if any.
            targetGraph = getGraphUri(namedGraph, cnx, true);

            BatchStatementAppender appender =
                        new BatchStatementAppender(cnx, targetGraph, mapper);
            // Load triples, mapping URIs on the fly.
            // Note: we're using an RDF parser and directly adding statements
            //       because Sesame RepositoryConnection.add(File, ...) is
            //       really not optimized for perf. and high throughput...
            RDFParser parser = newRdfParser(mimeType);
            parser.setRDFHandler(appender);
            parser.parse(source, baseUri);

            log.debug("Inserted {} RDF triples into <{}> in {} seconds",
                      Long.valueOf(appender.getStatementCount()), namedGraph,
                      Double.valueOf(appender.getDuration() / 1000.0));
        }
        catch (Exception e) {
            try {
                // Forget pending triples.
                cnx.rollback();
                // Clear target named graph, if any.
                if (targetGraph != null) {
                    cnx.clear(targetGraph);
                }
            }
            catch (Exception e2) { /* Ignore... */ }

            throw new RdfException(e.getMessage(), e);
        }
        finally {
            // Commit pending data (including graph removal in case of error).
            try { cnx.commit(); } catch (Exception e) { /* Ignore... */ }
            // Close repository connection.
            try { cnx.close();  } catch (Exception e) { /* Ignore... */ }
        }
    }

    /**
     * Insert a collection of triples into the specified RDF store,
     * optionally placing them in the specified named graph.
     * @param  source       a collection of RDF triples, as a Java
     *                      {@link Iterable} object.
     * @param  target       the RDF store to persist triples into.
     * @param  namedGraph   the named graph to use as context for the
     *                      triples or <code>null</code>. If the named
     *                      graph exists, it will be cleared prior
     *                      loading the triples.
     * @param  mapper       an optional {@link UriMapper mapper} to
     *                      translate URIs as triples are loaded or
     *                      <code>null</code> if no mapping is needed.
     *
     * @throws IllegalArgumentException if no source collection or
     *         target RDF store are provided.
     * @throws RdfException if any error occurred reading triples or
     *         accessing the RDF store.
     */
    public static void upload(CloseableIterable<Statement> source,
                              Repository target, URI namedGraph,
                              final UriMapper mapper) throws RdfException {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        if (target == null) {
            throw new IllegalArgumentException("target");
        }
        org.openrdf.model.URI targetGraph = null;
        RepositoryConnection cnx = target.newConnection();
        try {
            // Clear target named graph, if any.
            targetGraph = getGraphUri(namedGraph, cnx, true);
            // Load triples, mapping URIs on the fly.
            BatchStatementAppender appender =
                        new BatchStatementAppender(cnx, targetGraph, mapper);
            appender.startRDF();
            for (Statement stmt : source) {
                appender.handleStatement(stmt);
            }
            appender.endRDF();

            log.debug("Inserted {} RDF triples into <{}> in {} seconds",
                      Long.valueOf(appender.getStatementCount()), namedGraph,
                      Double.valueOf(appender.getDuration() / 1000.0));
        }
        catch (Exception e) {
            try {
                // Forget pending triples.
                cnx.rollback();
                // Clear target named graph, if any.
                if (targetGraph != null) {
                    cnx.clear(targetGraph);
                }
            }
            catch (Exception e2) { /* Ignore... */ }

            throw new RdfException("Failed to upload RDF triples", e);
        }
        finally {
            // Commit pending data (including graph removal in case of error).
            try { cnx.commit(); } catch (Exception e) { /* Ignore... */ }
            // Close repository connection.
            try { cnx.close();  } catch (Exception e) { /* Ignore... */ }
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
     * @param  clearTargetGraph   whether to clear the target name graph
     *                            prior inserting the new triples.
     *
     * @throws IllegalArgumentException if no source RDF store or
     *         CONSTRUCT query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         stores or executing the CONSTRUCT queries.
     *
     * @see    #convert(Repository, List, Repository, URI, String, boolean)
     */
    public static void convert(Repository source, List<String> constructQueries,
                               Repository target, boolean clearTargetGraph)
                                                        throws RdfException {
        convert(source, constructQueries, target, null, clearTargetGraph);
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
     * @param  clearTargetGraph   whether to clear the target name graph
     *                            prior inserting the new triples.
     *
     * @throws IllegalArgumentException if no source RDF store or
     *         CONSTRUCT query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         stores or executing the CONSTRUCT queries.
     *
     * @see    #convert(Repository, List, Repository, URI, String, boolean)
     */
    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target, URI namedGraph,
                               boolean clearTargetGraph) throws RdfException {
        convert(source, constructQueries, target, namedGraph,
                            (namedGraph != null)? namedGraph.toString(): null,
                            clearTargetGraph);
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
     * @param  clearTargetGraph   whether to clear the target name graph
     *                            prior inserting the new triples.
     *
     * @throws IllegalArgumentException if no source RDF store or
     *         CONSTRUCT query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         stores or executing the CONSTRUCT queries.
     */
    public static void convert(Repository source,
                               List<String> constructQueries,
                               Repository target, URI namedGraph,
                               String baseUri, boolean clearTargetGraph)
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
        baseUri = getBaseUri(baseUri);

        RepositoryConnection in  = null;
        RepositoryConnection out = null;
        org.openrdf.model.URI u  = null;
        String query = null;
        try {
            in  = source.newConnection();
            out = target.newConnection();
            // Clear target named graph, if requested.
            u = getGraphUri(namedGraph, out, clearTargetGraph);
            // Apply CONSTRUCT queries to generate and insert triples.
            for (String s : constructQueries) {
                query = s;
                GraphQuery q = in.prepareGraphQuery(QueryLanguage.SPARQL,
                                                    query, baseUri);
                out.add(q.evaluate(), u);
            }
            query = null;       // No query in error.
        }
        catch (Exception e) {
            try {
                // Clear target named graph, if any.
                if (out != null) {
                    out.clear(u);
                }
            }
            catch (Exception e2) { /* Ignore... */ }

            throw new RdfException((query != null)? query: e.getMessage(), e);
        }
        finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
            }
            if (out != null) {
                try { out.close();  } catch (Exception e) { /* Ignore... */ }
            }
        }
    }

    /**
     * Applies a set of SPARQL update queries on an RDF store to
     * insert or delete data.
     * @param  target          the RDF store to update.
     * @param  updateQueries   the SPARQL update queries to execute.
     *
     * @throws IllegalArgumentException if no RDF store or SPARQL update
     *         query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         store or executing the update queries.
     *
     * @see    #update(Repository, List, String)
     */
    public static void update(Repository target,
                              List<String> updateQueries) throws RdfException {
        update(target, updateQueries, null);
    }

    /**
     * Applies a set of SPARQL update queries on an RDF store to
     * insert or delete data.
     * @param  target          the RDF store to update.
     * @param  updateQueries   the SPARQL update queries to execute.
     * @param  baseUri         the (optional) base URI to resolve
     *                         relative URIs.
     *
     * @throws IllegalArgumentException if no RDF store or SPARQL update
     *         query list are provided.
     * @throws RdfException if any error occurred accessing the RDF
     *         store or executing the update queries.
     */
    public static void update(Repository target,
                              List<String> updateQueries, String baseUri)
                                                        throws RdfException {
        if (target == null) {
            throw new IllegalArgumentException("source");
        }
        if ((updateQueries == null) || (updateQueries.isEmpty())) {
            throw new IllegalArgumentException("updateQueries");
        }
        baseUri = getBaseUri(baseUri);

        RepositoryConnection in  = null;
        String query = null;
        try {
            in = target.newConnection();

            for (String s : updateQueries) {
                query = s;
                in.prepareUpdate(QueryLanguage.SPARQL, query, baseUri)
                  .execute();
            }
            query = null;       // No query in error.
        }
        catch (Exception e) {
            throw new RdfException((query != null)? query: e.getMessage(), e);
        }
        finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
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
     * @see    #newRdfParser(RdfFormat)
     */
    public static RDFParser newRdfParser(String mimeType) {
        return newRdfParser(RdfFormat.find(mimeType));
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
     * @see    #newRdfParser(RdfFormat)
     */
    public static RDFParser newRdfParser(MediaType mimeType) {
        return newRdfParser(RdfFormat.find(mimeType));
    }

    /**
     * Returns a RDF parser suitable for parsing files of the
     * specified type.
     * @param  type   the RDF type of the data to be parsed.
     *
     * @return a RDF parser.
     * @throws IllegalArgumentException if type is <code>null</code>.
     */
    public static RDFParser newRdfParser(RdfFormat type) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        return type.newParser(new UriCachingValueFactory());
    }

    /**
     * Attempts to guess the type of RDF data a file contains by
     * examining the file extension.
     * @param  fileName   the name (or path) of the file the type
     *                    of which is to determine.
     *
     * @return the RDF format of the file content or <code>null</code>
     *         if the extension was not recognized.
     * @throws IllegalArgumentException if <code>f</code> is
     *         <code>null</code> or is not a regular file.
     */
    public static RdfFormat guessRdfFormatFromExtension(String fileName) {
        String ext = "";
        int i = fileName.lastIndexOf('.');
        if ((i > 0) && (i < fileName.length() - 1)) {
            ext = fileName.substring(i+1);
        }
        RdfFormat format = null;
        for (RdfFormat t : RdfFormat.values()) {
            if (t.isExtensionKnown(ext)) {
                format = t;
                break;
            }
        }
        return format;
    }

    /**
     * Attempts to guess the type of RDF data a file contains by
     * examining the file extension.
     * @param  fileName   the name (or path) of the file the type
     *                    of which is to determine.
     *
     * @return the MIME type of the file content or <code>null</code>
     *         if the extension was not recognized.
     * @throws IllegalArgumentException if <code>f</code> is
     *         <code>null</code> or is not a regular file.
     */
    public static MediaType guessRdfTypeFromExtension(String fileName) {
        RdfFormat format = guessRdfFormatFromExtension(fileName);
        return (format != null)? format.getMimeType(): null;
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
        return RdfFormat.find(mimeType).getMimeType();
    }

    /**
     * Ensure that the provided URI is a valid base URI for RDF data.
     * If no ending '/' of '#' character is present, a slash '/' is
     * appended.
     * @param  uri   the URI to check, possibly <code>null</code>
     *
     * @return a valid base URI.
     * @see    #getBaseUri(String, char)
     */
    public static String getBaseUri(String uri) {
        return getBaseUri(uri, '/');
    }

    /**
     * Ensure that the provided URI is a valid base URI for RDF data.
     * If no ending '/' of '#' character is present, the specified
     * separator character is appended.
     * @param  uri   the URI to check, possibly <code>null</code>
     * @param  sep   the separator to append to the URI if need be.
     *
     * @return a valid base URI.
     */
    public final static String getBaseUri(String uri, char sep) {
        String baseUri = "";
        if (StringUtils.isSet(uri)) {
            baseUri = uri;
            int n = uri.length() - 1;
            if (! ((uri.charAt(n) == '/') || (uri.charAt(n) == '#'))) {
                baseUri += sep;
            }
        }
        return baseUri;
    }

    public final static boolean isValidStringLiteral(String s) {
        boolean valid = true;
        for (int i=0, max=s.length(); i<max; i++) {
            if (! isValidDataCharacter(s.charAt(i))) {
                valid = false;
                break;
            }
        }
        return valid;
    }

    /**
     * Removes all non-valid XML data characters from the specified
     * string as W3C RDF specification states that
     * <a href="http://www.w3.org/TR/rdf-syntax-grammar/#literal">RDF
     * literals shall only contain valid XML character</a>. If all
     * characters are valid, the input string is returned unchanged.
     * @param  s   the string to sanitize.
     *
     * @return a string expunged of all invalid XML data characters.
     */
    public final static String removeInvalidDataCharacter(String s) {
        StringBuilder buf = null;
        for (int i=0, max=s.length(); i<max; i++) {
            char c = s.charAt(i);
            if (isValidDataCharacter(c)) {
                if (buf != null) {
                    buf.append(c);
                }
                // Else: continue until an invalid character is encountered.
            }
            else {
                if (buf == null) {
                    // First invalid character. => Initiate clean copy.
                    buf = new StringBuilder(max);
                    buf.append(s.substring(0, i)); // Skip invalid.
                }
                // Else: skip invalid character.
            }
        }
        return (buf == null)? s: buf.toString();
    }

    /**
     * Removes all triples from the specified named graph.
     * @param  r           the RDF store.
     * @param  graphName   the named graph (context) to purge.
     *
     * @throws RdfException if any exception occurred while removing
     *         the triples.
     */
    public static void clearGraph(Repository r, URI graphName)
                                                        throws RdfException {
        RepositoryConnection cnx = null;
        try {
            cnx = r.newConnection();
            getGraphUri(graphName, cnx, true);
        }
        catch (Exception e) {
            throw new RdfException(String.valueOf(graphName), e);
        }
        finally {
            if (cnx != null) {
                try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private static org.openrdf.model.URI getGraphUri(URI graphName,
                                    RepositoryConnection cnx, boolean clear)
                                                     throws RepositoryException {
        org.openrdf.model.URI namedGraph = null;
        // Clear target named graph, if any.
        if (graphName != null) {
            namedGraph = cnx.getValueFactory().createURI(graphName.toString());
            if (clear) {
                cnx.clear(namedGraph);
            }
        }
        return namedGraph;
    }

    /**
     * Returns whether the specified character can appear in XML
     * character data. The integer encoding also makes the
     * representation of supplementary Unicode characters possible.
     * @param  c   the character to check.
     * 
     * @return <code>true</code> if the character is valid;
     *         <code>false</code> otherwise.
     */
    private final static boolean isValidDataCharacter(int c) {
        // Char ::= #x9 | #xA | #xD | [#x20-#xD7FF] | [#xE000-#xFFFD] |
        // [#x10000-#x10FFFF]
        return (c >= 32 && c <= 55295) || (c >= 57344 && c <= 65533) ||
               (c >= 65536 && c <= 1114111) || c == 9 || c == 10 || c == 13;
    }
}

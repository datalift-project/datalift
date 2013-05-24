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

package org.datalift.converter;


import java.net.URI;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Date;
import java.util.GregorianCalendar;

import static java.util.GregorianCalendar.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.datatype.DatatypeFactory;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CachingSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.SqlQuerySource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.UriBuilder;

import static org.datalift.fwk.rdf.ElementType.*;
import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.MediaTypes.*;


/**
 * A {@link ProjectModule project module} that performs SQL to RDF
 * conversion using
 * <a href="http://www.w3.org/TR/2011/WD-rdb-direct-mapping-20110324/">RDF
 * Direct Mapping</a> principles.
 *
 * @author lbihanic
 */
@Path(SqlDirectMapper.MODULE_NAME)
public class SqlDirectMapper extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "sqldirectmapper";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static DatatypeFactory dtFactory;
    private final static Logger log = Logger.getLogger();

    static {
        try {
            dtFactory = DatatypeFactory.newInstance();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public SqlDirectMapper() {
        super(MODULE_NAME, 100, SourceType.SqlQuerySource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        return this.newProjectView("sqlDirectMapper.vm", projectId);
    }

    @GET
    @Path("columns")
    @Produces(APPLICATION_JSON)
    public Response getColumnNames(@QueryParam("project") URI projectId,
                                   @QueryParam("source") URI sourceId,
                                   @Context Request request)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            SqlQuerySource in = (SqlQuerySource)p.getSource(sourceId);
            // Check data freshness HTTP headers (If-Modified-Since & ETags)
            CachingSource cs = null;
            Date lastModified = null;
            if (in instanceof CachingSource) {
                cs = (CachingSource)in;
                lastModified = cs.getLastCacheUpdate();
                if (lastModified != null) {
                    response = request.evaluatePreconditions(lastModified);
                }
            }
            if (response == null) {
                // Data not (yet) cached or staled cache. => Return data.
                StringBuilder sb = new StringBuilder(512).append('[');
                for (String s : in.getColumnNames()) {
                    sb.append('"').append(s).append("\",");
                }

                sb.setLength(sb.length() - 1);  // Remove last separator.
                sb.append(']');                 // End JSON array.

                response = Response.ok(sb.toString(), APPLICATION_JSON_UTF8);
                // Set page expiry & last modification date.
                if (lastModified != null) {
                    response = response.lastModified(lastModified)
                                       .expires(cs.getCacheExpiryDate());
                }
            }
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response.build();
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response mapSqlData(@FormParam("project") URI projectId,
                               @FormParam("source") URI sourceId,
                               @FormParam("dest_title") String destTitle,
                               @FormParam("dest_graph_uri") URI targetGraph,
                               @FormParam("key_column") String keyColumn)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            SqlQuerySource in = (SqlQuerySource)p.getSource(sourceId);
            // Convert CSV data and load generated RDF triples.
            Repository internal = Configuration.getDefault()
                                               .getInternalRepository();
            this.convert(in, keyColumn, internal, targetGraph);
            // Register new transformed RDF source.
            Source out = this.addResultSource(p, in, destTitle, targetGraph);
            // Display project source tab, including the newly created source.
            response = this.created(out).build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private void convert(SqlQuerySource src, String keyColumn,
                                        Repository target, URI targetGraph) {
        if (isBlank(keyColumn)) {
            keyColumn = null;
        }
        final UriBuilder uriBuilder = Configuration.getDefault()
                                                   .getBean(UriBuilder.class);
        final RepositoryConnection cnx = target.newConnection();
        try {
            final ValueFactory valueFactory = cnx.getValueFactory();

            long t0 = System.currentTimeMillis();
            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
            // Clear target named graph, if any.
            org.openrdf.model.URI ctx = null;
            if (targetGraph != null) {
                ctx = valueFactory.createURI(targetGraph.toString());
                cnx.clear(ctx);
            }
            String baseUri = RdfUtils.getBaseUri(
                    (targetGraph != null)? targetGraph.toString(): null, '/');
            String typeUri = RdfUtils.getBaseUri(
                    (targetGraph != null)? targetGraph.toString(): null, '#');
            org.openrdf.model.URI rdfType = valueFactory.createURI(typeUri,
                                    uriBuilder.urlify(src.getTitle(), RdfType));
            // Build predicates URIs.
            int max = src.getColumnNames().size();
            org.openrdf.model.URI[] predicates = new org.openrdf.model.URI[max];
            int i = 0;
            for (String s : src.getColumnNames()) {
                if (! s.equals(keyColumn)) {
                    predicates[i] = valueFactory.createURI(typeUri +
                                            uriBuilder.urlify(s, Predicate));
                }
                i++;
            }
            // Load triples
            long statementCount = 0L;
            int  batchSize = Env.getRdfBatchSize();
            i = 1;                              // Start numbering lines at 1.
            for (Row<Object> row : src) {
                String key = (keyColumn != null)? row.getString(keyColumn):
                                                  String.valueOf(i);
                org.openrdf.model.URI subject =
                                valueFactory.createURI(baseUri + key); // + "#_";
                log.trace("Mapping {} to <{}>", key, subject);
                boolean firstStatement = false;
                for (int j=0; j<max; j++) {
                    Object o = row.get(j);
                    if ((o != null) && (predicates[j] != null)) {
                        Literal value = this.mapValue(o, valueFactory);
                        cnx.add(valueFactory.createStatement(
                                        subject, predicates[j], value), ctx);
                        if (firstStatement) {
                            cnx.add(valueFactory.createStatement(
                                        subject, RDF.TYPE, rdfType), ctx);
                            firstStatement = false;
                        }
                        // Commit transaction according to the configured batch size.
                        statementCount++;
                        if ((statementCount % batchSize) == 0) {
                            cnx.commit();
                        }
                        //log.trace("<{}> <{}> {} ({})",
                        //                    subject, predicates[j], value, o);
                    }
                    // Else: ignore cell.
                }
                i++;
            }
            cnx.commit();
            long delay = System.currentTimeMillis() - t0;
            log.debug("Inserted {} RDF triples into <{}> from {} SQL results in {} seconds",
                      wrap(statementCount), targetGraph,
                      wrap(i - 1), wrap(delay / 1000.0));
        }
        catch (Exception e) {
            throw new TechnicalException("sql.conversion.failed", e);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore */ }
        }
    }

    private Literal mapValue(Object o, ValueFactory valueFactory) {
        Literal v = null;

        if (o instanceof String) {
            v = valueFactory.createLiteral(o.toString());
        }
        else if (o instanceof Boolean) {
            v = valueFactory.createLiteral(((Boolean)o).booleanValue());
        }
        else if (o instanceof Byte) {
            v = valueFactory.createLiteral(((Byte)o).byteValue());
        }
        else if ((o instanceof Double) || (o instanceof Float)) {
            v = valueFactory.createLiteral(((Number)o).doubleValue());
        }
        else if ((o instanceof Integer) || (o instanceof Short)) {
            v = valueFactory.createLiteral(((Number)o).intValue());
        }
        else if (o instanceof Long) {
            v = valueFactory.createLiteral(((Long)o).longValue());
        }
        else if (o instanceof java.sql.Date) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTimeInMillis(((java.sql.Date)o).getTime());

            v = valueFactory.createLiteral(
                    dtFactory.newXMLGregorianCalendarDate(this.getYear(c),
                                        c.get(MONTH) + 1, c.get(DAY_OF_MONTH),
                                        this.getTimeZoneOffsetInMinutes(c)));
        }
        else if (o instanceof Time) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTimeInMillis(((Time)o).getTime());

            v = valueFactory.createLiteral(
                    dtFactory.newXMLGregorianCalendarTime(
                                        c.get(HOUR_OF_DAY), c.get(MINUTE),
                                        c.get(SECOND), c.get(MILLISECOND),
                                        this.getTimeZoneOffsetInMinutes(c)));
        }
        else if (o instanceof Timestamp) {
            GregorianCalendar c = new GregorianCalendar();
            c.setTimeInMillis(((Timestamp)o).getTime());

            v = valueFactory.createLiteral(
                                        dtFactory.newXMLGregorianCalendar(c));
        }
        return v;
    }

    private final int getYear(GregorianCalendar c) {
        int year = c.get(YEAR);
        if (c.get(ERA) == BC) {
            year = -year;
        }
        return year;
    }

    private final int getTimeZoneOffsetInMinutes(GregorianCalendar c) {
        return (c.get(ZONE_OFFSET) + c.get(DST_OFFSET)) / (60*1000);
    }
}

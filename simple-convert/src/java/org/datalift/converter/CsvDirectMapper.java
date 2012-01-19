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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.Env;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * A {@link ProjectModule project module} that performs CSV to RDF
 * conversion using
 * <a href="http://www.w3.org/TR/2011/WD-rdb-direct-mapping-20110324/">RDF
 * Direct Mapping</a> principles.
 *
 * @author lbihanic
 */
@Path(CsvDirectMapper.MODULE_NAME)
public class CsvDirectMapper extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "csvdirectmapper";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public CsvDirectMapper() {
        super(MODULE_NAME, 100, SourceType.CsvSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        // Retrieve project.
        Project p = this.getProject(projectId);
        // Display conversion configuration page.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p);
        args.put("converter", this);
        return Response.ok(this.newViewable("/csvDirectMapper.vm", args))
                       .build();
    }

    @POST
    public Response loadSourceData(@QueryParam("project") URI projectId,
                                   @QueryParam("source") URI sourceId,
                                   @FormParam("dest_title") String destTitle,
                                   @FormParam("dest_graph_uri") URI targetGraph,
                                   @FormParam("base_uri") URI baseUri)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            CsvSource in = (CsvSource)(p.getSource(sourceId));
            // Convert CSV data and load generated RDF triples.
            this.convert(in, Configuration.getDefault().getInternalRepository(),
                             targetGraph, baseUri);
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

    private void convert(CsvSource src, Repository target,
                                        URI targetGraph, URI baseUri) {
        final RepositoryConnection cnx = target.newConnection();
        org.openrdf.model.URI ctx = null;
        try {
            final ValueFactory valueFactory = cnx.getValueFactory();

            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
            // Clear target named graph, if any.
            if (targetGraph != null) {
                ctx = valueFactory.createURI(targetGraph.toString());
                cnx.clear(ctx);
            }
            if (baseUri == null) {
                baseUri = targetGraph;
            }
            String root = RdfUtils.getBaseUri(
                                (baseUri != null)? baseUri.toString(): null);
            // Build predicates URIs.
            int max = src.getColumnNames().size();
            org.openrdf.model.URI[] predicates = new org.openrdf.model.URI[max];
            int i = 0;
            for (String s : src.getColumnNames()) {
                predicates[i++] = valueFactory.createURI(root + urlify(s));
            }
            // Load triples
            long statementCount = 0L;
            int  batchSize = Env.getRdfBatchSize();
            i = 1;                              // Start line numbering at 1.
            for (Row<String> row : src) {
                org.openrdf.model.URI subject =
                                valueFactory.createURI(root + i); // + "#_";
                log.trace("Mapping {} to \"{}\"", row, subject);
                for (int j=0, l=row.size(); j<l; j++) {
                    String v = row.get(j);
                    if (isSet(v)) {
                        cnx.add(valueFactory.createStatement(
                                        subject, predicates[j],
                                        this.mapValue(v, valueFactory)),
                                ctx);

                        // Commit transaction according to the configured batch size.
                        statementCount++;
                        if ((statementCount % batchSize) == 0) {
                            cnx.commit();
                        }
                    }
                    // Else: ignore cell.
                }
                i++;
            }
            cnx.commit();
            log.debug("Inserted {} RDF triples into {} from {} CSV lines",
                      Long.valueOf(statementCount), targetGraph,
                      Integer.valueOf(i));
        }
        catch (Exception e) {
            try {
                // Forget pending triples.
                cnx.rollback();
                // Clear target named graph, if any.
                if (ctx != null) {
                    cnx.clear(ctx);
                }
            }
            catch (Exception e2) { /* Ignore... */ }

            throw new TechnicalException("csv.conversion.failed", e);
        }
        finally {
            // Commit pending data (including graph removal in case of error).
            try { cnx.commit(); } catch (Exception e) { /* Ignore... */ }
            // Close repository connection.
            try { cnx.close();  } catch (Exception e) { /* Ignore... */ }
        }
    }

    private Literal mapValue(String s, ValueFactory valueFactory) {
        Literal v = null;
        s = s.trim();
        if ((s.indexOf('.') != -1) || (s.indexOf(',') != -1)) {
            // Try double.
            try {
                v = valueFactory.createLiteral(
                                    Double.parseDouble(s.replace(',', '.')));
            }
            catch (Exception e) { /* Ignore... */ }
        }
        if (v == null) {
            // Try integer.
            try {
                v = valueFactory.createLiteral(Long.parseLong(s));
            }
            catch (Exception e) { /* Ignore... */ }
        }
        if (v == null) {
            // Assume string literal.
            v = valueFactory.createLiteral(s);
        }
        return v;
    }
}

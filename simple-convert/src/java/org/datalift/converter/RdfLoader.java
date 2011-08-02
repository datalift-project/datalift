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


import java.io.File;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import com.sun.jersey.api.NotFoundException;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;


public class RdfLoader extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private ProjectManager projectManager = null;
    private SparqlEndpoint sparqlEndpoint = null;
    private File storage = null;
    private Repository internRepository = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public RdfLoader() {
        super("rdfloader", true);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);

        this.internRepository = configuration.getInternalRepository();
        this.storage = configuration.getPublicStorage();

        this.projectManager = configuration.getBean(ProjectManager.class);
        if (this.projectManager == null) {
            throw new TechnicalException("project.manager.not.available");
        }
        this.sparqlEndpoint = configuration.getBean(SparqlEndpoint.class);
        if (this.sparqlEndpoint == null) {
            throw new TechnicalException("sparql.endpoint.not.available");
        }
    }

    //-------------------------------------------------------------------------
    // ProjectModule contract support
    //-------------------------------------------------------------------------

    @Override
    public URI canHandle(Project p) {
        boolean hasRdfSource = false;
        for (Source s : p.getSources()) {
            if (s instanceof RdfSource) {
                hasRdfSource = true;
                break;
            }
        }
        URI projectPage = null;
        if (hasRdfSource) {
            try {
                return new URI(this.getName() + "?project=" + p.getUri());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return projectPage;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public Response publishProject(
                        @QueryParam("project") URI projectId,
                        @Context UriInfo uriInfo,
                        @Context Request request,
                        @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        RdfSource src = null;
        Project p = this.projectManager.findProject(projectId);
        if (p != null) {
            for (Source s : p.getSources()) {
                if (s instanceof RdfSource) {
                    src = (RdfSource)s;
                    // Continue to find last RDF source available...
                }
            }
        }
        if (src == null) {
            throw new NotFoundException("No RDF source found", projectId);
        }
        Response response = null;
        try {
            src.init(storage, uriInfo.getBaseUri());

            String srcName = " (RDF #" + p.getSources().size() + ')';
            URI targetGraph = new URI(src.getUri()
                                      + '/' + StringUtils.urlify(srcName));
            this.convert(src, this.internRepository, targetGraph);
            p.addSource(this.projectManager.newTransformedRdfSource(
                                        targetGraph, src.getTitle() + srcName,
                                        targetGraph, src));
            this.projectManager.saveProject(p);

            List<String> defGraphs = new LinkedList<String>();
            defGraphs.add(this.internRepository.getName());
            response = this.sparqlEndpoint.executeQuery(
                                    defGraphs, null,
                                    "SELECT * WHERE { GRAPH <"
                                        + targetGraph + "> { ?s ?p ?o . } }",
                                    uriInfo, request, acceptHdr).build();
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
            log.error(error.getMessage(), e);
            throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaType.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
        }
        return response;
    }

    private void convert(RdfSource src, Repository target, URI targetGraph) {
        final RepositoryConnection cnx = target.newConnection();
        try {
            final ValueFactory valueFactory = cnx.getValueFactory();

            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
            // Clear target named graph, if any.
            org.openrdf.model.URI ctx = null;
            if (targetGraph != null) {
                ctx = valueFactory.createURI(targetGraph.toString());
                cnx.clear(ctx);
            }
            // Load triples.
            cnx.add(src, ctx);
            cnx.commit();
        }
        catch (Exception e) {
            throw new TechnicalException("rdf.conversion.failed", e);
        }
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore */ }
        }
    }
}

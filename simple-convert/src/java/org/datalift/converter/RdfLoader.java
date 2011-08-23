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

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.Repository;


public class RdfLoader extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String MODULE_NAME = "rdfloader";

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public RdfLoader() {
        super(MODULE_NAME, SourceType.RdfSource, HttpMethod.POST);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @POST
    public Response publishProject(@QueryParam("project") URI projectId,
                                   @Context UriInfo uriInfo,
                                   @Context Request request,
                                   @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            RdfSource src = (RdfSource)this.getLastSource(p);
            src.init(this.configuration, uriInfo.getBaseUri());
            // Parse RDF to load triples.
            String srcName  = this.nextSourceName(p);
            URI targetGraph = this.newGraphUri(src, srcName);
            this.convert(src, this.internalRepository, targetGraph);
            // Register new transformed RDF source.
            this.addResultSource(p, src, srcName, targetGraph);
            // Display generated triples.
            response = this.displayGraph(this.internalRepository, targetGraph,
                                         uriInfo, request, acceptHdr);
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

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

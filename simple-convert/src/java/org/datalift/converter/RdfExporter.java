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


import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.openrdf.OpenRDFException;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.web.UriParam;
import org.datalift.fwk.view.TemplateModel;

import static org.datalift.fwk.MediaTypes.*;


/**
 * A {@link ProjectModule project module} that streams the RDF triples
 * of a source from the internal repository into the HTTP response, to
 * save them into a file on the user's computer.
 *
 * @author lbihanic
 */
@Path(RdfExporter.MODULE_NAME)
public class RdfExporter extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "rdfexporter";

    /* Web service parameter names. */
    private final static String TARGET_FMT_PARAM        = "mime_type";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public RdfExporter() {
        super(MODULE_NAME, 10020, SourceType.TransformedRdfSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam(PROJECT_ID_PARAM) URI projectId) {
        // Retrieve project.
        Project p = this.getProject(projectId);
        // Display conversion configuration page.
        TemplateModel view = this.newView("rdfExporter.vm", p);
        view.put("converter", this);
        view.put("formats",   RdfFormat.values());
        return Response.ok(view).build();
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response exportRdfSource(
                            @FormParam(PROJECT_ID_PARAM) UriParam projectId,
                            @FormParam(SOURCE_ID_PARAM)  UriParam sourceId,
                            @FormParam(TARGET_FMT_PARAM) String mimeType)
                                                throws WebApplicationException {
        if (! UriParam.isSet(projectId)) {
            this.throwInvalidParamError(PROJECT_ID_PARAM, null);
        }
        if (! UriParam.isSet(sourceId)) {
            this.throwInvalidParamError(SOURCE_ID_PARAM, null);
        }
        Response response = null;

        Repository internal = Configuration.getDefault()
                                           .getInternalRepository();
        try {
            RdfFormat rdfType = RdfFormat.get(mimeType);
            if (rdfType == null) {
                this.throwInvalidParamError(TARGET_FMT_PARAM, mimeType);
            }
            // Retrieve source.
            Project p = this.getProject(projectId.toUri(PROJECT_ID_PARAM));
            TransformedRdfSource s = (TransformedRdfSource)
                            (p.getSource(sourceId.toUri(SOURCE_ID_PARAM)));
            if (s == null) {
                throw new ObjectNotFoundException("project.source.not.found",
                                                  projectId, sourceId);
            }
            // Build default file name for downloaded data.
            String name = this.getTerminalName(s.getUri())
                                            + '.' + rdfType.getFileExtension();
            // Dump selected to directly into the socket.
            StreamingOutput out = new SourceDumpStreamingOutput(internal,
                                                s.getTargetGraph(), rdfType);
            response = Response.ok(out, rdfType.getMimeType())
                               .header("Content-Disposition",
                                       "attachment; filename=" + name)
                               .header("Refresh", "0.1; " + p.getUri())
                               .build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }


    private final class SourceDumpStreamingOutput implements StreamingOutput
    {
        private final Repository repository;
        private final String namedGraph;
        private final RdfFormat rdfType;

        public SourceDumpStreamingOutput(Repository repository,
                                         String namedGraph, RdfFormat rdfType) {
            this.repository = repository;
            this.namedGraph = namedGraph;
            this.rdfType    = rdfType;
        }

        @Override
        public void write(OutputStream out)
                                throws IOException, WebApplicationException {
            RepositoryConnection cnx = this.repository.newConnection();
            try {
                RDFHandler h = this.rdfType.newWriter(out);
                if (log.isDebugEnabled()) {
                    h = getDebugHandler(h, this.namedGraph);
                }
                cnx.export(h, cnx.getValueFactory().createURI(namedGraph));
            }
            catch (OpenRDFException e) {
                handleInternalError(e);
            }
            finally {
                Repository.closeQuietly(cnx);
            }
        }
    }
}

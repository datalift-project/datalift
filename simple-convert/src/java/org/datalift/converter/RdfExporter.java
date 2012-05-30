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
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandler;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.helpers.RDFHandlerWrapper;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.Repository;


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
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        // Retrieve project.
        Project p = this.getProject(projectId);
        // Display conversion configuration page.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p);
        args.put("converter", this);
        args.put("formats", RdfFormat.values());
        return Response.ok(this.newViewable("/rdfExporter.vm", args))
                       .build();
    }

    @POST
    public Response exportRdfSource(@FormParam("project") URI projectId,
                                    @FormParam("source") URI sourceId,
                                    @FormParam("mime_type") String mimeType)
                                                throws WebApplicationException {
        Response response = null;

        Repository internal = Configuration.getDefault()
                                           .getInternalRepository();
        try {
            RdfFormat rdfType = RdfFormat.get(mimeType);
            if (rdfType == null) {
                this.throwInvalidParamError("mime_type", mimeType);
            }
            // Retrieve source.
            Project p = this.getProject(projectId);
            TransformedRdfSource s = (TransformedRdfSource)
                                                        (p.getSource(sourceId));
            if (s == null) {
                this.throwInvalidParamError("source", sourceId);
            }
            // Build default file name for downloaded data.
            String name = this.getFileName(s.getUri())
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

    private String getFileName(String s) {
        int i = Math.max(s.lastIndexOf('/'), s.lastIndexOf('#'));
        return (i != -1)? (i == (s.length() - 1))?
                        getFileName(s.substring(0, i)): s.substring(i + 1): s;
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
                    h = getDebugHandler(h);
                }
                cnx.export(h, cnx.getValueFactory().createURI(namedGraph));
            }
            catch (OpenRDFException e) {
                handleInternalError(e);
            }
            finally {
                try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }

        private RDFHandler getDebugHandler(RDFHandler h) {
            return new RDFHandlerWrapper(h) {
                    private long statementCount = -1L;
                    private long t0 = -1L;

                    /** {@inheritDoc} */
                    @Override
                    public void startRDF() throws RDFHandlerException {
                        super.startRDF();
                        this.t0 = System.currentTimeMillis();
                        this.statementCount = 0L;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public void handleStatement(Statement st)
                                            throws RDFHandlerException {
                        super.handleStatement(st);
                        this.statementCount++;
                    }

                    /** {@inheritDoc} */
                    @Override
                    public void endRDF() throws RDFHandlerException {
                        super.endRDF();
                        long delay = System.currentTimeMillis() - this.t0;
                        log.debug("Exported {} triples from <{}> in {} seconds",
                                  Long.valueOf(this.statementCount), namedGraph,
                                  Double.valueOf(delay / 1000.0));
                    }
                };
        }
    }
}

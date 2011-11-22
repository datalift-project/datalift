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

package org.datalift.projectmanager;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.openrdf.model.Statement;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.rio.RDFHandlerException;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;

import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import static javax.ws.rs.core.MediaType.TEXT_HTML;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CachingSource;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.SqlSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.project.ProjectModule.UriDesc;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.web.json.GridJsonWriter;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.*;



/**
 * The web resource providing an HTML interface for managing
 * data-lifting projects.
 *
 * @author hdevos
 */
@Path("/" + Workspace.PROJECT_URI_PREFIX)
public class Workspace extends BaseModule
{
    //-------------------------------------------------------------------------
    // Project page tabs enumeration
    //-------------------------------------------------------------------------

    public enum ProjectTab {
        Description     ("description"),
        Sources         ("source"),
        Ontologies      ("ontology");

        public final String anchor;

        ProjectTab(String id) {
            this.anchor = "#" + id;
        }
    }

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "workspace";
    /** The prefix for the URI of the project objects. */
    public final static String PROJECT_URI_PREFIX = "project";
    /** The prefix for the URI of the source objects, within projects. */
    public final static String SOURCE_URI_PREFIX  = "source";

    /** The relative path prefix for project objects and resources. */
    private final static String REL_PROJECT_PATH = PROJECT_URI_PREFIX + '/';
    /** The relative path prefix for source objects, within projects. */
    private final static String SOURCE_PATH = "/" + SOURCE_URI_PREFIX  + '/';
    /** The path prefix for HTML page Velocity templates. */
    private final static String TEMPLATE_PATH = "/" + MODULE_NAME  + '/';

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** Project Manager bean. */
    private ProjectManager projectManager = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public Workspace() {
        super(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        this.projectManager = configuration.getBean(ProjectManager.class);
    }

    // ------------------------------------------------------------------------
    // Web services
    // ------------------------------------------------------------------------

    /**
     * <i>[Resource method]</i> Displays the project index page.
     * @return the project index HTML page.
     */
    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response displayIndexPage() throws WebApplicationException {
        Response response = null;
        try {
            response = this.displayIndexPage(Response.ok(), null).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, null);
        }
        return response;
    }

    /**
     * <i>[Resource method]</i> Creates a new data lifting project.
     * @param  title         the project name.
     * @param  description   the project description.
     * @param  license       the URL of the license that applies to the
     *                       project.
     * @param  uriInfo       the requested URI.
     *
     * @return A redirection to the new project summary page
     *         (<a href="http://en.wikipedia.org/wiki/Post/Redirect/Get">Post-Redirect-Get pattern</a>).
     * @throws WebApplicationException if any error occurred.
     */
    @POST
    public Response registerProject(
                                @FormParam("title") String title,
                                @FormParam("description") String description,
                                @FormParam("license") String license,
                                @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        // Check that project does not yet exist.
        URI projectId = this.newProjectId(uriInfo.getBaseUri(), title);
        if (this.findProject(projectId) == null) {
            try {
                // Create new project.
                License l = License.valueOf(license);
                Project p = this.projectManager.newProject(projectId, title,
                                                           description, l.uri);
                // Persist project to RDF store.
                this.projectManager.saveProject(p);
                // Notify user of successful creation, redirecting HTML clients
                // (browsers) to the project page.
                response = this.created(p, null, null).build();
            }
            catch (Exception e) {
                this.handleInternalError(e, "Failed to persist project");
            }
        }
        else {
            log.fatal("Duplicate identifier \"{}\" for new project \"{}\"",
                                                        urlify(title), title);
            TechnicalException error = new TechnicalException(
                                                "duplicate.identifier", title);
            throw new ConflictException(error.getMessage());
        }
        return response;
    }

    /**
     * <i>[Resource method]</i> Displays the project creation page.
     * @param  uriInfo   the requested URI.
     *
     * @return the project creation HTML page.
     * @throws WebApplicationException if any error occurred.
     */
    @GET
    @Path("add")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getNewProjectPage(@Context UriInfo uriInfo)
                                                throws WebApplicationException {
        return this.getProjectModifyPage(null, uriInfo);
    }

    /**
     * <i>[Resource method]</i> Displays the project update page.
     * @param  id        the identifier of the project to modify.
     * @param  uriInfo   the requested URI.
     *
     * @return the project update HTML page, populated with the current
     *         project information.
     * @throws WebApplicationException if any error occurred.
     */
    @GET
    @Path("{id}/modify")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getProjectModifyPage(@PathParam("id") String id,
                                         @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            Map<String, Object> args = new TreeMap<String, Object>();
            if (id != null) {
                // Retrieve project.
                URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
                args.put("it", this.loadProject(projectUri));
            }
            // Else: new project.

            args.put("licenses", License.values());
            // Display project modification page.
            response = Response.ok(
                        this.newViewable("workspaceModifyProject.vm", args),
                        TEXT_HTML).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load project");
        }
        return response;
    }

    /**
     * <i>[Resource method]</i> Updates the specified project.
     * @param  id            the identifier of the project to modify.
     * @param  title         the new project name or <code>null</code>
     *                       to leave it unchanged.
     * @param  description   the new project description or
     *                       <code>null</code> to leave it unchanged.
     * @param  license       the URL of the new project license or
     *                       <code>null</code> to leave it unchanged.
     * @param  delete        whether to delete the project.
     * @param  uriInfo       the requested URI.
     *
     * @return A redirection to the project summary page of to the
     *         project index page if the project was deleted
     *         (<a href="http://en.wikipedia.org/wiki/Post/Redirect/Get">Post-Redirect-Get pattern</a>).
     * @throws WebApplicationException if any error occurred.
     */
    @POST
    @Path("{id}")
    public Response modifyProject(
                    @PathParam("id") String id,
                    @FormParam("title") String title,
                    @FormParam("description") String description,
                    @FormParam("license") String license,
                    @Context UriInfo uriInfo) throws WebApplicationException {
        Response response = null;

        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);

            boolean modified = false;
            if (! isBlank(title)) {
                p.setTitle(title);
                modified = true;
            }
            if (! isBlank(description)) {
                p.setDescription(description);
                modified = true;
            }
            if (! isBlank(license)) {
                URI li = License.valueOf(license).uri;
                if (!p.getLicense().equals(li)) {
                    p.setLicense(li);
                    modified = true;
                }
            }
            if (modified) {
                this.projectManager.saveProject(p);
            }
            response = this.redirect(p, null).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to update project");
        }
        return response;
    }

    /**
     * <i>[Resource method]</i> Deletes the specified project.
     * @param  id        the identifier of the project to delete.
     * @param  uriInfo   the requested URI.
     *
     * @return A redirection to the project index page
     *         (<a href="http://en.wikipedia.org/wiki/Post/Redirect/Get">Post-Redirect-Get pattern</a>).
     * @throws WebApplicationException if any error occurred.
     */
    @DELETE
    @Path("{id}")
    public Response deleteProject(@PathParam("id") String id,
                                  @Context UriInfo uriInfo)
                                            throws WebApplicationException {
        Response response = null;

        URI uri = null;
        try {
            uri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(uri);
            for (Source s: p.getSources()) {
                s.delete();
            }
            this.projectManager.deleteProject(p);

            response = this.displayIndexPage(Response.ok(), null).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to delete project {}", uri);
        }
        return response;
    }

    /**
     * <i>[Resource method]</i> Display the summary page for the
     * specified project.
     * @param  id        the identifier of the project to display.
     * @param  uriInfo   the requested URI.
     * @param  request   the HTTP request.
     *
     * @return the project summary HTML page.
     * @throws WebApplicationException if any error occurred.
     */
    @GET
    @Path("{id}")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response displayProject(@PathParam("id") String id,
                                   @Context UriInfo uriInfo,
                                   @Context Request request)
                                                throws WebApplicationException {
        ResponseBuilder response = null;

        URI uri = uriInfo.getAbsolutePath();
        try {
            Project p = this.loadProject(uri);
            // Check data freshness HTTP headers (If-Modified-Since & ETags)
            Date lastModified = p.getModificationDate();
            if (lastModified != null) {
                response = request.evaluatePreconditions(lastModified);
            }
            if (response == null) {
                // Page not (yet) cached or staled cache. => Return page.
                response = this.displayIndexPage(Response.ok(), p);
            }
        }
        catch (Exception e) {
            this.handleInternalError(e, "Error accessing project {}", uri);
        }
        return response.build();
    }

    /**
     * <i>[Resource method]</i> Returns the RDF description of the
     * specified project.
     * @param  id          the identifier of the project to display.
     * @param  uriInfo     the requested URI.
     * @param  request     the HTTP request.
     * @param  acceptHdr   the Accept header of the HTTP request.
     *
     * @return the RDF description of the project, in the best
     *         available format (content negotiation).
     * @throws WebApplicationException if any error occurred.
     */
    @GET
    @Path("{id}")
    @Produces({ APPLICATION_RDF_XML, TEXT_TURTLE, APPLICATION_TURTLE,
                TEXT_N3, TEXT_RDF_N3, APPLICATION_N3, APPLICATION_NTRIPLES,
                APPLICATION_JSON })
    public Response describeProject(@PathParam("id") String id,
                                    @Context UriInfo uriInfo,
                                    @Context Request request,
                                    @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        // Check that projects exists in internal data store.
        URI uri = uriInfo.getAbsolutePath();
        this.loadProject(uri);
        // Forward request for project RDF description to the SPARQL endpoint.
        Configuration cfg = Configuration.getDefault();
        List<String> defGraph = Arrays.asList(cfg.getInternalRepository().name);
        return cfg.getBean(SparqlEndpoint.class)
                  .executeQuery(defGraph, null,
                                "DESCRIBE <" + uri + '>',
                                uriInfo, request, acceptHdr).build();
    }

    @GET
    @Path("{id}/srcupload")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Object getSourceUploadPage(@PathParam("id") String id,
                                      @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        URI uri = null;
        try {
            uri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(uri);

            Map<String, Object> args = new HashMap<String, Object>();
            args.put("it", p);
            args.put("sep", Separator.values());
            response = Response.ok(
                            this.newViewable("projectSourceUpload.vm", args),
                            TEXT_HTML).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load project {}", uri);
        }
        return response;
    }

    @GET
    @Path("{id}/source/{srcid}/modify")
    public Response getSourceModifyPage(@PathParam("id") String id,
                                        @PathParam("srcid") String srcId,
                                        @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);

            // Search for requested source in project.
            Source src = p.getSource(
                new URL(this.getSourceId(projectUri, srcId)).toURI());
            if (src == null) {
                // Not found.
                throw new NotFoundException();
            }
            Map<String, Object> args = new HashMap<String, Object>();
            args.put("it", p);
            args.put("current", src);
            args.put("sep", Separator.values());
            response = Response.ok(
                            this.newViewable("projectSourceUpload.vm", args),
                            TEXT_HTML).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load source {}", srcId);
        }
        return response;
    }

    @POST
    @Path("{id}/csvupload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadCsvSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("description") String description,
                            @FormDataParam("source") InputStream file,
                            @FormDataParam("source")
                            FormDataContentDisposition fileDisposition,
                            @FormDataParam("separator") String separator,
                            @FormDataParam("title_row") String titleRow,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        if (file == null) {
            this.throwInvalidParamError("source", null);
        }
        if (!isSet(separator)) {
            this.throwInvalidParamError("separator", separator);
        }
        Response response = null;

        String fileName = fileDisposition.getFileName();
        log.debug("Processing CSV source creation request for {}", fileName);
        try {
            // Build object URIs from request path.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            URI sourceUri = new URI(projectUri.getScheme(), null,
                                    projectUri.getHost(), projectUri.getPort(),
                                    this.getSourceId(projectUri.getPath(), fileName),
                                    null, null);
            // Retrieve project.
            Project p = this.loadProject(projectUri);
            // Save new source data to public project storage.
            String filePath = this.getProjectFilePath(projectId, fileName);
            File storagePath = this.getFileStorage(filePath);
            fileCopy(file, storagePath);

            Separator sep = Separator.valueOf(separator);
            boolean hasTitleRow = ((titleRow != null) &&
                                   (titleRow.toLowerCase().equals("on")));
            // Initialize new source.
            CsvSource src = this.projectManager.newCsvSource(p, sourceUri,
                                                fileName, description, filePath,
                                                sep.getValue(), hasTitleRow);
            // Iterate on source content to validate uploaded file.
            int n = 0;
            CloseableIterator<?> i = src.iterator();
            try {
                for (; i.hasNext(); ) {
                    n++;
                    i.next();   // Throws TechnicalException if data is invalid.
                }
            }
            finally {
                i.close();
            }
            // Persist new source.
            this.projectManager.saveProject(p);
            // Notify user of successful creation, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.created(p, sourceUri, ProjectTab.Sources).build();

            log.info("New CSV source \"{}\" created", sourceUri);
        }
        catch (IOException e) {
            this.throwInvalidParamError(fileName, e.getLocalizedMessage());
        }
        catch (Exception e) {
            this.handleInternalError(e,
                            "Failed to create CVS source for {}", fileName);
        }
        return response;
    }

    @POST
    @Path("{id}/csvmodify")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response modifyCsvSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("current_source") URI sourceUri,
                            @FormDataParam("description") String description,
                            @FormDataParam("separator") String separator,
                            @FormDataParam("title_row") String titleRow,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        if (!isSet(separator)) {
            this.throwInvalidParamError("separator", separator);
        }
        Response response = null;

        try {
            // Retrieve source.
            CsvSource s = this.loadSource(CsvSource.class, sourceUri);
            // Update source data.
            s.setDescription(description);
            s.setSeparator(separator);
            boolean hasTitleRow = ((titleRow != null) &&
                                   (titleRow.toLowerCase().equals("on")));
            s.setTitleRow(hasTitleRow);
            // Save updated source.
            Project p = s.getProject();
            this.projectManager.saveProject(p);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.redirect(p, ProjectTab.Sources).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Could not modify CSV source {}",
                                        sourceUri);
        }
        return response;
    }

    @POST
    @Path("{id}/rdfupload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadRdfSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("description") String description,
                            @FormDataParam("base_uri") URI baseUri,
                            @FormDataParam("source") InputStream file,
                            @FormDataParam("source")
                            FormDataContentDisposition fileDisposition,
                            @FormDataParam("mime_type") String mimeType,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        if (file == null) {
            this.throwInvalidParamError("source", null);
        }
        Response response = null;

        String fileName = fileDisposition.getFileName();
        log.debug("Processing RDF source creation request for {}", fileName);
        try {
            // Build object URIs from request path.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            URI sourceUri = new URI(projectUri.getScheme(), null,
                                    projectUri.getHost(), projectUri.getPort(),
                                    this.getSourceId(projectUri.getPath(), fileName),
                                    null, null);
            // Retrieve project.
            Project p = this.loadProject(projectUri);
            // Save new source to public project storage.
            String filePath = this.getProjectFilePath(projectId, fileName);
            File storagePath = this.getFileStorage(filePath);
            fileCopy(file, storagePath);
            // Initialize new source.
            RdfFileSource src = this.projectManager.newRdfSource(p,
                                            sourceUri, fileName, description,
                                            baseUri, filePath, mimeType);
            // Iterate on source content to validate uploaded file.
            int n = 0;
            CloseableIterator<?> i = src.iterator();
            try {
                for (; i.hasNext(); ) {
                    n++;
                    i.next();   // Throws TechnicalException if data is invalid.
                }
            }
            finally {
                i.close();
            }
            // Persist new source.
            this.projectManager.saveProject(p);
            // Notify user of successful creation, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.created(p, sourceUri, ProjectTab.Sources).build();

            log.info("New RDF source \"{}\" created", sourceUri);
        }
        catch (IOException e) {
            this.throwInvalidParamError(fileName, e.getLocalizedMessage());
        }
        catch (Exception e) {
            this.handleInternalError(e,
                            "Failed to create RDF source for {}", fileName);
        }
        return response;
    }

    @POST
    @Path("{id}/rdfmodify")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response modifyRdfSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("description") String description,
                            @FormDataParam("mime_type") String mimeType,
                            @FormDataParam("base_uri") URI baseUri,
                            @FormDataParam("current_source") URI sourceUri,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve source.
            RdfFileSource s = this.loadSource(RdfFileSource.class, sourceUri);
            // Update source data.
            s.setDescription(description);
            s.setSourceUrl(baseUri.toString());

            MediaType mappedType = null;
            try {
                mappedType = RdfUtils.parseMimeType(mimeType);
            }
            catch (Exception e) {
                this.throwInvalidParamError("mime_type", mimeType);
            }
            s.setMimeType(mappedType.toString());
            // Save updated source.
            Project p = s.getProject();
            this.projectManager.saveProject(p);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.redirect(p, ProjectTab.Sources).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Could not modify RDF source {}",
                                        sourceUri);
        }
        return response;
    }

    @POST
    @Path("{id}/dbupload")
    public Response uploadSqlSource(
                            @PathParam("id") String projectId,
                            @FormParam("title") String title,
                            @FormParam("description") String description,
                            @FormParam("source_url") String cnxUrl,
                            @FormParam("user") String user,
                            @FormParam("password") String password,
                            @FormParam("sql_query") String sqlQuery,
                            @FormParam("cache_duration") int cacheDuration,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            log.debug("Processing SQL source creation request for \"{}\"",
                      title);
            // Build object URIs from request path.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            URI sourceUri = new URI(projectUri.getScheme(), null,
                                    projectUri.getHost(), projectUri.getPort(),
                                    this.getSourceId(projectUri.getPath(), title),
                                    null, null);
            // Retrieve project.
            Project p = this.loadProject(projectUri);
            // Initialize new source.
            SqlSource src = this.projectManager.newSqlSource(p,
                                        sourceUri, title, description,
                                        cnxUrl, user, password,
                                        sqlQuery, cacheDuration);
            // Start iterating on source content to validate database
            // connection parameters and query.
            CloseableIterator<?> i = src.iterator();
            i.close();
            // Persist new source.
            this.projectManager.saveProject(p);
            // Notify user of successful creation, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.created(p, sourceUri, ProjectTab.Sources).build();

            log.info("New SQL source \"{}\" created", sourceUri);
        }
        catch (Exception e) {
            this.handleInternalError(e,
                            "Failed to create SQL source for {}", title);
        }
        return response;
    }

    @POST
    @Path("{id}/dbmodify")
    public Response modifySqlSource(
                            @PathParam("id") String projectId,
                            @FormParam("current_source") URI sourceUri,
                            @FormParam("title") String title,
                            @FormParam("description") String description,
                            @FormParam("source_url") String cnxUrl,
                            @FormParam("user") String user,
                            @FormParam("password") String password,
                            @FormParam("sql_query") String sqlQuery,
                            @FormParam("cache_duration") int cacheDuration,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve source.
            SqlSource s = this.loadSource(SqlSource.class, sourceUri);
            // Update source data.
            if ((s.getTitle() == null) || (! s.getTitle().equals(title))) {
                s.setTitle(title);
            }
            if ((s.getDescription() == null) ||
                                (! s.getDescription().equals(description))) {
                s.setDescription(description);
            }
            if ((s.getConnectionUrl() == null) ||
                                (! s.getConnectionUrl().equals(cnxUrl))) {
                s.setConnectionUrl(cnxUrl);
                s.setUser(user);
                s.setPassword(password);
            }
            if ((s.getQuery() == null) || (! s.getQuery().equals(sqlQuery))) {
                s.setQuery(sqlQuery);
            }
            if (s instanceof CachingSource) {
                ((CachingSource)s).setCacheDuration(cacheDuration);
            }
            // Save updated source.
            Project p = s.getProject();
            this.projectManager.saveProject(p);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.redirect(p, ProjectTab.Sources).build();
        }
        catch (Exception e) {
            this.handleInternalError(e,
                                "Failed to modify SQL source {}", sourceUri);
        }
        return response;
    }

    @POST
    @Path("{id}/sparqlupload")
    public Response uploadSparqlSource(
                            @PathParam("id") String id,
                            @FormParam("title") String title,
                            @FormParam("description") String description,
                            @FormParam("connection_url") String endpointUrl,
                            @FormParam("sparql_query") String sparqlQuery,
                            @FormParam("default_graph_uri") String defaultGraph,
                            @FormParam("user") String user,
                            @FormParam("password") String password,
                            @FormParam("cache_duration") int cacheDuration,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            log.debug("Processing SPARQL source creation request for \"{}\"",
                      title);
            // Build object URIs from request path.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            URI sourceUri = new URI(projectUri.getScheme(), null,
                                    projectUri.getHost(), projectUri.getPort(),
                                    this.getSourceId(projectUri.getPath(), title),
                                    null, null);
            // Retrieve project.
            Project p = this.loadProject(projectUri);
            // Initialize new source.
            SparqlSource src = this.projectManager.newSparqlSource(p, sourceUri,
                                        title, description,
                                        endpointUrl, sparqlQuery,
                                        cacheDuration);
            src.setDefaultGraphUri(defaultGraph);
            src.setUser(user);
            src.setPassword(password);
            // Start iterating on source content to validate database
            // connection parameters and query.
            CloseableIterator<?> i = src.iterator();
            i.close();
            // Persist new source.
            this.projectManager.saveProject(p);
            // Notify user of successful creation, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.created(p, sourceUri, ProjectTab.Sources).build();

            log.info("New SPARQL source \"{}\" created", sourceUri);
        }
        catch (Exception e) {
            this.handleInternalError(e,
                             "Failed to create SPARQL source for {}", title);
        }
        return response;
    }

    @POST
    @Path("{id}/sparqlmodify")
    public Response modifySparqlSource(
                            @PathParam("id") String projectId,
                            @FormParam("current_source") URI sourceUri,
                            @FormParam("title") String title,
                            @FormParam("description") String description,
                            @FormParam("connection_url") String endpointUrl,
                            @FormParam("sparql_query") String sparqlQuery,
                            @FormParam("default_graph_uri") String defaultGraph,
                            @FormParam("user") String user,
                            @FormParam("password") String password,
                            @FormParam("cache_duration") int cacheDuration,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve source.
            SparqlSource s = this.loadSource(SparqlSource.class, sourceUri);
            // Update source data.
            if ((s.getTitle() == null) || (! s.getTitle().equals(title))) {
                s.setTitle(title);
            }
            if ((s.getDescription() == null) ||
                                (! s.getDescription().equals(description))) {
                s.setDescription(description);
            }
            if ((s.getEndpointUrl() == null) ||
                                (! s.getEndpointUrl().equals(endpointUrl))) {
                s.setEndpointUrl(endpointUrl);
                s.setUser(user);
                s.setPassword(password);
            }
            if ((s.getQuery() == null) ||
                                (! s.getQuery().equals(sparqlQuery))) {
                s.setQuery(sparqlQuery);
            }
            if ((s.getDefaultGraphUri() == null) ||
                            (! s.getDefaultGraphUri().equals(defaultGraph))) {
                s.setDefaultGraphUri(defaultGraph);
            }
            if (s instanceof CachingSource) {
                ((CachingSource)s).setCacheDuration(cacheDuration);
            }
            // Save updated source.
            Project p = s.getProject();
            this.projectManager.saveProject(p);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.redirect(p, ProjectTab.Sources).build();
        }
        catch (Exception e) {
            this.handleInternalError(e,
                                "Failed to modify SPARQL source {}", sourceUri);
        }
        return response;
    }

    @GET
    @Path("{id}/{filename}")
    public Response getSourceData(@PathParam("id") String id,
                                  @PathParam("filename") String fileName,
                                  @Context UriInfo uriInfo,
                                  @Context Request request)
                                                throws WebApplicationException {
        String filePath = this.getProjectFilePath(id, fileName);
        Response response = Configuration.getDefault()
                                    .getBean(ResourceResolver.class)
                                    .resolveStaticResource(filePath, request);
        if (response == null) {
            throw new NotFoundException();
        }
        return response;
    }

    @GET
    @Path("{id}/source")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response displaySources(@PathParam("id") String id,
                                   @Context UriInfo uriInfo,
                                   @Context Request request)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);
            // Check data freshness HTTP headers (If-Modified-Since & ETags)
            Date lastModified = p.getModificationDate();
            if (lastModified != null) {
                response = request.evaluatePreconditions(lastModified);
            }
            if (response == null) {
                // Page not (yet) cached or staled cache. => Return page.
                response = this.displayIndexPage(Response.ok(), p);
            }
        }
        catch (Exception e) {
            this.handleInternalError(e, null);
        }
        return response.build();
    }

    @GET
    @Path("{id}/source/{srcid}")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response displaySource(@PathParam("id") String id,
                                  @PathParam("srcid") String srcId,
                                  @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);

            // Search for requested source in project.
            Source src = p.getSource(new URL(
                                this.getSourceId(projectUri, srcId)).toURI());
            if (src == null) {
                // Not found.
                throw new NotFoundException();
            }
            // Return the HTML template matching the source type.
            String template = null;
            if ((src instanceof CsvSource) || (src instanceof SqlSource)) {
                template = "RowSourceGrid.vm";
            }
            else if (src instanceof RdfSource) {
                template = "RdfSourceGrid.vm";
            }
            else {
                throw new TechnicalException("unknown.source.type",
                                             src.getClass());
            }
            response = Response.ok(this.newViewable(template, src)).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load source {}", srcId);
        }
        return response;
    }

/*
    @GET
    @Path("{id}/source/{srcid}")
    @Produces(APPLICATION_JSON)
    public StreamingOutput displaySource(
                    @PathParam("id") String id,
                    @PathParam("srcid") String srcId,
                    @QueryParam("min")  @DefaultValue("-1") int startOffset,
                    @QueryParam("max")  @DefaultValue("-1") int endOffset,
                    @Context UriInfo uriInfo) throws WebApplicationException {
        StreamingOutput out = null;
        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);

            // Search for requested source in project.
            Source src = p.getSource(new URL(
                                this.getSourceId(projectUri, srcId)).toURI());
            if (src == null) {
                // Not found.
                throw new NotFoundException();
            }
            // Return a JSON serializer for the source data.
            if (src instanceof CsvSource) {
                out = new RowJsonSerializer<String>(((CsvSource)src).iterator(),
                                                    startOffset, endOffset);
            }
            else if (src instanceof SqlSource) {
                out = new RowJsonSerializer<Object>(((SqlSource)src).iterator(),
                                                    startOffset, endOffset);
            }
            else if (src instanceof RdfSource) {
                out = new RdfJsonSerializer(((RdfSource)src).iterator(),
                                            startOffset, endOffset);
            }
            else {
                throw new TechnicalException("unknown.source.type",
                                             src.getClass());
            }
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load source {}", srcId);
        }
        return out;
    }
*/

    @GET
    @Path("{id}/source/{srcid}/delete")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response deleteSource(@PathParam("id") String projectId,
                                 @PathParam("srcid") String sourceId,
                                 @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            log.debug("Processing source deletion request for {}", sourceId);
            // Retrieve source.
            // As we can't infer the source type (CSV, SPARQL...), we have
            // to load the whole project and search it using its URI.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);
            URI u = new URI(uriInfo.getAbsolutePath().toString()
                                                     .replace("/delete", ""));
            Source s = p.getSource(u);
            // Delete source.
            this.projectManager.delete(s);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.redirect(p, ProjectTab.Sources).build();

            log.info("Source \"{}\" deleted", u);
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to delete source {}", sourceId);
        }
        return response;
    }

    @GET
    @Path("{id}/source/{srcid}")
    @Produces({ APPLICATION_RDF_XML, TEXT_TURTLE, APPLICATION_TURTLE,
                TEXT_N3, TEXT_RDF_N3, APPLICATION_N3, APPLICATION_NTRIPLES,
                APPLICATION_JSON })
    public Response describeSource(@PathParam("id") String id,
                                   @PathParam("srcid") String srcId,
                                   @Context UriInfo uriInfo,
                                   @Context Request request,
                                   @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        // Check that projects exists in internal data store.
        this.loadProject(this.newProjectId(uriInfo.getBaseUri(), id));
        // Forward request for source RDF description to the SPARQL endpoint.
        URI uri = uriInfo.getAbsolutePath();
        Configuration cfg = Configuration.getDefault();
        List<String> defGraph = Arrays.asList(cfg.getInternalRepository().name);
        return cfg.getBean(SparqlEndpoint.class)
                  .executeQuery(defGraph, null,
                                "DESCRIBE <" + uri + '>',
                                uriInfo, request, acceptHdr).build();
    }

    @GET
    @Path("{id}/ontologyupload")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Object getOntologyUploadPage(@PathParam("id") String id,
                                        @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);

            response = Response.ok(this.newViewable("projectOntoUpload.vm", p),
                                   TEXT_HTML)
                               .build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load project");
        }
        return response;
    }

    @POST
    @Path("{id}/ontologyupload")
    public Response uploadOntology(@PathParam("id") String projectId,
                                   @FormParam("source_url") URL srcUrl,
                                   @FormParam("title") String title,
                                   @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);
            // Add ontology to project.
            p.addOntology(this.projectManager.newOntology(p,
                                                        srcUrl.toURI(), title));
            // Persist new ontology.
            this.projectManager.saveProject(p);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the ontology tab of the project page.
            response = this.redirect(p, ProjectTab.Ontologies).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to add ontology {}", srcUrl);
        }
        return response;
    }

    @GET
    @Path("{id}/ontology/{ontologyTitle}/modify")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getOntologyModifyPage(
                            @PathParam("id") String id,
                            @PathParam("ontologyTitle") String ontologyTitle,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);
            // Search for requested ontology in project.
            Ontology ontology = p.getOntology(ontologyTitle);
            if (ontology == null) {
                // Not found.
                throw new NotFoundException();
            }
            Map<String, Object> args = new TreeMap<String, Object>();
            args.put("it", p);
            args.put("current", ontology);
            response = Response.ok(this.newViewable("projectOntoUpload.vm", args),
                                   TEXT_HTML).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load ontology {}",
                                        ontologyTitle);
        }
        return response;
    }

    @POST
    @Path("{id}/ontology/{ontologyTitle}/modify")
    public Response modifyOntology(@PathParam("id") String id,
            @Context UriInfo uriInfo, @FormParam("title") String title,
            @FormParam("source_url") URI source,
            @FormParam("oldTitle") String currentOntologyTitle)
            throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.loadProject(projectUri);
            // Search for requested ontology in project.
            Ontology ontology = p.getOntology(currentOntologyTitle);
            if (ontology == null) {
                // Not found.
                throw new NotFoundException();
            }
            // Update ontology data.
            ontology.setTitle(title);
            ontology.setSource(source);
            // Save updated ontology.
            this.projectManager.saveProject(p);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the ontology tab of the project page.
            response = this.redirect(p, ProjectTab.Ontologies).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to modify ontology \"{}\"",
                                        currentOntologyTitle);
        }
        return response;
    }

    @GET
    @Path("{id}/ontology/{ontologyTitle}/delete")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response deleteOntology(
                            @PathParam("id") String projectId,
                            @PathParam("ontologyTitle") String ontologyTitle,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve ontology.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);
            Ontology o = p.getOntology(ontologyTitle);
            // Delete ontology.
            this.projectManager.deleteOntology(p, o);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the ontology tab of the project page.
            response = this.redirect(p, ProjectTab.Ontologies).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to delete ontology \"{}\"",
                                        ontologyTitle);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private ResponseBuilder displayIndexPage(ResponseBuilder response,
                                             Project p) {
        Map<String, Object> args = new TreeMap<String, Object>();
        // Populate project list.
        args.put("it", this.projectManager.listProjects());
        // Display selected project.
        if (p != null) {
            // Search for modules accepting the selected project.
            Collection<UriDesc> modules = new TreeSet<UriDesc>(
                    new Comparator<UriDesc>() {
                        @Override
                        public int compare(UriDesc u1, UriDesc u2) {
                            int v = u1.getPosition() - u2.getPosition();
                            return (v != 0)? v: u1.getLabel().compareToIgnoreCase(u2.getLabel());
                        }
                    });
            for (ProjectModule m : Configuration.getDefault().getBeans(
                                                        ProjectModule.class)) {
                UriDesc modulePage = m.canHandle(p);
                if (modulePage != null) {
                    modules.add(modulePage);
                }
            }
            args.put("current", p);
            args.put("canHandle", modules);
            // Set page expiry & last modification date to force revalidation.
            Date lastModified = p.getModificationDate();
            if (lastModified != null) {
                response = response.lastModified(lastModified)
                                   .expires(lastModified);
            }
            // Force page revalidation.
            CacheControl cc = new CacheControl();
            cc.setPrivate(true);
            cc.setMustRevalidate(true);
            response = response.cacheControl(cc);
        }
        return response.entity(this.newViewable("workspace.vm", args))
                       .type(TEXT_HTML);
    }

    private Project findProject(URI uri) throws WebApplicationException {
        try {
            return this.projectManager.findProject(uri);
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
            log.error(error.getMessage(), e);
            throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
        }
    }

    private Project loadProject(URI uri) throws WebApplicationException {
        Project p = this.findProject(uri);
        if (p == null) {
            // Not found.
            throw new NotFoundException(uri);
        }
        return p;
    }

    private <C extends Source> C findSource(Class<C> clazz, URI uri)
                                                throws WebApplicationException {
        try {
            return this.projectManager.findSource(clazz, uri);
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
            log.error(error.getMessage(), e);
            throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
        }
    }

    private <C extends Source> C loadSource(Class<C> clazz, URI uri)
                                                throws WebApplicationException {
        C s = this.findSource(clazz, uri);
        if (s == null) {
            // Not found.
            throw new NotFoundException(uri);
        }
        return s;
    }

    /**
     * Return a viewable for the specified template, populated with the
     * specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated viewable.
     */
    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable(TEMPLATE_PATH + templateName, it);
    }

    private URI newProjectId(URI baseUri, String name) {
        URI u = null;
        try {
            u = new URL(baseUri.toURL(),
                           REL_PROJECT_PATH + urlify(name)).toURI();
        }
        catch (Exception e) {
            this.throwInvalidParamError("id", name);
        }
        return u;
    }

    private File getFileStorage(String path) {
        return new File(Configuration.getDefault().getPublicStorage(), path);
    }

    private String getProjectFilePath(String projectId, String fileName) {
        StringBuilder buf = new StringBuilder(80);
        buf.append(REL_PROJECT_PATH).append(projectId);
        if (isSet(fileName)) {
            buf.append('/').append(fileName);
        }
        return buf.toString();
    }

    private String getSourceId(URI projectUri, String sourceName) {
        return this.getSourceId(projectUri.toString(), sourceName);
    }

    private String getSourceId(String projectUri, String sourceName) {
        return projectUri + SOURCE_PATH + urlify(sourceName);
    }

    /**
     * Notifies the user of successful object creation, redirecting
     * HTML clients (i.e. browsers) to the source tab of the project
     * main page.
     * @param  p     the modified project.
     * @param  uri   the URI of the object the creation of which shall
     *               be reported.
     * @param  tab   the project page tab to display or
     *               <code>null</code> (default tab).
     *
     * @return an HTTP response redirecting to the project main page.
     * @throws TechnicalException if any error occurred.
     */
    private ResponseBuilder created(Project p, URI uri, ProjectTab tab) {
        try {
            if (uri == null) {
                uri = new URI(p.getUri());
            }
            String targetUrl = (tab != null)? p.getUri() + tab.anchor:
                                              p.getUri();
            return Response.created(uri)
                           .entity(this.newViewable("redirect.vm", targetUrl))
                           .type(TEXT_HTML);
        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    /**
     * Notifies the user of successful request execution, redirecting
     * HTML clients (i.e. browsers) to the source tab of the project
     * main page.
     * @param  p     the modified project.
     * @param  tab   the project page tab to display or
     *               <code>null</code> (default tab).
     *
     * @return an HTTP response redirecting to the project main page.
     * @throws TechnicalException if any error occurred.
     */
    private ResponseBuilder redirect(Project p, ProjectTab tab) {
        try {
            return Response.seeOther(new URI(
                        (tab != null)? p.getUri() + tab.anchor: p.getUri()));
        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    private static void fileCopy(InputStream src, File dest)
                                                        throws IOException {
        OutputStream out = null;
        try {
            dest.createNewFile();
            out = new FileOutputStream(dest);

            byte buffer[] = new byte[4096];
            int l;
            while ((l = src.read(buffer)) != -1) {
                out.write(buffer, 0, l);
            }
            out.flush();
        }
        catch (IOException e) {
            dest.delete();
            throw e;
        }
        finally {
            try { src.close(); } catch (Exception e) { /* Ignore... */ }
            if (out != null) {
                try { out.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
    }

    private void throwInvalidParamError(String name, Object value) {
        TechnicalException error = (value != null)?
                new TechnicalException("ws.invalid.param.error", name, value):
                new TechnicalException("ws.missing.param", name);
        throw new WebApplicationException(
                                Response.status(Status.BAD_REQUEST)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
    }

    private void handleInternalError(Exception e,
                                     String logMsg, Object... logArgs)
                                                throws WebApplicationException {
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        if (e instanceof EntityNotFoundException) {
            throw new NotFoundException();
        }
        else {
            if (isSet(logMsg)) {
                log.fatal(logMsg, e, logArgs);
            }
            else {
                log.fatal(e.getMessage(), e);
            }
            TechnicalException error = new TechnicalException(
                                    "ws.internal.error", e, e.getMessage());
            throw new WebApplicationException(
                            Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .type(MediaTypes.TEXT_PLAIN_TYPE)
                                    .entity(error.getMessage()).build());
        }
    }

/*
    private final static class RowJsonSerializer<T> implements StreamingOutput
    {
        private final CloseableIterator<Row<T>> data;
        private final int min;
        private final int max;

        public RowJsonSerializer(CloseableIterator<Row<T>> data,
                                                            int min, int max) {
            this.data = data;
            this.min  = min;
            this.max  = (max > 0)? max: Integer.MAX_VALUE;
        }

        @Override
        public void write(OutputStream out)
                                throws IOException, WebApplicationException {
            try {
                GridJsonWriter writer = new GridJsonWriter(out);

                int i = 0;
                while ((this.data.hasNext()) && (i < max)) {
                    Row<T> row = this.data.next();
                    if (i == 0) {
                        writer.startQueryResult(
                                        new LinkedList<String>(row.keys()));
                    }
                    i++;
                    if (i >= min) {
                        // Within requested range.
                        writer.handleRow(row);
                    }
                    // Else: Not yet in range => Skip.
                }
                writer.endQueryResult();
            }
            catch (TupleQueryResultHandlerException e) {
                Throwable cause = e;
                while ((cause = cause.getCause()) != null) {
                    if (cause instanceof IOException) {
                        throw (IOException)cause;
                    }
                }
                throw new IOException(e);
            }
            finally {
                this.data.close();
            }
        }
    }

    private final static class RdfJsonSerializer implements StreamingOutput
    {
        private final CloseableIterator<Statement> data;
        private final int min;
        private final int max;

        public RdfJsonSerializer(CloseableIterator<Statement> data,
                                                            int min, int max) {
            this.data = data;
            this.min  = min;
            this.max  = (max > 0)? max: Integer.MAX_VALUE;
        }

        @Override
        public void write(OutputStream out)
                                throws IOException, WebApplicationException {
            try {
                GridJsonWriter writer = new GridJsonWriter(out);
                writer.startRDF();

                int i = 0;
                while ((this.data.hasNext()) && (i < max)) {
                    i++;
                    Statement s = this.data.next();
                    if (i >= min) {
                        // Within requested range.
                        writer.handleStatement(s);
                    }
                    // Else: not yet in range => Skip.
                }
                writer.endRDF();
            }
            catch (RDFHandlerException e) {
                Throwable cause = e;
                while ((cause = cause.getCause()) != null) {
                    if (cause instanceof IOException) {
                        throw (IOException)cause;
                    }
                }
                throw new IOException(e);
            }
            finally {
                this.data.close();
            }
        }
    }
*/
}

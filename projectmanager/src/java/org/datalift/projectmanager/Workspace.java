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


import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import javax.persistence.EntityNotFoundException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
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
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.xml.parsers.SAXParserFactory;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.Response.Status.*;

import org.openrdf.model.Literal;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import static org.openrdf.query.QueryLanguage.SPARQL;

import com.google.gson.Gson;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CachingSource;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.DuplicateObjectKeyException;
import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.ShpSource;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.SqlSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.XmlSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.project.ProjectModule.UriDesc;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.sparql.SparqlEndpoint.DescribeType;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.io.FileUtils;
import org.datalift.fwk.util.web.Charsets;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.*;


/**
 * The web resource providing an HTML interface for managing
 * data-lifting projects.
 *
 * @author hdevos
 */
@Path(Workspace.PROJECT_URI_PREFIX)
public class Workspace extends BaseModule
{
    //-------------------------------------------------------------------------
    // Project page tabs enumeration
    //-------------------------------------------------------------------------

    /**
     * Project page tabs enumeration.
     */
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

    /**
     * The (optional) configuration property holding the path of
     * the RDF file to load available project licenses from.
     */
    public final static String LICENSES_FILE_PROPERTY = "project.licenses.file";
    /** The default license file, embedded in module JAR. */
    private final static String DEFAULT_LICENSES_FILE = "default-licenses.ttl";

    /** The prefix for the URI of the project objects. */
    public final static String PROJECT_URI_PREFIX = "project";
    /** The prefix for the URI of the source objects, within projects. */
    public final static String SOURCE_URI_PREFIX  = "source";
    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = PROJECT_URI_PREFIX;

    /** The relative path prefix for project objects and resources. */
    private final static String REL_PROJECT_PATH = PROJECT_URI_PREFIX + '/';
    /** The relative path prefix for source objects, within projects. */
    private final static String SOURCE_PATH = "/" + SOURCE_URI_PREFIX  + '/';
    /** The path prefix for HTML page Velocity templates. */
    private final static String TEMPLATE_PATH = "/" + MODULE_NAME  + '/';

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /**
     * The licenses available for new projects.
     */
    private final static Map<URI,License> licenses =
                                            new LinkedHashMap<URI,License>();

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
    public void init(Configuration configuration) {
        // Load licenses.
        this.loadLicenses(configuration.getProperty(LICENSES_FILE_PROPERTY),
                          configuration);
    }

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
                                @FormParam("license") URI license,
                                @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;

        URI projectId = this.newProjectId(uriInfo.getBaseUri(), title);
        try {
            // Create new project.
            Project p = this.projectManager.newProject(projectId, title,
                                                       description, license);
            // Persist project to RDF store.
            this.projectManager.saveProject(p);
            // Notify user of successful creation, redirecting HTML clients
            // (browsers) to the project page.
            response = this.created(p, null, null).build();
        }
        catch (Exception e) {
            this.handleInternalError(e,
                "Failed to create new project {} (\"{}\")", projectId, title);
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
            Project p = null;
            if (id != null) {
                // Retrieve project.
                p = this.loadProject(this.newProjectId(uriInfo.getBaseUri(), id));
            }
            // Else: new project.

            // Display project modification page.
            TemplateModel view = this.newView("workspaceModifyProject.vm", p);
            view.put("licenses", licenses.values());
            response = Response.ok(view, TEXT_HTML).build();
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
                    @FormParam("license") URI license,
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
            if (license != null) {
                if (! p.getLicense().equals(license)) {
                    p.setLicense(license);
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
            this.projectManager.deleteProject(this.loadProject(uri));
            // Notify user of successful deletion, redirecting HTML clients
            // (browsers) to the project page.
            URI targetUri = uriInfo.getBaseUriBuilder()
                                   .path(PROJECT_URI_PREFIX).build();
            response = Response.seeOther(targetUri).build();
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
        return this.getSourceIdModifyPage(id, null, uriInfo);
    }

    @GET
    @Path("{id}/source/{srcid}/modify")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getSourceIdModifyPage(@PathParam("id") String id,
                                          @PathParam("srcid") String srcId,
                                          @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            URI srcUri = null;
            if (isSet(srcId)) {
                srcUri = new URI(this.getSourceId(
                    this.newProjectId(uriInfo.getBaseUri(), id), srcId));
            }
            response = this.getSourceUriModifyPage(id, srcUri, uriInfo);
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load source {}", srcId);
        }
        return response;
    }

    @GET
    @Path("{id}/source/modify")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getSourceUriModifyPage(@PathParam("id") String id,
                                           @QueryParam("uri") URI srcUri,
                                           @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        URI prjUri = this.newProjectId(uriInfo.getBaseUri(), id);
        Project p = null;
        try {
            p = this.loadProject(prjUri);
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load project {}", prjUri);
        }
        try {
            // Prepare model for building view.
            TemplateModel view = this.newView("projectSourceUpload.vm", p);
            view.put("charsets", Charsets.availableCharsets);
            view.put("rdfFormats", RdfFormat.values());
            view.put("sep", Separator.values());

            // Search for requested source in project (if specified).
            if (srcUri != null) {
                Source src = p.getSource(srcUri);
                if (src == null) {
                    // Not found.
                    this.sendError(NOT_FOUND, srcUri.toString());
                }
                view.put("current", src);
            }
            response = Response.ok(view, TEXT_HTML).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load source {}", srcUri);
        }
        return response;
    }

    @POST
    @Path("{id}/csvupload")
    @Consumes(MULTIPART_FORM_DATA)
    public Response uploadCsvSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("description") String description,
                            @FormDataParam("source") InputStream fileData,
                            @FormDataParam("source")
                                    FormDataContentDisposition fileDisposition,
                            @FormDataParam("file_url") String sourceUrl,
                            @FormDataParam("charset") String charset,
                            @FormDataParam("separator") String separator,
                            @FormDataParam("title_row") String titleRow,
                            @FormDataParam("quote") String quote,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        if (!isSet(separator)) {
            this.throwInvalidParamError("separator", separator);
        }
        Charset encoding = null;
        if (isSet(charset)) {
            try {
                encoding = Charset.forName(charset);
            }
            catch (Exception e) {
                this.throwInvalidParamError("charset", charset);
            }
        }
        Response response = null;

        String fileName = null;
        URL fileUrl = null;
        File localFile = null;
        if (! isBlank(sourceUrl)) {
            // Not uploaded data. => A file URL must be provided.
            try {
                fileUrl = new URL(sourceUrl);
                fileName = this.extractFileName(fileUrl, "csv");
                // Reset input stream to force downloading data from URL.
                fileData = null;
            }
            catch (Exception e) {
                // Conversion of source base URI to URL failed.
                log.error("Failed to parse URL {}", e, sourceUrl);
                this.throwInvalidParamError("file_url",
                                    sourceUrl + " (" + e.getMessage() + ')');
            }
        }
        else {
            fileName = fileDisposition.getFileName();
            if (isBlank(fileName)) {
                this.throwInvalidParamError("source", null);
            }
        }
        // Else: File data have been uploaded.

        log.debug("Processing CSV source creation request for {}", fileName);
        boolean deleteFiles = false;
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
            localFile = this.getFileStorage(filePath);
            this.getFileData(fileData, fileUrl, localFile, uriInfo);

            Separator sep = Separator.valueOf(separator);
            boolean hasTitleRow = ((titleRow != null) &&
                                   (titleRow.toLowerCase().equals("on")));
            // Initialize new source.
            CsvSource src = this.projectManager.newCsvSource(p, sourceUri,
                                        fileName, description,
                                        filePath, sep.getValue(), hasTitleRow);
            if (encoding != null) {
                src.setEncoding(encoding.name());
            }
            src.setQuote(quote);
            // Iterate on source content to validate uploaded file.
            int n = 0;
            CloseableIterator<?> i = src.iterator();
            try {
                for (; i.hasNext(); ) {
                    n++;
                    if (n > 1000) break;        // 1000 lines is enough!
                    i.next();   // Throws TechnicalException if data is invalid.
                }
            }
            catch (Exception e) {
                throw new IOException("Invalid or empty source data", e);
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
            deleteFiles = true;
            String src = (fileData != null)? fileName:
                        (fileUrl != null)? fileUrl.toString(): "file_url";
            log.fatal("Failed to save source data from {}", e, src);
            this.throwInvalidParamError(src, e.getLocalizedMessage());
        }
        catch (Exception e) {
            deleteFiles = true;
            this.handleInternalError(e,
                            "Failed to create CVS source for {}", fileName);
        }
        finally {
            if ((deleteFiles) && (localFile != null)) {
                localFile.delete();
            }
        }
        return response;
    }

    @POST
    @Path("{id}/csvmodify")
    @Consumes(MULTIPART_FORM_DATA)
    public Response modifyCsvSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("current_source") URI sourceUri,
                            @FormDataParam("description") String description,
                            @FormDataParam("charset") String charset,
                            @FormDataParam("separator") String separator,
                            @FormDataParam("title_row") String titleRow,
                            @FormDataParam("quote") String quote,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        if (!isSet(separator)) {
            this.throwInvalidParamError("separator", separator);
        }
        Charset encoding = null;
        if (isSet(charset)) {
            try {
                encoding = Charset.forName(charset);
            }
            catch (Exception e) {
                this.throwInvalidParamError("charset", charset);
            }
        }
        Response response = null;

        try {
            // Retrieve source.
            Project p = this.loadProject(uriInfo, projectId);
            CsvSource s = this.loadSource(p, sourceUri, CsvSource.class);
            // Update source data.
            s.setDescription(description);
            if (encoding != null) {
                s.setEncoding(encoding.name());
            }
            boolean hasTitleRow = ((titleRow != null) &&
                                   (titleRow.toLowerCase().equals("on")));
            s.setTitleRow(hasTitleRow);
            s.setSeparator(separator);
            if (isSet(quote)) {
                s.setQuote(quote);
            }
            // Save updated source.
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
    @Consumes(MULTIPART_FORM_DATA)
    public Response uploadRdfSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("description") String description,
                            @FormDataParam("base_uri") URI baseUri,
                            @FormDataParam("source") InputStream fileData,
                            @FormDataParam("source")
                                    FormDataContentDisposition fileDisposition,
                            @FormDataParam("file_url") String sourceUrl,
                            @FormDataParam("mime_type") String mimeType,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        if (fileData == null) {
            this.throwInvalidParamError("source", null);
        }
        Response response = null;

        String fileName = null;
        URL fileUrl = null;
        File localFile = null;
        if (! isBlank(sourceUrl)) {
            // Not uploaded data. => A file URL must be provided.
            try {
                fileUrl = new URL(sourceUrl);
                fileName = this.extractFileName(fileUrl, "rdf");
                // Reset input stream to force downloading data from URL.
                fileData = null;
            }
            catch (Exception e) {
                // Conversion of source base URI to URL failed.
                log.error("Failed to parse URL {}", e, sourceUrl);
                this.throwInvalidParamError("file_url",
                                    sourceUrl + " (" + e.getMessage() + ')');
            }
        }
        else {
            fileName = fileDisposition.getFileName();
            if (isBlank(fileName)) {
                this.throwInvalidParamError("source", null);
            }
        }
        // Else: File data have been uploaded.

        log.debug("Processing RDF source creation request for {}", fileName);
        boolean deleteFiles = false;
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
            localFile = this.getFileStorage(filePath);
            this.getFileData(fileData, fileUrl, localFile, uriInfo);
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
                    if (n > 100000) break;      // 100000 triples is enough!
                    i.next();   // Throws TechnicalException if data is invalid.
                }
            }
            catch (Exception e) {
                throw new IOException("Invalid or empty source data", e);
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
            deleteFiles = true;
            String src = (fileData != null)? fileName:
                        (fileUrl != null)? fileUrl.toString(): "file_url";
            log.fatal("Failed to save source data from {}", e, src);
            this.throwInvalidParamError(src, e.getLocalizedMessage());
        }
        catch (Exception e) {
            deleteFiles = true;
            this.handleInternalError(e,
                            "Failed to create RDF source for {}", fileName);
        }
        finally {
            if ((deleteFiles) && (localFile != null)) {
                localFile.delete();
            }
        }
        return response;
    }

    @POST
    @Path("{id}/rdfmodify")
    @Consumes(MULTIPART_FORM_DATA)
    public Response modifyRdfSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("current_source") URI sourceUri,
                            @FormDataParam("description") String description,
                            @FormDataParam("mime_type") String mimeType,
                            @FormDataParam("base_uri") URI baseUri,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve source.
            Project p = this.loadProject(uriInfo, projectId);
            RdfFileSource s = this.loadSource(p, sourceUri, RdfFileSource.class);
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
            Project p = this.loadProject(uriInfo, projectId);
            SqlSource s = this.loadSource(p, sourceUri, SqlSource.class);
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
                            @PathParam("id") String projectId,
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
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
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
            Project p = this.loadProject(uriInfo, projectId);
            SparqlSource s = this.loadSource(p, sourceUri, SparqlSource.class);
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

    @POST
    @Path("{id}/xmlupload")
    @Consumes(MULTIPART_FORM_DATA)
    public Response uploadXmlSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("description") String description,
                            @FormDataParam("source") InputStream fileData,
                            @FormDataParam("source")
                                    FormDataContentDisposition fileDisposition,
                            @FormDataParam("file_url") String sourceUrl,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;

        String fileName = null;
        URL fileUrl = null;
        File localFile = null;
        if (! isBlank(sourceUrl)) {
            // Not uploaded data. => A file URL must be provided.
            try {
                fileUrl = new URL(sourceUrl);
                fileName = this.extractFileName(fileUrl, "xml");
                // Reset input stream to force downloading data from URL.
                fileData = null;
            }
            catch (Exception e) {
                // Conversion of source base URI to URL failed.
                log.error("Failed to parse URL {}", e, sourceUrl);
                this.throwInvalidParamError("file_url",
                                    sourceUrl + " (" + e.getMessage() + ')');
            }
        }
        else {
            fileName = fileDisposition.getFileName();
            if (isBlank(fileName)) {
                this.throwInvalidParamError("source", null);
            }
        }
        // Else: File data have been uploaded.

        log.debug("Processing XML source creation request for {}", fileName);
        boolean deleteFiles = false;
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
            localFile = this.getFileStorage(filePath);
            this.getFileData(fileData, fileUrl, localFile, uriInfo);
            // Initialize new source.
            XmlSource src = this.projectManager.newXmlSource(p, sourceUri,
                                            fileName, description, filePath);
            // Parse XML file content to validate well-formedness.
            try {
                SAXParserFactory spf = SAXParserFactory.newInstance();
                spf.setValidating(false);
                spf.setNamespaceAware(true);
                XMLReader parser = spf.newSAXParser().getXMLReader();
                parser.setContentHandler(new DefaultHandler());
                parser.parse(new InputSource(src.getInputStream()));
            }
            catch (Exception e) {
                throw new IOException("Invalid or empty source data", e);
            }
            // Persist new source.
            this.projectManager.saveProject(p);
            // Notify user of successful creation, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.created(p, sourceUri, ProjectTab.Sources).build();

            log.info("New XML source \"{}\" created", sourceUri);
        }
        catch (IOException e) {
            deleteFiles = true;
            String src = (fileData != null)? fileName:
                        (fileUrl != null)? fileUrl.toString(): "file_url";
            log.fatal("Failed to save source data from {}", e, src);
            this.throwInvalidParamError(src, e.getLocalizedMessage());
        }
        catch (Exception e) {
            deleteFiles = true;
            this.handleInternalError(e,
                            "Failed to create XML source for {}", fileName);
        }
        finally {
            if ((deleteFiles) && (localFile != null)) {
                localFile.delete();
            }
        }
        return response;
    }

    @POST
    @Path("{id}/xmlmodify")
    @Consumes(MULTIPART_FORM_DATA)
    public Response modifyXmlSource(
                            @PathParam("id") String projectId,
                            @FormDataParam("current_source") URI sourceUri,
                            @FormDataParam("description") String description,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;

        try {
            // Retrieve source.
            Project p = this.loadProject(uriInfo, projectId);
            XmlSource s = this.loadSource(p, sourceUri, XmlSource.class);
            // Update source data.
            s.setDescription(description);
            // Save updated source.
            this.projectManager.saveProject(p);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.redirect(p, ProjectTab.Sources).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Could not modify XML source {}",
                                        sourceUri);
        }
        return response;
    }

    @POST
	@Path("{id}/shpupload")
	@Consumes(MULTIPART_FORM_DATA)
	public Response uploadShpSource(
			@PathParam("id") String projectId,
			@FormDataParam("description") String description,
			@FormDataParam("source1") InputStream shpData,
			@FormDataParam("source1")
			        FormDataContentDisposition shpDisposition,
			@FormDataParam("source2") InputStream prjData,
			@FormDataParam("source3") InputStream shxData,
			@FormDataParam("source4") InputStream dbfData,
			@Context UriInfo uriInfo)
		                                throws WebApplicationException {
		// Extract common file root name from main (SHP) file.
		String fileRoot = shpDisposition.getFileName();
		int sep = fileRoot.lastIndexOf('.');
		if (sep > 0) {
		    fileRoot = fileRoot.substring(0, sep);
		}
		// Enforce strict naming convention (same root name) for all
		// files composing the Shapefile, ignoring user-provided
		// filenames for all files but the main(SHP) one.
		String[] fileNames = new String[] {
		                fileRoot + ".shp", fileRoot + ".shx",
		                fileRoot + ".dbf", fileRoot + ".prj" };
		InputStream[] fileData = new InputStream[] {
		                shpData, shxData, dbfData, prjData };
		// Check that both file name and data are present for all files.
		for (int i=0; i<4; i++) {
		    if ((fileData[i] == null) || (isBlank(fileNames[i]))) {
			this.throwInvalidParamError("source" + i, null);
		    }
		}
		Response response = null;
		File[] localFiles = new File[4];
		String title = fileNames[0];

		log.debug("Processing Shapefile source creation request for {}",
		          title);
		boolean deleteFiles = false;
		try {
		    // Retrieve project.
		    URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
		    Project p = this.loadProject(projectUri);

		    String[] paths = new String[4];
		    int i = 0;
		    try {
		        for (i=0; i<4; i++) {
		            // Save new source data to public project storage.
		            paths[i] = this.getProjectFilePath(projectId,
		                                               fileNames[i]);
		            localFiles[i] = this.getFileStorage(paths[i]);
		            this.getFileData(fileData[i], null,
		                             localFiles[i], uriInfo);
		        }
		    }
		    catch (IOException e) {
		        String src = (fileData[i] != null)? fileNames[i]:
		                                            "source" + i;
		        log.fatal("Failed to save data from {}", e, src);
		        this.throwInvalidParamError(src, e.getLocalizedMessage());
		    }
		    // Build object URIs from request path.
		    URI srcUri = new URI(projectUri.getScheme(), null,
                                    projectUri.getHost(), projectUri.getPort(),
                                    this.getSourceId(projectUri.getPath(), fileNames[0]),
                                    null, null);
		    // Initialize & persist new source.
		    this.projectManager.newShpSource(p, srcUri, title,
		        description, paths[0], paths[1], paths[2], paths[3]);
		    this.projectManager.saveProject(p);
		    // Notify user of successful creation, redirecting HTML clients
		    response = this.created(p, srcUri, ProjectTab.Sources)
		                   .build();
		    log.info("New Shapefile source \"{}\" created", title);
		}
		catch (Exception e) {
		    deleteFiles = true;
		    this.handleInternalError(e,
		            "Failed to create Shapefile source for {}", title);
		}
		finally {
		    if (deleteFiles) {
		        for (File f : localFiles) {
		            if (f != null) {
		                f.delete();
		            }
		        }
		    }
		}
		return response;
	}

	@POST
	@Path("{id}/shpmodify")
	@Consumes(MULTIPART_FORM_DATA)
	public Response modifyShpSource(
			@PathParam("id") String projectId,
			@FormDataParam("current_source") URI sourceUri,
			@FormDataParam("description") String description,
			@Context UriInfo uriInfo)
					throws WebApplicationException {
		Response response = null;
		try {
			// Retrieve source.
			Project p = this.loadProject(uriInfo, projectId);
			ShpSource s = this.loadSource(p, sourceUri, ShpSource.class);
			// Update source data.
			s.setDescription(description);
			// Save updated source.
			this.projectManager.saveProject(p);
			// Notify user of successful update, redirecting HTML clients
			// (browsers) to the source tab of the project page.
			response = this.redirect(p, ProjectTab.Sources).build();
		}
		catch (Exception e) {
			this.handleInternalError(e,
			    "Could not modify Shapefile source {}", sourceUri);
		}
		return response;
	}

	@POST
	@Path("{id}/gmlupload")
	@Consumes(MULTIPART_FORM_DATA)
	public Response uploadGmlSource(
			@PathParam("id") String projectId,
			@FormDataParam("description") String description,
			@FormDataParam("source1") InputStream fileData1,
			@FormDataParam("source1")
			FormDataContentDisposition fileDisposition1,
			@FormDataParam("source2") InputStream fileData2,
			@FormDataParam("source2")
			FormDataContentDisposition fileDisposition2,
			@Context UriInfo uriInfo)
					throws WebApplicationException {
		if (fileData1 == null) {
			this.throwInvalidParamError("source1", null);
		}
		if (fileData2 == null) {
			this.throwInvalidParamError("source2", null);
		}

		Response response = null;

		String fileName1 = null;
		String fileName2 = null;

		URL fileUrl1 = null;
		URL fileUrl2 = null;

		File localFile1 = null;
		File localFile2 = null;

		fileName1 = fileDisposition1.getFileName();
		if (isBlank(fileName1)) {
			this.throwInvalidParamError("source1", null);
		}

		fileName2 = fileDisposition2.getFileName();
		if (isBlank(fileName2)) {
			this.throwInvalidParamError("source2", null);
		}

		log.debug("Processing GML source creation request for {}", fileName1);
		log.debug("Processing XSD source creation request for {}", fileName2);

		try {
			// Build object URIs from request path.
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
			URI sourceUri1 = new URI(projectUri.getScheme(), null,
					projectUri.getHost(), projectUri.getPort(),
					this.getSourceId(projectUri.getPath(), fileName1),
					null, null);

			// Retrieve project.
			Project p = this.loadProject(projectUri);
			// Save new source data to public project storage.
			String filePath1 = this.getProjectFilePath(projectId, fileName1);
			String filePath2 = this.getProjectFilePath(projectId, fileName2);
			localFile1 = this.getFileStorage(filePath1);
			localFile2 = this.getFileStorage(filePath2);
			this.getFileData(fileData1, fileUrl1, localFile1, uriInfo);
			this.getFileData(fileData2, fileUrl2, localFile2, uriInfo);
			// Initialize new source.
			this.projectManager.newGmlSource(p, sourceUri1, fileName1, description, filePath1);
			//this.projectManager.newGmlSource(p, sourceUri2, fileName2, description, filePath2);
			// Persist new source.
			this.projectManager.saveProject(p);
			// Notify user of successful creation, redirecting HTML clients
			response = this.created(p, sourceUri1, ProjectTab.Sources).build();

			log.info("New GML source \"{}\" created", sourceUri1);
		}
		catch (IOException e) {
			if (localFile1 != null) {
				localFile1.delete();
			}
			if (localFile2 != null) {
				localFile2.delete();
			}
			String src1 = (fileData1 != null)? fileName1: (fileUrl1 != null)? fileUrl1.toString(): "file_url1";
			log.fatal("Failed to save source data from {}", e, src1);
			this.throwInvalidParamError(src1, e.getLocalizedMessage());
			String src2 = (fileData2 != null)? fileName2: (fileUrl2 != null)? fileUrl2.toString(): "file_url2";
			log.fatal("Failed to save source data from {}", e, src2);
			this.throwInvalidParamError(src2, e.getLocalizedMessage());
		}
		catch (Exception e) {
			if (localFile1 != null) {
				localFile1.delete();
			}
			this.handleInternalError(e,
					"Failed to create GML source for {}", fileName1);
			if (localFile2 != null) {
				localFile2.delete();
			}
			this.handleInternalError(e,
					"Failed to create XSD source for {}", fileName2);
		}
		return response;
	}

	@POST
	@Path("{id}/gmlmodify")
	@Consumes(MULTIPART_FORM_DATA)
	public Response modifyGmlSource(
			@PathParam("id") String projectId,
			@FormDataParam("current_source") URI sourceUri,
			@FormDataParam("description") String description,
			@Context UriInfo uriInfo)
					throws WebApplicationException {
		Response response = null;
		try {
			// Retrieve source.
			Project p = this.loadProject(uriInfo, projectId);
			GmlSource s = this.loadSource(p, sourceUri, GmlSource.class);
			// Update source data.
			s.setDescription(description);
			// Save updated source.
			this.projectManager.saveProject(p);
			// Notify user of successful update, redirecting HTML clients
			// (browsers) to the source tab of the project page.
			response = this.redirect(p, ProjectTab.Sources).build();
		}
		catch (Exception e) {
			this.handleInternalError(e, "Could not modify GML source {}",
					sourceUri);
		}
		return response;
	}

    @GET
    @Path("{id}/{filename}")
    public Response getSourceData(@PathParam("id") String projectId,
                                  @PathParam("filename") String fileName,
                                  @Context UriInfo uriInfo,
                                  @Context Request request)
                                                throws WebApplicationException {
        String filePath = this.getProjectFilePath(projectId, fileName);
        Response response = Configuration.getDefault()
                                    .getBean(ResourceResolver.class)
                                    .resolveStaticResource(filePath, request);
        if (response == null) {
            this.sendError(NOT_FOUND, null);
        }
        return response;
    }

    @GET
    @Path("{id}/source")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response displaySources(@PathParam("id") String projectId,
                                   @Context UriInfo uriInfo,
                                   @Context Request request)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
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
    public Response displaySource(@PathParam("id") String projectId,
                                  @PathParam("srcid") String srcId,
                                  @Context UriInfo uriInfo,
                                  @Context Request request,
                                  @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        ResponseBuilder response = null;
        try {
            // Search for requested source in project.
            Project p = this.loadProject(uriInfo, projectId);
            Source src = p.getSource(uriInfo.getAbsolutePath());
            if (src == null) {
                // Not found.
                this.sendError(NOT_FOUND, null);
            }
            if (src instanceof TransformedRdfSource) {
                // Forward source description request to the SPARQL endpoint.
                Configuration cfg = Configuration.getDefault();
                response = cfg.getBean(SparqlEndpoint.class)
                              .describe(src.getUri(), DescribeType.Graph,
                                        cfg.getInternalRepository(),
                                        5000, null, null,
                                        uriInfo, request, acceptHdr);
            }
            else {
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
                response = Response.ok(this.newView(template, src));
            }
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load source {}", srcId);
        }
        return response.build();
    }

    @GET
    @Path("{id}/source/{srcid}/{prop}")
    @Produces({ APPLICATION_JSON + ";charset=UTF-8" })
    public Response displayProperty(@PathParam("id") String projectId,
                                    @PathParam("srcid") String srcId,
                                    @PathParam("prop") String prop,
                                    @Context UriInfo uriInfo,
                                    @Context Request request,
                                    @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Search for requested source in project.
            Project p = this.loadProject(uriInfo, projectId);
            Source src = p.getSource(this.getSourceId(p.getUri(), srcId));
            if (src == null) {
                // Not found.
                this.sendError(NOT_FOUND, null);
            }
            else {
                Object value = null;
                boolean resolved = false;
                BeanInfo bean = Introspector.getBeanInfo(src.getClass());
                for (PropertyDescriptor desc : bean.getPropertyDescriptors()) {
                    if (prop.equalsIgnoreCase(desc.getName())) {
                        resolved = true;
                        value = desc.getReadMethod().invoke(src);
                        break;
                    }
                }
                if (resolved) {
                    ResponseBuilder b = Response.ok();
                    if (value != null) {
                        b.entity(new Gson().toJson(value));
                    }
                    response = b.build();
                }
            }
        }
        catch (Exception e) {
            this.handleInternalError(e,
                                "Failed to resolve property {} of source {}",
                                prop, srcId);
        }
        return response;
    }

//    @GET
//    @Path("{id}/source/{srcid}/delete")
//    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
//    public Response deleteSource(@PathParam("id") String projectId,
//                                 @PathParam("srcid") String sourceId,
//                                 @Context UriInfo uriInfo)
//                                                throws WebApplicationException {
//    }

    @GET
    @Path("{id}/source/delete")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response deleteSource(@PathParam("id") String projectId,
                                 @QueryParam("uri") URI srcUri,
                                 @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            log.debug("Processing source deletion request for {}", srcUri);
            // Retrieve source.
            // As we can't infer the source type (CSV, SPARQL...), we have
            // to load the whole project and search it using its URI.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);
            // Search for requested source in project.
            Source s = p.getSource(srcUri);
            if (s == null) {
                // Not found.
                this.sendError(NOT_FOUND, srcUri.toString());
            }
            // Delete source.
            this.projectManager.delete(s);
            // Notify user of successful update, redirecting HTML clients
            // (browsers) to the source tab of the project page.
            response = this.redirect(p, ProjectTab.Sources).build();

            log.info("Source \"{}\" deleted", srcUri);
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to delete source {}", srcUri);
        }
        return response;
    }

    @GET
    @Path("{id}/source/{srcid}")
    @Produces({ APPLICATION_RDF_XML, TEXT_TURTLE, APPLICATION_TURTLE,
                TEXT_N3, TEXT_RDF_N3, APPLICATION_N3, APPLICATION_NTRIPLES,
                APPLICATION_JSON })
    public Response describeSource(@PathParam("id") String projectId,
                                   @PathParam("srcid") String srcId,
                                   @Context UriInfo uriInfo,
                                   @Context Request request,
                                   @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Check that projects exists in internal data store.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);
            // Search for requested source in project.
            URI u = uriInfo.getAbsolutePath();
            Source src = p.getSource(u);
            if (src == null) {
                // Not found.
                this.sendError(NOT_FOUND, null);
            }
            else if (! (src instanceof TransformedRdfSource)) {
                TechnicalException error =
                                    new TechnicalException("not.rdf.source");
                this.sendError(UNSUPPORTED_MEDIA_TYPE, error.getMessage());
            }
            // Forward source description request to the SPARQL endpoint.
            Configuration cfg = Configuration.getDefault();
            response = cfg.getBean(SparqlEndpoint.class)
                          .describe(u.toString(), DescribeType.Graph,
                                    cfg.getInternalRepository(),
                                    uriInfo, request, acceptHdr).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load source {}", srcId);
        }
        return response;
    }

    @GET
    @Path("{id}/ontologyupload")
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Object getOntologyUploadPage(@PathParam("id") String projectId,
                                        @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);

            response = Response.ok(this.newView("projectOntoUpload.vm", p),
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
                            @PathParam("id") String projectId,
                            @PathParam("ontologyTitle") String ontologyTitle,
                            @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);
            // Search for requested ontology in project.
            Ontology ontology = p.getOntology(ontologyTitle);
            if (ontology == null) {
                // Not found.
                this.sendError(NOT_FOUND, null);
            }
            TemplateModel view = this.newView("projectOntoUpload.vm", p);
            view.put("current", ontology);
            response = Response.ok(view, TEXT_HTML).build();
        }
        catch (Exception e) {
            this.handleInternalError(e, "Failed to load ontology {}",
                                        ontologyTitle);
        }
        return response;
    }

    @POST
    @Path("{id}/ontology/{ontologyTitle}/modify")
    public Response modifyOntology(@PathParam("id") String projectId,
            @Context UriInfo uriInfo, @FormParam("title") String title,
            @FormParam("source_url") URI source,
            @FormParam("oldTitle") String currentOntologyTitle)
            throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            URI projectUri = this.newProjectId(uriInfo.getBaseUri(), projectId);
            Project p = this.loadProject(projectUri);
            // Search for requested ontology in project.
            Ontology ontology = p.getOntology(currentOntologyTitle);
            if (ontology == null) {
                // Not found.
                this.sendError(NOT_FOUND, null);
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
            if (o == null) {
                // Not found.
                this.sendError(NOT_FOUND, ontologyTitle);
            }
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

    /**
     * Traps accesses to module static resources and redirect them
     * toward the default {@link ResourceResolver} for resolution.
     * @param  path        the relative path of the module static
     *                     resource being accessed.
     * @param  uriInfo     the request URI data (injected).
     * @param  request     the JAX-RS request object (injected).
     * @param  acceptHdr   the HTTP "Accept" header value.
     *
     * @return a {@link Response JAX-RS response} to download the
     *         content of the specified public resource.
     * @throws WebApplicationException complete with status code and
     *         plain-text error message if any error occurred while
     *         accessing the requested resource.
     */
    @GET
    @Path("static/{path: .*$}")
    public Object getStaticResource(@PathParam("path") String path,
                                    @Context UriInfo uriInfo,
                                    @Context Request request,
                                    @HeaderParam(ACCEPT) String acceptHdr)
                                                throws WebApplicationException {
        return Configuration.getDefault()
                            .getBean(ResourceResolver.class)
                            .resolveModuleResource(this.getName(),
                                                   uriInfo, request, acceptHdr);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private ResponseBuilder displayIndexPage(ResponseBuilder response,
                                             Project p) {
        // Populate view with project list.
        TemplateModel view = this.newView("workspace.vm",
                                          this.projectManager.listProjects());
        // Display selected project.
        if (p != null) {
            view.put("current", p);
            License l = licenses.get(p.getLicense());
            if (l == null) {
                l = new License(p.getLicense());        // Unknown license.
            }
            view.put("license", l.getLabel());
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
                try {
                    UriDesc modulePage = m.canHandle(p);
                    if (modulePage != null) {
                        modules.add(modulePage);
                    }
                }
                catch (Exception e) {
                    TechnicalException error = new TechnicalException(
                                                "module.internal.error", e,
                                                m.getName(), e.getMessage());
                    log.error(error.getMessage(), e);
                    // Ignore module error...
                }
            }
            view.put("canHandle", modules);
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
        return response.entity(view).type(TEXT_HTML);
    }

    private Project findProject(URI uri) throws WebApplicationException {
        Project p = null;
        try {
            p = this.projectManager.findProject(uri);
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
            log.error(error.getMessage(), e);
            this.sendError(INTERNAL_SERVER_ERROR, error.getMessage());
        }
        return p;
    }

    private Project loadProject(UriInfo uriInfo, String id)
                                                throws WebApplicationException {
        return this.loadProject(this.newProjectId(uriInfo.getBaseUri(), id));
    }

    private Project loadProject(URI uri) throws WebApplicationException {
        Project p = this.findProject(uri);
        if (p == null) {
            // Not found.
            this.sendError(NOT_FOUND, uri.toString());
        }
        return p;
    }

    private <C extends Source> C findSource(Project p, URI id, Class<C> clazz) {
        Source s = p.getSource(id);
        if ((clazz != null) && (! clazz.isAssignableFrom(s.getClass()))) {
            // Invalid source type. => not found.
            s = null;
        }
        return clazz.cast(s);
    }

    private <C extends Source> C loadSource(Project p, URI id, Class<C> clazz) {
        C s = this.findSource(p, id, clazz);
        if (s == null) {
            // Not found.
            this.sendError(NOT_FOUND, id.toString());
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
    protected final TemplateModel newView(String templateName, Object it) {
        return ViewFactory.newView(TEMPLATE_PATH + templateName, it);
    }

    private void loadLicenses(String path, Configuration cfg) {
        InputStream in = null;
        Repository r = null;
        RepositoryConnection cnx = null;
        try {
            if (isSet(path)) {
                // Licenses file specified. => Check presence.
                log.info("Loading licenses from {}", path);
                File f = new File(path);
                if (! (f.isFile() && f.canRead())) {
                    throw new FileNotFoundException(path);
                }
                in = new FileInputStream(f);
            }
            else {
                // No licenses file specified. => Use default.
                log.debug("No licenses file specified, using default licenses");
                path = DEFAULT_LICENSES_FILE;
                in = this.getClass().getClassLoader().getResourceAsStream(path);
            }
            r = cfg.newRepository(null, "sail:///", false);
            cnx = r.newConnection();
            // Load ontology into RDF store.
            cnx.add(in, "", RdfUtils.guessRdfFormatFromExtension(path)
                                    .getNativeFormat());
            cnx.close();
            // Extract licenses data from RDF triples.
            String query =
                    "PREFIX rdfs: <" + RDFS.NAMESPACE + ">\n" +
                    "PREFIX doap: <http://usefulinc.com/ns/doap#>\n" +
                    "SELECT ?uri ?label WHERE { ?s doap:license ?uri ; " +
                                              "    rdfs:label ?label . }";
            cnx = r.newConnection();
            TupleQueryResult rs = cnx.prepareTupleQuery(SPARQL, query)
                                     .evaluate();
            while (rs.hasNext()) {
                BindingSet bs = rs.next();
                URI uri = new URI(bs.getValue("uri").stringValue());
                License l = licenses.get(uri);
                if (l == null) {
                    l = new License(uri);
                    licenses.put(uri, l);
                    log.trace("Added license: {}", uri);
                }
                Literal v  = (Literal)(bs.getValue("label"));
                if (v != null) {
                    l.setLabel(v.getLanguage(), v.getLabel());
                }
            }
            rs.close();
        }
        catch (Exception e) {
            throw new TechnicalException("licenses.load.failed", e, path);
        }
        finally {
            if (cnx != null) {
                try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
            }
            if (r != null) {
                try { r.shutdown(); } catch (Exception e) { /* Ignore... */ }
            }
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
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

    private File getFileStorage(String path) throws IOException {
        File f = new File(Configuration.getDefault().getPublicStorage(), path);
        if (! f.isFile()) {
            if (! f.createNewFile()) {
                throw new TechnicalException("file.create.error", f);
            }
        }
        return f;
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

    private String extractFileName(URL url, String suffix) {
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        String fileName = null;
        String[] elts = url.getPath().split("/");
        if (elts.length > 0) {
            fileName = elts[elts.length-1];
            if ((suffix != null) && (fileName.indexOf('.') == -1)) {
                fileName += "." + suffix;
            }
        }
        log.debug("{} -> {}", url, fileName);
        return fileName;
    }

    private String isLocalFile(URL fileUrl, UriInfo uriInfo) {
        String localPath = null;

        String path = fileUrl.toString();
        String appUrl = uriInfo.getBaseUri().toString();
        if (path.startsWith(appUrl)) {
            localPath = path.substring(appUrl.length());
            if (localPath.charAt(0) == '/') {
                localPath = localPath.substring(1);
            }
        }
        return localPath;
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
                           .entity(this.newView("redirect.vm", targetUrl))
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

    private void getFileData(InputStream in, URL u,
                             File destFile, UriInfo uriInfo)
                                                        throws IOException {
        if (in != null) {
            FileUtils.save(in, destFile);
        }
        else {
            // No data input stream provided. => Check for local file.
            File f = null;

            String localPath = this.isLocalFile(u, uriInfo);
            if (localPath != null) {
                f = this.getFileStorage(localPath);
            }
            if ((f != null) && (f.isFile()) && (f.canRead())) {
                // File has already been uploaded.
                if (! f.equals(destFile)) {
                    // Make a local copy to prevent data deletion.
                    log.debug("Copying source data from \"{}\" to \"{}\"",
                              f, destFile);
                    FileUtils.copy(f, destFile, false);
                }
                // Else: already where it shall be!
            }
            else {
                // Not a local file. => Download data from the provided URL.
                FileUtils.save(u, destFile);
            }
        }
    }

    private void throwInvalidParamError(String name, Object value) {
        TechnicalException error = (value != null)?
                new TechnicalException("ws.invalid.param.error", name, value):
                new TechnicalException("ws.missing.param", name);
        this.sendError(BAD_REQUEST, error.getLocalizedMessage());
    }

    private void handleInternalError(Exception e,
                                     String logMsg, Object... logArgs)
                                                throws WebApplicationException {
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        if (e instanceof EntityNotFoundException) {
            this.sendError(NOT_FOUND, null);
        }
        if (e instanceof DuplicateObjectKeyException) {
            this.sendError(CONFLICT, e.getLocalizedMessage());
        }
        else {
            if (isSet(logMsg)) {
                log.fatal(logMsg, e, logArgs);
            }
            else {
                log.fatal(e.getMessage(), e);
            }
            TechnicalException error = null;
            if (e instanceof TechnicalException) {
                error = (TechnicalException)e;
            }
            else {
                error = new TechnicalException(
                            "ws.internal.error", e, e.getLocalizedMessage());
            }
            this.sendError(INTERNAL_SERVER_ERROR, error.getLocalizedMessage());
        }
    }
}

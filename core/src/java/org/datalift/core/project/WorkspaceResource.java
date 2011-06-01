package org.datalift.core.project;

import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import javax.persistence.EntityNotFoundException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import com.clarkparsia.utils.NamespaceUtils;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.datalift.core.TechnicalException;
import org.datalift.core.log.LogContext;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.DbSource;
import org.datalift.fwk.project.FileSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.*;

@Path("/project")
public class WorkspaceResource implements LifeCycle {
	// -------------------------------------------------------------------------
	// Constants
	// -------------------------------------------------------------------------

	private final static String MODULE_NAME = "Project";

	// -------------------------------------------------------------------------
	// Class members
	// -------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger();

	// -------------------------------------------------------------------------
	// Instance members
	// -------------------------------------------------------------------------

	/** The DataLift configuration. */
	private Configuration configuration = null;
	
	private	ProjectManager	projectManager = null;

	// -------------------------------------------------------------------------
	// LifeCycle contract support
	// -------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public void init(Configuration configuration) {
		this.configuration = configuration;
		// Register application namespaces to Empire.
		this.registerRdfNamespaces();
		
		
	}
	
	@Override
	public void postInit() {
		// TODO Auto-generated method stub
		log.debug("WorkspaceResource : adding persistent classes to Project Manager");
		this.projectManager = configuration.getBean(ProjectManager.class);
	}

	/** {@inheritDoc} */
	@Override
	public void shutdown(Configuration configuration) {
	}

	// -------------------------------------------------------------------------
	// Web services
	// -------------------------------------------------------------------------

	@GET
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getIndex() {
		LogContext.setContexts(MODULE_NAME, "project");
		return this.displayIndexPage(Response.ok(), null).build();
	}

	@GET
	@Produces(APPLICATION_JSON)
	public Collection<Project> getIndexJSON() {
		LogContext.setContexts(MODULE_NAME, "project");
		return this.projectManager.getAllProjects();
	}

	@POST
	public Response registerProject(@FormParam("title") String title,
			@FormParam("description") String description,
			@FormParam("license") String license, @Context UriInfo uriInfo) {
		LogContext.setContexts(MODULE_NAME, urlify(title));

		Response response = null;
		// Check that project is unique.
		URI projectId = this.newProjectId(uriInfo.getBaseUri(), title);
		if (this.projectManager.findProject(projectId) == null) {
			// Create new project.
			Project p = this.projectManager.newProject(
					projectId, title, description, license);
			// Persist project to RDF store.
			try {
				this.projectManager.persistProject(p);
				String uri = p.getUri();
				response = Response.created(new URI(uri))
						.entity(new Viewable("/redirect.vm", uri))
						.type(TEXT_HTML).build();
			} catch (Exception e) {
				this.handleInternalError(e, "Failed to persist project");
			}
		} else {
			log.fatal("Duplicate identifier \"{}\" for new project \"{}\"",
					urlify(title), title);
			TechnicalException error = new TechnicalException(
					"duplicate.identifier", title);
			throw new ConflictException(error.getMessage());
		}
		return response;
	}

	@GET
	@Path("add.html")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getNewProjectPage(@Context UriInfo uriInfo)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, "add.html");

		return this.getModifyProjectPage(null, uriInfo);
	}

	@GET
	@Path("{id}/modify.html")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getModifyProjectPage(@PathParam("id") String id,
			@Context UriInfo uriInfo) throws WebApplicationException {
		if (id != null) {
			LogContext.setContexts(MODULE_NAME, id + "/modify.html");
		}

		Response response = null;
		try {
			Map<String, Object> args = new TreeMap<String, Object>();
			if (id != null) {
				URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
				args.put("it", this.projectManager.getProject(projectUri));
			}
			args.put("licenses", License.values());
			response = Response
					.ok(new Viewable("/workspaceModifyProject.vm", args),
							TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load project");
		}
		return response;
	}

	@POST
	@Path("{id}")
	public Response modifyProject(@PathParam("id") String id,
			@FormParam("title") String title,
			@FormParam("description") String description,
			@FormParam("license") String license,
			@FormParam("delete") @DefaultValue("false") boolean delete,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id);

		Response response = null;
		if (delete) {
			this.deleteProject(id, uriInfo);
			response = this.getIndex();
		}
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			boolean modified = false;
			if (!StringUtils.isBlank(title)) {
				p.setTitle(title);
				modified = true;
			}
			if (!StringUtils.isBlank(description)) {
				p.setDescription(description);
				modified = true;
			}
			URI li = License.valueOf(license).uri;
			if (!p.getLicense().equals(li)) {
				p.setLicense(li);
				modified = true;
			}
			if (modified) {
				p.setDateModification(new Date());
				this.projectManager.saveProject(p);
			}
			response = Response.seeOther(projectUri).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to update project");
		}
		return response;
	}

	@DELETE
	@Path("{id}")
	public Response deleteProject(@PathParam("id") String id,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id);

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			this.projectManager.deleteProject(p);

			response = Response.ok().build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to delete project");
		}
		return response;
	}

	@GET
	@Path("{id}")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getProjectPage(@PathParam("id") String id,
			@Context UriInfo uriInfo, @Context Request request)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id);

		Project p = this.projectManager.getProject(uriInfo.getAbsolutePath());
		return this.displayIndexPage(Response.ok(), p).build();
	}

	@GET
	@Path("{id}")
	@Produces({ APPLICATION_RDF_XML, TEXT_TURTLE, APPLICATION_TURTLE, TEXT_N3,
			TEXT_RDF_N3, APPLICATION_N3, APPLICATION_NTRIPLES, APPLICATION_JSON })
	public Response getProjectDesc(@PathParam("id") String id,
			@Context UriInfo uriInfo, @Context Request request,
			@HeaderParam("Accept") String acceptHdr)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id);

		// Check that projects exists in internal data store.
		URI uri = uriInfo.getAbsolutePath();
		this.projectManager.findProject(uri);
		// Forward request for project RDF description to the SPARQL endpoint.
		List<String> defGraph = Arrays.asList(this.configuration
				.getInternalRepository().name);
		return this.configuration
				.getBean(SparqlEndpoint.class)
				.executeQuery(defGraph, null, "DESCRIBE <" + uri + '>',
						uriInfo, request, acceptHdr).build();
	}

	@GET
	@Path("{id}/srcupload.html")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Object getSourceUploadPage(@PathParam("id") String id,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/srcupload.html");

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			Map<String, Object> args = new HashMap<String, Object>();
			args.put("it", p);
			args.put("sep", Separator.values());
			response = Response.ok(
					new Viewable("/projectSourceUpload.vm", args), TEXT_HTML)
					.build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load project");
		}
		return response;
	}

	@GET
	@Path("{id}/source/{srcid}/modify.html")
	public Response getSourceModifyPage(@PathParam("id") String id,
			@PathParam("srcid") String srcId, @Context UriInfo uriInfo) {
		LogContext.setContexts(MODULE_NAME, id + "/source/" + srcId);
		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			// Search for requested source in project
			URI urlSource = new URI(uriInfo.getRequestUri().toString()
					.replace("/modify.html", ""));
			Source src = p.getSource(urlSource);
			if (src == null) {
				// Not found.
				throw new NotFoundException();
			}
			Map<String, Object> args = new HashMap<String, Object>();
			args.put("it", p);
			args.put("current", src);
			args.put("sep", Separator.values());
			response = Response.ok(
					new Viewable("/projectSourceUpload.vm", args)).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load source {}", srcId);
		}
		return response;
	}

	@POST
	@Path("{id}/csvupload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response csvUpload(
			@PathParam("id") String id,
			@FormDataParam("source") InputStream file,
			@FormDataParam("source") FormDataContentDisposition fileDisposition,
			@FormDataParam("separator") String separator,
			@FormDataParam("title_row") String titleRow,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/csvupload");
		if (file == null) {
			this.throwInvalidParamError("source", null);
		}
		if (!isSet(separator)) {
			this.throwInvalidParamError("separator", separator);
		}
		Response response = null;

		String fileName = fileDisposition.getFileName();
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			URI sourceUri = new URI(projectUri.getScheme(), null,
					projectUri.getHost(), projectUri.getPort(),
					projectUri.getPath() + this.getRelativeSourceId(fileName),
					null, null);
			this.projectManager.addCsvSource(projectUri, sourceUri, id, fileName, file, titleRow, separator);
			String redirectUrl = projectUri.toString() + "#source";
			response = Response.created(sourceUri)
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create CSV source for {}",
					fileName);
		}
		return response;
	}

	@POST
	@Path("{id}/csvuploadModify")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response csvUploadModify(
			@PathParam("id") String id,
			@FormDataParam("source") InputStream file,
			@FormDataParam("source") FormDataContentDisposition fileDisposition,
			@FormDataParam("separator") String separator,
			@FormDataParam("title_row") String titleRow,
			@FormDataParam("current_source") URI currentSourceUri,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/csvuploadModify");
		if (file == null) {
			this.throwInvalidParamError("source", null);
		}
		if (!isSet(separator)) {
			this.throwInvalidParamError("separator", separator);
		}
		Response response = null;

		String fileName = fileDisposition.getFileName();
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			this.projectManager.updateCsvSource(p, currentSourceUri, id, fileName, file, titleRow, separator);
			String redirectUrl = projectUri.toString() + "#source";
			response = Response.ok(currentSourceUri)
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create CSV source for {}",
					fileName);
		}
		return response;
	}

	@POST
	@Path("{id}/rdfupload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response rdfUpload(
			@PathParam("id") String id,
			@Context UriInfo uriInfo,
			@FormDataParam("source") InputStream file,
			@FormDataParam("source") FormDataContentDisposition fileDisposition,
			@FormDataParam("mime_type") String mimeType)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/rdfupload");
		if (file == null) {
			this.throwInvalidParamError("source", null);
		}
		String fileName = fileDisposition.getFileName();
		Response response = null;
		try {
			this.projectManager.addRdfSource(uriInfo.getBaseUri(), id,
					fileName, mimeType, file);
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create RDF source for {}",
					fileName);
		}
		return response;
	}

	@POST
	@Path("{id}/rdfuploadModify")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response rdfUploadModify(
			@PathParam("id") String id,
			@Context UriInfo uriInfo,
			@FormDataParam("source") InputStream file,
			@FormDataParam("source") FormDataContentDisposition fileDisposition,
			@FormDataParam("mime_type") String mimeType,
			@FormDataParam("current_source") URI currentSourceUri)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/rdfupload");
		if (file == null) {
			this.throwInvalidParamError("source", null);
		}
		String fileName = fileDisposition.getFileName();
		Response response = null;
		try {

			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			this.projectManager.updateRdfSource(projectUri, currentSourceUri, 
					id, mimeType, fileName, file);
			String redirectUrl = projectUri.toString() + "#source";
			response = Response.ok()
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create RDF source for {}",
					fileName);
		}
		return response;
	}

	@POST
	@Path("{id}/dbupload")
	public Response dbUpload(@PathParam("id") String id,
			@Context UriInfo uriInfo, @FormParam("database") String database,
			@FormParam("source_url") String srcUrl,
			@FormParam("title") String title, @FormParam("user") String user,
			@FormParam("request") String request,
			@FormParam("password") String password,
			@FormParam("cache_duration") String cacheDuration)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/dbupload");
		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			URI sourceUri = new URI(projectUri.getScheme(), null,
					projectUri.getHost(), projectUri.getPort(),
					projectUri.getPath() + this.getRelativeSourceId(title),
					null, null);
			this.projectManager.addDbSource(projectUri, sourceUri, title, database, 
					srcUrl, request, user, password, new Integer(cacheDuration));
			String redirectUrl = projectUri.toString() + "#source";
			response = Response.created(sourceUri)
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create SQL source for {}",
					title);
		}
		return response;
	}

	@POST
	@Path("{id}/dbuploadModify")
	public Response dbUploadModify(@PathParam("id") String id,
			@Context UriInfo uriInfo, @FormParam("database") String database,
			@FormParam("source_url") String srcUrl,
			@FormParam("title") String title, @FormParam("user") String user,
			@FormParam("request") String request,
			@FormParam("password") String password,
			@FormParam("cache_duration") String cacheDuration,
			@FormParam("current_source") URI currentSourceUri)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/dbupload");
		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			
			this.projectManager.updateDbSource(projectUri, currentSourceUri, title, database,
						user, password, request, new Integer(cacheDuration));
			String redirectUrl = projectUri.toString() + "#source";
			response = Response.ok()
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create SQL source for {}",
					title);
		}
		return response;
	}

	@GET
	@Path("{id}/{filename}")
	public Response getSourceData(@PathParam("id") String id,
			@PathParam("filename") String fileName, @Context UriInfo uriInfo,
			@Context Request request) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + '/' + fileName);

		String filePath = this.projectManager.getProjectFilePath(id, fileName);
		Response response = this.configuration.getBean(ResourceResolver.class)
				.resolveStaticResource(filePath, request);
		if (response == null) {
			throw new NotFoundException();
		}
		return response;
	}

	@GET
	@Path("{id}/source")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getSources(@PathParam("id") String id,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/source");

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			response = this.displayIndexPage(Response.ok(), p).build();
		} catch (Exception e) {
			this.handleInternalError(e, null);
		}
		return response;
	}

	@GET
	@Path("{id}/source/{srcid}")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getSourcePage(@PathParam("id") String id,
			@PathParam("srcid") String srcId, @Context UriInfo uriInfo)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/source/" + srcId);

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			// Search for requested source in project.
			Source src = p.getSource(uriInfo.getAbsolutePath());

			if (src == null) {
				// Not found.
				throw new NotFoundException();
			}
			// initialize source and return grid View
			if (src instanceof FileSource<?>) {
				((FileSource<?>) src).init(
						configuration.getPublicStorage(), uriInfo.getBaseUri());
			}
			String template = null;
			if (src instanceof CsvSource) {
				template = "/CsvSourceGrid.vm";
			} else if (src instanceof RdfSource) {
				template = "/RdfSourceGrid.vm";
			} else if (src instanceof DbSource) {
				((DbSource) src).init(this.getFileStorage(this
						.projectManager.getProjectFilePath(id, srcId)));
				template = "/DbSourceGrid.vm";
			} else {
				throw new TechnicalException("Unknown source type: {1}",
						src.getClass());
			}
			response = Response.ok(new Viewable(template, src)).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load source {}", srcId);
		}
		return response;
	}

	@GET
	@Path("{id}/source/{srcid}/delete")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response deleteSource(@PathParam("id") String id,
			@PathParam("srcid") String srcId, @Context UriInfo uriInfo)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/source/" + srcId);

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);
			String url = uriInfo.getAbsolutePath().toString()
					.replace("/delete", "");
			// Delete
			p.deleteSource(new URI(url));
			this.projectManager.saveProject(p);
			String redirectUrl = projectUri.toString() + "#source";
			response = Response.ok()
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load source {}", srcId);
		}
		return response;
	}

	@GET
	@Path("{id}/source/{srcid}")
	@Produces({ APPLICATION_RDF_XML, TEXT_TURTLE, APPLICATION_TURTLE, TEXT_N3,
			TEXT_RDF_N3, APPLICATION_N3, APPLICATION_NTRIPLES, APPLICATION_JSON })
	public Response getSourceDesc(@PathParam("id") String id,
			@PathParam("srcid") String srcId, @Context UriInfo uriInfo,
			@Context Request request, @HeaderParam("Accept") String acceptHdr)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id);

		// Check that projects exists in internal data store.
		this.projectManager.getProject(this.newProjectId(uriInfo.getBaseUri(), id));
		// Forward request for source RDF description to the SPARQL endpoint.
		URI uri = uriInfo.getAbsolutePath();
		List<String> defGraph = Arrays.asList(this.configuration
				.getInternalRepository().name);
		return this.configuration
				.getBean(SparqlEndpoint.class)
				.executeQuery(defGraph, null, "DESCRIBE <" + uri + '>',
						uriInfo, request, acceptHdr).build();
	}

	@GET
	@Path("{id}/ontologyUpload.html")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Object getOntologyUploadPage(@PathParam("id") String id,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/ontologyUpload.html");

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			response = Response.ok(new Viewable("/projectOntoUpload.vm", p),
					TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load project");
		}
		return response;
	}

	@POST
	@Path("{id}/ontologyUpload")
	public Response ontologyUpload(@PathParam("id") String id,
			@FormParam("source_url") URI srcUrl,
			@FormParam("title") String title, @Context UriInfo uriInfo)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/ontologyUpload");

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			this.projectManager.addOntology(projectUri, srcUrl, title);

			String redirectUrl = projectUri.toString() + "#ontology";
			response = Response.ok(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to add ontology {}", srcUrl);
		}
		return response;
	}

	@GET
	@Path("{id}/ontology/{ontologyTitle}/modify.html")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getOntologyModifyPage(@PathParam("id") String id,
			@PathParam("ontologyTitle") String ontologyTitle,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/ontology/" + ontologyTitle);

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			// Search for requested ontology in project.
			Ontology ontology = p.getOntology(ontologyTitle);

			if (ontology == null) {
				// Not found.
				throw new NotFoundException();
			}
			Map<String, Object> args = new TreeMap<String, Object>();
			args.put("it", p);
			args.put("current", ontology);
			response = Response.ok(new Viewable("/projectOntoUpload.vm", args),
					TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to load ontology {}",
					ontologyTitle);
		}
		return response;
	}

	@POST
	@Path("{id}/ontology/{ontologyTitle}/modify")
	public Response ontologyModify(@PathParam("id") String id,
			@Context UriInfo uriInfo, @FormParam("title") String title,
			@FormParam("source_url") URI source,
			@FormParam("oldTitle") String currentOntologyTitle)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/ontology/"
				+ currentOntologyTitle);
		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);

			// Get ontology to persistent project
			Ontology ontology = p.getOntology(currentOntologyTitle);
			if (ontology == null) {
				// Not found.
				throw new NotFoundException();
			}

			// Save informations
			ontology.setTitle(title);
			ontology.setSource(source);
			this.projectManager.saveProject(p);

			String redirectUrl = projectUri.toString() + "#ontology";
			response = Response.ok()
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create SQL source for {}",
					title);
		}
		return response;
	}

	@GET
	@Path("{id}/ontology/{ontologyTitle}/delete")
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response deleteOntology(@PathParam("id") String id,
			@PathParam("ontologyTitle") String ontologyTitle,
			@Context UriInfo uriInfo) throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/ontology/" + ontologyTitle);

		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectManager.getProject(projectUri);
			// Delete
			p.deleteOntology(ontologyTitle);
			this.projectManager.saveProject(p);
			String redirectUrl = projectUri.toString() + "#ontology";
			response = Response.ok()
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to delete source {}",
					ontologyTitle);
		}
		return response;
	}

	// -------------------------------------------------------------------------
	// Specific implementation
	// -------------------------------------------------------------------------

	private URI newProjectId(URI baseUri, String name) {
		try {
			return new URL(baseUri.toURL(), "project/" + urlify(name)).toURI();
		} catch (Exception e) {
			throw new RuntimeException("Invalid base URI: " + baseUri);
		}
	}

	private File getFileStorage(String path) {
		return new File(this.configuration.getPublicStorage(), path);
	}

	

	private String getRelativeSourceId(String sourceName) {
		return "/source/" + urlify(sourceName);
	}

	

	private ResponseBuilder displayIndexPage(ResponseBuilder response, Project p) {
		Map<String, Object> args = new TreeMap<String, Object>();
		// Populate project list.
		args.put("it", this.projectManager.getAllProjects());
		// Display selected project.
		if (p != null) {
			// Search for modules accepting the selected project.
			Collection<ProjectModule> modules = new LinkedList<ProjectModule>();
			for (ProjectModule m : this.configuration
					.getBeans(ProjectModule.class)) {
				if (m.canHandle(p) != null) {
					modules.add(m);
				}
			}
			args.put("current", p);
			args.put("canHandle", modules);
		}
		return response.entity(new Viewable("/workspace.vm", args)).type(
				TEXT_HTML);
	}

	private void throwInvalidParamError(String name, Object value) {
		TechnicalException error = new TechnicalException(
				"ws.invalid.param.error", name, value);
		throw new WebApplicationException(Response.status(Status.BAD_REQUEST)
				.type(MediaTypes.TEXT_PLAIN_TYPE).entity(error.getMessage())
				.build());
	}

	private void handleInternalError(Exception e, String logMsg,
			Object... logArgs) throws WebApplicationException {
		if (e instanceof WebApplicationException) {
			throw (WebApplicationException) e;
		}
		if (e instanceof EntityNotFoundException) {
			throw new NotFoundException();
		} else {
			if (StringUtils.isSet(logMsg)) {
				log.fatal(logMsg, e, logArgs);
			} else {
				log.fatal(e.getMessage(), e);
			}
			TechnicalException error = new TechnicalException(
					"ws.internal.error", e, e.getMessage());
			throw new WebApplicationException(Response
					.status(Status.INTERNAL_SERVER_ERROR)
					.type(MediaTypes.TEXT_PLAIN_TYPE)
					.entity(error.getMessage()).build());
		}
	}

	/**
	 * Register application namespaces to Empire.
	 * <p>
	 * Yes, Empire's Namespaces and RdfProperty annotations suck too as they use
	 * prefixes as key in a global namespace table (RdfNamespace) rather than
	 * using the namespace URI as key and consider prefixes as a local matter
	 * (local to each query and class).
	 * </p>
	 */
	private void registerRdfNamespaces() {
		for (RdfNamespace ns : RdfNamespace.values()) {
			NamespaceUtils.addNamespace(ns.prefix, ns.uri);
		}
	}

	// -------------------------------------------------------------------------
	// Object contract support
	// -------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return this.getClass().getSimpleName() + " ("
				+ this.configuration.getInternalRepository().url + ')';
	}
}

package org.datalift.core.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityNotFoundException;
import javax.persistence.spi.PersistenceProvider;
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

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.utils.NamespaceUtils;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.datalift.core.TechnicalException;
import org.datalift.core.log.LogContext;
import org.datalift.core.project.CsvSourceImpl.Separator;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.DbSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.StringUtils.*;

@Path("/project")
public class WorkspaceResource implements LifeCycle, ProjectManager {
	// -------------------------------------------------------------------------
	// Constants
	// -------------------------------------------------------------------------

	private final static String REPOSITORY_URL_PARSER = "/repositories/";
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

	private EntityManagerFactory emf = null;
	private EntityManager entityMgr = null;
	private ProjectJpaDao projectDao = null;

	// -------------------------------------------------------------------------
	// LifeCycle contract support
	// -------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public void init(Configuration configuration) {
		this.configuration = configuration;
		// Register application namespaces to Empire.
		this.registerRdfNamespaces();
		// Create Empire JPA persistence provider.
		this.emf = this.createEntityManagerFactory(configuration
				.getInternalRepository().url);
		this.entityMgr = this.emf.createEntityManager();
		log.debug("JPA persistence provider initialized for repository \"{}\"",
				configuration.getInternalRepository().name);
		// Create Data Access Object for Projects.
		this.projectDao = new ProjectJpaDao(this.entityMgr);
	}

	/** {@inheritDoc} */
	@Override
	public void shutdown(Configuration configuration) {
		// Shutdown JPA persistence provider.
		if (this.emf != null) {
			if (this.entityMgr != null) {
				this.entityMgr.close();
			}
			this.emf.close();
		}
	}

	// -------------------------------------------------------------------------
	// ProjectManager contract support
	// -------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public Project findProject(URI uri) {
		return this.projectDao.find(uri);
	}

	/** {@inheritDoc} */
	@Override
	public Collection<Project> listProjects() {
		return this.projectDao.getAll();
	}

	/** {@inheritDoc} */
	@Override
	public CsvSource newCsvSource(URI uri, String title, String filePath,
			char separator, boolean hasTitleRow) throws IOException {
		CsvSourceImpl src = new CsvSourceImpl(uri.toString());
		src.setTitle(title);
		File f = this.getFileStorage(filePath);
		if (!f.isFile()) {
			throw new FileNotFoundException(filePath);
		}
		src.setFilePath(filePath);
		src.setMimeType("text/csv");
		src.setSeparator(String.valueOf(separator));
		for (Separator s : Separator.values()) {
			if (s.value == separator) {
				src.setSeparator(s.name());
				break;
			}
		}
		src.init(this.configuration.getPublicStorage(), uri);
		return src;
	}

	/** {@inheritDoc} */
	@Override
	public RdfSource newRdfSource(URI uri, String title, String filePath,
			String mimeType) throws IOException {
		RdfSourceImpl src = new RdfSourceImpl(uri.toString());
		src.setTitle(title);
		File f = this.getFileStorage(filePath);
		if (!f.isFile()) {
			throw new FileNotFoundException(filePath);
		}
		src.setFilePath(filePath);
		src.setMimeType(mimeType);
		return src;
	}

	/** {@inheritDoc} */
	@Override
	public DbSource newDbSource(URI uri, String title, String database,
			String srcUrl, String user, String password, String request,
			int cacheDuration) {
		DbSourceImpl src = new DbSourceImpl(uri.toString());
		src.setTitle(title);
		src.setDatabase(database);
		src.setConnectionUrl(srcUrl);
		src.setUser(user);
		src.setPassword(password);
		src.setRequest(request);
		src.setCacheDuration(cacheDuration);
		return src;
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
		return this.projectDao.getAll();
	}

	@POST
	public Response registerProject(@FormParam("title") String title,
			@FormParam("description") String description,
			@FormParam("license") String license, @Context UriInfo uriInfo) {
		LogContext.setContexts(MODULE_NAME, urlify(title));

		Response response = null;
		// Check that project is unique.
		URI projectId = this.newProjectId(uriInfo.getBaseUri(), title);
		if (this.projectDao.find(projectId) == null) {
			// Create new project.
			Project p = new ProjectImpl(projectId.toString());
			p.setTitle(title);

			p.setOwner(SecurityContext.getUserPrincipal());
			p.setDescription(description);
			p.setLicense(License.valueOf(license).uri);

			Date date = new Date();
			p.setDateCreation(date);
			p.setDateModification(date);

			// Persist project to RDF store.
			try {
				this.projectDao.persist(p);
				// create Project directory in public storage
				String id = p.getUri().substring(
						p.getUri().lastIndexOf("/") + 1);
				File projectStorage = this.getFileStorage(this
						.getProjectFilePath(id, null));
				projectStorage.mkdirs();

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
				args.put("it", this.projectDao.get(projectUri));
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
			Project p = this.projectDao.get(projectUri);

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
				this.projectDao.save(p);
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
			Project p = this.projectDao.get(projectUri);

			this.projectDao.delete(p);

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

		Project p = this.projectDao.get(uriInfo.getAbsolutePath());
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
		this.projectDao.find(uri);
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
			Project p = this.projectDao.get(projectUri);

			Map<String, Object> args = new HashMap<String, Object>();
			args.put("it", p);
			args.put("sep", CsvSourceImpl.Separator.values());
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
			Project p = this.projectDao.get(projectUri);

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
			args.put("sep", CsvSourceImpl.Separator.values());
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
			Project p = this.projectDao.get(projectUri);

			// Save new source to public project storage
			String filePath = this.getProjectFilePath(id, fileName);
			File storagePath = this.getFileStorage(filePath);
			fileCopy(file, storagePath);

			// Add new source to persistent project
			Separator sep = CsvSourceImpl.Separator.valueOf(separator);
			boolean hasTitleRow = ((titleRow != null) && (titleRow
					.toLowerCase().equals("on")));

			CsvSource src = this.newCsvSource(sourceUri, fileName, filePath,
					sep.value, hasTitleRow);
			p.addSource(src);
			this.projectDao.save(p);

			String redirectUrl = projectUri.toString() + "#source";
			response = Response.created(sourceUri)
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create CVS source for {}",
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
			Project p = this.projectDao.get(projectUri);

			// Get source of project
			CsvSourceImpl source = (CsvSourceImpl) p
					.getSource(currentSourceUri);
			if (source == null) {
				// Not found.
				throw new NotFoundException();
			}

			// Delete old source
			File oldStoragePath = this.getFileStorage(source.getFilePath());
			oldStoragePath.delete();

			// Save new source to public project storage
			String filePath = this.getProjectFilePath(id, fileName);
			File storagePath = this.getFileStorage(filePath);

			fileCopy(file, storagePath);

			// Save infos to persistent project
			boolean hasTitleRow = ((titleRow != null) && (titleRow
					.toLowerCase().equals("on")));
			source.setFilePath(filePath);
			source.setTitle(fileName);
			source.setMimeType("text/csv");
			source.setSeparator(String.valueOf(separator));
			source.setTitleRow(hasTitleRow);
			this.projectDao.save(p);

			String redirectUrl = projectUri.toString() + "#source";
			response = Response.ok(currentSourceUri)
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
		} catch (Exception e) {
			this.handleInternalError(e, "Failed to create CVS source for {}",
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
		MediaType mappedType = null;
		try {
			mappedType = RdfSourceImpl.parseMimeType(mimeType);
		} catch (Exception e) {
			this.throwInvalidParamError("mime_type", mimeType);
		}
		String fileName = fileDisposition.getFileName();
		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			URI sourceUri = new URI(projectUri.getScheme(), null,
					projectUri.getHost(), projectUri.getPort(),
					projectUri.getPath() + this.getRelativeSourceId(fileName),
					null, null);
			Project p = this.projectDao.get(projectUri);

			// Save new source to public project storage
			String filePath = this.getProjectFilePath(id, fileName);
			File storagePath = this.getFileStorage(filePath);
			fileCopy(file, storagePath);

			// Add new source to persistent project
			RdfSource src = this.newRdfSource(sourceUri, fileName, filePath,
					mappedType.toString());
			p.addSource(src);
			this.projectDao.save(p);

			String redirectUrl = projectUri.toString() + "#source";
			response = Response.created(sourceUri)
					.entity(new Viewable("/redirect.vm", redirectUrl))
					.type(TEXT_HTML).build();
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
		MediaType mappedType = null;
		try {
			mappedType = RdfSourceImpl.parseMimeType(mimeType);
		} catch (Exception e) {
			this.throwInvalidParamError("mime_type", mimeType);
		}
		String fileName = fileDisposition.getFileName();
		Response response = null;
		try {

			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			Project p = this.projectDao.get(projectUri);

			// Get source to persistent project
			RdfSourceImpl src = (RdfSourceImpl) p.getSource(currentSourceUri);
			if (src == null) {
				// Not found.
				throw new NotFoundException();
			}

			// Delete old source
			File oldStoragePath = this.getFileStorage(src.getFilePath());
			oldStoragePath.delete();

			// Save new source to public project storage
			String filePath = this.getProjectFilePath(id, fileName);
			File storagePath = this.getFileStorage(filePath);
			fileCopy(file, storagePath);

			// Save infos
			src.setTitle(fileName);
			src.setMimeType(mappedType.toString());
			this.projectDao.save(p);

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
			@FormParam("cache_duration") int cacheDuration)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/dbupload");
		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);
			URI sourceUri = new URI(projectUri.getScheme(), null,
					projectUri.getHost(), projectUri.getPort(),
					projectUri.getPath() + this.getRelativeSourceId(title),
					null, null);
			Project p = this.projectDao.get(projectUri);

			// Add new source to persistent project
			DbSource src = newDbSource(sourceUri, title, database, srcUrl,
					user, password, request, cacheDuration);
			p.addSource(src);
			this.projectDao.save(p);

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
			@FormParam("cache_duration") int cacheDuration,
			@FormParam("current_source") URI currentSourceUri)
			throws WebApplicationException {
		LogContext.setContexts(MODULE_NAME, id + "/dbupload");
		Response response = null;
		try {
			URI projectUri = this.newProjectId(uriInfo.getBaseUri(), id);

			Project p = this.projectDao.get(projectUri);

			// Get source to persistent project
			DbSource src = (DbSource) p.getSource(currentSourceUri);
			if (src == null) {
				// Not found.
				throw new NotFoundException();
			}

			// Save informations
			src.setTitle(title);
			src.setDatabase(database);
			src.setUser(user);
			src.setPassword(password);
			src.setRequest(request);
			src.setCacheDuration(cacheDuration);
			this.projectDao.save(p);

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

		String filePath = this.getProjectFilePath(id, fileName);
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
			Project p = this.projectDao.get(projectUri);

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
			Project p = this.projectDao.get(projectUri);

			// Search for requested source in project.
			Source src = p.getSource(uriInfo.getAbsolutePath());

			if (src == null) {
				// Not found.
				throw new NotFoundException();
			}
			// initialize source and return grid View
			if (src instanceof BaseFileSource<?>) {
				((BaseFileSource<?>) src).init(
						configuration.getPublicStorage(), uriInfo.getBaseUri());
			}
			String template = null;
			if (src instanceof CsvSource) {
				template = "/CsvSourceGrid.vm";
			} else if (src instanceof RdfSource) {
				template = "/RdfSourceGrid.vm";
			} else if (src instanceof DbSource) {
				((DbSource) src).init(this.getFileStorage(this
						.getProjectFilePath(id, srcId)));
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
			Project p = this.projectDao.get(projectUri);
			String url = uriInfo.getAbsolutePath().toString()
					.replace("/delete", "");
			// Delete
			p.deleteSource(new URI(url));
			this.projectDao.save(p);
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
		this.projectDao.get(this.newProjectId(uriInfo.getBaseUri(), id));
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
			Project p = this.projectDao.get(projectUri);

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
			Project p = this.projectDao.get(projectUri);

			// Add ontology to persistent project
			OntologyImpl ontology = new OntologyImpl();
			ontology.setTitle(title);
			ontology.setSource(srcUrl);
			ontology.setDateSubmitted(new Date());
			ontology.setOperator(SecurityContext.getUserPrincipal());
			p.addOntology(ontology);

			this.projectDao.save(p);

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
			Project p = this.projectDao.get(projectUri);

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
			Project p = this.projectDao.get(projectUri);

			// Get ontology to persistent project
			Ontology ontology = p.getOntology(currentOntologyTitle);
			if (ontology == null) {
				// Not found.
				throw new NotFoundException();
			}

			// Save informations
			ontology.setTitle(title);
			ontology.setSource(source);
			this.projectDao.save(p);

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
			Project p = this.projectDao.get(projectUri);
			// Delete
			p.deleteOntology(ontologyTitle);
			this.projectDao.save(p);
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

	private String getProjectFilePath(String projectId, String fileName) {
		String path = "project/" + projectId;
		if (isSet(fileName)) {
			path += "/" + fileName;
		}
		return path;
	}

	private String getRelativeSourceId(String sourceName) {
		return "/source/" + urlify(sourceName);
	}

	private static void fileCopy(InputStream src, File dest) throws IOException {
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
		} catch (IOException e) {
			dest.delete();
			throw e;
		} finally {
			try {
				src.close();
			} catch (Exception e) { /* Ignore... */
			}
			if (out != null) {
				try {
					out.close();
				} catch (Exception e) { /* Ignore... */
				}
			}
		}
	}

	private ResponseBuilder displayIndexPage(ResponseBuilder response, Project p) {
		Map<String, Object> args = new TreeMap<String, Object>();
		// Populate project list.
		args.put("it", this.projectDao.getAll());
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
	 * Creates and configures a new Empire JPA EntityManagerFactory to persist
	 * objects into the specified RDF repository.
	 * 
	 * @param repository
	 *            the RDF repository to persist objects into.
	 * 
	 * @return a configured Empire EntityManagerFactory.
	 */
	private EntityManagerFactory createEntityManagerFactory(URL repository) {
		// Build Empire configuration.
		EmpireConfiguration empireCfg = new EmpireConfiguration();
		// Configure target repository.
		Map<String, String> props = empireCfg.getGlobalConfig();
		String[] repo = repository.toString().split(REPOSITORY_URL_PARSER);
		props.put("factory", "sesame");
		props.put("url", repo[0]);
		props.put("repo", repo[1]);
		// Set persistent classes and associated (custom) annotation provider.
		empireCfg.setAnnotationProvider(CustomAnnotationProvider.class);
		props.put(CustomAnnotationProvider.ANNOTATED_CLASSES_PROP,
				join(this.getPersistentClasses(), ",").replace("class ", ""));
		// Initialize Empire.
		Empire.init(empireCfg, new OpenRdfEmpireModule());
		// Create Empire JPA persistence provider.
		PersistenceProvider provider = Empire.get().persistenceProvider();
		return provider.createEntityManagerFactory("",
				new HashMap<Object, Object>());
	}

	/**
	 * Returns the list of persistent classes to be handled by Empire JPA
	 * provider.
	 * 
	 * @return the list of persistent classes.
	 */
	@SuppressWarnings("unchecked")
	private Collection<Class<?>> getPersistentClasses() {
		Collection<Class<?>> classes = new LinkedList<Class<?>>();
		classes.addAll(Arrays.asList(ProjectImpl.class, CsvSourceImpl.class,
				RdfSourceImpl.class, DbSourceImpl.class, OntologyImpl.class));
		return classes;
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

	// -------------------------------------------------------------------------
	// ProjectJpaDao nested class
	// -------------------------------------------------------------------------

	/**
	 * A JPA DAO implementation for persisting ProjectImpl objects.
	 */
	private final static class ProjectJpaDao extends GenericRdfJpaDao<Project> {
		public ProjectJpaDao(EntityManager em) {
			super(ProjectImpl.class, em);
		}

		@Override
		public Project save(Project entity) {
			entity.setDateModification(new Date());
			return super.save(entity);
		}
	}
}

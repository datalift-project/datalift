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
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
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
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.util.StringUtils;

import static org.datalift.fwk.util.StringUtils.*;


@Path("/project")
public class WorkspaceResource implements LifeCycle, ProjectManager
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String REPOSITORY_URL_PARSER = "/repositories/";
    private final static String MODULE_NAME = "Project";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift configuration. */
    private Configuration configuration = null;

    private EntityManagerFactory emf = null;
    private EntityManager entityMgr = null;
    private ProjectJpaDao projectDao = null;

    //-------------------------------------------------------------------------
    // LifeCycle contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        this.configuration = configuration;
        // Register application namespaces to Empire.
        this.registerRdfNamespaces();
        // Create Empire JPA persistence provider.
        this.emf = this.createEntityManagerFactory(
                                    configuration.getInternalRepository().url);
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

    //-------------------------------------------------------------------------
    // ProkectManager contract support
    //-------------------------------------------------------------------------

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
                                  char separator, boolean hasTitleRow)
                                                            throws IOException {
        CsvSourceImpl src = new CsvSourceImpl(uri.toString());
        src.setTitle(title);
        File f = this.getFileStorage(filePath);
        if (! f.isFile()) {
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
        if (! f.isFile()) {
            throw new FileNotFoundException(filePath);
        }
        src.setFilePath(filePath);
        src.setMimeType(mimeType);
        return src;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces(MediaType.TEXT_HTML)
    public Response getIndex() {
        LogContext.setContexts(MODULE_NAME, "project");
        
        return Response.ok(new Viewable("/workspace.vm",
                                        this.projectDao.getAll()),
                           MediaType.TEXT_HTML)
                       .build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Collection<Project> getIndexJSON() {
    	LogContext.setContexts(MODULE_NAME, "project");
        return this.projectDao.getAll();
    }

    @POST
    public Response registerProject(
                                @FormParam("title") String title,
                                @FormParam("description") String description,
                                @FormParam("license") String license,
                                @Context UriInfo uriInfo) {
        LogContext.setContexts(MODULE_NAME, "project");
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
                String id = p.getUri().substring(p.getUri().lastIndexOf("/") + 1);
                File projectStorage = this.getFileStorage(
                                            this.getProjectFilePath(id, null));
                projectStorage.mkdirs();

                return Response.created(new URI(p.getUri())).entity(
                        new Viewable("/workspace.vm",this.projectDao.getAll())
                        ).build();
            }
            catch (Exception e) {
                log.fatal("Failed to persist project {}", e, title);
               /* TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage()); */
                throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(e.getMessage()).build());
            }
        }
        else {
            /*TechnicalException error = new TechnicalException(
                                                "duplicate.identifier", title);*/
            throw new ConflictException();
        }
        
    }

    @GET
    @Path("add.html")
    public Response getNewProjectPage() {
        LogContext.setContexts(MODULE_NAME, "add");

        Map<String, Object> args = new TreeMap<String, Object>();
        args.put("licenses", License.values());
        return Response.ok(new Viewable("/workspaceAddProject.vm", args),
                           MediaType.TEXT_HTML)
                       .build();
    }
    
    @GET
    @Path("{id}/modify.html")
    @Produces(MediaType.TEXT_HTML)
    public Response getModifyProjectPage(
            @PathParam("id") String id,
            @Context UriInfo uriInfo) {
        LogContext.setContexts(MODULE_NAME, "modify");
        
        URI projectUri  = this.newProjectId(uriInfo.getBaseUri(), id);
        Project p = this.projectDao.get(projectUri);
        
        Map<String, Object> args = new TreeMap<String, Object>();
        args.put("licenses", License.values());
        args.put("it", p);
        return Response.ok(new Viewable("/workspaceModifyProject.vm",
                                        args),
                           MediaType.TEXT_HTML)
                       .build();
    }
    
    @POST
    @Path("{id}")
    public Response modifyProject(
            @PathParam("id") String id,
            @FormParam("title") String title,
            @FormParam("description") String description,
            @FormParam("license") String license,
            @FormParam("delete") @DefaultValue("false") boolean delete ,
            @Context UriInfo uriInfo)
    		throws WebApplicationException {
        LogContext.setContexts(MODULE_NAME, id);
        
        if(delete) {
        	this.deleteProject(id, uriInfo);
        	return getIndex();
        }
    	try {
            URI projectUri  = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.projectDao.get(projectUri);

            boolean modified = false;
            if(!StringUtils.isBlank(title)) {
            	p.setTitle(title);
            	modified = true;
            }
            if(!StringUtils.isBlank(description)) {
            	p.setDescription(description);
            	modified = true;
            }
            URI li = License.valueOf(license).uri;
            if(!p.getLicense().equals(li)) {
            	p.setLicense(li);
            	modified = true;
            }
            if(modified) {
	            p.setDateModification(new Date());
	            this.projectDao.save(p);
            }
            return Response.seeOther(projectUri).build();

    	}
        catch (Exception e) {
            log.fatal("Failed to modified project {}", e);
           /* TechnicalException error = new TechnicalException(
                                    "ws.internal.error", e, e.getMessage()); */
            throw new WebApplicationException(
                            Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .type(MediaTypes.TEXT_PLAIN_TYPE)
                                    .entity(e.getMessage()).build());
        }
    }
    
    @DELETE
    @Path("{id}")
    public Response deleteProject(
            @PathParam("id") String id,
            @Context UriInfo uriInfo)
    		throws WebApplicationException {
        LogContext.setContexts(MODULE_NAME, id + "/modifyProject");
    	try {
            URI projectUri  = this.newProjectId(uriInfo.getBaseUri(), id);
            Project p = this.projectDao.get(projectUri);
            
            this.projectDao.delete(p);
            
            return Response.ok().build();

    	}
        catch (Exception e) {
            log.fatal("Failed to delete project {}", e);
           /* TechnicalException error = new TechnicalException(
                                    "ws.internal.error", e, e.getMessage()); */
            throw new WebApplicationException(
                            Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .type(MediaTypes.TEXT_PLAIN_TYPE)
                                    .entity(e.getMessage()).build());
        }
    }
    
    
    @GET
    @Path("{id}")
    public Response getProject(@PathParam("id") String id,
                               @Context UriInfo uriInfo,
                               @Context Request request,
                               @Context HttpHeaders hh)
                                                throws WebApplicationException {
        LogContext.setContexts(MODULE_NAME, id);

        Response response = null;
        // Check that projects exists in internal data store.
        Project p = this.projectDao.find(uriInfo.getAbsolutePath());
        
        if (p != null) {
        	// Search for modules which can handle project
        	Collection<ProjectModule> modules = new LinkedList<ProjectModule>();
        	for (ProjectModule m : this.configuration.getBeans(ProjectModule.class)) {
        		if (m.canHandle(p) != null) {
        			modules.add(m);
        		}
        	}
        	// Build	 arguments and response
            Map<String, Object> args = new TreeMap<String, Object>();
        	args.put("it", this.projectDao.getAll());
        	args.put("current", p);
        	args.put("canHandle", modules);
        		response = Response.ok(new Viewable("/workspace.vm", args),
        				MediaType.TEXT_HTML)
        				.build();
        }
        // Else: Can't query project information.
        if (response == null) {
            throw new NotFoundException();
        }
        return response;
    }

    @GET
    @Path("{id}/srcupload.html")
    public Object srcUpload(@PathParam("id") String id,
                            @Context UriInfo uriInfo) {
        LogContext.setContexts(MODULE_NAME, id + "/srcupload");

        URI projectUri  = this.newProjectId(uriInfo.getBaseUri(), id);
        Project p = this.projectDao.get(projectUri);
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p);
        args.put("sep", CsvSourceImpl.Separator.values());
        return Response.ok(new Viewable("/projectSourceUpload.vm", args),
                MediaType.TEXT_HTML)
            .build();
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
        if (! isSet(separator)) {
            this.throwInvalidParamError("separator", separator);
        }
        try {
            String fileName = fileDisposition.getFileName();
            URI projectUri  = this.newProjectId(uriInfo.getBaseUri(), id);
            URI sourceUri   = new URI(projectUri.getScheme(), null,
                                    projectUri.getHost(), projectUri.getPort(),
                                    projectUri.getPath()
                                        + this.getRelativeSourceId(fileName),
                                    null, null);
            Project p = this.projectDao.get(projectUri);

            // Save new source to public project storage
            String filePath = this.getProjectFilePath(id, fileName);
            File storagePath = this.getFileStorage(filePath);
            fileCopy(file, storagePath);

            // Add new source to persistent project
            Separator sep = CsvSourceImpl.Separator.valueOf(separator);
            boolean hasTitleRow = ((titleRow != null) &&
                                   (titleRow.toLowerCase().equals("on")));
    
            CsvSource src = this.newCsvSource(sourceUri, fileName, filePath,
                                              sep.value, hasTitleRow);
            ((ProjectImpl)p).addSource(src);
            this.projectDao.save(p);

            return Response.created(sourceUri).entity(
                    new Viewable("/workspace.vm", this.projectDao.getAll()))
                           .build();
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
        }
        catch (Exception e) {
            this.throwInvalidParamError("mime_type", mimeType);
        }
        try {
            String fileName = fileDisposition.getFileName();
            URI projectUri  = this.newProjectId(uriInfo.getBaseUri(), id);
            URI sourceUri   = new URI(projectUri.getScheme(), null,
                                    projectUri.getHost(), projectUri.getPort(),
                                    projectUri.getPath()
                                        + this.getRelativeSourceId(fileName),
                                    null, null);
            Project p = this.projectDao.get(projectUri);

            // Save new source to public project storage
            String filePath = this.getProjectFilePath(id, fileName);
            File storagePath = this.getFileStorage(filePath);
            fileCopy(file, storagePath);

            // Add new source to persistent project
            RdfSource src = this.newRdfSource(sourceUri, fileName, filePath,
                                              mappedType.toString());
            ((ProjectImpl)p).addSource(src);
            this.projectDao.save(p);

            return Response.created(sourceUri).entity(
                    new Viewable("/workspace.vm", this.projectDao.getAll()))
                           .build();
        
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
    
    @POST
    @Path("{id}/dbupload")
    public Response dbUpload(@PathParam("id") String id,
    						@Context UriInfo uriInfo,
    						@FormParam("database") String	database,
    						@FormParam("source_url") String srcUrl, 
    						@FormParam("title") String title,
    						@FormParam("user") String user, 
    						@FormParam("request") String request,
    						@FormParam("password") String password, 
    						@FormParam("cache_duration") String cacheDuration) {
    	URI projectUri = null;
    	URI sourceUri = null;
    	try {
			projectUri = new URI(uriInfo.getBaseUri() + "project/" + id);
    		sourceUri = new URI(projectUri + "/source/" + title);
    	} catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		Project p = this.projectDao.get(projectUri);
		// Add new source to persistent project
		DbSource src = new DbSource(sourceUri.toString());
		src.setTitle(title);
		src.setDatabase(database);
		src.setConnectionUrl(srcUrl);
		src.setUser(user);
		src.setPassword(password);
		src.setRequest(request);
		src.setCacheDuration(new Integer(cacheDuration).intValue());
		((ProjectImpl)p).addSource(src);
		this.projectDao.save(p);
		try {
			return Response.created(sourceUri).entity(
				new Viewable("/workspace.vm", this.projectDao.getAll())).build();
		}
		catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}    	
    }

    @GET
    @Path("{project}/{filename}")
    public Response getSourceData(@PathParam("project") String projectId,
                                  @PathParam("filename") String fileName,
                                  @Context UriInfo uriInfo,
                                  @Context Request request)
                                                throws WebApplicationException {
        LogContext.setContexts(MODULE_NAME, projectId + '/' + fileName);

        String filePath = this.getProjectFilePath(projectId, fileName);
        Response response = this.configuration.getBean(ResourceResolver.class)
                                 .resolveStaticResource(filePath, request);
        if (response == null) {
        	throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        return response;
    }

    @GET
    @Path("{id}/source")
    public Response	getSources(@PathParam("id") String id, @Context UriInfo uriInfo) {
    	 // Get persistent project by its Id
        URI uri = null;
        try {
            uri = new URI(uriInfo.getBaseUri() + "project/" + id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Project p = this.projectDao.get(uri);
        Map<String, Object> args = new TreeMap<String, Object>();
        args.put("it", this.projectDao.getAll());
        args.put("current", p);
        args.put("sources", p.getSources());
        Response response = Response.ok(new Viewable("/workspace.vm", args),
                MediaType.TEXT_HTML)
            .build();
        if (response == null) {
        	throw new NotFoundException();
        }
        return response;
    }
    
    @GET
    @Path("{id}/source/{srcid}")
    public Response getSource(@PathParam("id") String id,
                              @PathParam("srcid") String srcId,
                              @Context UriInfo uriInfo) {
        LogContext.setContexts(MODULE_NAME, id + "/source/" + srcId);
        // Get persistent project by its Id
        URI uri = null;
        try {
            uri = new URI(uriInfo.getBaseUri() + "project/" + id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Project p = this.projectDao.get(uri);
        // Search for source in project
        Source src = null;
        String url = uriInfo.getRequestUri().toString();
        for (Source s : p.getSources()) {
            if (url.equals(s.getUri())) {
                src = s;
                break;
            }
        }
        // initialize source and return grid View
        if (src != null && src instanceof BaseFileSource<?>) {
            try {
                ((BaseFileSource<?>)src).init(configuration.getPublicStorage(), uriInfo.getBaseUri());
            } catch (Exception e) {
                log.error("Failed to initialize source {}: {}", e, src.getTitle(), e.getMessage());
                throw new WebApplicationException(e, Response.Status.NOT_FOUND);
            }
            if (src instanceof CsvSourceImpl)
                return Response.ok(new Viewable("/CsvSourceGrid.vm", src)).build();
            else if (src instanceof RdfSourceImpl)
                return Response.ok(new Viewable("/RdfSourceGrid.vm", src)).build();
        }
        else if (src != null && src instanceof DbSource) {
            ((DbSource)src).init(this.getFileStorage(this.getProjectFilePath(id, srcId)));
            return Response.ok(new Viewable("/DbSourceGrid.vm", src)).build();
        }
        return null;
    }
    
    @GET
    @Path("{id}/ontologyUpload.html")
    public Object ontologyUpload(@PathParam("id") String id,
                            @Context UriInfo uriInfo) {
        LogContext.setContexts(MODULE_NAME, id + "/ontoupload");

        URI uri = null;
        try {
            uri = new URI(uriInfo.getBaseUri() + "project/" + id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        Project p = this.projectDao.get(uri);
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p);
        return Response.ok(new Viewable("/projectOntoUpload.vm", args),
                MediaType.TEXT_HTML)
            .build();
    }

    @POST
    @Path("{id}/ontologyUpload")
    public Response ontologyUpload(@PathParam("id") String id,
			@Context UriInfo uriInfo,
			@FormParam("source_url") String srcUrl) {
    	URI projectUri = null;
    	try {
			projectUri = new URI(uriInfo.getBaseUri() + "project/" + id);
    	} catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		Project p = this.projectDao.get(projectUri);
		//Add new source to persistent project
		OntologyImpl src = new OntologyImpl();
		
		src.setTitle(srcUrl);
		src.setDateSubmitted(new Date());
		src.setOperator(SecurityContext.getUserPrincipal());

		((ProjectImpl)p).addOntology(src);
		
		this.projectDao.save(p);
		
		try {
	        return Response.ok(new Viewable("/workspace.vm",this.projectDao.getAll()),
	                MediaType.TEXT_HTML)
	            .build();
		}
		catch (Exception e) {
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}    	
    }
    
    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private URI newProjectId(URI baseUri, String name) {
        try {
            return new URL(baseUri.toURL(), "project/" + urlify(name)).toURI();
        }
        catch (Exception e) {
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
        TechnicalException error = new TechnicalException(
                                        "ws.invalid.param.error", name, value);
        throw new WebApplicationException(
                                Response.status(Status.BAD_REQUEST)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
    }

    /**
     * Creates and configures a new Empire JPA EntityManagerFactory
     * to persist objects into the specified RDF repository.
     * @param  repository   the RDF repository to persist objects into.
     *
     * @return a configured Empire EntityManagerFactory.
     */
    private EntityManagerFactory createEntityManagerFactory(URL repository) {
        // Build Empire configuration.
        EmpireConfiguration empireCfg = new EmpireConfiguration();
        // Configure target repository.
        Map<String,String> props = empireCfg.getGlobalConfig();
        String[] repo = repository.toString().split(REPOSITORY_URL_PARSER);
        props.put("factory", "sesame");
        props.put("url",     repo[0]);
        props.put("repo",    repo[1]);
        // Set persistent classes and associated (custom) annotation provider.
        empireCfg.setAnnotationProvider(CustomAnnotationProvider.class);
        props.put(CustomAnnotationProvider.ANNOTATED_CLASSES_PROP,
                  join(this.getPersistentClasses(), ",").replace("class ", ""));
        // Initialize Empire.
        Empire.init(empireCfg, new OpenRdfEmpireModule());
        // Create Empire JPA persistence provider.
        PersistenceProvider provider = Empire.get().persistenceProvider();
        return provider.createEntityManagerFactory("",
                                                new HashMap<Object,Object>());
    }

    /**
     * Returns the list of persistent classes to be handled by Empire
     * JPA provider. 
     * @return the list of persistent classes.
     */
    @SuppressWarnings("unchecked")
    private Collection<Class<?>> getPersistentClasses() {
        Collection<Class<?>> classes = new LinkedList<Class<?>>();
        classes.addAll(Arrays.asList(
                            ProjectImpl.class, CsvSourceImpl.class,
                            RdfSourceImpl.class, DbSource.class, OntologyImpl.class));
        return classes;
    }

    /**
     * Register application namespaces to Empire.
     * <p>
     * Yes, Empire's Namespaces and RdfProperty annotations suck too
     * as they use prefixes as key in a global namespace table
     * (RdfNamespace) rather than using the namespace URI as key and
     * consider prefixes as a local matter (local to each query and
     * class).</p>
     */
    private void registerRdfNamespaces() {
        for (RdfNamespace ns : RdfNamespace.values()) {
            NamespaceUtils.addNamespace(ns.prefix, ns.uri);
        }
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName()
                + " (" + this.configuration.getInternalRepository().url + ')';
    }

    //-------------------------------------------------------------------------
    // ProjectJpaDao nested class
    //-------------------------------------------------------------------------

    /**
     * A JPA DAO implementation for persisting ProjectImpl objects.
     */
    private final static class ProjectJpaDao extends GenericRdfJpaDao<Project>
    {
        public ProjectJpaDao(EntityManager em) {
            super(ProjectImpl.class, em);
        }

//        public Project findByTitle(String title) {
//            List<Project> l = executeQuery(
//                            "where { ?result dc:title \"" + title + "\" . }");
//            return ((! l.isEmpty())? l.get(0): null);
//        }
    }    
}

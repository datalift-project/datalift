package org.datalift.core.project;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;

import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.annotation.RdfGenerator;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.utils.NamespaceUtils;
import com.sun.jersey.api.ConflictException;
import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;

import org.datalift.core.project.CsvSource.Separator;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.util.StringUtils;

@Path("/project")
public class WorkspaceResource implements LifeCycle
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String REPOSITORY_URL_PARSER = "/repositories/";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift configuration. */
    private Configuration configuration = null;

    /** ? */
    private EntityManagerFactory emf = null;
    /** ? */
    private EntityManager entityMgr = null;
    /** ? */
    private ProjectJpaDao projectDao = null;

    //-------------------------------------------------------------------------
    // LifeCycle contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        this.configuration = configuration;
        // Create Empire JPA persistence provider.
        this.emf = this.getEntityManagerFactory(
                                    configuration.getInternalRepository().url);
        this.entityMgr = this.emf.createEntityManager();
        log.debug("JPA persistence provider initialized for repository \"{}\"",
                  configuration.getInternalRepository().name);
        // Registers mapped classes.
        this.registerPersistentClasses(
        		ProjectImpl.class, 
        		BaseSource.class, 
        		FileSource.class, 
        		CsvSource.class,
        		RdfSource.class);
        // Register application namespaces to Empire.
        this.registerRdfNamespaces();
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
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public Response getIndex() {
        return Response.ok(new Viewable("/workspace.vm",
                                        this.projectDao.getAll()),
                           MediaType.TEXT_HTML)
                       .build();
    }

    @POST
    public Response registerProject(
                                @FormParam("title") String title,
                                @FormParam("owner") String owner,
                                @FormParam("description") String description,
                                @Context UriInfo uriInfo) {
        // Check that project is unique.
        if (this.projectDao.findByTitle(title) == null) {
            // Create new project.
            Project p = new ProjectImpl(
                                this.newProjectId(uriInfo.getBaseUri(), title));
            p.setTitle(title);
            p.setOwner(owner);
            p.setDescription(description);
            // Persist project to RDF store.
            try {
                this.projectDao.persist(p);
                // create Project directory in public storage
                String id = p.getUri().substring(p.getUri().lastIndexOf("/") + 1);
                String storagePath = configuration.getPublicStorage().getAbsolutePath() + 
    						"/project/" + id + "/source/";
            	File projectStorage = new File(storagePath);
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
    @Path("add")
    public Response getNewProjectPage() {
        return Response.ok(new Viewable("/workspaceAddProject.vm", null),
                           MediaType.TEXT_HTML)
                       .build();
    }

    @GET
    @Path("{id}")
    public Response getProject(@PathParam("id") String id,
                               @Context UriInfo uriInfo,
                               @Context Request request,
                               @HeaderParam("Accept") String acceptHdr)
                                                throws WebApplicationException {
        Response response = null;
        // Check that projects exists in internal data store.
        Project p = this.projectDao.find(uriInfo.getAbsolutePath());
        if (p != null) {
            // Search for modules which can handle project
            Collection<ProjectModule> modules = new LinkedList<ProjectModule>();
            for (ProjectModule m : configuration.getBeans(ProjectModule.class)) {
                if (m.canHandle(p) != null) {
                    modules.add(m);
                }
            }
            // Build arguments and response
            Map<String, Object> args = new TreeMap<String, Object>();
            args.put("it", p);
            args.put("canHandle", modules);
            response = Response.ok(new Viewable("/projectDescription.vm", args),
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
    @Path("{id}/srcupload")
    public Object	srcUpload(@PathParam("id") String id, @Context UriInfo uriInfo) {
    	URI uri = null;
		try {
			uri = new URI(uriInfo.getBaseUri() + "project/" + id);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Project p = this.projectDao.get(uri);
		Map<String, Object>	args = new HashMap<String, Object>();
		args.put("it", p);
		args.put("sep", Separator.values());
     	return Response.ok(new Viewable("/projectSourceUpload.vm", args),
                MediaType.TEXT_HTML)
            .build();
    }
    
    @POST
    @Path("{id}/csvupload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response	csvUpload(
    		@PathParam("id") String id,
    		@Context UriInfo uriInfo,
            @FormDataParam("source") InputStream file,
            @FormDataParam("source") FormDataContentDisposition fileDisposition,
            @FormDataParam("separator") String separator,
            @FormDataParam("title_row") String titleRow){
    	
    	// Get persistent project by its Id
    	URI projectUri = null;
    	URI sourceUri = null;
		try {
			projectUri = new URI(uriInfo.getBaseUri() + "project/" + id);
			sourceUri = new URI(projectUri.getScheme(), null, projectUri.getHost(), projectUri.getPort(), projectUri.getPath() + "/source/" + fileDisposition.getFileName(), null, null);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Project p = this.projectDao.get(projectUri);
		// Save new source to public project storage
		String storageUrl = "project/" + id + "/source/" + fileDisposition.getFileName();
		String storagePath = configuration.getPublicStorage().getAbsolutePath() + 
							"/" + storageUrl;
		File dest = new File(storagePath);
		try{
			this.fileCopy(file, dest);
		} catch (IOException e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		// Add new source to persistent project
		CsvSource src = new CsvSource();
		src.setTitle(fileDisposition.getFileName());
		src.setUrl(storageUrl);
		src.setMimeType("application/octet-stream");
		src.setSeparator(Separator.valueOf(separator).toString());
		if (titleRow != null && titleRow.equals("on"))
			src.setTitleRow(true);
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
    
    @POST
    @Path("{id}/rdfupload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response	rdfUpload(
    		@PathParam("id") String id,
    		@Context UriInfo uriInfo,
            @FormDataParam("source") InputStream file,
            @FormDataParam("source") FormDataContentDisposition fileDisposition,
            @FormDataParam("mime_type") String mimeType) {
    	
    	// Get persistent project by its Id
    	URI projectUri = null;
    	URI sourceUri = null;
		try {
			projectUri = new URI(uriInfo.getBaseUri() + "project/" + id);
			sourceUri = new URI(projectUri.getScheme(), null, projectUri.getHost(), projectUri.getPort(), projectUri.getPath() + "/source/" + fileDisposition.getFileName(), null, null);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		Project p = this.projectDao.get(projectUri);
		// Save new source to public project storage
		String storageUrl = "project/" + id + "/source/" + fileDisposition.getFileName();
		String storagePath = configuration.getPublicStorage().getAbsolutePath() + 
							"/" + storageUrl;
		File dest = new File(storagePath);
		try{
			this.fileCopy(file, dest);
		} catch (IOException e){
			throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
		}
		// Add new source to persistent project
		RdfSource src = new RdfSource();
		src.setTitle(fileDisposition.getFileName());
		src.setUrl(storageUrl);
		src.setMimeType(mimeType);
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
    
    public void	fileCopy(InputStream src, File dest) throws IOException {
    	try{
			dest.createNewFile();
			try{
				java.io.FileOutputStream destinationFile = null;
				try{
					destinationFile = new FileOutputStream(dest);
					
					byte buffer[] = new byte[512 * 1024];
					int nbLecture;
					
					while ((nbLecture = src.read(buffer)) != -1){
						destinationFile.write(buffer, 0, nbLecture);
					}
				} finally {
					destinationFile.close();
				}
			} finally {
				src.close();
			}
		} catch (IOException e){
			throw e;
		}
    }

    @GET
    @Path("{id}/source/{srcid}")
    public Response getSource(
    		@PathParam("id") String id,
    		@PathParam("srcid") String srcid,
    		@Context UriInfo uriInfo){
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
		for (Source s : p.getSources()) {
			String url = "project/" + id + "/source/" + srcid;
			if (s.getUrl().equals(url)) {
				src = s;
				break;
			}
		}
		// initialize source and return grid View
		if (src != null && src instanceof FileSource) {
			String storagePath = configuration.getPublicStorage().getAbsolutePath() + 
			"/" + src.getUrl();
			try {
				((FileSource)src).init(storagePath);
			} catch (Exception e) {
				throw new WebApplicationException(e, Response.Status.NOT_FOUND);
			}
			if (src instanceof CsvSource)
				return Response.ok(new Viewable("/CsvSourceGrid.vm", src)).build();
			else if (src instanceof RdfSource)
				return Response.ok(new Viewable("/RdfSourceGrid.vm", src)).build();
		}
		return null;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private String newProjectId(URI baseUri, String name) {
        try {
            return new URL(baseUri.toURL(),
                            "project/" + StringUtils.urlify(name)).toString();
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid base URI: " + baseUri); 
        }
    }

    private EntityManagerFactory getEntityManagerFactory(URL repository) {
        // Build Empire configuration.
        EmpireConfiguration empireCfg = new EmpireConfiguration();
        Map<String,String> props = empireCfg.getGlobalConfig();
        String[] repo = repository.toString().split(REPOSITORY_URL_PARSER);
        props.put("factory", "sesame");
        props.put("url",     repo[0]);
        props.put("repo",    repo[1]);
        // Initialize Empire.
        Empire.init(empireCfg, new OpenRdfEmpireModule());
        // Create Empire JPA persistence provider.
        PersistenceProvider provider = Empire.get().persistenceProvider();
        return provider.createEntityManagerFactory("",
                                                new HashMap<Object,Object>());
    }

    /**
     * Registers mapped classes, the rough way... as all available
     * EmpireAnnotationProvider implementations suck, thanks to
     * Google Guice!
     */
    private void registerPersistentClasses(Class<?>... classes) {
        RdfGenerator.init(Arrays.asList(classes));
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


    private final static class ProjectJpaDao extends GenericRdfJpaDao<Project>
    {
        public ProjectJpaDao(EntityManager em) {
            super(ProjectImpl.class, em);
        }

        public Project findByTitle(String title) {
            List<Project> l = executeQuery(
                            "where { ?result dc:title \"" + title + "\" . }");
            return ((! l.isEmpty())? l.get(0): null);
        }
    }
}

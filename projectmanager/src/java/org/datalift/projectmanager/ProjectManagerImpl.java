package org.datalift.projectmanager;


import static org.datalift.fwk.util.StringUtils.isSet;
import static org.datalift.fwk.util.StringUtils.join;
import static org.datalift.fwk.util.StringUtils.urlify;

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

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;
import javax.ws.rs.core.MediaType;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.DbSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.security.SecurityContext;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.utils.NamespaceUtils;
import com.sun.jersey.api.NotFoundException;

public class ProjectManagerImpl implements ProjectManager, LifeCycle {

	private final static String REPOSITORY_URL_PARSER = "/repositories/";
	
	private Configuration	configuration;
	
	private EntityManagerFactory emf = null;
	private EntityManager entityMgr = null;
	private ProjectJpaDao projectDao = null;
	
	private Collection<Class<?>>	classes = null;
	
	private Logger	log = null;
	
	public ProjectManagerImpl() {
		log = Logger.getLogger();
	}
	
	@Override
	public void init(Configuration configuration) {
		this.configuration = configuration;
		this.classes = new LinkedList<Class<?>>();
	}
	
	@Override
	public void postInit() {
		this.classes.addAll(this.getPersistentClasses());
		this.registerRdfNamespaces();
		this.emf = this.createEntityManagerFactory(configuration
				.getInternalRepository().url);
		this.entityMgr = this.emf.createEntityManager();
		// Create Data Access Object for Projects.
		this.projectDao = new ProjectJpaDao(this.entityMgr);
	}

	@Override
	public void shutdown(Configuration configuration) {
		
	}
	
	public void	registerRdfNamespace(RdfNamespace ns) {
		NamespaceUtils.addNamespace(ns.prefix, ns.uri);
	}
	
	private void registerRdfNamespaces() {
		for (RdfNamespace ns : RdfNamespace.values()) {
			NamespaceUtils.addNamespace(ns.prefix, ns.uri);
		}
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
		src.setTitleRow(hasTitleRow);
		src.setFilePath(filePath);
		src.setMimeType("text/csv");
		src.setSeparator(String.valueOf(separator));
		for (Separator s : Separator.values()) {
			if (s.getValue() == separator) {
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
			Integer cacheDuration) {
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
	
	private File getFileStorage(String path) {
		return new File(this.configuration.getPublicStorage(), path);
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
				join(this.classes, ",").replace("class ", ""));
		// Initialize Empire.
		Empire.init(empireCfg, new OpenRdfEmpireModule());
		// Create Empire JPA persistence provider.
		PersistenceProvider provider = Empire.get().persistenceProvider();
		return provider.createEntityManagerFactory("",
				new HashMap<Object, Object>());
	}
	
	public void addPersistentClasses(Collection<Class<?>> classes) {
		this.classes.addAll(classes);
	}
	// -------------------------------------------------------------------------
	// ProjectJpaDao nested class
	// -------------------------------------------------------------------------

	/**
	 * A JPA DAO implementation for persisting ProjectImpl objects.
	 */
	public final static class ProjectJpaDao extends GenericRdfJpaDao<Project> {
		public ProjectJpaDao(EntityManager em) {
			super(ProjectImpl.class, em);
		}

		@Override
		public Project save(Project entity) {
			entity.setDateModification(new Date());
			return super.save(entity);
		}
	}
	
	public Project newProject(URI projectId, String title, String description, String license) {
		// Create new project.
		Project p = new ProjectImpl(projectId.toString());
		p.setTitle(title);

		p.setOwner(SecurityContext.getUserPrincipal());
		p.setDescription(description);
		p.setLicense(License.valueOf(license).uri);

		Date date = new Date();
		p.setDateCreation(date);
		p.setDateModification(date);
		return p;
	}
	
	public void addCsvSource(URI projectUri, URI sourceUri, String id, String fileName, InputStream file, 
			String titleRow, String separator) throws Exception {
		try {
			Project p = this.projectDao.get(projectUri);
			if (p.getSource(sourceUri) == null) {
				// Save new source to public project storage
				String filePath = this.getProjectFilePath(id, fileName);
				File storagePath = this.getFileStorage(filePath);
				fileCopy(file, storagePath);
				
				// Add new source to persistent project
				Separator sep = Separator.valueOf(separator);
				boolean hasTitleRow = ((titleRow != null) && (titleRow
						.toLowerCase().equals("on")));
				CsvSource src = this.newCsvSource(sourceUri, fileName, filePath,
						sep.getValue(), hasTitleRow);
				p.addSource(src);
				this.projectDao.save(p);
			}
			// Else : source already exist
		}
		catch (Exception e) {
			throw e;
		}
	}

	public void addRdfSource(URI baseUri, String id, String fileName, 
								String mimeType, InputStream file) throws Exception {
		MediaType mappedType = null;
		try {
			mappedType = RdfSourceImpl.parseMimeType(mimeType);
		} catch (Exception e) {
			throw e;
		}
		try {
			URI projectUri = this.newProjectId(baseUri, id);
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
			
			((RdfSourceImpl)src).init(configuration.getPublicStorage(), baseUri);
			
			p.addSource(src);
			this.projectDao.save(p);
		}
		catch (Exception e) {
			throw e;
		}
	}
	
	public void addDbSource(URI projectUri, URI sourceUri, String title, String database, 
			String srcUrl, String request, String user, String password, int cacheDuration) {
		Project p = this.projectDao.get(projectUri);

		// Add new source to persistent project
		DbSource src = newDbSource(sourceUri, title, database, srcUrl,
				user, password, request, cacheDuration);
		p.addSource(src);
		this.projectDao.save(p);
	}
	
	public void addOntology(URI projectUri, URI srcUrl, String title) {
		Project p = this.projectDao.get(projectUri);

		// Add ontology to persistent project
		OntologyImpl ontology = new OntologyImpl();
		ontology.setTitle(title);
		ontology.setSource(srcUrl);
		ontology.setDateSubmitted(new Date());
		ontology.setOperator(SecurityContext.getUserPrincipal());
		p.addOntology(ontology);
		//log.debug("PROJECT MANAGER | Add Ontology {}", ontology.getTitle());
		this.projectDao.save(p);
	}
	
	public void	updateCsvSource(Project p, URI sourceUri, String id,
			String titleRow, String separator) throws Exception {
		try {
			// Get source of project
			CsvSourceImpl source = (CsvSourceImpl) p.getSource(sourceUri);
			if (source == null) {
				// Not found.
				throw new NotFoundException();
			}
			// Save infos to persistent project
			boolean hasTitleRow = ((titleRow != null) && (titleRow
			.toLowerCase().equals("on")));
			source.setSeparator(String.valueOf(separator));
			source.setTitleRow(hasTitleRow);
			this.projectDao.save(p);
		}
		catch (Exception e) {
			throw e;
		}
	}
	
	public void	updateRdfSource(URI projectUri, URI sourceUri, String id, 
			String mimeType) throws Exception {
		try {
			Project p = this.projectDao.get(projectUri);
			MediaType mappedType = null;
			try {
				mappedType = RdfSourceImpl.parseMimeType(mimeType);
			} catch (Exception e) {
				//this.throwInvalidParamError("mime_type", mimeType);
				throw e;
			}
			// Get source to persistent project
			RdfSourceImpl src = (RdfSourceImpl) p.getSource(sourceUri);
			if (src == null) {
				// Not found.
				throw new NotFoundException();
			}
			
			// Save infos
			src.setMimeType(mappedType.toString());
			this.projectDao.save(p);
		}
		catch (Exception e) {
			throw e;
		}
	}
	
	public void updateDbSource(URI projectUri, URI sourceUri, String title, String database, 
			String user, String password, String request, int cacheDuration) {
		Project p = this.projectDao.get(projectUri);
		// Get source to persistent project
		DbSource src = (DbSource) p.getSource(sourceUri);
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
	}
	
	public String getProjectFilePath(String projectId, String fileName) {
		String path = "project/" + projectId;
		if (isSet(fileName)) {
			path += "/" + fileName;
		}
		return path;
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
	
	private URI newProjectId(URI baseUri, String name) {
		try {
			return new URL(baseUri.toURL(), "workspace/project/" + urlify(name)).toURI();
		} catch (Exception e) {
			throw new RuntimeException("Invalid base URI: " + baseUri);
		}
	}
	
	private String getRelativeSourceId(String sourceName) {
		return "/source/" + urlify(sourceName);
	}

	@Override
	public void deleteProject(Project p) {
		this.projectDao.delete(p);
	}

	@Override
	public Project getProject(URI id) {
		return this.projectDao.get(id);
	}
	
	public void saveProject(Project p) {
		this.projectDao.save(p);
	}
	
	public void persistProject(Project p) {
		this.projectDao.persist(p);
		// create Project directory in public storage
		String id = p.getUri().substring(
				p.getUri().lastIndexOf("/") + 1);
		File projectStorage = this.getFileStorage(this.getProjectFilePath(id, null));
		projectStorage.mkdirs();
	}
	
	public Collection<Project> getAllProjects() {
		return this.projectDao.getAll();
	}

	
}

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

package org.datalift.core.project;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import com.clarkparsia.common.util.PrefixMapping;
import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.config.ConfigKeys;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.empire.sesametwo.RepositoryFactoryKeys;

import org.datalift.core.project.ProvEntity;

import javassist.ClassPool;
import javassist.LoaderClassPath;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.FileStore;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Event;
import org.datalift.fwk.project.ProcessingTask;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.SqlDatabaseSource;
import org.datalift.fwk.project.SqlQuerySource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.ShpSource;
import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.User;
import org.datalift.fwk.project.XmlSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.security.SecurityContext;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * Default implementation of the {@link ProjectManager} interface.
 *
 * @author hdevos, lbihanic, oventura, avalensi
 */
public class DefaultProjectManager implements ProjectManager, LifeCycle
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /* package */ final static String PROJECT_DIRECTORY_NAME = "project";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private EntityManagerFactory emf = null;
    private GenericRdfJpaDao<Project> projectDao = null;

    private final Collection<Class<?>> classes = new HashSet<Class<?>>();

    //-------------------------------------------------------------------------
    // LifeCycle contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        this.classes.addAll(this.getPersistentClasses());
        this.registerRdfNamespaces();
    }

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        try {
            // Configures Empire JPA persistence provider.
            this.emf = this.createEntityManagerFactory(
                                        configuration.getInternalRepository());
            // Create Data Access Object for persisting Project objects.
            this.projectDao = new GenericRdfJpaDao<Project>(
                                                ProjectImpl.class, this.emf);
            // Register DAO so that it receives request lifecycle events.
            configuration.registerBean(this.projectDao);
            log.debug("Initialized Empire persistence provider");
        }
        catch (RuntimeException e) {
            // Shutdown JPA persistence provider.
            if (this.emf != null) {
                try {
                    this.emf.close();
                }
                catch (Exception x) { /* Ignore... */ }
                this.emf = null;
            }
            this.projectDao = null;
            log.fatal("Failed to initialize Empire persistence provider", e);
            throw e;
        }
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown(Configuration configuration) {
        // Shutdown JPA persistence provider.
        if (this.emf != null) {
            try {
                this.emf.close();
            } catch (Exception e) { /* Ignore... */ }
            this.emf = null;
            log.debug("Empire persistence provider shut down");
        }
        this.projectDao = null;
    }

    //-------------------------------------------------------------------------
    // ProjectManager contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Project findProject(URI uri) {
        this.checkAvailable();
        return this.projectDao.find(uri);
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("unchecked")
    public Collection<Project> listProjects() {
        this.checkAvailable();
        return (Collection<Project>)(this.projectDao.getAll());
    }

    /** {@inheritDoc} */
    @Override
    public CsvSource newCsvSource(Project project, URI uri, String title,
                                  String description, String filePath,
                                  char separator) throws IOException {
        // Create new CSV source.
        CsvSourceImpl src = new CsvSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        File f = this.getFileStorage(filePath);
        if (!f.isFile()) {
            throw new FileNotFoundException(filePath);
        }
        src.setFilePath(filePath);
        src.setMimeType(MediaTypes.TEXT_CSV);
        src.setSeparator(String.valueOf(separator));
        for (Separator s : Separator.values()) {
            if (s.getValue() == separator) {
                src.setSeparator(s.name());
                break;
            }
        }
        // Add source to project.
        project.add(src);
        log.debug("New CSV source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public RdfFileSource newRdfSource(Project project, URI uri, String title,
                                      String description, URI baseUri,
                                      String filePath, String mimeType)
                                                            throws IOException {
        // Create new CSV source.
        RdfFileSourceImpl src = new RdfFileSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        File f = this.getFileStorage(filePath);
        if (!f.isFile()) {
            throw new FileNotFoundException(filePath);
        }
        src.setFilePath(filePath);
        src.setMimeType(mimeType);
        if (baseUri != null) {
            src.setBaseUri(baseUri.toString());
        }
        // Add source to project.
        project.add(src);
        log.debug("New RDF file source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public SqlQuerySource newSqlQuerySource(Project project, URI uri,
                                  String title, String description,
                                  String srcUrl, String user, String password,
                                  String request, int cacheDuration)
                                                            throws IOException {
        // Create new CSV source.
        SqlQuerySourceImpl src = new SqlQuerySourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        src.setConnectionUrl(srcUrl);
        src.setUser(user);
        src.setPassword(password);
        src.setQuery(request);
        src.setCacheDuration(cacheDuration);
        // Add source to project.
        project.add(src);
        log.debug("New SQL source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public SqlDatabaseSource newSqlDatabaseSource(Project project, URI uri,
            					String title, String description,
            					String srcUrl, String user, String password)
            							throws IOException{
        // Create new SQL Database source.
        SqlDatabaseSourceImpl src = new SqlDatabaseSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        src.setConnectionUrl(srcUrl);
        src.setUser(user);
        src.setPassword(password);
        // Add source to project.
        project.add(src);
        log.debug("New Database source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
    	return src;
    }
    
    /** {@inheritDoc} */
    @Override
    public SparqlSource newSparqlSource(Project project, URI uri, String title,
                                        String description, String endpointUrl,
                                        String sparqlQuery, int cacheDuration)
                                                            throws IOException {
        // Create new CSV source.
        SparqlSourceImpl src = new SparqlSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        src.setEndpointUrl(endpointUrl);
        src.setQuery(sparqlQuery);
        src.setCacheDuration(cacheDuration);
        // Add source to project.
        project.add(src);
        log.debug("New SPARQL source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public XmlSource newXmlSource(Project project, URI uri, String title,
                                  String description, String filePath)
                                                            throws IOException {
        // Create new XML source.
        XmlSourceImpl src = new XmlSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        File f = this.getFileStorage(filePath);
        if (! f.isFile()) {
            throw new FileNotFoundException(filePath);
        }
        src.setFilePath(filePath);
        src.setMimeType(MediaTypes.APPLICATION_XML);
        // Add source to project.
        project.add(src);
        log.debug("New XML source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public TransformedRdfSource newTransformedRdfSource(Project project,
                                    URI uri, String title, String description,
                                    URI targetGraph, Source parent)
                                                            throws IOException {
        // Create new CSV source.
        TransformedRdfSourceImpl src =
                        new TransformedRdfSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        src.setTargetGraph(targetGraph.toString());
        src.setParent(parent);
        // Add source to project.
        project.add(src);
        log.debug("New transformed RDF source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public ShpSource newShpSource(Project project, URI uri, String title,
                                  String description, String shpFilePath,
                                  String shxFilePath, String dbfFilePath,
                                  String prjFilePath)
                                                            throws IOException {
        // Create new Shapefile source.
        ShpSourceImpl src = new ShpSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        // Check and set Shape main file (SHP).
        File f = this.getFileStorage(shpFilePath);
        if (! f.isFile()) {
            throw new FileNotFoundException(shpFilePath);
        }
        src.setShapeFilePath(shpFilePath);
        // Check and set index file (SHX).
        f = this.getFileStorage(shxFilePath);
        if (! f.isFile()) {
            throw new FileNotFoundException(shxFilePath);
        }
        src.setIndexFilePath(shxFilePath);
        // Check and set attribute file (DBF).
        f = this.getFileStorage(dbfFilePath);
        if (! f.isFile()) {
            throw new FileNotFoundException(dbfFilePath);
        }
        src.setAttributeFilePath(dbfFilePath);
        // Check and set projection file (PRJ).
        f = this.getFileStorage(prjFilePath);
        if (! f.isFile()) {
            throw new FileNotFoundException(prjFilePath);
        }
        src.setProjectionFilePath(prjFilePath);
        // Add source to project.
        project.add(src);
        log.debug("New ShapeFile source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public GmlSource newGmlSource(Project project, URI uri, String title,
                                  String description, String gmlFilePath,
                                  String xsdFilePath)
                                                            throws IOException {
        // Create new Gmlfile source.
        GmlSourceImpl src = new GmlSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        // Check and set GML main file (GML).
        File f = this.getFileStorage(gmlFilePath);
        if (! f.isFile()) {
            throw new FileNotFoundException(gmlFilePath);
        }
        src.setGmlFilePath(gmlFilePath);
        // Check and set schema file (XSD).
        f = this.getFileStorage(xsdFilePath);
        if (! f.isFile()) {
            throw new FileNotFoundException(xsdFilePath);
        }
        src.setXsdFilePath(xsdFilePath);
        // Add source to project.
        project.add(src);
        log.debug("New GML source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Source source) {
        this.delete(source, true);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Source source, boolean deleteResources) {
        this.checkAvailable();
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        Project p = source.getProject();
        // Remove source from project.
        p.remove(source);
        if (deleteResources) {
            // Release source resources (files, caches...).
            log.debug("Releasing resources for source <{}>", source.getUri());
            source.delete();
        }
        // Persist changes.
        this.saveProject(p);
        this.projectDao.delete(source);
        log.debug("Source <{}> removed from project \"{}\"",
                                                source.getUri(), p.getTitle());
    }

    /** {@inheritDoc} */
    @Override
    public Ontology newOntology(Project project, URI url, String title) {
        if (project == null) {
            throw new IllegalArgumentException("project");
        }
        if (url == null) {
            throw new IllegalArgumentException("url");
        }
        if (title == null) {
            throw new IllegalArgumentException("title");
        }
        // Create new ontology.
        OntologyImpl ontology = new OntologyImpl();
        // Set ontology parameters.
        ontology.setTitle(title);
        ontology.setSource(url);
        ontology.setProject(project);
        log.debug("New ontology <{}> added to project \"{}\"",
                                                    url, project.getTitle());
        return ontology;
    }

    /** {@inheritDoc} */
    @Override
    public Ontology saveOntology(Ontology o) {
        this.checkAvailable();
        if (o== null) {
            throw new IllegalArgumentException("ontology");
        }
        this.projectDao.save(o);
        log.debug("Ontology <{}> saved to RDF store", o.getUri());
        
        return o;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteOntology(Project project, Ontology ontology) {
        this.checkAvailable();
        if (project == null) {
            throw new IllegalArgumentException("project");
        }
        if (ontology == null) {
            throw new IllegalArgumentException("ontology");
        }
        this.projectDao.delete(ontology);
        log.debug("Ontology <{}> removed form project \"{}\"",
                                    ontology.getSource(), project.getTitle());
    }

    /** {@inheritDoc} */
    @Override
    public List<Ontology> getOntologies(Project project) {
        this.checkAvailable();
        if (project == null) {
            throw new IllegalArgumentException("project");
        }
        
//        String query = "select distinct ?result where {" + 
//        		       "?result a <http://www.datalift.org/core#ontology>;" +
//        		       "<http://www.datalift.org/core#project> <http://datalift.fr/proj/myproject> }";

        StringBuilder query = new StringBuilder();
        query.append("select distinct ?result where {");
        query.append("?result a datalift:ontology;");
        query.append("datalift:project <");
        query.append(project.getUri().toString());
        query.append("> }");
        
        
        @SuppressWarnings("unchecked")
		List<Ontology> ontologies = (List<Ontology>) this.projectDao.executeQuery(query.toString(), Ontology.class);
        System.out.println(ontologies);
        
        return ontologies;
    }

    /** {@inheritDoc} */
    @Override
    public Project newProject(URI projectId, String title,
                              String description, URI license) {
        // Check that no project with the same URI already exists.
        if (this.findProject(projectId) != null) {
            throw new DuplicateUriException("duplicate.project.uri",
                                                           projectId, title);
        }
        // Create new project.
        ProjectImpl p = new ProjectImpl(projectId.toString());
        p.setTitle(title);
        p.setDescription(description);
        p.setLicense(license);

        Date date = new Date();
        p.setCreationDate(date);
        p.setModificationDate(date);
        
        UserImpl user = new UserImpl(SecurityContext.getUserPrincipal());
        p.setWasAttributedTo(user);

        log.debug("New project <{}> created", p.getUri());
        return p;
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectFilePath(String projectId, String fileName) {
        String path = PROJECT_DIRECTORY_NAME + File.separatorChar + projectId;
        if (isSet(fileName)) {
            path = path + File.separatorChar + fileName;
        }
        return path;
    }

    /** {@inheritDoc} 
     * @throws  
     * @throws MalformedURLException */
    @Override
    public void deleteProject(Project p) {
        this.checkAvailable();
        // Delete server-side resources attached to sources.
        for (Source s : p.getSources()) {
            s.delete();
        }
        // Delete project (and dependent objects: sources, ontologies...)
        try {
			this.projectDao.delete(new URI(p.getUri()));
		} 
        catch (URISyntaxException e) {
            throw new RuntimeException("Invalid project URI: " + p.getUri(), e);
		}
        log.debug("Project <{}> deleted", p.getUri());
    }

    /** {@inheritDoc} */
    @Override
    public void saveProject(Project p) {
        this.checkAvailable();
        if (p == null) {
            throw new IllegalArgumentException("p");
        }
        p.setModificationDate(new Date());
        try {
            if (this.findProject(new URI(p.getUri())) == null) {
                this.projectDao.persist(p);
                this.projectDao.persist(new ProvEntity(p.getUri()));
                this.projectDao.persist(new ProvAgent(p.getWasAttributedTo().getUri()));
                String id = p.getUri().substring(p.getUri().lastIndexOf("/") + 1);
                File projectStorage = this.getFileStorage(
                                            this.getProjectFilePath(id, null));
                projectStorage.mkdirs();
            }
            else {
                this.projectDao.save(p);
            }
            log.debug("Project <{}> saved to RDF store", p.getUri());
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid project URI: " + p.getUri(), e);
        }
    }

    /** {@inheritDoc} */
    // TODO: Maybe to remove.
    @Override
    public ProjectCreationEventImpl newProjectCreationEvent(User u, Project p) {
    	ProjectCreationEventImpl e = new ProjectCreationEventImpl();

    	//e.setId(eventURI);
		//e.setDescription("");
		//e.setParameter("none");
		e.setStartedAtTime(new Date());
		e.setEndedAtTime(new Date());
		e.setUsed(p);
		e.setWasAssociatedWith(u);
		
		return e;
    }
    
    /** {@inheritDoc} */
    @Override
    public void saveEvent(Event e) {
        this.checkAvailable();
        if (e == null) {
            throw new IllegalArgumentException("e");
        }
        if (e.getUri() == null) {
        	throw new RuntimeException("Invalid event URI");
        }
        this.projectDao.save(e);
        log.debug("Event <{}> saved to RDF store", e.getUri());
    }

    /** {@inheritDoc} */
    @Override
    public void deleteEvent(Event e) {
        try {
			this.projectDao.delete(new URI(e.getUri()));
		} 
        catch (URISyntaxException ex) {
            throw new RuntimeException("Invalid event URI: " + e.getUri(), ex);
		}
        log.debug("Event <{}> deleted", e.getUri());
    }

    /** {@inheritDoc} */
    @Override
    public void addPersistentClasses(Collection<Class<?>> classes) {
        if (this.emf != null) {
            // Too late! empire is already started.
            throw new IllegalStateException("Already started");
        }
        this.classes.addAll(classes);
    }
    
    /** {@inheritDoc} */
    @Override
	public ProcessingTask newProcessingTask(URI transformationId) {
    	return new ProcessingTaskImpl(transformationId);
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() +
                " (" + Configuration.getDefault().getInternalRepository() + ')';
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Adds the specified namespace to the list of RDF namespaces
     * known of Empire persistence manager.
     * @param  ns   the RDF namespace to add.
     */
    public void registerRdfNamespace(RdfNamespace ns) {
        if (this.emf != null) {
            // Too late! empire is already started.
            throw new IllegalStateException("Already started");
        }
        log.trace("Added mapping \"{}\" to RDF JPA provider", ns);
        PrefixMapping.GLOBAL.addMapping(ns.prefix, ns.uri);
    }

    /**
     * Registers all {@link RdfNamespace pre-defined RDF namespaces}
     * to Empire persistence manager.
     */
    private void registerRdfNamespaces() {
        for (RdfNamespace ns : RdfNamespace.values()) {
            this.registerRdfNamespace(ns);
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
        // Use intermediate collection to bypass generics typing issue
        // with the compiler-computed return type of Arrays.asList().
        Collection<Class<?>> classes = new HashSet<Class<?>>();
        classes.addAll(Arrays.asList(
                    ProjectImpl.class, OntologyImpl.class,
                    CsvSourceImpl.class, RdfFileSourceImpl.class,
                    SqlQuerySourceImpl.class, SqlDatabaseSourceImpl.class, SparqlSourceImpl.class,
                    XmlSourceImpl.class,
                    TransformedRdfSourceImpl.class, ShpSourceImpl.class, GmlSourceImpl.class));
        return classes;
    }

    /**
     * Returns the {@link File} object associated to the specified
     * path in DataLift public file store.
     * @param  path   the file path.
     *
     * @return the File object in the DataLift public storage.
     */
    private File getFileStorage(String path) {
        FileStore fs = Configuration.getDefault().getPublicStorage();
        if (fs == null) {
            fs = Configuration.getDefault().getPrivateStorage();
        }
        return fs.getFile(path);
    }

    /**
     * Creates and configures a new Empire JPA EntityManagerFactory to
     * persist objects into the specified RDF repository.
     *
     * @param  repository   the RDF repository to persist objects into.
     *
     * @return a configured Empire EntityManagerFactory.
     */
    private EntityManagerFactory createEntityManagerFactory(
                                                        Repository repository) {
        // Build Empire configuration.
        Map<String,String> props = new HashMap<String,String>();
        Map<String,Map<String,String>> unitCfg =
                                    new HashMap<String, Map<String, String>>();
        // Set Empire global options.
        props.put("STRICT_MODE", Boolean.FALSE.toString());     // Lax mode.
        // Register persistent classes in Empire configuration.
        String classList = join(this.classes, ",").replace("class ", "");
        props.put(CustomAnnotationProvider.ANNOTATED_CLASSES_PROP, classList);
        // Register the path of each persistent class to Javassist bytecode
        // generation tool (otherwise object instantiation will fail in
        // multi-classloader environments (such as web apps)).
        ClassPool cp = ClassPool.getDefault();
        for (Class<?> c : this.classes) {
            // cp.insertClassPath(new ClassClassPath(c));
            cp.appendClassPath(new LoaderClassPath(c.getClassLoader()));
        }
        log.debug("Registered persistent classes: {}", classList);
        // Register custom annotation provider to retrieve the list of
        // persistent classes from Empire configuration rather than from a file.
        EmpireConfiguration empireCfg = new EmpireConfiguration(props, unitCfg);
        empireCfg.setAnnotationProvider(CustomAnnotationProvider.class);
        // Initialize Empire.
        Empire.init(empireCfg, new OpenRdfEmpireModule());
        // Configure target repository.
        Map<Object, Object> map = new HashMap<Object,Object>();
        map.put(ConfigKeys.FACTORY, "sesame");
        map.put(RepositoryFactoryKeys.REPO_HANDLE,
                                            repository.getNativeRepository());
        // Create Empire JPA persistence provider.
        PersistenceProvider provider = Empire.get().persistenceProvider();
        return provider.createEntityManagerFactory("", map);
    }

    private void initSource(BaseSource src, String title, String description) {
        // Check that no source with the same URI already exists.
        if (src.getProject().getSource(src.getUri()) != null) {
            throw new DuplicateUriException("duplicate.source.uri",
                            src.getUri(), title, src.getProject().getTitle());
        }
        src.setTitle(title);
        src.setDescription(description);
        src.setCreationDate(new Date());
        src.setOperator(SecurityContext.getUserPrincipal());
    }

    private void checkAvailable() {
        if (this.emf == null) {
            throw new IllegalStateException("Not available");
        }
    }

}

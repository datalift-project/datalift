package org.datalift.projectmanager;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
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

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.utils.NamespaceUtils;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.DbSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.RdfSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.security.SecurityContext;

import static org.datalift.fwk.util.StringUtils.*;


public class ProjectManagerImpl implements ProjectManager, LifeCycle
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String REPOSITORY_URL_PARSER = "/repositories/";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private Configuration configuration;

    private EntityManagerFactory emf = null;
    private EntityManager entityMgr = null;
    private ProjectJpaDao projectDao = null;

    private final Collection<Class<?>> classes = new LinkedList<Class<?>>();

    //-------------------------------------------------------------------------
    // LifeCycle contract support
    //-------------------------------------------------------------------------

    @Override
    public void init(Configuration configuration) {
        this.configuration = configuration;
        this.classes.addAll(this.getPersistentClasses());
        this.registerRdfNamespaces();
    }

    @Override
    public void postInit(Configuration configuration) {
        this.emf = this.createEntityManagerFactory(
                            configuration.getInternalRepository().url);
        this.entityMgr = this.emf.createEntityManager();
        // Create Data Access Object for Projects.
        this.projectDao = new ProjectJpaDao(this.entityMgr);
    }

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
                                String srcUrl, String user, String password,
                                String request, int cacheDuration) {
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

    /** {@inheritDoc} */
    @Override
    public Ontology newOntology(URI srcUrl, String title) {
         OntologyImpl ontology = new OntologyImpl();
         ontology.setTitle(title);
         ontology.setSource(srcUrl);
         ontology.setDateSubmitted(new Date());
         ontology.setOperator(SecurityContext.getUserPrincipal());
         return ontology;
    }

    /** {@inheritDoc} */
    @Override
    public Project newProject(URI projectId, String title,
                              String description, String license) {
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
    
    /** {@inheritDoc} */
    @Override
    public String getProjectFilePath(String projectId, String fileName) {
        String path = "project/" + projectId;
        if (isSet(fileName)) {
            path += "/" + fileName;
        }
        return path;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteProject(Project p) {
        this.projectDao.delete(p);
    }

    /** {@inheritDoc} */
    @Override
    public void saveProject(Project p) {
        try {
            if (this.findProject(new URL(p.getUri()).toURI()) == null) {
                this.projectDao.persist(p);
                String id = p.getUri().substring(p.getUri().lastIndexOf("/") + 1);
                File projectStorage = this.getFileStorage(
                                            this.getProjectFilePath(id, null));
                projectStorage.mkdirs();
            }
            else {
                this.projectDao.save(p);
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid project URI: " + p.getUri());
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addPersistentClasses(Collection<Class<?>> classes) {
        this.classes.addAll(classes);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void registerRdfNamespace(RdfNamespace ns) {
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

    private File getFileStorage(String path) {
        return new File(this.configuration.getPublicStorage(), path);
    }

    /**
     * Creates and configures a new Empire JPA EntityManagerFactory to
     * persist objects into the specified RDF repository.
     *
     * @param  repository   the RDF repository to persist objects into.
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

    //-------------------------------------------------------------------------
    // ProjectJpaDao nested class
    //-------------------------------------------------------------------------

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
}

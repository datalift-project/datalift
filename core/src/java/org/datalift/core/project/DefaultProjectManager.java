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
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import com.clarkparsia.common.util.PrefixMapping;
import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.config.ConfigKeys;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;
import com.clarkparsia.empire.sesametwo.RepositoryFactoryKeys;

import javassist.ClassClassPath;
import javassist.ClassPool;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.SqlSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.security.SecurityContext;

import static org.datalift.fwk.util.StringUtils.*;


public class DefaultProjectManager implements ProjectManager, LifeCycle
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /* package */ final static String PROJECT_DIRECTORY_NAME = "project";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private EntityManagerFactory emf = null;
    private EntityManager entityMgr = null;
    private ProjectJpaDao projectDao = null;

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
        this.emf = this.createEntityManagerFactory(
                                        configuration.getInternalRepository());
        this.entityMgr = this.emf.createEntityManager();
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
                this.entityMgr = null;
            }
            this.emf.close();
            this.emf = null;
        }
    }

    //-------------------------------------------------------------------------
    // ProjectManager contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public Project findProject(URI uri) {
        return this.projectDao.find(uri);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<? extends Project> listProjects() {
        return this.projectDao.getAll();
    }

    /** {@inheritDoc} */
    @Override
    public CsvSource newCsvSource(Project project, URI uri, String title,
                                  String description, String filePath,
                                  char separator, boolean hasTitleRow)
                                                            throws IOException {
        // Create new CSV source.
        CsvSourceImpl src = new CsvSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description, null);
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
        // Add source to project.
        project.add(src);
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
        this.initSource(src, title, description,
                             (baseUri != null)? baseUri.toString(): null);
        File f = this.getFileStorage(filePath);
        if (!f.isFile()) {
            throw new FileNotFoundException(filePath);
        }
        src.setFilePath(filePath);
        src.setMimeType(mimeType);
        // Add source to project.
        project.add(src);
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public SqlSource newSqlSource(Project project, URI uri,
                                  String title, String description,
                                  String srcUrl, String user, String password,
                                  String request, int cacheDuration)
                                                            throws IOException {
        // Create new CSV source.
        SqlSourceImpl src = new SqlSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description, null);
        src.setConnectionUrl(srcUrl);
        src.setUser(user);
        src.setPassword(password);
        src.setQuery(request);
        src.setCacheDuration(cacheDuration);
        // Add source to project.
        project.add(src);
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
        this.initSource(src, title, description, null);
        src.setEndpointUrl(endpointUrl);
        src.setQuery(sparqlQuery);
        src.setCacheDuration(cacheDuration);
        // Add source to project.
        project.add(src);
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
        this.initSource(src, title, description, null);
        src.setTargetGraph(targetGraph.toString());
        src.setParent(parent);
        // Add source to project.
        project.add(src);
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public void delete(Source source) {
        if (source == null) {
            throw new IllegalArgumentException("source");
        }
        Project p = source.getProject();
        // Remove source from project.
        p.remove(source);
        // Release source resources (files, caches...).
        source.delete();
        // Persist changes.
        this.saveProject(p);
        this.projectDao.delete(source);
    }

    /** {@inheritDoc} */
    @Override
    public Ontology newOntology(Project project, URI url, String title) {
        // Create new ontology.
        OntologyImpl ontology = new OntologyImpl();
        // Set ontology parameters.
        ontology.setTitle(title);
        ontology.setSource(url);
        ontology.setDateSubmitted(new Date());
        ontology.setOperator(SecurityContext.getUserPrincipal());
        // Add ontology to project.
        project.addOntology(ontology);
        return ontology;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteOntology(Project project, Ontology ontology) {
        if (project == null) {
            throw new IllegalArgumentException("project");
        }
        if (ontology == null) {
            throw new IllegalArgumentException("ontology");
        }
        // Remove ontology from project.
        project.removeOntology(ontology.getTitle());
        // Delete ontology from persistent store.
        this.projectDao.delete(ontology);
        // Update project.
        this.saveProject(project);
    }

    /** {@inheritDoc} */
    @Override
    public Project newProject(URI projectId, String title,
                              String description, URI license) {
        // Create new project.
        ProjectImpl p = new ProjectImpl(projectId.toString());
        p.setTitle(title);
        p.setDescription(description);
        p.setLicense(license);

        Date date = new Date();
        p.setCreationDate(date);
        p.setModificationDate(date);
        p.setOwner(SecurityContext.getUserPrincipal());
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

    /** {@inheritDoc} */
    @Override
    public void deleteProject(Project p) {
        this.projectDao.delete(p);
    }

    /** {@inheritDoc} */
    @Override
    public void saveProject(Project p) {
        if (p == null) {
            throw new IllegalArgumentException("p");
        }
        p.setModificationDate(new Date());
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
            throw new RuntimeException("Invalid project URI: " + p.getUri(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void addPersistentClasses(Collection<Class<?>> classes) {
        if (this.entityMgr != null) {
            // Too late! empire is already started.
            throw new IllegalStateException("Already started");
        }
        this.classes.addAll(classes);
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
        if (this.entityMgr != null) {
            // Too late! empire is already started.
            throw new IllegalStateException("Already started");
        }
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
                    SqlSourceImpl.class, SparqlSourceImpl.class,
                    TransformedRdfSourceImpl.class));
        return classes;
    }

    /**
     * Returns the {@link File} object associated to the specified
     * path in the DataLift public storage.
     * @param  path   the file path.
     *
     * @return the File object in the DataLift public storage.
     */
    private File getFileStorage(String path) {
        return new File(Configuration.getDefault().getPublicStorage(), path);
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
        EmpireConfiguration empireCfg = new EmpireConfiguration();
        // Set persistent classes and associated (custom) annotation provider.
        empireCfg.setAnnotationProvider(CustomAnnotationProvider.class);
        Map<String,String> props = empireCfg.getGlobalConfig();
        props.put(CustomAnnotationProvider.ANNOTATED_CLASSES_PROP,
                  join(this.classes, ",").replace("class ", ""));
        // Register the path of each persistent class to Javassist bytecode
        // generation tool (otherwise object instantiation will fail in
        // multi-classloader environments (such as web apps)).
        ClassPool cp = ClassPool.getDefault();
        for (Class<?> c : this.classes) {
            cp.insertClassPath(new ClassClassPath(c));
        }
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

    private void initSource(BaseSource src, String title, String description,
                                            String sourceUrl) {
        src.setTitle(title);
        src.setDescription(description);
        src.setSourceUrl(sourceUrl);
        src.setCreationDate(new Date());
        src.setOperator(SecurityContext.getUserPrincipal());
    }

    //-------------------------------------------------------------------------
    // ProjectJpaDao nested class
    //-------------------------------------------------------------------------

    /**
     * A JPA DAO implementation for persisting ProjectImpl objects.
     */
    public final static class ProjectJpaDao extends GenericRdfJpaDao<Project>
    {
        public ProjectJpaDao(EntityManager em) {
            super(ProjectImpl.class, em);
        }
    }
}

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Query;
import javax.persistence.spi.PersistenceProvider;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.annotation.RdfsClass;
import com.clarkparsia.empire.config.ConfigKeys;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.impl.RdfQuery;
import com.clarkparsia.empire.sesame.OpenRdfEmpireModule;
import com.clarkparsia.empire.sesame.RepositoryFactoryKeys;
import com.complexible.common.util.PrefixMapping;

import javassist.ClassPool;
import javassist.LoaderClassPath;

import org.datalift.core.prov.EventImpl;
import org.datalift.core.replay.WorkflowImpl;
import org.datalift.core.replay.WorkflowStepImpl;
import org.datalift.core.util.JsonStringParameters;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.FileStore;
import org.datalift.fwk.LifeCycle;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.async.TaskContext;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.GenericRdfDao;
import org.datalift.fwk.project.PersistenceException;
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
import org.datalift.fwk.project.XmlSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.prov.Event;
import org.datalift.fwk.prov.EventSubject;
import org.datalift.fwk.prov.EventType;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.replay.Workflow;
import org.datalift.fwk.replay.WorkflowStep;
import org.datalift.fwk.security.SecurityContext;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * Default implementation of the {@link ProjectManager} interface.
 *
 * @author hdevos, lbihanic, oventura
 */
public class DefaultProjectManager implements ProjectManager, LifeCycle
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /* package */ final static String PROJECT_DIRECTORY_NAME = "project";
    final static String DEFAULT_OPERATIONS_BASE_URI =
            "http://www.datalift.org/core/ProjectManager/operation/";

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
            this.emf = this.createEntityManagerFactory(configuration);
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
        return this.newCsvSource(project, uri, title, description, filePath,
                separator, null, null, new Date());
    }

    /** {@inheritDoc} */
    @Override
    public CsvSource newCsvSource(Project project, URI uri, String title,
            String description, String filePath, char separator, URI operation,
            Map<String, String> parameters, Date start)
            throws IOException{
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
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("filePath", filePath);
            parametersE.put("separator", "" + separator);
            parametersE.put("uri", uri.toString());
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
        return src;
    }
    
    /** {@inheritDoc} */
    @Override
    public RdfFileSource newRdfSource(Project project, URI uri, String title,
                                      String description, URI baseUri,
                                      String filePath, String mimeType)
                                                            throws IOException {
        return this.newRdfSource(project, uri, title, description, baseUri,
                filePath, mimeType, null, null, new Date());
    }

    /** {@inheritDoc} */
    @Override
    public RdfFileSource newRdfSource(Project project, URI uri, String title,
            String description, URI baseUri, String filePath, String mimeType, 
            URI operation, Map<String, String> parameters, Date start)
            throws IOException{
        // Create new RDF file source.
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
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            if(baseUri != null)
                parametersE.put("baseUri", baseUri.toString());
            else
                parametersE.put("baseUri", null);
            parametersE.put("filePath", filePath);
            parametersE.put("mimeType", mimeType);
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
        return src;
    }
    
    /** {@inheritDoc} */
    @Override
    public SqlQuerySource newSqlQuerySource(Project project, URI uri,
                                  String title, String description,
                                  String srcUrl, String user, String password,
                                  String request, int cacheDuration)
                                                            throws IOException {
        return this.newSqlQuerySource(project, uri, title, description, srcUrl,
                user, password, request, cacheDuration, null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public SqlQuerySource newSqlQuerySource(Project project, URI uri,
            String title, String description,
            String srcUrl, String user, String password,
            String request, int cacheDuration,
            URI operation, Map<String, String> parameters,
            Date start) throws IOException{
     // Create new SQL source.
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
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("srcUrl", srcUrl);
            parametersE.put("password", password);
            parametersE.put("user", user);
            parametersE.put("request", request);
            parametersE.put("cacheDuration", Integer.toString(cacheDuration));
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public SqlDatabaseSource newSqlDatabaseSource(Project project, URI uri,
            					String title, String description,
            					String srcUrl, String user, String password)
            							throws IOException{
        return this.newSqlDatabaseSource(project, uri, title, description,
                srcUrl, user, password, null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public SqlDatabaseSource newSqlDatabaseSource(Project project, URI uri,
            String title, String description, String srcUrl, String user,
            String password, URI operation, Map<String, String> parameters,
            Date start) throws IOException{
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
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("srcUrl", srcUrl);
            parametersE.put("password", password);
            parametersE.put("user", user);
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
        return src;
    }
    
    /** {@inheritDoc} */
    @Override
    public SparqlSource newSparqlSource(Project project, URI uri, String title,
                                        String description, String endpointUrl,
                                        String sparqlQuery, int cacheDuration)
                                                            throws IOException {
        return this.newSparqlSource(project, uri, title, description,
                endpointUrl, sparqlQuery, cacheDuration, null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public SparqlSource newSparqlSource(Project project, URI uri, String title,
            String description, String endpointUrl, String sparqlQuery,
            int cacheDuration, URI operation, Map<String, String> parameters,
            Date start) throws IOException{
        // Create new RDF SPARQL source.
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
      //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("endpointUrl", endpointUrl);
            parametersE.put("sparqlQuery", sparqlQuery);
            parametersE.put("cacheDuration", Integer.toString(cacheDuration));
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public XmlSource newXmlSource(Project project, URI uri, String title,
                                  String description, String filePath)
                                                            throws IOException {
        return this.newXmlSource(project, uri, title, description, filePath,
                null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public XmlSource newXmlSource(Project project, URI uri, String title,
            String description, String filePath, URI operation,
            Map<String, String> parameters, Date start) throws IOException{
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
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("filePath", filePath);
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public TransformedRdfSource newTransformedRdfSource(Project project,
                                    URI uri, String title, String description,
                                    URI targetGraph, Source parent)
                                                            throws IOException {
        return this.newTransformedRdfSource(project, uri, title, description,
                                            targetGraph, null, parent);
    }

    /** {@inheritDoc} */
    @Override
    public TransformedRdfSource newTransformedRdfSource(Project project,
                                    URI uri, String title, String description,
                                    URI targetGraph, URI baseUri, Source parent)
                                                            throws IOException {
        return this.newTransformedRdfSource(project, uri, title, description,
                targetGraph, baseUri, parent, null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public TransformedRdfSource newTransformedRdfSource(Project project,
            URI uri, String title, String description,
            URI targetGraph, URI baseUri, Source parent,
            URI operation, Map<String, String> parameters,
            Date start) throws IOException{
        if (targetGraph == null) {
            throw new IllegalArgumentException("targetGraph");
        }
        // Create new transformed RDF source.
        TransformedRdfSourceImpl src =
                        new TransformedRdfSourceImpl(uri.toString(), project);
        // Set source parameters.
        this.initSource(src, title, description);
        src.setTargetGraph(targetGraph.toString());
        if (baseUri != null) {
            src.setBaseUri(baseUri.toString());
        }
        src.setParent(parent);
        // Add source to project.
        project.add(src);
        log.debug("New transformed RDF source <{}> added to project \"{}\"",
                                                    uri, project.getTitle());
      //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            if(targetGraph != null)
                parametersE.put("targetGraph", targetGraph.toString());
            if(baseUri != null)
                parametersE.put("baseUri", baseUri.toString());
            else
                parametersE.put("baseUri", null);
            parametersE.put("parent", parent.getUri());
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()), URI.create(parent.getUri()));
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public ShpSource newShpSource(Project project, URI uri, String title,
                                  String description, String shpFilePath,
                                  String shxFilePath, String dbfFilePath,
                                  String prjFilePath)
                                                            throws IOException {
        return this.newShpSource(project, uri, title, description, shpFilePath,
                shxFilePath, dbfFilePath, prjFilePath, null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public ShpSource newShpSource(Project project, URI uri, String title,
            String description, String shpFilePath, String shxFilePath,
            String dbfFilePath, String prjFilePath, URI operation,
            Map<String, String> parameters, Date start) throws IOException{
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
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("shpFilePath", shpFilePath);
            parametersE.put("shxFilePath", shxFilePath);
            parametersE.put("dbfFilePath", dbfFilePath);
            parametersE.put("prjFilePath", prjFilePath);
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public GmlSource newGmlSource(Project project, URI uri, String title,
                                  String description, String gmlFilePath,
                                  String xsdFilePath)
                                                            throws IOException {
        return this.newGmlSource(project, uri, title, description, gmlFilePath,
                xsdFilePath, null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public GmlSource newGmlSource(Project project, URI uri, String title,
            String description, String gmlFilePath, String xsdFilePath,
            URI operation, Map<String, String> parameters, Date start)
                                                        throws IOException{
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
      //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("uri", uri.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("gmlFilePath", gmlFilePath);
            parametersE.put("xsdFilePath", xsdFilePath);
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, start, new Date(), null,
                URI.create(src.getUri()));
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
        this.delete(source, deleteResources, null, null);
    }
    
    /** {@inheritDoc} */
    @Override
    public void delete(Source source, boolean deleteResources, URI operation,
            Map<String, String> parameters){
        Date eventStart = new Date();
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
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("source", source.getUri());
            parametersE.put("deleteResources", Boolean.toString(deleteResources));
        }
        this.addEvent(p, operationE, parametersE, Event.DESTRUCTION_EVENT_TYPE,
                Event.SOURCE_EVENT_SUBJECT, eventStart, new Date(), null,
                URI.create(source.getUri()));
        // Persist changes.
        this.saveProject(p);
        this.projectDao.delete(source);
        log.debug("Source <{}> removed from project \"{}\"",
                                                source.getUri(), p.getTitle());
    }

    /** {@inheritDoc} */
    @Override
    public Ontology newOntology(Project project, URI url, String title) {
        return this.newOntology(project, url, title, null, null);
    }

    /** {@inheritDoc} */
    @Override
    public Ontology newOntology(Project project, URI url, String title,
            URI operation, Map<String, String> parameters){
        Date eventStart = new Date();
        // Create new ontology.
        OntologyImpl ontology = new OntologyImpl();
        // Set ontology parameters.
        ontology.setTitle(title);
        ontology.setSource(url);
        // Add ontology to project.
        project.addOntology(ontology);
        log.debug("New ontology <{}> added to project \"{}\"",
                                                    url, project.getTitle());
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("ontology", ontology.getUri());
            parametersE.put("title", title);
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.ONTOLOGY_EVENT_SUBJECT, eventStart, new Date(), null,
                URI.create(ontology.getUri()));
        return ontology;
    }
    
    /** {@inheritDoc} */
    @Override
    public void deleteOntology(Project project, Ontology ontology) {
        this.deleteOntology(project, ontology, null, null);
    }
    
    /** {@inheritDoc} */
    @Override
    public void deleteOntology(Project project, Ontology ontology,
            URI operation, Map<String, String> parameters){
        Date eventStart = new Date();
        this.checkAvailable();
        if (project == null) {
            throw new IllegalArgumentException("project");
        }
        if (ontology == null) {
            throw new IllegalArgumentException("ontology");
        }
        // Remove ontology from project.
        ontology = project.removeOntology(ontology.getTitle());
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("ontology", ontology.getUri());
        }
        this.addEvent(project, operationE, parametersE, Event.DESTRUCTION_EVENT_TYPE,
                Event.ONTOLOGY_EVENT_SUBJECT, eventStart, new Date(), null,
                URI.create(ontology.getUri()));
        // Persist changes.
        this.saveProject(project);
        this.projectDao.delete(ontology);
        log.debug("Ontology <{}> removed form project \"{}\"",
                                    ontology.getSource(), project.getTitle());
    }

    /** {@inheritDoc} */
    @Override
    public Project newProject(URI projectId, String title,
                              String description, URI license) {
        return this.newProject(projectId, title, description, license, null,
                null);
    }

    /** {@inheritDoc} */
    @Override
    public Project newProject(URI projectId, String title,
            String description, URI license,
            URI operation, Map<String, String> parameters){
        Date eventStart = new Date();
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
        p.setModificationDate(date);
        p.setOwner(SecurityContext.getUserPrincipal());
        log.debug("New project <{}> created", p.getUri());
        //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("projectId", projectId.toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("license", license.toString());
        }
        this.addEvent(p, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.PROJECT_EVENT_SUBJECT, eventStart, new Date(), null,
                URI.create(p.getUri()));
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
        this.deleteProject(p, null, null);
    }
    
    /** {@inheritDoc} */
    @Override
    public void deleteProject(Project p, URI operation,
            Map<String, String> parameters){
        Date eventStart = new Date();
        this.checkAvailable();
        // Delete server-side resources attached to sources.
        for (Source s : p.getSources()) {
            s.delete();
        }
        // Delete project (and dependent objects: sources, ontologies...)
        this.projectDao.delete(p);
        log.debug("Project <{}> deleted", p.getUri());
        //save the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", p.getUri());
        }
        this.saveEvent(p, operationE, parametersE, Event.DESTRUCTION_EVENT_TYPE,
                Event.PROJECT_EVENT_SUBJECT, eventStart, new Date(), null,
                URI.create(p.getUri()));
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
            log.debug("Project <{}> saved to RDF store", p.getUri());
            //save events
            if(p.getEvents().isEmpty()){
                Map<String, String> parameters = new HashMap<String, String>();
                parameters.put("project", p.getUri());
                this.addEvent(p, this.createDefaultMethodOperationId(),
                        parameters, Event.UPDATE_EVENT_TYPE,
                        Event.PROJECT_EVENT_SUBJECT, null, null, null,
                        URI.create(p.getUri()));
            }
            while(!p.getEvents().isEmpty())
                this.saveEvent((Event) p.getEvents().toArray()[0]);
        }
        catch (Exception e) {
            throw new RuntimeException("Invalid project URI: " + p.getUri(), e);
        }
    }
    
    /** {@inheritDoc} */
    @Override
    public void saveProject(Project p, URI operation,
            Map<String, String> parameters, EventType eventType,
            EventSubject eventSubject, Date start, URI influenced){
      //add the event
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", p.getUri());
        }
        EventType eventTypeE = eventType;
        if(eventType == null)
            eventTypeE = Event.UPDATE_EVENT_TYPE;
        EventSubject eventSubjectE = eventSubject;
        if(eventSubject == null)
            eventSubjectE = Event.PROJECT_EVENT_SUBJECT;
        URI influencedE = influenced;
        if(influenced == null)
            influencedE = URI.create(p.getUri());
        this.addEvent(p, operationE, parametersE, eventTypeE,
                eventSubjectE, start, new Date(), null, influencedE);
        this.saveProject(p);
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
    public Event addEvent(Project project, URI operation,
            Map<String, String> parameters, EventType eventType,
            EventSubject eventSubject, Date start, Date end, URI agent,
            URI influenced, URI... used){
        //control parameters values, put the default one if absent
        URI operationE = operation;
        if(operation == null)
            operationE = URI
            .create("http://www.datalift.org/core/operation/DefaultOperation");
        EventType eventTypeE = eventType;
        if(eventType == null)
            eventTypeE = Event.INFORMATION_EVENT_TYPE;
        Date startE = start;
        if(start == null)
            startE = new Date();
        Date endE = end;
        if(end == null)
            endE = startE;
        URI agentE = agent;
        if(agent == null)
            agentE = TaskContext.getCurrent().getCurrentAgent();
        //build event uri
        StringBuilder str;
        if(project == null)
            str = new StringBuilder("http://www.datalift.org/core");
        else
            str = new StringBuilder(project.getUri());
        str.append("/event/").append(eventTypeE.getInitial());
        if(eventSubject != null)
            str.append(eventSubject.getInitial());
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm.SSS'Z'");
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        str.append("/").append(format.format(startE)).append("/");
        str.append(Double.toString(Math.random()).substring(2, 6));
        URI id = URI.create(str.toString());
        //create event and put it on the project
        EventImpl event = new EventImpl(id, project, operationE, parameters,
                eventTypeE, startE, endE, agentE, influenced,
                TaskContext.getCurrent().getCurrentEvent(), used);
        if(project != null)
            project.addEvent(event);
        return event;
    }
    
    /** {@inheritDoc} */
    @Override
    public Event saveEvent(Project project, URI operation,
            Map<String, String> parameters, EventType eventType,
            EventSubject eventSubject, Date start, Date end, URI agent,
            URI influenced, URI... used){
        Event ret = this.addEvent(project, operation, parameters, eventType,
                eventSubject, start, end, agent, influenced, used);
        return this.saveEvent(ret);
    }
    
    /** {@inheritDoc} */
    @Override
    public Event saveOutputEvent(Project project, URI operation,
            Map<String, String> parameters, Date start, Date end, URI agent,
            URI... used){
        URI operationE = operation;
        if(operation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = parameters;
        if(parameters == null){
            parametersE = new HashMap<String, String>();
            for(int i = 0; i < used.length; i++)
                parametersE.put("used " + (i + 1), used[i].toString());
        }
        return this.saveEvent(project, operationE, parametersE,
                Event.OUTPUT_EVENT_TYPE, Event.SOURCE_EVENT_SUBJECT, start, end,
                agent, null, used);
    }
    
    /** {@inheritDoc} */
    @Override
    public Event saveEvent(Event event){
        this.checkAvailable();
        Event ret = this.projectDao.save(event);
        if(event.getProject() != null)
            event.getProject().removeEvent(event);
        return ret;
    }
    
    /** {@inheritDoc} */
    @Override
    public GenericRdfDao getRdfDao(){
        return this.projectDao;
    }
    
    /** {@inheritDoc} */
    @Override
    public Workflow newWorkflow(Project project, URI url, String title,
            String description, Map<String, String> variables,
            WorkflowStep outputStep) {
        return this.newWorkflow(project, url, title, description, variables,
                outputStep, null, null, new Date());
    }
    
    /** {@inheritDoc} */
    @Override
    public Workflow newWorkflow(Project project, URI url, String title,
            String description, Map<String, String> variables,
            WorkflowStep outputStep,  URI eventOperation,
            Map<String, String> eventParameters, Date eventStart){
        Date eventStartE = eventStart;
        if(eventStart == null)
            eventStartE = new Date();
        // Create the workflow
        WorkflowImpl wfl = new WorkflowImpl(url, title, description, variables,
                (WorkflowStepImpl) outputStep);
        // Add it to the project
        project.addWorkflow(wfl);
        log.debug("New workflow <{}> added to project \"{}\"",
                                                    url, project.getTitle());
      //add the event
        URI operationE = eventOperation;
        if(eventOperation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = eventParameters;
        if(eventParameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("workflow", wfl.getUri().toString());
            parametersE.put("title", title);
            parametersE.put("description", description);
            parametersE.put("variables", new JsonStringParameters(variables)
                                                                .toString());
            parametersE.put("outputStep", outputStep.toString());
        }
        this.addEvent(project, operationE, parametersE, Event.CREATION_EVENT_TYPE,
                Event.WORKFLOW_EVENT_SUBJECT, eventStartE, new Date(), null,
                wfl.getUri());
        return wfl;
    }

    /** {@inheritDoc} */
    @Override
    public void deleteWorkflow(Project project, Workflow workflow) {
        this.deleteWorkflow(project, workflow, null, null);
    }
    
    /** {@inheritDoc} */
    @Override
    public void deleteWorkflow(Project project, Workflow workflow,
            URI eventOperation, Map<String, String> eventParameters){
        Date eventStart = new Date();
        this.checkAvailable();
        if (project == null) {
            throw new IllegalArgumentException("project");
        }
        if (workflow == null) {
            throw new IllegalArgumentException("workflow");
        }
        project.removeWorkflow(workflow.getUri());
        //add the event
        URI operationE = eventOperation;
        if(eventOperation == null)
            operationE = this.createDefaultMethodOperationId();
        Map<String, String> parametersE = eventParameters;
        if(eventParameters == null){
            parametersE = new HashMap<String, String>();
            parametersE.put("project", project.getUri());
            parametersE.put("workflow", workflow.getUri().toString());
        }
        this.addEvent(project, operationE, parametersE, Event.DESTRUCTION_EVENT_TYPE,
                Event.WORKFLOW_EVENT_SUBJECT, eventStart, new Date(), null,
                workflow.getUri());
        // Persist changes.
        this.saveProject(project);
        this.projectDao.delete(workflow);
        log.debug("Workflow <{}> removed form project \"{}\"",
                                    workflow.getTitle(), project.getTitle());
    }

    /** {@inheritDoc} */
    @Override
    public WorkflowStep NewWorkflowStep(URI operation,
            Map<String, String> parameters) {
        return new WorkflowStepImpl(operation, parameters);
    }

    @Override
    public Event getEvent(URI uri) {
        return this.projectDao.find(EventImpl.class, uri);
    }

    @Override
    public Map<URI, Event> getEventsAbout(URI uri) {
        RdfsClass rdfsClass = EventImpl.class.getAnnotation(RdfsClass.class);
        if (rdfsClass == null) {
            throw new IllegalArgumentException(EventImpl.class.getName());
        }
        String rdfType = rdfsClass.value();
        if(this.getQnamePrefix(rdfType) == null)
            rdfType = "<" + rdfType + ">";
        Map<URI, Event> results = new HashMap<URI, Event>();
        EntityManager em = this.emf.createEntityManager();
        try {
            Query query = em.createQuery(
                                "where { ?result rdf:type " + rdfType +
                                        " . ?result prov:influenced " +
                                        uri.toString() + "}");
            query.setHint(RdfQuery.HINT_ENTITY_CLASS, EventImpl.class);
            for (Object p : query.getResultList()) {
                results.put(((Event)p).getUri(), (Event)p);
            }
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
        return results;
    }

    @Override
    public Map<URI, Event> getEvents() {
        RdfsClass rdfsClass = EventImpl.class.getAnnotation(RdfsClass.class);
        if (rdfsClass == null) {
            throw new IllegalArgumentException(EventImpl.class.getName());
        }
        String rdfType = rdfsClass.value();
        if(this.getQnamePrefix(rdfType) == null)
            rdfType = "<" + rdfType + ">";
        Map<URI, Event> results = new HashMap<URI, Event>();
        EntityManager em = this.emf.createEntityManager();
        try {
            Query query = em.createQuery(
                                "where { ?result rdf:type " + rdfType + "}");
            query.setHint(RdfQuery.HINT_ENTITY_CLASS, EventImpl.class);
            for (Object p : query.getResultList()) {
                results.put(((Event)p).getUri(), (Event)p);
            }
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
        return results;
    }

    @Override
    public Map<URI, Event> getEvents(Project project) {
        RdfsClass rdfsClass = EventImpl.class.getAnnotation(RdfsClass.class);
        if (rdfsClass == null) {
            throw new IllegalArgumentException(EventImpl.class.getName());
        }
        String rdfType = rdfsClass.value();
        if(this.getQnamePrefix(rdfType) == null)
            rdfType = "<" + rdfType + ">";
        Map<URI, Event> results = new HashMap<URI, Event>();
        EntityManager em = this.emf.createEntityManager();
        try {
            Query query;
            if(project == null){
                query = em.createQuery(
                        "where { ?result rdf:type " + rdfType +
                        " . FILTER NOT EXISTS { ?result datalift:project ?p }");
            } else {
                String pUri = project.getUri().toString();
                if(this.getQnamePrefix(pUri) == null)
                    pUri = "<" + pUri + ">";
                query = em.createQuery(
                        "where { ?result rdf:type " + rdfType +
                        " . ?result datalift:project " + pUri + "}");
            }
            query.setHint(RdfQuery.HINT_ENTITY_CLASS, EventImpl.class);
            for (Object p : query.getResultList()) {
                results.put(((Event)p).getUri(), (Event)p);
            }
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
        return results;
    }
    
    @Override
    public Map<URI, Event> getOutputEvents(Project project) {
        RdfsClass rdfsClass = EventImpl.class.getAnnotation(RdfsClass.class);
        if (rdfsClass == null) {
            throw new IllegalArgumentException(EventImpl.class.getName());
        }
        if (project == null) {
            throw new IllegalArgumentException("project");
        }
        String rdfType = rdfsClass.value();
        if(this.getQnamePrefix(rdfType) == null)
            rdfType = "<" + rdfType + ">";
        Map<URI, Event> results = new HashMap<URI, Event>();
        EntityManager em = this.emf.createEntityManager();
        try {
            String pUri = project.getUri().toString();
            if(this.getQnamePrefix(pUri) == null)
                pUri = "<" + pUri + ">";
            Query query = em.createQuery(
                    "where { ?result rdf:type " + rdfType +
                    " . ?result datalift:project " + pUri +
                    " . ?result datalift:eventType <" +
                    Event.OUTPUT_EVENT_TYPE.getUri().toString() + ">}");
            query.setHint(RdfQuery.HINT_ENTITY_CLASS, EventImpl.class);
            for (Object p : query.getResultList()) {
                results.put(((Event)p).getUri(), (Event)p);
            }
        }
        catch (javax.persistence.PersistenceException e) {
            throw new PersistenceException(e);
        }
        return results;
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
     * return the namespace used
     * @param qname the qname or URI
     * @return  the namespace used as a prefix on the qname or null if qname is
     * an URI without prefix or if the namespace is unknown
     */
    private RdfNamespace getQnamePrefix(String qname){
        for(RdfNamespace ns : RdfNamespace.values())
            if(qname.startsWith(ns.prefix + ":"))
                return ns;
        return null;
    }
    
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
                    SqlQuerySourceImpl.class, SqlDatabaseSourceImpl.class,
                    SparqlSourceImpl.class, XmlSourceImpl.class,
                    TransformedRdfSourceImpl.class, ShpSourceImpl.class,
                    GmlSourceImpl.class, EventImpl.class));
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
     * @param  cfg   the Datalift configuration.
     *
     * @return a configured Empire EntityManagerFactory.
     */
    private EntityManagerFactory createEntityManagerFactory(Configuration cfg) {
        // Build Empire configuration.
        Map<String,String> props = new HashMap<String,String>();
        Map<String,Map<String,String>> unitCfg =
                                    new HashMap<String, Map<String, String>>();
        // Set Empire global options.
        props.put("STRICT_MODE", Boolean.FALSE.toString());     // Lax mode.
        // Register persistence classes through Datalift configuration to
        // preserve originating classloader information that would be lost
        // by passing through Empire configuration that only accepts strings.
        cfg.registerBean(CustomAnnotationProvider.ANNOTATED_CLASSES_PROP,
                         Collections.unmodifiableCollection(this.classes));
        // Register the path of each persistent class to Javassist bytecode
        // generation tool (otherwise object instantiation will fail in
        // multi-classloader environments (such as web apps)).
        ClassPool cp = ClassPool.getDefault();
        for (Class<?> c : this.classes) {
            // cp.insertClassPath(new ClassClassPath(c));
            cp.appendClassPath(new LoaderClassPath(c.getClassLoader()));
        }
        if (log.isDebugEnabled()) {
            String classList = join(this.classes, ",").replace("class ", "");
            log.debug("Registered persistent classes: {}", classList);
        }
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
                cfg.getInternalRepository().getNativeRepository());
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
    }

    private void checkAvailable() {
        if (this.emf == null) {
            throw new IllegalStateException("Not available");
        }
    }

    private URI createDefaultMethodOperationId(){
        StringBuilder str = new StringBuilder(DEFAULT_OPERATIONS_BASE_URI);
        StackTraceElement[] st = Thread.currentThread().getStackTrace();
        str.append(st[2].getMethodName());
        return URI.create(str.toString());
    }

}

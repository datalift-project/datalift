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

package org.datalift.fwk.project;


import java.io.IOException;
import java.net.URI;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.prov.Event;
import org.datalift.fwk.prov.EventSubject;
import org.datalift.fwk.prov.EventType;


/**
 * Project managers are responsible for handling the lifecycle and the
 * persistence of the data-lifting projects.
 * <p>
 * An implementation of this interface shall be provided by the
 * framework implementation. Datalift module can retrieve it through
 * the DataLift runtime
 * {@link org.datalift.fwk.Configuration#getBean(Class) configuration},
 * during or after the post-initialization step:</p>
 * <blockquote><pre>
 *   ProjectManager pm = configuration.getBean(ProjectManager.class);
 * </pre></blockquote>
 *
 * @author hdevos
 */
public interface ProjectManager
{
    /**
     * Finds a project object in the DataLift internal RDF repository
     * from its URI.
     * @param  uri   the project URI.
     *
     * @return a project object or <code>null</code> if no Project
     *         object matching the specified URI was found.
     */
    public Project findProject(URI uri);

    /**
     * Returns a list of all projects known in the DataLift internal
     * RDF repository.
     * @return a list of all known projects, possible empty.
     */
    public Collection<Project> listProjects();

    /**
     * Creates a new project.
     * @param  projectId   the project identifier as a URI
     * @param  title       the project name/title.
     * @param  description a short description of the project
     * @param  license     the project license.
     * @return a project object.
     */
    public Project newProject(URI projectId, String title,
                              String description, URI license);
    
    /**
     * Creates a new project.
     * With event informations
     * 
     * @param  projectId   the project identifier as a URI
     * @param  title       the project name/title.
     * @param  description a short description of the project
     * @param  license     the project license.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @return a project object.
     */
    public Project newProject(URI projectId, String title,
                              String description, URI license,
                              URI operation, Map<String, Object> parameters);

    /**
     * Marks the specified project for being persisted into permanent
     * storage.
     * @param  p   the project to save or update in the RDF store.
     */
    public void saveProject(Project p);

    /**
     * Removes the specified project from the DataLift internal RDF
     * repository.
     * @param  p   the project to remove.
     */
    public void deleteProject(Project p);
    
    /**
     * Removes the specified project from the DataLift internal RDF
     * repository.
     * With event informations
     * 
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  p   the project to remove.
     */
    public void deleteProject(Project p, URI operation,
            Map<String, Object> parameters);

    /**
     * Creates a new CSV source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  filePath      the CSV file path in the public storage.
     * @param  separator     the column separator character.
     *
     * @return a new CSV source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public CsvSource newCsvSource(Project project, URI uri, String title,
                                  String description, String filePath,
                                  char separator) throws IOException;
    
    /**
     * Creates a new CSV source object.
     * With event informations
     * 
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  filePath      the CSV file path in the public storage.
     * @param  separator     the column separator character.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new CSV source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public CsvSource newCsvSource(Project project, URI uri, String title,
                                  String description, String filePath,
                                  char separator, URI operation,
                                  Map<String, Object> parameters, Date start)
                                  throws IOException;

    /**
     * Creates a new RDF source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  baseUri       the base URI for resolving relative URIs or
     *                       </code>null</code>.
     * @param  filePath      the RDF file path in the public storage.
     * @param  mimeType      the RDF data format as a MIME type.
     *                       Supported types are:
     *                       {@link MediaTypes#TEXT_TURTLE},
     *                       {@link MediaTypes#TEXT_N3},
     *                       {@link MediaTypes#APPLICATION_RDF_XML}
     *                       and {@link MediaTypes#APPLICATION_XML}.
     *
     * @return a new RDF source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public RdfFileSource newRdfSource(Project project, URI uri, String title,
                                      String description, URI baseUri,
                                      String filePath, String mimeType)
                                                            throws IOException;
    /**
     * Creates a new RDF source object.
     * With event informations
     * 
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  baseUri       the base URI for resolving relative URIs or
     *                       </code>null</code>.
     * @param  filePath      the RDF file path in the public storage.
     * @param  mimeType      the RDF data format as a MIME type.
     *                       Supported types are:
     *                       {@link MediaTypes#TEXT_TURTLE},
     *                       {@link MediaTypes#TEXT_N3},
     *                       {@link MediaTypes#APPLICATION_RDF_XML}
     *                       and {@link MediaTypes#APPLICATION_XML}.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new RDF source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public RdfFileSource newRdfSource(Project project, URI uri, String title,
                                      String description, URI baseUri,
                                      String filePath, String mimeType, 
                                      URI operation, Map<String, Object> parameters,
                                      Date start) throws IOException;
    
    /**
     * Creates a new database's resultset source object.
     * @param  project         the owning project.
     * @param  uri             the source URI.
     * @param  title           the source label.
     * @param  description     the description of the source content or
     *                         intent.
     * @param  srcUrl          the connection string of the database.
     * @param  user            username for connection.
     * @param  password        password for connection.
     * @param  request         SQL query to extract data.
     * @param  cacheDuration   duration of local data cache.
     *
     * @return a new SQL query source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the configured database.
     */
    public SqlQuerySource newSqlQuerySource(Project project, URI uri,
                                  String title, String description,
                                  String srcUrl, String user, String password,
                                  String request, int cacheDuration)
                                                            throws IOException;

    /**
     * Creates a new database's resultset source object.
     * With event informations
     * 
     * @param  project         the owning project.
     * @param  uri             the source URI.
     * @param  title           the source label.
     * @param  description     the description of the source content or
     *                         intent.
     * @param  srcUrl          the connection string of the database.
     * @param  user            username for connection.
     * @param  password        password for connection.
     * @param  request         SQL query to extract data.
     * @param  cacheDuration   duration of local data cache.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new SQL query source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the configured database.
     */
    public SqlQuerySource newSqlQuerySource(Project project, URI uri,
                                  String title, String description,
                                  String srcUrl, String user, String password,
                                  String request, int cacheDuration,
                                  URI operation, Map<String, Object> parameters,
                                  Date start) throws IOException;
    
    /**
     * Creates a new database source object.
     * @param  project         the owning project.
     * @param  uri             the source URI.
     * @param  title           the source label.
     * @param  description     the description of the source content or
     *                         intent.
     * @param  srcUrl          the connection string of the database.
     * @param  user            username for connection.
     * @param  password        password for connection.
     *
     * @return a new SQL database source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the configured database.
     */
    public SqlDatabaseSource newSqlDatabaseSource(Project project, URI uri,
                                  String title, String description,
                                  String srcUrl, String user, String password)
                                                            throws IOException;

    /**
     * Creates a new database source object.
     * With event informations
     * 
     * @param  project         the owning project.
     * @param  uri             the source URI.
     * @param  title           the source label.
     * @param  description     the description of the source content or
     *                         intent.
     * @param  srcUrl          the connection string of the database.
     * @param  user            username for connection.
     * @param  password        password for connection.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new SQL database source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the configured database.
     */
    public SqlDatabaseSource newSqlDatabaseSource(Project project, URI uri,
                                  String title, String description,
                                  String srcUrl, String user, String password,
                                  URI operation, Map<String, Object> parameters,
                                  Date start) throws IOException;

    /**
     * Creates a new SPARQL source object.
     * @param  project         the owning project.
     * @param  uri             the source URI.
     * @param  title           the source label.
     * @param  description     the description of the source content or
     *                         intent.
     * @param  endpointUrl     the URL of the HTTP SPARQL endpoint.
     * @param  sparqlQuery     the SPARQL query (SELECT, CONSTRUCT or
     *                         DESCRIBE).
     * @param  cacheDuration   duration of local data cache.
     *
     * @return a new SPARQL source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the underlying data.
     */
    public SparqlSource newSparqlSource(Project project, URI uri, String title,
                                        String description, String endpointUrl,
                                        String sparqlQuery, int cacheDuration)
                                                            throws IOException;
    
    /**
     * Creates a new SPARQL source object.
     * With event informations
     * 
     * @param  project         the owning project.
     * @param  uri             the source URI.
     * @param  title           the source label.
     * @param  description     the description of the source content or
     *                         intent.
     * @param  endpointUrl     the URL of the HTTP SPARQL endpoint.
     * @param  sparqlQuery     the SPARQL query (SELECT, CONSTRUCT or
     *                         DESCRIBE).
     * @param  cacheDuration   duration of local data cache.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new SPARQL source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the underlying data.
     */
    public SparqlSource newSparqlSource(Project project, URI uri, String title,
                                        String description, String endpointUrl,
                                        String sparqlQuery, int cacheDuration,
                                        URI operation,
                                        Map<String, Object> parameters,
                                        Date start) throws IOException;

    /**
     * Creates a new XML source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  filePath      the XML file path in the public storage.
     *
     * @return a new XML source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public XmlSource newXmlSource(Project project, URI uri, String title,
                                  String description, String filePath)
                                                            throws IOException;
    /**
     * Creates a new XML source object.
     * With event informations
     * 
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  filePath      the XML file path in the public storage.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new XML source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public XmlSource newXmlSource(Project project, URI uri, String title,
                                  String description, String filePath,
                                  URI operation, Map<String, Object> parameters,
                                  Date start) throws IOException;

    /**
     * Creates a new transformed RDF source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  targetGraph   the URI of the target named graph.
     * @param  parent        the parent source.
     *
     * @return a new transformed RDF source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the underlying data.
     */
    public TransformedRdfSource newTransformedRdfSource(Project project,
                                URI uri, String title, String description,
                                URI targetGraph, Source parent)
                                                            throws IOException;

    /**
     * Creates a new transformed RDF source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  targetGraph   the URI of the target named graph.
     * @param  baseUri       the URI that was used to compute the URIs
     *                       of the resources and predicates of the
     *                       source.
     * @param  parent        the parent source.
     *
     * @return a new transformed RDF source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the underlying data.
     */
    public TransformedRdfSource newTransformedRdfSource(Project project,
                                URI uri, String title, String description,
                                URI targetGraph, URI baseUri, Source parent)
                                                            throws IOException;
    
    /**
     * Creates a new transformed RDF source object.
     * With event informations
     * 
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  targetGraph   the URI of the target named graph.
     * @param  baseUri       the URI that was used to compute the URIs
     *                       of the resources and predicates of the
     *                       source.
     * @param  parent        the parent source.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new transformed RDF source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the underlying data.
     */
    public TransformedRdfSource newTransformedRdfSource(Project project,
                                URI uri, String title, String description,
                                URI targetGraph, URI baseUri, Source parent,
                                URI operation, Map<String, Object> parameters,
                                Date start) throws IOException;

    /**
     * Creates a new Shapefile source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  shpFilePath      the SHP file path in the public storage.
     * @param  shxFilePath      the SHX file path in the public storage.
     * @param  dbfFilePath      the DBF file path in the public storage.
     * @param  prjFilePath      the PRJ file path in the public storage.
     *
     * @return a new Shapefile source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public ShpSource newShpSource(Project project, URI uri, String title,
                                  String description, String shpFilePath,
                                  String shxFilePath, String dbfFilePath,
                                  String prjFilePath)
                                                            throws IOException;
    
    /**
     * Creates a new Shapefile source object.
     * With event informations
     * 
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  shpFilePath      the SHP file path in the public storage.
     * @param  shxFilePath      the SHX file path in the public storage.
     * @param  dbfFilePath      the DBF file path in the public storage.
     * @param  prjFilePath      the PRJ file path in the public storage.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     *
     * @return a new Shapefile source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public ShpSource newShpSource(Project project, URI uri, String title,
                                  String description, String shpFilePath,
                                  String shxFilePath, String dbfFilePath,
                                  String prjFilePath, URI operation,
                                  Map<String, Object> parameters, Date start)
                                                            throws IOException;


    /**
     * Creates a new GML source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  gmlFilePath      the GML file path in the public storage.
     * @param  xsdFilePath      the XSD file path in the public storage.
     * @return a new GML source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public GmlSource newGmlSource(Project project, URI uri, String title,
                                  String description, String gmlFilePath,
                                  String xsdFilePath)
                                                            throws IOException;

    /**
     * Creates a new GML source object.
     * With event informations
     * 
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  gmlFilePath      the GML file path in the public storage.
     * @param  xsdFilePath      the XSD file path in the public storage.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @param  start        the event start time
     * 
     * @return a new GML source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public GmlSource newGmlSource(Project project, URI uri, String title,
                                  String description, String gmlFilePath,
                                  String xsdFilePath, URI operation,
                                  Map<String, Object> parameters, Date start)
                                                            throws IOException;
    
    /**
     * Deletes the specified source object and the associated resources
     * (local files, cached data...).
     * @param  source   the source object to delete.
     */
    public void delete(Source source);

    /**
     * Deletes the specified source object and, optionally, the
     * associated resources (local files, cached data...).
     * @param  source            the source object to delete.
     * @param  deleteResources   whether to delete the resources
     *                           associated with the source.
     */
    public void delete(Source source, boolean deleteResources);
    
    /**
     * Deletes the specified source object and, optionally, the
     * associated resources (local files, cached data...).
     * With event informations
     * 
     * @param  source            the source object to delete.
     * @param  deleteResources   whether to delete the resources
     *                           associated with the source.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     */
    public void delete(Source source, boolean deleteResources, URI operation,
            Map<String, Object> parameters);

    /**
     * Create a new ontology description.
     * @param  srcUrl   the ontology URL.
     * @param  title    the ontology name or description.
     *
     * @return a new ontology description, ready to be
     *         {@link Project#addOntology(Ontology) associated} to a
     *         project.
     */
    public Ontology newOntology(Project project, URI srcUrl, String title);
    
    /**
     * Create a new ontology description.
     * With event informations
     * 
     * @param  srcUrl   the ontology URL.
     * @param  title    the ontology name or description.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     * @return a new ontology description, ready to be
     *         {@link Project#addOntology(Ontology) associated} to a
     *         project.
     */
    public Ontology newOntology(Project project, URI srcUrl, String title,
            URI operation, Map<String, Object> parameters);

    /**
     * Deletes the specified ontology from the project.
     * @param  project    the owning project.
     * @param  ontology   the ontology to remove.
     */
    public void deleteOntology(Project project, Ontology ontology);
    
    /**
     * Deletes the specified ontology from the project.
     * With event informations
     * 
     * @param  project    the owning project.
     * @param  ontology   the ontology to remove.
     * @param  operation    the operation of the event
     * @param  parameters   the parameters of the event
     */
    public void deleteOntology(Project project, Ontology ontology,
            URI operation, Map<String, Object> parameters);

    /**
     * Builds the path to the project directory or to the specified
     * file belonging to the project. The returned path is relative to
     * the DataLift public storage directory.
     * @param  projectId   the project identifier, i.e. the last member
     *                     of the project URI.
     * @param  fileName    the file name or <code>null</code>.
     *
     * @return the path to the project directory or file.
     */
    public String getProjectFilePath(String projectId, String fileName);

    /**
     * Register the specified classes as persistent classes. These
     * classes shall be annotated with
     * <a href="http://en.wikipedia.org/wiki/Java_Persistence_API">JPA</a>
     * and <a href="https://github.com/mhgrove/Empire">Empire</a>
     * annotations.
     * @param  classes   the persistent classes.
     */
    public void addPersistentClasses(Collection<Class<?>> classes);
    
    /**
     * Declare that an event happened and persist it
     * 
     * @param  project   the project this event is associated with
     * @param  operation   the uri of the operation associated with this event
     * @param  parameters   the parameters used during the event
     * @param  eventType   the type of event
     * @param  eventSubject   the type of the influenced entity
     * @param  start   date of the event begining
     * @param  end   date of the event end
     * @param  agent   the agent who triggered the event
     * @param  influenced   the uri of the influenced entity
     * @param  used   the list of entity used during the event
     */
    public Event addEvent(Project project, URI operation,
            Map<String, Object> parameters, EventType eventType,
            EventSubject eventSubject, Date start, Date end, URI agent,
            URI influenced, URI... used);
}

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

import org.datalift.fwk.MediaTypes;


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
    public Collection<? extends Project> listProjects();

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
     * Creates a new CSV source object.
     * @param  project       the owning project.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  description   the description of the source content or
     *                       intent.
     * @param  filePath      the CSV file path in the public storage.
     * @param  separator     the column separator character.
     * @param  hasTitleRow   whether the first row holds the column
     *                       titles.
     * @return a new CSV source, associated to the specified project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public CsvSource newCsvSource(Project project, URI uri, String title,
                                  String description, String filePath,
                                  char separator, boolean hasTitleRow)
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
     * Creates a new database source object.
     * @param  project         the owning project.
     * @param  uri             the source URI.
     * @param  title           the source label.
     * @param  description     the description of the source content or
     *                         intent.
     * @param  connectionUrl   the connection string of the database.
     * @param  user            username for connection.
     * @param  password        password for connection.
     * @param  request         SQL query to extract data.
     * @param  cacheDuration   duration of local data cache.
     *
     * @return a new SQL database source, associated to the specified
     *         project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the configured database.
     */
    public SqlSource newSqlSource(Project project, URI uri,
                                  String title, String description, 
                                  String srcUrl, String user, String password, 
                                  String request, int cacheDuration)
                                                            throws IOException;

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
     * Deletes the specified source object and the associated resources
     * (local files, cached data...).
     * @param  source   the source object to delete.
     */
    public void delete(Source source);

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

    public void deleteOntology(Project project, Ontology ontology);

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
}

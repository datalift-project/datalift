/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
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


public interface ProjectManager
{
    /**
     * Finds a Project object in the RDF repository from its URI.
     * @param  uri   the project URI.
     *
     * @return a Project object or <code>null</code> if no Project
     *         object matching the specified URI was found.
     */
    public Project findProject(URI uri);

    /**
     * Returns a list of all Projects known in the RDF repository.
     * @return a list of all known Projects.
     */
    public Collection<Project> listProjects();

    /**
     * Creates a new CSV source object.
     * @param  uri           the source URI.
     * @param  title         the source labelProject p = .
     * @param  filePath      the CSV file path in the public storage.
     * @param  separator     the column separator character.
     * @param  hasTitleRow   whether the first row holds the column
     *                       titles.
     * @return a new CSV source, ready to be
     *         {@link Project#addSource(Source) associated} to a
     *         Project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public CsvSource newCsvSource(URI uri, String title, String filePath,
                                  char separator, boolean hasTitleRow)
                                                            throws IOException;

    /**
     * Creates a new RDF source object.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  filePath      the RDF file path in the public storage.
     * @param  mimeType      the RDF file data format as a MIME type:
     *  supported types are: {@link MediaTypes#TEXT_TURTLE},
     *  {@link MediaTypes#TEXT_N3}, {@link MediaTypes#APPLICATION_RDF_XML}
     *  and {@link MediaTypes#APPLICATION_XML}.
     *
     * @return a new RDF source, ready to be
     *         {@link Project#addSource(Source) associated} to a
     *         Project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public RdfSource newRdfSource(URI uri, String title, String filePath,
                                  String mimeType) throws IOException;
    
    /**
     * Creates a new Database source object.
     * @param  uri           the source URI.
     * @param  title         the source label.
     * @param  database      the name of the database
     * @param  srcUrl        the connection string of the database
     * @param  user          username for connection
     * @param  password      password for connection
     * @param  request       SQL query to extract data
     * @param  cacheDuration duration of local data cache
     *
     * @return a new Database source, ready to be
     *         {@link Project#addSource(Source) associated} to a
     *         Project.
     * @throws IOException if any error occurred creating the source
     *         or accessing the specified file.
     */
    public DbSource newDbSource(URI uri, String title, String database, 
                                String srcUrl, String user, String password, 
                                String request, int cacheDuration)
                                                            throws IOException;

    public TransformedRdfSource newTransformedRdfSource(URI uri, String title, 
                                URI targetGraph, Source parent);
    
    public Ontology newOntology(URI srcUrl, String title);

    public Project newProject(URI projectId, String title,
                              String description, URI license);

    public String getProjectFilePath(String projectId, String fileName);

    public void deleteProject(Project p);

    public void saveProject(Project p);

    public void addPersistentClasses(Collection<Class<?>> classes);
}

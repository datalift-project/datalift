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
     * @param  title         the source label.
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
     * @param  database      the name of the Database	
     * @param  srcUrl        the connection string of the Database	
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
			String request, int cacheDuration) throws IOException;
}

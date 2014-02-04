/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *                  A. Valensi
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

import java.util.Date;


/**
 * A source of data, external (file, URL, database query...) or
 * internal (named graph, SPARQL query...).
 *
 * @author hdevos
 */
public interface Source extends Entity
{
    //-------------------------------------------------------------------------
    // SourceType enumeration
    //-------------------------------------------------------------------------

    /**
     * The supported DataLift source types.
     */
    public enum SourceType {
        RdfFileSource,
        CsvSource,
        SqlQuerySource,
        SqlDatabaseSource,
        TransformedRdfSource,
        SparqlSource,
        XmlSource,
        ShpSource,
        GmlSource;
    }

    //-------------------------------------------------------------------------
    // Source contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the source identifier as a URI.
     * @return the source identifier.
     */
    public String getUri();

    /**
     * Returns the source title.
     * @return the source title.
     */
    public String getTitle();

    /**
     * Sets the source title.
     * @param  title   the source title string.
     */
    public void setTitle(String title);

    /**
     * Returns the source description.
     * @return the source description.
     */
    public String getDescription();

    /**
     * Sets the source description.
     * @param  description   the source description string.
     */
    public void setDescription(String description);

    /**
     * Returns the origin URL for this source.
     * @return the origin URL.
     */
    public String getSourceUrl();

    /**
     * Sets the origin URL for this source.
     * @param  url   the origin URL.
     */
    public void setSourceUrl(String url);

    /**
     * Returns the creation date of this source.
     * @return the creation date.
     */
    public Date getCreationDate();

    /**
     * Returns the operator that created this source.
     * @return the operator name.
     */
    public String getOperator();

    /**
     * Returns the {@link SourceType source type}.
     * @return the source type.
     */
    public SourceType getType();

    /**
     * Returns the {@link Project} object this source belongs to.
     * @return the owning project.
     */
    public Project getProject();
    
    /**
     * Returns a String which contain all the notes took for a Source
     * @return the string with the notes.
     */
    public String getNotes();
    
    /**
     * Sets the String with the notes that a user takes for a source.
     * @param  notes   the String that contains the note.
     */
    public void setNotes(String notes);

    /**
     * Deletes all resources associated to this source (uploaded files,
     * temporary files, named graphs...).
     */
    public void delete();
    
    /**
     * Returns the {@link User} object this source belongs to.
     * @return the owning user.
     */
    public User getWasAttributedTo();

    /**
     * Set the {@link User} object this source belongs to.
     * @param the {@link User} object this source belongs to.
     */
    public void setWasAttributedTo(User user);

    /**
     * Get the {@link Source} object this source derived from.
     * @return the {@link Source} object this source derived from.
     */
    public Source getWasDerivedFrom();

    /**
     * Set the {@link Source} object this source derived from.
     * @param the {@link Source} object this source derived from.
     */
    public void setWasDerivedFrom(Source source);

    /**
     * Get the {@link Event} object this source was generated by.
     * @return the {@link Event} object this source was generated by.
     */
    public Event getWasGeneratedBy();

    /**
     * Set the {@link Event} object this source was generated by.
     * @param the {@link Event} object this source was generated by.
     */
    public void setWasGeneratedBy(Event event);

}

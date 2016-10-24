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


import java.net.URLDecoder;
import javax.persistence.MappedSuperclass;

import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.util.StringUtils;
import org.datalift.fwk.util.web.Charsets;


/**
 * An abstract superclass for implementations of the {@link Source}
 * interface.
 *
 * @author hdevos
 */
@MappedSuperclass
public abstract class BaseSource extends BaseRdfEntity implements Source
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfId
    private String uri;
    @RdfProperty("dc:title")
    private String title;
    @RdfProperty("dc:description")
    private String description;
    @RdfProperty("dc:source")
    private String source;
    @RdfProperty("datalift:project")
    private Project project;

    private transient final SourceType type;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new source of the specified type.
     * @param  type   the {@link SourceType source type}.
     *
     * @throws IllegalArgumentException if <code>type</code> is
     *         <code>null</code>.
     */
    protected BaseSource(SourceType type) {
        this(type, null, null);
    }

    /**
     * Creates a new source of the specified type, identifier and
     * owning project.
     * @param  type      the {@link SourceType source type}.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if <code>type</code> is
     *         <code>null</code> or if <code>uri</code> is specified
     *         but <code>project</code> is <code>null</code>.
     */
    protected BaseSource(SourceType type, String uri, Project project) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        if (uri != null) {
            uri = uri.trim();
            if (uri.length() == 0) {
                throw new IllegalArgumentException("uri");
            }
        }
        if ((StringUtils.isSet(uri)) && (project == null)) {
            throw new IllegalArgumentException("project");
        }
        this.type    = type;
        this.uri     = uri;
        this.project = project;
    }

    //-------------------------------------------------------------------------
    // Source contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getUri() {
        return this.uri;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle(){
        return this.title;
    }

    /** {@inheritDoc} */
    @Override
    public void setTitle(String title) {
        this.title = title;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return this.description;
    }

    /** {@inheritDoc} */
    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    /** {@inheritDoc} */
    @Override
    public String getSourceUrl() {
        return this.source;
    }

    /** {@inheritDoc} */
    @Override
    public void setSourceUrl(String source) {
        this.source = source;
    }

    /** {@inheritDoc} */
    @Override
    public SourceType getType() {
        return this.type;
    }

    /** {@inheritDoc} */
    @Override
    public Project getProject() {
        return this.project;
    }

    /** {@inheritDoc} */
    @Override
    public void delete() {
        // NOP
    }

    //-------------------------------------------------------------------------
    // BaseRdfEntity contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected void setId(String id) {
        // Somewhere between Empire and Sesame, the object id. URIs get
        // escaped (the non-Latin unicode characters are replaced by
        // escape sequences: %...). We need to fix that when reading these
        // URIs from the triple store and installing them as object id.
        try {
            this.uri = URLDecoder.decode(id, Charsets.UTF8_CHARSET);
        }
        catch (Exception e) {
            // Can't happen: UTF-8 charset is supported by all JVMs.
            this.uri = id;
        }
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder(64);
        b.append(this.getUri())
         .append(" (").append(this.getClass().getSimpleName())
         .append(", \"").append(this.getTitle())
         .append("\")");
        return b.toString();
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Complementary {@link #toString()} method to let subclasses
     * complement the default string representation without overriding
     * the whole {@link #toString()} method.
     * @param  b   the being-build string representation of this source.
     *
     * @return the buffer, augmented with the subclass-specific
     *         complement.
     */
    protected StringBuilder toString(StringBuilder b) {
        return b;
    }
}

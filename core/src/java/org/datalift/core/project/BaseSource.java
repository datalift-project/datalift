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
import java.io.IOException;
import java.net.URI;

import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Source;

import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;


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

    private final SourceType type;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    protected BaseSource(SourceType type) {
        this(type, null);
    }

    protected BaseSource(SourceType type, String uri) {
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        this.type = type;
        this.uri  = uri;
    }

    //-------------------------------------------------------------------------
    // Source contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getTitle(){
        return title;
    }
    /** {@inheritDoc} */
    @Override
    public void setTitle(String t) {
        title = t;
    }

    /** {@inheritDoc} */
    @Override
    public final SourceType getType() {
        return this.type;
    }

    //-------------------------------------------------------------------------
    // BaseRdfEntity contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected void setId(String id) {
        this.uri = id;
    }

    //-------------------------------------------------------------------------
    // BaseSource contract definition
    //-------------------------------------------------------------------------

    public String getUri() {
        return this.uri;
    }

    abstract public void init(File docRoot, URI baseUri) throws IOException;
}

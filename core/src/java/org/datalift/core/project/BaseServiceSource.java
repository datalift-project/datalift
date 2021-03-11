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


import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ServiceSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.util.StringUtils;

import com.clarkparsia.empire.annotation.RdfProperty;


@MappedSuperclass
public abstract class BaseServiceSource extends BaseSource
                                        implements ServiceSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("pav:version")
    private String version;
    @RdfProperty("dc:publisher")
    private String publisher;
    @RdfProperty("datalift:serverTypeStrategy")
    private String serverTypeStrategy;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    protected BaseServiceSource(SourceType type) {
        super(type);
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
     * @throws IllegalArgumentException if any of <code>type</code>,
     *         <code>uri</code> or <code>project</code> is
     *         <code>null</code>.
     */
    protected BaseServiceSource(SourceType type, String uri, Project project) {
        super(type, uri, project);
        if (StringUtils.isBlank(uri)) {
            throw new IllegalArgumentException("uri");
        }
    }

    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------

    @Override
    public String getVersion() {
        return version;
    }

    @Override
    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getPublisher() {
        return publisher;
    }

    @Override
    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    @Override
    public void setServerTypeStrategy(String server)
    {
        this.serverTypeStrategy=server;
    }

    @Override
    public String getServerTypeStrategy()
    {
        return this.serverTypeStrategy;
    }
}

/*
 * Copyright / Copr. IGN
 * Contributor(s) : F. Hamdi
 *
 * Contact: hamdi.faycal@gmail.com
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
import java.io.InputStream;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.ShpSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.util.io.FileUtils;

import static org.datalift.fwk.util.StringUtils.isSet;


/**
 * Default implementation of the {@link ShpSource} interface.
 *
 * @author Fayçal Hamdi
 */
@Entity
@RdfsClass("datalift:shpSource")
public class ShpSourceImpl extends BaseFileSource
                           implements ShpSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:shapeIndex")
    private String shxFilePath;
    @RdfProperty("datalift:shapeAttr")
    private String dbfFilePath;
    @RdfProperty("datalift:shapeProj")
    private String prjFilePath;

    private transient File shxFile = null;
    private transient File dbfFile = null;
    private transient File prjFile = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new Shapefile source.
     */
    public ShpSourceImpl() {
        super(SourceType.ShpSource);
    }

    /**
     * Creates a new Shapefile source with the specified identifier and
     * owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    public ShpSourceImpl(String uri, Project project) {
        super(SourceType.ShpSource, uri, project);
    }

    //-------------------------------------------------------------------------
    // Source contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void delete() {
        super.delete();

        if (this.shxFile != null) {
            this.shxFile.delete();
        }
        if (this.dbfFile != null) {
            this.dbfFile.delete();
        }
        if (this.prjFile != null) {
            this.prjFile.delete();
        }
    }

    //-------------------------------------------------------------------------
    // ShpSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getShapeFilePath() {
        return this.getFilePath();
    }

    /** {@inheritDoc} */
    @Override
    public String getIndexFilePath() {
        return this.shxFilePath;
    }

    /** {@inheritDoc} */
    @Override
    public String getAttributeFilePath() {
        return this.dbfFilePath;
    }

    /** {@inheritDoc} */
    @Override
    public String getProjectionFilePath() {
        return this.prjFilePath;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getShapeFileInputStream() throws IOException {
        return this.getInputStream();
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getIndexFileInputStream() throws IOException {
        this.init();
        return (this.shxFile != null)?
                FileUtils.getInputStream(this.shxFile, this.getBufferSize()):
                null;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getAttributeFileInputStream() throws IOException {
        this.init();
        return (this.dbfFile != null)?
                FileUtils.getInputStream(this.dbfFile, this.getBufferSize()):
                null;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getProjectionFileInputStream() throws IOException {
        this.init();
        return (this.prjFile != null)?
                FileUtils.getInputStream(this.prjFile, this.getBufferSize()):
                null;
    }

    //-------------------------------------------------------------------------
    // BaseFileSource contract support
    //-------------------------------------------------------------------------

    @Override
    protected void init() {
        super.init();

        if (this.prjFile == null) {
            File docRoot = Configuration.getDefault().getPublicStorage();
            if ((docRoot == null) || (! docRoot.isDirectory())) {
                throw new TechnicalException("public.storage.not.directory",
                                             docRoot);
            }
            if (isSet(this.shxFilePath)) {
                this.shxFile = new File(docRoot, this.shxFilePath);
            }
            if (isSet(this.dbfFilePath)) {
                this.dbfFile = new File(docRoot, this.dbfFilePath);
            }
            if (isSet(this.prjFilePath)) {
                this.prjFile = new File(docRoot, this.prjFilePath);
            }
        }
        // Else: Already initialized.
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void setShapeFilePath(String path) {
        this.setFilePath(path);
    }

    public void setIndexFilePath(String path) {
        this.shxFilePath = path;
    }

    public void setAttributeFilePath(String path) {
        this.dbfFilePath = path;
    }

    public void setProjectionFilePath(String path) {
        this.prjFilePath = path;
    }
}

/*
 * Copyright / Copr. IGN 2013
 * Contributor(s) : Faycal Hamdi
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

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.Project;


/**
 * Default implementation of the {@link GmlSource} interface.
 *
 * @author fhamdi
 */
@Entity
@RdfsClass("datalift:gmlSource")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="http://www.datalift.org/core/projects")
public class GmlSourceImpl extends BaseFileSource
implements GmlSource
{
	//-------------------------------------------------------------------------
	// Instance members
	//-------------------------------------------------------------------------

	@RdfProperty("datalift:shapeXsd")
	private String xsdFilePath;

	private transient File xsdFile = null;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/**
	 * Creates a new GML source.
	 */
	public GmlSourceImpl() {
		super(SourceType.GmlSource);
	}

	/**
	 * Creates a new GML source with the specified identifier and
	 * owning project.
	 * @param  uri       the source unique identifier (URI) or
	 *                   <code>null</code> if not known at this stage.
	 * @param  project   the owning project or <code>null</code> if not
	 *                   known at this stage.
	 *
	 * @throws IllegalArgumentException if either <code>uri</code> or
	 *         <code>project</code> is <code>null</code>.
	 */
	public GmlSourceImpl(String uri, Project project) {
		super(SourceType.GmlSource, uri, project);
	}

	//-------------------------------------------------------------------------
	// Source contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public void delete() {
		super.delete();

		this.delete(this.xsdFilePath);
		this.xsdFile = null;
	}

	//-------------------------------------------------------------------------
	// ShpSource contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public String getGmlFilePath() {
		return this.getFilePath();
	}

	/** {@inheritDoc} */
	@Override
	public String getXsdFilePath() {
		return this.xsdFilePath;
	}

	/** {@inheritDoc} */
	@Override
	public InputStream getGmlFileInputStream() throws IOException {
		return this.getInputStream();
	}

	/** {@inheritDoc} */
	@Override
	public InputStream getXsdFileInputStream() throws IOException {
		this.init();
		return this.getInputStream(this.xsdFile);
	}

	//-------------------------------------------------------------------------
	// BaseFileSource contract support
	//-------------------------------------------------------------------------

	@Override
	protected void init() {
		super.init();

		if (this.xsdFile == null) {
			this.xsdFile = this.getFile(this.xsdFilePath);
		}
		// Else: Already initialized.
	}

	//-------------------------------------------------------------------------
	// Specific implementation
	//-------------------------------------------------------------------------

	public void setGmlFilePath(String path) {
		this.setFilePath(path);
	}

	public void setXsdFilePath(String path) {
		this.xsdFilePath = path;
	}

}

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

package org.datalift.core.project;

import java.net.URI;
import java.util.Date;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.ManyToOne;

import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.User;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;


/**
 * Default implementation of the {@link Ontology} interface.
 *
 * @author oventura
 */
@Entity
@RdfsClass("datalift:ontology")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="datalift:datalift")
public class OntologyImpl extends BaseRdfEntity implements Ontology
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfId
    private String uri;
    @RdfProperty("dcterms:title")
    private String title;
    @RdfProperty("dcterms:source")
    private URI source;
    @RdfProperty("datalift:project")
    @ManyToOne(fetch = FetchType.LAZY, cascade = {})
    private Project project;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new project.
     */
    public OntologyImpl() {
        // NOP
    }

    /**
     * Creates a new ontology with the specified URI as identifier.
     * @param  uri   the ontology identifier.
     */
    public OntologyImpl(String title) {
		this.uri = Ontology.BASE_USER_URI + title;
        this.title = title;
    }
    
    //-------------------------------------------------------------------------
    // Ontology contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void setTitle(String title) {
		this.uri = Ontology.BASE_USER_URI + title;
        this.title = title;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle() {
        return this.title;
    }

    /** {@inheritDoc} */
    @Override
    public URI getSource() {
        return this.source;
    }

    /** {@inheritDoc} */
    @Override
    public void setSource(URI source) {
        this.source = source;
    }
    
    /** {@inheritDoc} */
    @Override
    public Project getProject() {
		return project;
	}

    /** {@inheritDoc} */
    @Override
	public void setProject(Project project) {
		this.project = project;
	}

    /** {@inheritDoc} */
    /**
     * @deprecated date is now in Event 
     */
    @Override
    public Date getDateSubmitted() {
        return new Date(0);
    }

    /** {@inheritDoc} */
    /**
     * @deprecated publisher is not used anymore.
     */
    @Override
    public String getOperator() {
        return "Deprecated";
    }

    /** {@inheritDoc} */
    @Override
    public String getUri() {
        return this.getRdfId().toString();
    }

    //-------------------------------------------------------------------------
    // BaseRdfEntity contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected void setId(String id) {
        // NOP
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * @deprecated date is now in Event 
     */
    public void setDateSubmitted(Date dateSubmitted) {
    	// NOP
    }

    /**
     * @deprecated publisher is not used anymore.
     */
    public void setOperator(String operator) {
        // NOP
    }
}

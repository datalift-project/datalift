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

import javax.persistence.Entity;

import org.datalift.fwk.project.User;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

/**
 * A user class to identify someone.
 * 
 * @author avalensi
 *
 */
@Entity
@RdfsClass("datalift:User")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="datalift:datalift")
public class UserImpl extends BaseRdfEntity implements User {

	//-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfId
    private String uri;

    @RdfProperty("dcterms:identifier")
	private String identifier;

	@RdfProperty("prv:actedOnBehalfOf")
	private String actedOnBehalfOf;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

	public UserImpl(String identifier) {
		this.uri = User.BASE_USER_URI + identifier;
		this.identifier = identifier;
	}
	
	//-------------------------------------------------------------------------
    // User contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getUri() {
        return this.uri;
    }

    /** {@inheritDoc} */
    @Override
	public String getIdentifier() {
		return identifier;
	}

    /** {@inheritDoc} */
    @Override
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}

    /** {@inheritDoc} */
    @Override
	public String getActedOnBehalfOf() {
		return actedOnBehalfOf;
	}

    /** {@inheritDoc} */
    @Override
	public void setActedOnBehalfOf(String actedOnBehalfOf) {
		this.actedOnBehalfOf = actedOnBehalfOf;
	}

	//-------------------------------------------------------------------------
    // BaseRedfEntity contract support
    //-------------------------------------------------------------------------

    @Override
	protected void setId(String id) {
		// TODO Auto-generated method stub
		
	}
	
}

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
import java.net.URISyntaxException;
import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Event;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.SourceCreationEvent;
import org.datalift.fwk.project.User;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;


/**
 * SourceCreationEvent default implementation.
 * 
 * @author avalensi
 *
 */
@Entity
@MappedSuperclass
@RdfsClass("datalift:SourceCreationEvent")
//@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="datalift:datalift")
public class SourceCreationEventImpl extends EventImpl 
implements SourceCreationEvent {

	@RdfProperty("prov:generated")
	private URI generated;

	/** Instantiate SourceCreationEventImpl 
	 * @throws URISyntaxException */
	public SourceCreationEventImpl(
			URI sourceUri,
			String projectUri,
			String description,
			String parameters,
			Date startedAtTime,
			Date endedAtTime,
			User wasAssociatedWith,
			Source used,
			Event wasInformedBy
	) throws URISyntaxException {
		char lastChar = projectUri.charAt(projectUri.length() - 1);
		if (lastChar != '#' && lastChar != '/')
			projectUri += '/';
		
		this.setId(projectUri + "event/" + UUID.randomUUID());
		this.setDescription(description);
		this.setParameters(parameters);
		this.setStartedAtTime(startedAtTime);
		this.setEndedAtTime(endedAtTime);
		this.setWasAssociatedWith(wasAssociatedWith);
		this.setUsed(used);
		this.setWasInformedBy(wasInformedBy);
		this.setTarget(sourceUri);
	}

	public void setTarget(URI uri) {
		super.setTarget(uri);
		this.generated = uri;
	}
	
}

package org.datalift.core.project;

import java.util.Date;
import java.util.UUID;

import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;

import org.datalift.fwk.project.Event;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectCreationEvent;
import org.datalift.fwk.project.User;

import com.clarkparsia.empire.annotation.NamedGraph;
import com.clarkparsia.empire.annotation.RdfsClass;


/**
 * ProjectModificationEvent default implementation.
 * 
 * @author avalensi
 *
 */
@Entity
@MappedSuperclass
@RdfsClass("datalift:ProjectModificationEvent")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="datalift:datalift")
public class ProjectModificationEventImpl extends EventImpl 
implements ProjectCreationEvent {

	/** Instantiate ProjectModificationEventImpl */
	public ProjectModificationEventImpl(
			String uri,
			String description,
			String parameters,
			Date startedAtTime,
			Date endedAtTime,
			User wasAssociatedWith,
			Project used,
			Event wasInformedBy
	) {
		char lastChar = uri.charAt(uri.length() - 1);
		if (lastChar != '#' && lastChar != '/')
			uri += '/';

		this.setId(uri + UUID.randomUUID());
		this.setDescription(description);
		this.setParameters(parameters);
		this.setStartedAtTime(startedAtTime);
		this.setEndedAtTime(endedAtTime);
		this.setWasAssociatedWith(wasAssociatedWith);
		this.setUsed(used);
		this.setWasInformedBy(wasInformedBy);
	}

}

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
 * ProjectSuppressionEvent default implementation.
 * 
 * @author avalensi
 *
 */
@Entity
@MappedSuperclass
@RdfsClass("datalift:ProjectSuppressionEvent")
@NamedGraph(type = NamedGraph.NamedGraphType.Static, value="datalift:datalift")
public class ProjectSuppressionEventImpl extends EventImpl 
implements ProjectCreationEvent {

	/** Instantiate ProjectSuppressionEventImpl */
	public ProjectSuppressionEventImpl(
			String projectUri,
			String description,
			String parameters,
			Date startedAtTime,
			Date endedAtTime,
			User wasAssociatedWith,
			Project used,
			Event wasInformedBy
	) {
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
	}

}

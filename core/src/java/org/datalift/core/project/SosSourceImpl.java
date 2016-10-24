package org.datalift.core.project;


import javax.persistence.Entity;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.SosSource;

import com.clarkparsia.empire.annotation.RdfsClass;


@Entity
@RdfsClass("datalift:sosSource")
public class SosSourceImpl extends BaseServiceSource implements SosSource
{
    public SosSourceImpl() {
        super(SourceType.SosSource);
    }

    /**
     * Creates a new SOS source with the specified identifier and
     * owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    public SosSourceImpl(String uri, Project project) {
        super(SourceType.SosSource, uri, project);
    }
}

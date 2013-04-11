/*
 * Copyright / INRIA 2011-2012
 * Contributor(s) : Z. Fan, J. Euzenat, F. Scharffe
 *
 * Contact: zhengjie.fan@inria.fr
 */

package org.datalift.samples.project;

import java.io.ObjectStreamException;
import java.net.URI;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;


/**
 * A common superclass for InterconnectionModule, providing some utility methods.
 * 
 * @author Zhengjie Fan
 */
public abstract class BaseInterconnectionModule
                                extends BaseModule implements ProjectModule
                                {
	
	protected BaseInterconnectionModule(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	protected final Project getProject(URI projectId)
                                      throws ObjectStreamException {
		
		ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
		Project p = pm.findProject(projectId);
                
                return p;
                }

    protected final TemplateModel newViewable(String templateName, Object it) {
        return ViewFactory.newView("/" + this.getName() + templateName, it);
    }
}

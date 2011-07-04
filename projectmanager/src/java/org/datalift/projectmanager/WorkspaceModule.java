package org.datalift.projectmanager;


import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;

import com.sun.jersey.api.view.Viewable;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.ProjectManager;


@Path("/workspace")
public class WorkspaceModule extends BaseModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String MODULE_NAME = "workspace";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The DataLift configuration. */
    private Configuration configuration = null;
    /** Project Manager bean. */
    private ProjectManager projectManager = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public WorkspaceModule() {
        super(MODULE_NAME);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        this.configuration  = configuration;
        this.projectManager = configuration.getBean(ProjectManager.class);
    }

    /** {@inheritDoc} */
    @Override
    public Map<String, Class<?>> getResources() {
        Map<String, Class<?>> rsc = new HashMap<String, Class<?>>();
        rsc.put("project", ProjectResource.class);
        return rsc;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public ProjectManager getProjectManager() {
        return this.projectManager;
    }

    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Return a viewable for the specified template, populated with the
     * specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated viewable.
     */
    protected final Viewable newViewable(String templateName, Object it) {
        return new Viewable("/" + this.getName() + templateName, it);
    }
}

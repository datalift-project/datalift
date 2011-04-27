package org.datalift.samples.project;


import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;


public class HandleProjectModule extends BaseModule implements ProjectModule
{
    private final static Logger log = Logger.getLogger();

    public HandleProjectModule() {
        super("sample-project", true);
    }

    @Override
    public URI canHandle(Project p) {
        try {
            return new URI(this.getName() + "/java-guy.jpg");
        }
        catch (Exception e) {
            log.fatal("Uh?", e);
            throw new RuntimeException(e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String doGet() {
        return "Test HandleProjectModule index";
    }
}

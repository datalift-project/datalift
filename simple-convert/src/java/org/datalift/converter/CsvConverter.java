package org.datalift.converter;

import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;

public class CsvConverter extends BaseModule implements ProjectModule
{
    public CsvConverter() {
        super("csvconverter", true);
    }

    @Override
    public URI canHandle(Project p) {
        boolean hasCsvSource = false;
        for (Source s : p.getSources()) {
            if (s instanceof CsvSource) {
                hasCsvSource = true;
                break;
            }
        }
        URI projectPage = null;
        if (hasCsvSource) {
            try {
                return new URI(this.getName() + "?project=" + p.getUri());
            }
            catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return projectPage;
    }

    @GET
    public Object getIndexPage(@PathParam("projectId") String projectId){
    	if (projectId != null) {
        	return "Converter project " + projectId;
    	}
    	return "Converter index page";
    }
    
}

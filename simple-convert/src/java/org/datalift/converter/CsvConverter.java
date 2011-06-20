package org.datalift.converter;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.datalift.fwk.Module;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;

public class CsvConverter implements ProjectModule
{	
	private final static String MODULE_NAME = "csvconverter";
	
	private ProjectManager	projectManager = null;
	
	private File	storage = null;
	
	private Logger log = Logger.getLogger();
	
    public CsvConverter() {
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
    public Object getIndexPage(@QueryParam("project") String projectId){
    	if (projectId != null) {
    		Project p;
    		try {
    			p = this.projectManager.findProject(new URL(projectId).toURI());
    		} catch (Exception e) {
    			// should never append
    			return new RuntimeException(e);
    		}
    		for (Source s : p.getSources()) {
    	            if (s instanceof CsvSource) {
    	            	this.convert((CsvSource)s);
    	            }
    		  }
        	return "Converter project " + projectId;
    	}
    	return "Converter index page";
    }
    
    public void convert(CsvSource s) {
    	
    	s.init(docRoot, baseUri)
    	for (String[] line : s) {
    		log.debug("[0]" + line[0] + ", [1]" + line[1] + ", [2]" + line[2]);
    	}
    }

	@Override
	public String getName() {
		return MODULE_NAME;
	}

	@Override
	public Map<String, Class<?>> getResources() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isResource() {
		// TODO Auto-generated method stub
		return true;
	}

	@Override
	public void init(Configuration configuration) {
		ProjectManager mgr = configuration.getBean(ProjectManager.class);
		if (mgr != null) {
			this.projectManager = mgr;
			this.storage = configuration.getPublicStorage();
		}
		else
			throw new RuntimeException("Could not retrieve Project Manager");
	}

	@Override
	public void postInit(Configuration configuration) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void shutdown(Configuration configuration) {
		// TODO Auto-generated method stub
		
	}
}

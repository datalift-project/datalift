package org.datalift.converter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

public class CsvConverter extends BaseModule implements ProjectModule 
{	
	private final static String MODULE_NAME = "csvconverter";
	
	private ProjectManager	projectManager = null;
	
	private File	storage = null;
	
	private Repository internal = null;
	
	private Logger log = Logger.getLogger();
	
    public CsvConverter() {
    	super(MODULE_NAME, true);
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
    public Object getIndexPage(@QueryParam("project") String projectId,
    		                   @Context UriInfo uriInfo){
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
    	            	try {
							((CsvSource)s).init(storage, uriInfo.getBaseUri());
						} catch (IOException e) {
							throw new RuntimeException("Could not initialize Source");
						}
    	            	try {
    	            		URI	transformedUri = new URL(s.getUri() + "/rdf" + p.getSources().size()).toURI();
							this.convert((CsvSource)s, this.internal, transformedUri);
							TransformedRdfSource src = this.projectManager.newTransformedRdfSource(
									transformedUri, s.getTitle() + "-rdf" + p.getSources().size(), 
									transformedUri);
							p.addSource(src);
							this.projectManager.saveProject(p);
						} catch (Exception e) {
							log.debug(e);
						} 
    	            }
    		  }
        	return "CSV Conversion done for project " + projectId;
    	}
    	return "Converter index page";
    }
    
    public void convert(CsvSource src, Repository target, URI namedGraph) {
    	int	i = 0;
        final RepositoryConnection cnx = target.newConnection();
        try {
            final ValueFactory valueFactory = cnx.getValueFactory();
            // Clear target named graph, if any.
            org.openrdf.model.URI u = null;
           if (namedGraph != null) {
        	   u = valueFactory.createURI(namedGraph.toString());
        	   cnx.clear(u);
           }
           // Prevent transaction commit for each triple inserted.
           cnx.setAutoCommit(false);
           // Load triples
           for (String[] line : src) {
        	   String subject = namedGraph + "/row" + i + "#_";    		
        	   for (int j = 0; j < line.length && j < src.getColumnsHeader().size(); j++) {
        		   String predicate = namedGraph + "/column" + src.getColumnsHeader().get(j);
        		   log.debug("S[{}]P[{}]O[{}]", subject, predicate, line[j]);
        		   Statement stmt = valueFactory.createStatement(
        				   valueFactory.createURI(subject), 
        				   valueFactory.createURI(predicate), 
        				   valueFactory.createLiteral(line[j]));        		  
        		   cnx.add(stmt, valueFactory.createURI(namedGraph.toString()));
        	   }
    		   i++;
           }
           cnx.commit();
        }
        catch (Exception e) {
    		throw new RuntimeException("Could not convert CsvSource into RDF triples");
    	}
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore */ }
        }
    }
    
	@Override
	public void init(Configuration configuration) {
		ProjectManager mgr = configuration.getBean(ProjectManager.class);
		if (mgr != null) {
			this.projectManager = mgr;
			this.storage = configuration.getPublicStorage();
			this.internal = configuration.getInternalRepository();
		}
		else
			throw new RuntimeException("Could not retrieve Project Manager");
	}
}

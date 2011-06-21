package org.datalift.converter;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.rdf.Repository;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.repository.RepositoryConnection;

public class CsvConverter implements ProjectModule
{	
	private final static String MODULE_NAME = "csvconverter";
	
	private ProjectManager	projectManager = null;
	
	private File	storage = null;
	
	private Repository internal = null;
	
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
							TransformedRdfSource src = new TransformedRdfSource(transformedUri.toString());
							src.setTitle(s.getTitle() + "/rdf" + p.getSources().size());
							src.setTargetGraph(transformedUri);
							src.setTargetGraph(new URL(s.getUri()).toURI());
							p.addSource(src);
							this.projectManager.saveProject(p);
						} catch (Exception e) {
							// Should never occur
						} 
    	            }
    		  }
        	return "Converter project " + projectId;
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
           final org.openrdf.model.URI ctx = u;
           // Prevent transaction commit for each triple inserted.
           cnx.setAutoCommit(false);
           // Load triples
           for (String[] line : src) {
        	   String subject = namedGraph + "/row" + i + "#_";    		
        	   for (int j = 0; j < line.length; j++) {
        		   String predicate = namedGraph + "/column" + getColumnName(j);
        		   
        		   Statement stmt = valueFactory.createStatement(
        				   new URIImpl(subject), new URIImpl(predicate), new LiteralImpl(line[j]));        		  
        		   cnx.add(stmt);
        	   }
        	   i++;
           }
           cnx.commit();
           
        }
        catch (Exception e) {
    		throw new RuntimeException("Could not convert CsvSource into RDF triples");
    	}
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
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
	        Collection<Class<?>> classes = new LinkedList<Class<?>>();
	        classes.add(TransformedRdfSource.class);
			this.projectManager.addPersistentClasses(classes);
			this.storage = configuration.getPublicStorage();
			this.internal = configuration.getInternalRepository();
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
	
	public static String getColumnName(int n) {
        StringBuilder s = new StringBuilder();
        for (; n >= 0; n = n / 26 - 1) {
            s.insert(0, (char)(n % 26 + 65));
        }
        return s.toString();
    }
	
	
}

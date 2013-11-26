package org.datalift.interlinker;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.SparqlSource;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;

import org.datalift.interlinker.sparql.DataSource;
import org.datalift.interlinker.sparql.SparqlCursor;
import org.datalift.interlinker.sparql.SparqlDataSource;
import static org.datalift.interlinker.sparql.DataSource.*;

public abstract class Model {
	
    /** Datalift's internal Sesame {@link Repository repository}. **/
    protected static final Repository INTERNAL_REPO = Configuration.getDefault().getInternalRepository();
    /** Datalift's internal Sesame {@link Repository repository} URL. */
    public static final String INTERNAL_URL = INTERNAL_REPO.getEndpointUrl();
    /** Datalift's logging system. */
    protected static final Logger LOG = Logger.getLogger();
    
    protected static final  String GUI_RESOURCES_BUNDLE = "resources";

    /**
     * Checks if a given {@link Source} is valid for our uses.
     * @param src The source to check.
     * @return True if src is {@link TransformedRdfSource} or {@link SparqlSource}.
     */
    protected abstract boolean isValidSource(Source src);
    
    /**
     * Checks if a {@link Project proj} contains at least one valid RDF sources.
     * @param proj The project to check.
     * @return True if there is a valid sources.
     */
    protected final boolean hasRDFSource(Project proj) {
    	Iterator<Source> sources = proj.getSources().iterator();
    	
    	while (sources.hasNext()) {
    		if (isValidSource(sources.next())) {
    			return true;
    		}
    	}
    	return false;
    }
    
    /**
     * Resource getter.
     * @param key The key to retrieve.
     * @return The value of key.
     */
    protected String getTranslatedResource(String key) {
    	return PreferredLocales.get().getBundle(GUI_RESOURCES_BUNDLE, Model.class).getString(key);
    }
    
    protected final HashMap<String, String> getSources(Project proj){
    	HashMap<String, String> sourceMap = new HashMap<String, String>();
        for (Source src : proj.getSources()) {
        	if (isValidSource(src)) {
        		sourceMap.put(src.getUri(),src.getTitle());
        	}
       	}
       	return sourceMap;
    }
    
    /**
     * Creates a new transformed RDF source and attaches it to a project.
     * @param  p        the owning project.
     * @param  parent   the parent source object.
     * @param  name     the new source name.
     * @param  uri      the new source URI.
     *
     * @return the newly created transformed RDF source.
     * @throws IOException if any error occurred creating the source.
     */
    protected void addResultSource(Project p, Source parent, String name, URI uri) throws IOException {
    	ProjectManager pm = Configuration.getDefault().getBean(ProjectManager.class);
        pm.newTransformedRdfSource(p, uri, name, null, uri, parent);
        pm.saveProject(p);
    }
    
	/**
     * Retrieves the predicates of a source.
     * @param uriContext The uri of the source.
     * @return A List of all of the predicates used inside the contexts.
     */
	protected final List<String> getPredicates(String uriContext) {
		DataSource datasource = new SparqlDataSource();
		SparqlCursor cursor = datasource.query(uriContext, new String[]{PREDICATE_BINDER},
				new String[]{"?" + SUBJECT_BINDER + " ?" + PREDICATE_BINDER + " ?" + OBJECT_BINDER});
		cursor.moveToFirstPosition();
		List<String> predicates = new ArrayList<String>();
		for(int i = 0;i<cursor.getCount();i++){
			predicates.add(cursor.getValue(0));
			cursor.moveToNextPosition();
		}
		return predicates;
	}
    
   
}

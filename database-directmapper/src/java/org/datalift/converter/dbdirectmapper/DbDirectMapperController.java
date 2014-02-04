package org.datalift.converter.dbdirectmapper;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.net.URISyntaxException;
import java.sql.Connection;
import java.io.IOException;

import net.antidot.semantic.rdf.rdb2rdf.dm.core.DirectMapper;
import net.antidot.sql.model.core.DriverType;
import net.antidot.sql.model.core.SQLConnector;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.SqlDatabaseSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.rdf.Repository;

import static org.datalift.fwk.MediaTypes.*;

/**
 * A {@link ProjectModule project module} that performs SQL to RDF
 * conversion using
 * <a href="http://www.w3.org/TR/2011/WD-rdb-direct-mapping-20110324/">RDF
 * Direct Mapping</a> principles on an entire database, basing on an edited version of the
 * library <a hfref="http://www.w3.org/2001/sw/wiki/Db2triples">Db2Triples</a>
 *
 * @author csuglia
 */
@Path(DbDirectMapperController.MODULE_NAME)
public class DbDirectMapperController extends ModuleController {
	//-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration.  */
    public static final String MODULE_NAME = "database-directmapper";
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
	public DbDirectMapperController() {
		super(MODULE_NAME, 400);
	}
	
	//-------------------------------------------------------------------------
    // Specific Implementation
    //-------------------------------------------------------------------------

	/**
     * Tell the project manager to add a new the module button to project if there is at least
     * one database source
     */
    @Override
	public boolean accept(Source s){
        if (s == null) {
            throw new IllegalArgumentException("s");
        }
        return(s.getType() == SourceType.SqlDatabaseSource);
	}
	
    /**
     * Convert the Database Source to rdf and put the new triples into the internal repository
     * @param source the Database source to convert
     * @param targetUri the Uri where the converted db will be saved to, and the fragment to create new uris 
     * @param repository the DataLift repository where the converted triples will be saved
     */
    private void convert(SqlDatabaseSource source, String targetUri, Repository repository) {
    	try{
	    	Connection sqlConnection=SQLConnector.connect(source.getUser(), source.getPassword(), source.getDatabasePath(), 
	    			DriverType.MysqlDriver,source.getDatabaseName());
	    	DirectMapper.generateDirectMapping(sqlConnection, DriverType.MysqlDriver, targetUri, repository);
    	}catch (Exception e) {
    		log.fatal(e);
            throw new TechnicalException("sql.conversion.failed", e);
    	}
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        return this.newProjectView("/databaseDirectMapper.vm", projectId);
    }
    
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response mapDBSource(@FormParam("project") URI projectId,
            	@FormParam("source") URI sourceId, 
            	@FormParam("dest_title") String destTitle,
            	@FormParam("dest_graph_uri") String targetGraph) throws WebApplicationException {
        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            SqlDatabaseSource in = (SqlDatabaseSource)p.getSource(sourceId);
            // Get the rdf store
            Repository internal = Configuration.getDefault().getInternalRepository();
            log.trace("Mapping database {} to RDF", in.getDatabaseName());
            // Get a proper base uri:
            URI baseUri = targetGraph.endsWith("/") ? new URI(targetGraph) : new URI(targetGraph + "/"); 
            // Convert the database and put the new triples into the repository
            this.convert(in, baseUri.toString(), internal);
            // Register new transformed RDF source.
            TransformedRdfSource newSrc = this.projectManager.newTransformedRdfSource(p, baseUri,
            		destTitle, null, baseUri, in);
            // Save the project
            this.projectManager.saveProject(p);
            // Redirect to source page
            response = this.getSourceListPage(newSrc).build();
        }catch(URISyntaxException e){
        	throw new TechnicalException("Wrong.uri.syntax", e);
        }catch(IOException e){
        	throw new TechnicalException("I/O.error",e);
        }
        return response;
    }
}

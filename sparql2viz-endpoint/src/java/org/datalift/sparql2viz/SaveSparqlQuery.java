/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.sparql2viz;


import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ResourceBundle;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.FileStore;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.fwk.view.ViewFactory;
import org.datalift.sparql.AbstractSparqlEndpoint;
import org.datalift.sparql.TechnicalException;

import static javax.ws.rs.core.MediaType.TEXT_PLAIN;

/**
 * Collect the sending request and save it in the local
 * repistory of the user
 *
 * @author zkhattabi
 */
@Path(AbstractSparqlEndpoint.MODULE_NAME + "/save-request")
public class SaveSparqlQuery extends BaseModule
{
    private final static Logger log = Logger.getLogger();

    /** The default configuration file for visualization */
    private final static String DEFAULT_JSON_CONF = "sparql-visualizations.json";
	
    public SaveSparqlQuery() {
        super("save-request");
    }
	
    //-------------------------------------------------------------------------
    // Web service utility methods
    //-------------------------------------------------------------------------

    /**
     * Return a model for the specified template view, populated with
     * the specified model object.
     * <p>
     * The template name shall be relative to the module, the module
     * name is automatically prepended.</p>
     * @param  templateName   the relative template name.
     * @param  it             the model object to pass on to the view.
     *
     * @return a populated template model.
     */
    protected final TemplateModel newView(String template, Object it) {
        return ViewFactory.newView("/" + this.getName() + '/' + template, it);
    }
	
	/**
     * Loads the available predefined SPARQL queries from the RDF
     * file {@link #QUERIES_FILE_PROPERTY} specified in the Datalift
     * configuration or from the default query definition file present
     * in the module JAR.
     * @param  cfg   the Datalift configuration.
     *
     * @return the loaded queries, as a list.
     * @throws TechnicalException if any error occurred while loading
     *         the query definitions.
     */
	@Path("graphs")
	@GET
	@Consumes(MediaType.APPLICATION_JSON)
    public String loadJsonConf() {
		String response = null;
		String path = DEFAULT_JSON_CONF;
        InputStream in = null;
        try {
        	File f = new File(Configuration.getDefault().getProperty("datalift.home"),
        					  "/conf/" + path);
            if (f.canRead() && f.isFile()) {
                // Graph definition file specified. => Check presence.
            	path = f.getPath();
                log.info("Loading visualization conf from {}", path);
                in = new FileInputStream(f);
            }
            else {
                // No query definition file specified. => Use default.
                log.info("No visualization configuration file specified, using default");
                in = this.getClass().getClassLoader().getResourceAsStream(path);

                FileOutputStream fos = new FileOutputStream(f);
                try {
                	byte[] buffer = new byte[4096];
                	int bytesRead;
                	while ((bytesRead = in.read(buffer)) != -1) {
                		fos.write(buffer, 0, bytesRead);
                	}
                } finally {
                	if (fos != null) {
                		fos.close();
                	}
                }
            }

            Reader r = new InputStreamReader(in, "UTF-8");
            StringBuilder buf = new StringBuilder(4096);
            char[] c = new char[1024];
            int l;
            while ((l = r.read(c)) != -1)
            {
            	buf.append(c, 0, l);
            }
            response = buf.toString();
        }
        catch (Exception e) {
            TechnicalException error =
                        new TechnicalException("visu.load.failed", e, path);
            log.error(error.getLocalizedMessage(), e);
        }
        finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
        return response;
    }
	
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public void saveRequest(@FormParam("requestName") String requestName,
			@FormParam("requestData") String requestData,
			@FormParam("version") String version) throws IOException {
		FileStore fs = Configuration.getDefault().getPrivateStorage();
		File dir = new File("sparql/requests");
		File f = fs.getFile(new File(dir, requestName).getPath());
		log.info("Saving user request {} to {}: {}", requestName, f, requestData);
		if(!version.equals("")){
			version = "\n"+version;
			requestData = requestData+version;
		}
		InputStream in = new ByteArrayInputStream(requestData.getBytes("UTF-8"));

		fs.save(in, f);
	}
	
	/**
     * Save a new request
     */
	@Path("{requestName}")
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	public String saveRequest(@PathParam("requestName") String requestName) throws IOException {
		FileStore fs = Configuration.getDefault().getPrivateStorage();
		File dir = new File("sparql/requests");
		File f = fs.getFile(new File(dir, requestName).getPath());
		
		InputStream in = fs.getInputStream(f);
		Reader r = new InputStreamReader(in, "UTF-8");
		StringBuilder buf = new StringBuilder(4096);
		char[] c = new char[1024];
		int l;
		while ((l = r.read(c)) != -1)
		{
			buf.append(c, 0, l);
		}
		String requestData = buf.toString();

		return requestData;
	}
	
	/**
     * get the list of requests
     */
	@GET
	@Consumes(MediaType.TEXT_PLAIN)
	public String getListRequests() throws IOException {
		ResourceBundle b = PreferredLocales.get().getBundle("resources", this);
		FileStore fs = Configuration.getDefault().getPrivateStorage();
		File f = fs.getFile("sparql/requests");
		String[] lists = f.list();
		StringBuilder files = new StringBuilder(256);
		files.append("<select id=\"sub_diag\" class=\"form-select\">")
		     .append("<option value=\"--\" selected>")
		     .append(b.getString("sparql2viz.select.query"))
		     .append("</option>");		
		for(String s : lists) {
			files.append("<option value=\"").append(s).append("\">")
			                                .append(s).append("</option>");
		}
		return files.append("</select>").toString();
	}
	
	/**
     * Delete request file
     */
	@Path("{requestName}")
	@DELETE
	@Consumes(MediaType.TEXT_PLAIN)
	public void deleteRequest(@PathParam("requestName") String requestName) throws IOException {
		FileStore fs = Configuration.getDefault().getPrivateStorage();
		File dir = new File("sparql/requests");
		//File f = fs.getFile(new File(dir, requestName + ".txt").getPath());
		File f = fs.getFile(new File(dir, requestName).getPath());
		f.delete();
	}
	
	/**
     * Load mapline script
     */
	@Path("mapline.js")
	@GET
    @Produces(TEXT_PLAIN)
    public Response loadMapLineScript() {
    	TemplateModel view = this.newView("js/mapline.js.vm", null);
    	return Response.ok(view, TEXT_PLAIN).build();
    }
}
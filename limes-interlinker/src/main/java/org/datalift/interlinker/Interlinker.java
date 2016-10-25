package org.datalift.interlinker;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import static org.datalift.fwk.MediaTypes.TEXT_HTML_UTF8;

import java.io.File;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.view.TemplateModel;

import com.google.gson.Gson;

/**
 * @author Bouca Nova Dany
 *
 */
@Path(Interlinker.MODULE_PATH)
public class Interlinker extends BaseModule implements ProjectModule
{
	private final static Logger log = Logger.getLogger();

	private LimesXmlFile file;
	private Form form;

	public final static String MODULE_PATH = "interlinker";

	public Interlinker() {
		super(MODULE_PATH);
		this.file = null;
	}

	@GET
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getIndexPage() {
		TemplateModel view = this.newView("./interlinker.vm", null);
		return Response.ok(view, TEXT_HTML_UTF8).build();
	}

	@POST
	@Path("/link")
	@Consumes({ MediaType.APPLICATION_JSON })
	public void Form(String json) {
		form = new Gson().fromJson(json, Form.class);
		file = new LimesXmlFile(form);

		file.createDocument();
		file.createRoot("LIMES");
		file.formatLimesFile();
		file.SaveDocument(LimesXmlFile.FILENAME);

		file.print();
		String[] arg = new String[] { LimesXmlFile.FILENAME };

		try {
			de.uni_leipzig.simba.controller.PPJoinController.main(arg);
		} catch (Exception e) {
			log.error("Limes execution failed", e);
			throw new WebApplicationException(Response.Status.NOT_ACCEPTABLE);
		}
		file.deleteDocument(LimesXmlFile.FILENAME);
	}

	@GET
	@Path("/download/acceptance")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getAcceptanceFile() {
		return downloadFile(form.getAcceptanceFile());
	}

	@GET
	@Path("/download/review")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response getReviewFile() {
		return downloadFile(form.getReviewFile());
	}

	private Response downloadFile(String fileName) {
		File file = new File(fileName);
		ResponseBuilder response = Response.ok((Object) file);
		response.header("Content-Disposition", "attachment; filename=" + fileName);
		return response.build();
	}

	@POST
	@Path("/visualize")
	@Produces("text/html")
	public String Visualize() {
		if (file != null)
			return file.toString();
		else
			return null;
	}

	@Override
	public UriDesc canHandle(Project p) {
		return null;
	}
}

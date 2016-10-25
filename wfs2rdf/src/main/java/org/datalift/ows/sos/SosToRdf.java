package org.datalift.ows.sos;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.net.URI;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriInfo;

import org.datalift.ows.exceptions.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.SosSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.ows.model.BaseConverterModule;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.model.ObservationMetaData;
import org.datalift.ows.webServiceConverter2.WfsSos2Converter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;


/**
 * The main class of the module sos2rdf.
 * the module enables the transformation of a SOS response to RDF.
 * The SOS version supported is : 2.0.0.
 * The RDF created could be compliant to either a generic ontology or a specific one.
 * Besides the generic ontology, this module also implements a customized version of om-lite ontology.
 *
 * @author Hanane Eljabiri
 *
 */
@Path(SosToRdf.MODULE_NAME)
public class SosToRdf extends BaseConverterModule {

	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger();
	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "sos2rdf";		
	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------
	public SosToRdf() {
		super(MODULE_NAME,1400, SourceType.SosSource);
	}
	public SosToRdf(String name, int position, SourceType[] inputSources) {
		super(name, position, inputSources);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------
	@GET
	@Path("{path: .*$}")
	public Response getStaticResource(@PathParam("path") String path,
			@Context UriInfo uriInfo,
			@Context Request request,
			@HeaderParam(ACCEPT) String acceptHdr)
					throws WebApplicationException {
		log.trace("Reading static resource: {}", path);
		return Configuration.getDefault()
				.getBean(ResourceResolver.class)
				.resolveModuleResource(this.getName(),
						uriInfo, request, acceptHdr);
	}

	/**
	 * returns the index module page which shows the list of available sos source registered in the current project
	 * @param projectId : the current project id
	 * @return the index page
	 */
	@GET
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getIndexPage(@QueryParam("project") URI projectId) {
		return this.newProjectView("availableSosSources.vm", projectId);
	}
	/**
	 * 
	 * @param projectId
	 * @param sourceId
	 * @return the description of available feature types
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response getAvailableObservations(
			@FormParam("project") URI projectId,
			@FormParam("source") URI sourceId)
	{	
		//get the list of featuretypedescription using the source id
		//put the list into the web page availablewfsSources
		//lists of : FeatureType names, titles, count, summury (list for each information type)
		ResponseBuilder response = null;
		// Retrieve project.
		Project p = this.getProject(projectId);
		// Retrieve source.
		SosSource src = (SosSource)(p.getSource(sourceId));
		if (src == null) {
			this.throwInvalidParamError("source", sourceId);
		}
		TemplateModel view = this.newView("availableObservations.vm", p);
		view.put("source", sourceId);
		List<ObservationMetaData> observationOffering=new ArrayList<>();
		try {
			observationOffering=this.getObservationOffering(src.getSourceUrl(),src.getVersion());
			if(observationOffering!=null)
			{
				view.put("observationsOffering",observationOffering);
			}
			response = Response.ok(view);
		} catch (Exception e) {
			TechnicalException error = new TechnicalException("gettingAvailableObservationsFailed", e, sourceId);
			log.error(error.getMessage(), e);
			response = Response.serverError().entity(error.getLocalizedMessage())
					.type(MediaTypes.TEXT_PLAIN);
		}
		return response.build();
	}
	private List<ObservationMetaData> getObservationOffering(String sourceUrl, String version) throws Exception {

		SOS2Client mp=new SOS2Client(sourceUrl);
		mp.getCapabilities();
		return mp.getObservationOffering();
	}
	/**
	 * get the list of selected feature types selected by the user to be converted
	 * @param json the json representation of the array containing the feeatures to be converted
	 * @return the URL of the source project's page to be used by ajax to redirect the user
	 */
	@POST
	@Path("TransformSelectedObservations")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public String TransformSelectedObservations(String json)
	{
		String response = null;
		JsonParser parser = new JsonParser();
		JsonElement elements = parser.parse(json);
		JsonObject o = elements.getAsJsonObject();
		String project=o.get("project").getAsString();
		String source=o.get("source").getAsString();
		int optionOntology= Integer.parseInt(o.get("ontologyOption").getAsString());
		if(isSet(project) && isSet(source))
		{	Project p=null;
		// Retrieve project
		URI projectUri;
		try {
			projectUri = new URI(project);
			p = this.getProject(projectUri);
			// Retrieve source.
			SosSource s = (SosSource)(p.getSource(source));

			JsonArray j = o.get("values").getAsJsonArray();

			Iterator<JsonElement> i = j.iterator();

			while ( i.hasNext() ){
				JsonObject obj= i.next().getAsJsonObject();
				String id = obj.get("id").getAsString();
				String begin = obj.get("begin").getAsString();
				String end = obj.get("end").getAsString();
				//String format = obj.get("format").getAsString();

				String potentialtargetGraph=s.getUri()+"/"+id;
				int countGraph=getOccurenceGraph(p, potentialtargetGraph);
				URI targetGraph = constructTargetGraphURI(p,potentialtargetGraph);	
				countGraph++;
				URI baseUri=createBaseUri(targetGraph);
				String targetType=id+"-sos";
				String destination_title=id+"(RDF# )"+countGraph; //count to be added later
				boolean done=convertObservations2Rdf(projectUri,s, destination_title, targetGraph, baseUri, targetType,id,begin, end, null, optionOntology);
				if (done) {
					// Register new transformed RDF source.
					Source out = this.addResultSource(p, s,
							"RDF mapping of " + s.getTitle()+"("+id+")", targetGraph);
					// Display project source tab, including the newly created source.
					response = this.created(out);
				}
				else {
					// No data retrieved of service error.
					throw new TechnicalException("gettingAvailableObservationsFailed", id);
				}
			}
		} catch (Exception e) {
			this.handleInternalError(e);
		}
		}
		return response;
	}	
	private boolean convertObservations2Rdf(URI projectUri, SosSource s, String destination_title, URI targetGraph,
			URI baseUri, String targetType, String id,  String begin, String end, String format, int optionOntology)  {
		try {			
			SOS2Client client=new SOS2Client(s.getSourceUrl());
			client.getObservation(id,begin,end,format);
			//return a list of parsed features contained in typeName
			ComplexFeature observationDataToConvert=client.getUtilData(id);
			if (observationDataToConvert == null) {
				return false;
			}
			//0: default converter
			//1: EMF group Converter
			org.datalift.fwk.rdf.Repository target = Configuration.getDefault().getInternalRepository();
			WfsSos2Converter converter=new WfsSos2Converter(optionOntology, target , targetGraph);
			//handle exception
			if(!converter.ConvertFeaturesToRDF(observationDataToConvert))
			{
				return false;
			}
		} catch (Exception e) {
			throw new TechnicalException("convertFeatureTypeFailed", e, id);
		}
		return true;
	}
	/****the end of web services***/

	private URI createBaseUri(URI targetGraph) {

		String graph=targetGraph.toString();
		//String graphuri="http://localhost:9091/project/demo/source/geoservice-brgm/availableFT-2";
		int startproj,startsource;
		startproj=graph.indexOf("/project");
		String part1 = graph.substring(0, startproj);
		startsource=graph.indexOf("/source");
		String part2= graph.substring(startproj+8,startsource);
		String part3= graph.substring(startsource+7);
		return URI.create(part1+part2+part3);
	}

	private URI constructTargetGraphURI(Project p,String candidate) {
		int countExistingGraph=getOccurenceGraph(p, candidate);
		countExistingGraph++;
		return URI.create(candidate+"-"+countExistingGraph);
	}

	private int getOccurenceGraph(Project p,String candidate) {
		List<Integer> numberValues=new ArrayList<Integer>();
		List<String> existingGraph = new ArrayList<String>();
		for (Source ss : p.getSources()) {
			if(ss.getUri().startsWith(candidate))
				existingGraph.add(ss.getUri());
		}
		for (String s : existingGraph) {
			Pattern pp = Pattern.compile("[0-9]+$");
			Matcher m = pp.matcher(s);
			if(m.find()) {
				numberValues.add(Integer.valueOf(m.group()));
			}
		}
		if(numberValues.size()!=0)
		{
			return numberValues.get(numberValues.size()-1).intValue();
		}
		else
		{
			return 0;
		}
	}

	private boolean isSet(String s) {
		if (s==null || s.equals("")) return false;
		return true;
	}
}

package org.datalift.wfs;

import static javax.ws.rs.core.HttpHeaders.ACCEPT;
import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
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
import javax.xml.parsers.ParserConfigurationException;

import org.datalift.core.util.SimpleCache;
import org.datalift.exceptions.TechnicalException;
//import org.datalift.core.util.SimpleCache;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.ResourceResolver;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.project.WfsSource;
import org.datalift.fwk.view.TemplateModel;
import org.datalift.model.BaseConverterModule;
import org.datalift.model.ComplexFeature;
import org.datalift.model.FeatureTypeDescription;
import org.datalift.utilities.Helper;
import org.datalift.webServiceConverter2.WFS2Converter;
import org.datalift.wfs.wfs1_x.WfsConverter1_x;
import org.datalift.wfs.wfs1_x.WfsParser1_x;
import org.datalift.wfs.wfs2.WFS2Client;
import org.geotools.data.DataStore;
import org.geotools.data.Query;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureSource;
import org.opengis.referencing.ReferenceIdentifier;
import org.openrdf.rio.RDFHandlerException;
import org.xml.sax.SAXException;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.ign.datalift.model.AbstractFeature;




@Path(Wfstordf.MODULE_NAME)
public class Wfstordf extends BaseConverterModule{


	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------
	private final static int CACHE_DURATION = 3600 * 3; //3 hours
	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger();
	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "wfs2rdf";
	private final static SimpleCache<String,List<FeatureTypeDescription>> cache =
            new SimpleCache<String,List<FeatureTypeDescription>>(1000, CACHE_DURATION);
	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------
	public Wfstordf() {
		super(MODULE_NAME,1200, SourceType.WfsSource);
	}
	public Wfstordf(String name, int position, SourceType[] inputSources) {
		super(name, position, inputSources);
		// TODO Auto-generated constructor stub
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


	@GET
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getIndexPage(@QueryParam("project") URI projectId) {

		// Display available sources page.

		return this.newProjectView("availableWfsSources.vm", projectId);

	}
	/**
	 * get the list of selected feature types selected by the user to be converted
	 * @param json the json representation of the array containing the features to be converted
	 * @return the URL of the source project's page to be used by ajax to redirect the user
	 * @throws Exception 
	 */
	@POST
	@Path("postSelectedTypes")
	@Produces(MediaType.TEXT_PLAIN)
	@Consumes(MediaType.APPLICATION_JSON)
	public String postSelectedTypes(String json) throws Exception
	{
		String response = null;
		JsonParser parser = new JsonParser();
		JsonElement elements = parser.parse(json);

		JsonObject o = elements.getAsJsonObject();
		String project=o.get("project").getAsString();
		String source=o.get("source").getAsString();
		//int optionGraph= Integer.parseInt(o.get("graphOption").getAsString());
		int optionOntology= Integer.parseInt(o.get("ontologyOption").getAsString());
		boolean optionWGS84= Boolean.valueOf((o.get("convertSrsOption").getAsString()));
		if(Helper.isSet(project) && Helper.isSet(source))
		{	Project p=null;
		// Retrieve project
		URI projectUri;
		try {
			projectUri = new URI(project);
			p = this.getProject(projectUri);
			// Retrieve source.
			WfsSource s = (WfsSource)(p.getSource(source));
			JsonArray j = o.get("values").getAsJsonArray();
			Iterator<JsonElement> i = j.iterator();
			while ( i.hasNext() ){
				String typeName = i.next().getAsString();
				String potentialtargetGraph=s.getUri()+"/"+typeName;
				int countGraph=getOccurenceGraph(p, potentialtargetGraph);
				URI targetGraph = constructTargetGraphURI(p,potentialtargetGraph);
				countGraph++;
				URI baseUri=createBaseUri(targetGraph);
				String targetType=typeName+"-wfs";
				String destination_title=typeName+"(RDF# )"+countGraph; //count to be added later
				if(s.getVersion().equals("2.0.0"))
				{
					convertFeatureTypeToRdf2(projectUri,s, destination_title, targetGraph, baseUri, targetType,typeName,optionOntology, optionWGS84 );
				}
				else
				{
					convertFeatureTypeToRdf(projectUri,s, destination_title, targetGraph, baseUri, targetType,typeName,optionWGS84 );
				}
				System.out.println("done for "+typeName);
				// Register new transformed RDF source.
				Source out;
				try {
					out = this.addResultSource(p, s,
							"RDF mapping of " + s.getTitle()+"("+typeName+")", targetGraph);
					// Display project source tab, including the newly created source.
					response = this.created(out);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();		
				}					
			} 
		}catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			log.error(e1.getMessage());

		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		}
		return response;
	}

	@POST
	@Path("saveDescription")
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response saveDescription(@FormParam("project") URI projectId,
			@FormParam("source") URI sourceId)
	{
		Response response=null;
		// Retrieve project.
		Project project = this.getProject(projectId);
		// Retrieve source.
		WfsSource src = (WfsSource)(project.getSource(sourceId));
		if (src == null) {
			this.throwInvalidParamError("source", sourceId);
		}

		String potentialtargetGraph=src.getUri()+"/availableFT";

		URI targetGraph;
		try {
			int countExistingGraph=getOccurenceGraph(project, potentialtargetGraph);
			countExistingGraph++;
			targetGraph = constructTargetGraphURI(project,potentialtargetGraph);
			URI baseUri=createBaseUri(targetGraph);

			String targetType="availableFT"; //count to be added later

			convertFeatureTypeDescriptionToRdf(src, targetGraph, baseUri, targetType);

			// Register new transformed RDF source.
			Source out;
			try {
				out = this.addResultSource(project, src,
						"RDF mapping of " + src.getTitle()+"("+targetType+"#"+countExistingGraph+")", targetGraph);
				// Display project source tab, including the newly created source.
				response = this.createdRedirect(out).build();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();		
			}	
		} catch (URISyntaxException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return response;
	}

	private void convertFeatureTypeDescriptionToRdf(WfsSource src, URI targetGraph, URI baseUri, String targetType) {
		List<FeatureTypeDescription> data=null;
		if(src.getVersion().equals("2.0.0"))
		{
			data=cache.get(src.getSourceUrl()+src.getVersion());
		}
		else
		{
			data=cache.get(src.getSourceUrl()+src.getVersion()+"/"+src.getserverTypeStrategy());
		}
		if(data!=null)
		{
			WfsConverter1_x converter=new WfsConverter1_x();
			org.datalift.fwk.rdf.Repository target = Configuration.getDefault().getInternalRepository();
			converter.ConvertFeatureTypeDescriptionToRDF(data,target , targetGraph, baseUri, targetType);

		}
	}
	private boolean convertFeatureTypeToRdf(URI projectUri, WfsSource s, String destination_title, URI targetGraph,
			URI baseUri, String targetType, String typeName, boolean optionWGS84) {
		try {
			WfsParser1_x parser=new WfsParser1_x(s.getSourceUrl(),s.getVersion(),s.getserverTypeStrategy());
			ArrayList<AbstractFeature> featuresToConvert;

			featuresToConvert=parser.loadFeature(typeName,optionWGS84);


			if (featuresToConvert==null || featuresToConvert.size()==0) 
			{
				return false; //in this case, there is no features in this feature type!!!
			}
			WfsConverter1_x converter=new WfsConverter1_x();
			org.datalift.fwk.rdf.Repository target = Configuration.getDefault().getInternalRepository();
			converter.ConvertFeaturesToRDF(featuresToConvert,target , targetGraph, baseUri, targetType,parser.getFtCrs());

		} catch (IOException e) {
			TechnicalException error = new TechnicalException("convertFeatureTypeFailed", e, typeName);
			log.error(error.getMessage(), e);
			return false;
		}
		return true;

	}

	private URI createBaseUri(URI targetGraph) throws URISyntaxException {

		String graph=targetGraph.toString();
		//String graphuri="http://localhost:9091/project/demo/source/geoservice-brgm/availableFT-2";
		int startproj,startsource;

		startproj=graph.indexOf("/project");
		String part1 = graph.substring(0, startproj);

		startsource=graph.indexOf("/source");
		String part2= graph.substring(startproj+8,startsource);

		String part3= graph.substring(startsource+7);

		return new URI(part1+part2+part3);

	}
	private URI constructTargetGraphURI(Project p,String candidate) throws URISyntaxException
	{
		int countExistingGraph=getOccurenceGraph(p, candidate);

		countExistingGraph++;
		return new URI(candidate+"-"+countExistingGraph);
	}
	private int getOccurenceGraph(Project p,String candidate)
	{
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
			return numberValues.get(numberValues.size()-1);
		}
		else
		{
			return 0;
		}


	}




	/**
	 * 
	 * @param projectId
	 * @param sourceId
	 * @return the description of available feature types
	 */
	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response getFeatureTypes(
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
		WfsSource src = (WfsSource)(p.getSource(sourceId));
		if (src == null) {
			this.throwInvalidParamError("source", sourceId);
		}
		TemplateModel view = this.newView("availableFeatureTypes.vm", p);
		view.put("source", sourceId);
		try {
			List <FeatureTypeDescription> types;
			if(src.getVersion().equals("2.0.0"))
			{
				types= this.getfeatureTypeDescription2(src.getSourceUrl(),src.getVersion());			
			}
			else
			{
				types= this.getfeatureTypeDescription(src.getSourceUrl(),src.getVersion(),src.getserverTypeStrategy());
			}
			if(types!=null) 
			{
				view.put("types", types);
			}
			else
			{
				types=new ArrayList<FeatureTypeDescription>();
			}
			response = Response.ok(view);

		} catch (Exception e) {
			TechnicalException error = new TechnicalException("describeFeatureTypeFailed", e, sourceId);
			log.error(error.getMessage(), e);
			response = Response.serverError().entity(error.getLocalizedMessage())
					.type(MediaTypes.TEXT_PLAIN);
		}
		return response.build();
	}
	private List<FeatureTypeDescription> getfeatureTypeDescription2(String sourceUrl, String version) throws Exception {

		WFS2Client mp=new WFS2Client(sourceUrl);
		mp.getCapabilities();
		return mp.getFeatureTypeDescription();

	}
	private boolean convertFeatureTypeToRdf2(URI projectUri, WfsSource s, String destination_title, URI targetGraph,
			URI baseUri, String targetType, String typeName, int ontologyOption, boolean covertSrs) throws Exception {
		try {
			String srs=null;
			if(covertSrs)
			{
				srs="EPSG:4326";
			}
			WFS2Client client=new WFS2Client(s.getSourceUrl());
			client.getFeatureType(typeName,srs);
			//return a list of parsed features contained in typeName
			ComplexFeature featureCollectionToConvert=client.getUtilData(typeName);

			if (featureCollectionToConvert==null ) 
			{
				return false;
			}
			org.datalift.fwk.rdf.Repository target = Configuration.getDefault().getInternalRepository();
			//0: default converter
			//1: EMF group Converter
			WFS2Converter converter=new WFS2Converter(ontologyOption, target , targetGraph);
			converter.ConvertFeaturesToRDF(featureCollectionToConvert);
		} catch (IOException e) {
			TechnicalException error = new TechnicalException("convertFeatureTypeFailed", e, typeName);
			log.error(error.getMessage(), e);
			return false;
		} catch (RDFHandlerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return true;
	}
	/****
	 * send a request to wfs, parse the response, 
	 * retrieves data (feature type description) and insert them into features list 
	 * @param wfsUrl
	 * @throws IOException 
	 */

	private List<FeatureTypeDescription> getfeatureTypeDescription(String url, String version, String serverType) throws IOException
	{
		List<FeatureTypeDescription> descriptor=null;
		String urlcap=url;  
		WfsParser1_x p=new WfsParser1_x(urlcap,version,serverType);
		DataStore dataStore=p.getDataStore();
		if(dataStore!=null) 
		{	String cacheKey = urlcap+version;
		if(!version.equals("2.0.0")) {
			cacheKey += "/" + serverType;
		}
		descriptor = cache.get(cacheKey);

		if(descriptor==null)
		{	
			descriptor=new ArrayList<FeatureTypeDescription>();
			String typeNames[] = dataStore.getTypeNames();
			for (String typeName : typeNames) {
				log.debug("getting the source "+typeName+"in process...");
				SimpleFeatureSource source = dataStore.getFeatureSource(typeName); 
				log.debug("got the feature "+typeName+"source");
				log.debug("getting resource info for "+typeName+"in process...");
				ResourceInfo inf= source.getInfo();
				log.debug("got the resource info for"+typeName+"!");
				FeatureTypeDescription ftd=new FeatureTypeDescription();
				Iterator<ReferenceIdentifier> i = inf.getCRS().getIdentifiers().iterator();
				if(i.hasNext())
				{
					ftd.setDefaultSrs(Helper.constructSRIDValue(i.next().getCode()));
				}
				ftd.setName(inf.getName()); //name pattern : ns_featuretypename
				ftd.setSummary(inf.getDescription());
				ftd.setTitle(inf.getTitle());
				int numberreturned;
				try{
					log.debug("getting source count for "+typeName+"in process...");
					Query q=new Query();
					q.getMaxFeatures();
					numberreturned=source.getCount(null);
					log.debug("got the resource count for "+typeName+"!");
					ftd.setNumberFeature(numberreturned);
				}catch (Exception ee)
				{	//oups! i can't execute the request to get feature number! Bad version has been specified
					ftd.setNumberFeature(-1);
					log.warn("Failed to get description for typename {}", typeName);
				}
				//only add the feature type if it is available using the parameter specified (version and strategy)
				//if(ftd.getNumberFeature()!=-1)
				descriptor.add(ftd);
			}
			cache.put(cacheKey, descriptor);
		}

		} 
		return descriptor;
	}

}

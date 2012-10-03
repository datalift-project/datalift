package com.mondeca.datalift.client;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.http.client.URL;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootPanel;
import com.mondeca.datalift.client.actions.Argument;
import com.mondeca.datalift.client.actions.TransformationAction;
import com.mondeca.datalift.client.objects.GraphDataArray;
import com.mondeca.datalift.client.objects.OntologyDataArray;
import com.mondeca.datalift.client.objects.Resource;
import com.mondeca.datalift.client.widgets.ActionPanel;
import com.mondeca.datalift.client.widgets.SchemaSource;
import com.mondeca.datalift.client.widgets.SourceSelector;

/**
 * Entry point classes define <code>onModuleLoad()</code>.
 */
public class rdf2rdf implements EntryPoint,ICom {
	
	/*
	 * In development mode, add the project in URL e.g. "&project=http://localhost:9091/datalift/project/test"
	 * 
	 */
	
	public static String WORKSPACE_HASH_URL="project";
	public static String RDF2RDF_HASH_URL="rdf-transform";
	public static String LOV_HASH_URL="lov";
	
	private String project=null;
	
	private String serverBaseURL=null;
	private String sourceGraphURI=null;
//	private String sourceGraphName=null;
	private String targetGraphURI=null;
	private String targetGraphName=null;
	
	private List<TransformationAction> allActions=new ArrayList<TransformationAction>();
	
	private HorizontalPanel content = null;
	
	

	/**
	 * This is the entry point method.
	 */
	public void onModuleLoad() {
		
		project = Window.Location.getParameter("project");
		
		serverBaseURL = project.substring(0, project.lastIndexOf("/"+WORKSPACE_HASH_URL+"/")+1);
		
		//initialize the container (page without header)
		content = new HorizontalPanel();
		content.setWidth("100%");
		content.setHeight("100%");
		RootPanel.get("container").add(content);
		
		//initialize header
		initHeader();
		
		//display source selector
		
		final String JSON_URL = getServerBaseURL()+rdf2rdf.RDF2RDF_HASH_URL+"/sources?project="+project;
		String url = URL.encode(JSON_URL);
		
		JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
		jsonp.requestObject(url, new AsyncCallback<GraphDataArray>() {

			public void onFailure(Throwable caught) {
				displayError("Failure:" + caught.getMessage());
			}

			public void onSuccess(GraphDataArray result) {
				SourceSelector sourceSelector = new SourceSelector(rdf2rdf.this,result);
				content.add(sourceSelector);
				content.setCellHorizontalAlignment(sourceSelector, HasHorizontalAlignment.ALIGN_CENTER);
				content.setCellVerticalAlignment(sourceSelector, HasVerticalAlignment.ALIGN_MIDDLE);
			}					
			
		});
		
		
	
	}
	
	/**
	 * this method inject action buttons in the header
	 */
	private void initHeader(){
		RootPanel executeBtnWrapper = RootPanel.get("executeBtn");
		Label executeBtn = new Label("Executer");
		executeBtn.setStyleName("execute");
		executeBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent arg0) {
				
				final DialogBox popup = new DialogBox(false,true);
				popup.setGlassEnabled(true);
				popup.setWidget(new Image("./images/loading.gif"));
				
				popup.center();
				
//				@POST
//			    @Path("execute")
//			    public Response executeScript(
//			            @QueryParam("project") URI projectId,
//			            @QueryParam("source") URI sourceId,
//			            @FormParam("dest_title") String destTitle,
//			            @FormParam("dest_graph_uri") URI targetGraph,
//			            @FormParam("script") String script)
			
				StringBuilder script=new StringBuilder();
				for (TransformationAction action : allActions) {
					String text = action.toText();
					if(text!=null){
						if(script.length()>0)script.append(";");
						script.append(action.toText());
					}
				};
				System.out.println("execute ! "+script.toString());
				
				
				
				
				final StringBuilder JSON_URL = new StringBuilder();
				JSON_URL.append(getServerBaseURL()+rdf2rdf.RDF2RDF_HASH_URL+"/execute");
				JSON_URL.append("?project="+URL.encodeQueryString(project));
				JSON_URL.append("&source="+URL.encodeQueryString(sourceGraphURI));
				System.out.println(JSON_URL);
	
	
				
				// Send request to server and catch any errors.
				RequestBuilder builder = new RequestBuilder(RequestBuilder.POST, JSON_URL.toString());
//				builder.setHeader("Content-type", "application/x-www-form-urlencoded");
				
				StringBuilder params = new StringBuilder();
				params.append("dest_title="+URL.encodeQueryString(targetGraphName));
				params.append("&dest_graph_uri="+URL.encode(targetGraphURI));
				params.append("&script="+URL.encodeQueryString(script.toString()));
//				System.out.println(formParams);
				try {
					builder.sendRequest(params.toString(), new RequestCallback() {
						public void onError(Request request, Throwable exception) {
							popup.hide();
							displayError("Couldn't execute");
						}
	
						public void onResponseReceived(Request request, Response response) {
							if (200 == response.getStatusCode()) {
//								System.out.println(response.getText());
								Window.Location.replace(project+"#source");
							} else {
								displayError("Couldn't execute (" + response.getStatusText()+", "+response.getText()
										+ ")");
							}
						}
					});
				} catch (RequestException e) {
					displayError("Couldn't retrieve JSON");
				}
			}
		});
		executeBtnWrapper.add(executeBtn);
		
		RootPanel cancelBtnWrapper = RootPanel.get("cancelBtn");
		Label cancelBtn = new Label("Retourner au workspace");
		cancelBtn.setStyleName("cancel");
		cancelBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent arg0) {
				Window.Location.replace(project);
			}
		});
		cancelBtnWrapper.add(cancelBtn);
	}
	
	
	/**
	 * This method display the source schema and propose some transformation actions on each element
	 */
	private void displaySourceSchema(){
		content.clear();
		
		//display source selector
				final String JSON_URL = getServerBaseURL()+rdf2rdf.RDF2RDF_HASH_URL+"/ontology?source="+sourceGraphURI;
				String url = URL.encode(JSON_URL);
				
				JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
				jsonp.requestObject(url, new AsyncCallback<OntologyDataArray>() {

					public void onFailure(Throwable caught) {
						displayError("Failure:" + caught.getMessage());
					}

					public void onSuccess(OntologyDataArray result) {
						//add schemaSourcePart
						SchemaSource schemaSource = new SchemaSource(rdf2rdf.this, result);
						content.add(schemaSource);
						content.setCellWidth(schemaSource, "50%");
						schemaSource.init();
					}					
					
				});
	}
	

	public void removeActionPanel(){
		//remove action panel if already present
		if(content.getWidgetCount()>1)content.remove(1);
	}
	
	public void displayActionsForResource(Resource actionSubjectResource){
		//remove action panel if already present
		if(content.getWidgetCount()>1)content.remove(1);
		
		//display action panel
		//TODO fetch dynamically the list of available actions
		ActionPanel actionPanel = new ActionPanel(this, actionSubjectResource,getAvailableAction());
		content.add(actionPanel);
	}


	public void displayError(String error) {
		//TODO popup  ?
		System.out.println(error);
	}
	
	public void setGraphInformation(String sourceGraphURI,
			String sourceGraphName, String targetGraphURI,
			String targetGraphName) {
		this.sourceGraphURI=sourceGraphURI;
		this.targetGraphURI=targetGraphURI;
		this.targetGraphName=targetGraphName;
		
		//get source schema and display screen
		displaySourceSchema();
	}
	
	
	/*  ####### STATIC objects using in tests ############## */
//	private GraphDataArray getListOfSourceGraphs(){
//		String json = "{\"graphs\":["+
//        "{\"graphURI\": \"http://datalift/source_graph_01.rdf\", \"graphName\": \"source graph 01\"}, "+
//        "{\"graphURI\": \"http://datalift/source_graph_02.rdf\", \"graphName\": \"source graph 02\"}, "+
//        "{\"graphURI\": \"http://datalift/source_graph_03.rdf\", \"graphName\": \"source graph 03\"} "+
//        "]}";
//		return JSONDataParser.asArrayOfGraphDataArray(json);
//	}
	
//	private ClassDataArray getClassesSourceSchema(){
//		String json = "{\"classes\":["+
//        "{\"classURI\": \"http://datalift/source_graph_01.rdf/University\", \"className\": \"University\", \"classParentURI\": \"\"}, "+
//        "{\"classURI\": \"http://datalift/source_graph_01.rdf/College\", \"className\": \"College\", \"classParentURI\": \"\"}, "+
//        "{\"classURI\": \"http://datalift/source_graph_01.rdf/School\", \"className\": \"School\", \"classParentURI\": \"\"}, "+
//        "]," +
//        "\"properties\":["+
//        "{\"propertyURI\": \"http://datalift/source_graph_01.rdf/name\", \"propertyName\": \"name\", \"propertyParentURI\": \"\", \"propertyType\": \"\"}, "+
//        "{\"propertyURI\": \"http://datalift/source_graph_01.rdf/latitude\", \"propertyName\": \"latitude\", \"propertyParentURI\": \"\", \"propertyType\": \"\"}, "+
//        "{\"propertyURI\": \"http://datalift/source_graph_01.rdf/longitude\", \"propertyName\": \"longitude\", \"propertyParentURI\": \"\", \"propertyType\": \"\"}, "+
//        "]" +
//        "}";
//		return JSONDataParser.asArrayOfClassDataArray(json);
//	}
	
//	private PropertyDataArray getPropertiesSourceSchema(){
//		String json = "{\"properties\":["+
//        "{\"propertyURI\": \"http://datalift/source_graph_01.rdf/name\", \"propertyName\": \"name\", \"propertyParentURI\": \"\", \"propertyType\": \"\"}, "+
//        "{\"propertyURI\": \"http://datalift/source_graph_01.rdf/latitude\", \"propertyName\": \"latitude\", \"propertyParentURI\": \"\", \"propertyType\": \"\"}, "+
//        "{\"propertyURI\": \"http://datalift/source_graph_01.rdf/longitude\", \"propertyName\": \"longitude\", \"propertyParentURI\": \"\", \"propertyType\": \"\"}, "+
//        "]}";
//		return JSONDataParser.asArrayOfPropertyDataArray(json);
//	}
	
	private List<TransformationAction> getAvailableAction(){
		
		List<TransformationAction> actionsAvailable = new ArrayList<TransformationAction>();
		
		//moveP2P
		Argument moveP2P_source = new Argument("source", "Source predicate", true, 0, "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property", true);
		Argument moveP2P_target = new Argument("target", "Target predicate", true, 1, "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property", false);
		TransformationAction action_P2P = new TransformationAction("MOVE_PREDICATE","Move predicate to another","./images/simple_match.png","DELETE { ?s ?source ?o } INSERT { ?s ?target ?o } WHERE { ?s ?source ?o }");
		action_P2P.getHasArgument().add(moveP2P_source);
		action_P2P.getHasArgument().add(moveP2P_target);
		actionsAvailable.add(action_P2P);
		
		//moveC2C
		Argument moveC2C_source = new Argument("source", "Source class", true, 0, "http://www.w3.org/2000/01/rdf-schema#Class", true);
		Argument moveC2C_target = new Argument("target", "Target class", true, 1, "http://www.w3.org/2000/01/rdf-schema#Class", false);
		TransformationAction action_C2C = new TransformationAction("MOVE_CLASS","Move class to another","./images/simple_match.png","DELETE { ?s ?source ?o } INSERT { ?s ?target ?o } WHERE { ?s ?source ?o }");
		action_C2C.getHasArgument().add(moveC2C_source);
		action_C2C.getHasArgument().add(moveC2C_target);
		actionsAvailable.add(action_C2C);
		
		return actionsAvailable;
	}
	
	public List<TransformationAction> getAllActions(){
		return allActions;
	}


	public String getServerBaseURL(){
		return serverBaseURL;
	}
}

package com.mondeca.datalift.client.widgets;

import com.google.gwt.dom.client.Document;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DomEvent;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DecoratorPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.mondeca.datalift.client.ICom;
import com.mondeca.datalift.client.objects.GraphDataArray;

/**
 * This widget is displayed when the module is loaded in order to choose a source and target RDF graph
 * 
 * @author Pierre-Yves
 *
 */
public class SourceSelector extends DecoratorPanel{
	ListBox listBoxSourceGraph;
	TextBox txtBoxTargetGraphName;
	TextBox txtBoxTargetGrapheURI;

	
	public SourceSelector(final ICom iCom, final GraphDataArray sourceGraphs){
		
		// Create a table to layout the form options
	    FlexTable layout = new FlexTable();
	    layout.setCellSpacing(6);
	    FlexCellFormatter cellFormatter = layout.getFlexCellFormatter();

	    // Add a title to the form
	    layout.setHTML(0, 0, "Sélectionnez les informations de sources");
	    cellFormatter.setColSpan(0, 0, 2);
	    cellFormatter.setHorizontalAlignment(
	        0, 0, HasHorizontalAlignment.ALIGN_CENTER);

	    // Source graph to use
	    layout.setHTML(1, 0, "Source à convertir :");
	    listBoxSourceGraph = new ListBox();
	    if(sourceGraphs!=null){
		    for (int i = 0; i < sourceGraphs.getEntries().length(); i++) {
		    	listBoxSourceGraph.addItem(sourceGraphs.getEntries().get(i).getGraphName(),sourceGraphs.getEntries().get(i).getGraphURI());
		     }
	    }
	    listBoxSourceGraph.addChangeHandler(new ChangeHandler() {
			
			public void onChange(ChangeEvent arg0) {
				//on selection, fill the two other text box
				String graphURI = listBoxSourceGraph.getValue(listBoxSourceGraph.getSelectedIndex());
				String graphName = listBoxSourceGraph.getItemText(listBoxSourceGraph.getSelectedIndex());
				
				int i=1;
				String targetGraphName = graphName+" - (RDF TRANSFORM #"+i+")";
				while(graphNameExists(targetGraphName,sourceGraphs)){
					i++;
					targetGraphName = graphName+" - (RDF TRANSFORM #"+i+")";
				}
				txtBoxTargetGraphName.setText(targetGraphName);
				
//				txtBoxTargetGrapheURI.setText(graphURI.substring(0,graphURI.lastIndexOf("/")+1)+URL.encode(targetGraphName));
				txtBoxTargetGrapheURI.setText(graphURI+"-rdf_tranform-"+i);
			}
		});
	   
	    layout.setWidget(1, 1, listBoxSourceGraph);
	    
	    
	    //Target Graphe Name
	    layout.setHTML(2, 0, "Nom de la source à créer :");
	    txtBoxTargetGraphName = new TextBox();
	    txtBoxTargetGraphName.setWidth("600px");
	    layout.setWidget(2, 1, txtBoxTargetGraphName);
	    
	    layout.setHTML(3, 0, "URI du graphe nommé associé :");
	    txtBoxTargetGrapheURI = new TextBox();
	    txtBoxTargetGrapheURI.setWidth("600px");
	    layout.setWidget(3, 1, txtBoxTargetGrapheURI);
	    
	    Button validateBtn = new Button("Valider");
	    validateBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent arg0) {
				String targetGraphName  = txtBoxTargetGraphName.getText().trim();
//				String targetGraphURI = URL.decode(txtBoxTargetGrapheURI.getText().trim());
				String targetGraphURI = txtBoxTargetGrapheURI.getText().trim();
				if(targetGraphName.length()>0 && targetGraphURI.length()>0  && !graphNameExists(targetGraphName,sourceGraphs)){
					// action on validate sources
					String sourceGraphURI = listBoxSourceGraph.getValue(listBoxSourceGraph.getSelectedIndex());
					String sourceGraphName = listBoxSourceGraph.getItemText(listBoxSourceGraph.getSelectedIndex());
					iCom.setGraphInformation(sourceGraphURI, sourceGraphName, targetGraphURI, targetGraphName);
				}
				else{
					iCom.displayError("Tous les champs de saisi sont obligatoires");
				}
			}
		});
	    layout.setWidget(4, 0, validateBtn);
	    cellFormatter.setColSpan(4, 0, 2);
	    cellFormatter.setHorizontalAlignment(
	        4, 0, HasHorizontalAlignment.ALIGN_CENTER);
		
	    this.addStyleName("SourceSelector");
	    this.setWidget(layout);
	    
	    
	    //if a source exists, fire changeevent 
	    if(sourceGraphs!=null){
	    	DomEvent.fireNativeEvent(Document.get().createChangeEvent(), listBoxSourceGraph);
	    }
	}
	
	private boolean graphNameExists(String graphName,  GraphDataArray sourceGraphs){
		 if(sourceGraphs!=null){
			    for (int i = 0; i < sourceGraphs.getEntries().length(); i++) {
			    	if(sourceGraphs.getEntries().get(i).getGraphName().equals(graphName))return true;
			     }
		    }
		return false;
	}

}

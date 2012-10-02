package com.mondeca.datalift.client.widgets;


import java.util.List;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.ActionCell;
import com.google.gwt.cell.client.Cell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.http.client.URL;
import com.google.gwt.jsonp.client.JsonpRequestBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.SimplePager;
import com.google.gwt.user.cellview.client.SimplePager.TextLocation;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.Range;
import com.google.gwt.view.client.RangeChangeEvent;
import com.google.gwt.view.client.RangeChangeEvent.Handler;
import com.mondeca.datalift.client.ICom;
import com.mondeca.datalift.client.rdf2rdf;
import com.mondeca.datalift.client.actions.Argument;
import com.mondeca.datalift.client.actions.TransformationAction;
import com.mondeca.datalift.client.objects.Resource;
import com.mondeca.datalift.client.objects.VocabElement;
import com.mondeca.datalift.client.objects.VocabElementDataArray;
import com.mondeca.datalift.client.objects.VocabularyData;
import com.sun.corba.se.pept.transport.ContactInfo;

public class ActionPanel extends VerticalPanel {
	
	private ICom iCom;
	private FlexTable table = new FlexTable();
	private Resource actionSubjectResource;
	private List<TransformationAction> availableActions;
	
	public ActionPanel(ICom iCom, Resource actionSubjectResource, List<TransformationAction> availableActions){
		this.iCom=iCom;
		this.actionSubjectResource=actionSubjectResource;
		this.availableActions=availableActions;
		
		this.setWidth("100%");
		
		//add header
		HTML header = new HTML("<div class=\"bar_title\">&nbsp;</div>" +
				"<div class=\"title\"><span class=\"white_bg\">Action de transformation de \"<b>"+actionSubjectResource.getName()+"</b>\"&nbsp;&nbsp;</span></div>");
		this.add(header);
		
		//add box
		VerticalPanel box = new VerticalPanel();
		box.addStyleName("gray_box");
		this.add(box);
		
		//add action panel
		box.add(buildActionPanel());
		
		//add classes and properties columns
//		table.addStyleName();
		box.add(table);
		box.setCellWidth(table, "100%");
		table.setWidth("100%");
		table.setCellSpacing(0);
		table.addStyleName("tableActions");
		
		displayActions();
		
	}
	
	private HorizontalPanel buildActionPanel(){
		HorizontalPanel addActionPanel = new HorizontalPanel();
		addActionPanel.add(new Label("Ajouter une action de transformation : "));
		
		final ListBox listBoxAction = new ListBox();
		addActionPanel.add(listBoxAction);
	    for (int i = 0; i < availableActions.size(); i++) {
	    	//if applicable
	    	if(actionSubjectResource.isClass() && availableActions.get(i).isApplicableOnClass() ||
	    			actionSubjectResource.isProperty() && availableActions.get(i).isApplicableOnProperty())
	    	listBoxAction.addItem(availableActions.get(i).getDisplayName(),availableActions.get(i).getName());
	    }
	    
	    Button validateBtn = new Button("OK");
	    addActionPanel.add(validateBtn);
	    validateBtn.addClickHandler(new ClickHandler() {
			
			public void onClick(ClickEvent arg0) {
				String value  = listBoxAction.getValue(listBoxAction.getSelectedIndex());
				 for (int i = 0; i < availableActions.size(); i++) {
					 if(availableActions.get(i).getName().equals(value)){
						 addAction(availableActions.get(i));
					 }
				 }
			}
		});
	    
//				String graphURI = listBoxSourceGraph.getValue(listBoxSourceGraph.getSelectedIndex());
//				String graphName = listBoxSourceGraph.getItemText(listBoxSourceGraph.getSelectedIndex());
		
		return addActionPanel;
	}
	
	public void addAction(TransformationAction action){
		
		//instanciate a new action with current resource
		final TransformationAction act = new TransformationAction(action);
		act.addResourceToFirstARgumentMatchable(actionSubjectResource);
		
		
		final DialogBox popup = new DialogBox(false,true);
		popup.setGlassEnabled(true);
		
		FlexTable table = new FlexTable();
		table.setCellSpacing(0);
		//header
		HorizontalPanel header = new HorizontalPanel();
		
		if(act.getImagePath()!=null && act.getImagePath().length()>0){
			header.add(new Image(act.getImagePath()));
		}
		Label headerLbl = new Label(act.getDisplayName());
		headerLbl.addStyleName("headerLbl");
		header.add(headerLbl);
		
		Button cancel = new Button("Annuler");
		cancel.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent arg0) {
				popup.hide();
			}
		});
		header.add(cancel);
		
		Button valid = new Button("Valider");
		valid.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent arg0) {
				
				//check if all args are valid
				if(act.isValid()){
					//add action to the list
					iCom.getAllActions().add(act);
					displayActions();
					
					//hide and refresh action list display
					popup.hide();
				}
			}
		});
		header.add(valid);
		
		table.setWidget(0, 0, header);
		table.getFlexCellFormatter().setColSpan(0, 0, 3);
		
		
		//arguments
		for (final Argument arg : act.getHasArgument()) {
			int row = table.getRowCount();
			table.setWidget(row, 0, new Label(arg.getDisplayName()+" : "));
			if(arg.getResource()!=null){
				Label resLinked = new Label(arg.getResource().getName());
				table.setWidget(row, 1, resLinked);
			}
			else{
				
				final Label resLinked = new Label("Choisir ...");
				final FocusPanel focusResLinked = new FocusPanel(resLinked);
				resLinked.addStyleName("clickable");
				resLinked.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent arg0) {
						
						
						
						final DialogBox popup = new DialogBox(false,true);
						popup.setGlassEnabled(true);
						final VerticalPanel tablePopupSearch = new VerticalPanel();
						tablePopupSearch.addStyleName("tableActions");
						
						
						final SimplePanel resultPanel = new SimplePanel();
						//search bar
						final TextBox inputSearchBox = new TextBox();
						if(actionSubjectResource.getName().length()>3){
							inputSearchBox.setText(actionSubjectResource.getName());
						}
						inputSearchBox.addKeyDownHandler(new KeyDownHandler() {
						    public void onKeyDown(KeyDownEvent event) {
						        if (event.getNativeKeyCode() == KeyCodes.KEY_ENTER) {
						        	displayResults(resultPanel,inputSearchBox,arg,resLinked,popup, focusResLinked);
						        }

						    }

						});
						Button searchBtn = new Button("Rechercher");
						searchBtn.addClickHandler(new ClickHandler() {
							public void onClick(ClickEvent arg0) {
								displayResults(resultPanel, inputSearchBox,arg,resLinked,popup, focusResLinked);
							}
						});
						HorizontalPanel header = new HorizontalPanel();
						header.add(inputSearchBox);
						header.add(searchBtn);
						
						HorizontalPanel titlePanel = new HorizontalPanel();
						titlePanel.add(new Label("Choisir une valeur pour \""+arg.getDisplayName()+"\""));
						Button cancel = new Button("Annuler");
						cancel.addClickHandler(new ClickHandler() {
							public void onClick(ClickEvent arg0) {
								popup.hide();
								focusResLinked.setFocus(true);
							}
						});
						titlePanel.add(cancel);
						
						tablePopupSearch.add(titlePanel);
						tablePopupSearch.add(header);
						tablePopupSearch.add(resultPanel);
						popup.setWidget(tablePopupSearch);
						popup.center();
						
						
						
					}
				});
				table.setWidget(row, 1, focusResLinked);
			}
		}
		
		popup.setWidget(table);
		
		popup.center();
//		System.out.println("Add action "+act.getDisplayName());
	}
	
	public void displayResults(final SimplePanel tablePopupSearch, TextBox inputSearchBox,final Argument arg, final Label resLinked, final DialogBox popup, final FocusPanel focusResLinked ){
		tablePopupSearch.clear();
		
		if(inputSearchBox.getText().trim().length()>0){
			tablePopupSearch.setWidget(new Image("./images/loading.gif"));
			final String JSON_URL = iCom.getServerBaseURL()+rdf2rdf.LOV_HASH_URL+"/vocabElements?query="+URL.encodeQueryString(inputSearchBox.getText().trim())+"&type="+URL.encodeQueryString(arg.getArgumentRange());
			String url = JSON_URL;
//			System.out.println("1 : " + url);
//			url += "&type="+arg.getArgumentRange().replaceAll("#", "%23");
//			System.out.println("2 : " + url);
			
			JsonpRequestBuilder jsonp = new JsonpRequestBuilder();
			jsonp.requestObject(url, new AsyncCallback<VocabElementDataArray>() {

				public void onFailure(Throwable caught) {
					iCom.displayError("Failure:" + caught.getMessage());
				}

				public void onSuccess(final VocabElementDataArray result) {
					final List<VocabElement> elements = VocabElement.getListFormDataArray(result);
					
					if(elements.size()>0){
						final CellTable<VocabElement> table = new CellTable<VocabElement>();
						table.setPageSize(10);
					    table.setRowCount(elements.size());
						
					    
					    // Add a text user to show the label.
					    TextColumn<VocabElement> userColumn = new TextColumn<VocabElement>() {
					      @Override
					      public String getValue(VocabElement object) {
					        return object.getVocabElementName();
					      }
					    };
					    table.addColumn(userColumn, "Label");
	
					    // Add a text column to show the namURIe.
					    TextColumn<VocabElement> dateColumn = new TextColumn<VocabElement>() {
					      @Override
					      public String getValue(VocabElement elem) {
					        return  elem.getVocabElementURIPrefixed();
					      }
					    };
					    dateColumn.setCellStyleNames("uriPrefixed");
					    table.addColumn(dateColumn, "URI");
					    
					   
					    table.addColumn(buildColumn(new ActionCell<VocabElement>("Choisir", new ActionCell.Delegate<VocabElement>() {
					        public void execute(VocabElement elem) {
					        	arg.setResource(new Resource(elem.getVocabElementURI(), elem.getVocabElementName(), elem.getVocabElementURIPrefixed()));
								resLinked.setText(elem.getVocabElementName());
								popup.hide();
								focusResLinked.setFocus(true);
					        }
					      }),"", new GetValue<VocabElement>() {
					        public VocabElement getValue(VocabElement elem) {
					          return elem;
					        }
					      }, null),"");
					    
					    
					    
					 // Push the data into the widget.
					    table.setRowData(0, elements);
					    
					    // for update
					    table.addRangeChangeHandler(new Handler() {
							public void onRangeChange(RangeChangeEvent event) {
								 Range range = table.getVisibleRange();
								    int start = range.getStart();
								    table.setRowData(start, elements.subList(start, elements.size()));
							}
						});
					    
					    
					    // Create a Pager to control the table.
					    SimplePager.Resources pagerResources = GWT.create(SimplePager.Resources.class);
					    SimplePager pager = new SimplePager(TextLocation.CENTER, pagerResources, false, 0, true);
					    pager.setDisplay(table);
					
					    
					    VerticalPanel vp = new VerticalPanel();
					    vp.add(table);
					    vp.add(pager);
					    vp.setCellWidth(pager, "100%");
					    vp.setCellHorizontalAlignment(pager, HasHorizontalAlignment.ALIGN_CENTER);
					    tablePopupSearch.setWidget(vp);
					}
					else{//no result
						tablePopupSearch.setWidget(new Label("Aucun résultat ne correspond à cette recherche"));
					}
				    
				}
			});
		}
		
		
	}
	
	public void displayActions(){
		//clean table
		for (int i = 0; i < table.getRowCount(); i++) {
			table.removeRow(i);
		}
		
		List<TransformationAction> actions = iCom.getAllActions();
		
		
		for (final TransformationAction act : actions) {
			
			//select only actions where the current resource is involved
			if(act.containsResource(actionSubjectResource)){
			
				FlexTable tableAction = new FlexTable();
				tableAction.setWidth("100%");
				
				
				//header
				HorizontalPanel header = new HorizontalPanel();
				
				if(act.getImagePath()!=null && act.getImagePath().length()>0){
					header.add(new Image(act.getImagePath()));
				}
				header.add(new Label(act.getDisplayName()));
				tableAction.setWidget(0, 0, header);
				tableAction.getFlexCellFormatter().setColSpan(0, 0, 2);
				
				
				for (final Argument arg : act.getHasArgument()) {
					int rowAction = tableAction.getRowCount();	
					tableAction.setWidget(rowAction, 0, new Label(arg.getDisplayName()+" : "));
					if(arg.getResource()!=null){
						Label resLinked = new Label(arg.getResource().getName());
						tableAction.setWidget(rowAction, 1, resLinked);
					}
				}
				
				int row =table.getRowCount();
				table.setWidget(row, 0, tableAction);
				if(row%2==0)table.getRowFormatter().addStyleName(row, "row_odd");
				
				//add delete action
				Image deleteImage = new Image("./images/delete.png");
				deleteImage.addStyleName("clickable");
				deleteImage.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent arg0) {
						iCom.getAllActions().remove(act);
						displayActions();
					}
				});
				table.setWidget(row, 1, deleteImage);
				//TODO add edit
			}
		}
	}
	
	
	
	
	
	
	/**
	   * Add a column with a header.
	   * 
	   * @param <C> the cell type
	   * @param cell the cell used to render the column
	   * @param headerText the header string
	   * @param getter the value getter for the cell
	   */
	  private <C> Column<VocabElement, C> buildColumn(Cell<C> cell, String text,
	      final GetValue<C> getter, FieldUpdater<VocabElement, C> fieldUpdater) {
	    Column<VocabElement, C> column = new Column<VocabElement, C>(cell) {
	      @Override
	      public C getValue(VocabElement object) {
	        return getter.getValue(object);
	      }
	    };
	    column.setFieldUpdater(fieldUpdater);
	    
	    return column;
	  }
	
	  /**
	   * Get a cell value from a record.
	   * 
	   * @param <C> the cell type
	   */
	  private static interface GetValue<C> {
	    C getValue(VocabElement contact);
	  }
}

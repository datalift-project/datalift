package com.mondeca.datalift.client.widgets;


import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.mondeca.datalift.client.ICom;
import com.mondeca.datalift.client.objects.ClassData;
import com.mondeca.datalift.client.objects.OntologyDataArray;
import com.mondeca.datalift.client.objects.PropertyData;
import com.mondeca.datalift.client.objects.Resource;

public class SchemaSource extends VerticalPanel {
	private FlexTable table = new FlexTable();
	JsArray<ClassData> classes = null;
	JsArray<PropertyData> properties = null;
	private ICom iCom = null;
	HTML breadCrumbs = new HTML();

	public SchemaSource(ICom iCom, OntologyDataArray ontologyDataArray){
		this.iCom=iCom;
		this.setWidth("100%");
		this.classes=ontologyDataArray.getClasses();
		this.properties=ontologyDataArray.getProperties();
		
		//add header
		HTML header = new HTML("<div class=\"bar_title\">&nbsp;</div>" +
				"<div class=\"title\"><span class=\"white_bg\">Vocabulaire source &nbsp;&nbsp;</span></div>");
		this.add(header);
		
		//add box
		VerticalPanel box = new VerticalPanel();
		box.addStyleName("gray_box");
		this.add(box);
		
		box.add(breadCrumbs);
		
		//add classes and properties columns
		table.addStyleName("miller_columns");
		table.getColumnFormatter().setWidth(0, "50%");
		table.getColumnFormatter().setWidth(1, "50%");
		box.add(table);
		box.setCellWidth(table, "100%");
	}

	public void init(){
		//header
		HorizontalPanel header = new HorizontalPanel();
		final Label classes = new Label();
		classes.addStyleName("tab_class");
		classes.addStyleName("floatl");
		 
		final Label properties = new Label();
		properties.addStyleName("tab_property");
		properties.addStyleName("floatl");
		
		classes.setText("Classes");
		classes.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent arg0) {
				classes.addStyleName("selected");
				properties.removeStyleName("selected");
				displayClasses();
			}
		});
		header.add(classes);
		properties.setText("Propriétés");
		properties.addClickHandler(new ClickHandler() {
			public void onClick(ClickEvent arg0) {
				properties.addStyleName("selected");
				classes.removeStyleName("selected");
				displayProperties();
			}
		});
		header.add(properties);
		table.setWidget(0, 0, header);
		
		//select classes by default
		if(this.classes.length()>0){
			classes.addStyleName("selected");
			displayClasses();
		}else{
			properties.addStyleName("selected");
			displayProperties();
		}
		
	}
	
	/**
	 * Display classes hierarchy
	 */
	private void displayClasses(){
		//clean table but header
		if(table.getRowCount()>1){
			for (int i = table.getRowCount()-1; i > 0; i--) {
				table.removeRow(i);
			}
		}
		
		if(classes.length()>0){
			
			for (int i = 0; i < classes.length(); i++) {
				
				final String classNameString = classes.get(i).getClassName();
				final String classURIString = classes.get(i).getClassURI();
				final HTML className = new HTML("<div>"+classNameString+"</div>");
				className.setTitle(classURIString);
				className.addStyleName("miller_columns_element");
				className.addStyleName("item");
				className.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent arg0) {
						//clear selection and select the current one
						for(int i=1; i<table.getRowCount(); i++){
							table.getWidget(i, 0).removeStyleName("selected");
						}
						
						selectClass(className,new Resource(classURIString, classNameString, Resource.TYPE_CLASS));
					}
				});
				
				
				//select the first one
				if(i==0){
					selectClass(className,new Resource(classURIString, classNameString, Resource.TYPE_CLASS));
				}
				table.setWidget(table.getRowCount(), 0, className);
				//TODO add subclasses in the next column
			}
		}else{
			iCom.removeActionPanel();
			breadCrumbs.setHTML("<div class=\"breadcrumbs\">Il n'existe pas de classe</div>");
		}
	}
	
	/**
	 * Display Properties Hierarchy
	 */
	private void displayProperties(){
		//clean table but header
		if(table.getRowCount()>1){
			for (int i = table.getRowCount()-1; i > 0; i--) {
				table.removeRow(i);
			}
		}
		
		if(properties.length()>0){
			for (int i = 0; i < properties.length(); i++) {
				
				final String propertyNameString = properties.get(i).getPropertyName();
				final String propertyURIString = properties.get(i).getPropertyURI();
				final HTML propertyName = new HTML("<div>"+propertyNameString+"</div>");
				propertyName.setTitle(propertyURIString);
				propertyName.addStyleName("miller_columns_element");
				propertyName.addStyleName("item");
				propertyName.addClickHandler(new ClickHandler() {
					public void onClick(ClickEvent arg0) {
						//clear selection and select the current one
						for(int i=1; i<table.getRowCount(); i++){
							table.getWidget(i, 0).removeStyleName("selected");
						}
						
						selectProperty(propertyName, new Resource(propertyURIString, propertyNameString, Resource.TYPE_PROPERTY));
					}
				});
				
				
				//select the first one
				if(i==0){
					selectProperty(propertyName, new Resource(propertyURIString, propertyNameString, Resource.TYPE_PROPERTY));
				}
				table.setWidget(table.getRowCount(), 0, propertyName);
				//TODO add subproperties in the next column
			}
		}else{
			iCom.removeActionPanel();
			breadCrumbs.setHTML("<div class=\"breadcrumbs\">Il n'existe pas de propriété</div>");
		}
	}
	
	private void selectClass(Widget classWidget, Resource classResource){
		classWidget.addStyleName("selected");
		buildClassBreadCrumbs(classResource.getURI());
		iCom.displayActionsForResource(classResource);
	}
	
	private void selectProperty(Widget propWidget, Resource propResource){
		propWidget.addStyleName("selected");
		buildPropertyBreadCrumbs(propResource.getURI());
		iCom.displayActionsForResource(propResource);
	}
	
	
	private void buildClassBreadCrumbs(String classURI ){
		String s = "";
		while(classURI!=null){
			for (int i = 0; i < classes.length(); i++) {
				if(classes.get(i).getClassURI().equals(classURI)){
					s=" > "+classes.get(i).getClassName()+s;
					if(classes.get(i).getClassParentURI()!=null && classes.get(i).getClassParentURI().length()>0){
						//it has a parent, then continue.
						classURI=classes.get(i).getClassParentURI();
					}
					else classURI=null;
						
					break;
				}
			}
		}
		s="Classes"+s;
		
		breadCrumbs.setHTML("<div class=\"breadcrumbs\">"+s+"</div>");
	}
	
	private void buildPropertyBreadCrumbs(String propertyURI ){
		String s = "";
		while(propertyURI!=null){
			for (int i = 0; i < properties.length(); i++) {
				if(properties.get(i).getPropertyURI().equals(propertyURI)){
					s=" > "+properties.get(i).getPropertyName()+s;
					if(properties.get(i).getPropertyParentURI()!=null && properties.get(i).getPropertyParentURI().length()>0){
						//it has a parent, then continue.
						propertyURI=properties.get(i).getPropertyParentURI();
					}
					else propertyURI=null;
						
					break;
				}
			}
		}
		s="Propriétés"+s;
		
		breadCrumbs.setHTML("<div class=\"breadcrumbs\">"+s+"</div>");
	}
	
}

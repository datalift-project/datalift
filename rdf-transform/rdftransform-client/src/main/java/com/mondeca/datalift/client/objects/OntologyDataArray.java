package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class OntologyDataArray extends JavaScriptObject {
	
	  protected OntologyDataArray() {}

	  public final native JsArray<ClassData> getClasses() /*-{return this.classes;}-*/;
	  public final native JsArray<PropertyData> getProperties() /*-{return this.properties;}-*/;
}

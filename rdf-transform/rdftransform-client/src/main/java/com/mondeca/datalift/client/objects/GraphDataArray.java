package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class GraphDataArray extends JavaScriptObject {
	
	  protected GraphDataArray() {}

	  public final native JsArray<GraphData> getEntries() /*-{return this.graphs;}-*/;

}

package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class InstanceDataArray extends JavaScriptObject {
	
	  protected InstanceDataArray() {}

	  public final native JsArray<InstanceData> getEntries() /*-{return this.instances;}-*/;

}

package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;

public class InstanceData  extends JavaScriptObject  {

	// Overlay types always have protected, zero argument constructors.
		protected InstanceData() {}   

		// JSNI methods to get stock data.
		public final native String getInstanceName() /*-{ return this.instanceName; }-*/;
		
}

package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;

public class GraphData  extends JavaScriptObject  {

	// Overlay types always have protected, zero argument constructors.
		protected GraphData() {}   

		// JSNI methods to get stock data.
		public final native String getGraphURI() /*-{ return this.graphURI; }-*/;
		public final native String getGraphName() /*-{ return this.graphName; }-*/;
		
}

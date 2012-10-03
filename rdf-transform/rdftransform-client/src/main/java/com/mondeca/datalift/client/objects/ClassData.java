package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;

public class ClassData  extends JavaScriptObject  {

	// Overlay types always have protected, zero argument constructors.
		protected ClassData() {}   

		// JSNI methods to get stock data.
		public final native String getClassURI() /*-{ return this.classURI; }-*/;
		public final native String getClassName() /*-{ return this.className; }-*/;
		public final native String getClassParentURI() /*-{ return this.classParentURI; }-*/;
		
}

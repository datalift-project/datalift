package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;

public class PropertyData  extends JavaScriptObject  {

	// Overlay types always have protected, zero argument constructors.
		protected PropertyData() {}   

		// JSNI methods to get stock data.
		public final native String getPropertyURI() /*-{ return this.propertyURI; }-*/;
		public final native String getPropertyName() /*-{ return this.propertyName; }-*/;
		public final native String getPropertyParentURI() /*-{ return this.propertyParentURI; }-*/;
		public final native String getPropertyType() /*-{ return this.propertyType; }-*/;
		
}

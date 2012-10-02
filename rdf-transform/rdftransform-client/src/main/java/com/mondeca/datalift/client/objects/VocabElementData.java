package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;

public class VocabElementData  extends JavaScriptObject  {

	// Overlay types always have protected, zero argument constructors.
		protected VocabElementData() {}   

		// JSNI methods to get stock data.
		public final native String getVocabElementURI() /*-{ return this.vocabElementURI; }-*/;
		public final native String getVocabElementName() /*-{ return this.vocabElementName; }-*/;
		public final native String getVocabElementType() /*-{ return this.vocabElementType; }-*/;
		
		public final native String getVocabElementVocabURI() /*-{ return this.vocabElementVocabURI; }-*/;
		
		public final native String getVocabElementOverallMetric() /*-{ return this.vocabElementOverallMetric; }-*/;
		
}

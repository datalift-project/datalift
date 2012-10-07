package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;

public class VocabularyData  extends JavaScriptObject  {

	// Overlay types always have protected, zero argument constructors.
		protected VocabularyData() {}   

		// JSNI methods to get stock data.
		public final native String getVocabURI() /*-{ return this.vocabURI; }-*/;
		public final native String getVocabName() /*-{ return this.vocabName; }-*/;
		public final native String getVocabNamespace() /*-{ return this.vocabNsp; }-*/;
		public final native String getVocabPrefix() /*-{ return this.vocabPrefix; }-*/;
}

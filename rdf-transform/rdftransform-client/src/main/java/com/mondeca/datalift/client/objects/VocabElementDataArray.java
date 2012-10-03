package com.mondeca.datalift.client.objects;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class VocabElementDataArray extends JavaScriptObject {
	
	  protected VocabElementDataArray() {}

	  public final native JsArray<VocabElementData> getElements() /*-{return this.vocabElements;}-*/;
	  public final native JsArray<VocabularyData> getVocabularies() /*-{return this.vocabs;}-*/;

}

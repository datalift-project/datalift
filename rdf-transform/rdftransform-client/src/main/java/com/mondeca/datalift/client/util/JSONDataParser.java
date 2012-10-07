package com.mondeca.datalift.client.util;

import com.google.gwt.core.client.JsArray;
import com.mondeca.datalift.client.objects.GraphDataArray;
import com.mondeca.datalift.client.objects.InstanceDataArray;
import com.mondeca.datalift.client.objects.OntologyDataArray;
import com.mondeca.datalift.client.objects.VocabElementDataArray;

public class JSONDataParser {

	
	public static final native GraphDataArray asArrayOfGraphDataArray(String json) /*-{
	   return eval(	"("+ json+ ")");
	 }-*/;
	
	public static final native OntologyDataArray asArrayOfClassDataArray(String json) /*-{
	   return eval(	"("+ json+ ")");
	 }-*/;
	
	public static final native JsArray<InstanceDataArray> asArrayOfInstanceDataArray(String json) /*-{
	   return eval(	"("+ json+ ")");
	 }-*/;
	
	public static final native JsArray<VocabElementDataArray> asArrayOfVocabElementDataArray(String json) /*-{
	   return eval(	"("+ json+ ")");
	 }-*/;
	
//	private void printClassesList(JsArray<RDFClassData> data) {
//		for(int i=0; i < data.length(); i++) {
//			Window.alert(data.get(i).getType()+" '"+data.get(i).getTypeLabel()+"'");
//		}		
//	}
}

package com.mondeca.datalift.client.objects;

import java.util.ArrayList;
import java.util.List;

public class VocabElement {
	private String vocabElementURI=null;
	private String vocabElementURIPrefixed=null;
	private String vocabElementName=null;
	private String vocabElementType=null;
	
	private String vocabElementVocabURI=null;
	
	private String vocabElementOverallMetric=null;
	
	
	public String getVocabElementURIPrefixed() {
		return vocabElementURIPrefixed;
	}

	public void setVocabElementURIPrefixed(String vocabElementURIPrefixed) {
		this.vocabElementURIPrefixed = vocabElementURIPrefixed;
	}

	public String getVocabElementURI() {
		return vocabElementURI;
	}

	public void setVocabElementURI(String vocabElementURI) {
		this.vocabElementURI = vocabElementURI;
	}

	public String getVocabElementName() {
		return vocabElementName;
	}

	public void setVocabElementName(String vocabElementName) {
		this.vocabElementName = vocabElementName;
	}

	public String getVocabElementType() {
		return vocabElementType;
	}

	public void setVocabElementType(String vocabElementType) {
		this.vocabElementType = vocabElementType;
	}

	public String getVocabElementVocabURI() {
		return vocabElementVocabURI;
	}

	public void setVocabElementVocabURI(String vocabElementVocabURI) {
		this.vocabElementVocabURI = vocabElementVocabURI;
	}

	public String getVocabElementOverallMetric() {
		return vocabElementOverallMetric;
	}

	public void setVocabElementOverallMetric(String vocabElementOverallMetric) {
		this.vocabElementOverallMetric = vocabElementOverallMetric;
	}
	
	public static List<VocabElement> getListFormDataArray(VocabElementDataArray array){
		List<VocabElement> list = new ArrayList<VocabElement>();
	
		for (int i = 0; i < array.getElements().length(); i++) {
			final VocabElementData elem = array.getElements().get(i);
			VocabElement element = new VocabElement();
			element.setVocabElementName(elem.getVocabElementName());
			element.setVocabElementOverallMetric(elem.getVocabElementOverallMetric());
			element.setVocabElementType(elem.getVocabElementType());
			element.setVocabElementURI(elem.getVocabElementURI());
			element.setVocabElementVocabURI(elem.getVocabElementVocabURI());
			
			//fetch vocab
			for (int j = 0; j < array.getVocabularies().length(); j++) {
				if(elem.getVocabElementVocabURI().equals(array.getVocabularies().get(j).getVocabURI())){
					element.setVocabElementURIPrefixed(prefixMachine(elem.getVocabElementURI(), array.getVocabularies().get(j)));
					break;
				}
			}
			if(element.getVocabElementURIPrefixed()==null)element.setVocabElementURIPrefixed(element.getVocabElementURI());
			
			list.add(element);
		}
		return list;
	}
	
	
	private static String prefixMachine(String elementURI,VocabularyData vocab ){
		elementURI=elementURI.replace(vocab.getVocabNamespace(), vocab.getVocabPrefix()+":");
		return elementURI;
	}
}

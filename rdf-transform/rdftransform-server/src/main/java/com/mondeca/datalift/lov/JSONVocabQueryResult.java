package com.mondeca.datalift.lov;

import java.util.List;

public class JSONVocabQueryResult {

	protected List<JSONVocabElement> vocabElements;
	protected List<JSONVocab> vocabs;

	public JSONVocabQueryResult(List<JSONVocabElement> vocabElements, List<JSONVocab> vocabs) {
		super();
		this.vocabElements = vocabElements;
		this.vocabs = vocabs;
	}
	public List<JSONVocabElement> getVocabElements() {
		return vocabElements;
	}
	public void setVocabElements(List<JSONVocabElement> vocabElements) {
		this.vocabElements = vocabElements;
	}
	public List<JSONVocab> getVocabs() {
		return vocabs;
	}
	public void setVocabs(List<JSONVocab> vocabs) {
		this.vocabs = vocabs;
	}
	
	
}

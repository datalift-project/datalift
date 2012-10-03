package com.mondeca.datalift.lov;

public class JSONVocabElement {

	protected String vocabElementURI;
	protected String vocabElementName;
	protected String vocabElementType;
	protected String vocabElementVocabURI;
	protected String vocabElementScore;

	public JSONVocabElement(String vocabElementURI) {
		super();
		this.vocabElementURI = vocabElementURI;
	}

	public String getVocabElementURI() {
		return vocabElementURI;
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

	public String getVocabElementScore() {
		return vocabElementScore;
	}

	public void setVocabElementScore(String vocabElementScore) {
		this.vocabElementScore = vocabElementScore;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((vocabElementURI == null) ? 0 : vocabElementURI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JSONVocabElement other = (JSONVocabElement) obj;
		if (vocabElementURI == null) {
			if (other.vocabElementURI != null)
				return false;
		} else if (!vocabElementURI.equals(other.vocabElementURI))
			return false;
		return true;
	}
	
	
	
}

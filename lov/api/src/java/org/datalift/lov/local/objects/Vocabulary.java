package org.datalift.lov.local.objects;

public class Vocabulary {
	private String uri = null;
	private String namespace = null;
	private String prefix = null;
	private String vocabSpace = null;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getNamespace() {
		return namespace;
	}

	public void setNamespace(String namespace) {
		this.namespace = namespace;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getVocabSpace() {
		return vocabSpace;
	}

	public void setVocabSpace(String vocabSpace) {
		this.vocabSpace = vocabSpace;
	}
}

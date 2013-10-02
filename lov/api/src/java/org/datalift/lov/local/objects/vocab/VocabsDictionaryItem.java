package org.datalift.lov.local.objects.vocab;

import java.util.ArrayList;
import java.util.List;

import org.datalift.lov.local.objects.Literal;

/**
 * This class represent an item in a vocabulary dictionary with the very basic
 * identification of a vocabulary
 * 
 * @author Pierre-Yves Vandenbussche
 * 
 */
public class VocabsDictionaryItem {
	private String uri = null;
	private String namespace = null;
	private String prefix = null;
	List<Literal> titles = new ArrayList<Literal>();
	List<Literal> descriptions = new ArrayList<Literal>();
	VocabularyVersion lastVersionReviewed = null;

	public VocabsDictionaryItem(String uri, String namespace, String prefix) {
		this.uri = uri;
		this.namespace = namespace;
		this.prefix = prefix;
	}

	public boolean equals(Object o) {
		if (o instanceof VocabsDictionaryItem) {
			if (((VocabsDictionaryItem) o).getUri().equals(this.getUri()))
				return true;
		}
		return false;
	}

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

	public List<Literal> getTitles() {
		return titles;
	}

	public void setTitles(List<Literal> titles) {
		this.titles = titles;
	}

	public List<Literal> getDescriptions() {
		return descriptions;
	}

	public void setDescriptions(List<Literal> descriptions) {
		this.descriptions = descriptions;
	}

	public VocabularyVersion getLastVersionReviewed() {
		return lastVersionReviewed;
	}

	public void setLastVersionReviewed(VocabularyVersion lastVersionReviewed) {
		this.lastVersionReviewed = lastVersionReviewed;
	}
}

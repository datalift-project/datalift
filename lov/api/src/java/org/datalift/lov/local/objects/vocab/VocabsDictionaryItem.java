package org.datalift.lov.local.objects.vocab;

import java.util.ArrayList;
import java.util.List;

import org.datalift.lov.local.LovUtil;
import org.datalift.lov.local.objects.JSONSerializable;
import org.datalift.lov.local.objects.Literal;

/**
 * This class represent an item in a vocabulary dictionary with the very basic
 * identification of a vocabulary
 * 
 * @author Pierre-Yves Vandenbussche
 * 
 */
public class VocabsDictionaryItem implements JSONSerializable {
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

	@Override
	public boolean equals(Object o) {
		if (o instanceof VocabsDictionaryItem) {
			if (((VocabsDictionaryItem) o).getUri().equals(this.getUri()))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
	    return (this.getUri() != null)? this.getUri().hashCode(): 0;
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
	
	@Override
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		// String properties
		jsonResult.append("\"uri\": " + LovUtil.toJSON(uri) + ",");
		jsonResult.append("\"namespace\": " + LovUtil.toJSON(namespace) + ",");
		jsonResult.append("\"prefix\": " + LovUtil.toJSON(prefix) + ",");
		
		// List properties
		// titles
		jsonResult.append("\"titles\": " + LovUtil.toJSON(titles));
		
		// descriptions
		jsonResult.append("\"descriptions\": " + LovUtil.toJSON(descriptions));
		
		
		// Vocab version
		jsonResult.append("\"lastVersionReviewed\": " + LovUtil.toJSON(lastVersionReviewed));
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}
}

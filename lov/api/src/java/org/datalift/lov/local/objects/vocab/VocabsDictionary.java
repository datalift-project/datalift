package org.datalift.lov.local.objects.vocab;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.datalift.lov.local.LovUtil;

/**
 * 
 * This class represents a dictionary (list) of vocabulary with very basics
 * identification + complementary information and some method to access them
 * easily
 * 
 * @author Pierre-Yves Vandenbussche
 * 
 */
public class VocabsDictionary {
	private List<VocabsDictionaryItem> vocabularies = new ArrayList<VocabsDictionaryItem>();

	public List<VocabsDictionaryItem> getVocabularies() {
		return vocabularies;
	}

	public void setVocabularies(List<VocabsDictionaryItem> vocabularies) {
		this.vocabularies = vocabularies;
	}

	public void add(VocabsDictionaryItem vocabularyDictionaryItem) {
		this.vocabularies.add(vocabularyDictionaryItem);
	}

	public VocabsDictionaryItem getVocabularyWithNamespace(String namespace) {
		for (Iterator<VocabsDictionaryItem> iterator = vocabularies.iterator(); iterator
				.hasNext();) {
			VocabsDictionaryItem voc = iterator.next();
			if (voc.getNamespace().equals(namespace))
				return voc;
		}
		return null;
	}

	public VocabsDictionaryItem getVocabularyWithURI(String uri) {
		for (Iterator<VocabsDictionaryItem> iterator = vocabularies.iterator(); iterator
				.hasNext();) {
			VocabsDictionaryItem voc = iterator.next();
			if (voc.getUri().equals(uri))
				return voc;
		}
		return null;
	}

	public VocabsDictionaryItem getVocabularyWithPrefix(String prefix) {
		for (Iterator<VocabsDictionaryItem> iterator = vocabularies.iterator(); iterator
				.hasNext();) {
			VocabsDictionaryItem voc = iterator.next();
			if (voc.getPrefix().equals(prefix))
				return voc;
		}
		return null;
	}
	
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		// List properties
		// vocabularies
		jsonResult.append("\"vocabularies\": " + LovUtil.toJSON(vocabularies, true));

		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}

}

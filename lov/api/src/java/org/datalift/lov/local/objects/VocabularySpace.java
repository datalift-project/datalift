package org.datalift.lov.local.objects;

import org.datalift.lov.local.LovUtil;

public class VocabularySpace implements JSONSerializable, Comparable<VocabularySpace> {

	private String title;
	private String shortTitle;
	
	public VocabularySpace() {
		
	}
	
	public VocabularySpace(String title, String shortTitle) {
		this.setTitle(title);
		this.setShortTitle(shortTitle);
	}
	
	@Override
	public String toJSON() {
		return new StringBuilder()
		
		// beginning of json
		.append("{")
		
		// String properties
		.append("\"title\": " + LovUtil.toJSON(title) + ",")
		.append("\"shortTitle\": " + LovUtil.toJSON(shortTitle))
		
		// end of json
		.append("}")
		
		.toString();
	}

	/**
	 * @return the title
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * @param title the title to set
	 */
	public void setTitle(String title) {
		this.title = title;
	}

	/**
	 * @return the shortTitle
	 */
	public String getShortTitle() {
		return shortTitle;
	}

	/**
	 * @param shortTitle the shortTitle to set
	 */
	public void setShortTitle(String shortTitle) {
		this.shortTitle = shortTitle;
	}

	@Override
	public int compareTo(VocabularySpace otherVocSpace) {
		return this.title.compareToIgnoreCase(otherVocSpace.title);
	}

}

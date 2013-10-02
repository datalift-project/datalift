package org.datalift.lov.local.objects.vocab;

public class VocabularyVersion {
	String date = null;
	String versionDecimal = null;
	String label = null;
	String link = null;

	public VocabularyVersion(String date, String label) {
		this.date = date;
		this.label = label;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public String getVersionDecimal() {
		return versionDecimal;
	}

	public void setVersionDecimal(String versionDecimal) {
		this.versionDecimal = versionDecimal;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLink() {
		return link;
	}

	public void setLink(String link) {
		this.link = link;
	}

}
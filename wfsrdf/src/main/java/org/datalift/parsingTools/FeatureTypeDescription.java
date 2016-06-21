package org.datalift.parsingTools;

public class FeatureTypeDescription {
	
	private String name;
	private String title;
	private int numberFeature;
	private String summary;
	private String epsgSrs;
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public int getNumberFeature() {
		return numberFeature;
	}
	public void setNumberFeature(int numberFeature) {
		this.numberFeature = numberFeature;
	}
	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public String getEpsgSrs() {
		return epsgSrs;
	}
	public void setEpsgSrs(String epsgSrs) {
		this.epsgSrs = epsgSrs;
	}
	

}

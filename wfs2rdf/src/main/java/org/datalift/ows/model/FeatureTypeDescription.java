package org.datalift.ows.model;

import java.util.ArrayList;
import java.util.List;
/**
 * The class containing the description  of a feature type (according to the WFS definition) 
 * @author Hanane Eljabiri
 *
 */
public class FeatureTypeDescription {
	
	private String name;
	private String title;
	private int numberFeature;
	private String summary;
	private String defaultSrs;
	private List<String> otherSrs;
	public FeatureTypeDescription()
	{
		setOtherSrs(new ArrayList<String>());
	}
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
	public String getDefaultSrs() {
		return defaultSrs;
	}
	public void setDefaultSrs(String epsgSrs) {
		this.defaultSrs = epsgSrs;
	}
	public List<String> getOtherSrs() {
		return otherSrs;
	}
	public void setOtherSrs(List<String> otherSrs) {
		this.otherSrs = otherSrs;
	}
}

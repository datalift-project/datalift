package org.datalift.lov;

public class OntologyDesc {

	/*
	 * The data model for an ontology
	 */
	public OntologyDesc() {

	}

	public OntologyDesc(String prefix, String url, String name, String desc) {
		this.prefix = prefix;
		this.url = url;
		this.name = name;
		this.description = desc;
	}

	public String getPrefix() {
		return prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	private String prefix;
	private String url;
	private String name;
	private String description;

}

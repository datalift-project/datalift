package com.mondeca.datalift.rdf2rdf.model;

public class JSONProperty {

	protected String propertyURI;
	// default to empty string
	protected String propertyName = "";
	// default to empty string	
	protected String propertyType = "";
	// default to empty string
	protected String propertyParentURI = "";
	
	public JSONProperty(String URI) {
		super();
		this.propertyURI = URI;
	}

	public String getPropertyURI() {
		return propertyURI;
	}

	public void setPropertyURI(String pURI) {
		this.propertyURI = pURI;
	}

	public String getPropertyName() {
		return propertyName;
	}

	public void setPropertyName(String pName) {
		this.propertyName = pName;
	}

	public String getPropertyType() {
		return propertyType;
	}

	public void setPropertyType(String propertyType) {
		this.propertyType = propertyType;
	}

	public String getPropertyParentURI() {
		return propertyParentURI;
	}

	public void setPropertyParentURI(String propertyParentURI) {
		this.propertyParentURI = propertyParentURI;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((propertyURI == null) ? 0 : propertyURI.hashCode());
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
		JSONProperty other = (JSONProperty) obj;
		if (propertyURI == null) {
			if (other.propertyURI != null)
				return false;
		} else if (!propertyURI.equals(other.propertyURI))
			return false;
		return true;
	}
}

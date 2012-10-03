package com.mondeca.datalift.rdf2rdf.model;

public class JSONClass {

	protected String classURI;
	// default to empty string
	protected String className = "";
	// default to empty string
	protected String classParentURI = "";
	
	public JSONClass(String URI) {
		super();
		this.classURI = URI;
	}

	public String getClassURI() {
		return classURI;
	}

	public void setClassURI(String classURI) {
		this.classURI = classURI;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public String getClassParentURI() {
		return classParentURI;
	}

	public void setClassParentURI(String classParentURI) {
		this.classParentURI = classParentURI;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((classURI == null) ? 0 : classURI.hashCode());
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
		JSONClass other = (JSONClass) obj;
		if (classURI == null) {
			if (other.classURI != null)
				return false;
		} else if (!classURI.equals(other.classURI))
			return false;
		return true;
	}
	
}

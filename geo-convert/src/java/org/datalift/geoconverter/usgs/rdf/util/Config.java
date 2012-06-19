/**
 * @file Config.java
 * @author Andrew Bulen
 * @brief stores the configuration used to convert GML attribute to RDF
 */

package org.datalift.geoconverter.usgs.rdf.util;

/**
 * Configuration for converting an Attribute to RDF.
 * stores the parent and predicate used to store an attribute
 * in RDF format as well as whether or not the attribute represents
 * a literal value or a resource and the namespace of the resource
 * that it represents.
 * @author Andrew Bulen
 */
public class Config {
	/** true if the attribute is a resource, false for literal. */
	private boolean m_isResource;
	/** URI of the attribute. */
	private String m_namespace;
	/** parent of the attribute if it is a resource. */
	private String m_parent;
	/** predicate used for accessing the object. */
	private String m_predicate;

	/**
	 * default constructor
	 * initializes to literal value stored at the CEGIS default
	 * RDF namespace.
	 */
	public Config() {
		this.m_namespace = " ";
		this.m_predicate = "http://cegis.usgs.gov/rdf#";
		this.m_parent = " ";
		this.m_isResource = false;
	}
	/**
	 * initialization constructor.
	 * @param isResource true if attribute is resource
	 * @param namespace URI of the object
	 * @param parent parent of the attribute
	 * @param predicate accessing predicate for RDF
	 */
	public Config(final boolean isResource, final String namespace,
			final String parent, final String predicate) {
		this.m_namespace = namespace;
		this.m_predicate = predicate;
		this.m_parent = parent;
		this.m_isResource = isResource;
	}
	/**
	 * retrieves the URI of the attribute.
	 * @return attribute URI
	 */
	public final String getNamespace() { return this.m_namespace; }
	/**
	 * retrieves the parent of the attribute.
	 * @return attribute parent
	 */
	public final String getParent() { return this.m_parent; }
	/**
	 * retrieves the predicate of the attribute.
	 * @return attribute predicate
	 */
	public final String getPredicate() { return this.m_predicate; }
	/**
	 * retrieves whether the attribute is a resource or literal value.
	 * @return true if resource, false if literal
	 */
	public final boolean getIsResource() { return this.m_isResource; }
	/**
	 * sets the attribute's URI.
	 * @param namespace URI representing the feature
	 */
	public final void setNamespace(final String namespace) {
		this.m_namespace = namespace;
	}
	/**
	 * sets the attribute's parent.
	 * @param parent resource to which the property belongs
	 */
	public final void setParent(final String parent) {
		this.m_parent = parent;
	}
	/**
	 * sets the attribute's predicate.
	 * @param predicate URI of the predicate
	 */
	public final void setPredicate(final String predicate) {
		this.m_predicate = predicate;
	}
	/**
	 * sets whether the attribute is a resource or literal.
	 * @param isResource true if the property represents a resource
	 */
	public final void setIsResource(final boolean isResource) {
		this.m_isResource = isResource;
	}
	/**
	 * equals operator.
	 * @param c input configuration to compare
	 * @return true if configurations are equal
	 */
	public final boolean equals(final Config c) {
		if (!c.m_isResource && this.m_isResource) {
			return false;
		} else if (!c.m_namespace.equals(this.m_namespace)) {
			return false;
		} else if (!c.m_parent.equals(this.m_parent)) {
			return false;
		} else if (!c.m_predicate.equals(this.m_predicate)) {
			return false;
		}
		return true;
	}
}

package org.datalift.core.project;


import java.util.Collection;
import java.util.LinkedList;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import com.clarkparsia.empire.annotation.Namespaces;
import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;


/**
 * An implementation of {@link Project} that relies on Empire RDF JPA
 * provider for persistence.
 * <p>
 * Note: Empire namespace handling sucks as it uses namespace prefixes
 * as key in a global namespace table (RdfNamespace) rather than using
 * the namespace URI as key and consider prefixes as a local matter
 * (local to each query and class).</p>
 * <p>
 * So, <strong>be warned</strong>, this classes relies on the following
 * <i>global</i> prefix mappings be installed:</p>
 * <dl>
 *  <dt>datalift</dt><dd>http://www.datalift.org/core#</dd>
 *  <dt>dc</dt><dd>http://purl.org/dc/elements/1.1/</dd>
 * </dl>
 *
 * @author hdevos
 */
@Entity
@Namespaces({"rdfs", "http://rdfs.org/ns/void#"})
@RdfsClass("rdfs:Dataset")
public class ProjectImpl extends BaseRdfEntity implements Project
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfId
    private String uri;
    @RdfProperty("dc:title")
    private String title;
    @RdfProperty("dc:creator")
    private String owner;
    @RdfProperty("dc:description")
    private String description;

    @RdfProperty("datalift:source")
	@OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
	private Collection<Source> sources = new LinkedList<Source>();
    
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public ProjectImpl() {
        // NOP
    }

    public ProjectImpl(String uri) {
        this.uri = uri;
    }

    //-------------------------------------------------------------------------
    // Project contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getUri() {
        return this.uri;
    }

    /** {@inheritDoc} */
    @Override
    public String getTitle(){
        return title;
    }
    /** {@inheritDoc} */
    @Override
    public void setTitle(String t) {
        title = t;
    }

    /** {@inheritDoc} */
    @Override
    public String getOwner() {
        return owner;
    }
    /** {@inheritDoc} */
    @Override
    public void setOwner(String o) {
        owner = o;
    }

    /** {@inheritDoc} */
    @Override
    public String getDescription() {
        return description;
    }
    /** {@inheritDoc} */
    @Override
    public void setDescription(String d) {
        description = d;
    }

	@Override
	public void addSource(Source s) {
		sources.add(s);		
	}

	@Override
	public Collection<Source> getSources() {
		return sources;
	}

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

//    public final static String newId(URI baseUri) {
//        try {
//            URL u = new URL(baseUri.toURL(), "project/" + UUID.randomUUID());
//            return u.toString();
//        }
//        catch (Exception e) {
//            throw new RuntimeException("Invalid base URI: " + baseUri); 
//        }
//    }
}

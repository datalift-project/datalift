package org.datalift.core.project;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import com.clarkparsia.empire.annotation.RdfId;
import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;

import static org.datalift.fwk.rdf.RdfNamespace.VDPP;

/**
 * An implementation of {@link Project} that relies on Empire RDF JPA provider
 * for persistence.
 * <p>
 * Note: Empire namespace handling sucks as it uses namespace prefixes as key in
 * a global namespace table (RdfNamespace) rather than using the namespace URI
 * as key and consider prefixes as a local matter (local to each query and
 * class).
 * </p>
 * <p>
 * So, <strong>be warned</strong>, this classes relies on the following
 * <i>global</i> prefix mappings be installed:
 * </p>
 * <dl>
 * <dt>datalift</dt>
 * <dd>http://www.datalift.org/core#</dd>
 * <dt>dc</dt>
 * <dd>http://purl.org/dc/elements/1.1/</dd>
 * </dl>
 * 
 * @author hdevos
 */
@Entity
@RdfsClass("vdpp:Project")
public class ProjectImpl extends BaseRdfEntity implements Project {
	// -------------------------------------------------------------------------
	// Instance members
	// -------------------------------------------------------------------------

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

	@RdfProperty("dcterms:issued")
	private Date dateCreated;
	@RdfProperty("dcterms:modified")
	private Date dateModified;
	@RdfProperty("dcterms:license")
	private URI license;
	@RdfProperty("prv:Execution")
	private URI execution;

	@RdfProperty("void:vocabulary")
	@OneToMany(fetch = FetchType.EAGER, cascade = { CascadeType.ALL })
	private Collection<Ontology> ontologies = new LinkedList<Ontology>();

	// -------------------------------------------------------------------------
	// Constructors
	// -------------------------------------------------------------------------

	public ProjectImpl() {
		// NOP
	}

	public ProjectImpl(String uri) {
		this.uri = uri;
	}

	// -------------------------------------------------------------------------
	// Project contract support
	// -------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public String getUri() {
		return this.uri;
	}

	/** {@inheritDoc} */
	@Override
	public String getTitle() {
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

	@Override
	public Source getSource(URI uri) {
		for (Source source : this.sources) {
			if (source.getUri().toString().equals(uri.toString())) {
				return source;
			}
		}
		return null;
	}

	@Override
	public void deleteSource(URI uri) {
		Source source = getSource(uri);
		if(source != null) {
			this.sources.remove(source);
		}
	}

	@Override
	public Date getDateCreation() {
		return dateCreated;
	}

	@Override
	public void setDateCreation(Date date) {
		dateCreated = date;
	}

	@Override
	public Date getDateModification() {
		return dateModified;
	}

	@Override
	public void setDateModification(Date date) {
		this.dateModified = date;
	}

	@Override
	public URI getLicense() {
		return license;
	}

	@Override
	public void setLicense(URI license) {
		this.license = license;
	}

	@Override
	public void addOntology(Ontology src) {
		ontologies.add(src);
	}

	@Override
	public Collection<Ontology> getOntologies() {
		return ontologies;
	}

	@Override
	public Ontology getOntology(String title) {
		for (Ontology ontology : this.ontologies) {
			if (ontology.getTitle().equals(title)) {
				return ontology;
			}
		}
		return null;
	}

	@Override
	public void deleteOntology(String title) {
		Ontology ontology = getOntology(title);
		if(ontology != null) {
			this.ontologies.remove(ontology);
		}
	}

	@Override
	public URI getExecution() {
		return this.execution;
	}

	@Override
	public void setExecution(URI execution) {
		this.execution = execution;
	}

	// -------------------------------------------------------------------------
	// BaseRdfEntity contract support
	// -------------------------------------------------------------------------

	protected void setId(String id) {
		this.uri = id;
	}

	public enum Execution {
		Selection(VDPP.uri + "Selection"), Publication(VDPP.uri + "Publication"), Interlinking(
				VDPP.uri + "Interlinking"), Convertion(VDPP.uri + "Convertion");

		public final URI uri;

		Execution(String s) {
			try {
				this.uri = new URI(s);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}
	}
}

/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.geoconverter;


import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.ResponseBuilder;

import static javax.ws.rs.core.Response.Status.*;

import com.sun.jersey.api.view.Viewable;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.ShpSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;

import static org.datalift.fwk.MediaTypes.*;


/**
 * A common superclass for converters, providing some utility methods.
 * 
 * @author lbihanic
 */
public abstract class BaseConverterModule
extends BaseModule implements ProjectModule
{
	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------

	/** Base name of the resource bundle for converter GUI. */
	protected final static String GUI_RESOURCES_BUNDLE = "resources";

	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger();

	//-------------------------------------------------------------------------
	// Instance members
	//-------------------------------------------------------------------------

	/** The type of input source this module can handle. */
	protected final Collection<SourceType> inputSources;
	/** The requested module position in menu. */
	protected final int position;
	/** The HTTP method to access the module entry page. */
	protected final HttpMethod accessMethod;

	/** The DataLift project manager. */
	protected ProjectManager projectManager = null;
	/** The DataLift SPARQL endpoint. */
	protected SparqlEndpoint sparqlEndpoint = null;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/**
	 * Creates a new module instance, accepting the specified type of
	 * input source, with an entry page accessed using HTTP GET.
	 * @param  name           the module name.
	 * @param  position       the requested module position in menu.
	 * @param  inputSources   the types of sources this module expects
	 *                        as input.
	 */
	public BaseConverterModule(String name, int position,
			SourceType... inputSources) {
		this(name, position, HttpMethod.GET, inputSources);
	}

	/**
	 * Creates a new module instance.
	 * @param  name           the module name.
	 * @param  position       the requested module position in menu.
	 * @param  method         the HTTP method (GET or POST) to access
	 *                        the module entry page.
	 * @param  inputSources   the types of sources this module expects
	 *                        as input.
	 */
	public BaseConverterModule(String name, int position,
			HttpMethod method, SourceType... inputSources) {
		super(name);

		if (method == null) {
			throw new IllegalArgumentException("method");
		}
		this.accessMethod = method;
		this.position = position;

		if ((inputSources == null) || (inputSources.length == 0)) {
			throw new IllegalArgumentException("inputSource");
		}
		Collection<SourceType> srcTypes = new HashSet<SourceType>();
		srcTypes.addAll(Arrays.asList(inputSources));
		this.inputSources = Collections.unmodifiableCollection(srcTypes);
	}

	//-------------------------------------------------------------------------
	// Module contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public void postInit(Configuration configuration) {
		super.postInit(configuration);

		this.projectManager = configuration.getBean(ProjectManager.class);
		if (this.projectManager == null) {
			throw new TechnicalException("project.manager.not.available");
		}
		this.sparqlEndpoint = configuration.getBean(SparqlEndpoint.class);
		if (this.sparqlEndpoint == null) {
			throw new TechnicalException("sparql.endpoint.not.available");
		}
	}

	//-------------------------------------------------------------------------
	// ProjectModule contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public UriDesc canHandle(Project p) {
		UriDesc projectPage = null;
		try {
			if (this.findSource(p, false) != null) {
				try {
					String label = PreferredLocales.get()
							.getBundle(GUI_RESOURCES_BUNDLE, this)
							.getString(this.getName() + ".module.label");

					projectPage = new UriDesc(
							this.getName() + "?project=" + p.getUri(),
							this.accessMethod, label);
					if (this.position > 0) {
						projectPage.setPosition(this.position);
					}
				}
				catch (Exception e) {
					throw new TechnicalException(e);
				}
			}
		}
		catch (Exception e) {
			log.fatal("Failed to check status of project {}: {}", e,
					p.getUri(), e.getMessage());
		}
		return projectPage;
	}

	//-------------------------------------------------------------------------
	// Object contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public String toString() {
		return this.getClass().getSimpleName()
				+ " (" + this.getName() + this.inputSources + ')';
	}

	//-------------------------------------------------------------------------
	// Specific implementation
	//-------------------------------------------------------------------------

	/**
	 * Returns whether the specified source can be handled by this
	 * converter.
	 * @param  s   the source.
	 *
	 * @return <code>true</code> if this converter supports the source;
	 *         <code>false</code> otherwise.
	 */
	public boolean canHandle(Source s) {
		if (s == null) {
			throw new IllegalArgumentException("s");
		}
		return (this.inputSources.contains(s.getType()));
	}

	/**
	 * Retrieves a {@link Project} using its URI.
	 * @param  projectId   the project URI.
	 *
	 * @return the project.
	 * @throws ObjectNotFoundException if the project does not exist.
	 */
	protected final Project getProject(URI projectId)
			throws ObjectNotFoundException {
		Project p = this.projectManager.findProject(projectId);
		if (p == null) {
			throw new ObjectNotFoundException("project.not.found", projectId);
		}
		return p;
	}

	/**
	 * Searches the specified project for a source matching the expected
	 * input source type of the module.
	 * @param  p          the project.
	 * @param  findLast   whether to return the last matching source or
	 *                    the first.
	 * @return a matching source of <code>null</code> if no matching
	 *         source was found.
	 */
	protected final Source findSource(Project p, boolean findLast) {
		if (p == null) {
			throw new IllegalArgumentException("p");
		}
		Source src = null;
		for (Source s : p.getSources()) {
			if (this.canHandle(s)) {
				src = s;
				if (! findLast) break;
				// Else: continue to get last source of type in project...
			}
		}
		return src;
	}

	/**
	 * Creates a new transformed RDF source and attach it to the
	 * specified project.
	 * @param  p        the owning project.
	 * @param  parent   the parent source object.
	 * @param  name     the new source name.
	 * @param  uri      the new source URI.
	 *
	 * @return the newly created transformed RDF source.
	 * @throws IOException if any error occurred creating the source.
	 */
	protected TransformedRdfSource addResultSource(Project p, Source parent,
			String name, URI uri)
					throws IOException {
		TransformedRdfSource newSrc =
				this.projectManager.newTransformedRdfSource(p, uri,
						name, null, uri, parent);
		this.projectManager.saveProject(p);
		return newSrc;
	}

	/**
	 * Creates a new SHP source object.
	 * @param  p		     the owning project.
	 * @param  uri           the source URI.
	 * @param  title         the source label.
	 * @param  description   the description of the source content or
	 *                       intent.
	 * @param  filePath      the SHP file path in the public storage.
	 * @return a new SHP source, associated to the specified project.
	 * @throws IOException if any error occurred creating the source
	 *         or accessing the specified file.
	 */
	public ShpSource newShpSource(Project p, URI uri, String title,
			String description, String filePath)
					throws IOException {
		ShpSource newSrc =
				this.projectManager.newShpSource(p, uri,
						title, null, filePath);
		this.projectManager.saveProject(p);
		return newSrc;
	}

	/**
	 * Creates a new GML source object.
	 * @param  project       the owning project.
	 * @param  uri           the source URI.
	 * @param  title         the source label.
	 * @param  description   the description of the source content or
	 *                       intent.
	 * @param  filePath      the SHP file path in the public storage.
	 * @return a new SHP source, associated to the specified project.
	 * @throws IOException if any error occurred creating the source
	 *         or accessing the specified file.
	 */
	public GmlSource newGmlSource(Project p, URI uri, String title,
			String description, String filePath)
					throws IOException {
		GmlSource newSrc =
				this.projectManager.newGmlSource(p, uri,
						title, null, filePath);
		this.projectManager.saveProject(p);
		return newSrc;
	}

	/**
	 * Return a viewable for the specified template, populated with the
	 * specified model object.
	 * <p>
	 * The template name shall be relative to the module, the module
	 * name is automatically prepended.</p>
	 * @param  templateName   the relative template name.
	 * @param  it             the model object to pass on to the view.
	 *
	 * @return a populated viewable.
	 */
	protected final Viewable newViewable(String templateName, Object it) {
		return new Viewable("/" + this.getName() + templateName, it);
	}

	/**
	 * Notifies the user of successful source creation, redirecting
	 * HTML clients (i.e. browsers) to the source tab of the project
	 * main page.
	 * @param  src   the source the creation of which shall
	 *               be reported.
	 *
	 * @return an HTTP response redirecting to the project main page.
	 * @throws TechnicalException if any error occurred.
	 */
	protected final ResponseBuilder created(Source src) {
		try {
			String targetUrl = src.getProject().getUri() + "#source";
			return Response.created(new URI(src.getUri()))
					.entity(this.newViewable("/redirect.vm", targetUrl))
					.type(TEXT_HTML);
		}
		catch (Exception e) {
			throw new TechnicalException(e);
		}
	}

	/**
	 * Displays the content of the specified named graph.
	 * @param repository   the RDF store to query.
	 * @param namedGraph   the named graph the content of which shall
	 *                     be displayed.
	 * @param uriInfo      the requested URI.
	 * @param request      the HTTP request.
	 * @param acceptHdr    the HTTP Accept header string.
	 *
	 * @return A {@link Response JAX-RS Response object} containing a
	 *         representation of the named graph triples acceptable for
	 *         the client (content negotiation) or an error response.
	 */
	protected Response displayGraph(Repository repository,
			URI namedGraph, UriInfo uriInfo,
			Request request, String acceptHdr) {
		List<String> defGraphs = null;
		if (repository != null) {
			defGraphs = new LinkedList<String>();
			defGraphs.add(repository.getName());
		}
		return this.sparqlEndpoint.executeQuery(defGraphs, null,
				"SELECT * WHERE { GRAPH <"
						+ namedGraph + "> { ?s ?p ?o . } }",
						uriInfo, request, acceptHdr).build();
	}

	protected void throwInvalidParamError(String name, Object value) {
		TechnicalException error = (value != null)?
				new TechnicalException("ws.invalid.param.error", name, value):
					new TechnicalException("ws.missing.param", name);
				this.sendError(BAD_REQUEST, error.getMessage());
	}

	protected void handleInternalError(Exception e)
			throws WebApplicationException {
		TechnicalException error = null;
		if (e instanceof WebApplicationException) {
			throw (WebApplicationException)e;
		}
		else if (e instanceof ObjectNotFoundException) {
			this.sendError(NOT_FOUND, e.getMessage());
		}
		else if (e instanceof TechnicalException) {
			error = (TechnicalException)e;
		}
		else {
			error = new TechnicalException(
					"ws.internal.error", e, e.getMessage());
		}
		log.fatal(e.getMessage(), e);
		this.sendError(INTERNAL_SERVER_ERROR, error.getMessage());
	}
}

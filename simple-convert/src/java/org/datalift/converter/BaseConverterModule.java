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

package org.datalift.converter;


import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import com.sun.jersey.api.NotFoundException;
import com.sun.jersey.api.view.Viewable;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.sparql.SparqlEndpoint;
import org.datalift.fwk.util.StringUtils;


/**
 * A common superclass for converters, providing some utility methods.
 * 
 * @author lbihanic
 */
public abstract class BaseConverterModule
                                extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The type of input source this module can handle. */
    protected final SourceType inputSource;
    /** The HTTP method to access the module entry page. */
    protected final HttpMethod accessMethod;

    /** The DataLift configuration. */
    protected Configuration configuration   = null;
    /** The DataLift project manager. */
    protected ProjectManager projectManager = null;
    /** The DataLift SPARQL endpoint. */
    protected SparqlEndpoint sparqlEndpoint = null;
    /** The DataLift internal RDF store. */
    protected Repository internalRepository = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new module instance, accepting the specified type of
     * input source, with an entry page accessed using HTTP GET.
     * @param  name          the module name.
     * @param  inputSource   the type of source this module expects
     *                       as input.
     */
    public BaseConverterModule(String name, SourceType inputSource) {
        this(name, inputSource, HttpMethod.GET);
    }

    /**
     * Creates a new module instance.
     * @param  name   the module name.
     * @param  inputSource   the type of source this module expects
     *                       as input.
     * @param  method        the HTTP method (GET or POST) to access
     *                       the module entry page.
     */
    public BaseConverterModule(String name, SourceType inputSource,
                                            HttpMethod method) {
        super(name, true);
        if (inputSource == null) {
            throw new IllegalArgumentException("inputSource");
        }
        if (method == null) {
            throw new IllegalArgumentException("method");
        }
        this.inputSource = inputSource;
        this.accessMethod = method;
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);

        this.configuration = configuration;
        this.internalRepository = configuration.getInternalRepository();

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
        if (this.findSource(p, false) != null) {
            try {
                projectPage = new UriDesc(
                                    this.getName() + "?project=" + p.getUri(),
                                    this.accessMethod,
                                    this.getName() + ".module.label");
            }
            catch (Exception e) {
                throw new TechnicalException(e);
            }
        }
        return projectPage;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

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
            if (s.getType() == this.inputSource) {
                src = s;
                if (! findLast) break;
                // Else: continue to get last source of type in project...
            }
        }
        return src;
    }

    /**
     * Retrieves the latest source matching the expected input source
     * type of the module in the specified project.
     * @param  p   the project.
     *
     * @return the latest source.
     * @throws ObjectNotFoundException if the project does not exist.
     */
    protected final Source getLastSource(Project p) {
        Source src = this.findSource(p, true);
        if (src == null) {
            throw new ObjectNotFoundException("project.source.not.found",
                                              p.getUri(), this.inputSource);
        }
        return src;
    }

    protected final String nextSourceName(Project p) {
        return " (RDF #" + p.getSources().size() + ')';
    }

    protected final URI newGraphUri(Source parent, String name) {
        try {
            return new URI(parent.getUri() + '-' + StringUtils.urlify(name));
        }
        catch (Exception e) {
            throw new TechnicalException(e);
        }
    }

    protected void addResultSource(Project p, Source parent,
                                              String name, URI namedGraph)
                                                            throws IOException {
        p.addSource(this.projectManager.newTransformedRdfSource(
                                        namedGraph, parent.getTitle() + name,
                                        namedGraph, parent));
        this.projectManager.saveProject(p);
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
        throw new WebApplicationException(
                                Response.status(Status.BAD_REQUEST)
                                        .type(MediaTypes.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
    }

    protected void handleInternalError(Exception e)
                                                throws WebApplicationException {
        TechnicalException error = null;
        if (e instanceof WebApplicationException) {
            throw (WebApplicationException)e;
        }
        else if (e instanceof ObjectNotFoundException) {
            throw new NotFoundException(e.getMessage());
        }
        else if (e instanceof TechnicalException) {
            error = (TechnicalException)e;
        }
        else {
            error = new TechnicalException(
                                    "ws.internal.error", e, e.getMessage());
        }
        log.fatal(e.getMessage(), e);
        throw new WebApplicationException(
                            Response.status(Status.INTERNAL_SERVER_ERROR)
                                    .type(MediaTypes.TEXT_PLAIN_TYPE)
                                    .entity(error.getMessage()).build());
    }
}

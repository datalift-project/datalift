/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.converter;


import java.io.File;
import java.net.URI;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.core.Response.Status;

import org.openrdf.model.Literal;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;

import com.sun.jersey.api.NotFoundException;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectManager;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.StringUtils;


public class CsvConverter extends BaseModule implements ProjectModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String MODULE_NAME = "csvconverter";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private ProjectManager projectManager = null;
    private File storage = null;
    private Repository internal = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public CsvConverter() {
    	super(MODULE_NAME, true);
    }

    //-------------------------------------------------------------------------
    // Module contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void postInit(Configuration configuration) {
        super.postInit(configuration);

        this.storage = configuration.getPublicStorage();
        this.internal = configuration.getInternalRepository();

        this.projectManager = configuration.getBean(ProjectManager.class);
        if (this.projectManager == null) {
            throw new TechnicalException("project.manager.not.available");
        }
    }

    //-------------------------------------------------------------------------
    // ProjectModule contract support
    //-------------------------------------------------------------------------

    /**
     * {@inheritDoc}
     * @return <code>true</code> if the project contains at least one
     *         source of type {@link CsvSource}.
     */
    @Override
    public URI canHandle(Project p) {
        boolean hasCsvSource = false;
        for (Source s : p.getSources()) {
            if (s instanceof CsvSource) {
                hasCsvSource = true;
                break;
            }
        }
        URI projectPage = null;
        if (hasCsvSource) {
            try {
                return new URI(this.getName() + "?project=" + p.getUri());
            }
            catch (Exception e) {
                throw new TechnicalException(e);
            }
        }
        return projectPage;
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public String getIndexPage(@QueryParam("project") URI projectId,
                               @Context UriInfo uriInfo)
                                                throws WebApplicationException {
        CsvSource src = null;
        Project p = this.projectManager.findProject(projectId);
        if (p != null) {
            for (Source s : p.getSources()) {
                if (s instanceof CsvSource) {
                    src = (CsvSource)s;
                    // Continue to get last CSV source in project...
                }
            }
        }
        if (src == null) {
            throw new NotFoundException("No CSV source found", projectId);
        }
        try {
            src.init(storage, uriInfo.getBaseUri());

            String srcName = " (RDF #" + p.getSources().size() + ')';
            URI targetGraph = new URI(src.getUri()
                                      + '-' + StringUtils.urlify(srcName));
            this.convert(src, this.internal, targetGraph);
            p.addSource(this.projectManager.newTransformedRdfSource(
                                        targetGraph, src.getTitle() + srcName,
                                        targetGraph, src));
            this.projectManager.saveProject(p);
        }
        catch (Exception e) {
            TechnicalException error = new TechnicalException(
                                        "ws.internal.error", e, e.getMessage());
            log.error(error.getMessage(), e);
            throw new WebApplicationException(
                                Response.status(Status.INTERNAL_SERVER_ERROR)
                                        .type(MediaType.TEXT_PLAIN_TYPE)
                                        .entity(error.getMessage()).build());
        }
        return "CSV to RDF conversion completed for source " + src.getTitle();
    }

    private void convert(CsvSource src, Repository target, URI targetGraph) {
        final RepositoryConnection cnx = target.newConnection();
        try {
            final ValueFactory valueFactory = cnx.getValueFactory();

            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
            // Clear target named graph, if any.
            org.openrdf.model.URI ctx = null;
            if (targetGraph != null) {
                ctx = valueFactory.createURI(targetGraph.toString());
                cnx.clear(ctx);
            }
            String baseUri = (targetGraph != null)?
                                            targetGraph.toString() + '/': "";
            // Build predicates URIs.
            int max = src.getColumnsHeader().size();
            org.openrdf.model.URI[] predicates = new org.openrdf.model.URI[max];
            int i = 0;
            for (String s : src.getColumnsHeader()) {
                predicates[i++] = valueFactory.createURI(
                                            baseUri + StringUtils.urlify(s));
            }
            // Load triples
            i = 0;
            for (String[] line : src) {
                String subject = baseUri + i; // + "#_";
                max = Math.min(line.length, max);
                for (int j=0; j<max; j++) {
                    String v = line[j];
                    if (StringUtils.isSet(v)) {
                        cnx.add(valueFactory.createStatement(
                                        valueFactory.createURI(subject),
                                        predicates[j],
                                        this.mapValue(v, valueFactory)),
                                ctx);
                    }
                    // Else: ignore cell.
                }
                i++;
            }
            cnx.commit();
        }
        catch (Exception e) {
            throw new TechnicalException("csv.conversion.failed", e);
    	}
        finally {
            try { cnx.close(); } catch (Exception e) { /* Ignore */ }
        }
    }

    private Literal mapValue(String s, ValueFactory valueFactory) {
        Literal v = null;
        s = s.trim();
        if ((s.indexOf('.') != -1) || (s.indexOf(',') != -1)) {
            // Try double.
            try {
                v = valueFactory.createLiteral(
                                    Double.parseDouble(s.replace(',', '.')));
            }
            catch (Exception e) { /* Ignore... */ }
        }
        if (v == null) {
            // Try integer.
            try {
                v = valueFactory.createLiteral(Long.parseLong(s));
            }
            catch (Exception e) { /* Ignore... */ }
        }
        if (v == null) {
            // Assume string literal.
            v = valueFactory.createLiteral(s);
        }
        return v;
    }
}

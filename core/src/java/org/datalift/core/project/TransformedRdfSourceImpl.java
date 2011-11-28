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

package org.datalift.core.project;


import javax.persistence.Entity;

import org.openrdf.model.Statement;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.repository.RepositoryConnection;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.util.CloseableIterator;


@Entity
@RdfsClass("datalift:TransformedRdfSource")
public class TransformedRdfSourceImpl extends BaseSource
                                      implements TransformedRdfSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:targetGraph")
    private String targetGraph;
    @RdfProperty("datalift:parentSource")
    private Source parent;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new transformed RDF source with the specified
     * identifier.
     */
    public TransformedRdfSourceImpl() {
        super(SourceType.TransformedRdfSource);
    }

    /**
     * Creates a new transformed RDF source with the specified
     * identifier and owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    public TransformedRdfSourceImpl(String uri, Project project) {
        super(SourceType.TransformedRdfSource, uri, project);
    }

    //-------------------------------------------------------------------------
    // Source contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getSourceUrl() {
        String source = super.getSourceUrl();
        if (source == null) {
            source = this.getParent().getSourceUrl();
        }
        return source;
    }

    /** {@inheritDoc} */
    @Override
    public void delete() {
        RepositoryConnection cnx = Configuration.getDefault()
                                                .getInternalRepository()
                                                .newConnection();
        try {
            cnx.clear(cnx.getValueFactory().createURI(this.getTargetGraph()));
        }
        catch (Exception e) {
            try { cnx.close(); } catch (Exception e2) { /* Ignore... */ }

            throw new TechnicalException("rdf.store.access.error", e);
        }
    }

    //-------------------------------------------------------------------------
    // TransformedRdfSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getTargetGraph() {
        return this.targetGraph;
    }

    /** {@inheritDoc} */
    @Override
    public Source getParent() {
        return this.parent;
    }

    /** {@inheritDoc} */
    @Override
    public void setParent(Source parent) {
        this.parent = parent;
    }

    /** {@inheritDoc} */
    @Override
    public CloseableIterator<Statement> iterator() {
        final RepositoryConnection cnx = Configuration.getDefault()
                                                      .getInternalRepository()
                                                      .newConnection();
        try {
            final GraphQueryResult result =
                        cnx.prepareGraphQuery(QueryLanguage.SPARQL,
                                        "CONSTRUCT { ?s ?p ?o . }" +
                                        " WHERE { GRAPH <" + this.targetGraph +
                                            "> { ?s ?p ?o . } }",
                                        this.getSourceUrl()).evaluate();
            return new CloseableIterator<Statement>() {
                    @Override
                    public boolean hasNext() {
                        try {
                            boolean hasNext = result.hasNext();
                            if (! hasNext) {
                                this.close();
                            }
                            return hasNext;
                        }
                        catch (Exception e) {
                            this.close();
                            throw new TechnicalException("rdf.store.access.error", e);
                        }
                    }
    
                    @Override
                    public Statement next() {
                        try {
                            return result.next();
                        }
                        catch (Exception e) {
                            this.close();
                            throw new TechnicalException("rdf.store.access.error", e);
                        }
                    }
    
                    @Override
                    public void remove() {
                        this.close();
                        throw new UnsupportedOperationException();
                    }

                    /**
                     * Ensures resources are released even when
                     * {@link #close()} has not been invoked by user class.
                     */
                    @Override
                    protected void finalize() {
                        this.close();
                    }

                    public void close() {
                        try { result.close(); } catch (Exception e) { /* Ignore... */ }
                        try { cnx.close();    } catch (Exception e) { /* Ignore... */ }
                    }
                };
        }
        catch (Exception e) {
            try { cnx.close(); } catch (Exception e2) { /* Ignore... */ }

            throw new TechnicalException("rdf.store.access.error", e);
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void setTargetGraph(String targetGraph) {
        this.targetGraph = targetGraph;
        if (this.getTitle() == null) {
            this.setTitle("<" + targetGraph + '>');
        }
    }
}

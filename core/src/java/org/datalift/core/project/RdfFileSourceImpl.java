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


import java.io.IOException;

import javax.persistence.Entity;

import org.openrdf.model.Statement;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFParser;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import org.datalift.core.TechnicalException;
import org.datalift.core.rdf.BoundedAsyncRdfParser;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.RdfFileSource;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.util.CloseableIterator;
import org.datalift.fwk.util.Env;


/**
 * Default implementation of the {@link RdfFileSource} interface.
 *
 * @author hdevos
 */
@Entity
@RdfsClass("datalift:rdfSource")
public class RdfFileSourceImpl extends BaseFileSource
                               implements RdfFileSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:baseUri")
    private String baseUri;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new RDF file source.
     */
    public RdfFileSourceImpl() {
        super(SourceType.RdfFileSource);
    }

    /**
     * Creates a new RDF file source with the specified identifier and
     * owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    public RdfFileSourceImpl(String uri, Project project) {
        super(SourceType.RdfFileSource, uri, project);
    }

    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void setMimeType(String mimeType) {
        super.setMimeType(RdfUtils.parseMimeType(mimeType).toString());
    }

    //-------------------------------------------------------------------------
    // RdfSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("deprecation")
    public CloseableIterator<Statement> iterator() {
        try {
            ParserConfig config = new ParserConfig(
                            true,       // Assume data are valid.
                            false,      // Report all errors.
                            false,      // Don't preserve BNode ids.
                            org.openrdf.rio.RDFParser.DatatypeHandling.VERIFY);
            RDFParser parser = RdfUtils.newRdfParser(this.getMimeType());
            parser.setParserConfig(config);
            return BoundedAsyncRdfParser.parse(this.getInputStream(),
                                               parser, this.getBaseUri(),
                                               Env.getRdfBatchSize());
        }
        catch (IOException e) {
            throw new TechnicalException(e.getMessage(), e);
        }
    }

    /** {@inheritDoc} */
    @Override
    public final String getBaseUri() {
        return this.baseUri;
    }

    //-------------------------------------------------------------------------
    // RdfFileSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final void setBaseUri(String uri) {
        this.baseUri = uri;
    }
}

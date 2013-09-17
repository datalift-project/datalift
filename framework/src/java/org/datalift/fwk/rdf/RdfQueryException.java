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

package org.datalift.fwk.rdf;


import java.text.MessageFormat;
import java.util.Map;

import org.openrdf.query.Dataset;

import org.datalift.fwk.TechnicalException;


/**
 * An exception that reports technical errors encountered while
 * processing a SPARQL query.
 *
 * @author lbihanic
 */
public abstract class RdfQueryException extends TechnicalException
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final String query;
    private Map<String,Object> bindings;
    private Dataset dataset;
    private String baseUri;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Constructs a new exception with the specified cause but no
     * detail message.  The detail message of this exception will
     * be the detail message of the cause.
     * @param  query   the query.
     * @param  cause   the cause.
     */
    protected RdfQueryException(String query, Throwable cause) {
        super(cause);
        this.query = query;
    }

    /**
     * Constructs a new exception with the specified message code
     * and arguments to build a detail message from the format
     * associated to the message code.
     * <p>
     * The message code can be either the actual message format or
     * the identifier of a resource (defined in the
     * {@link #getMessageBundleName exception type resource bundle})
     * that contains the message format.</p>
     * <p>
     * If no message arguments are provided, the query will
     * automatically be provided.</p>
     * @param  query   the query.
     * @param  code    the message code or format. In the latter
     *                 case, it shall be compliant with the
     *                 grammar defined by {@link MessageFormat}.
     * @param  data    the arguments to build the detail message
     *                 from the format.
     */
    protected RdfQueryException(String query, String code, Object... data) {
        super(code, null, (data != null)? data: query);
        this.query = query;
    }

    /**
     * Constructs a new exception with the specified message code
     * and the arguments to build a detail message from the format
     * associated to the message code.
     * <p>
     * The message code can be either the actual message format or
     * the identifier of a resource (defined in the
     * {@link #getMessageBundleName exception type resource bundle})
     * that contains the message format.</p>
     * <p>
     * If no message arguments are provided, the query will
     * automatically be provided.</p>
     * @param  query   the query.
     * @param  code    the message code or format. In the latter
     *                 case, it shall be compliant with the
     *                 grammar defined by {@link MessageFormat}.
     * @param  cause   the cause. A <code>null</code> value is
     *                 allowed to indicate that the cause is
     *                 nonexistent or unknown.
     * @param  data    the arguments to build the detail message
     *                 from the format.
     */
    protected RdfQueryException(String query, String code,
                                Throwable cause, Object... data) {
        super(code, cause, (data != null)? data: query);
        this.query = query;
    }

    //-------------------------------------------------------------------------
    // RdfQueryException contract definition
    //-------------------------------------------------------------------------

    public String getQuery() {
        return query;
    }

    public void setBindings(Map<String,Object> bindings) {
        this.bindings = bindings;
    }

    public Map<String,Object> getBindings() {
        return this.bindings;
    }

    public void setDataset(Dataset dataset) {
        this.dataset = dataset;
    }

    public Dataset getDataset() {
        return this.dataset;
    }

    public void setBaseUri(String baseUri) {
        this.baseUri = baseUri;
    }

    public String getBaseUri() {
        return this.baseUri;
    }

    public final RdfQueryException populate(Map<String,Object> bindings,
                                            Dataset dataset, String baseUri) {
        this.setBindings(bindings);
        this.setDataset(dataset);
        this.setBaseUri(baseUri);
        return this;
    }
}

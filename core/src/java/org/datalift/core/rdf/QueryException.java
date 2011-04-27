package org.datalift.core.rdf;


import org.datalift.core.TechnicalException;
import org.datalift.fwk.rdf.RdfQueryException;


public class QueryException extends RdfQueryException
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String BUNDLE_NAME =
            TechnicalException.class.getPackage().getName().replace('.', '/')
            + DEFAULT_BUNDLE_NAME;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    public QueryException(String query, Throwable cause) {
        super(query, cause);
    }
    /** {@inheritDoc} */
    public QueryException(String query, String code, Object... data) {
        super(query, code, data);
    }

    /** {@inheritDoc} */
    public QueryException(String query, String code,
                                        Throwable cause, Object... data) {
        super(query, code, cause, data);
    }

    //-------------------------------------------------------------------------
    // TechnicalException contract support
    //-------------------------------------------------------------------------

    protected String getMessageBundleName() {
        return BUNDLE_NAME;
    }
}

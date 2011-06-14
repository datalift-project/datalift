package org.datalift.projectmanager;


public class TechnicalException extends org.datalift.fwk.TechnicalException
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    protected TechnicalException() {
        super();
    }

    /** {@inheritDoc} */
    protected TechnicalException(Throwable cause) {
        super(cause);
    }

    /** {@inheritDoc} */
    public TechnicalException(String code, Object... data) {
        super(code, data);
    }

    /** {@inheritDoc} */
    public TechnicalException(String code, Throwable cause, Object... data) {
        super(code, cause, data);
    }
}

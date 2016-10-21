package org.datalift.stringtouri;


public class TechnicalException extends org.datalift.fwk.TechnicalException
{
    //------------------------------------------------------------------------
    // Constants
    //------------------------------------------------------------------------

    /** Default serialization version id. */
    private final static long serialVersionUID = 1L;

    //------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------
	
    protected TechnicalException(String cause) {
        super(cause);
    }
}

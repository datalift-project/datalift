package org.datalift.core.project;


import com.clarkparsia.empire.SupportsRdfId;
import com.clarkparsia.empire.annotation.SupportsRdfIdImpl;


public abstract class BaseRdfEntity implements SupportsRdfId
{
    private SupportsRdfId rdfId = new SupportsRdfIdImpl();

    //-------------------------------------------------------------------------
    // SupportsRdfId contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public RdfKey<?> getRdfId() {
        return this.rdfId.getRdfId();
    }

    /** {@inheritDoc} */
    @Override @SuppressWarnings("unchecked")
    public void setRdfId(RdfKey theId) {
        this.rdfId.setRdfId(theId);
    }
}

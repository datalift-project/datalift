package org.datalift.projectmanager;


import com.clarkparsia.empire.SupportsRdfId;
import com.clarkparsia.empire.annotation.SupportsRdfIdImpl;


public abstract class BaseRdfEntity implements SupportsRdfId
{
    private SupportsRdfId rdfId = new SupportsRdfIdImpl();

    //-------------------------------------------------------------------------
    // BaseRdfEntity contract definition
    //-------------------------------------------------------------------------

    abstract protected void setId(String id);

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
    public void setRdfId(RdfKey id) {
        this.rdfId.setRdfId(id);
        this.setId(String.valueOf(id));
    }
}

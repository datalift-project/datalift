package org.datalift.wfs.wfs2.mapping;

import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Context;
import org.openrdf.rio.RDFHandlerException;

public interface Mapper {
	public void map(ComplexFeature cf, Context ctx) throws RDFHandlerException;
}

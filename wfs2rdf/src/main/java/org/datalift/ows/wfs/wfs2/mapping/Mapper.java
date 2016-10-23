package org.datalift.ows.wfs.wfs2.mapping;

import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Context;
import org.openrdf.rio.RDFHandlerException;

public interface Mapper {
	public void map(ComplexFeature cf, Context ctx) throws RDFHandlerException;
}

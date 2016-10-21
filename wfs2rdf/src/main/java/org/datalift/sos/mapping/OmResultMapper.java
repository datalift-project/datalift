package org.datalift.sos.mapping;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Context;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.rio.RDFHandlerException;

public class OmResultMapper extends BaseMapper{

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		return;
	}

	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				ctx.getMapper(f.getTypeName()).map(f, ctx);
			}
		}
	}

	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) {
		return;
	}


	@Override
	protected boolean mapAsIntermediate(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		return false;
	}
}

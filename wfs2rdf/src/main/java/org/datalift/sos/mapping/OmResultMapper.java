package org.datalift.sos.mapping;

import javax.xml.namespace.QName;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.wfs.wfs2.mapping.BaseMapper;

public class OmResultMapper extends BaseMapper{

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		return;
	}

	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) {
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
}

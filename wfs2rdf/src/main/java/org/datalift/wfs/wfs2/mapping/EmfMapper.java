package org.datalift.wfs.wfs2.mapping;

import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.openrdf.model.URI;

public class EmfMapper extends BaseMapper {

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {		
		if(!cf.isSimple()
				)super.addRdfTypes(cf, ctx);
		URI typeSmodURI = ctx.vf.createURI(Context.nsSmod+Helper.capitalize(cf.name.getLocalPart()));
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	

			}
}

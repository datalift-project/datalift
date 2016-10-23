package org.datalift.ows.wfs.wfs2.mapping;

import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.utilities.Helper;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;

/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class EmfMapper extends BaseMapper {

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {		
		if(!cf.isSimple()
				)super.addRdfTypes(cf, ctx);
		URI typeSmodURI = ctx.vf.createURI(Context.nsSmod+Helper.capitalize(cf.name.getLocalPart()));
		ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	

	}
}

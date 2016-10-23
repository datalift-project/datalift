package org.datalift.ows.wfs.wfs2.mapping;

import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.utilities.Helper;
import org.openrdf.model.Resource;
import org.openrdf.rio.RDFHandlerException;
/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
//exemple legal backgroun, broader
public class AnyTypeMapper extends BaseMapper{

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		/****add the parentlinked statement****/
		if(cf.name.equals(Const.belongsTo))
		{
			Resource subjectURI;
			if(cf.getParent()!=null)
			{
				subjectURI= cf.getParent().getId();
			}
			else 
			{
				subjectURI=Context.DefaultSubjectURI;
			}
			ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), cf.getId()));			
		}
		else
		{
			super.addParentLinkStatements(cf, ctx);
		}
	}

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!cf.isSimple())
			{
			if(cf.isReferencedObject())
			{
				ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(Context.referencedObjectType.getLocalPart()))));
				if(cf.name.equals(Const.belongsTo))
				{
					return;
				}
			}
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(cf.name.getLocalPart()))));

			}
			}

}

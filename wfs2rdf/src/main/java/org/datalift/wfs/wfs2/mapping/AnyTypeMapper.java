package org.datalift.wfs.wfs2.mapping;

import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.openrdf.model.Resource;
//exemple legal backgroun, broader
public class AnyTypeMapper extends BaseMapper{

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
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
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), cf.getId()));			
		}
		else
		{
			super.addParentLinkStatements(cf, ctx);
		}
	}

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		if(!cf.isSimple())
			{
			if(cf.isReferencedObject())
			{
				ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(Context.referencedObjectType.getLocalPart()))));
				if(cf.name.equals(Const.belongsTo))
				{
					return;
				}
			}
			ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(cf.name.getLocalPart()))));

			}
			}

}

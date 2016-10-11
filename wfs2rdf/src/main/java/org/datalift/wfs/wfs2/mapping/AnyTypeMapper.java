package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;

public class AnyTypeMapper extends BaseMapper{

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		boolean found=false;
		if(cf.name.equals(Const.belongsTo))
			found=true;
		if(ignore(cf))
		{
			return;
		}
		if(cf.name.equals(Const.omResult))
		{
			ctx.getMapper(Const.omResult).map(cf, ctx);
		}
		else
		{
			if(isIntermediateFeature(cf))
			{
				//do specific mapping : rac
				this.setCfId(cf,ctx);			
				for (Attribute a : cf.itsAttr) {
					if (a instanceof ComplexFeature) {
						ComplexFeature f = (ComplexFeature)a;
						//exceptionnellement ici!!
						setCfId(f,ctx);
						addChildLinkedStatement(cf,f,ctx);		
						this.rememberGmlId(cf,ctx);
						//insert type of f if f will not be mappedwith basic mapper
						ctx.getMapper(f.getTypeName()).map(f, ctx);
					}
				}
			}
			else
			{				
				this.setCfId(cf,ctx);
				if(!alreadyLinked)
				{
					this.addParentLinkStatements(cf, ctx);
				}
				this.addRdfTypes(cf, ctx);
				if(cf.vividgeom!=null)
				{
					ctx.getMapper(new QName("geometry")).map(cf, ctx);
				}
				for (Attribute a : cf.itsAttr) {
					if (a instanceof ComplexFeature) {
						ComplexFeature f = (ComplexFeature)a;
						ctx.getMapper(f.getTypeName()).map(f, ctx);
					}
				}
				this.mapFeatureSimpleAttributes(cf, ctx,null);
			}
		}	
	}
	/**
	 * inserts special predicate whish links directely the current feature with its child : the shortcut
	 * @param cf : the current feature = behaves like the father
	 * @param f : the feature son whish will be used to linked to
	 * @param ctx
	 */
	private void addChildLinkedStatement(ComplexFeature cf, ComplexFeature f, Context ctx) {
		// TODO Auto-generated method stub
		ctx.model.add(ctx.vf.createStatement(cf.getParent().getId(), ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), f.getId()));
	}
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
				subjectURI=ctx.DefaultSubjectURI;
			}
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), cf.getId()));			
		}
		else
		{
			super.addParentLinkStatements(cf, ctx);
		}
	}

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		if(isReferencedObject(cf))
		{
			ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(ctx.referencedObjectType.getLocalPart()))));
			if(cf.name.equals(Const.belongsTo))
			{
				return;
			}
		}
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(cf.name.getLocalPart()))));

	}

}

package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;

public class CodeListMapper extends BaseMapper{
	boolean clAlreadyDefined=false;
	@Override
	public void map(ComplexFeature cf, Context ctx) {
		boolean found=false;
		if(cf.name.equals(Const.onlineResource))
			found=true;
		if(ignore(cf))
			{
				return;
			}
		this.setCfId(cf,ctx);
		if(!cf.name.equals(Const.mediaMonitored))
		{
			if(!alreadyLinked)
			{
				this.addParentLinkStatements(cf, ctx);
			}
		    this.rememberGmlId(cf,ctx);
		if(!clAlreadyDefined)
			{
				this.addRdfTypes(cf, ctx);
				this.mapFeatureSimpleAttributes(cf, ctx,null);
			}
		}

	/////add the element as smod property
			if(cf.name.equals(Const.purpose) || cf.name.equals(Const.specialisedEMFType)) //get litteral value embeded in the feature (especially the litteraal in title)
			{
				URI smodPredicate=ctx.vf.createURI(ctx.nsSmod+cf.name.getLocalPart());
				Value v=ctx.vf.createLiteral(cf.getLiteral());
				ctx.model.add(ctx.vf.createStatement(cf.getParent().getId(), smodPredicate, v));
				
			}
			if(cf.name.equals(Const.mediaMonitored))
			{
				URI smodPredicate=ctx.vf.createURI(ctx.nsSmod+cf.name.getLocalPart());
				Value v=ctx.vf.createURI(cf.getResource());
				ctx.model.add(ctx.vf.createStatement(cf.getParent().getId(), smodPredicate, v));
				
			}
	}
	@Override
	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource id) {
		// TODO Auto-generated method stub
		for (Attribute a : cf.itsAttr) {
			if(! (a instanceof ComplexFeature) && !a.name.equals(SosConst.frame))
			{
				
				if(a.getTypeName().equals(Const.hrefType) || a.getTypeName().equals(Const.anyURI))
				{
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsRDFS+"isDefinedBy"), ctx.vf.createURI(a.value)));
					
				}
				if(a.getTypeName().equals(Const.titleAttrType))
				{
					String value=a.value;
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsSkos+"prefLabel"), ctx.vf.createLiteral(value)));
				} 
	
			}
		}
	}
	
	@Override
	protected void setCfId(ComplexFeature cf, Context ctx) {
		/******give the feature an identifier****/
		Resource os;
		int count=0;		
		if(cf.getId()!=null)
			{
				alreadyLinked=true;
				return;
			}
		Resource codeListRefId=ctx.codeListOccurences.get(cf.getAttributeValue(Const.href));
		if(codeListRefId!=null)
		{
			cf.setId(codeListRefId);
			clAlreadyDefined=true;
		}
		else
		{
			QName type=ctx.referencedCodeListType;//Const.ReferenceType;
			count =ctx.getInstanceOccurences(type);
			os=ctx.vf.createURI(ctx.nsProject+type.getLocalPart()+"_"+count);
			cf.setId(os);
			ctx.codeListOccurences.put(cf.getAttributeValue(Const.href), os);
		}
		alreadyLinked=false;		
	}
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {		
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(cf.name.getLocalPart()))));
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsSkos+"Concept")));
	}
}

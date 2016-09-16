package org.datalift.wfs.wfs2.mapping;



import javax.xml.namespace.QName;

import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;
import org.openrdf.model.Resource;


public class BaseMapper implements Mapper {
	
	
	boolean alreadyLinked=false;
	protected boolean ignore(ComplexFeature f)
	{	
		return (isEmpty(f) || f.name.equals(Const.identifier) || f.name.equals(Const.inspireId))? true:false;

	}
	protected boolean isEmpty(ComplexFeature f)
	{
		boolean empty=false;
		if(f.value==null || f.value.equals(""))
			{
				empty=true;
			}
		for (Attribute a : f.itsAttr) {
			if(!a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.nil)&& !a.name.equals(Const.nilReason))
			{
				return false;		 
			}
		}
		return empty;
	}




	protected void setCfId(ComplexFeature cf, Context ctx) {
		/******give the feature an identifier****/
		Resource os;
		int count=0;		
		if(cf.getId()!=null)
			{
				alreadyLinked=true;
				return;
			}
		if(isReferencedObject(cf))
		{
			QName type=ctx.referencedObjectType;//Const.ReferenceType;
			count =ctx.getInstanceOccurences(type);
			os=ctx.vf.createURI(ctx.nsProject+type.getLocalPart()+"_"+count);
		}
		else
		{
			count=ctx.getInstanceOccurences(cf.name);
			os=ctx.vf.createURI(ctx.nsProject+cf.name.getLocalPart()+"_"+count);
		}
		
		cf.setId(os);
		alreadyLinked=false;		
	}

	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
		Resource subjectURI;
		if(cf.getParent()!=null )
		{
			if(cf.getParent().getId()!=null)
				{
					subjectURI= cf.getParent().getId();
				}
			else
			{
				return;
			}
		}
		else 
		{
			subjectURI=ctx.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), cf.getId()));
	}

	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+capitalize(cf.name.getLocalPart()))));
		if(isReferencedObject(cf))
			ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+capitalize(ctx.referencedObjectType.getLocalPart()))));
	}

	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		for (Attribute a : cf.itsAttr) {
			if(! (a instanceof ComplexFeature) && !a.name.equals(SosConst.frame))
			{
				
				if(a.getTypeName().equals(Const.xsdBoolan) && !a.name.equals(Const.owns) && !a.name.equals(Const.nil))
				{
					boolean value=Boolean.valueOf(a.value);
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsDatalift+a.name.getLocalPart()), ctx.vf.createLiteral(value)));
				}
				if(a.getTypeName().equals(Const.StringOrRefType) )
				{
					String value=a.value;
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsDcTerms+"description"), ctx.vf.createLiteral(value)));
				}
				if(a.getTypeName().equals(Const.string))
				{
					String value=a.value;
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsFoaf+"name"), ctx.vf.createLiteral(value)));
				}
				if(a.getTypeName().equals(Const.positiveInteger))
				{
					int value=Integer.valueOf(a.value);
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsDatalift+a.name.getLocalPart()), ctx.vf.createLiteral(value)));
				}
				if(a.getTypeName().equals(Const.hrefType) || a.getTypeName().equals(Const.anyURI))
				{
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsRDFS+"isDefinedBy"), ctx.vf.createURI(a.value)));
					
				}
				if(a.getTypeName().equals(Const.titleAttrType))
				{
					String value=a.value;
					ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsDcTerms+"title"), ctx.vf.createLiteral(value)));
				} 	
			}
		}
	}
	protected boolean isIntermediateFeature(ComplexFeature cf)
	{
		int nbr_cf_found=0;
		for (Attribute a : cf.itsAttr) {
			if(a instanceof ComplexFeature)
			{
				nbr_cf_found++;
			}
			else	
			{
				if(!a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.nil)&& !a.name.equals(Const.nilReason))
					return false;
			}
			if(nbr_cf_found>1)
				return false;
			}
		return true;
	}
	protected String capitalize(String className)
	{
		return className.substring(0, 1).toUpperCase() + className.substring(1);
	}
	protected boolean isReferencedObject(ComplexFeature cf)
	{
		for (Attribute a : cf.itsAttr) {
			if(a.getTypeName().equals(Const.hrefType))
				{
					return true;
				}
		}
		return false;
	}
	public void map(ComplexFeature cf, Context ctx) {
		// 
		boolean found=false;
		if(cf.name.equals(Const.onlineResource))
			found=true;
		if(ignore(cf))
			{
				return;
			}
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
		this.mapFeatureSimpleAttributes(cf, ctx);
		
	}

}

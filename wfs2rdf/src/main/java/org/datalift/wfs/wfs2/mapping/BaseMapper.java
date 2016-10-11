package org.datalift.wfs.wfs2.mapping;



import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.datalift.model.ComplexFeature;
import org.datalift.fwk.log.Logger;
import org.datalift.geoutility.Context;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;


public class BaseMapper implements Mapper {

	protected final static Logger log = Logger.getLogger();
	protected boolean alreadyLinked=false;

	protected boolean ignore(ComplexFeature f)
	{	
		//include the case where the element is just a wrapper, exp: member 
		return (Helper.isEmpty(f) || f.name.equals(Const.identifier) || f.name.equals(Const.inspireId))? true:false;

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
		//check if there is any gml identifier. if yes, use it as id if not, create a generic id
		String id=cf.getAttributeValue(Const.identifier);
		if(id==null)
		{
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
		}
		else
		{
			try {
				cf.setId(ctx.vf.createURI(id));
			} catch (Exception e) {
				//if the id is not a valid URI, then use the standard base URI 
				cf.setId(ctx.vf.createURI(ctx.baseURI+id));
			}
		}	
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

		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(cf.name.getLocalPart()))));
		if(isReferencedObject(cf))
		{ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(ctx.referencedObjectType.getLocalPart()))));}
	}

	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource toLinkWith) {

		Resource id;
		String predicat=null;
		if(toLinkWith==null)
		{
			id=cf.getId();
		}else
		{
			id=toLinkWith;
		}
		for (Attribute a : cf.itsAttr) {
			if(! (a instanceof ComplexFeature) && !a.name.equals(SosConst.frame))
			{			
				mapTypedValue(id, a.value, a.getTypeName(), a.name, predicat, ctx);
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

	protected boolean containsReference(ComplexFeature cf)
	{
		for (Attribute a : cf.itsAttr) {
			if(a.getTypeName().equals(Const.hrefType) /*|| a.getTypeName().equals(Const.anyURI)*/)
			{
				return true;
			}
		}
		return false;
	}
	//a referenced object is an xml element which contains a reference (URI, href) and at least one other "util" attribute (id, title...)
	protected boolean isReferencedObject(ComplexFeature cf)
	{
		if(!containsReference(cf))
		{
			return false;
		}
		for (Attribute a : cf.itsAttr) {
			if(!a.getTypeName().equals(Const.hrefType) && !a.getTypeName().equals(Const.anyURI) && !a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.nil)&& !a.name.equals(Const.nilReason))
			{
				return true;
			}
		}
		return false;
	}
	public void map(ComplexFeature cf, Context ctx) {
		// 
		boolean found=false;
		if(cf.name.getLocalPart().equals("result"))
			found=true;
		if(ignore(cf))
		{
			return;
		}
		this.setCfId(cf,ctx);
		if(!alreadyLinked)
		{
			if(cf.isSimple())
			{
				this.addParentSimpleLinkStatements(cf, ctx);
				return;
			}else
			{
				this.addParentLinkStatements(cf, ctx);
			}
		}
		this.rememberGmlId(cf,ctx);
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
	/**
	 * FOR SHORTCUT MAPPING : the feature's content is directly linked with its parent 
	 * links the unique value of the  cf with the subject cf.getParent.id using the name of cf as a predicat
	 * it generates ONE triple
	 * use case of this is <om:procedure xlink:href="urn:xxx"/>
	 * @param cf
	 * @param ctx
	 */
	protected void addParentSimpleLinkStatements(ComplexFeature cf, Context ctx) {
		// first of all, look at the value of the feature. if any then try to create the triple with the value of one attribute 
		if(Helper.isSet(cf.value))
		{
			mapTypedValue(cf.getParent().getId(), cf.value, cf.getTypeName(), cf.name, null, ctx);
			return;
		}
		this.mapFeatureSimpleAttributes(cf, ctx, cf.getParent().getId());		
	}
	protected void rememberGmlId(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		String id=cf.getAttributeValue(SosConst.id);
		if(id!=null)
		{
			ctx.referenceCatalogue.put("#"+id, cf.getId().stringValue());
		}
	}
	protected String getObjectIdReferencedBy(String hrefValue, Context ctx) {
		String id = ctx.referenceCatalogue.get(hrefValue);
		return (id != null)? id: ctx.referenceCatalogue.get(null)+hrefValue;
	}
	protected void mapTypedValue(Resource id, String value, QName type, QName attrName, String predicate, Context ctx)
	{
		boolean added=false;
		if(type.equals(Const.xsdBoolan) && !attrName.equals(Const.owns) && !attrName.equals(Const.nil))
		{
			if(predicate==null)
			{
				predicate=ctx.nsDatalift+attrName.getLocalPart();
			}
			boolean bvalue=Boolean.valueOf(value);
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(bvalue)));
			added=true;
		}
		if(type.equals(Const.StringOrRefType) )
		{
			if(predicate==null)
			{
				predicate=ctx.nsDcTerms+"description";
			}
			String svalue=value;
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added=true;
		}
		if(type.equals(Const.string))
		{
			if(predicate==null)
			{
				predicate=ctx.nsFoaf+"name";
			}
			String svalue=value;
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added=true;
		}
		if(type.equals(Const.positiveInteger))
		{
			if(predicate==null)
			{
				predicate=ctx.nsDatalift+attrName.getLocalPart();
			}
			int ivalue=Integer.valueOf(value);
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(ivalue)));
			added=true;
		}
		if(type.equals(Const.xsdDouble) || type.equals(Const.xsdDecimal))
		{
			if(predicate==null)
			{
				predicate=ctx.nsDatalift+attrName.getLocalPart();
			}
			double dvalue=Integer.valueOf(value);
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(dvalue)));
			added=true;
		}
		if(type.equals(Const.hrefType) || type.equals(Const.anyURI))
		{
			try{
				if(predicate==null)
				{
					predicate=ctx.nsDatalift+attrName.getLocalPart();
				}
				ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createURI(value)));
				added=true;
			}catch (IllegalArgumentException e)
			{
				log.warn(e.getMessage());
				ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createURI(getObjectIdReferencedBy(value,ctx))));
				added=true;
			}
		}
		if(type.equals(Const.titleAttrType))
		{
			if(predicate==null)
			{
				predicate=ctx.nsDcTerms+"title";
			}
			String svalue=value;
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added=true;
		}
		if(type.equals(Const.TimePositionType) || type.equals(Const.xsdDate) || type.equals(SosConst.TimeInstantType) )
		{
			XMLGregorianCalendar d=Helper.getDate(value);
			if(d!=null)
			{
				if(predicate==null)
				{
					predicate=ctx.nsDatalift+attrName.getLocalPart();
				}
				Value v5=ctx.vf.createLiteral(d);
				ctx.model.add(ctx.vf.createStatement(id,ctx.vf.createURI(predicate), v5));
				added=true;
			}  
		}
//		if(!added)
//		{
//			if(predicate==null)
//			{
//				predicate=ctx.nsDatalift+attrName.getLocalPart();
//			}
//
//			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(value)));
//			added=true;
//		}
	}
}

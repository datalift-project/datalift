package org.datalift.sos.mapping;

import javax.xml.namespace.QName;

import org.datalift.geoutility.Context;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.eclipse.xsd.ecore.MapBuilder.Mapper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

public class ObservationMemberMapper extends BaseMapper {
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {

		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+"Member")));
		if(isReferencedObject(cf))
		{
			ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(ctx.referencedObjectType.getLocalPart()))));
		}

		URI typeSmodURI = ctx.vf.createURI(ctx.nsOml+"Observation");
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	
	}

	@Override
	public void map(ComplexFeature cf, Context ctx) {
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
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				if(f.getTypeName().equals(Const.MeasureTVPType))
				{
					BaseMapper m=new SimpleMeasureMapper();
					m.map(f, ctx);
				}
			}
		}
		this.mapFeatureSimpleAttributes(cf, ctx,null);

	}
	//here we want to link the current feature not with the parent but with the observationcollection 
	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
		Resource idCollection=cf.getIdTypedParent(Const.OM_ObservationPropertyType);
		if(idCollection==null)
		{
			idCollection=ctx.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		ctx.model.add(ctx.vf.createStatement(idCollection, ctx.vf.createURI(ctx.nsOml+"member"), cf.getId()));
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
				QName name=new QName ("member");
				count=ctx.getInstanceOccurences(name);
				os=ctx.vf.createURI(ctx.nsProject+name.getLocalPart()+"_"+count);
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

}

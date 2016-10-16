package org.datalift.wfs.wfs2.mapping;

import org.openrdf.model.Resource;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;

import javax.xml.namespace.QName;


public class ObservationPropertyTypeMapper extends BaseMapper {

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {

		Resource subjectURI;
		if(cf.getParent()!=null)
		{
			subjectURI= cf.getParent().getId();
		}
		else 
		{
			subjectURI=ctx.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
	   ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), cf.getId()));	
	}

	@Override
	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource toLinkWith) {
		Resource id;
		if (toLinkWith == null) {
			id = cf.getId();
		} else {
			id = toLinkWith;
		}
		for (Attribute a : cf.itsAttr) {
			if (!(a instanceof ComplexFeature) && isUsefulAttribute(a)) {
				mapTypedValue(id, a.value, a.getTypeName(), a.name, Context.nsDatalift+cf.name.getLocalPart(), ctx);
			}
		}
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(ctx.referencedObjectType.getLocalPart()))));
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsOml+ctx.observationType.getLocalPart())));
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
		if(isReferencedObject(cf))
		{
			QName type=ctx.referencedObjectType;//Const.ReferenceType;
			count =ctx.getInstanceOccurences(type);
			os=ctx.vf.createURI(ctx.nsProject+type.getLocalPart()+"_"+count);
		}
		else
		{
			count=ctx.getInstanceOccurences(new QName("","Observation"));
			os=ctx.vf.createURI(ctx.nsProject+"Observation_"+count);
		}
		
		cf.setId(os);
		alreadyLinked=false;		
	}
}

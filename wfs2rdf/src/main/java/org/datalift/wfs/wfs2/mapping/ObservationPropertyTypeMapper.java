package org.datalift.wfs.wfs2.mapping;

import org.openrdf.model.Resource;
import org.datalift.model.ComplexFeature;

import javax.xml.namespace.QName;

import org.datalift.geoutility.Context;


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
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+capitalize(ctx.referencedObjectType.getLocalPart()))));
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

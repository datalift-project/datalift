package org.datalift.wfs.wfs2.mapping;

import org.openrdf.model.Resource;
import org.openrdf.rio.RDFHandlerException;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;

import javax.xml.namespace.QName;


public class ObservationPropertyTypeMapper extends BaseMapper {

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {

		Resource subjectURI;
		if(cf.getParent()!=null)
		{
			subjectURI= cf.getParent().getId();
		}
		else 
		{
			subjectURI=Context.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
	   ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), cf.getId()));	
	}

	@Override
	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource toLinkWith) throws RDFHandlerException {
		if(!cf.isSimple())
			{Resource id;
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
	}}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!cf.isSimple())
			{
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(Context.referencedObjectType.getLocalPart()))));
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsOml+Context.observationType.getLocalPart())));
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
		if(cf.isReferencedObject())
		{
			QName type=Context.referencedObjectType;//Const.ReferenceType;
			count =ctx.getInstanceOccurences(type);
			os=ctx.vf.createURI(Context.nsProject+type.getLocalPart()+"_"+count);
		}
		else
		{
			count=ctx.getInstanceOccurences(new QName("","Observation"));
			os=ctx.vf.createURI(Context.nsProject+"Observation_"+count);
		}
		
		cf.setId(os);
		alreadyLinked=false;		
	}
}

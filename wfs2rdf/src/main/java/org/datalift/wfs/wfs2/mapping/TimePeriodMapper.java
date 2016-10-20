package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

public class TimePeriodMapper extends BaseMapper{

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		if(ignore(cf))
			return;
		this.setCfId(cf,ctx);
		if(!alreadyLinked)
		{			
			this.addParentLinkStatements(cf, ctx);
		}
		
	    this.rememberGmlId(cf,ctx);
		//this.addRdfTypes(cf, ctx);
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature position=(ComplexFeature) a;
				InstantPositionMapper m =new InstantPositionMapper();
				m.map(position, ctx);
			    this.rememberGmlId(position,ctx);
			}
		}
		this.mapFeatureSimpleAttributes(cf, ctx,null);
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {	
		//ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(cf.name.getLocalPart()))));
		//ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsIsoTP+"TM_Period")));
		URI typeIntervalURI = ctx.vf.createURI(Context.nsw3Time+"ProperInterval");
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeIntervalURI));
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
			if(cf.isReferencedObject())
			{
				QName type=Context.referencedObjectType;//Const.ReferenceType;
				count =ctx.getInstanceOccurences(type);
				os=ctx.vf.createURI(Context.nsProject+type.getLocalPart()+"_"+count);
			}
			else
			{
				count=ctx.getInstanceOccurences(cf.name);
				os=ctx.vf.createBNode(cf.name.getLocalPart()+"_"+count);
			}		
			cf.setId(os);
		}
		else
		{
			try {
				cf.setId(ctx.vf.createBNode(id));
			} catch (Exception e) {
				//if the id is not a valid URI, then use the standard base URI 
				cf.setId(ctx.vf.createURI(ctx.baseURI+id));
			}
		}	
		alreadyLinked=false;	
	}
}

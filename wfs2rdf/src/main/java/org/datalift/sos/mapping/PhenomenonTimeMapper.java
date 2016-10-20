package org.datalift.sos.mapping;

import javax.xml.namespace.QName;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;

public class PhenomenonTimeMapper extends BaseMapper {

	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				if(f.name.equals(Const.TimePeriod) || f.name.equals(Const.TimeInstantPropertyType))
				{
					this.setCfId(f, ctx);
					this.addParentLinkStatements(f, ctx);
				}

				ctx.getMapper(f.getTypeName()).map(f, ctx); 
			}
		}
	}
	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) {
		return;
	}
	/**
	 * link the timeperiod here as we want to skip phenomenonTime which is just an intermediate xml element 
	 */
	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
		Resource subjectURI;
		ComplexFeature parent=cf.getParent();
		if(parent!=null && parent.getParent()!=null)
		{
			if(parent.getId()!=null && parent.getParent().getId()!=null)
			{
				subjectURI= parent.getParent().getId();
			}
			else
			{
				return;
			}
		}
		else 
		{
			subjectURI=Context.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsOml+"phenomenonTime"), cf.getId()));	
	}

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		return;
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
				cf.setId(ctx.vf.createURI(id));
			} catch (Exception e) {
				//if the id is not a valid URI, then use the standard base URI 
				cf.setId(ctx.vf.createURI(ctx.baseURI+id));
			}
		}	
		alreadyLinked=false;	
	}

}

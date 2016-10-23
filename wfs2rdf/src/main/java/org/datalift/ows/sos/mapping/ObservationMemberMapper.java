package org.datalift.ows.sos.mapping;

import javax.xml.namespace.QName;

import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;

/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class ObservationMemberMapper extends BaseMapper {
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!cf.isSimple())
			{URI typeSmodURI = ctx.vf.createURI(Context.nsOml+"Observation");
		ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	
	}
	}
	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {
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
	}
	@Override
	protected boolean mapAsIntermediate(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		return false;
	}
	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) throws RDFHandlerException {
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
	}

	//here we want to link the current feature not with the parent but with the observationcollection 
	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		Resource idCollection=cf.getIdTypedParent(Const.OM_ObservationPropertyType);
		if(idCollection==null)
		{
			idCollection=Context.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		ctx.model.handleStatement(ctx.vf.createStatement(idCollection, ctx.vf.createURI(Context.nsOml+"member"), cf.getId()));
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
			if(cf.isReferencedObject())
			{
				QName type=Context.referencedObjectType;//Const.ReferenceType;
				count =ctx.getInstanceOccurences(type);
				os=ctx.vf.createURI(Context.nsProject+type.getLocalPart()+"_"+count);
			}
			else
			{
				QName name=new QName ("member");
				count=ctx.getInstanceOccurences(name);
				os=ctx.vf.createURI(Context.nsProject+name.getLocalPart()+"_"+count);
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

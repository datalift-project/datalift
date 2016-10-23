package org.datalift.ows.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.openrdf.model.Resource;
import org.openrdf.rio.RDFHandlerException;


/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class TimePeriodMapper extends BaseMapper{

	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature position=(ComplexFeature) a;
				InstantPositionMapper m =new InstantPositionMapper();
				m.map(position, ctx);
			    this.rememberGmlId(position,ctx);
			}
		}
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {	
		return;
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

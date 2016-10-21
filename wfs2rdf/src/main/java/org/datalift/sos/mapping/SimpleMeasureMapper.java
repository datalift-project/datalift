package org.datalift.sos.mapping;


import javax.xml.datatype.XMLGregorianCalendar;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;
import org.openrdf.rio.RDFHandlerException;

public class SimpleMeasureMapper extends BaseMapper {

	@Override
	protected boolean mapAsIntermediate(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		return false;
	}
	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				//map amount and unit
				if(f.getTypeName().equals(Const.MeasureType))
				{
					String str_val=f.value;
					if(str_val!=null)
					{
						double val=Double.valueOf(str_val);	
						ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(Context.nsOml+"amount"), ctx.vf.createLiteral(val)));
						String str_unit =ctx.sosMetaData.get(Const.uom);
						if(Helper.isSet(str_unit))
						{
							String uriUnit=ctx.unitsSymbUri.get(str_unit);
							if(uriUnit!=null)
							{
								ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(Context.nsOml+"uom"), ctx.vf.createURI(uriUnit)));
							}

						}
					}
				}			
			}
		}
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
	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {
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
			subjectURI=Context.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsOml+"result"), cf.getId()));
		//add time result as well
		this.mapTimeResult(subjectURI, cf,ctx);		
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!cf.isSimple())
			{ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsOml+"SimpleMesure")));
		if(cf.isReferencedObject())
		{ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(Context.referencedObjectType.getLocalPart()))));}
	}}

	/**
	 * add a triple related to the time of the measure. the triple should be directly linked to the observation (and not to the current simpleMeasure)
	 * @param subjectURI the URI of the observation
	 * @param cf the feature representing the simpleMeasure
	 * @param ctx the context object
	 * @throws RDFHandlerException 
	 */
	private void mapTimeResult(Resource subjectURI, ComplexFeature cf, Context ctx) throws RDFHandlerException {
		//first of all, let extract the time of the measure
		String str_time=cf.getAttributeValue(Const.wmlTime);
		if(Helper.isSet(str_time))
		{
			XMLGregorianCalendar v=Helper.getDate(str_time);	
			if(v!=null)
			{
				ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsOml+"timeResult"), ctx.vf.createLiteral(v))); 
			}
		}
	}
}

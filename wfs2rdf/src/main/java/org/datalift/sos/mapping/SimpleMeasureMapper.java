package org.datalift.sos.mapping;


import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.datalift.geoutility.Context;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;

public class SimpleMeasureMapper extends BaseMapper {
	@Override 
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
						ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsOml+"amount"), ctx.vf.createLiteral(val)));
						String str_unit =ctx.sosMetaData.get(Const.uom);
						if(Helper.isSet(str_unit))
								{
							ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsOml+"uom"), ctx.vf.createLiteral(str_unit)));
								}
					}
				}			
			}
		}
	}
	@Override
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
		ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsOml+"result"), cf.getId()));
		//add time result as well
		this.mapTimeResult(subjectURI, cf,ctx);		
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {

		super.addRdfTypes(cf, ctx);
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsOml+"SimpleMesure")));
		if(isReferencedObject(cf))
		{ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(ctx.referencedObjectType.getLocalPart()))));}
	}

	/**
	 * add a triple related to the time of the measure. the triple should be directly linked to the observation (and not to the current simpleMeasure)
	 * @param subjectURI the URI of the observation
	 * @param cf the feature representing the simpleMeasure
	 * @param ctx the context object
	 */
	private void mapTimeResult(Resource subjectURI, ComplexFeature cf, Context ctx) {
		//first of all, let extract the time of the measure
		String str_time=cf.getAttributeValue(Const.wmlTime);
		if(Helper.isSet(str_time))
		{
			XMLGregorianCalendar v=Helper.getDate(str_time);	
			if(v!=null)
			{
				ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsOml+"timeResult"), ctx.vf.createLiteral(v))); 
			}
		}
	}
}

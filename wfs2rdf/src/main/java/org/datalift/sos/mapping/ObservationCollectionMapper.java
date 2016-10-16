package org.datalift.sos.mapping;

import javax.xml.datatype.XMLGregorianCalendar;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

public class ObservationCollectionMapper extends BaseMapper {
	private boolean mapped=false;
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		URI typeSmodURI = ctx.vf.createURI(Context.nsOml+"ObservationCollection");
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	
	}
	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) {

		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				mapped=false;
				ComplexFeature f = (ComplexFeature)a;
				if(f.name.equals(Const.omResult))
				{
					ctx.getMapper(f.name).map(f, ctx); //in this case, we base the mapping on the feature name as result has no specific type (anytype)
					mapped=true;
				}
				if(f.name.equals(Const.resultTime))
				{
					//map directly just as for observationMember
					this.mapTimeResult(cf.getId(), cf,ctx);
					mapped=true;
				}
				if(!mapped)
				{
					ctx.getMapper(f.getTypeName()).map(f, ctx); 
					mapped=false;
				}
			}
		}
	}

	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) {
		if(!alreadyLinked)
		{
			if(cf.isSimple())
			{
				super.addParentSimpleLinkStatements(cf, ctx);
				return;
			}else
			{
				this.addParentLinkStatements(cf, ctx);
			}
		}
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
				ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsOml+"timeResult"), ctx.vf.createLiteral(v))); 
			}
		}
	}
}

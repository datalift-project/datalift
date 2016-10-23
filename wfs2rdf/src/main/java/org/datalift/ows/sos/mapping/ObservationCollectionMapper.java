package org.datalift.ows.sos.mapping;

import javax.xml.datatype.XMLGregorianCalendar;

import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.utilities.Helper;
import org.datalift.ows.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.rio.RDFHandlerException;

/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class ObservationCollectionMapper extends BaseMapper {
	private boolean mapped=false;
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!cf.isSimple())
			{URI typeSmodURI = ctx.vf.createURI(Context.nsOml+"ObservationCollection");
		ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	
	}}
	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {

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
					//map directly just like for observationMember
					this.mapTimeResult(cf.getId(), f,ctx);
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
	protected void mapWithParent(ComplexFeature cf, Context ctx) throws RDFHandlerException {
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
	 * @throws RDFHandlerException 
	 */
	private void mapTimeResult(Resource subjectURI, ComplexFeature f, Context ctx) throws RDFHandlerException {
		//first of all, let extract the time of the measure
		ComplexFeature timePosition=f.findChildByType(Const.TimePositionType);
		String str_time=null;
		if(f!=null)
		{
			str_time=timePosition.value;
		}
		if(Helper.isSet(str_time))
		{
			XMLGregorianCalendar v=Helper.getDate(str_time);	
			if(v!=null)
			{
				ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsOml+"timeResult"), ctx.vf.createLiteral(v))); 
			}
		}
	}
	@Override
	protected boolean mapAsIntermediate(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		return false;
	}
}

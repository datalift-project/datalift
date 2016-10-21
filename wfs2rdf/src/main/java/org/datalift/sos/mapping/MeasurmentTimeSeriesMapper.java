package org.datalift.sos.mapping;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.datalift.wfs.wfs2.mapping.Mapper;
import org.openrdf.rio.RDFHandlerException;

public class MeasurmentTimeSeriesMapper extends BaseMapper {

	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) {
		return;
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		return;
	}
	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				if(f.getTypeName().equals(Const.TimeseriesMetadataPropertyType) || f.getTypeName().equals(Const.TVPDefaultMetadataPropertyType))
				{
					extractMetaData(f,ctx); //extract util metadata to be used in mapping later 
					mapCommunMetaData(f,ctx); //map commun data directly to the collection
				}
				if(f.name.getLocalPart().equals("point")) //map only observations (wml2:point)
				{
					Mapper m=new ObservationMemberMapper();
					m.map(f, ctx); //map using the id of collection, and some metadata extracted (exp. uom)
				}
			}
		}
	}
	private void mapCommunMetaData(ComplexFeature f, Context ctx) {

	}
	/**
	 * extract metadata related to the observation. expl : temporal extent, interpolationtype, uom
	 * @param f
	 * @param ctx
	 */
	private void extractMetaData(ComplexFeature f, Context ctx) {
		ComplexFeature cf=f.findChildByName(Const.uom);
		if(cf!=null)
		{
			for (Attribute a : cf.itsAttr) {
				if(a.getTypeName().equals(Const.UomSymbol))
				{
					ctx.sosMetaData.put(Const.uom, a.value);
				}
			}
		}
	}
	@Override
	protected boolean mapAsIntermediate(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		return false;
	}
}

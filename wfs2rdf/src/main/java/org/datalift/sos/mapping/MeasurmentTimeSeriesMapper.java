package org.datalift.sos.mapping;

import javax.xml.namespace.QName;

import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.datalift.wfs.wfs2.mapping.Mapper;

public class MeasurmentTimeSeriesMapper extends BaseMapper {

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		boolean found=false;
		if(cf.name.getLocalPart().equals("result"))
			found=true;
		if(ignore(cf))
		{
			return;
		}
		this.setCfId(cf,ctx);
		this.rememberGmlId(cf,ctx);
		this.addRdfTypes(cf, ctx);	
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
		this.mapFeatureSimpleAttributes(cf, ctx,null);
	}

	private void mapCommunMetaData(ComplexFeature f, Context ctx) {
		// TODO Auto-generated method stub

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
}

package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.URI;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;


public class GeomMapper extends BaseMapper{

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		int count=0;
		count=ctx.getInstanceOccurences(new QName(cf.vividgeom.getGeometryType()));
		URI geomType=ctx.vf.createURI(ctx.nsProject+cf.vividgeom.getGeometryType()+"_"+count);
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsIGN+"geometry"), geomType));		
		ctx.model.add(ctx.vf.createStatement(geomType, ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsIGN+"Geometry")));	
		ctx.model.add(ctx.vf.createStatement(geomType, ctx.vf.createURI(ctx.nsGeoSparql+"asWKT"), ctx.vf.createLiteral("<EPSG:" + cf.vividgeom.getSRID() + ">" + cf.vividgeom.toString())));
	}
	


}

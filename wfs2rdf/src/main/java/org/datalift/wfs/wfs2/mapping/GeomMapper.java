package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.URI;

import fr.ign.datalift.constants.GeoSPARQL;

import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.geoutility.CreateGeoStatement;


public class GeomMapper extends BaseMapper{

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		int count=0;
		count=ctx.getInstanceOccurences(new QName(cf.vividgeom.getGeometryType()));
		URI geomType=ctx.vf.createURI(ctx.nsProject+cf.vividgeom.getGeometryType()+"_"+count);
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(ctx.nsGeoSparql+"hasGeometry"), geomType));		
		ctx.model.add(ctx.vf.createStatement(geomType, ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsGeoSparql+"Geometry")));	
		String crs=CreateGeoStatement.CRS.get(String.valueOf(cf.vividgeom.getSRID()));
		if(crs==null)
		{
			crs=String.valueOf(cf.vividgeom.getSRID());
		}

		ctx.model.add(ctx.vf.createStatement(geomType, ctx.vf.createURI(ctx.nsGeoSparql+"asWKT"), ctx.vf.createLiteral("<" + crs + "> " + cf.vividgeom.toString(),GeoSPARQL.WKTLITERAL)));
	}
}

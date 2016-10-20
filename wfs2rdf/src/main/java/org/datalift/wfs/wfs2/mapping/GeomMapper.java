package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;

import fr.ign.datalift.constants.GeoSPARQL;

import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Context;
import org.datalift.utilities.CreateGeoStatement;


public class GeomMapper extends BaseMapper{

	@Override
	protected void mapGeometryProperty(ComplexFeature cf, Context ctx)
	{
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

	@Override
	protected void mapGeometryIfAny(ComplexFeature cf, Context ctx) {
		mapGeometryProperty(cf,ctx);
	}

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
		return;
	}

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		return;
	}

	@Override
	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource toLinkWith) {
		return;
	}

	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) {
		return;
	}

	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) {
		return;
	}

	@Override
	protected void mapFeatureSimpleValue(ComplexFeature cf, Context ctx) {
		return;
	}

	@Override
	protected void addParentSimpleLinkStatements(ComplexFeature cf, Context ctx) {
		return;
	}
}

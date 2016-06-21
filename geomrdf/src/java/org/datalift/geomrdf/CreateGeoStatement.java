/*
 * Copyright / Copr. IGN 2014
 * Contributor(s) : Faycal Hamdi
 *
 * Contact: hamdi.faycal@gmail.com
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.geomrdf;

import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.BNode;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;

import fr.ign.datalift.constants.CRS;
import fr.ign.datalift.constants.GeoSPARQL;
import fr.ign.datalift.constants.Geometrie;
import fr.ign.datalift.model.GeometryProperty;

public class CreateGeoStatement {

	ValueFactory vf;
	Statement geoStatement;
	List<Statement> aboutGeometry = new ArrayList<Statement>();

	public void createStatement (GeometryProperty gp, ValueFactory vf, URI feature, URI geomFeature, String geoType, String crs){

		this.vf = vf;
		
		geoStatement = vf.createStatement(feature, Geometrie.GEOMETRIE, geomFeature);
		aboutGeometry.add(geoStatement);

		//geoStatement = vf.createStatement(geomFeature, GeoSPARQL.ASWKT, vf.createLiteral("<" + CRS.IGNFCRS + "> " + gp.getValue(),GeoSPARQL.WKTLITERAL));
		geoStatement = vf.createStatement(geomFeature, GeoSPARQL.ASWKT, vf.createLiteral("<http://www.opengis.net/gml/srs/epsg.xml#" + crs + "> " + gp.getValue(),GeoSPARQL.WKTLITERAL));
		aboutGeometry.add(geoStatement);

		/*if (geoType.equals("MultiPolygon")){
			this.serializeMultipolygon(gp,geomFeature,crs);
		}

		if (geoType.equals("MultiLineString")){
			this.serializeMultiLineString(gp,geomFeature,crs);
		}

		if (geoType.equals("MultiPoint")){
			this.serializeMultiPoint(gp,geomFeature,crs);
		}

		if (geoType.equals("Polygon")){
			this.serializePolygon(gp,geomFeature,crs);
		}

		if (geoType.equals("LineString")){
			this.serializeLineString(gp,geomFeature,crs,0);
		}

		if (geoType.equals("LinearRing")){
			this.serializeLineString(gp,geomFeature,crs,0);
		}
		if (geoType.equals("Point")){
			this.serializePoint(gp,geomFeature,crs,0);
		}*/
	}

	// serialize Geometry Features into RDF

	public ValueFactory getVf() {
		return vf;
	}

	public void setVf(ValueFactory vf) {
		this.vf = vf;
	}

	public Statement getGeoStatement() {
		return geoStatement;
	}

	public void setGeoStatement(Statement geoStatement) {
		this.geoStatement = geoStatement;
	}

	public List<Statement> getAboutGeometry() {
		return aboutGeometry;
	}

	public void setAboutGeometry(List<Statement> aboutGeometry) {
		this.aboutGeometry = aboutGeometry;
	}

	protected void serializeMultipolygon(GeometryProperty gp, Resource geomFeature, String crs){
		geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.MULTIPOLYGON);
		aboutGeometry.add(geoStatement);
		this.setCrs(geomFeature,crs);
		int n = gp.getNumGeometries(); 
		for (int i = 0; i < n; i++) {
			BNode polygonMember = vf.createBNode();
			geoStatement = vf.createStatement(geomFeature, Geometrie.POLYGONMEMBER, polygonMember);
			aboutGeometry.add(geoStatement);
			this.serializePolygon(gp, polygonMember, crs);
		}
	}

	protected void serializeMultiLineString(GeometryProperty gp, Resource geomFeature, String crs){
		geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.MULTILINESTRING);
		aboutGeometry.add(geoStatement);
		this.setCrs(geomFeature,crs);
		int n = gp.getNumGeometries(); 
		for (int i = 0; i < n; i++) {
			BNode lineStringMember = vf.createBNode();
			geoStatement = vf.createStatement(geomFeature, Geometrie.LINESTRINGMEMBER, lineStringMember);
			aboutGeometry.add(geoStatement);
			this.serializeLineString(gp, lineStringMember, crs, i);
		}
	}

	protected void serializeMultiPoint(GeometryProperty gp, Resource geomFeature, String crs){
		geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.MULTIPOINT);
		aboutGeometry.add(geoStatement);
		this.setCrs(geomFeature,crs);
		int n = gp.getNumGeometries(); 
		for (int i = 0; i < n; i++) {
			BNode pointMember = vf.createBNode();
			geoStatement = vf.createStatement(geomFeature, Geometrie.POINTMEMBER, pointMember);
			aboutGeometry.add(geoStatement);
			this.serializePoint(gp, pointMember, crs, i);
		}
	}

	protected void serializePolygon(GeometryProperty gp, Resource geomFeature, String crs){
		int indexPointList = 0;
		geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.POLYGON);
		aboutGeometry.add(geoStatement);
		this.setCrs(geomFeature,crs);
		BNode exterior = vf.createBNode();
		geoStatement = vf.createStatement(geomFeature, Geometrie.EXTERIOR, exterior);
		aboutGeometry.add(geoStatement);
		this.serializeLineString(gp, exterior, crs, indexPointList);
		int n = gp.getNumInteriorRing();
		for (int i = 0; i < n; i++) {
			indexPointList++;
			BNode interiorMember = vf.createBNode();
			geoStatement = vf.createStatement(geomFeature, Geometrie.INTERIOR, interiorMember);
			aboutGeometry.add(geoStatement);
			this.serializeLineString(gp, interiorMember, crs, indexPointList);
		}
	}

	protected void serializeLineString(GeometryProperty gp, Resource geomFeature, String crs, int indexPointList){
		if (gp.getIsRing(indexPointList)) {
			geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.LINEARRING);
			aboutGeometry.add(geoStatement);
			this.setCrs(geomFeature,crs);
		}
		else {			
			// carry out the Line case (when numPoints = 2)
			if (gp.getNumPoint(indexPointList) == 2){
				geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.LINE);
				aboutGeometry.add(geoStatement);
				this.setCrs(geomFeature,crs);
			}
			else{
				geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.LINESTRING);
				aboutGeometry.add(geoStatement);
				this.setCrs(geomFeature,crs);
			}
		}
		////// Add  the line Case

		BNode points = vf.createBNode();
		geoStatement = vf.createStatement(geomFeature, Geometrie.POINTS, points);
		aboutGeometry.add(geoStatement);
		this.serializePointsList(gp, points, crs, indexPointList);
	}

	protected void serializePointsList(GeometryProperty gp, Resource geomFeature, String crs, int indexPointList){
		int n = gp.getNumPoint(indexPointList);
		int indexPoint = getCurrentIndexPoint(gp, indexPointList);
		boolean ring = gp.getIsRing(indexPointList);
		Resource firstPoint = null;
		// In the case of LinearRing the first point and the last point is the same
		if (ring) n = n - 1;
		for (int i = 0; i < n; i++) {
			geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.POINTSLIST);
			aboutGeometry.add(geoStatement);
			this.setCrs(geomFeature,crs);
			BNode first = vf.createBNode();
			if (!ring) {
				geoStatement = vf.createStatement(geomFeature, RDF.FIRST, first);
			}
			else {
				geoStatement = vf.createStatement(geomFeature, Geometrie.FIRSTANDLAST, first);
				firstPoint = geomFeature;
				ring = false;
			}
			aboutGeometry.add(geoStatement);
			this.setCrs(geomFeature,crs);
			this.serializePoint(gp, first, crs, i+indexPoint);
			if (i == n - 1) {
				if (gp.getIsRing(indexPointList)){
					geoStatement = vf.createStatement(geomFeature, RDF.REST, firstPoint);
				}
				else {
					geoStatement = vf.createStatement(geomFeature, RDF.REST, RDF.NIL);
				}
				aboutGeometry.add(geoStatement);
			}
			else {
				BNode rest = vf.createBNode();
				geoStatement = vf.createStatement(geomFeature, RDF.REST, rest);
				aboutGeometry.add(geoStatement);
				geomFeature = rest;
			}
		}
	}

	protected void serializePoint(GeometryProperty gp, Resource geomFeature, String crs, int indexPoint){
		Double[] point = gp.getPoint(indexPoint);
		geoStatement = vf.createStatement(geomFeature, RDF.TYPE, Geometrie.POINT);
		aboutGeometry.add(geoStatement);
		this.setCrs(geomFeature,crs);
		if ((point.length > 0) && (point[0] != null)) {
		    geoStatement = vf.createStatement(geomFeature, Geometrie.COORDX, vf.createLiteral(point[0].doubleValue()));
		    aboutGeometry.add(geoStatement);
		}
		if ((point.length > 1) && (point[1] != null)) {
		    geoStatement = vf.createStatement(geomFeature, Geometrie.COORDY, vf.createLiteral(point[1].doubleValue()));
		    aboutGeometry.add(geoStatement);
		}
	}

	protected int getCurrentIndexPoint(GeometryProperty gp, int currentIndexPoint){
		int indexPoint = 0;
		for (int i=0; i < currentIndexPoint; i++){
			indexPoint = indexPoint + gp.getNumPoint(i);
		}
		return indexPoint;
	}

	protected void setCrs(Resource geomFeature, String crs){
		if (crs != null){
			geoStatement = vf.createStatement(geomFeature, Geometrie.SYSTCOORD, CRS.IGNFCRS);
			aboutGeometry.add(geoStatement);
		}
	}

}

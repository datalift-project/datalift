package org.datalift.ows.wfs.wfs1_x;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

//import org.datalift.core.util.SimpleCache;
import org.datalift.fwk.log.Logger;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import fr.ign.datalift.model.AbstractFeature;
import fr.ign.datalift.model.FeatureProperty;
import fr.ign.datalift.model.GeometryProperty;

/**
 * A WFS client with an embedded parser 
 * The WFS versions supported by this class are 1.0.0 and 1.1.0
 * @author Hanane Eljabiri
 *
 */
public class WfsParser1_x {
	
	private final static Map<String,DataStore> cache = new HashMap<String, DataStore>();	
	private final static Logger log = Logger.getLogger();
	private final String bodyRequest="?service=wfs&request=getCapabilities&version=";
	private String ftCrs;
	private DataStore dataStore;
	private String version;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	public WfsParser1_x(String url, String version, String serverTypeStrategy )
	{
		this.version=version;
		String getCapabilities = url+bodyRequest+version;
		String cacheKey = getCapabilities;
		cacheKey += "/" + serverTypeStrategy;
		DataStore ds = cache.get(cacheKey);
		if (ds == null) {
			Map<Object, Object> connectionParameters = new HashMap<>();
			connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilities );
			connectionParameters.put("WFSDataStoreFactory:TIMEOUT",Integer.valueOf(900000000));
			connectionParameters.put("WFSDataStoreFactory:ENCODING","UTF-8");
			if(!serverTypeStrategy.equals("autre"))
				connectionParameters.put("WFSDataStoreFactory:WFS_STRATEGY", serverTypeStrategy); // if not specified for a mapserver => error: noxsdelement declaration found for {http://www.opengis.net/wfs}REM_NAPPE_SEDIM
			// initialisation - connection
			try {
				ds = DataStoreFinder.getDataStore( connectionParameters );
			} catch (IOException e) {
				log.error("Failed to create parser for: {} ({}, {})", e, url, version, serverTypeStrategy);
			}
			cache.put(cacheKey, ds);
		}
		this.dataStore = ds ;
	}

	public ArrayList<AbstractFeature> loadFeature(String typeName, boolean optionWGS84) throws IOException {
		ArrayList<AbstractFeature> features = new ArrayList<AbstractFeature>();
		if(dataStore!=null)
		{
			SimpleFeatureSource source = dataStore.getFeatureSource(typeName);
			String code="4326";
			Query query = new Query();
			CoordinateReferenceSystem crs=source.getInfo().getCRS();
			Iterator<ReferenceIdentifier> i = crs.getIdentifiers().iterator();
			if(i.hasNext())
			{
				code=i.next().getCode();		
			}
			this.ftCrs=code;
			if(optionWGS84)
			{	
				query.setCoordinateSystem(DefaultGeographicCRS.WGS84);
			}else
			{
				query.setCoordinateSystem(crs);
			}
			SimpleFeatureCollection fc = source.getFeatures(query); //describeft
			SimpleFeatureIterator  fiterator=fc.features();
			while(fiterator.hasNext()){
				SimpleFeature sf = fiterator.next();
				addFeature(sf,crs,features);        
			}
		}
		return features;
	}

	public void addFeature(SimpleFeature sf,CoordinateReferenceSystem crs,ArrayList<AbstractFeature> features){

		/******Fayçal's readfeaturecollection method + some edits***/
		fr.ign.datalift.model.AbstractFeature ft = new AbstractFeature();

		Collection<Property> propColl = sf.getProperties();
		for (Iterator<Property> iterator = propColl.iterator(); iterator.hasNext();) {
			Property prop = iterator.next();
			if (!prop.getName().toString().equals("metaDataProperty")
					&& !prop.getName().toString().equals("description")
					&& !prop.getName().toString().equals("name")
					&& !prop.getName().toString().equals("boundedBy")
					&& !prop.getName().toString().equals("location")) {

				// checks if the property value is not null
				if (prop.getValue() != null) {

					// check if Property is GeometryProperty
					if (prop.getType() instanceof GeometryTypeImpl) {
						GeometryProperty gp = new GeometryProperty();

						Geometry geom = (Geometry) sf.getDefaultGeometry();

						Iterator<ReferenceIdentifier> i = crs.getIdentifiers().iterator();
						if(i.hasNext())
							geom.setSRID( Integer.parseInt(i.next().getCode()));   					
						// Parse MultiPolygon
						if (geom instanceof MultiPolygon) {
							MultiPolygon mp = (MultiPolygon) geom;
							gp.setType(new StringBuilder(new String(mp.getGeometryType().getBytes(),UTF8_CHARSET)).toString());
							parseMultiPolygon(gp,mp);
						}
						// Parse MultiLineString
						if (geom instanceof MultiLineString) {
							MultiLineString mls = (MultiLineString) geom;
							gp.setType(new StringBuilder(new String(mls.getGeometryType().getBytes(),UTF8_CHARSET)).toString());
							parseMultiLineString(gp,mls);
						}
						// Parse MultiPoint
						if (geom instanceof MultiPoint) {
							MultiPoint mpt = (MultiPoint) geom;
							gp.setType(new StringBuilder(new String(mpt.getGeometryType().getBytes(),UTF8_CHARSET)).toString());
							parseMultiPoint(gp,mpt);
						}
						// Parse Polygon
						if (geom instanceof Polygon) {
							Polygon polygon = (Polygon) geom;
							gp.setType(new StringBuilder(new String(polygon.getGeometryType().getBytes(),UTF8_CHARSET)).toString());
							parsePolygon(gp,polygon);
						}
						// Parse LineString
						if (geom instanceof LineString) {							
							LineString ls = (LineString) geom;
							gp.setType(new StringBuilder(new String(ls.getGeometryType().getBytes(),UTF8_CHARSET)).toString());
							parseLineString(gp,ls);
						}
						// Parse LinearRing
						if (geom instanceof LinearRing) {							
							LinearRing lr = (LinearRing) geom;
							gp.setType(new StringBuilder(new String(lr.getGeometryType().getBytes(),UTF8_CHARSET)).toString());
							parseLineString(gp,lr);
						}
						// Parse Point							
						if (geom instanceof Point) {
							Point pt = (Point) geom;
							gp.setType(new StringBuilder(new String(pt.getGeometryType().getBytes(),UTF8_CHARSET)).toString());
							parsePoint(gp,pt);
						}
						gp.setName(new StringBuilder(new String(prop.getName().toString().getBytes(),UTF8_CHARSET)).toString());
						gp.setValue(new StringBuilder(new String(geom.toString().getBytes(),UTF8_CHARSET)).toString());

						ft.addProperty(gp);

					} else {
						FeatureProperty fp = new FeatureProperty();
						fp.setName(new StringBuilder(new String(prop.getName().toString().getBytes(),UTF8_CHARSET)).toString());
						if (prop.getType().toString().contains("Float") || (prop.getType().toString().contains("Double"))) {
							fp.setDoubleValue(Double.parseDouble(prop.getValue().toString()));
							fp.setType("double");
							ft.addProperty(fp);
						}
						if (prop.getType().toString().contains("Integer") || (prop.getType().toString().contains("Long"))) {
							fp.setIntValue(Integer.parseInt(prop.getValue().toString()));
							fp.setType("int");
							ft.addProperty(fp);
						}
						else  {
							fp.setValue(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());
							ft.addProperty(fp);
						}

						if(prop.getName().toString().equals("name")){
							ft.setLabel(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());

						} if(prop.getName().toString().contains("label")){
							ft.setLabel(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());
						}

					}
				}
			} 
		}
		features.add(ft);
	}

	/***geometry parsers****/
	protected void parseMultiPolygon(GeometryProperty gp, MultiPolygon mp){
		int numGeometries = mp.getNumGeometries();
		gp.setNumGeometries(numGeometries);
		for (int i=0; i<numGeometries ; i++){
			parsePolygon(gp,(Polygon)mp.getGeometryN(i));
		}
	}

	protected void parseMultiLineString(GeometryProperty gp, MultiLineString mls){
		int numGeometries = mls.getNumGeometries();
		gp.setNumGeometries(numGeometries);
		for (int i=0; i<numGeometries ; i++){
			parseLineString(gp,(LineString)mls.getGeometryN(i));
		}
	}

	protected void parseMultiPoint(GeometryProperty gp, MultiPoint mpt){
		int numGeometries = mpt.getNumGeometries();
		gp.setNumGeometries(numGeometries);
		for (int i=0; i<numGeometries ; i++){
			parsePoint(gp,(Point)mpt.getGeometryN(i));
		}
	}

	protected void parsePolygon(GeometryProperty gp, Polygon polygon){
		parseLineString(gp,polygon.getExteriorRing());
		int numInteriorRing = polygon.getNumInteriorRing();
		gp.setNumInteriorRing(numInteriorRing);
		for (int i=0; i<numInteriorRing ; i++){
			parseLineString(gp,polygon.getInteriorRingN(i));
		}
	}

	protected void parseLineString(GeometryProperty gp, LineString ls){
		// in the case of LinearRing, setIsRing true
		gp.setIsRing(ls.isClosed());
		int numPoint = ls.getNumPoints();
		gp.setNumPoint(numPoint);
		for (int i=0; i<numPoint ; i++){
			Point pt = ls.getPointN(i);
			parsePoint(gp,pt);
		}
	}

	protected void parsePoint(GeometryProperty gp, Point p){
		Double[] pt = { Double.valueOf(p.getX()), Double.valueOf(p.getY()) };
		gp.setPointsLists(pt);
	}

	/******getters and setters***/
	public String getFtCrs() {
		return ftCrs;
	}

	public void setFtCrs(String ftCrs) {
		this.ftCrs = ftCrs;
	}

	public DataStore getDataStore() {
		return dataStore;
	}

	public void getDataStore(DataStore data) {
		this.dataStore = data;
	}

	public String getVersion() {
		return version;
	}

	public void setVersion(String version) {
		this.version=version;
	}
}

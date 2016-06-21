package org.datalift.parsingTools;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.Query;
import org.geotools.data.ResourceInfo;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;

import org.opengis.feature.type.Name;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
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

import org.opengis.feature.Property;

//import fr.ign.datalift.model.AbstractFeature;

import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.data.wfs.WFSDataStore;
import org.geotools.data.wfs.WFSDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.filter.identity.FeatureIdImpl;



public class WfsParser {
	

	private ArrayList<AbstractFeature> features;
	private String crs;
	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");
	
	public WfsParser() {
		features = new ArrayList<AbstractFeature>();
		// TODO Auto-generated constructor stub
	}
	
	
	public ArrayList<AbstractFeature> getFeatures() {
		return features;
	}


	public void setFeatures(ArrayList<AbstractFeature> features) {
		this.features = features;
	}


	public void addFeature(SimpleFeature sf,CoordinateReferenceSystem crs){
	   	/*Geometry geom = (Geometry) sf.getDefaultGeometry();
    	Iterator<ReferenceIdentifier> i = crs.getIdentifiers().iterator();
    	if(i.hasNext())
    	   	geom.setSRID( Integer.parseInt(i.next().getCode()));   	
        System.out.println(sf.getAttribute("CODE_REG"));
        Object o=sf.getAttribute("the_geom");
        System.out.println(sf.getAttribute("the_geom"));
        System.out.println(sf.getAttribute("NOM_REGION"));*/
        
		fr.ign.datalift.model.AbstractFeature ft = new AbstractFeature();
        
        
        /******Fayçal's readfeaturecollection method + some edits***/
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
					

						// Parse GeometryCollection (MultiLineString, MultiPoint, MultiPolygon)							
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

						//gp.setValue(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());
						//gp.setType(new StringBuilder(new String(prop.getDescriptor().getType().getBinding().getSimpleName().getBytes(),UTF8_CHARSET)).toString());

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
	
	/****
	 * send a request to wfs, parse the response, 
	 * retrieves data (features) and insert them into features list 
	 * @param wfsUrl
	 */
	public void getDataWFS(String wfsUrl)
	{
			
		String getCapabilities = wfsUrl;
				//"http://geoservices.brgm.fr/risques?service=WFS&version=1.0.0&request=GetCapabilities"
		Map connectionParameters = new HashMap();
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilities);
		connectionParameters.put("WFSDataStoreFactory:WFS_STRATEGY", "mapserver");
		WFSDataStoreFactory  dsf = new WFSDataStoreFactory();
		try {
		    WFSDataStore dataStore = dsf.createDataStore(connectionParameters);
		    //getdatastore
		   
		    List<Name> list= dataStore.getNames();
		    
		    //SimpleFeatureSource source = dataStore.getFeatureSource("ef_EnvironmentalMonitoringFacility");
		    //SimpleFeatureSource source = dataStore.getFeatureSource("hanane_workspace_regions_nouvelles");
		     SimpleFeatureSource source = dataStore.getFeatureSource("ms_JDD_2060139");
		  
		    
		    /* FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
	        Set<FeatureId> fids = new HashSet<FeatureId>();
	        fids.add(new FeatureIdImpl("Piezometre.1.01143X0062-F"));
	        Query query = new Query("ef_EnvironmentalMonitoringFacility", ff.id(fids));*/
	        
	        Query query = new Query();
		    //query.setMaxFeatures(1);
		    
		    //FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2( GeoTools.getDefaultHints() );
		    //FeatureId id= ff.featureId("Piezometre.1.01143X0062-F");
		    
		    //query.setFilter(ff.id(id));
		    //query.setStartIndex(1);
		    //query.setTypeName("ef_EnvironmentalMonitoringFacility");
		    	    
		    CoordinateReferenceSystem crs=source.getInfo().getCRS();
		    query.setCoordinateSystem(source.getInfo().getCRS());
		  
		    SimpleFeatureCollection fc = source.getFeatures(query); //describeft
		    SimpleFeatureIterator  fiterator=fc.features();
		    while(fiterator.hasNext()){
		    	SimpleFeature sf = fiterator.next();
		    	addFeature(sf,crs);
		    	System.out.println(sf.getName());		         
		    }
		   
		} catch (IOException ex) {
		    ex.printStackTrace();
		}
	
		   
	}
	
	public List<FeatureTypeDescription> getfeatureTypeDescription()
	{
		List<FeatureTypeDescription> descriptor=new ArrayList<FeatureTypeDescription>();
		//String getCapabilitiesUrl="https://ids.craig.fr/wxs/public/wfs?request=getcapabilities"; // Exception in thread "main" java.lang.UnsupportedOperationException: implement! : 177 response returned!
		//String getCapabilitiesUrl="http://geoservices.brgm.fr/risques?service=WFS&request=Getcapabilities"; //18
		//String getCapabilitiesUrl="https://wfspoc.brgm-rec.fr/geoserver/ows?service=wfs&request=GetCapabilities";
		String getCapabilitiesUrl="http://ows.region-bretagne.fr/geoserver/rb/wfs?service=wfs&request=getcapabilities&version=1.0.0";
		String version="1.0.0";
		String strategy="geoserver";

		Map connectionParameters = new HashMap();
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilitiesUrl+"&version="+version );

		if(!version.equals("2.0.0"))
			connectionParameters.put("WFSDataStoreFactory:WFS_STRATEGY", strategy); // if not specified for a mapserver => error: noxsdelement declaration found for {http://www.opengis.net/wfs}REM_NAPPE_SEDIM
		// Step 2 - connection
		DataStore data;
		try {
			data = DataStoreFinder.getDataStore( connectionParameters );
			// Step 3 - discouvery
			String typeNames[] = data.getTypeNames();
			for (String typeName : typeNames) {
				SimpleFeatureSource source = data.getFeatureSource(typeName); 
				ResourceInfo inf= source.getInfo();
				FeatureTypeDescription ftd=new FeatureTypeDescription();
				Iterator<ReferenceIdentifier> i = inf.getCRS().getIdentifiers().iterator();
				if(i.hasNext())
					ftd.setEpsgSrs(i.next().getCode());
				ftd.setName(inf.getName()); //name pattern ns_featuretypename
				ftd.setSummary(inf.getDescription());
				ftd.setTitle(inf.getTitle());
				int numberreturned;
				try{
				numberreturned=source.getCount(null);
				ftd.setNumberFeature(numberreturned);
				}catch (Exception ee)
				{	//oups! i can't execute the request to get feature number! Bad version has been specified
					ftd.setNumberFeature(-1);
					
				}
				//only add the feature type if it is available using the parameter specified (version and strategy)
				if(ftd.getNumberFeature()!=-1)
					descriptor.add(ftd);

			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}



		return descriptor;
	}


	public  void tryGetDataStore() throws IOException
	{
		//String getCapabilities = "http://ogc.geo-ide.developpement-durable.gouv.fr/cartes/mapserv?map=/opt/data/carto/geoide-catalogue/REG042A/JDD.www.map&service=WfS&request=GetCapabilities&version=1.1.0"; //enmptyfeaturereader
		//String getCapabilities = "http://ows.region-bretagne.fr/geoserver/rb/wfs?service=wfs&request=getcapabilities&version=1.0.0"; //ok 
		//String getCapabilities = "http://geoservices.brgm.fr/risques?service=WFS&request=Getcapabilities"; //ok
		//String getCapabilities = "https://wfspoc.brgm-rec.fr/geoserver/ows?service=wfs&version=2.0.0&request=GetCapabilities"; //net.opengis.wfs20.impl.WFSCapabilitiesTypeImpl cannot be cast to net.opengis.wfs.WFSCapabilitiesType
		//String getCapabilities = "http://cartographie.aires-marines.fr/wfs?service=wfs&request=getcapabilities&version=2.0.0"; //client does not support any of the server supported output format
		//String getCapabilities = "http://ids.craig.fr/wxs/public/wfs?request=getcapabilitie&version=2.0.0";  //ok for v1.1 and v1.0
		String getCapabilities = "http://localhost:8081/geoserver/hanane_workspace/ows?service=WFS&version=2.0.0&request=Getcapabilities";  
		Map connectionParameters = new HashMap();
		String version="";
		String code="";
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL", getCapabilities );
		//connectionParameters.put("WFSDataStoreFactory:WFS_STRATEGY", "geoserver");
		//map.put (WFSDataStoreFactory.URL.key, "....");

		// Step 2 - connection
		DataStore data = DataStoreFinder.getDataStore( connectionParameters );

		// Step 3 - discouvery
		String typeNames[] = data.getTypeNames();
		String typeName = typeNames[0];
		SimpleFeatureType schema = data.getSchema( typeName );

		// Step 4 - target
		SimpleFeatureSource source = data.getFeatureSource("hanane_workspace_regions_nouvelles");
		//System.out.println( "Metadata Bounds:"+ source.getBounds() );

		// Step 5 - query
		

		Query query = new Query(  );
		 CoordinateReferenceSystem crs=source.getInfo().getCRS();
		 Iterator<ReferenceIdentifier> i = crs.getIdentifiers().iterator();
			if(i.hasNext())
				i.next().getCode();
			if (version.equals("1.1.0") || version.equals("2.0.0"))
			{
				this.crs="urn:x-ogc:def:crs:EPSG:"+code;
			}
			else
			{
				this.crs="http://www.opengis.net/gml/srs/epsg.xml#"+code;
			}
		    query.setCoordinateSystem(crs);
		    query.setMaxFeatures(100);
		    
		 SimpleFeatureCollection fc = source.getFeatures(query); //describeft
		    SimpleFeatureIterator  fiterator=fc.features();
		    while(fiterator.hasNext()){
		    	SimpleFeature sf = fiterator.next();
		    	addFeature(sf,crs);
		    	System.out.println(sf.getName());		         
		    }
		   
	}

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


	public String getCRs() {
		// TODO Auto-generated method stub
		return crs;
	}

}

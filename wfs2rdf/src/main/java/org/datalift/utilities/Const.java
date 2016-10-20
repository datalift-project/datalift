package org.datalift.utilities;

import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import javax.xml.namespace.QName;

public class Const {
	public final static String GML_3_2_NS_URI = "http://www.opengis.net/gml/3.2";

	public final static  QName MemberPropertyType= new QName("http://www.opengis.net/wfs/2.0","MemberPropertyType");
	
	public final static  QName EnvironmentalMonitoringFacilityType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","EnvironmentalMonitoringFacilityType");
	public final static  QName ID= new QName(W3C_XML_SCHEMA_NS_URI,"ID");
	public final static  QName StringOrRefType= new QName("http://www.opengis.net/gml/3.2","StringOrRefType");

	public final static  QName CodeWithAuthorityType= new QName("http://www.opengis.net/gml/3.2","CodeWithAuthorityType");
	public final static  QName anyURI= new QName(W3C_XML_SCHEMA_NS_URI,"anyURI");
	public final static  QName IdentifierPropertyType= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","IdentifierPropertyType");
	public final static  QName IdentifierType= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","IdentifierType");
	public final static  QName string= new QName(W3C_XML_SCHEMA_NS_URI,"string");
	public final static  QName ReferenceType= new QName("http://www.opengis.net/gml/3.2","ReferenceType");
	public final static  QName hrefType= new QName("http://www.w3.org/1999/xlink","hrefType");
	public final static  QName titleAttrType= new QName("http://www.w3.org/1999/xlink","titleAttrType");
	public final static  QName xsdBoolean= new QName(W3C_XML_SCHEMA_NS_URI,"boolean");

	public final static  QName AbstractMemberType= new QName("http://www.opengis.net/gml/3.2","AbstractMemberType");

	public final static  QName anyType= new QName(W3C_XML_SCHEMA_NS_URI,"anyType");
	public final static  QName AbstractMonitoringObjectPropertyType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","AbstractMonitoringObjectPropertyType");
	public final static  QName OM_ObservationPropertyType= new QName("http://www.opengis.net/om/2.0","OM_ObservationPropertyType");
	public final static  QName PointPropertyType= new QName("http://www.opengis.net/gml/3.2","PointPropertyType");
	public final static  QName positiveInteger= new QName(W3C_XML_SCHEMA_NS_URI,"positiveInteger");
	public final static  QName DirectPositionType= new QName("http://www.opengis.net/gml/3.2","DirectPositionType");
	public final static QName timePosition= new QName("http://www.opengis.net/gml/3.2","timePosition");
	public final static  QName TimePositionType= new QName("http://www.opengis.net/gml/3.2","TimePositionType");
	public final static QName PointType= new QName("http://www.opengis.net/gml/3.2","PointType");
	public final static QName NetworkFacilityType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","NetworkFacilityType");
	public final static QName TimePeriodType= new QName("http://www.opengis.net/gml/3.2","TimePeriodType");

	/******final *attribute's names***************/

	public final static QName member= new QName("http://www.opengis.net/wfs/2.0","member");
	public final static QName type= new QName("http://www.w3.org/1999/xlink","type");
    public final static QName explicitType= new QName("http://www.w3.org/2001/XMLSchema-instance","type");
	public final static QName EnvironmentalMonitoringFacility= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","EnvironmentalMonitoringFacility");

	public final static QName description= new QName("http://www.opengis.net/gml/3.2","description");

	public final static QName identifier= new QName("http://www.opengis.net/gml/3.2","identifier");
	public final static QName codeSpace= new QName("","codeSpace");
	public final static QName inspireId= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","inspireId");
	public final static QName Identifier= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","Identifier");
	public final static QName localId= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","localId");
	public final static QName namespace= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","namespace");
	public final static QName versionId= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","versionId");
	public final static QName additionalDescription= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","additionalDescription");
	public final static QName mediaMonitored= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","mediaMonitored");
	public final static QName href= new QName("http://www.w3.org/1999/xlink","href");
	public final static QName title= new QName("http://www.w3.org/1999/xlink","title");
	public final static QName owns= new QName("","owns");

	public final static QName legalBackground= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","legalBackground");

	public final static QName onlineResource= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","onlineResource");
	public final static QName purpose= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","purpose");

	public final static QName broader= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","broader");

	public final static QName supersedes= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","supersedes");

	public final static QName supersededBy= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","supersededBy");

	public final static QName reportedTo= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","reportedTo");
	public final static QName nil= new QName("http://www.w3.org/2001/XMLSchema-instance","nil");
	public final static QName nilReason= new QName("","nilReason");
	public final static QName hasObservation= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","hasObservation");

	public final static QName involvedIn= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","involvedIn");

	public final static QName representativePoint= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","representativePoint");

	public final static QName Point= new QName("http://www.opengis.net/gml/3.2","Point");

	public final static QName srsDimension= new QName("","srsDimension");
	public final static QName srsName= new QName("","srsName");
	public final static QName pos= new QName("http://www.opengis.net/gml/3.2","pos");
	public final static QName measurementRegime= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","measurementRegime");

	public final static QName mobile= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","mobile");
	public final static QName resultAcquisitionSource= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","resultAcquisitionSource");

	public final static QName specialisedEMFType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","specialisedEMFType");

	public final static QName OperationalActivityPeriod= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","OperationalActivityPeriod");

	public final static QName activityTime= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","activityTime");


	public final static QName TimePeriod= new QName("http://www.opengis.net/gml/3.2","TimePeriod");
	public final static QName id= new QName("http://www.opengis.net/gml/3.2","id");

	public final static QName beginPosition= new QName("http://www.opengis.net/gml/3.2","beginPosition");

	public final static QName endPosition= new QName("http://www.opengis.net/gml/3.2","endPosition");

	public final static QName relatedTo= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","relatedTo");

	public final static QName belongsTo= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","belongsTo");
	public final static QName NetworkFacility= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","NetworkFacility");
 
	public final static QName linkingTime= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","linkingTime");
	public final static QName contains= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","contains");
	public final static QName name=new QName ("http://inspire.ec.europa.eu/schemas/ef/4.0","name");

	public final static QName geometry= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","geometry");
	public final static QName GeometryPropertyType= new QName("http://www.opengis.net/gml/3.2","GeometryPropertyType");
	//gml3.final 1 types 
	public final static QName FeaturePropertyType= new QName("http://www.opengis.net/gml/3.2","FeaturePropertyType");
	/****fake qname****/
	public final static QName inspireCodeList= new QName("http://inspire.ec.europa.eu/codelist","codeList");
	
	
	public final static QName FeatureArrayPropertyType= new QName("http://www.opengis.net/gml","FeatureArrayPropertyType");
	public final static QName BoundingShapeType= new QName("http://www.opengis.net/gml","BoundingShapeType");
	/*----- ---------------------------------------------------------*/
	/****capabilities****/
	public final static QName FeatureType= new QName("http://www.opengis.net/wfs/2.0","FeatureType");
	public final static QName FeatureTypeType= new QName("http://www.opengis.net/wfs/2.0","FeatureTypeType");
	public final static QName FeatureTypeList= new QName("http://www.opengis.net/wfs/2.0","FeatureTypeList");

	public final static QName Name= new QName("http://www.opengis.net/wfs/2.0","Name");
	public final static QName QName= new QName(W3C_XML_SCHEMA_NS_URI,"QName");
	public final static QName Title= new QName("http://www.opengis.net/wfs/2.0","Title");

	public final static QName Abstract= new QName("http://www.opengis.net/wfs/2.0","Abstract");

	public final static QName Keywords= new QName("http://www.opengis.net/ows/1.1","Keywords");
	public final static QName KeywordsType= new QName("http://www.opengis.net/ows/1.1","KeywordsType");
	public final static QName DefaultCRS= new QName("http://www.opengis.net/wfs/2.0","DefaultCRS");

	public final static QName OtherCRS= new QName("http://www.opengis.net/wfs/2.0","OtherCRS");

	public final static QName WGS84BoundingBox= new QName("http://www.opengis.net/ows/1.1","WGS84BoundingBox");
	public final static QName WGS84BoundingBoxType= new QName("http://www.opengis.net/ows/1.1","WGS84BoundingBoxType");
	public final static QName MetadataURL= new QName("http://www.opengis.net/wfs/2.0","MetadataURL");
	public final static QName MetadataURLType= new QName("http://www.opengis.net/wfs/2.0","MetadataURLType");

	public final static QName CurvePropertyType= new QName("http://www.opengis.net/gml/3.2","CurvePropertyType");
	public final static QName Curve= new QName("http://www.opengis.net/gml/3.2","Curve");
	public final static QName CurveType= new QName("http://www.opengis.net/gml/3.2","CurveType");
	public final static QName segments= new QName("http://www.opengis.net/gml/3.2","segments");
	public final static QName CurveSegmentArrayPropertyType= new QName("http://www.opengis.net/gml/3.2","CurveSegmentArrayPropertyType");
	public final static QName LineStringSegment= new QName("http://www.opengis.net/gml/3.2","LineStringSegment");
	public final static QName LineStringSegmentType= new QName("http://www.opengis.net/gml/3.2","LineStringSegmentType");
	public final static QName posList= new QName("http://www.opengis.net/gml/3.2","posList");
	public final static QName DirectPositionListType= new QName("http://www.opengis.net/gml/3.2","DirectPositionListType");

	public final static QName Polygon= new QName("http://www.opengis.net/gml/3.2","Polygon");
	public final static QName PolygonType= new QName("http://www.opengis.net/gml/3.2","PolygonType");
	public final static QName exterior= new QName("http://www.opengis.net/gml/3.2","exterior");
	public final static QName AbstractRingPropertyType= new QName("http://www.opengis.net/gml/3.2","AbstractRingPropertyType");

	/***geofinal metry aggregation types****/

	public final static QName MultiSurfacePropertyType= new QName("http://www.opengis.net/gml/3.2","MultiSurfacePropertyType");
	public final static QName MultiSurface= new QName("http://www.opengis.net/gml/3.2","MultiSurface");
	public final static QName MultiSurfaceType= new QName("http://www.opengis.net/gml/3.2","MultiSurfaceType");
	public final static QName SurfacePropertyType=new QName("http://www.opengis.net/gml/3.2","SurfacePropertyType");

	/****Sifinal mple types*****/
	public final static QName xsdDouble= new QName(W3C_XML_SCHEMA_NS_URI,"double");
	public final static QName xsdInteger= new QName(W3C_XML_SCHEMA_NS_URI,"integer");
	public final static QName xsdDecimal= new QName(W3C_XML_SCHEMA_NS_URI,"decimal");
	public final static QName xsdFloat= new QName(W3C_XML_SCHEMA_NS_URI,"float");
	public final static QName xsdDate= new QName(W3C_XML_SCHEMA_NS_URI,"date");

	/*****sfinal os temp***/
	public final static QName ContentsType= new QName("http://www.opengis.net/sos/2.0","ContentsType");

	public final static QName offering= new QName("http://www.opengis.net/swes/2.0","offering");

	public final static QName ObservationOffering= new QName("http://www.opengis.net/sos/2.0","ObservationOffering");
	public final static QName ObservationOfferingType= new QName("http://www.opengis.net/sos/2.0","ObservationOfferingType");
	public final static QName descriptionSWES= new QName("http://www.opengis.net/swes/2.0","description");
	public final static QName identifierSWES= new QName("http://www.opengis.net/swes/2.0","identifier");
	public final static QName nameSWES= new QName("http://www.opengis.net/swes/2.0","name");
	public final static QName responseFormat= new QName("http://www.opengis.net/sos/2.0","responseFormat");

	public final static QName OM_Observation= new QName("http://www.opengis.net/om/2.0","OM_Observation");
	public final static QName OM_ObservationType= new QName("http://www.opengis.net/om/2.0","OM_ObservationType");
	public final static QName phenomenonTime= new QName("http://www.opengis.net/om/2.0","phenomenonTime");
	public final static QName TimeObjectPropertyType= new QName("http://www.opengis.net/om/2.0","TimeObjectPropertyType");
	public final static QName resultTime= new QName("http://www.opengis.net/om/2.0","resultTime");
	public final static QName TimeInstantPropertyType= new QName("http://www.opengis.net/gml/3.2","TimeInstantPropertyType");
	public final static QName observedProperty= new QName("http://www.opengis.net/om/2.0","observedProperty");

	public final static QName procedure= new QName("http://www.opengis.net/om/2.0","procedure");
	public final static QName OM_ProcessPropertyType= new QName("http://www.opengis.net/om/2.0","OM_ProcessPropertyType");
	public final static QName parameter= new QName("http://www.opengis.net/om/2.0","parameter");
	public final static QName NamedValuePropertyType= new QName("http://www.opengis.net/om/2.0","NamedValuePropertyType");

	public final static QName featureOfInterest= new QName("http://www.opengis.net/om/2.0","featureOfInterest");

	public final static QName result= new QName("http://www.opengis.net/om/2.0","result");
	public final static QName MeasurementTimeseries= new QName("http://www.opengis.net/waterml/2.0","MeasurementTimeseries");
	public final static QName MeasurementTimeseriesType= new QName("http://www.opengis.net/waterml/2.0","MeasurementTimeseriesType");
	public final static QName metadata= new QName("http://www.opengis.net/waterml/2.0","metadata");
	public final static QName TimeseriesMetadataPropertyType= new QName("http://www.opengis.net/waterml/2.0","TimeseriesMetadataPropertyType");
	public final static QName defaultPointMetadata= new QName("http://www.opengis.net/waterml/2.0","defaultPointMetadata");
	public final static QName TVPDefaultMetadataPropertyType= new QName("http://www.opengis.net/waterml/2.0","TVPDefaultMetadataPropertyType");
	public final static QName point= new QName("http://www.opengis.net/waterml/2.0","point");
	public final static QName omResult= new QName("http://www.opengis.net/om/2.0","result");
	public final static QName MeasurementTVP= new QName("http://www.opengis.net/waterml/2.0","MeasurementTVP");
	public final static QName MeasureTVPType= new QName("http://www.opengis.net/waterml/2.0","MeasureTVPType");
	public final static QName uom= new QName("http://www.opengis.net/waterml/2.0","uom");
	public final static QName UnitReference= new QName("http://www.opengis.net/swe/2.0","UnitReference");
	public final static QName interpolationType= new QName("http://www.opengis.net/waterml/2.0","interpolationType");
	public final static QName wmlTime= new QName("http://www.opengis.net/waterml/2.0","time");

	public final static QName wmlValue= new QName("http://www.opengis.net/waterml/2.0","value");
	public final static QName MeasureType= new QName("http://www.opengis.net/waterml/2.0","MeasureType");
	public final static QName UomSymbol= new QName("http://www.opengis.net/swe/2.0","UomSymbol");
	
	


	/******final exception****/
	public final static QName exception= new QName("http://schemas.opengis.net/ows/1.1.0/owsExceptionReport.xsd","Exception");
	public final static QName exceptionReport= new QName("http://www.opengis.net/ows/1.1","ExceptionReport");
	 
	

	
	
	



}

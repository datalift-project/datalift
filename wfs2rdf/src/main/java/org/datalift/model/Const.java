package org.datalift.model;

import javax.xml.namespace.QName;

public class Const {
	
	public static  QName MemberPropertyType= new QName("http://www.opengis.net/wfs/2.0","MemberPropertyType");
	
	public static  QName EnvironmentalMonitoringFacilityType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","EnvironmentalMonitoringFacilityType");
	public static  QName ID= new QName("http://www.w3.org/2001/XMLSchema","ID");
	public static  QName StringOrRefType= new QName("http://www.opengis.net/gml/3.2","StringOrRefType");
	
	public static  QName CodeWithAuthorityType= new QName("http://www.opengis.net/gml/3.2","CodeWithAuthorityType");
	public static  QName anyURI= new QName("http://www.w3.org/2001/XMLSchema","anyURI");
	public static  QName IdentifierPropertyType= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","IdentifierPropertyType");
	public static  QName IdentifierType= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","IdentifierType");
	public static  QName string= new QName("http://www.w3.org/2001/XMLSchema","string");
	public static  QName ReferenceType= new QName("http://www.opengis.net/gml/3.2","ReferenceType");
	public static  QName hrefType= new QName("http://www.w3.org/1999/xlink","hrefType");
	public static  QName titleAttrType= new QName("http://www.w3.org/1999/xlink","titleAttrType");
	public static  QName bool= new QName("http://www.w3.org/2001/XMLSchema","boolean");
	
	public static  QName AbstractMemberType= new QName("http://www.opengis.net/gml/3.2","AbstractMemberType");
	
	public static  QName anyType= new QName("http://www.w3.org/2001/XMLSchema","anyType");
	public static  QName AbstractMonitoringObjectPropertyType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","AbstractMonitoringObjectPropertyType");
	public static  QName OM_ObservationPropertyType= new QName("http://www.opengis.net/om/2.0","OM_ObservationPropertyType");
	public static  QName PointPropertyType= new QName("http://www.opengis.net/gml/3.2","PointPropertyType");
	public static  QName positiveInteger= new QName("http://www.w3.org/2001/XMLSchema","positiveInteger");
	public static  QName DirectPositionType= new QName("http://www.opengis.net/gml/3.2","DirectPositionType");
	public static  QName TimePositionType= new QName("http://www.opengis.net/gml/3.2","TimePositionType");
	public static QName PointType= new QName("http://www.opengis.net/gml/3.2","PointType");
	public static QName NetworkFacilityType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","NetworkFacilityType");
	public static QName TimePeriodType= new QName("http://www.opengis.net/gml/3.2","TimePeriodType");

	/*******attribute's names***************/
	
	public static QName member= new QName("http://www.opengis.net/wfs/2.0","member");
	public static QName type= new QName("http://www.w3.org/1999/xlink","type");
	public static QName EnvironmentalMonitoringFacility= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","EnvironmentalMonitoringFacility");
	
	public static QName description= new QName("http://www.opengis.net/gml/3.2","description");
	
	public static QName identifier= new QName("http://www.opengis.net/gml/3.2","identifier");
	public static QName codeSpace= new QName("","codeSpace");
	public static QName inspireId= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","inspireId");
	public static QName Identifier= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","Identifier");
	public static QName localId= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","localId");
	public static QName namespace= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","namespace");
	public static QName versionId= new QName("http://inspire.ec.europa.eu/schemas/base/3.3","versionId");
	public static QName additionalDescription= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","additionalDescription");
	public static QName mediaMonitored= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","mediaMonitored");
	public static QName href= new QName("http://www.w3.org/1999/xlink","href");
	public static QName title= new QName("http://www.w3.org/1999/xlink","title");
	public static QName owns= new QName("","owns");
	
	public static QName legalBackground= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","legalBackground");

	public static QName onlineResource= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","onlineResource");
	public static QName purpose= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","purpose");

	public static QName broader= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","broader");
	
	public static QName supersedes= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","supersedes");
	
	public static QName supersededBy= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","supersededBy");
	
	public static QName reportedTo= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","reportedTo");
	public static QName nil= new QName("http://www.w3.org/2001/XMLSchema-instance","nil");
	public static QName nilReason= new QName("","nilReason");
	public static QName hasObservation= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","hasObservation");
	
	public static QName involvedIn= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","involvedIn");

	public static QName representativePoint= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","representativePoint");

	public static QName Point= new QName("http://www.opengis.net/gml/3.2","Point");
	
	public static QName srsDimension= new QName("","srsDimension");
	public static QName srsName= new QName("","srsName");
	public static QName pos= new QName("http://www.opengis.net/gml/3.2","pos");
	public static QName measurementRegime= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","measurementRegime");

	public static QName mobile= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","mobile");
	public static QName resultAcquisitionSource= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","resultAcquisitionSource");

	public static QName specialisedEMFType= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","specialisedEMFType");

	public static QName OperationalActivityPeriod= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","OperationalActivityPeriod");
	
	public static QName activityTime= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","activityTime");
	
	
	public static QName TimePeriod= new QName("http://www.opengis.net/gml/3.2","TimePeriod");
	public static QName id= new QName("http://www.opengis.net/gml/3.2","id");
	
	public static QName beginPosition= new QName("http://www.opengis.net/gml/3.2","beginPosition");
	
	public static QName endPosition= new QName("http://www.opengis.net/gml/3.2","endPosition");

	public static QName relatedTo= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","relatedTo");
	
	public static QName belongsTo= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","belongsTo");
	public static QName NetworkFacility= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","NetworkFacility");
	
	public static QName linkingTime= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","linkingTime");
	public static QName contains= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","contains");
	public static QName name=new QName ("http://inspire.ec.europa.eu/schemas/ef/4.0","name");

	public static QName geometry= new QName("http://inspire.ec.europa.eu/schemas/ef/4.0","geometry");
	public static QName GeometryPropertyType= new QName("http://www.opengis.net/gml/3.2","GeometryPropertyType");
	//gml3.1 types 
	public static QName FeaturePropertyType= new QName("http://www.opengis.net/gml","FeaturePropertyType");
	/****fake qname****/
	public static QName inspireCodeList= new QName("http://inspire.ec.europa.eu/codelist","codeList");
	public static QName FeatureArrayPropertyType= new QName("http://www.opengis.net/gml","FeatureArrayPropertyType");
	public static QName BoundingShapeType= new QName("http://www.opengis.net/gml","BoundingShapeType");
	/*--------------------------------------------------------------*/
	public static String clInspire="http://inspire.ec.europa.eu/codelist/";
	public static String clSandre="http://www.sandre.eaufrance.fr/";
	/****capabilities****/
	public static QName FeatureType= new QName("http://www.opengis.net/wfs/2.0","FeatureType");
	public static QName FeatureTypeType= new QName("http://www.opengis.net/wfs/2.0","FeatureTypeType");
	public static QName FeatureTypeList= new QName("http://www.opengis.net/wfs/2.0","FeatureTypeList");
	
	public static QName Name= new QName("http://www.opengis.net/wfs/2.0","Name");
	public static QName QName= new QName("http://www.w3.org/2001/XMLSchema","QName");
	public static QName Title= new QName("http://www.opengis.net/wfs/2.0","Title");
	
	public static QName Abstract= new QName("http://www.opengis.net/wfs/2.0","Abstract");
	
	public static QName Keywords= new QName("http://www.opengis.net/ows/1.1","Keywords");
	public static QName KeywordsType= new QName("http://www.opengis.net/ows/1.1","KeywordsType");
	public static QName DefaultCRS= new QName("http://www.opengis.net/wfs/2.0","DefaultCRS");
	
	public static QName OtherCRS= new QName("http://www.opengis.net/wfs/2.0","OtherCRS");
	
	public static QName WGS84BoundingBox= new QName("http://www.opengis.net/ows/1.1","WGS84BoundingBox");
	public static QName WGS84BoundingBoxType= new QName("http://www.opengis.net/ows/1.1","WGS84BoundingBoxType");
	public static QName MetadataURL= new QName("http://www.opengis.net/wfs/2.0","MetadataURL");
	public static QName MetadataURLType= new QName("http://www.opengis.net/wfs/2.0","MetadataURLType");
	

}

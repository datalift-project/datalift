package org.datalift.lov.local;

public class LovConstants {
	
	/* NAMESPACES */
	public static String NSP_BIBO="http://purl.org/ontology/bibo/";
	public static String NSP_DC="http://purl.org/dc/elements/1.1/";
	public static String NSP_DC_TERMS="http://purl.org/dc/terms/";
	public static String NSP_FOAF="http://xmlns.com/foaf/0.1/";
	public static String NSP_FRBR="http://purl.org/vocab/frbr/core#";
	public static String NSP_LEXVO="http://lexvo.org/ontology#";
	public static String NSP_LOV="http://lov.okfn.org/dataset/lov/lov#";
	public static String NSP_MOAT="http://moat-project.org/ns#";
	public static String NSP_MREL="http://id.loc.gov/vocabulary/relators/";
	public static String NSP_OWL="http://www.w3.org/2002/07/owl#";
	public static String NSP_RDF="http://www.w3.org/1999/02/22-rdf-syntax-ns#";
	public static String NSP_RDFS="http://www.w3.org/2000/01/rdf-schema#";
	public static String NSP_REV="http://purl.org/stuff/rev#";
	public static String NSP_SKOS="http://www.w3.org/TR/skos-reference/#";
	public static String NSP_TAGS="http://www.holygoat.co.uk/owl/redwood/0.1/tags/";
	public static String NSP_VANN="http://purl.org/vocab/vann/";
	public static String NSP_VOAF="http://purl.org/vocommons/voaf#";
	public static String NSP_VOID="http://rdfs.org/ns/void#";
	public static String NSP_XSD="http://www.w3.org/2001/XMLSchema#";
	
	/* LOV dataset */
	public static String LOV_FULL_VOCABULARYSPACE =NSP_LOV+"LOV";
	
	/* VOAF */
	public static String VOAF_FULL_VOCABULARY=NSP_VOAF+"Vocabulary";
	
	public static String PREFIXES = "PREFIX rdf:<"+LovConstants.NSP_RDF+"> \n"+
	"PREFIX xsd:<"+LovConstants.NSP_XSD+"> \n"+
	"PREFIX dc:<"+LovConstants.NSP_DC+"> \n"+
	"PREFIX rdfs:<"+LovConstants.NSP_RDFS+"> \n"+
	"PREFIX owl:<"+LovConstants.NSP_OWL+"> \n"+
	"PREFIX skos:<"+LovConstants.NSP_SKOS+"> \n"+
	"PREFIX foaf:<"+LovConstants.NSP_FOAF+"> \n"+
	"PREFIX dcterms:<"+LovConstants.NSP_DC_TERMS+"> \n"+
	"PREFIX bibo:<"+LovConstants.NSP_BIBO+"> \n"+
	"PREFIX vann:<"+LovConstants.NSP_VANN+"> \n"+
	"PREFIX voaf:<"+LovConstants.NSP_VOAF+"> \n"+
	"PREFIX frbr:<"+LovConstants.NSP_FRBR+"> \n"+
	"PREFIX void:<"+LovConstants.NSP_VOID+"> \n"+
	"PREFIX lov:<"+LovConstants.NSP_LOV+"> \n";
	
	/* XSD / RDF / RDFS / OWL */
	public static String XSD_FULL_DATE = NSP_XSD+"date";
	public static String XSD_FULL_DATETIME = NSP_XSD+"dateTime";
	public static String XSD_FULL_BOOLEAN = NSP_XSD+"boolean";
	public static String XSD_FULL_INTEGER = NSP_XSD+"integer";
	public static String RDF_TYPE = "rdf:type";
	public static String RDF_FULL_TYPE = NSP_RDF+"type";
	public static String RDFS_LABEL = "rdfs:label";
	public static String RDFS_FULL_LABEL = NSP_RDFS+"label";
	public static String OWL_IMPORTS="owl:imports";
	public static String OWL_FULL_IMPORTS=NSP_OWL+"imports";
	public static String OWL_VERSION_INFO="owl:versionInfo";
	public static String OWL_FULL_VERSION_INFO=NSP_OWL+"versionInfo";
	
	/* DC / FOAF / VOID */
	public static String DC_TERMS_CREATOR="dcterms:creator";
	public static String DC_TERMS_FULL_CREATOR = NSP_DC_TERMS+"creator";
	public static String DC_TERMS_CONTRIBUTOR="dcterms:contributor";
	public static String DC_TERMS_FULL_CONTRIBUTOR = NSP_DC_TERMS+"contributor";
	public static String DC_TERMS_DATE = "dcterms:date";
	public static String DC_TERMS_FULL_DATE = NSP_DC_TERMS+"date";
	public static String DC_TERMS_DESCRIPTION = "dcterms:description";
	public static String DC_TERMS_FULL_DESCRIPTION = NSP_DC_TERMS+"description";
	public static String DC_TERMS_HAS_PART="dcterms:hasPart";
	public static String DC_TERMS_FULL_IDENTIFIER = NSP_DC_TERMS+"identifier";
	public static String DC_TERMS_FULL_ISSUED=NSP_DC_TERMS+"issued";
	public static String DC_TERMS_FULL_LANGUAGE = NSP_DC_TERMS+"language";
	public static String DC_TERMS_FULL_MODIFIED=NSP_DC_TERMS+"modified";
	public static String DC_TERMS_PUBLISHER="dcterms:publisher";
	public static String DC_TERMS_FULL_PUBLISHER = NSP_DC_TERMS+"publisher";
	public static String DC_TERMS_TITLE = "dcterms:title";
	
	public static String FOAF_FULL_NAME = NSP_FOAF+"name";
	public static String FOAF_FULL_PERSON = NSP_FOAF+"Person";
	public static String FOAF_FULL_ORGANIZATION = NSP_FOAF+"Organization";
		
	public static String VOID_FULL_DATASET = NSP_VOID+"Dataset";
	public static String VOID_FULL_CLASS_PARTITION = NSP_VOID+"classPartition";
	public static String VOID_FULL_PROPERTY_PARTITION = NSP_VOID+"propertyPartition";
	public static String VOID_FULL_CLASS = NSP_VOID+"class";
	public static String VOID_FULL_PROPERTY = NSP_VOID+"property";
	public static String VOID_FULL_TRIPLES = NSP_VOID+"triples";
	public static String VOID_FULL_SPARQL_ENDPOINT = NSP_VOID+"sparqlEndpoint";
	
	/* MREL / REV / BIBO / VANN*/
	public static String MREL_FULL_REV = NSP_MREL+"rev";
	
	public static String REV_FULL_HAS_REVIEW=NSP_REV+"hasReview";
	public static String REV_FULL_REVIEW=NSP_REV+"Review";
	public static String REV_FULL_TEXT=NSP_REV+"text";
	
	public static String BIBO_SHORT_TITLE="bibo:shortTitle";
	
	public static String VANN_PREFERRED_NAMESPACE_PREFIX="vann:preferredNamespacePrefix";
	public static String VANN_FULL_PREFERRED_NAMESPACE_PREFIX=NSP_VANN+"preferredNamespacePrefix";
	public static String VANN_FULL_PREFERRED_NAMESPACE_URI=NSP_VANN+"preferredNamespaceUri";
	
	/* FRBR */
	public static String FRBR_REALIZATION="frbr:realization";
	public static String FRBR_EXPRESSION="frbr:Expression";
	public static String FRBR_EMBODIMENT="frbr:embodiment";
	public static String FRBR_MANIFESTATION="frbr:Manifestation";
	public static String FRBR_FULL_REALIZATION=NSP_FRBR+"realization";
	public static String FRBR_FULL_EXPRESSION=NSP_FRBR+"Expression";
	public static String FRBR_FULL_EMBODIMENT=NSP_FRBR+"embodiment";
	public static String FRBR_FULL_MANIFESTATION=NSP_FRBR+"Manifestation";
}

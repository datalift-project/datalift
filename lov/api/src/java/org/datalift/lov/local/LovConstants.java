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
}

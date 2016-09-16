package org.datalift.wfs.wfs2.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;

import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSTypeDefinition;

import org.datalift.model.ComplexFeature;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.Feature;
import org.datalift.model.MyGeometry;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class MultiPolygonHandler extends DefaultHandler{
	private ComplexFeature multiPolygoneCf;
	private PSVIProvider psvi ;
	private MyGeometry g;
	boolean goBack=false;
	private SAXParser parser;
    private GeoHandler fHandler;
    private Stack <ComplexFeature> fpile;
    private Stack<ComplexFeature> stack = new Stack<ComplexFeature>();
    private String localNameRetrieved;
    private List <String> multiPolygoneParts= new ArrayList<String>();
    
	public MultiPolygonHandler(SAXParser parser, GeoHandler geoHandler, Stack<ComplexFeature> fpile, MyGeometry g,
			ComplexFeature tmp, String localNameRetrieved) {
		multiPolygoneCf=tmp;
		this.g=g;
		this.parser=parser;
    	this.fHandler=geoHandler;
    	this.fpile=fpile;
    	this.localNameRetrieved=localNameRetrieved;
    	this.psvi=fHandler.psvi;
	}
	
    @Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
    	if(localName.equals(multiPolygoneCf.name.getLocalPart()))
		{
			initMultiPoLygone();
		}
    	ElementPSVI elt=psvi.getElementPSVI();
    	Feature ft = new Feature();
    	XSElementDeclaration dec= elt.getElementDeclaration();
    	XSTypeDefinition td=null;	    	
    	if(dec!=null)
    	{
    		td=dec.getTypeDefinition();

    		if(td!=null)
    		{
    			ft.typeTitle=td.getName();
    			if (ft.typeTitle==null && td.getBaseType()!=null ) 
    				ft.typeTitle=td.getBaseType().getName(); //go until the next base to get the name type 
    		}

    	}

    	if(td!=null && td.getName().equals(Const.SurfacePropertyType.getLocalPart()))
    	{
    		PolygonMemberHandler ph=new PolygonMemberHandler(parser, this,fpile,g, multiPolygoneCf,Const.Polygon.getLocalPart());
			parser.getXMLReader().setContentHandler(ph);
			ph.startElement(uri, localName, qName, attributes);
    	}
	}
	private boolean initMultiPoLygone()
	{
		g.type=multiPolygoneCf.name;
		for (Attribute a : multiPolygoneCf.itsAttr) {
			if(a.attrType.getName().equals(Const.ID))
				{
					g.gml_id=a.value;
				}
			if(a.name.equals(Const.srsName))
				{
					g.SRS=a.value;
				}
			if(a.name.equals(Const.srsDimension))
			{
				g.dimension=Integer.parseInt(a.value);
			}
		}
		return true;
	}
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		if(localName.equals(Const.Polygon.getLocalPart()))
		{
			multiPolygoneParts.add(g.WKT);
		}
		if(localName.equals(localNameRetrieved)) //if we have finished the building of geometric feature, the feature is added to pile and removed from geo_pile, so go back 
		{
			
				g.WKT=createWKT("MULTIPOLYGON");
								 

			parser.getXMLReader().setContentHandler(fHandler);	
			fHandler.endElement(uri, localName, qName);
		}
	}
	private String createWKT(String type) {	
		String wkt=type+"(";
		for (String part : multiPolygoneParts) {
			wkt+=part+",";
		}
		wkt = wkt.substring(0, wkt.length()-1);
		wkt+=")";
		return wkt;
	}





}

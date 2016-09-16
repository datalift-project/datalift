package org.datalift.wfs.wfs2.parsing;

import java.util.Stack;

import javax.xml.parsers.SAXParser;


import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.Feature;
import org.datalift.model.MyGeometry;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PointHandler extends DefaultHandler{
	private ComplexFeature pointCf;
	private MyGeometry g;
	boolean goBack=false;
    private SAXParser parser;
    private GeoHandler fHandler;
    private Stack <ComplexFeature> fpile;
    private Stack<ComplexFeature> stack = new Stack<ComplexFeature>();
    private String localNameRetrieved;
    StringBuilder currentCoordinateValues= new StringBuilder();

	public PointHandler(SAXParser parser2, GeoHandler geoHandler, Stack<ComplexFeature> fpile, MyGeometry g, ComplexFeature emp, String localNameRetrieved) throws SAXException {
		pointCf=emp;
		this.g=g;
		this.parser=parser2;
    	this.fHandler=geoHandler;
    	this.fpile=fpile;
    	this.localNameRetrieved=localNameRetrieved;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		currentCoordinateValues.delete(0, currentCoordinateValues.length());
		if(localName.equals(pointCf.name.getLocalPart()))
		{
			initPoint();
		}
	}
	private boolean initPoint()
	{
		g.type=pointCf.name;
		for (Attribute a : pointCf.itsAttr) {
			if(a.attrType.getName().equals(Const.ID))
				{
					g.gml_id=a.value;
				}
			if(a.name.equals(Const.srsName))
				{
					g.SRS=Helper.constructSRIDValue(a.value);
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
		if(localName.equals(localNameRetrieved)) //if we have fionished the building of geometric feature, the feature is added to pile and removed from geo_pile, so go back 
		{
			String val=currentCoordinateValues.toString();
			handleCharacters(val);
			parser.getXMLReader().setContentHandler(fHandler);	
			fHandler.endElement(uri, localName, qName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
//		String val=String.copyValueOf(ch, start, length);
//		if(val.contains("\n")) // to escape some bad gml responses 
//		{
//			return;
//		}
		//String val=new String(ch, start, length).trim();
		currentCoordinateValues.append(ch,start,length);
		if(currentCoordinateValues.toString().trim().length()==0)
			{
				currentCoordinateValues.delete(0, currentCoordinateValues.length());				
			}
	}
	private void handleCharacters(String val)
	{
		if (val.length() == 0)
		{
			return; // ignore white space
		}
		if(g.dimension==2)
		{
			g.WKT=createWKT(val,"POINT");
		}
		if(g.dimension==3)
		{
			g.WKT=createWKT(val,"POINT"); //Should be "POINTZ", but as we don't have a wktreader for 3d dimension, we just keep the x and y coordinate for now
		}
		g.isEmpty=false;
	}
	private String createWKT(String valueOf, String typeGeom) {
		// TODO Auto-generated method stub
		String wkt="";
		wkt+=typeGeom+"("+valueOf+")";
		return wkt;
	}
}

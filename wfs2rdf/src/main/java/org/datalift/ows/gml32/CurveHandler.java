package org.datalift.ows.gml32;
import java.util.Stack;

import javax.xml.parsers.SAXParser;

import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.model.DlGeometry;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Helper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A specific handler for a curve geometry 
 * @author Hanane Eljabiri
 *
 */
public class CurveHandler extends DefaultHandler {
	private ComplexFeature pointCf;
	private DlGeometry g;
	boolean goBack=false;
	private SAXParser parser;
	private GeoHandler fHandler;
	private Stack <ComplexFeature> fpile;
	private Stack<ComplexFeature> stack = new Stack<ComplexFeature>();
	private String localNameRetrieved;
	private StringBuilder currentCoordinateValues= new StringBuilder();

	public CurveHandler(SAXParser parser2, GeoHandler geoHandler, Stack<ComplexFeature> fpile2, DlGeometry g2,
			ComplexFeature tmp, String localNameRetrieved2) {
		pointCf=tmp;
		this.g=g2;
		this.parser=parser2;
		this.fHandler=geoHandler;
		this.fpile=fpile2;
		this.localNameRetrieved=localNameRetrieved2;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		currentCoordinateValues.setLength(0);
		if(localName.equals(pointCf.name.getLocalPart()))
		{
			initCurve();
		}
	}
	private boolean initCurve()
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
			String val=currentCoordinateValues.toString().trim().replaceAll("\\s+", " ");
			currentCoordinateValues.setLength(0);
			handleCharacters(val);
			parser.getXMLReader().setContentHandler(fHandler);	
			fHandler.endElement(uri, localName, qName);
		}
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		currentCoordinateValues.append(ch,start,length);
	}
	private void handleCharacters(String val)
	{
		if (val.length() == 0)
		{
			return; // ignore white space
		}
		if(g.dimension==2)
		{
			g.WKT=createWKT(val,"LINESTRING");
		}
		if(g.dimension==3)
		{
			g.WKT=createWKTZ(val,"LINESTRING"); //Should be "LINESTRINGZ", but as we don't have a wktreader for 3d dimension, we just keep the x and y coordinate for now
		}
	}

	private String createWKT(String valueOf, String typeGeom) {
		// TODO Auto-generated method stub
		String wkt="";
		wkt+=typeGeom+"(";
		valueOf=valueOf.trim();
		String [] coord=valueOf.split(" ");
		for(int i=0;i<coord.length-1;i++)
		{
			if(i==coord.length-2)
			{
				wkt+=coord[i]+" "+coord[i+1]+" ";
			}
			else
			{
				wkt+=coord[i]+" "+coord[i+1]+", ";
			}
		}
		wkt+=")";
		return wkt;
	}
	private String createWKTZ(String valueOf, String typeGeom) {
		String wkt="";
		wkt+=typeGeom+"(";
		String [] coord=valueOf.split(" ");
		for(int i=0;i<coord.length-2;i++)
		{
			if(i==coord.length-3)
			{
				wkt+=coord[i]+" "+coord[i+1]+" "+coord[i+2]+" ";
			}
			else
			{
				wkt+=coord[i]+" "+coord[i+1]+" "+coord[i+2]+" "+", ";
			}
		}
		wkt+=")";
		return wkt;
	}
}

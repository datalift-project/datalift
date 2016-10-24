package org.datalift.ows.gml32;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.SAXParser;

import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.model.DlGeometry;
import org.datalift.ows.utilities.Const;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A specific handler for a polygon geometry 
 * @author Hanane Eljabiri
 *
 */
public class PolygoneHandler extends DefaultHandler{

	private ComplexFeature polygoneCf;
	private DlGeometry g;
	boolean goBack=false;
    private SAXParser parser;
    private GeoHandler fHandler;
    private String localNameRetrieved;
    private List <String> polygoneParts= new ArrayList<String>();
	private StringBuilder currentCoordinateValues= new StringBuilder();
	public PolygoneHandler(SAXParser parser2, GeoHandler geoHandler, Stack<ComplexFeature> fpile2, DlGeometry g2,
			ComplexFeature tmp, String localNameRetrieved2) {
		polygoneCf=tmp;
		this.g=g2;
		this.parser=parser2;
    	this.fHandler=geoHandler;
    	this.localNameRetrieved=localNameRetrieved2;
	}
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		currentCoordinateValues.setLength(0);
		if(localName.equals(polygoneCf.name.getLocalPart()))
		{
			initPoLygone();
		}
	}
	private boolean initPoLygone()
	{
		g.type=polygoneCf.name;
		for (Attribute a : polygoneCf.itsAttr) {
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
		
		String val=currentCoordinateValues.toString();
		currentCoordinateValues.setLength(0);
		handleCharacters(val);
		if(localName.equals(localNameRetrieved)) //if we have finished the building of geometric feature, the feature is added to pile and removed from geo_pile, so go back 
		{
			if(g.dimension==2)
			{
				g.WKT=createWKT("POLYGON");
			}
			if(g.dimension==3)
			{
				g.WKT=createWKT("POLYGON"); 
			}	
			parser.getXMLReader().setContentHandler(fHandler);	
			fHandler.endElement(uri, localName, qName);
		}
	}
	

	private String createWKT(String type) {	
		String wkt=type+"(";
		for (String part : polygoneParts) {
			wkt+=part+",";
		}
		wkt = wkt.substring(0, wkt.length()-1);
		wkt+=")";
		return wkt;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		
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
			polygoneParts.add(createPolygonePart(val));
		}
	if(g.dimension==3)
	{
		g.WKT=createPolygonePartZ(val);
	}
		g.isEmpty=false;
		//clean polygon part coordinates
		currentCoordinateValues.delete(0, currentCoordinateValues.length());
	}
	
	private String createPolygonePart(String valueOf) {
		String wkt="";
		wkt+="(";
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
	private String createPolygonePartZ(String valueOf) {
		String wkt="";
		wkt+="(";
		String [] coord=valueOf.split(" ");
		for(int i=0;i<coord.length-2;i++)
		{
			if(i==coord.length-3)
			{
				wkt+=coord[i]+" "+coord[i+1]+" "/*+coord[i+2]+" "*/; //IGNORE z
			}
			else
			{
				wkt+=coord[i]+" "+coord[i+1]+" "/*+coord[i+2]+" "*/+", ";
			}
		}
		wkt+=")";
		return wkt;
	}



}

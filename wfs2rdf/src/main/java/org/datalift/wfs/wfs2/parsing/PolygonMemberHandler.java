package org.datalift.wfs.wfs2.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.parsers.SAXParser;


import org.datalift.model.ComplexFeature;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.Feature;
import org.datalift.model.MyGeometry;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class PolygonMemberHandler extends DefaultHandler{

	private ComplexFeature polygoneCf;
	private MyGeometry g;
	boolean goBack=false;
    private SAXParser parser;
    private MultiPolygonHandler fHandler;
    private Stack <ComplexFeature> fpile;
    private Stack<ComplexFeature> stack = new Stack<>();
    private String localNameRetrieved;
    private List <String> polygoneParts= new ArrayList<String>();
    private Boolean newPart=true;
    
	public PolygonMemberHandler(SAXParser parser2, MultiPolygonHandler geoHandler, Stack<ComplexFeature> fpile2, MyGeometry g2,
			ComplexFeature tmp, String localNameRetrieved2) {
		polygoneCf=tmp;
		this.g=g2;
		this.parser=parser2;
    	this.fHandler=geoHandler;
    	this.fpile=fpile2;
    	this.localNameRetrieved=localNameRetrieved2;
	}

	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		if(localName.equals(polygoneCf.name.getLocalPart()))
		{
			initPoLygone();
		}
		newPart=true;
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
		if(localName.equals(localNameRetrieved)) //if we have finished the building of geometric feature, the feature is added to pile and removed from geo_pile, so go back 
		{
				g.WKT=createWKT("POLYGON");
					
			parser.getXMLReader().setContentHandler(fHandler);	
			fHandler.endElement(uri, localName, qName);
		}
		
	}

	private String createWKT(String type) {	
		String wkt="(";
		for (String part : polygoneParts) {
			wkt+=part+",";
		}
		wkt = wkt.substring(0, wkt.length()-1);
		wkt+=")";
		return wkt;
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		
//		String val=String.copyValueOf(ch, start, length);
//		if(val.contains("\n")) // to escape some bad gml responses 
//		{
//			return;
//		}
		String val=new String(ch, start, length).trim();
		 
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
	}
	private String createPolygonePart(String valueOf) {
		// TODO Auto-generated method stub
		String wkt="";
		wkt+="(";
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
		// TODO Auto-generated method stub
		String wkt="";
		wkt+="(";
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

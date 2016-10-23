package org.datalift.ows.gml32;

import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;

import org.apache.xerces.impl.xs.XSComplexTypeDecl;
import org.apache.xerces.xs.AttributePSVI;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.model.Feature;
import org.datalift.ows.model.DlGeometry;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Helper;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.io.WKTReader;

/**
 * A specific handler for a geometry feature
 * @author Hanane Eljabiri
 *
 */
public class GeoHandler  extends DefaultHandler{
	public DlGeometry g = null;
	private Geometry vg;
	private ComplexFeature tmp=null;
	public PSVIProvider psvi ;
	private SAXParser parser;
	private DefaultHandler fHandler;
	private Stack <ComplexFeature> fpile;
	public boolean goBack,isRepPoint;
	private String localNameRetrieved;

	public GeoHandler(SAXParser p, DefaultHandler fh,Stack <ComplexFeature>fpile, String explicitType) throws SAXException {
		this.parser=p;
		this.psvi = (PSVIProvider)p.getXMLReader();
		this.fHandler=fh;
		this.fpile=fpile;
		this.goBack=false;
		this.tmp=new ComplexFeature();
	} 
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		ElementPSVI elt=psvi.getElementPSVI();
		//initialize cmplx object and set its attribute
		tmp = new ComplexFeature();
		tmp.name=new QName(uri, localName);
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
				{
					ft.typeTitle=td.getBaseType().getName(); //go until the next base to get the name type 
				}
			}
		}
		tmp.attrType=td;
		String explicitType="";
		for (int i=0;i<attributes.getLength();i++)
		{
			Attribute a=new Attribute();
			AttributePSVI psvia = psvi.getAttributePSVI(i);
			//XSSimpleTypeDefinition  mtd= psvia.getMemberTypeDefinition();
			XSSimpleTypeDefinition  mtd= psvia.getAttributeDeclaration().getTypeDefinition();
			a.name= new QName(attributes.getURI(i), attributes.getLocalName(i));
			a.value=attributes.getValue(i);
			Feature f = new Feature();
			f.typeTitle=psvia.getAttributeDeclaration().getTypeDefinition().getName();
			a.attrType=mtd;
			tmp.itsAttr.add(a);
			if(a.name.equals(Const.explicitType))
			{			
				explicitType=a.value;
				explicitType=explicitType.substring(explicitType.indexOf(":")+1);
			}
			if(Helper.isSet(explicitType)) //override the type of the feature with the explicite type
			{
				final String newType = explicitType;
				XSTypeDefinition t = new XSComplexTypeDecl() {
					@Override
					public String getName() { return newType; }
				};
				tmp.attrType=t;
			}
		}
		if(tmp.attrType.getName()!=null)
		{
			if(tmp.getTypeName().equals(Const.GeometryPropertyType) || tmp.getTypeName().getLocalPart().equals(Const.GeometryPropertyType.getLocalPart())
					||tmp.getTypeName().equals(Const.PointPropertyType) || tmp.getTypeName().equals(Const.CurvePropertyType) 
					|| tmp.getTypeName().equals(Const.MultiSurfacePropertyType))
			{
				localNameRetrieved=localName;
				g=new DlGeometry();
			}
			if(tmp.getTypeName().equals(Const.PointType))
			{
				PointHandler ph=new PointHandler(parser, this,fpile,g, tmp,localNameRetrieved);
				parser.getXMLReader().setContentHandler(ph);
				ph.startElement(uri, localName, qName, attributes);
			}
			if(tmp.getTypeName().equals(Const.CurveType))
			{
				CurveHandler ph=new CurveHandler(parser, this,fpile,g, tmp,localNameRetrieved);
				parser.getXMLReader().setContentHandler(ph);
				ph.startElement(uri, localName, qName, attributes);
			}
			if(tmp.getTypeName().equals(Const.PolygonType))
			{
				PolygoneHandler ph=new PolygoneHandler(parser, this,fpile,g, tmp,localNameRetrieved);
				parser.getXMLReader().setContentHandler(ph);
				ph.startElement(uri, localName, qName, attributes);
			}
			if(tmp.getTypeName().equals(Const.MultiSurfaceType))
			{
				MultiPolygonHandler mph=new MultiPolygonHandler(parser, this,fpile,g, tmp,localNameRetrieved);
				parser.getXMLReader().setContentHandler(mph);
				mph.startElement(uri, localName, qName, attributes);		    			
			}
		}	
	}



	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {

		if(localName.equals(localNameRetrieved)) //if the building of geometric feature is finished, the feature is added to pile and removed from geo_pile, so go back 
		{		
			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );
			WKTReader reader = new WKTReader( geometryFactory );
			int index=fpile.size()-2;
			try {
				vg =  reader.read(g.WKT);
				String code  = "4326"; //default srs
				if(g.SRS!=null) 
				{
					code=g.SRS;
				}
				vg.setSRID(Integer.parseInt(code));

				fpile.get(index).geom=g;
				if(fpile.peek().vividgeom==null)
				{
					fpile.get(index).vividgeom=vg;
				}
				else
				{
					if(vg.getGeometryType().equals("Point"))
					{
						fpile.get(index).representativePoint=(Point) vg;
					}
				}
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fpile.get(index).vividgeom=null;
			}
			if(isRepPoint)
			{
				fpile.pop();
			}
			parser.getXMLReader().setContentHandler(fHandler);	
			fHandler.endElement(uri, localName, qName);
		}

	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		String val=new String(ch, start, length).trim();
		if (val.length() == 0)
		{
			return; // ignore white space
		}
		g.WKT=createWKT(val,"POINT");
		g.isEmpty=false;
	}
	private String createWKT(String valueOf, String typeGeom) {
		String wkt="";
		wkt+=typeGeom+"("+valueOf+")";
		return wkt;
	}

}

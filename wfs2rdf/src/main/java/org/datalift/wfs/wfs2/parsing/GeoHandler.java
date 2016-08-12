package org.datalift.wfs.wfs2.parsing;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import javax.xml.namespace.QName;
import javax.xml.parsers.SAXParser;

import org.apache.xerces.xs.AttributePSVI;
import org.apache.xerces.xs.ElementPSVI;
import org.apache.xerces.xs.PSVIProvider;
import org.apache.xerces.xs.XSElementDeclaration;
import org.apache.xerces.xs.XSSimpleTypeDefinition;
import org.apache.xerces.xs.XSTypeDefinition;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.model.Feature;
import org.datalift.model.Attribute;
import org.datalift.model.MyGeometry;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class GeoHandler  extends DefaultHandler{


    private MyGeometry g = null;
    private Geometry vg;

    private ComplexFeature emp=null;
    private PSVIProvider psvi ;
    private SAXParser parser;
    private DefaultHandler fHandler;
    private Stack <ComplexFeature> fpile;
    private boolean goBack,isRepPoint;
    private List <ComplexFeature> geo_pile= new ArrayList<ComplexFeature>();
    String localNameRetrieved;
 
    public GeoHandler(SAXParser p, DefaultHandler fh,Stack <ComplexFeature>fpile) throws SAXException {
    	this.parser=p;
    	this.psvi = (PSVIProvider)p.getXMLReader();
    	this.fHandler=fh;
    	this.fpile=fpile;
    	this.goBack=false;
    	this.emp=new ComplexFeature();
    } 
	
	
	
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		
		if(goBack)
			{
			GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory( null );

			WKTReader reader = new WKTReader( geometryFactory );
			try {
				vg =  reader.read(g.WKT);
				String code  = "4326"; //default srs
				if(g.SRS!=null) 
					g.SRS.substring(g.SRS.lastIndexOf(":")+1);
				vg.setSRID(Integer.parseInt(code));
				fpile.peek().geom=g;
				fpile.peek().vividgeom=vg;
				CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:"+vg.getSRID());
				CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:2154");

				/*MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
				Geometry targetGeometry = JTS.transform( vg, transform);
				System.out.println(targetGeometry.getCoordinate().toString());*/
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				fpile.peek().vividgeom=null;
			}
			if(isRepPoint)
				fpile.pop();
			parser.getXMLReader().setContentHandler(fHandler);	
			fHandler.startElement(uri, localName, qName,attributes);
			}
		else
		{
			ElementPSVI elt=psvi.getElementPSVI();
	    	//initialize cmplx object and set its attribute
	    	emp = new ComplexFeature();
	    	emp.name=new QName(uri, localName);
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

	    	emp.attrType=td;

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
	    		emp.itsAttr.add(a);

	    	}
	    	if(emp.attrType.getName()!=null) //ajoute l'élément courant comme ATTRIBUT du dernier élément ( encore ouvert ) dans la pile
	    	{
	    		if(emp.getTypeName().equals(Const.GeometryPropertyType) || emp.getTypeName().equals(Const.PointPropertyType))
	    			{
	    				localNameRetrieved=localName;
	    				g=new MyGeometry();
	    				
	    			}
	    		if(emp.getTypeName().equals(Const.PointType))
	    			
	    			{
	    				if(g==null) 
	    					{
	    					localNameRetrieved=localName;
		    				g=new MyGeometry();
	    					}
	    				initPoint();
	    			}
	    	}
		}
	}
	private boolean initPoint()
	{
		if(!emp.attrType.getName().equals(Const.PointType))
			return false;
		
		g.type=emp.name;
		for (Attribute a : emp.itsAttr) {
			if(a.attrType.getName().equals(Const.ID))
				g.gml_id=a.value;
			if(a.name.equals(Const.srsName))
				g.SRS=a.value;
		}


		return true;
	}


	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		
		//some tremoving processing
		if(goBack)
			{
			isRepPoint=true;
			return; //passe la main au startelement 
			}
		if(localName.equals(localNameRetrieved)) //if we have fionished the building of geometric feature, the feature is added to pile and removed from geo_pile, so go back 
			{
				goBack=true;
				fpile.pop();
			}
		
		
	}

	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		
		String val=String.copyValueOf(ch, start, length);
		g.WKT=createWKT(val,"POINT");
		g.isEmpty=false;
	}
	private String createWKT(String valueOf, String typeGeom) {
		// TODO Auto-generated method stub
		String wkt="";
		wkt+=typeGeom+"("+valueOf+")";
		return wkt;
	}

}

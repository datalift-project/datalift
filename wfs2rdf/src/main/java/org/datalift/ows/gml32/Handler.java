package org.datalift.ows.gml32;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Helper;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
/**
 * Main handler for gml content (version 3.2) 
 * @author Hanane Eljabiri
 *
 */
public class Handler extends DefaultHandler {
    
	private List<ComplexFeature> tmpList = new ArrayList<ComplexFeature>();
    private ComplexFeature tmp = null;
    private PSVIProvider psvi ;
    private SAXParser parser;
    private Stack<ComplexFeature> stack = new Stack<ComplexFeature>();
    private StringBuilder currentCoordinateValues= new StringBuilder();
 
    public Handler(SAXParser p) throws SAXException {
    	this.parser=p;
    	this.psvi = (PSVIProvider)p.getXMLReader();
    }
    //getter method for getting the root of the XML/GML document
    public ComplexFeature getRoot() {
        if(tmpList!=null && tmpList.size()>0)
        {
        	return tmpList.get(0);        
        }
        return null;
    }
    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
    		throws SAXException {
    	String explicitType="";
    	currentCoordinateValues.setLength(0);
    	ElementPSVI elt=psvi.getElementPSVI();
    	//initialize cmplx object and set its attribute
    	tmp = new ComplexFeature();
    	tmp.name=new QName(uri, localName);
    	XSElementDeclaration dec= elt.getElementDeclaration();
    	XSTypeDefinition td=null;
    	if(dec!=null)
    	{
    		td=dec.getTypeDefinition(); 
    	}
    	tmp.attrType=td;
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
    	
    	if(!stack.isEmpty()) //ajoute l'élément courant comme ATTRIBUT du dernier élément (encore ouvert ) dans la pile
    	{
    		if(tmp.getTypeName().equals(Const.GeometryPropertyType)  || tmp.getTypeName().getLocalPart().equals(Const.GeometryPropertyType.getLocalPart())
    				|| tmp.getTypeName().equals(Const.PointPropertyType) || tmp.getTypeName().equals(Const.CurvePropertyType)
    				|| tmp.getTypeName().equals(Const.MultiSurfacePropertyType))    			
    			//create a geometry proprety
    		{
    			/**Call the geoHandler**/
    			GeoHandler gh=new GeoHandler(parser, this,stack,explicitType );
    			parser.getXMLReader().setContentHandler(gh);
    			gh.startElement(uri, localName, qName, attributes);
    		}
    		else {
    			ComplexFeature parent = (stack.isEmpty())? null: stack.peek();
    			if (parent != null) {
    				parent.itsAttr.add(tmp);
    				tmp.setParent(parent);
    			}
    		}
    	}

    	stack.push(tmp);
    }

	@Override
    public void endElement(String uri, String localName, String qName) throws SAXException {    
    	if(!stack.isEmpty())
    	{
    		ComplexFeature lastcurrentElt=stack.peek();
    		lastcurrentElt.value=currentCoordinateValues.toString().trim().replaceAll("\\s+"," ");
    		currentCoordinateValues.setLength(0);
    		QName currentQname=new QName(uri,localName);
    		if(currentQname.equals(lastcurrentElt.name))
    		{
   			 if (stack.size()==1)
    			{
   				 tmpList.add(lastcurrentElt);
    			}
   			 stack.pop();
    		}
    	}
    }
		
	@Override
    public void characters(char ch[], int start, int length) throws SAXException {
    	if(!stack.isEmpty())
    		{
    			currentCoordinateValues.append(ch,start,length);
    		}
    }

	private Map<String,Stack<String>> nsMappings = new HashMap<>();

	@Override
	public void startPrefixMapping(String prefix, String uri) throws SAXException {
		Stack<String> s = nsMappings.get(prefix);
		if (s == null) {
			s = new Stack<>();
			nsMappings.put(prefix, s);
		}
		s.push(uri);
	}

	@Override
	public void endPrefixMapping(String prefix) throws SAXException {
		Stack<String> s = nsMappings.get(prefix);
		if (s != null) {
			s.pop();
			if (s.isEmpty()) {
				nsMappings.remove(prefix);
			}
		}
	}
}
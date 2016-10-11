package org.datalift.wfs.wfs2.parsing;


import java.io.IOException;
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
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.model.Feature;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
 

 
 
public class Handler extends DefaultHandler {
 
    //List to hold object
    private List<ComplexFeature> tmpList = new ArrayList<ComplexFeature>();
    private ComplexFeature tmp = null;
    private PSVIProvider psvi ;
    private SAXParser parser;
    Stack<ComplexFeature> stack = new Stack<ComplexFeature>();
    StringBuilder currentCoordinateValues= new StringBuilder();
 
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
    	boolean foundit;
    	if(localName.equals("representativePoint"))
    		foundit=true;
    	currentCoordinateValues.delete(0, currentCoordinateValues.length());
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
    	}
    	if(!stack.isEmpty()) //ajoute l'élément courant comme ATTRIBUT du dernier élément (encore ouvert ) dans la pile
    	{
    		if(tmp.getTypeName().equals(Const.GeometryPropertyType) || tmp.getTypeName().equals(Const.PointPropertyType) || tmp.getTypeName().equals(Const.CurvePropertyType)|| tmp.getTypeName().equals(Const.MultiSurfacePropertyType))
    			
    			//create a geometry proprety
    		{
    			/**Call the geoHandler**/
    			GeoHandler gh=new GeoHandler(parser, this,stack);
    			parser.getXMLReader().setContentHandler(gh);
    			gh.startElement(uri, localName, qName, attributes);

    		}
    		else {
    			// stack.peek().itsAttr.add(tmp);
    			ComplexFeature parent = (stack.isEmpty())? null: stack.peek();
    			if (parent != null) {
    				parent.itsAttr.add(tmp);
    				tmp.setParent(parent);
    			}
    			//pile.get(pile.size()-1).itsAttr.add(tmp);
    		}
    	}
    	// else //sinon ajoute l'élément comme nouvel element racine
    	//pile.add(tmp);
    	stack.push(tmp);
    }


    @Override
	public InputSource resolveEntity(String publicId, String systemId) throws IOException, SAXException {
		// TODO Auto-generated method stub
    	System.out.println("publicid: " + publicId + " -> systemid: " + systemId);
		return super.resolveEntity(publicId, systemId);
	}

	@Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
      
    	boolean found;
		if(localName.equals("FeatureCollection")) 
    		found =true;
		if(!stack.isEmpty())
    	{
    		ComplexFeature lastcurrentElt=stack.peek();
    		lastcurrentElt.value=currentCoordinateValues.toString().trim().replaceAll("\\s+"," ");
    				//pile.get(pile.size()-1);
    		QName currentQname=new QName(uri,localName);
    		//if(currentQname.equals(pile.get(pile.size()-1).name))
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
}
package org.datalift.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSTypeDefinition;
import org.openrdf.model.Resource;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class ComplexFeature extends Attribute{
	
	public List <Attribute> itsAttr;
	public MyGeometry geom;
	public Geometry vividgeom;
	public Point representativePoint;
	private Resource id;
	private ComplexFeature parent;
	private ComplexFeature childFound;
	
	public ComplexFeature getChild(QName n) {
		//findChild(n);
		return childFound;
	}
	public ComplexFeature getParent() {
		return parent;
	}

	public ComplexFeature()
	{
		itsAttr=new ArrayList<Attribute>();
		
	}

	public String getLiteral() {
		// TODO Auto-generated method stub
		for (Attribute attribute : itsAttr) {
			if (attribute.name.equals(Const.title))
					return attribute.value;
		}
		return null;
	}
	public String getResource() {
		// TODO Auto-generated method stub
		for (Attribute attribute : itsAttr) {
			if (attribute.name.equals(Const.href))
					return attribute.value;
		}
		return null;
	}
	public String getAttributeValue(QName attributeName)
	{		
		for (Attribute attribute : itsAttr) {
			if(attribute.name.equals(attributeName))
					return attribute.value;			
		}
		return null;		
	}

	public Resource getId() {
		// TODO Auto-generated method stub
		
		return id;
	}
	public void setId(Resource identifier) {
		// TODO Auto-generated method stub
		
		this.id=identifier;
	}

	public void setParent(ComplexFeature parent) {
		// TODO Auto-generated method stub
		this.parent=parent;
	}

	public ComplexFeature findFirstChild(QName name)
	{
		List<ComplexFeature> results=findChildren(name);
		if(results!=null)
		{
			return results.get(0);
		}
		return null;
	}
	public ComplexFeature findChildByType( QName childType)
	{
		ComplexFeature child=null;
		for (Attribute attribute : this.itsAttr) {
			if(attribute instanceof ComplexFeature)
			{
				if(/*attribute.name.equals(childName) && */attribute.getTypeName().equals(childType))
				{
					child=(ComplexFeature)attribute;
				}
				else
				{
					child=((ComplexFeature)attribute).findChildByType(childType);
				}
			}
			if(child!=null)
			{
				break;
			}
		}
		return child; 
	}
	
	public ComplexFeature findChildByName( QName childName)
	{
		ComplexFeature child=null;
		for (Attribute attribute : this.itsAttr) {
			if(attribute instanceof ComplexFeature)
			{
				if(attribute.name.equals(childName) )
				{
					child=(ComplexFeature)attribute;
				}
				else
				{
					child=((ComplexFeature)attribute).findChildByName(childName);
				}
			}
			if(child!=null)
			{
				break;
			}
		}
		return child; 
	}
	public List<ComplexFeature> findChildren(QName name) {
		// TODO Auto-generated method stub
		List<ComplexFeature> children=new ArrayList<ComplexFeature>();
		for (Attribute attribute : itsAttr) {
			if(attribute instanceof ComplexFeature && attribute.name.equals(name))
			{
				  children.add(((ComplexFeature) attribute)) ;
			}
		}
		return children;
			/*if(this.name.equals(name))
			{
				childFound = new ComplexFeature();
				childFound=this;
				
			}
		for (Attribute attribute : itsAttr) {
			if(attribute instanceof ComplexFeature)
			{
				 ((ComplexFeature) attribute).findchild(name);
			}
		}*/
		
		/*for (Attribute attribute : itsAttr) {
			if(attribute instanceof ComplexFeature)
			{
				ComplexFeature fc=(ComplexFeature) attribute;
				if(fc.name.equals(name))
				{
					return fc;
				}
				else
				{
					for (Attribute attribute2 : fc.itsAttr) {
						if(attribute2 instanceof ComplexFeature)
						{
							ComplexFeature fcc=(ComplexFeature) attribute2;
							if(fcc.name.equals(name))
							{
								return fcc;
							}
							else
							{
								fcc.getchild(name);
							}
						}
					}
				}
			}
		}*/
		
	}
	
	
}

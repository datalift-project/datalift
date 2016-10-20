package org.datalift.model;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.datalift.utilities.Const;
import org.datalift.utilities.Helper;
import org.datalift.utilities.SosConst;
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
	}
	/**
	 * returns true int the following cases :
	 * 1- Contains only one util simple attribute
	 * OR
	 * 2-No util attribute but a simple content. expl : <toto> titi</toto>
	 * @return
	 */
	public boolean isSimple() {
		int nbrUtilAttri=0;
		boolean hasValue=false;
		if(Helper.isSet(this.value))
		{
			hasValue=true;
		}
		for (Attribute a : this.itsAttr) {
			if(!a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.nil)&& !a.name.equals(Const.nilReason))
			{
				if(a instanceof ComplexFeature)
					{
						return false;
					}
				nbrUtilAttri++;
			}
			if(nbrUtilAttri>1)
			{
				return false;
			}
		}
		if(hasValue && nbrUtilAttri==1)
		{
			return false;
		}
		return true;
	}
	public Resource getIdTypedParent(QName parentType) {
//		Resource id=null;
//		while(parent!=null)
//		{
//			if(parent.getTypeName().equals(parentType))
//			{
//				id=parent.id;
//			}
//			else
//			{
//				id=parent.getIdTypedParent(parentType);
//			}
//			if(id!=null)
//			{
//				break;
//			}
//		}
		return parent.parent.parent.id;	}
	public ComplexFeature test(QName parentType) {
		ComplexFeature id=null;
		while(parent!=null)
		{
			if(parent.name.equals(parentType))
			{
				id=parent;
			}
			else
			{
				id=parent.test(parentType);
			}
			if(id!=null)
			{
				break;
			}
		}
		return id;
	}
	public static void main (String [] args)
	{
		ComplexFeature root=new ComplexFeature();
		root.name=new QName("rrr");
		
		ComplexFeature a=new ComplexFeature();
		a.name=new QName("aaa");
		ComplexFeature b=new ComplexFeature();
		b.name=new QName("bbb");
		ComplexFeature c=new ComplexFeature();
		c.name=new QName("ccc");
		b.parent=a;
		c.parent=b;
		a.parent=root;
		ComplexFeature f=c.test(new QName("aaa"));
		System.out.println(f.name);
	}
	
	/**
	 * a referenced object is an xml element which contains a reference (URI,
	 * href) and at least one other "util" attribute (id, title...)
	 * 
	 * @param cf
	 * @return
	 */
	public boolean isReferencedObject() {
		if (!this.containsReference()) {
			return false;
		}
		for (Attribute a : this.itsAttr) {
			if (!a.getTypeName().equals(Const.hrefType) && !a.getTypeName().equals(Const.anyURI)
					&& !a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame)
					&& !a.name.equals(Const.nil) && !a.name.equals(Const.nilReason)) {
				return true;
			}
		}
		return false;
	}
	
	protected boolean containsReference() {
		for (Attribute a : this.itsAttr) {
			if (a.getTypeName().equals(
					Const.hrefType) /*
					 * || a.getTypeName().equals(Const.anyURI)
					 */) {
				return true;
			}
		}
		return false;
	}
}

package org.datalift.ows.model;


import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSTypeDefinition;

/**
 * An attribute of a complex feature
 * @author Hanane Eljabiri
 *
 */
public class Attribute {

	public QName name;
	public XSTypeDefinition attrType;
	public String value;
	@Override
	public String  toString() {
		String s="";
		s+=  this.name.getLocalPart()+"= new QName(\""+this.name.getNamespaceURI()+"\",\""+this.name.getLocalPart()+"\");\n";
		s+= this.getTypeName().getLocalPart()+"= new QName(\""+this.getTypeName().getNamespaceURI()+"\",\""+this.getTypeName().getLocalPart()+"\");\n";
		return s;
	}
	public QName getTypeName()
	{	
		XSTypeDefinition type=attrType;
		if(type.getName()!=null) return new QName(type.getNamespace(),type.getName());
		while (type.getName()==null)
		{
			type=type.getBaseType();
			if(type.getName()!=null) 
			{
				return new QName(type.getNamespace(),type.getName());
			}		
		}
		return null; // i didn't find any type! i will return null :/ 
	}

	public QName getBaseTypeName()
	{	
		XSTypeDefinition type=attrType;
		if(type==null)
		{
			return null;
		}
		type=type.getBaseType();
		if(type.getName()!=null) 
		{
			return new QName(type.getNamespace(),type.getName());
		}
		return null; // i didn't find any type! 
	}
}

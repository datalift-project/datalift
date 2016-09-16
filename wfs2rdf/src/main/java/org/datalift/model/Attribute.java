package org.datalift.model;


import javax.xml.namespace.QName;

import org.apache.xerces.xs.XSTypeDefinition;

public class Attribute {
	
	public QName name;
	public XSTypeDefinition attrType;
	public String value;
	@Override
	public String  toString() {
		// TODO Auto-generated method stub
		String s="";

		//return s+=name+"***"+value+"***"+this.getTypeName()+"///";
		//return s+=name.getLocalPart()+"***"+this.getTypeName().getLocalPart()+"///";
		s+=  "static QName "+this.name.getLocalPart()+"= new QName(\""+this.name.getNamespaceURI()+"\",\""+this.name.getLocalPart()+"\");\n";
		s+= "static QName "+this.getTypeName().getLocalPart()+"= new QName(\""+this.getTypeName().getNamespaceURI()+"\",\""+this.getTypeName().getLocalPart()+"\");\n";
		return s;
	}
	public QName getTypeName()
	{	
		XSTypeDefinition type=attrType;
		
		if(type.getName()!=null) return new QName(type.getNamespace(),type.getName());
		while (type.getName()==null)
		{
			type=type.getBaseType();
			if(type.getName()!=null) return new QName(type.getNamespace(),type.getName());
			
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

package org.datalift.wfs.wfs2.mapping;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;

public class SimpleTypeMapper extends BaseMapper{
	public void map(ComplexFeature cf, Context ctx) {
		if(ignore(cf))
			return;
		this.setCfId(cf,ctx);
		this.addParentLinkStatements(cf, ctx);
		
	}

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
		Resource subjectURI;
		if(cf.getParent()!=null)
		{
			subjectURI= cf.getParent().getId();
		}
		else 
		{
			subjectURI=ctx.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		//we first check the type value and the add the corresponding statement type object 
		if(cf.attrType!=null && cf.getTypeName().equals(Const.string))
		{
			String object=cf.value;
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
		} 
		//integer
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdInteger))
		{
			int object=Integer.parseInt(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
		} 
		//boolean
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdBoolan))
		{
			boolean object=Boolean.getBoolean(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
		} 
		//double or decimal
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdDouble) ||  cf.getTypeName().equals(Const.xsdDecimal))
		{
			double object=Double.valueOf(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
		} 
		//float 
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdFloat))
		{
			float object=Float.valueOf(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
		} 
		
		//date
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdDate))
		{
			XMLGregorianCalendar object=Helper.getDate(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
		} 
	}
	protected boolean isEmpty(ComplexFeature f)
	{
		boolean empty=false;
		if(f.value==null)
			{
				empty=true;
			}
		for (Attribute a : f.itsAttr) {
			if(!a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.nil)&& !a.name.equals(Const.nilReason))
			{
				return false;		 
			}
		}
		return empty;
	}

}

package org.datalift.wfs.wfs2.mapping;

import javax.xml.datatype.XMLGregorianCalendar;

import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.openrdf.model.Resource;
/**
 * exemple : description
 * @author a631207
 *
 */
//expl: additional description
public class SimpleTypeMapper extends BaseMapper{
	public void map(ComplexFeature cf, Context ctx) {
		if(ignore(cf))
			return;
		this.setCfId(cf,ctx);
		this.addParentLinkStatements(cf, ctx);
	    this.rememberGmlId(cf,ctx);
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
			subjectURI=Context.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		//we first check the type value and the add the corresponding statement type object 
		if(cf.attrType!=null && cf.getTypeName().equals(Const.string))
		{
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
		} 
		//integer
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdInteger))
		{
			int object=Integer.parseInt(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(object)));
		} 
		//boolean
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdBoolean))
		{
			boolean object=Boolean.getBoolean(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(object)));
		} 
		//double or decimal
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdDouble) ||  cf.getTypeName().equals(Const.xsdDecimal))
		{
			double object=Double.valueOf(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(object)));
		} 
		//float 
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdFloat))
		{
			float object=Float.valueOf(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(object)));
		} 
		
		//date
		if(cf.attrType!=null && cf.getTypeName().equals(Const.xsdDate))
		{
			XMLGregorianCalendar object=Helper.getDate(cf.value);
			ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(object)));
		} 
	}


}

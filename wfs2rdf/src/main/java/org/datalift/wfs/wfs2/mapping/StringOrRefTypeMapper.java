package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;

public class StringOrRefTypeMapper extends BaseMapper{
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
		ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createLiteral(cf.value)));
	
	}

}

package org.datalift.wfs.wfs2.mapping;

import org.openrdf.model.Resource;
import org.datalift.geoutility.Context;
import org.datalift.model.ComplexFeature;


public class AnyURIMapper extends BaseMapper {

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		if(ignore(cf))
		{
			return;
		}
	this.setCfId(cf,ctx);
	if(!alreadyLinked)
		{
			this.addParentLinkStatements(cf, ctx);
		}
    this.rememberGmlId(cf,ctx);
	}
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
        ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), ctx.vf.createURI(cf.value)));
	}

}

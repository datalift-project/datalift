package org.datalift.sos.mapping;

import org.datalift.geoutility.Context;
import org.datalift.model.ComplexFeature;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;

public class FeatureOfInterestMapper extends BaseMapper  {

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
	   ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsOml+cf.name.getLocalPart()), cf.getId()));	
	}
}

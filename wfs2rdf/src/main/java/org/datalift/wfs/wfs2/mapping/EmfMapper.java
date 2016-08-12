package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;

public class EmfMapper extends BaseMapper {

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		super.addRdfTypes(cf, ctx);
		URI typeSmodURI = ctx.vf.createURI(ctx.nsSmod+capitalize(cf.name.getLocalPart()));
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	
	}
	@Override
	protected void setCfId(ComplexFeature cf, Context ctx) {
		/******give the feature an identifier****/
		String id=cf.getAttributeValue(Const.identifier);
		if(id==null)
			{
				setCfId(cf, ctx);
			}
		else
		{
			cf.setId(ctx.vf.createURI(id));
			alreadyLinked=false;
		}	
	}
}

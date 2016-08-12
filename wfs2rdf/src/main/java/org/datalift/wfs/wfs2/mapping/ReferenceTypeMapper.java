package org.datalift.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;
public class ReferenceTypeMapper extends BaseMapper{

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		if(isCodeList(cf,ctx))
		{
			ctx.getMapper(Const.inspireCodeList).map(cf, ctx);	
		}
		else
		{
			super.map(cf, ctx);
		}
	}

	private boolean isCodeList(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		String potentielCodeList=cf.getAttributeValue(Const.href);
		if(potentielCodeList!=null)
		{
			if(potentielCodeList.startsWith(Const.clInspire) || potentielCodeList.startsWith(Const.clSandre))
				return true;
		}
		return false;
	}

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		if(cf.getTypeName().equals(Const.OM_ObservationPropertyType))
			{
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
				ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(ctx.nsDatalift+cf.name.getLocalPart()), cf.getId()));
			}
		else
		{
			super.addParentLinkStatements(cf, ctx);
		}
	}
}

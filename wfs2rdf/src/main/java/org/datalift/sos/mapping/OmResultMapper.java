package org.datalift.sos.mapping;

import javax.xml.namespace.QName;

import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.wfs.wfs2.mapping.BaseMapper;

public class OmResultMapper extends BaseMapper{
	@Override
	public void map(ComplexFeature cf, Context ctx) {
		boolean found=false;
		if(cf.name.equals(Const.belongsTo))
			found=true;
		
			if(ignore(cf))
				{
					return;
				}
			this.setCfId(cf,ctx);
			if(!alreadyLinked)
			{
				if(cf.isSimple())
				{
					this.addParentSimpleLinkStatements(cf, ctx);
					return;
				}else
				{
					this.addParentLinkStatements(cf, ctx);
				}
			}
			this.rememberGmlId(cf,ctx);
			this.addRdfTypes(cf, ctx);
			for (Attribute a : cf.itsAttr) {
				if (a instanceof ComplexFeature) {
					ComplexFeature f = (ComplexFeature)a;
					ctx.getMapper(f.getTypeName()).map(f, ctx);
				}
			}
			this.mapFeatureSimpleAttributes(cf, ctx,null);
			}
}

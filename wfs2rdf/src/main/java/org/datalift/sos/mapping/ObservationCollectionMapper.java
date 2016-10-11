package org.datalift.sos.mapping;

import javax.xml.namespace.QName;

import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.wfs.wfs2.mapping.BaseMapper;
import org.openrdf.model.URI;

public class ObservationCollectionMapper extends BaseMapper {
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		super.addRdfTypes(cf, ctx);
		URI typeSmodURI = ctx.vf.createURI(ctx.nsOml+"ObservationCollection");
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,typeSmodURI));	
	}
	public void map(ComplexFeature cf, Context ctx) {
		// 
		boolean found=false;
		if(cf.name.getLocalPart().equals("MeasurementTVP"))
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
				super.addParentSimpleLinkStatements(cf, ctx);
				return;
			}else
			{
				this.addParentLinkStatements(cf, ctx);
			}
		}
		this.rememberGmlId(cf,ctx);
		this.addRdfTypes(cf, ctx);
		if(cf.vividgeom!=null)
		{
			ctx.getMapper(new QName("geometry")).map(cf, ctx);
		}

		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				if(f.name.equals(Const.omResult))
				{
					ctx.getMapper(f.name).map(f, ctx); //in this case, we base the mapping on the feature name as result has no specific type (anytype)
				}
				else
				{
					ctx.getMapper(f.getTypeName()).map(f, ctx); 
				}
			}
		}
		this.mapFeatureSimpleAttributes(cf, ctx,null);

	}


}

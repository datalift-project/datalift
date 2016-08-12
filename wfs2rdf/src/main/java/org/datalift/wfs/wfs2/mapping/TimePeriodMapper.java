package org.datalift.wfs.wfs2.mapping;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.datalift.model.ComplexFeature;
import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.Const;
import org.datalift.model.SosConst;

public class TimePeriodMapper extends BaseMapper{

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		if(ignore(cf))
			return;
		this.setCfId(cf,ctx);
		//this.addParentLinkStatements(cf, ctx);
		this.addRdfTypes(cf, ctx);
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature position=(ComplexFeature) a;
				XMLGregorianCalendar d=getDate(position.value);
				if(d!=null)
				{
					Value v5=ctx.vf.createLiteral(d);
					int indexPosition=position.name.getLocalPart().indexOf("Position");
					String p=position.name.getLocalPart().substring(0, indexPosition);
					URI preperty=ctx.vf.createURI(ctx.nsIsoTP+p);
					ctx.model.add(ctx.vf.createStatement(cf.getId(), preperty, v5));
				}  
			}
		}
		this.mapFeatureSimpleAttributes(cf, ctx);
	}
	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {	
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+capitalize(cf.name.getLocalPart()))));
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsIsoTP+"TM_Period")));
		}
	
}

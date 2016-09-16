package org.datalift.wfs.wfs2.mapping;

import java.io.FileNotFoundException;
import java.net.URI;

import javax.xml.namespace.QName;

import org.datalift.fwk.rdf.Repository;
import org.datalift.geoutility.Context;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.openrdf.rio.RDFHandlerException;

public class WFS2Converter {
	
	private Context ctx;
	
	public WFS2Converter(int i)
	{
		ctx=new Context();
		if(i==1) //we assume that 1 is the id to say we should use EMF group mappers
		{
			Mapper m=new SimpleTypeMapper();
			ctx.mappers.put(Const.StringOrRefType, m);
			ctx.mappers.put(Const.ReferenceType, new ReferenceTypeMapper());
			ctx.mappers.put(Const.EnvironmentalMonitoringFacilityType, new EmfMapper());
			ctx.mappers.put(Const.TimePeriodType, new TimePeriodMapper());
			Mapper m2= new AnyTypeMapper(); 
			ctx.mappers.put(Const.anyType,m2);
			ctx.mappers.put(Const.AbstractMemberType,m2);
			ctx.mappers.put(Const.OM_ObservationPropertyType,new ObservationPropertyTypeMapper());
			ctx.mappers.put(Const.inspireCodeList,new CodeListMapper());
		}
	}
	public void ConvertFeaturesToRDF(ComplexFeature fc, org.datalift.fwk.rdf.Repository target , URI targetGraph, URI baseUri, String targetType)
	{
		for (Attribute a : fc.itsAttr) {
			if(a instanceof ComplexFeature)
			{
				if(!a.getTypeName().equals(Const.BoundingShapeType)) //to ignore general bounding box if any
				{
					ComplexFeature member =(ComplexFeature)a;
					for (Attribute aa : member.itsAttr) {
						if(aa instanceof ComplexFeature)
						{
							ComplexFeature ef =(ComplexFeature)aa;
							ctx.getMapper(ef.getTypeName()).map(ef, ctx);
							
						}
					}
				}
			}
		}	
	}
	public void StoreRDF() throws FileNotFoundException, RDFHandlerException
	{
		this.ctx.exportTtl("C:/Users/A631207/Documents/my_resources/emf1.ttl");
	}
	public void StoreRdfTS(Repository target, URI targetGraph, URI baseUri, String targetType) throws FileNotFoundException, RDFHandlerException
	{
		this.ctx.exportTS(target, targetGraph, baseUri, targetType);
	}
}

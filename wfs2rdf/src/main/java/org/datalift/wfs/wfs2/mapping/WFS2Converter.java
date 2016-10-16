package org.datalift.wfs.wfs2.mapping;

import java.io.FileNotFoundException;
import java.net.URI;

import javax.xml.namespace.QName;

import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.sos.mapping.FeatureOfInterestMapper;
import org.datalift.sos.mapping.MeasurmentTimeSeriesMapper;
import org.datalift.sos.mapping.ObservationCollectionMapper;
import org.datalift.sos.mapping.ObservedPropertyMapper;
import org.datalift.sos.mapping.OmResultMapper;
import org.datalift.sos.mapping.PhenomenonTimeMapper;
import org.datalift.sos.mapping.ProcedureMapper;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
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
			
			//to be replaced later by a new bunch of mappers
			ctx.mappers.put(Const.OM_ObservationType,new ObservationCollectionMapper());
			ctx.mappers.put(Const.FeaturePropertyType,new FeatureOfInterestMapper());
			ctx.mappers.put(Const.observedProperty,new ObservedPropertyMapper());
			ctx.mappers.put(Const.MeasurementTimeseriesType,new MeasurmentTimeSeriesMapper());
			ctx.mappers.put(Const.omResult,new OmResultMapper());
			ctx.mappers.put(Const.TimeObjectPropertyType,new PhenomenonTimeMapper());
			ctx.mappers.put(Const.OM_ProcessPropertyType, new ProcedureMapper());
		}
	}
	public boolean ConvertFeaturesToRDF(ComplexFeature fc, org.datalift.fwk.rdf.Repository target , URI targetGraph, URI baseUri, String targetType) throws FileNotFoundException, RDFHandlerException
	{
		//ctx.saver.initConnexion(target, targetGraph, baseUri, targetType);
		if(fc.name.equals(Const.exception) || fc.name.equals(Const.exceptionReport))
		{
			return false;
		}
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
							//flush model if needed
							//ctx.saver.flush(this.ctx.model);		
							StoreRDF();
							storeRdfTS(target, targetGraph, baseUri, targetType);				
						}
					}
				}
			}
		}
		return true;
		//ctx.saver.close();
	}
	public void StoreRDF() throws FileNotFoundException, RDFHandlerException
	{
		this.ctx.exportTtl("C:/Users/A631207/Documents/my_resources/emf1.ttl");
	}
	public void storeRdfTS(Repository target, URI targetGraph, URI baseUri, String targetType) throws FileNotFoundException, RDFHandlerException
	{
		this.ctx.exportTS(target, targetGraph, baseUri, targetType);
	}
}

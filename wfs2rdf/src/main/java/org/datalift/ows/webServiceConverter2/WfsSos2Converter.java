package org.datalift.ows.webServiceConverter2;

import org.datalift.fwk.rdf.Repository;
import org.datalift.ows.exceptions.TechnicalException;
import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.sos.mapping.FeatureOfInterestMapper;
import org.datalift.ows.sos.mapping.MeasurmentTimeSeriesMapper;
import org.datalift.ows.sos.mapping.ObservationCollectionMapper;
import org.datalift.ows.sos.mapping.ObservedPropertyMapper;
import org.datalift.ows.sos.mapping.OmResultMapper;
import org.datalift.ows.sos.mapping.PhenomenonTimeMapper;
import org.datalift.ows.sos.mapping.ProcedureMapper;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.wfs.wfs2.mapping.AnyTypeMapper;
import org.datalift.ows.wfs.wfs2.mapping.CodeListMapper;
import org.datalift.ows.wfs.wfs2.mapping.EmfMapper;
import org.datalift.ows.wfs.wfs2.mapping.Mapper;
import org.datalift.ows.wfs.wfs2.mapping.ObservationPropertyTypeMapper;
import org.datalift.ows.wfs.wfs2.mapping.ReferenceTypeMapper;
import org.datalift.ows.wfs.wfs2.mapping.TimePeriodMapper;

import org.openrdf.rio.RDFHandlerException;

public class WfsSos2Converter {

	private Context ctx;
	public WfsSos2Converter(int ontologyOption, Repository target, java.net.URI targetGraph) throws RDFHandlerException {
		ctx=new Context( target, targetGraph);
		if(ontologyOption==1) //we assume that 1 is the id to say we should use EMF group mappers
		{
			//ctx.mappers.put(Const.StringOrRefType, m);
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
	public boolean ConvertFeaturesToRDF(ComplexFeature fc) throws RDFHandlerException
	{
		//ctx.saver.initConnexion(target, targetGraph, baseUri, targetType);
		if(fc.name.equals(Const.exception) || fc.name.equals(Const.exceptionReport))
		{
			// return false;
			throw new TechnicalException("gettingAvailableObservationsFailed");
		}
		ctx.model.startRDF();
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
		ctx.model.endRDF();
		return true;
	}
}

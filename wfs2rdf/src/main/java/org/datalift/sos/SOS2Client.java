package org.datalift.sos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

import org.datalift.model.Attribute;
import org.datalift.model.BaseServiceClient;
import org.datalift.model.ComplexFeature;
import org.datalift.model.ObservationMetaData;
import org.datalift.model.Store;
import org.datalift.utilities.Const;
import org.datalift.utilities.Helper;
import org.xml.sax.SAXException;

public class SOS2Client extends BaseServiceClient{

	public SOS2Client(String url)
	{
		super(url);
		serviceType="SOS";
	}


	public List<ObservationMetaData> getObservationOffering() {
		// 
		List<ObservationMetaData> observationOffering= new ArrayList<ObservationMetaData>();
		if(this.dataStore!=null && this.dataStore.getCapParsed!=null)
		{
			ComplexFeature root=this.dataStore.getCapParsed;

			if(root!=null)
			{
				ComplexFeature contents=root.findChildByType(Const.ContentsType);
				if(contents!=null)
				{
					for (Attribute a : contents.itsAttr) {
						if(a instanceof ComplexFeature)
						{
							ComplexFeature ObservationOffering=((ComplexFeature) a).findChildByType(Const.ObservationOfferingType);
							if(ObservationOffering!=null)
							{
								ObservationMetaData ftd=new ObservationMetaData();
								ComplexFeature  name= ObservationOffering.findChildByName(Const.nameSWES);
								ComplexFeature  identifier= ObservationOffering.findChildByName(Const.identifierSWES);
								ComplexFeature  description= ObservationOffering.findChildByName(Const.descriptionSWES);
								ComplexFeature  timePeriod= ObservationOffering.findChildByType(Const.TimePeriodType);
								ComplexFeature beginPosition=null,endPosition=null;
								if(timePeriod!=null)
								{
									beginPosition= timePeriod.findChildByName(Const.beginPosition);
									endPosition= timePeriod.findChildByName(Const.endPosition);
								}
								List<ComplexFeature> responsFormats= ObservationOffering.findChildren(Const.responseFormat);


								if(name!=null)
								{
									ftd.setName(name.value);
								}
								if(identifier!=null)
								{
									ftd.setIdentifier(identifier.value);
								}

								if(description!=null)
								{
									ftd.setDescription(description.value);
								}
								if(responsFormats!=null)
								{
									List<String> formats=new ArrayList<String>();
									for (ComplexFeature format : responsFormats) {
										formats.add(format.value);
									}
									ftd.setVailableFormat(formats);
								}
								if(beginPosition!=null)
								{
									ftd.setPhonomenonTimeBegin(Helper.getDate(beginPosition.value));

								}
								if(endPosition!=null)
								{
									ftd.setPhonomenonTimeEnd(Helper.getDate(endPosition.value));
								}

								observationOffering.add(ftd);
							}
						}

					}

				}
			}
		}
		return observationOffering;
	}


	public static void main (String[] args) throws Exception
	{
		SOS2Client c=new SOS2Client("");	
		c.getCapabilities();
		c.getObservationOffering();

	}


	public void getObservation(String id, String begin, String end, String format) throws Exception {
		Store ds=null;
		if(ds==null || ds.getFtParsed.size()==0)
		{ 
			ComplexFeature root;
			if(!Helper.isSet(begin) || !Helper.isSet(end) ||!Helper.isSet(format) )
			{
				return;
			}
			else
			{
				//the parameter om:phonomenonTime could be generalized
				root=parser.doParse(baseUrl+"?service=SOS&version=2.0.0&request=GetObservation&responseFormat=http://www.opengis.net/waterml/2.0"+/*format+*/"&temporalFilter=om:phenomenonTime,"+
			begin+"/"+end+"&featureOfInterest="+id);
				
			if(ds==null)
			{
				ds=new Store();
				cache.put(baseUrl+serviceType, ds);
			}
			//Get feature list
			ds.getFtParsed.put(id, root);
		}
		this.dataStore=ds;
		this.dataStore.getFtParsed=ds.getFtParsed;
	}
	}



}

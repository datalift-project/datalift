package org.datalift.sos;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.datalift.geoutility.Helper;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.model.Const;
import org.datalift.model.FeatureTypeDescription;
import org.datalift.model.Store;
import org.datalift.sos.model.ObservationMetaData;
import org.datalift.wfs.wfs2.parsing.GMLParser32;
import org.xml.sax.SAXException;

public class SOS2Client {

	private final static Map<String,Store> cache = new HashMap<String, Store>();
	//10, 3 * 3600
	public Store dataStore;
	private String serverUrl;
	private GMLParser32 parser;
	
	public SOS2Client(String url)
	{
		serverUrl=url;
		parser=new GMLParser32();
	}
	public void getCapabilities() throws ClientProtocolException, IOException, SAXException, ParserConfigurationException
	{
		Store ds=cache.get(serverUrl);
		if(ds==null || ds.getCapParsed==null || ds.getCapParsed.size()==0)
		{
			List<ComplexFeature> caps=parser.doParse(serverUrl+"?service=SOS&request=GetCapabilities");
			//Get feature list
			if(ds==null)
			{
				ds=new Store();
				cache.put(serverUrl, ds);
			}
			ds.getCapParsed=caps;	
		}
		this.dataStore=ds;
		this.dataStore.getCapParsed = ds.getCapParsed;
	}
	
	public List<ObservationMetaData> getObservationOffering() {
		// 
		List<ObservationMetaData> observationOffering= new ArrayList<ObservationMetaData>();
		if(this.dataStore!=null && this.dataStore.getCapParsed!=null)
		{
			ComplexFeature root=this.dataStore.getCapParsed.get(0);

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

	
		public static void main (String[] args) throws SAXException, ParserConfigurationException, IOException
		{
			SOS2Client c=new SOS2Client("");
//			ComplexFeature testRoot=new ComplexFeature();
//			
//			ComplexFeature A=new ComplexFeature();
//			A.name=new QName("aa","AA");
//			
//			
//			testRoot.itsAttr.add(A);
//			ComplexFeature B=new ComplexFeature();
//			
//			B.name=new QName("bb","BB");
//			ComplexFeature Bf=new ComplexFeature();
//			
//			Bf.name=new QName("bbf","BBf");
//			B.itsAttr.add(Bf);
//ComplexFeature Bf2=new ComplexFeature();
//			
//			Bf2.name=new QName("bbf2","BBf2");
//			B.itsAttr.add(Bf2);
//			testRoot.itsAttr.add(B);
//			ComplexFeature child=testRoot.findChild(new QName("bbf2","BBf2"), new QName("aa","AA"));
			
			c.getCapabilities();
			c.getObservationOffering();
			
		}
	
}

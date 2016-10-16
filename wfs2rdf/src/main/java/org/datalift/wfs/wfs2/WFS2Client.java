package org.datalift.wfs.wfs2;


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.http.client.ClientProtocolException;
import org.datalift.model.Attribute;
import org.datalift.model.BaseServiceClient;
import org.datalift.model.ComplexFeature;
import org.datalift.model.FeatureTypeDescription;
import org.datalift.model.Store;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.xml.sax.SAXException;

public class WFS2Client extends BaseServiceClient{

	public WFS2Client(String sourceUrl) {
		// TODO Auto-generated constructor stub	
		super (sourceUrl);
		serviceType="WFS";
	}

	//	public static void main (String[] args) throws SAXException, ParserConfigurationException, IOException
	//	{
	//		WFS2Parser mp=new WFS2Parser("http://geoservices.brgm.fr/risques?service=WFS&version=1.1.0&request=getCapabilities");
	//		mp.doParse(mp.url);
	//	}
	public void getFeatureType(String FeatureName, String srs) throws Exception
	{
		Store ds=null;
				//cache.get(url); url is not a suitbale key (should think about a proper key including options (srs)
		if(ds==null || ds.getFtParsed.size()==0)
		{ 
			ComplexFeature ft;
			if(srs!=null)
			{
				ft=parser.doParse(baseUrl+"?service=wfs&version=2.0.0&request=getFeature&typename="+FeatureName+"&srsName="+srs);
			}
			else
			{
				ft=parser.doParse(baseUrl+"?service=wfs&version=2.0.0&request=getFeature&typename="+FeatureName);
			}
			if(ds==null)
			{
				ds=new Store();
				cache.put(baseUrl+serviceType, ds);
			}
			//Get feature list
			ds.getFtParsed.put(FeatureName, ft);
		}
		this.dataStore=ds;
		this.dataStore.getFtParsed=ds.getFtParsed;
	}

	public List<FeatureTypeDescription> getFeatureTypeDescription() {
		// 
		List<FeatureTypeDescription> types=new ArrayList<FeatureTypeDescription>();
		if(this.dataStore!=null && this.dataStore.getCapParsed!=null)
		{
			ComplexFeature fc=this.dataStore.getCapParsed;

			if(fc!=null)
			{
				//QName name= new QName("http://www.opengis.net/wfs/2.0","FeatureTypeList");

				ComplexFeature child=fc.findFirstChild(Const.FeatureTypeList);
				System.out.println(child);
				if(child!=null)
				{
					for (Attribute a : child.itsAttr) {
						if(a instanceof ComplexFeature)
						{
							ComplexFeature ft=(ComplexFeature) a;
							if(ft.getTypeName().equals(Const.FeatureTypeType))
							{
								FeatureTypeDescription ftd=new FeatureTypeDescription();
								ComplexFeature  name= ft.findFirstChild(Const.Name);
								ComplexFeature  abstractt= ft.findFirstChild(Const.Abstract);
								ComplexFeature  title= ft.findFirstChild(Const.Title);
								ComplexFeature defaultsrs= ft.findFirstChild(Const.DefaultCRS);
								List<ComplexFeature> otherSrs= ft.findChildren(Const.OtherCRS);
								if(defaultsrs!=null)
								{	/*int delimeter= defaultsrs.value.lastIndexOf(":");
									String epsgSrs = defaultsrs.value.substring(delimeter+1);*/
									ftd.setDefaultSrs(Helper.constructSRIDValue(defaultsrs.value));
								}

								if(name!=null)
								{
									ftd.setName(name.value);
								}
								if(abstractt!=null)
								{
									ftd.setSummary(abstractt.value);
								}

								if(title!=null)
								{
									ftd.setTitle(title.value);
								}
								if(otherSrs!=null)
								{
									List<String> srss=new ArrayList<String>();
									for (ComplexFeature srs : otherSrs) {
										srss.add(Helper.constructSRIDValue(srs.value));
									}
									ftd.setOtherSrs(srss);
								}
								ftd.setNumberFeature(-1);
								types.add(ftd);
							}
						}

					}

				}

			}
		}
		return types;
	}

	/******to be moved from here later ****/
	private void buildFeatures(List<ComplexFeature> elements, Context ctx)
	{
		if(elements!=null)
		{
			ComplexFeature fc=elements.get(0);
			processFeatureCollection(fc, ctx);
		}
	}
	private void processFeatureCollection(ComplexFeature fc, Context ctx)
	{
		for (Attribute a : fc.itsAttr) {
			if(a instanceof ComplexFeature)
			{

				/* if(a.getTypeName().equals(Const.FeatureArrayPropertyType))
					 {
						 ComplexFeature members =(ComplexFeature)a;
						 processFeatureCollection(members,ctx);

					 }
					 else
					 {*/
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


}

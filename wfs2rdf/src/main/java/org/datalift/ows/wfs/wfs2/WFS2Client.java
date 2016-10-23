package org.datalift.ows.wfs.wfs2;


import java.util.ArrayList;
import java.util.List;

import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.BaseServiceClient;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.model.FeatureTypeDescription;
import org.datalift.ows.model.Store;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Helper;

/**
 * The WFS client for WFS 2.0.0 
 * @author Hanane Eljabiri
 *
 */
public class WFS2Client extends BaseServiceClient{

	public WFS2Client(String sourceUrl) {
		super (sourceUrl);
		serviceType="WFS";
	}
	public void getFeatureType(String FeatureName, String srs) throws Exception
	{
		Store ds=null;
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
								{	
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
}

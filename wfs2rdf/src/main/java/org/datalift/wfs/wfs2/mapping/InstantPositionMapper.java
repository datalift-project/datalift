package org.datalift.wfs.wfs2.mapping;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.geotools.referencing.wkt.Preprocessor;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

public class InstantPositionMapper extends BaseMapper{

	@Override
	public void map(ComplexFeature cf, Context ctx) {
		if(ignore(cf))
		{
			return;
		}
		this.setCfId(cf,ctx);
		if(!alreadyLinked)
		{
			this.addParentLinkStatements(cf, ctx);
		}
		this.rememberGmlId(cf,ctx);
		this.addRdfTypes(cf, ctx);
		this.addInstantValue(cf,ctx);
	}

	private void addInstantValue(ComplexFeature cf, Context ctx) {
		XMLGregorianCalendar d=Helper.getDate(cf.value);
		if(d!=null)
		{
			Value v5=ctx.vf.createLiteral(d);
			ctx.model.add(ctx.vf.createStatement(cf.getId(),ctx.vf.createURI(ctx.nsw3Time+"inXSDDateTime"), v5));
		}

	}

	@Override
	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsw3Time+"Instant")));
		if(isReferencedObject(cf))
		{
			ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(ctx.nsDatalift+Helper.capitalize(ctx.referencedObjectType.getLocalPart()))));
		}

	}

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {	
		Resource subjectURI;
		ComplexFeature parent=cf.getParent();
		if(parent!=null )
		{
			if(parent.getId()!=null)
			{
				subjectURI= parent.getId();
			}
			else
			{
				return;
			}
		}
		else 
		{
			subjectURI=ctx.DefaultSubjectURI;
		}
		/****add the parentlinked statement****/
		String p=cf.name.getLocalPart();
		URI preperty=null;
		if(p.contains("begin"))
		{
			preperty=ctx.vf.createURI(ctx.nsw3Time+"begin");
		}
		if(p.contains("end"))
		{
			preperty=ctx.vf.createURI(ctx.nsw3Time+"end");	
		}
		if(preperty!=null)
		{
			ctx.model.add(ctx.vf.createStatement(subjectURI, preperty, cf.getId()));
		}
	}

	@Override
	protected void setCfId(ComplexFeature cf, Context ctx) {
		/******give the feature an identifier****/
		Resource os;
		int count=0;		
		if(cf.getId()!=null)
		{
			alreadyLinked=true;
			return;
		}
		//check if there is any gml identifier. if yes, use it as id if not, create a generic id
		String id=cf.getAttributeValue(Const.identifier);
		if(id==null)
		{
			if(isReferencedObject(cf))
			{
				QName type=ctx.referencedObjectType;//Const.ReferenceType;
				count =ctx.getInstanceOccurences(type);
				os=ctx.vf.createURI(ctx.nsProject+type.getLocalPart()+"_"+count);
			}
			else
			{
				count=ctx.getInstanceOccurences(cf.name);
				os=ctx.vf.createBNode(cf.name.getLocalPart()+"_"+count);
			}		
			cf.setId(os);
		}
		else
		{
			try {
				cf.setId(ctx.vf.createURI(id));
			} catch (Exception e) {
				//if the id is not a valid URI, then use the standard base URI 
				cf.setId(ctx.vf.createURI(ctx.baseURI+id));
			}
		}	
		alreadyLinked=false;	
	}



}

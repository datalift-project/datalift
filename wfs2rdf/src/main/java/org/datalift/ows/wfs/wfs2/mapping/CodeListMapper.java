package org.datalift.ows.wfs.wfs2.mapping;

import javax.xml.namespace.QName;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandlerException;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.utilities.Helper;
import org.datalift.ows.utilities.SosConst;
import org.datalift.ows.model.Attribute;

/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class CodeListMapper extends BaseMapper{
	boolean clAlreadyDefined=false;
	
	@Override
	protected void mapWithParent(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!alreadyLinked)
		{
			super.mapWithParent(cf, ctx);
		}
	}
	@Override
	protected boolean mapAsIntermediate(ComplexFeature cf, Context ctx) {
		return false;
	}
	@Override
	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource id) throws RDFHandlerException {
		if(!clAlreadyDefined && !mappedAsSimple)
		{
			for (Attribute a : cf.itsAttr) {
				if(! (a instanceof ComplexFeature) && !a.name.equals(SosConst.frame))
				{

					if(a.getTypeName().equals(Const.hrefType) || a.getTypeName().equals(Const.anyURI))
					{
						ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(Context.nsRDFS+"isDefinedBy"), ctx.vf.createURI(a.value)));

					}
					if(a.getTypeName().equals(Const.titleAttrType))
					{
						String value=a.value;
						ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.vf.createURI(Context.nsSkos+"prefLabel"), ctx.vf.createLiteral(value)));
					} 

				}
			}
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
		Resource codeListRefId=ctx.codeListOccurences.get(cf.getAttributeValue(Const.href));
		if(codeListRefId!=null)
		{
			cf.setId(codeListRefId);
			clAlreadyDefined=true;
		}
		else
		{
			QName type=Context.referencedCodeListType;//Const.ReferenceType;
			count =ctx.getInstanceOccurences(type);
			os=ctx.vf.createURI(Context.nsProject+type.getLocalPart()+"_"+count);
			cf.setId(os);
			ctx.codeListOccurences.put(cf.getAttributeValue(Const.href), os);
		}
		alreadyLinked=false;		
	}
	@Override
	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		/////add the element as smod property
		if(cf.name.equals(Const.purpose) || cf.name.equals(Const.specialisedEMFType)) //get litteral value embeded in the feature (especially the litteraal in title)
		{
			URI smodPredicate=ctx.vf.createURI(Context.nsSmod+cf.name.getLocalPart());
			Value v=ctx.vf.createLiteral(cf.getLiteral());
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getParent().getId(), smodPredicate, v));

		}
		if(cf.name.equals(Const.mediaMonitored))
		{
			URI smodPredicate=ctx.vf.createURI(Context.nsSmod+cf.name.getLocalPart());
			Value v=ctx.vf.createURI(cf.getResource());
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getParent().getId(), smodPredicate, v));
		}
	}
	@Override
	protected void mapFeatureSimpleValue(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!clAlreadyDefined &&!cf.isSimple()){
			super.mapFeatureSimpleValue(cf, ctx);
		}

	}
	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {		
		if(!clAlreadyDefined && !cf.isSimple())
		{
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsDatalift+Helper.capitalize(cf.name.getLocalPart()))));
			ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI, ctx.vf.createURI(Context.nsSkos+"Concept")));
		}
	}
}

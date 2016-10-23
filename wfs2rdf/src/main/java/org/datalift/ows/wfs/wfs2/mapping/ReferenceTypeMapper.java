package org.datalift.ows.wfs.wfs2.mapping;

import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.openrdf.model.Resource;
import org.openrdf.rio.RDFHandlerException;
/**
 * A specific mapper
 * This class handles features that have as a type : xsd:referenceType
 * NB: the project defines another specific type called "referencedObject" as an xml element which has an "href" attribute. don't confuse  the two types please
 *
 * @author Hanane Eljabiri
 * 
 */
public class ReferenceTypeMapper extends BaseMapper{

	@Override
	protected boolean handleSpecificReferenceType(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(isCodeList(cf,ctx))
		{
			ctx.getMapper(Const.inspireCodeList).map(cf, ctx);
			return true;
		}
		if(isSosObservedProperty(cf,ctx))
		{
			ctx.getMapper(Const.observedProperty).map(cf, ctx);	
			return true;
		}	
		return false;
	}
	
	private boolean isSosObservedProperty(ComplexFeature cf, Context ctx) {
		if(cf.name.equals(Const.observedProperty))
		{
			return true;
		}
		return false;
	}

	private boolean isCodeList(ComplexFeature cf, Context ctx) {
		String potentielCodeList=cf.getAttributeValue(Const.href);
		if(potentielCodeList!=null)
		{
			for (String code : Context.codeList) {
				if(potentielCodeList.startsWith(code))
				{
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(cf.getTypeName().equals(Const.OM_ObservationPropertyType))
		{
			Resource subjectURI;
			if(cf.getParent()!=null)
			{
				subjectURI= cf.getParent().getId();
			}
			else 
			{
				subjectURI=Context.DefaultSubjectURI;
			}
			/****add the parentlinked statement****/
			ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), cf.getId()));
		}
		else
		{
			super.addParentLinkStatements(cf, ctx);
		}
	}
}

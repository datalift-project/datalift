package org.datalift.ows.wfs.wfs2.mapping;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.datalift.fwk.log.Logger;
import org.datalift.ows.model.Attribute;
import org.datalift.ows.model.ComplexFeature;
import org.datalift.ows.utilities.Const;
import org.datalift.ows.utilities.Context;
import org.datalift.ows.utilities.Helper;
import org.datalift.ows.utilities.SosConst;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.rio.RDFHandlerException;

/**
 * A specific mapper 
 * @author Hanane Eljabiri
 *
 */
public class BaseMapper implements Mapper {

	//*****************Class member
	protected final static Logger log = Logger.getLogger();
	protected boolean alreadyLinked = false;
	protected boolean mappedAsSimple=false;

	//************* The template method
	public final void map(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		boolean found=false;
		if(cf.name.getLocalPart().equals("hasObservation"))
			found=true;
		if (ignore(cf)) {
			return;
		}
		if(this.handleSpecificReferenceType(cf,ctx))
		{
			return;
		}
		this.setCfId(cf, ctx);
		if(isIntermediateFeature(cf))
		{
			if(this.mapAsIntermediate(cf,ctx))
			{
				return;
			}	
		}
		this.mapWithParent(cf,ctx);
		this.rememberGmlId(cf, ctx);
		this.addRdfTypes(cf, ctx);
		this.mapGeometryIfAny(cf,ctx);
		this.mapComplexChildren(cf,ctx);
		this.mapFeatureSimpleAttributes(cf, ctx, null);
		this.mapFeatureSimpleValue(cf,ctx);
	}

	
	//**************definition of the template method members (1st level)
	protected boolean ignore(ComplexFeature f) {
		// include the case where the element is just a wrapper, exp: member
		return (Helper.isEmpty(f) || f.name.equals(Const.identifier) || f.name.equals(Const.inspireId)) ? true : false;
	}

	protected void setCfId(ComplexFeature cf, Context ctx) {
		/****** give the feature an identifier ****/
		Resource os;
		int count = 0;
		if (cf.getId() != null) {
			alreadyLinked = true;
			return;
		}
		// check if there is any gml identifier. if yes, use it as id if not,
		// create a generic id
		String id = cf.getAttributeValue(Const.identifier);
		if (id == null) {
			if (cf.isReferencedObject()) {
				QName type = Context.referencedObjectType;// Const.ReferenceType;
				count = ctx.getInstanceOccurences(type);
				os = ctx.vf.createURI(Context.nsProject + type.getLocalPart() + "_" + count);
			} else {
				count = ctx.getInstanceOccurences(cf.name);
				os = ctx.vf.createURI(Context.nsProject + cf.name.getLocalPart() + "_" + count);
			}
			cf.setId(os);
		} else {
			try {
				cf.setId(ctx.vf.createURI(id));
			} catch (Exception e) {
				// if the id is not a valid URI, then use the standard base URI
				cf.setId(ctx.vf.createURI(ctx.baseURI + id));
			}
		}
		alreadyLinked = false;
	}

	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		Resource subjectURI;
		if (cf.getParent() != null) {
			if (cf.getParent().getId() != null) {
				subjectURI = cf.getParent().getId();
			} else {
				return;
			}
		} else {
			subjectURI = Context.DefaultSubjectURI;
		}
		/**** add the parentlinked statement ****/
		ctx.model.handleStatement(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift + cf.name.getLocalPart()),
				cf.getId()));
	}

	protected void addRdfTypes(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!cf.isSimple())
		{
			ctx.model.handleStatement((ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,
					ctx.vf.createURI(Context.nsDatalift + Helper.capitalize(cf.name.getLocalPart())))));
			if (cf.isReferencedObject()) {
				ctx.model.handleStatement(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,
						ctx.vf.createURI(Context.nsDatalift + Helper.capitalize(Context.referencedObjectType.getLocalPart()))));
			}			
		}
	}

	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource toLinkWith) throws RDFHandlerException {
		if(!mappedAsSimple)
		{Resource id;
		String predicate=null;
		if (toLinkWith == null) {
			id = cf.getId();
		} else {
			id = toLinkWith;
			predicate=Context.nsDatalift+cf.name.getLocalPart();
		}
		for (Attribute a : cf.itsAttr) {
			if (!(a instanceof ComplexFeature) && isUsefulAttribute(a)) {
				mapTypedValue(id, a.value, a.getTypeName(), a.name, predicate, ctx);
			}
		}
		}
	}
	
	protected boolean handleSpecificReferenceType(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		return false;
	}
	/**
	 * skip the current feature (cf) and map directly its only child with the parent 
	 * @param cf
	 * @param ctx
	 * @throws RDFHandlerException
	 */
	protected boolean mapAsIntermediate(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(cf.getParent()!=null && cf.getParent().isIntermediate)
		{
			return false;
		}
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				//exceptionnellement ici!!
				setCfId(f,ctx);
				addChildLinkedStatement(cf,f,ctx);		
				this.rememberGmlId(cf,ctx);
				if(cf.name.equals(Const.omResult))
				{
					Mapper m= ctx.getMapper(Const.omResult);
					if(m instanceof BaseMapper)
					{
						ctx.getMapper(f.getTypeName()).map(f, ctx);
					}
					else
					{
						m.map(cf, ctx);
					}
				}
				else
				{
					ctx.getMapper(f.getTypeName()).map(f, ctx);
				}
			}
		}
		return true;
	}
	
	protected void mapGeometryIfAny(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if (cf.vividgeom != null) {
			ctx.getMapper(new QName("geometry")).map(cf, ctx);
		}		
	}

	protected void mapComplexChildren(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature) a;
				ctx.getMapper(f.getTypeName()).map(f, ctx);
			}
		}		
	}

	protected void mapWithParent(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if (!alreadyLinked) {
			if (cf.isSimple()) {
				this.addParentSimpleLinkStatements(cf, ctx);
				return;
			} else {
				this.addParentLinkStatements(cf, ctx);
			}
		}		
	}

	protected void mapFeatureSimpleValue(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		if(!cf.isSimple())
		{if (Helper.isSet(cf.value)) {
			mapTypedValue(cf.getId(), cf.value, cf.getTypeName(), cf.name, null, ctx);
		}	
		}}

	/**
	 * save the gml id of the current feature to be used later to ensure cross-referencing
	 * @param cf
	 * @param ctx
	 */
	protected void rememberGmlId(ComplexFeature cf, Context ctx) {
		String id = cf.getAttributeValue(Const.id);
		if (id != null) {
			ctx.referenceCatalogue.put("#" + id, cf.getId().stringValue());
		}
	}
	protected void mapGeometryProperty(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		return;
	}

	//**************definition of the template method members (2nd level)	
	/**
	 * inserts special predicate whish links directely the current feature with its child : the shortcut
	 * @param cf : the current feature = behaves like the father
	 * @param f : the feature son whish will be used to linked to
	 * @param ctx
	 * @throws RDFHandlerException 
	 */
	protected void addChildLinkedStatement(ComplexFeature cf, ComplexFeature f, Context ctx) throws RDFHandlerException {
		ctx.model.handleStatement(ctx.vf.createStatement(cf.getParent().getId(), ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), f.getId()));
	}

	/**
	 * FOR SHORTCUT MAPPING : the feature's content is directly linked with its
	 * parent links the UNIQUE value of the cf with the subject cf.getParent.id
	 * using the name of cf as a predicat it generates ONE triple use case of
	 * this is <om:procedure xlink:href="urn:xxx"/>
	 * 
	 * @param cf
	 * @param ctx
	 * @throws RDFHandlerException 
	 */
	protected void addParentSimpleLinkStatements(ComplexFeature cf, Context ctx) throws RDFHandlerException {
		// first of all, look at the value of the feature. if any then try to
		// create the triple with the value of one attribute
		if (Helper.isSet(cf.value)) {
			mapTypedValue(cf.getParent().getId(), cf.value, cf.getTypeName(), cf.name, null, ctx);
			return;
		}
		this.mapFeatureSimpleAttributes(cf, ctx, cf.getParent().getId());
		mappedAsSimple=true;
	}
	//***** utilities method
	/**
	 * check if the attribute a worth to be mapped
	 * @param a
	 * @return
	 */
	protected boolean isUsefulAttribute(Attribute a)
	{
		return (!a.name.equals(Const.owns) && !a.name.equals(Const.nil) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.type) && !a.name.equals(Const.id) && !a.getTypeName().equals(Const.explicitType))?  true:false;
	}

	/**
	 * an intermediate feature is a feature with no simple attributes and ONLY
	 * ONE complex feature exp. <toto> <titi>... </toto> is an intermediate
	 * feature this method is designed' to be used to test if a feature worth to
	 * be mapped or not
	 * 
	 * @param cf
	 *            : the feature to test
	 * @return true if cf is an intermediate one, false else
	 */
	protected boolean isIntermediateFeature(ComplexFeature cf) {
		int nbr_cf_found = 0;
		if(Helper.isSet(cf.value))
		{
			return false;
		}
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				nbr_cf_found++;
			} else {
				if (!a.name.equals(Const.type) && !a.name.equals(Const.owns) && !a.name.equals(SosConst.frame)
						&& !a.name.equals(Const.nil) && !a.name.equals(Const.nilReason))
					return false;
			}
			if (nbr_cf_found > 1)
				return false;
		}
		cf.isIntermediate=true;
		return true;
	}

	/**
	 * return the id of the feature referenced by the hrefValue. 
	 * @param hrefValue
	 * @param ctx
	 * @return
	 */
	protected String getObjectIdReferencedBy(String hrefValue, Context ctx) {
		String id = ctx.referenceCatalogue.get(hrefValue);
		return (id != null) ? id : ctx.referenceCatalogue.get(null) + hrefValue;
	}
	protected void mapTypedValue(Resource id, String value, QName type, QName attrName, String predicate, Context ctx) throws RDFHandlerException {
		boolean added = false;
		if (type.equals(Const.xsdBoolean) && !attrName.equals(Const.owns) && !attrName.equals(Const.nil)) {
			if (predicate == null) {
				predicate = Context.nsDatalift + attrName.getLocalPart();
			}
			boolean bvalue = Boolean.valueOf(value);
			ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(bvalue)));
			added = true;
		}
		if (type.equals(Const.StringOrRefType)) {
			if (predicate == null) {
				predicate = Context.nsDcTerms + "description";
			}
			String svalue = value;
			ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added = true;
		}
		if (type.equals(Const.string)) {
			if (predicate == null) {
				predicate = Context.nsFoaf + "name";
			}
			String svalue = value;
			ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added = true;
		}
		if (type.equals(Const.positiveInteger)) {
			if (predicate == null) {
				predicate = Context.nsDatalift + attrName.getLocalPart();
			}
			int ivalue = Integer.valueOf(value);
			ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(ivalue)));
			added = true;
		}
		if (type.equals(Const.xsdDouble) || type.equals(Const.xsdDecimal)) {
			if (predicate == null) {
				predicate = Context.nsDatalift + attrName.getLocalPart();
			}
			double dvalue = Integer.valueOf(value);
			ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(dvalue)));
			added = true;
		}
		if (type.equals(Const.hrefType) || type.equals(Const.anyURI)) {
			try {
				if (predicate == null) {
					predicate = Context.nsDatalift + attrName.getLocalPart();
				}
				ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createURI(value)));
				added = true;
			} catch (IllegalArgumentException e) {
				log.warn(e.getMessage());
				ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate),
						ctx.vf.createURI(getObjectIdReferencedBy(value, ctx))));
				added = true;
			}
		}
		if (type.equals(Const.titleAttrType)) {
			if (predicate == null) {
				predicate = Context.nsDcTerms + "title";
			}
			String svalue = value;
			ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added = true;
		}
		if (type.equals(Const.TimePositionType) || type.equals(Const.xsdDate)
				|| type.equals(SosConst.TimeInstantType)) {
			XMLGregorianCalendar d = Helper.getDate(value);
			if (d != null) {
				if (predicate == null) {
					predicate = Context.nsDatalift + attrName.getLocalPart();
				}
				Value v5 = ctx.vf.createLiteral(d);
				ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), v5));
				added = true;
			}
		}
		if(!added)
		{
			if(predicate==null)
			{
				predicate=Context.nsDatalift+attrName.getLocalPart();
			}

			ctx.model.handleStatement(ctx.vf.createStatement(id, ctx.vf.createURI(predicate),
					ctx.vf.createLiteral(value)));
			added=true;
		}
	}
}

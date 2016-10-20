package org.datalift.wfs.wfs2.mapping;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.datalift.fwk.log.Logger;
import org.datalift.model.Attribute;
import org.datalift.model.ComplexFeature;
import org.datalift.utilities.Const;
import org.datalift.utilities.Context;
import org.datalift.utilities.Helper;
import org.datalift.utilities.SosConst;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;

public class BaseMapper implements Mapper {

	protected final static Logger log = Logger.getLogger();
	protected boolean alreadyLinked = false;

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

	protected void addParentLinkStatements(ComplexFeature cf, Context ctx) {
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
		ctx.model.add(ctx.vf.createStatement(subjectURI, ctx.vf.createURI(Context.nsDatalift + cf.name.getLocalPart()),
				cf.getId()));
	}

	protected void addRdfTypes(ComplexFeature cf, Context ctx) {
		ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,
				ctx.vf.createURI(Context.nsDatalift + Helper.capitalize(cf.name.getLocalPart()))));
		if (cf.isReferencedObject()) {
			ctx.model.add(ctx.vf.createStatement(cf.getId(), ctx.rdfTypeURI,
					ctx.vf.createURI(Context.nsDatalift + Helper.capitalize(Context.referencedObjectType.getLocalPart()))));
		}
	}

	protected void mapFeatureSimpleAttributes(ComplexFeature cf, Context ctx, Resource toLinkWith) {
		Resource id;
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
	protected boolean isUsefulAttribute(Attribute a)
	{
		return (!a.name.equals(Const.owns) && !a.name.equals(Const.nil) && !a.name.equals(SosConst.frame) && !a.name.equals(Const.type) && !a.name.equals(Const.id) && !a.getTypeName().equals(Const.explicitType))?  true:false;
	}

	/**
	 * an intermediate feature is a feature with no simple attributes and only
	 * one complex feature exp. <toto> <titi>... </toto> is an intermediate
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
		return true;
	}

	public void map(ComplexFeature cf, Context ctx) {
		boolean found=false;
		if(cf.name.getLocalPart().equals("purpose"))
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
			this.mapAsIntermediate(cf,ctx);
			return;
		}
		this.mapWithParent(cf,ctx);
		this.rememberGmlId(cf, ctx);
		if(!cf.isSimple())
		{
			this.addRdfTypes(cf, ctx);
		}
		this.mapGeometryIfAny(cf,ctx);
		this.mapComplexChildren(cf,ctx);
		if(!cf.isSimple())
		{
			this.mapFeatureSimpleAttributes(cf, ctx, null);
			this.mapFeatureSimpleValue(cf,ctx);
		}
	}

	protected boolean handleSpecificReferenceType(ComplexFeature cf, Context ctx) {
		return false;
	}

	protected void mapAsIntermediate(ComplexFeature cf, Context ctx) {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature)a;
				//exceptionnellement ici!!
				setCfId(f,ctx);
				addChildLinkedStatement(cf,f,ctx);		
				this.rememberGmlId(cf,ctx);
				//insert type of f if f will not be mappedwith basic mapper
				if(cf.name.equals(Const.omResult))
				{
					ctx.getMapper(Const.omResult).map(cf, ctx);
				}
				else
				{
					ctx.getMapper(f.getTypeName()).map(f, ctx);
				}
			}
		}
	}
	/**
	 * inserts special predicate whish links directely the current feature with its child : the shortcut
	 * @param cf : the current feature = behaves like the father
	 * @param f : the feature son whish will be used to linked to
	 * @param ctx
	 */
	protected void addChildLinkedStatement(ComplexFeature cf, ComplexFeature f, Context ctx) {
		ctx.model.add(ctx.vf.createStatement(cf.getParent().getId(), ctx.vf.createURI(Context.nsDatalift+cf.name.getLocalPart()), f.getId()));
	}

	protected void mapGeometryIfAny(ComplexFeature cf, Context ctx) {
		if (cf.vividgeom != null) {
			ctx.getMapper(new QName("geometry")).map(cf, ctx);
		}		
	}

	protected void mapComplexChildren(ComplexFeature cf, Context ctx) {
		for (Attribute a : cf.itsAttr) {
			if (a instanceof ComplexFeature) {
				ComplexFeature f = (ComplexFeature) a;
				ctx.getMapper(f.getTypeName()).map(f, ctx);
			}
		}		
	}

	protected void mapWithParent(ComplexFeature cf, Context ctx) {
		if (!alreadyLinked) {
			if (cf.isSimple()) {
				this.addParentSimpleLinkStatements(cf, ctx);
				return;
			} else {
				this.addParentLinkStatements(cf, ctx);
			}
		}		
	}

	protected void mapFeatureSimpleValue(ComplexFeature cf, Context ctx) {
		if (Helper.isSet(cf.value)) {
			mapTypedValue(cf.getId(), cf.value, cf.getTypeName(), cf.name, null, ctx);
		}	
	}

	/**
	 * FOR SHORTCUT MAPPING : the feature's content is directly linked with its
	 * parent links the UNIQUE value of the cf with the subject cf.getParent.id
	 * using the name of cf as a predicat it generates ONE triple use case of
	 * this is <om:procedure xlink:href="urn:xxx"/>
	 * 
	 * @param cf
	 * @param ctx
	 */
	protected void addParentSimpleLinkStatements(ComplexFeature cf, Context ctx) {
		// first of all, look at the value of the feature. if any then try to
		// create the triple with the value of one attribute
		if (Helper.isSet(cf.value)) {
			mapTypedValue(cf.getParent().getId(), cf.value, cf.getTypeName(), cf.name, null, ctx);
			return;
		}
		this.mapFeatureSimpleAttributes(cf, ctx, cf.getParent().getId());
	}

	protected void rememberGmlId(ComplexFeature cf, Context ctx) {
		// TODO Auto-generated method stub
		String id = cf.getAttributeValue(Const.id);
		if (id != null) {
			ctx.referenceCatalogue.put("#" + id, cf.getId().stringValue());
		}
	}

	protected String getObjectIdReferencedBy(String hrefValue, Context ctx) {
		String id = ctx.referenceCatalogue.get(hrefValue);
		return (id != null) ? id : ctx.referenceCatalogue.get(null) + hrefValue;
	}

	protected void mapTypedValue(Resource id, String value, QName type, QName attrName, String predicate, Context ctx) {
		boolean added = false;
		if (type.equals(Const.xsdBoolean) && !attrName.equals(Const.owns) && !attrName.equals(Const.nil)) {
			if (predicate == null) {
				predicate = Context.nsDatalift + attrName.getLocalPart();
			}
			boolean bvalue = Boolean.valueOf(value);
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(bvalue)));
			added = true;
		}
		if (type.equals(Const.StringOrRefType)) {
			if (predicate == null) {
				predicate = Context.nsDcTerms + "description";
			}
			String svalue = value;
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added = true;
		}
		if (type.equals(Const.string)) {
			if (predicate == null) {
				predicate = Context.nsFoaf + "name";
			}
			String svalue = value;
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
			added = true;
		}
		if (type.equals(Const.positiveInteger)) {
			if (predicate == null) {
				predicate = Context.nsDatalift + attrName.getLocalPart();
			}
			int ivalue = Integer.valueOf(value);
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(ivalue)));
			added = true;
		}
		if (type.equals(Const.xsdDouble) || type.equals(Const.xsdDecimal)) {
			if (predicate == null) {
				predicate = Context.nsDatalift + attrName.getLocalPart();
			}
			double dvalue = Integer.valueOf(value);
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(dvalue)));
			added = true;
		}
		if (type.equals(Const.hrefType) || type.equals(Const.anyURI)) {
			try {
				if (predicate == null) {
					predicate = Context.nsDatalift + attrName.getLocalPart();
				}
				ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createURI(value)));
				added = true;
			} catch (IllegalArgumentException e) {
				log.warn(e.getMessage());
				ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate),
						ctx.vf.createURI(getObjectIdReferencedBy(value, ctx))));
				added = true;
			}
		}
		if (type.equals(Const.titleAttrType)) {
			if (predicate == null) {
				predicate = Context.nsDcTerms + "title";
			}
			String svalue = value;
			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), ctx.vf.createLiteral(svalue)));
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
				ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate), v5));
				added = true;
			}
		}
		if(!added)
		{
			if(predicate==null)
			{
				predicate=Context.nsDatalift+attrName.getLocalPart();
			}

			ctx.model.add(ctx.vf.createStatement(id, ctx.vf.createURI(predicate),
					ctx.vf.createLiteral(value)));
			added=true;
		}
	}

	protected void mapGeometryProperty(ComplexFeature cf, Context ctx) {
		return;
	}
}

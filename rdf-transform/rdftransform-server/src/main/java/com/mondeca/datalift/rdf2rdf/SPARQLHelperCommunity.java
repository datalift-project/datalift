package com.mondeca.datalift.rdf2rdf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.datalift.fwk.sparql.SparqlQueries;
import org.openrdf.model.Resource;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.Repository;

import com.mondeca.datalift.rdf2rdf.model.JSONClass;
import com.mondeca.datalift.rdf2rdf.model.JSONProperty;
import com.mondeca.sesame.toolkit.query.SelectSPARQLHelperBase;

public class SPARQLHelperCommunity {

	abstract class PropertiesSparqlHelper extends SelectSPARQLHelperBase {
		
		protected String sparql;
		protected Repository repository;
		
		public PropertiesSparqlHelper(SparqlQueries queries, Repository repository) {
			this.sparql = queries.get(getQueryKey());
			this.repository = repository;
		}
		
		protected abstract String getQueryKey();
		
		@Override
		public String getSPARQL() {
			return this.sparql;
		}
	}
	
	class AllClassesHelper extends PropertiesSparqlHelper {

		protected LabelFetcher labelFetcher;
		private List<JSONClass> classes = new ArrayList<JSONClass>();

		public AllClassesHelper(SparqlQueries queries, Repository repository, LabelFetcher labelFetcher) {
			super(queries, repository);
			this.labelFetcher = labelFetcher;
		}

		@Override
		protected String getQueryKey() {
			return "classes.all";
		}
		@Override
		public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
			String typeURI = ((Resource)bs.getValue("type")).stringValue();
			
			System.out.println("Got a type : "+typeURI);
			
			JSONClass c = new JSONClass(typeURI);
			c.setClassName(LabelFetcher.concat(labelFetcher.getLabels(typeURI)));
			
			this.classes.add(c);
		}

		public List<JSONClass> getClasses() {
			return classes;
		}
	}
	
	class SuperTypeHelper extends PropertiesSparqlHelper {

		protected List<JSONClass> jsonClasses;
		private Map<String, String> superTypes = new HashMap<String, String>();

		public SuperTypeHelper(SparqlQueries queries, Repository repository, List<JSONClass> jsonClasses) {
			super(queries, repository);
			this.jsonClasses = jsonClasses;
		}

		@Override
		protected String getQueryKey() {
			return "classes.superType";
		}
		
		@Override
		public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
			String typeURI = ((Resource)bs.getValue("type")).stringValue();
			String supertypeURI = ((Resource)bs.getValue("superType")).stringValue();
			this.superTypes.put(typeURI, supertypeURI);
			
			for (JSONClass aJsonClass : this.jsonClasses) {
				if(aJsonClass.getClassURI().equals(typeURI)) {
					aJsonClass.setClassParentURI(supertypeURI);
				}
			}
		}

		public Map<String, String> getSuperTypes() {
			return superTypes;
		}

	}

	
	class AllPropertiesHelper extends PropertiesSparqlHelper {

		protected LabelFetcher labelFetcher;
		private List<JSONProperty> properties = new ArrayList<JSONProperty>();

		public AllPropertiesHelper(SparqlQueries queries, Repository repository, LabelFetcher labelFetcher) {
			super(queries, repository);
			this.labelFetcher = labelFetcher;
		}

		@Override
		protected String getQueryKey() {
			return "properties.all";
		}
		
		@Override
		public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
			String propertyURI = ((Resource)bs.getValue("property")).stringValue();
			Resource propertyType = ((Resource)bs.getValue("propertyType"));
			
			JSONProperty p = new JSONProperty(propertyURI);
			
			
			// do we already have that property ?
			if(this.properties.contains(p)) {
				p = this.properties.get(this.properties.indexOf(p));
			} else {
				p.setPropertyName(LabelFetcher.concat(labelFetcher.getLabels(propertyURI)));
				this.properties.add(p);
			}
			
			// on positionne le type une seule fois
			if(
					propertyType != null
					&&
					(
							propertyType.stringValue().equals(OWL.OBJECTPROPERTY)
							||
							propertyType.stringValue().equals(OWL.DATATYPEPROPERTY)
					)
			) {
				p.setPropertyType(propertyType.stringValue());
			}
			
		}

		public List<JSONProperty> getProperties() {
			return properties;
		}
	}
	
	class SuperPropertyHelper extends PropertiesSparqlHelper {

		protected List<JSONProperty> jsonProperties;
		private Map<String, String> superProperties = new HashMap<String, String>();

		public SuperPropertyHelper(SparqlQueries queries, Repository repository, List<JSONProperty> jsonProperties) {
			super(queries, repository);
			this.jsonProperties = jsonProperties;
		}

		@Override
		protected String getQueryKey() {
			return "properties.superProperty";
		}
		
		@Override
		public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
			String propertyURI = ((Resource)bs.getValue("property")).stringValue();
			String superPropertyURI = ((Resource)bs.getValue("superProperty")).stringValue();
			this.superProperties.put(propertyURI, superPropertyURI);
			
			for (JSONProperty aJsonProperty : this.jsonProperties) {
				if(aJsonProperty.getPropertyURI().equals(propertyURI)) {
					aJsonProperty.setPropertyParentURI(superPropertyURI);
				}
			}
		}

		public Map<String, String> getSuperProperties() {
			return superProperties;
		}

	}
	
}

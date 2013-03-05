package org.datalift.lov;

import java.util.HashMap;
import java.util.Map;

import org.datalift.fwk.sparql.SparqlQueries;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.Repository;

import com.mondeca.sesame.toolkit.query.SPARQLExecutionException;
import com.mondeca.sesame.toolkit.query.SelectSPARQLHelperBase;
import com.mondeca.sesame.toolkit.query.SesameSPARQLExecuter;

public class LabelFetcher {

	protected SparqlQueries queries;
	protected Repository repository;
	protected boolean generateDefaultIfEmpty = true;

	/**
	 * Concatenate a map of labels organized by language into a single String
	 * @param labels
	 * @return
	 */
	public static String concat(Map<String, String> labels) {
		if(labels == null || labels.size() == 0) {
			return "";
		}
		
		StringBuffer sb = new StringBuffer();
		for (Map.Entry<String, String> anEntry : labels.entrySet()) {
			sb.append(anEntry.getValue()+", ");
		}
		return sb.toString().substring(0, sb.toString().length() - ", ".length());
	}
	
	public LabelFetcher(SparqlQueries queries, Repository repository) {
		super();
		this.queries = queries;
		this.repository = repository;
	}
	
	public Map<String, String> getLabels(String uri) {
		// TODO : caching
		LabelFetcherHelper helper = new LabelFetcherHelper(this.queries, uri, this.repository.getValueFactory());
		try {
			(new SesameSPARQLExecuter(this.repository)).executeSelect(helper);
		} catch (SPARQLExecutionException e) {
			e.printStackTrace();
		}
		
		if(this.generateDefaultIfEmpty && helper.getLabels().size() == 0) {
			helper.getLabels().put(null, new DefaultLabelGenerator().generateLabel(uri));
		}
		return helper.getLabels();
	}
	
	class DefaultLabelGenerator {
		
		public String generateLabel(String uri) {
			
			String labelUri;
			// return everything after # ...
			if(uri.indexOf('#') > 0) {
				labelUri = uri.substring(uri.indexOf('#')+1); 
			// or after last /
			} else {
				labelUri = uri.substring(uri.lastIndexOf('/')+1);
			}
			
			// now replace _ with blanks
			String labelWithoutWhitespaces = labelUri.replaceAll("_", " ");
			
			// now handle camelCase
			String label = "";
			for(int i=0; i<labelWithoutWhitespaces.length();i++) {
				boolean addWhitespace = false;
				if(
						i != 0
						&&
						Character.isUpperCase(labelWithoutWhitespaces.charAt(i))
						&&
						Character.isLowerCase(labelWithoutWhitespaces.charAt(i-1))
				) {
					addWhitespace = true;
				}
				label += ((addWhitespace)?" ":"")+labelWithoutWhitespaces.charAt(i);
			}
			
			return label;
		}
		
	}
	
	class LabelFetcherHelper extends SelectSPARQLHelperBase {

		protected String sparql;
		protected String uri;
		protected ValueFactory factory;
		private Map<String, String> labels = new HashMap<String, String>();

		public LabelFetcherHelper(SparqlQueries queries, String uri, ValueFactory vf) {
			this.sparql = queries.get("labels");
			this.uri = uri;
			this.factory = vf;
		}
		
		@Override
		public String getSPARQL() {
			return this.sparql;
		}

		@Override
		public Map<String, Value> getBindings() {
			return new HashMap<String, Value>(){{
				put("x",factory.createURI(uri));
			}};
		}

		@Override
		public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
			Literal aLabel = ((Literal)bs.getValue("label"));
			if(labels.containsKey(aLabel.getLanguage())) {
				labels.put(aLabel.getLanguage(), labels.get(aLabel.getLanguage())+", "+aLabel.stringValue());
			} else {
				labels.put(aLabel.getLanguage(), aLabel.stringValue());
			}
		}

		public Map<String, String> getLabels() {
			return this.labels;
		}
	}	
	
	public boolean isGenerateDefaultIfEmpty() {
		return generateDefaultIfEmpty;
	}

	public void setGenerateDefaultIfEmpty(boolean generateDefaultIfEmpty) {
		this.generateDefaultIfEmpty = generateDefaultIfEmpty;
	}
	
	public static void main(String[] args) {
		Map<String, String> labels = new HashMap<String, String>(){{
			put("en","toto");
			put(null,"tata");
		}};
		System.out.println(concat(labels));
	}
	
}

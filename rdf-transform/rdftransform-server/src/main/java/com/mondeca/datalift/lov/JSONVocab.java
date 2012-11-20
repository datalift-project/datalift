package com.mondeca.datalift.lov;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerBase;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.repository.Repository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.inferencer.fc.DirectTypeHierarchyInferencer;
import org.openrdf.sail.memory.MemoryStore;

import com.mondeca.sesame.toolkit.query.DelegatingSelectSPARQLHelper;
import com.mondeca.sesame.toolkit.query.SesameSPARQLExecuter;
import com.mondeca.sesame.toolkit.query.builder.StringSPARQLQueryBuilder;
import com.mondeca.sesame.toolkit.repository.FileRepositoryLoader;

public class JSONVocab {

	protected String vocabURI;
	protected String vocabPrefix;
	protected String vocabNsp;
	protected String vocabName;

	public JSONVocab(String vocabURI) {
		super();
		this.vocabURI = vocabURI;
	}

	public String getVocabURI() {
		return vocabURI;
	}

	public String getVocabPrefix() {
		return vocabPrefix;
	}

	public void setVocabPrefix(String vocabPrefix) {
		this.vocabPrefix = vocabPrefix;
	}

	public String getVocabNsp() {
		return vocabNsp;
	}

	public void setVocabNsp(String vocabNsp) {
		this.vocabNsp = vocabNsp;
	}

	public String getVocabName() {
		return vocabName;
	}

	public void setVocabName(String vocabName) {
		this.vocabName = vocabName;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((vocabURI == null) ? 0 : vocabURI.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		JSONVocab other = (JSONVocab) obj;
		if (vocabURI == null) {
			if (other.vocabURI != null)
				return false;
		} else if (!vocabURI.equals(other.vocabURI))
			return false;
		return true;
	}

	public static void main(String[] args) throws Exception {
		File lovData = new File("/home/thomas/workspace/testing/apache-tomcat-7.0.27-datalift/bin/datalift-home/storage/lov/data");
		final Repository lovRepository = new SailRepository(new DirectTypeHierarchyInferencer(new MemoryStore()));
		// this.lovRepository = new SailRepository(new MemoryStore());
		lovRepository.initialize();
		FileRepositoryLoader loader = new FileRepositoryLoader(lovRepository);
		loader.load(Collections.singletonList(lovData.getAbsolutePath()));

		Properties p = new Properties();
		p.load(Thread.currentThread().getContextClassLoader().getResourceAsStream("com/mondeca/datalift/lov/sparql-queries.properties"));
		String query = p.get("prefixMappings")+" "+p.get("lov.search");
		System.out.println(query);

		SesameSPARQLExecuter executer = new SesameSPARQLExecuter(lovRepository);
		executer.executeSelect(
				new DelegatingSelectSPARQLHelper(
						new StringSPARQLQueryBuilder(query),
						new TupleQueryResultHandlerBase() {
							@Override
							public void handleSolution(BindingSet bs)
									throws TupleQueryResultHandlerException {
								String uri = ((Resource)bs.getValue("uri")).stringValue();

								Literal score = (Literal)bs.getValue("inverseScore");
								if(score != null) {
									System.out.println("score is NOT NULL !");

								} else {
									System.out.println("Score is null !");
								}
								
								String aLine = "";
								for (Binding aBinding : bs) {
									aLine += aBinding.getName()+":"+aBinding.getValue().stringValue()+"\t";
								}
								System.out.println(aLine);
							}							
						}
						) {



					@Override
					public Map<String, Value> getBindings() {
						return new HashMap<String, Value>() {{
							put("key",lovRepository.getValueFactory().createLiteral("person"));
							// put("type",lovRepository.getValueFactory().createURI("http://www.w3.org/2002/07/owl#Class"));
						}};
					}					
				}
				);

	}

}

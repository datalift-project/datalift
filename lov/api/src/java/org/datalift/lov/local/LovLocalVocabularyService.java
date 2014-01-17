package org.datalift.lov.local;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.lov.local.objects.VocabularySpace;
import org.datalift.lov.local.objects.vocab.VocabsDictionary;
import org.datalift.lov.local.objects.vocab.VocabsDictionaryItem;
import org.datalift.lov.local.objects.vocab.VocabularyVersion;
import org.openrdf.model.Literal;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class LovLocalVocabularyService {

	private final static Logger log = Logger
			.getLogger(LovLocalVocabularyService.class);

	private Configuration configuration;
	
	private VocabsDictionary vocabsCache;

	public LovLocalVocabularyService(Configuration configuration) {
		this.configuration = configuration;
	}
	
	public VocabsDictionaryItem getVocabularyWithUri(String uri) {
		loadVocabularies();
		return vocabsCache.getVocabularyWithURI(uri);
	}
	
	public VocabsDictionary getVocabularies() {
		loadVocabularies();
		return vocabsCache;
	}
	
	public List<VocabularySpace> getVocabularySpaces() {
		List<VocabularySpace> vocSpaces = new ArrayList<VocabularySpace>();
		
		StringBuilder query = new StringBuilder();
		query
			.append("SELECT DISTINCT ?title ?shortTitle WHERE {")
			.append("?s a <http://purl.org/vocommons/voaf#VocabularySpace>.")
			.append("?s <http://purl.org/dc/terms/title> ?title.")
			.append("?s <http://purl.org/ontology/bibo/shortTitle> ?shortTitle.")
			.append("}");
		
		RepositoryConnection conn = null;
		try {
			conn = this.configuration.getInternalRepository().newConnection();
			TupleQueryResult result = conn.prepareTupleQuery(
					QueryLanguage.SPARQL, LovConstants.PREFIXES + query.toString())
					.evaluate();
			
			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				String title = ((Literal) bindingSet.getBinding("title").getValue())
						.getLabel();
				String shortTitle = ((Literal) bindingSet.getBinding("shortTitle").getValue())
						.getLabel();
				vocSpaces.add(new VocabularySpace(title, shortTitle));
			}

		} catch (QueryEvaluationException e) {
			log.error("Query evaluation exception : {}", e.getMessage());
		} catch (RepositoryException e) {
			log.error("Repository exception : {}", e.getMessage());
		} catch (MalformedQueryException e) {
			log.error("Malformed query exception : {}", e.getMessage());
		} finally {
			LovUtil.closeQuietly(conn);
		}
		
		Collections.sort(vocSpaces);
		
		return vocSpaces;
	}
	
	private void loadVocabularies() {
		if (vocabsCache == null) {
			try {
				vocabsCache = getVocabularyDictionaryOverloaded();
			} catch (Exception e) {
				log.error("{}", e.getMessage());
				log.error("Stack trace :");
				for (StackTraceElement s : e.getStackTrace()) {
					log.error(s.toString());
				}
			}
		}
			
	}

	private VocabsDictionary getVocabularyDictionaryOverloaded()
			throws Exception {
		VocabsDictionary vocabs = new VocabsDictionary();
		String query = "SELECT distinct ?vocabNspUri ?vocabURI ?prefix ?title ?description "
				+ "WHERE{ "
				+ "	?vocabURI a voaf:Vocabulary. "
				+ "	?vocabURI vann:preferredNamespaceUri ?vocabNspUri. "
				+ "	?vocabURI vann:preferredNamespacePrefix ?prefix. "
				+ "	OPTIONAL{?vocabURI dcterms:title ?title.} "
				+ "	OPTIONAL{?vocabURI dcterms:description ?description.} "
				+ "}  " + "ORDER BY ?vocabNspUri  ";
		
		RepositoryConnection lovFileCon = configuration.getInternalRepository()
				.newConnection();
		TupleQueryResult result2 = lovFileCon.prepareTupleQuery(
				QueryLanguage.SPARQL, LovConstants.PREFIXES + query).evaluate();
		
		while (result2.hasNext()) {
			BindingSet bindingSet = result2.next();
			VocabsDictionaryItem vocab = vocabs.getVocabularyWithURI(bindingSet
					.getBinding("vocabURI").getValue().toString());
			if (vocab == null) {
				vocab = new VocabsDictionaryItem(bindingSet
						.getBinding("vocabURI").getValue().toString(),
						bindingSet.getBinding("vocabNspUri")
								.getValue().stringValue().toString(),
						bindingSet.getBinding("prefix").getValue()
								.stringValue().toString());
				vocabs.add(vocab);
			}

			// title
			if (bindingSet.getBinding("title") != null) {
				org.datalift.lov.local.objects.Literal title = new org.datalift.lov.local.objects.Literal(
						((Literal) bindingSet.getBinding("title").getValue())
								.getLabel(),
						((Literal) bindingSet.getBinding("title").getValue())
								.getLanguage(), null);
				if (!vocab.getTitles().contains(title))
					vocab.getTitles().add(title);
			}

			// description
			if (bindingSet.getBinding("description") != null) {
				org.datalift.lov.local.objects.Literal description = new org.datalift.lov.local.objects.Literal(
						((Literal) bindingSet.getBinding("description").getValue())
								.getLabel(),
						((Literal) bindingSet.getBinding("description").getValue())
								.getLanguage(), null);
				if (!vocab.getDescriptions().contains(description))
					vocab.getDescriptions().add(description);
			}

			// getlast version
			StringBuilder queryBuilder = new StringBuilder();
			queryBuilder
					.append("SELECT ?expression ?date ?label ?version ?manifestation \n");
			queryBuilder.append("WHERE { \n");
			queryBuilder.append("<" + vocab.getUri() + "> "
					+ LovConstants.FRBR_REALIZATION + " ?expression. \n");
			queryBuilder.append("?expression " + LovConstants.DC_TERMS_DATE
					+ " ?date. \n");
			queryBuilder.append("?expression <" + LovConstants.MREL_FULL_REV
					+ "> ?reviewer. \n");// force to have a reviewer
			queryBuilder.append("OPTIONAL{?expression "
					+ LovConstants.RDFS_LABEL + " ?label.} \n");
			queryBuilder.append("OPTIONAL{?expression "
					+ LovConstants.OWL_VERSION_INFO + " ?version.} \n");
			queryBuilder.append("?expression " + LovConstants.FRBR_EMBODIMENT
					+ " ?manifestation. FILTER(isIRI(?manifestation)) \n");
			queryBuilder.append("}ORDER BY DESC(?date) LIMIT 1");
			TupleQueryResult result = lovFileCon.prepareTupleQuery(
					QueryLanguage.SPARQL,
					LovConstants.PREFIXES + queryBuilder.toString()).evaluate();
			while (result.hasNext()) {
				BindingSet bindingSet1 = result.next();
				String date = bindingSet1.getBinding("date")
						.getValue().stringValue();
				String label = bindingSet1.getBinding("label")
						.getValue().stringValue();
				String version = null;
				if (bindingSet1.getBinding("version") != null)
					version = bindingSet1.getBinding("version")
							.getValue().stringValue();
				String manifestation = "";
				if (bindingSet1.getBinding("manifestation") != null)
					manifestation = bindingSet1.getBinding("manifestation")
							.getValue().stringValue();

				VocabularyVersion vocabVersion = new VocabularyVersion(date,
						label);
				vocabVersion.setVersionDecimal(version);
				vocabVersion.setLink(manifestation);

				vocab.setLastVersionReviewed(vocabVersion);
			}
		}

		return vocabs;
	}
}

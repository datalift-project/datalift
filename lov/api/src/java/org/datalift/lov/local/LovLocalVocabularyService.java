package org.datalift.lov.local;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.lov.local.objects.vocab.VocabsDictionary;
import org.datalift.lov.local.objects.vocab.VocabsDictionaryItem;
import org.datalift.lov.local.objects.vocab.VocabularyVersion;
import org.openrdf.model.Literal;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

public class LovLocalVocabularyService {

	private final static Logger log = Logger
			.getLogger(LovLocalVocabularyService.class);

	private Configuration configuration;

	public VocabsDictionary getVocabularyDictionaryOverloaded(
			RepositoryConnection lovFileCon) throws Exception {
		VocabsDictionary vocabs = new VocabsDictionary();
		String query = "SELECT distinct ?vocabNspUri ?vocabURI ?prefix ?title ?description "
				+ "WHERE{ "
				+ "	?vocabURI a voaf:Vocabulary. "
				+ "	?vocabURI vann:preferredNamespaceUri ?vocabNspUri. "
				+ "	?vocabURI vann:preferredNamespacePrefix ?prefix. "
				+ "	OPTIONAL{?vocabURI dcterms:title ?title.} "
				+ "	OPTIONAL{?vocabURI dcterms:description ?description.} "
				+ "}  " + "ORDER BY ?vocabNspUri  ";
		TupleQueryResult result2 = lovFileCon.prepareTupleQuery(
				QueryLanguage.SPARQL, LovConstants.PREFIXES + query).evaluate();
		while (result2.hasNext()) {
			BindingSet bindingSet = result2.next();
			VocabsDictionaryItem vocab = vocabs.getVocabularyWithURI(bindingSet
					.getBinding("vocabURI").getValue().toString());
			if (vocab == null) {
				vocab = new VocabsDictionaryItem(bindingSet
						.getBinding("vocabURI").getValue().toString(),
						((Literal) bindingSet.getBinding("vocabNspUri")
								.getValue()).stringValue().toString(),
						((Literal) bindingSet.getBinding("prefix").getValue())
								.stringValue().toString());
				vocabs.add(vocab);
			}

			// title
			if (bindingSet.getBinding("title") != null) {
				org.datalift.lov.local.objects.Literal title = new org.datalift.lov.local.objects.Literal(
						((Literal) bindingSet.getBinding("title").getValue())
								.stringValue().toString(),
						((Literal) bindingSet.getBinding("title").getValue())
								.getLanguage(), null);
				if (!vocab.getTitles().contains(title))
					vocab.getTitles().add(title);
			}

			// description
			if (bindingSet.getBinding("description") != null) {
				org.datalift.lov.local.objects.Literal description = new org.datalift.lov.local.objects.Literal(
						((Literal) bindingSet.getBinding("description")
								.getValue()).stringValue().toString(),
						((Literal) bindingSet.getBinding("description")
								.getValue()).getLanguage(), null);
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
				String date = ((Literal) bindingSet1.getBinding("date")
						.getValue()).stringValue();
				String label = ((Literal) bindingSet1.getBinding("label")
						.getValue()).stringValue();
				String version = null;
				if (bindingSet1.getBinding("version") != null)
					version = ((Literal) bindingSet1.getBinding("version")
							.getValue()).stringValue();
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

package org.datalift.lov.local;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.lov.local.objects.ResultItem;
import org.datalift.lov.local.objects.ResultItemMatch;
import org.datalift.lov.local.objects.ResultItemType;
import org.datalift.lov.local.objects.ResultItemVocSpace;
import org.datalift.lov.local.objects.SearchParams;
import org.datalift.lov.local.objects.SearchResult;
import org.datalift.lov.local.objects.TaxoNode;
import org.datalift.lov.local.objects.Vocabulary;

import org.openrdf.model.Literal;
import org.openrdf.query.BindingSet;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

public class LovLocalService {
	
	private final static Logger log = Logger.getLogger(LovLocalService.class);
	
	private Configuration configuration;
	
	public LovLocalService(Configuration configuration) {
		this.configuration = configuration;
	}
	
	public SearchResult search(String searchWords, int offset, int limit,
			String filter_Type, String filter_Domain, String filter_vocabulary) {
		
		RepositoryConnection conn = null;
		
		try {
			ResultsHandler resultsHandler = ResultsHandler.getInstance();

			// cas où première recherche ou nouvelle recherche
			if (resultsHandler.getResult_Filtered() == null
					|| !resultsHandler.getResult_Filtered().getSearch_query()
							.equals(searchWords)) {
				log.info("Search request for : {}.", searchWords);
//				logSearch(searchWords);
				conn = this.configuration.getInternalRepository().newConnection();

				SearchResult searchResult = new SearchResult();
				searchResult.setSearch_query(searchWords);
				if (limit > 0)
					searchResult.setLimit(limit);

				// on récupère les listes de valeurs
				List<Vocabulary> vocabulariesList = fetchVocabularyList(conn);
				TaxoNode vocabularySpaceRoot = fetchVocabularySpaces(conn);
				TaxoNode vocabList = new TaxoNode("AllVocabularies",
						"All Vocabularies");
				TaxoNode typesList = buildTypes();
				// Map<String, String> elemURI_LOVNbOcc =
				// fetchLOVNbOccMetrics();
				// Map<String, String> elemURI_LOVNbVoc =
				// fetchLOVNbVocMetrics();
				// Map<String, String> elemURI_LODNbOcc =
				// fetchLODNbOccMetrics();

				searchResult.setFacet_vocSpaces(vocabularySpaceRoot);
				searchResult.setFacet_types(typesList);

				// on initialise les paramètres
				// TODO passer les paramètres depuis l'UI
				SearchParams params = new SearchParams();
				searchResult.setParams(params);
				params.setWeightRatioSearchWordsInLabels(1);
				params.setWeightScoreNbMainLabel(0.5f);
				params.setWeightLodNbOcc(0.7f);
				
				StringBuilder query = new StringBuilder();
				query.append("SELECT DISTINCT * ");
				query.append("FROM ").append(LovUtil.LOV_CONTEXT_SPARQL);
				query.append(" WHERE {");
				query.append(" ?uri ?p ?label .");
				query.append(" FILTER(isURI(?uri))");
				query.append(" FILTER(isLiteral(?label))");
				query.append(" FILTER(CONTAINS(LCASE(STR(?label)),\"")
				     .append(searchWords.toLowerCase()).append("\"))");
				query.append(" ?uri rdf:type ?uriType .");
				// query.append(" OPTIONAL { ?vocabSpace dcterms:hasPart ?uri . }");
				query.append(" OPTIONAL {");
				query.append("   ?uri rdfs:isDefinedBy ?vocabulary .");
				query.append("   ?vocabulary rdf:type voaf:Vocabulary . }");
				query.append(" OPTIONAL { ?vocabSpace dcterms:hasPart ?vocabulary . }");
				query.append(" OPTIONAL { ?uri voaf:occurrencesInVocabularies ?occurrencesInVocabularies . }");
				query.append(" OPTIONAL { ?vocabulary voaf:reusedByVocabularies ?reusedByVocabularies . }");
				query.append(" OPTIONAL { ?vocabulary voaf:occurrencesInDatasets ?occurrencesInDatasets . }");
				query.append(" OPTIONAL { ?vocabulary voaf:reusedByDatasets ?reusedByDatasets . }");
				query.append("}");
				TupleQueryResult result = conn.prepareTupleQuery(
						QueryLanguage.SPARQL, LovConstants.PREFIXES + query.toString())
						.evaluate();

				List<ResultItem> resultItems = new ArrayList<ResultItem>();
				while (result.hasNext()) {
					BindingSet bindingSet = result.next();
					String uri = bindingSet.getBinding("uri").getValue()
							.toString();
					String p = bindingSet.getBinding("p").getValue().toString();
					String label = ((Literal) bindingSet.getBinding("label")
							.getValue()).stringValue().toString();
					String labelLang = ((Literal) bindingSet
							.getBinding("label").getValue()).getLanguage();
					String uriType = bindingSet.getBinding("uriType")
							.getValue().toString();
					String vocabulary = null;
					if (bindingSet.getBinding("vocabulary") != null)
						vocabulary = bindingSet.getBinding("vocabulary")
								.getValue().toString();
					String vocabSpace = null;
					if (bindingSet.getBinding("vocabSpace") != null)
						vocabSpace = bindingSet.getBinding("vocabSpace")
								.getValue().toString();

					String occurrencesInVocabularies = null;
					if (bindingSet.getBinding("occurrencesInVocabularies") != null)
						occurrencesInVocabularies = ((Literal) bindingSet
								.getBinding("occurrencesInVocabularies")
								.getValue()).stringValue().toString();
					String reusedByVocabularies = null;
					if (bindingSet.getBinding("reusedByVocabularies") != null)
						reusedByVocabularies = ((Literal) bindingSet
								.getBinding("reusedByVocabularies").getValue())
								.stringValue().toString();
					String occurrencesInDatasets = null;
					if (bindingSet.getBinding("occurrencesInDatasets") != null)
						occurrencesInDatasets = ((Literal) bindingSet
								.getBinding("occurrencesInDatasets").getValue())
								.stringValue().toString();
//					String reusedByDatasets = null;
//					if (bindingSet.getBinding("reusedByDatasets") != null)
//						reusedByDatasets = ((Literal) bindingSet.getBinding(
//								"reusedByDatasets").getValue()).stringValue()
//								.toString();

					// System.out.println(uri+"\t"+label);

					boolean firstTime = false;
					ResultItem resultItem = getResultItem(uri, resultItems);

					// new result
					if (resultItem == null) {
						resultItem = new ResultItem(uri);
						resultItems.add(resultItem);
						searchResult.setCount(searchResult.getCount() + 1);
						resultItem.setURIContainsSearchWords(uri.toLowerCase()
								.contains(searchWords.toLowerCase()));// scoring

						// addmetrics

						// LOV nb Occ
						if (occurrencesInVocabularies != null)
							resultItem.setLovNbOcc(Integer
									.parseInt(occurrencesInVocabularies));

						// LOV nb Voc
						if (reusedByVocabularies != null)
							resultItem.setLovNbVoc(Integer
									.parseInt(reusedByVocabularies));

						// LOD nb Occ
						if (occurrencesInDatasets != null)
							resultItem.setLodNbOcc(Integer
									.parseInt(occurrencesInDatasets));

						firstTime = true;
					}

					// System.out.println("URI: "+resultItem.getUri());
					resultItem.setUriPrefixed(prefixMachine(uri,
							vocabulariesList));
					// System.out.println("URI pref: "+resultItem.getUriPrefixed());

					// test pour exclure des doublons
					if (!resultItem.hasPropertyValue(p,
							addLangToValue(label, labelLang))) {
						ResultItemMatch match = new ResultItemMatch();
						match.setProperty(p);
						match.setPropertyPrefixed(prefixMachine(p,
								vocabulariesList));

						match.setValue(addLangToValue(label, labelLang));
						// System.out.println("value: "+label);
						String[] valueSmallOcc = getValueSmall(label,
								label.toLowerCase(), searchWords.toLowerCase());
						match.setValueShort(addLangToValue(valueSmallOcc[0],
								labelLang));
						String nbOccurences = valueSmallOcc[1];
						float ratio = Float.parseFloat(searchWords.length()
								+ "")
								* Float.parseFloat(nbOccurences)
								/ Float.parseFloat(label.length() + "");
						if (resultItem.getBestRatioSearchWordsInLabels() < ratio)
							resultItem.setBestRatioSearchWordsInLabels(ratio);
						// System.out.println("value small: "+resultItem.getValueSmall());
						resultItem.getMatches().add(match);
					}

					if (vocabulary != null) {
						resultItem.setVocabulary(vocabulary);
						resultItem.setVocabularyPrefix(getVocabularyPrefix(
								vocabulariesList, vocabulary));
						resultItem
								.setVocabularyLOVLink("http://lov.okfn.org/dataset/lov/details/vocabulary_"
										+ resultItem.getVocabularyPrefix()
										+ ".html");
						if (firstTime) {
							LovLocalService
									.addVocabularyIfNotPresentInTaxoNode(
											resultItem.getVocabulary(),
											resultItem.getVocabularyPrefix(),
											vocabList);
							LovLocalService.incrementTaxoNode(
									resultItem.getVocabulary(), vocabList);
						}
					}
					if (vocabSpace != null & firstTime) {
						incrementTaxoNode(vocabSpace, vocabularySpaceRoot);
						List<String[]> list = getStringsHierarchy(vocabSpace,
								vocabularySpaceRoot);
						for (int i = 0; i < list.size(); i++) {
							// System.out.println("vocabSpaceType= "+i+"\t"+list.get(i)[0]+"\t"+list.get(i)[1]);
							ResultItemVocSpace vocSpace = new ResultItemVocSpace();
							vocSpace.setUri(list.get(i)[0]);
							vocSpace.setLovLink("http://lov.okfn.org/dataset/lov/details/vocabularySpace_"
									+ URLEncoder.encode(list.get(i)[1], "UTF-8")
									+ ".html");
							vocSpace.setLabel(list.get(i)[1]);
							resultItem.getVocSpaces().add(vocSpace);
						}
					}

					// on peut avoir plusieurs types
					if (!resultItem.hasType(uriType)) {
						if (!incrementTaxoNode(uriType, typesList)) {
							incrementTaxoNode("other", typesList);
							ResultItemType type = new ResultItemType();
							type.setUri(uriType);
							type.setUriPrefixed(prefixMachine(uriType,
									vocabulariesList));
							// System.out.println("type: "+uriType);
							resultItem.getTypes().add(type);
						} else {
							List<String[]> list = getStringsHierarchy(uriType,
									typesList);
							for (int i = 0; i < list.size(); i++) {
								// System.out.println("type: "+list.get(i)[0]);
								if (!resultItem.hasType(list.get(i)[0])) {
									ResultItemType type = new ResultItemType();
									type.setUri(list.get(i)[0]);
									type.setUriPrefixed(list.get(i)[1]);
									resultItem.getTypes().add(type);
								}
							}
						}
					}
					// if(firstTime){
					// if(vocabSpace!=null){
					// resultItem.getVocabularySpaces().add(vocabSpace);
					// TaxoNode node =
					// getTaxoNodePresent(vocabSpace,vocabularySpaceRoot);
					// if(node!=null){
					// resultItem.getVocabularySpacesTitle().add(node.getTitle());
					// }
					// }
					// }
				}

				// System.out.println("traitement des resultats terminé");

				// classer les propriétés et les types par ordre d'importance et
				// calculer les max des metriques
				orderPropAndClassesAndComputeMetricsMax(resultItems,
						searchWords, params);

				// System.out.println("order intra élément terminé");

				// classer par importance et ajouter
				searchResult.setResults(ranking(resultItems, params));
				searchResult
						.setFacet_vocs(orderVocabulariesInTaxoNode(vocabList));
				// System.out.println("ranking des résultat terminé");

//				log.info("Found {} result(s).", searchResult.getCount());
				resultsHandler.setResult_All(searchResult, new TaxoNode(
						vocabularySpaceRoot), buildTypes());
//				log.info("Results are set.");
//				SearchResult results = resultsHandler.getResult(0, filter_Type, filter_Domain,
//						filter_vocabulary);
//				log.info("Starting JSON serialization.");
//				String json = results.toJSON();
//				log.info("JSON result : {} ", json);

				return resultsHandler.getResult(0, filter_Type, filter_Domain,
						filter_vocabulary);
			} else {
				// l'offset a changé
				if (resultsHandler.getResult_Filtered().getOffset() != offset) {
					return resultsHandler.getResultOffset(offset);
				} else {
					return resultsHandler.getResult(0, filter_Type, filter_Domain,
							filter_vocabulary);
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
			log.error("An error occured when searching. {}", e);
		} finally {
			LovUtil.closeQuietly(conn);
		}
		return new SearchResult();
	}

	private ResultItem getResultItem(String uri, List<ResultItem> resultItems) {
		for (int i = resultItems.size() - 1; i > -1; i--) {
			if (resultItems.get(i).getUri().equals(uri))
				return resultItems.get(i);
		}
		return null;
	}

	private List<ResultItem> ranking(List<ResultItem> searchResult,
			SearchParams params) {
		// calcul du score les résultats selon l'ordre d'importance
		for (ResultItem res : searchResult) {
			res.computeScore(params);
			// System.out.println("res score: "+res.getScore());
		}

		// trier
		List<ResultItem> list = new ArrayList<ResultItem>(searchResult.size());
		float bestScore = -1;
		int bestIndex = -1;
		while (searchResult.size() > 0) {
			// System.out.println(searchResult.size());
			for (int i = 0; i < searchResult.size(); i++) {
				float score = searchResult.get(i).getScore();
				// System.out.println("score "+new Float(score).toString());
				if (score > bestScore) {
					bestScore = score;
					bestIndex = i;
				}
			}
			list.add(searchResult.get(bestIndex));
			searchResult.remove(bestIndex);
			bestScore = -1;
		}
		// System.out.println("scoring ok");
		return list;
	}

	private void orderPropAndClassesAndComputeMetricsMax(
			List<ResultItem> searchResult, String searchWords,
			SearchParams params) {
		for (int i = 0; i < searchResult.size(); i++) {
			ResultItem res = searchResult.get(i);

			// prioritiser les attributs interessant
			orderProperties(res, searchWords, params);
			// prioritiser les types interessant
			orderTypes(res);

			// calcul des metriques max
			if (res.getScore_nbMainLabel() > params.getMaxScore_nbMainLabel()) {
				params.setMaxScore_nbMainLabel(res.getScore_nbMainLabel());
			}
			if (res.getScore_nbSecLabel() > params.getMaxScore_nbSecLabel()) {
				params.setMaxScore_nbSecLabel(res.getScore_nbSecLabel());
			}
			if (res.getLodNbOcc() > params.getMaxLodNbOcc()) {
				params.setMaxLodNbOcc(res.getLodNbOcc());
			}
			if (res.getLovNbOcc() > params.getMaxLovNbOcc()) {
				params.setMaxLovNbOcc(res.getLovNbOcc());
			}
			if (res.getLovNbVoc() > params.getMaxLovNbVoc()) {
				params.setMaxLovNbVoc(res.getLovNbVoc());
			}
		}
	}

	private void orderProperties(ResultItem res, String searchWords,
			SearchParams params) {
		List<ResultItemMatch> matches = new ArrayList<ResultItemMatch>(res
				.getMatches().size());

		// on sépare en deux pour les stats du nombres de mainlabel et secLabel
		List<String> propRankMain = new ArrayList<String>();
		propRankMain.add("http://www.w3.org/2000/01/rdf-schema#label");
		propRankMain.add("http://purl.org/dc/terms/title");
		propRankMain.add("http://purl.org/dc/elements/1.1/title");
		propRankMain.add("http://www.w3.org/2004/02/skos/core#prefLabel");

		List<String> propRankSec = new ArrayList<String>();
		propRankSec.add("http://www.w3.org/2004/02/skos/core#altLabel");
		propRankSec.add("http://www.w3.org/2000/01/rdf-schema#comment");
		propRankSec.add("http://purl.org/dc/terms/description");
		propRankSec.add("http://purl.org/dc/elements/1.1/description");

		// ajoute dans l'ordre les propriétés et valeurs principales
		for (int i = 0; i < propRankMain.size(); i++) {
			for (int j = res.getMatches().size() - 1; j > -1; j--) {
				if (res.getMatches().get(j).getProperty()
						.equals(propRankMain.get(i))) {
					ResultItemMatch match = new ResultItemMatch();
					match.setProperty(res.getMatches().get(j).getProperty());
					match.setPropertyPrefixed(res.getMatches().get(j)
							.getPropertyPrefixed());
					match.setValue(res.getMatches().get(j).getValue());
					match.setValueShort(res.getMatches().get(j).getValueShort());
					matches.add(match);
					res.getMatches().remove(j);
					// scoring
					res.incrementScore_nbMainLabel();
				}
			}
		}
		// ajoute dans l'ordre les propriétés et valeurs secondaires
		for (int i = 0; i < propRankSec.size(); i++) {
			for (int j = res.getMatches().size() - 1; j > -1; j--) {
				if (res.getMatches().get(j).getProperty()
						.equals(propRankSec.get(i))) {
					ResultItemMatch match = new ResultItemMatch();
					match.setProperty(res.getMatches().get(j).getProperty());
					match.setPropertyPrefixed(res.getMatches().get(j)
							.getPropertyPrefixed());
					match.setValue(res.getMatches().get(j).getValue());
					match.setValueShort(res.getMatches().get(j).getValueShort());
					matches.add(match);
					res.getMatches().remove(j);
					// scoring
					res.incrementScore_nbSecLabel();
				}
			}
		}
		// ajoute les prop et valeurs restantes
		for (int i = 0; i < res.getMatches().size(); i++) {
			ResultItemMatch match = new ResultItemMatch();
			match.setProperty(res.getMatches().get(i).getProperty());
			match.setPropertyPrefixed(res.getMatches().get(i)
					.getPropertyPrefixed());
			match.setValue(res.getMatches().get(i).getValue());
			match.setValueShort(res.getMatches().get(i).getValueShort());
			matches.add(match);
		}
		res.setMatches(matches);

	}

	private void orderTypes(ResultItem res) {
		List<ResultItemType> types = new ArrayList<ResultItemType>(res
				.getTypes().size());

		List<String> typesRank = new ArrayList<String>();
		typesRank.add(LovConstants.VOAF_FULL_VOCABULARY);
		typesRank.add("http://www.w3.org/2002/07/owl#Class");
		typesRank.add("http://www.w3.org/2000/01/rdf-schema#Datatype");
		typesRank.add("http://www.w3.org/2000/01/rdf-schema#Class");
		typesRank.add("http://www.w3.org/2002/07/owl#AsymmetricProperty");
		typesRank
				.add("http://www.w3.org/2002/07/owl#InverseFunctionalProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#IrreflexiveProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#ReflexiveProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#SymmetricProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#TransitiveProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#ObjectProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#DatatypeProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#AnnotationProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#FunctionalProperty");
		typesRank.add("http://www.w3.org/2002/07/owl#OntologyProperty");
		typesRank.add("http://www.w3.org/1999/02/22-rdf-syntax-ns#Property");

		// ajoute dans l'ordre les types
		for (int i = 0; i < typesRank.size(); i++) {
			for (int j = res.getTypes().size() - 1; j > -1; j--) {
				if (res.getTypes().get(j).getUri().equals(typesRank.get(i))) {
					ResultItemType type = new ResultItemType();
					type.setUri(res.getTypes().get(j).getUri());
					type.setUriPrefixed(res.getTypes().get(j).getUriPrefixed());
					types.add(type);
					res.getTypes().remove(j);
				}
			}
		}
		// ajoute les types restants
		for (int i = 0; i < res.getTypes().size(); i++) {
			ResultItemType type = new ResultItemType();
			type.setUri(res.getTypes().get(i).getUri());
			type.setUriPrefixed(res.getTypes().get(i).getUriPrefixed());
			types.add(type);
		}
		res.setTypes(types);
	}

	public static final int regexOccur(String text, String regex) {
		Matcher matcher = Pattern.compile(regex).matcher(text);
		int occur = 0;
		while (matcher.find()) {
			occur++;
		}
		return occur;
	}

	private String addLangToValue(String value, String lang) {
		if (lang != null && !lang.equals("null")) {
			return value + " <span style='color:#093;'>@" + lang + "</span>";
		} else
			return value;
	}

	private String[] getValueSmall(String value, String valueLow,
			String searchWordsLow) {
		int windowSize = 20;
		String addBeforeMatch = "<b>";
		String addAfterMatch = "</b>";
		String output = "";
		int nbOccurence = 0;
		int begin = 0;
		int end = 0;
		int lastend = 0;
		while (valueLow.indexOf(searchWordsLow, lastend) > -1) {
			nbOccurence++;
			// System.out.println("lastEnd: "+lastend);
			begin = valueLow.indexOf(searchWordsLow, lastend);
			end = begin + searchWordsLow.length();
			if ((begin - windowSize) <= lastend) {
				if ((end + windowSize) >= value.length()) {
					int nextIndex = valueLow.indexOf(searchWordsLow, begin
							+ searchWordsLow.length());
					if (nextIndex > 0 && nextIndex < end + windowSize) {
						// cas o� il faut s'arreter avant la fin parcequ'il y a
						// une autre occurence
						// output+=value.substring(lastend, nextIndex);
						output += value.substring(lastend, begin)
								+ addBeforeMatch
								+ value.substring(begin,
										begin + searchWordsLow.length())
								+ addAfterMatch
								+ value.substring(
										begin + searchWordsLow.length(),
										nextIndex);
						lastend = nextIndex;
					} else {
						// cas où nos 2 fenetres sont trop grandes
						// output+=value.substring(lastend, value.length());
						output += value.substring(lastend, begin)
								+ addBeforeMatch
								+ value.substring(begin,
										begin + searchWordsLow.length())
								+ addAfterMatch
								+ value.substring(
										begin + searchWordsLow.length(),
										value.length());
						lastend = value.length();
					}
				} else {
					// cas où seule la fenetre avant est trop grande
					// output+=value.substring(lastend, end+windowSize)+"...";
					output += value.substring(lastend, begin)
							+ addBeforeMatch
							+ value.substring(begin,
									begin + searchWordsLow.length())
							+ addAfterMatch
							+ value.substring(begin + searchWordsLow.length(),
									end + windowSize) + "...";
					lastend = end + windowSize;
				}
			} else {
				if ((end + windowSize) >= value.length()) {
					int nextIndex = valueLow.indexOf(searchWordsLow, begin
							+ searchWordsLow.length());
					if (nextIndex > 0 && nextIndex < end + windowSize) {
						// cas où il faut s'arreter avant la fin parcequ'il y a
						// une autre occurence
						// output+="..."+value.substring(begin-windowSize,
						// nextIndex);
						output += "..."
								+ value.substring(begin - windowSize, begin)
								+ addBeforeMatch
								+ value.substring(begin,
										begin + searchWordsLow.length())
								+ addAfterMatch
								+ value.substring(
										begin + searchWordsLow.length(),
										nextIndex);
						lastend = nextIndex;
					} else {
						// cas où seule la fenetre après est trop longue sont
						// trop grandes
						// output+="..."+value.substring(begin-windowSize,
						// value.length());
						output += "..."
								+ value.substring(begin - windowSize, begin)
								+ addBeforeMatch
								+ value.substring(begin,
										begin + searchWordsLow.length())
								+ addAfterMatch
								+ value.substring(
										begin + searchWordsLow.length(),
										value.length());
						lastend = value.length();
					}
				} else {
					// cas où tout va bien
					// output+="..."+value.substring(begin-windowSize,
					// end+windowSize)+"...";
					output += "..."
							+ value.substring(begin - windowSize, begin)
							+ addBeforeMatch
							+ value.substring(begin,
									begin + searchWordsLow.length())
							+ addAfterMatch
							+ value.substring(begin + searchWordsLow.length(),
									end + windowSize) + "...";
					lastend = end + windowSize;
				}
			}
		}
		return new String[] { output, "" + nbOccurence };
	}

	private String prefixMachine(String uri, List<Vocabulary> vocabulariesList) {
		// test si c'est un vocabulary
		for (int i = 0; i < vocabulariesList.size(); i++) {
			if (uri.equals(vocabulariesList.get(i).getUri()))
				return vocabulariesList.get(i).getPrefix();
		}

		for (int i = 0; i < vocabulariesList.size(); i++) {
			if (uri.contains(vocabulariesList.get(i).getNamespace()))
				return uri.replace(vocabulariesList.get(i).getNamespace(),
						vocabulariesList.get(i).getPrefix() + ":");
		}
		return uri;
	}

	private String getVocabularyPrefix(List<Vocabulary> vocabulariesList,
			String uri) {
		for (int i = 0; i < vocabulariesList.size(); i++) {
			if (vocabulariesList.get(i).getUri().equals(uri))
				return vocabulariesList.get(i).getPrefix();
		}
		return uri;
	}

	private TaxoNode buildTypes() {
		TaxoNode owl_thing = new TaxoNode(
				"http://www.w3.org/2002/07/owl#Thing", "owl:Thing");
		TaxoNode rdfs_Class = new TaxoNode(
				"http://www.w3.org/2000/01/rdf-schema#Class", "rdfs:Class");
		owl_thing.add(rdfs_Class);
		TaxoNode owl_Class = new TaxoNode(
				"http://www.w3.org/2002/07/owl#Class", "owl:Class");
		rdfs_Class.add(owl_Class);
		TaxoNode rdfs_Datatype = new TaxoNode(
				"http://www.w3.org/2000/01/rdf-schema#Datatype",
				"rdfs:Datatype");
		rdfs_Class.add(rdfs_Datatype);
		TaxoNode rdf_Property = new TaxoNode(
				"http://www.w3.org/1999/02/22-rdf-syntax-ns#Property",
				"rdf:Property");
		owl_thing.add(rdf_Property);
		TaxoNode owl_AnnotationProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#AnnotationProperty",
				"owl:AnnotationProperty");
		rdf_Property.add(owl_AnnotationProperty);
		TaxoNode owl_DatatypeProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#DatatypeProperty",
				"owl:DatatypeProperty");
		rdf_Property.add(owl_DatatypeProperty);
		TaxoNode owl_FunctionalProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#FunctionalProperty",
				"owl:FunctionalProperty");
		rdf_Property.add(owl_FunctionalProperty);
		TaxoNode owl_ObjectProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#ObjectProperty",
				"owl:ObjectProperty");
		rdf_Property.add(owl_ObjectProperty);
		TaxoNode owl_AsymmetricProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#AsymmetricProperty",
				"owl:AsymmetricProperty");
		owl_ObjectProperty.add(owl_AsymmetricProperty);
		TaxoNode owl_InverseFunctionalProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#InverseFunctionalProperty",
				"owl:InverseFunctionalProperty");
		owl_ObjectProperty.add(owl_InverseFunctionalProperty);
		TaxoNode owl_IrreflexiveProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#IrreflexiveProperty",
				"owl:IrreflexiveProperty");
		owl_ObjectProperty.add(owl_IrreflexiveProperty);
		TaxoNode owl_ReflexiveProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#ReflexiveProperty",
				"owl:ReflexiveProperty");
		owl_ObjectProperty.add(owl_ReflexiveProperty);
		TaxoNode owl_SymmetricProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#SymmetricProperty",
				"owl:SymmetricProperty");
		owl_ObjectProperty.add(owl_SymmetricProperty);
		TaxoNode owl_TransitiveProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#TransitiveProperty",
				"owl:TransitiveProperty");
		owl_ObjectProperty.add(owl_TransitiveProperty);
		TaxoNode owl_OntologyProperty = new TaxoNode(
				"http://www.w3.org/2002/07/owl#OntologyProperty",
				"owl:OntologyProperty");
		rdf_Property.add(owl_OntologyProperty);
		TaxoNode voaf_Vocabulary = new TaxoNode(
				LovConstants.VOAF_FULL_VOCABULARY, "voaf:Vocabulary");
		owl_thing.add(voaf_Vocabulary);
		TaxoNode other = new TaxoNode("other", "Other");
		owl_thing.add(other);

		return owl_thing;
	}

	private List<Vocabulary> fetchVocabularyList(RepositoryConnection con)
			throws LOVException {
		try {
			String query = "SELECT DISTINCT ?vocUri ?vocPrefix ?vocNamespace ?vocabSpace "
					+ "WHERE{ "
					+ "?vocUri a voaf:Vocabulary. "
					+ "?vocUri <http://purl.org/vocab/vann/preferredNamespacePrefix> ?vocPrefix.  "
					+ "?vocUri <http://purl.org/vocab/vann/preferredNamespaceUri> ?vocNamespace.  "
					+ "FILTER(isliteral(?vocPrefix)) "
					+ "FILTER(isliteral(?vocNamespace)) "
					+ "?vocabSpace dcterms:hasPart ?vocUri. " + "}";
			TupleQueryResult result = con.prepareTupleQuery(
					QueryLanguage.SPARQL, LovConstants.PREFIXES + query)
					.evaluate();
			List<Vocabulary> vocabularies = new ArrayList<Vocabulary>();
			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				String vocUri = bindingSet.getBinding("vocUri").getValue()
						.toString();

				String vocPrefix = ((Literal) bindingSet
						.getBinding("vocPrefix").getValue()).stringValue()
						.toString();
				String vocNamespace = ((Literal) bindingSet.getBinding(
						"vocNamespace").getValue()).stringValue().toString();
				String vocabSpace = bindingSet.getBinding("vocabSpace")
						.getValue().toString();
//				if (vocUri.equals("http://purl.org/dc/terms/")) {
//					System.out.println("this is DC");
//				}
				Vocabulary v = new Vocabulary();
				v.setUri(vocUri);
				v.setPrefix(vocPrefix);
				v.setNamespace(vocNamespace);
				v.setVocabSpace(vocabSpace);
				vocabularies.add(v);
			}
			Vocabulary rdf = new Vocabulary();
			rdf.setUri("http://www.w3.org/1999/02/22-rdf-syntax-ns");
			rdf.setNamespace("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			rdf.setPrefix("rdf");
			vocabularies.add(rdf);
			Vocabulary rdfs = new Vocabulary();
			rdfs.setUri("http://www.w3.org/2000/01/rdf-schema");
			rdfs.setNamespace("http://www.w3.org/2000/01/rdf-schema#");
			rdfs.setPrefix("rdfs");
			vocabularies.add(rdfs);
			Vocabulary owl = new Vocabulary();
			owl.setUri("http://www.w3.org/2002/07/owl");
			owl.setNamespace("http://www.w3.org/2002/07/owl#");
			owl.setPrefix("owl");
			vocabularies.add(owl);
			Vocabulary xsd = new Vocabulary();
			xsd.setUri("http://www.w3.org/2001/XMLSchema");
			xsd.setNamespace("http://www.w3.org/2001/XMLSchema#");
			xsd.setPrefix("xsd");
			vocabularies.add(xsd);

			return vocabularies;
		} catch (Exception e) {
			throw new LOVException(e.getMessage());
		}
	}

	private TaxoNode fetchVocabularySpaces(RepositoryConnection con)
			throws LOVException {
		try {
			String query = "SELECT ?vocabSpaceN1 ?vocabSpaceN1Title ?vocabSpaceN2 ?vocabSpaceN2Title "
					+ "?vocabSpaceN3 ?vocabSpaceN3Title "
					+ "WHERE{ "
					+ "<"
					+ LovConstants.LOV_FULL_VOCABULARYSPACE
					+ "> dcterms:hasPart ?vocabSpaceN1.  "
					+ "?vocabSpaceN1 a voaf:VocabularySpace. "
					+ "?vocabSpaceN1 bibo:shortTitle ?vocabSpaceN1Title. "
					+ "OPTIONAL{?vocabSpaceN1 dcterms:hasPart ?vocabSpaceN2.  "
					+ "?vocabSpaceN2 a voaf:VocabularySpace. "
					+ "?vocabSpaceN2 bibo:shortTitle ?vocabSpaceN2Title. "
					+ "OPTIONAL{?vocabSpaceN2 dcterms:hasPart ?vocabSpaceN3.  "
					+ "?vocabSpaceN3 a voaf:VocabularySpace. "
					+ "?vocabSpaceN3 bibo:shortTitle ?vocabSpaceN3Title.}} "
					+ "}ORDER BY ?vocabSpaceN1Title";
			TupleQueryResult result = con.prepareTupleQuery(
					QueryLanguage.SPARQL, LovConstants.PREFIXES + query)
					.evaluate();
			TaxoNode root = new TaxoNode();
			root.setUri(LovConstants.LOV_FULL_VOCABULARYSPACE);
			root.setLabel("All");
			while (result.hasNext()) {
				BindingSet bindingSet = result.next();
				String vocabSpaceN1 = bindingSet.getBinding("vocabSpaceN1")
						.getValue().toString();
				String vocabSpaceN1Title = ((Literal) bindingSet.getBinding(
						"vocabSpaceN1Title").getValue()).stringValue()
						.toString();
				String vocabSpaceN2 = null;
				if (bindingSet.getBinding("vocabSpaceN2") != null)
					vocabSpaceN2 = bindingSet.getBinding("vocabSpaceN2")
							.getValue().toString();
				String vocabSpaceN2Title = null;
				if (bindingSet.getBinding("vocabSpaceN2Title") != null)
					vocabSpaceN2Title = ((Literal) bindingSet.getBinding(
							"vocabSpaceN2Title").getValue()).stringValue()
							.toString();
				String vocabSpaceN3 = null;
				if (bindingSet.getBinding("vocabSpaceN3") != null)
					vocabSpaceN3 = bindingSet.getBinding("vocabSpaceN3")
							.getValue().toString();
				String vocabSpaceN3Title = null;
				if (bindingSet.getBinding("vocabSpaceN3Title") != null)
					vocabSpaceN3Title = ((Literal) bindingSet.getBinding(
							"vocabSpaceN3Title").getValue()).stringValue()
							.toString();

				if (vocabSpaceN1 != null
						&& getTaxoNodePresent(vocabSpaceN1, root) == null) {
					TaxoNode tn = new TaxoNode();
					tn.setUri(vocabSpaceN1);
					tn.setLabel(vocabSpaceN1Title);
					root.getChildren().add(tn);
				}
				if (vocabSpaceN2 != null
						&& getTaxoNodePresent(vocabSpaceN2, root) == null) {
					TaxoNode parent = getTaxoNodePresent(vocabSpaceN1, root);
					TaxoNode tn = new TaxoNode();
					tn.setUri(vocabSpaceN2);
					tn.setLabel(vocabSpaceN2Title);
					parent.getChildren().add(tn);
				}
				if (vocabSpaceN3 != null
						&& getTaxoNodePresent(vocabSpaceN3, root) == null) {
					TaxoNode parent = getTaxoNodePresent(vocabSpaceN1, root);
					TaxoNode tn = new TaxoNode();
					tn.setUri(vocabSpaceN3);
					tn.setLabel(vocabSpaceN3Title);
					parent.getChildren().add(tn);
				}
			}
			return root;
		} catch (Exception e) {
			throw new LOVException(e.getMessage());
		}
	}

	public static TaxoNode getTaxoNodePresent(String uri, TaxoNode root) {
		if (root.getUri().equals(uri))
			return root;
		for (int i = 0; i < root.getChildren().size(); i++) {
			TaxoNode tn = root.getChildren().get(i);
			if (getTaxoNodePresent(uri, tn) != null)
				return getTaxoNodePresent(uri, tn);
		}
		return null;
	}

	private List<String[]> getStringsHierarchy(String uri, TaxoNode root) {
		if (root.getUri().equals(uri)) {
			// System.out.println(uri);
			List<String[]> list = new ArrayList<String[]>();
			String[] s = { root.getUri(), root.getLabel() };
			list.add(s);
			return list;
		}
		List<String[]> list = new ArrayList<String[]>();
		for (int i = 0; i < root.getChildren().size(); i++) {
			TaxoNode tn = root.getChildren().get(i);
			List<String[]> l = getStringsHierarchy(uri, tn);
			if (l != null)
				list.addAll(l);
		}
		if (list.size() > 0) {
			String[] s = { root.getUri(), root.getLabel() };
			// System.out.println(root.getUri());
			list.add(s);
			return list;
		}
		return Collections.emptyList();
	}

	public static boolean incrementTaxoNode(String uri, TaxoNode root) {
		if (root.getUri().equals(uri)) {
			root.setCount(root.getCount() + 1);
			return true;
		}
		boolean isFound = false;
		for (int i = 0; i < root.getChildren().size(); i++) {
			TaxoNode tn = root.getChildren().get(i);
			if (incrementTaxoNode(uri, tn) == true) {
				isFound = true;
			}
		}
		if (isFound)
			root.setCount(root.getCount() + 1);
		return isFound;
	}

	public static void addVocabularyIfNotPresentInTaxoNode(String uri,
			String prefix, TaxoNode root) {
		boolean isFound = false;
		for (TaxoNode child : root.getChildren()) {
			if (child.getUri().equals(uri))
				isFound = true;
		}
		if (!isFound) {
			TaxoNode newVocabNode = new TaxoNode(uri, prefix);
			root.add(newVocabNode);
		}
	}

	public static TaxoNode orderVocabulariesInTaxoNode(TaxoNode root) {
		TaxoNode rootOut = new TaxoNode(root.getUri(), root.getLabel());
		rootOut.setCount(root.getCount());
		while (root.getChildren().size() > 0) {
			int index = 0;
			int maxNb = 0;
			for (int i = root.getChildren().size() - 1; i > -1; i--) {
				TaxoNode n = root.getChildren().get(i);
				if (n.getCount() > maxNb) {
					index = i;
					maxNb = n.getCount();
				}
			}
			TaxoNode newNode = new TaxoNode(root.getChildren().get(index));
			newNode.setCount(root.getChildren().get(index).getCount());
			rootOut.add(newNode);
			root.getChildren().remove(index);
		}
		return rootOut;
	}
}

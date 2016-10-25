package org.datalift.lov.local;

import java.util.ArrayList;
import java.util.List;

import org.datalift.lov.local.objects.ResultItem;
import org.datalift.lov.local.objects.SearchResult;
import org.datalift.lov.local.objects.TaxoNode;

public class ResultsHandler {
	private static ResultsHandler instance = null;
	private SearchResult result_All = null;
	private SearchResult result_Filtered = null;
	// private String lastFilterType=null;
	// private String lastFilterDomain=null;O
	private TaxoNode vocabularySpaceRoot = null;
	private TaxoNode typesList = null;

	public static ResultsHandler getInstance() {
		if (null == instance) { // Premier appel
			instance = new ResultsHandler();
		}
		return instance;
	}

	public SearchResult getResultOffset(int offset) {

		SearchResult result = new SearchResult();
		result.setOffset(offset);
		result.setSearch_vocSpace(result_Filtered.getSearch_vocSpace());
		result.setSearch_type(result_Filtered.getSearch_type());
		result.setSearch_voc(result_Filtered.getSearch_voc());
		int limit = result_Filtered.getLimit();
		result.setLimit(result_Filtered.getLimit());
		result.setCount(result_Filtered.getCount());
		result.setSearch_query(result_Filtered.getSearch_query());
		result.setFacet_types(result_Filtered.getFacet_types());
		result.setFacet_vocs(result_Filtered.getFacet_vocs());
		result.setFacet_vocSpaces(result_Filtered.getFacet_vocSpaces());
		result.setParams(result_Filtered.getParams());

		List<ResultItem> resultsItem = new ArrayList<ResultItem>();
		// limit results
		if (offset > -1 && limit > 0
				&& offset < result_Filtered.getResults().size()) {
			for (int i = offset; i < offset + limit
					&& i < result_Filtered.getResults().size(); i++) {
				resultsItem.add(result_Filtered.getResults().get(i));
			}
		}
		result.setResults(resultsItem);

		return result;
	}

	public SearchResult getResult(int offset, String filterType,
			String filterDomain, String filterVocabulary) {

		result_Filtered = new SearchResult();
		result_Filtered.setOffset(offset);
		if (filterDomain != null) {
			result_Filtered.setSearch_vocSpace(LovLocalService
					.getTaxoNodePresent(filterDomain,
							result_All.getFacet_vocSpaces()).getLabel());
		}
		if (filterType != null) {
			result_Filtered.setSearch_type(LovLocalService.getTaxoNodePresent(
					filterType, result_All.getFacet_types()).getLabel());
		}
		if (filterVocabulary != null) {
			result_Filtered.setSearch_voc(LovLocalService.getTaxoNodePresent(
					filterVocabulary, result_All.getFacet_vocs()).getLabel());
		}
		result_Filtered.setLimit(result_All.getLimit());
		result_Filtered.setCount(result_All.getCount());
		result_Filtered.setOffset(offset);
		result_Filtered.setSearch_query(result_All.getSearch_query());
		result_Filtered.setParams(result_All.getParams());

		List<ResultItem> resultsAllItem = result_All.getResults();
		List<ResultItem> resultsFilteredItem = new ArrayList<ResultItem>();

		// si les filtres ont changé alors on refiltre

		// TODO ne marche pas pour les "others"
		TaxoNode vocabSpaceList = new TaxoNode(vocabularySpaceRoot);
		TaxoNode typeList = new TaxoNode(typesList);
		TaxoNode vocabList = new TaxoNode("AllVocabularies", "All Vocabularies");

		if (filterType != null || filterDomain != null
				|| filterVocabulary != null) {
			for (int i = 0; i < resultsAllItem.size(); i++) {
				ResultItem res = resultsAllItem.get(i);
				int match = 0;
				if (filterType != null) {
					if (res.containsType(filterType))
						match++;
				} else
					match++;
				if (filterDomain != null) {
					if (res.hasVocSpaceURI(filterDomain))
						match++;
				} else
					match++;
				if (filterVocabulary != null) {
					if (res.getVocabulary() != null
							&& res.getVocabulary().equals(filterVocabulary))
						match++;
				} else
					match++;
				if (match == 3) {
					resultsFilteredItem.add(res);
					if (res.getVocSpaces().size() > 0) {
						LovLocalService.incrementTaxoNode(res.getVocSpaces()
								.get(0).getUri(), vocabSpaceList);
					}
					if (res.getTypes().size() > 0) {
						if (!LovLocalService.incrementTaxoNode(res.getTypes()
								.get(0).getUri(), typeList)) {
							LovLocalService
									.incrementTaxoNode("other", typeList);
						}
					}
					if (res.getVocabulary() != null) {
						LovLocalService.addVocabularyIfNotPresentInTaxoNode(
								res.getVocabulary(), res.getVocabularyPrefix(),
								vocabList);
						LovLocalService.incrementTaxoNode(res.getVocabulary(),
								vocabList);
					}
				}
			}
		} else {
			resultsFilteredItem = resultsAllItem;
		}
		// modifier les taxos domains et types selon les filtres
		if (filterDomain == null && filterType == null
				&& filterVocabulary == null) {
			result_Filtered.setFacet_vocSpaces(result_All.getFacet_vocSpaces());
			result_Filtered.setFacet_types(result_All.getFacet_types());
			result_Filtered.setFacet_vocs(result_All.getFacet_vocs());
		} else {
			result_Filtered.setFacet_vocSpaces(vocabSpaceList);
			result_Filtered.setFacet_types(typeList);
			result_Filtered.setFacet_vocs(LovLocalService
					.orderVocabulariesInTaxoNode(vocabList));
		}
		result_Filtered.setResults(resultsFilteredItem);
		result_Filtered.setCount(resultsFilteredItem.size());

		return getResultOffset(offset);
	}

	/*
	 * Accessors
	 */
	public SearchResult getResult_Filtered() {
		return result_Filtered;
	}

	public void setResult_Filtered(SearchResult result_Filtered) {
		this.result_Filtered = result_Filtered;
	}

	// la constitution de la liste de vocabulaire se fait lors de la restriction
	// du nombre de r�sultats
	public void setResult_All(SearchResult result_All,
			TaxoNode vocabularySpaceRoot, TaxoNode typesList) {
		this.result_All = result_All;
		this.result_Filtered = null;
		// lastFilterType=result_All.getFilterType();
		// lastFilterDomain=result_All.getFilterDomain();
		this.vocabularySpaceRoot = vocabularySpaceRoot;
		this.typesList = typesList;
	}
}

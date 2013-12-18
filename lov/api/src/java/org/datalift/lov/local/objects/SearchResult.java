package org.datalift.lov.local.objects;

import java.util.List;

import org.datalift.lov.local.LovUtil;

public class SearchResult {
	private int count = 0;
	private int offset = 0;
	private int limit = 15;
	private String search_query = null;
	private String search_type = null;
	private String search_vocSpace = null;
	private String search_voc = null;

	private TaxoNode facet_vocSpaces = null;
	private TaxoNode facet_types = null;
	private TaxoNode facet_vocs = null;

	private SearchParams params = null;

	private List<ResultItem> results = null;

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getOffset() {
		return offset;
	}

	public void setOffset(int offset) {
		this.offset = offset;
	}

	public int getLimit() {
		return limit;
	}

	public void setLimit(int limit) {
		this.limit = limit;
	}

	public String getSearch_query() {
		return search_query;
	}

	public void setSearch_query(String search_query) {
		this.search_query = search_query;
	}

	public String getSearch_type() {
		return search_type;
	}

	public void setSearch_type(String search_type) {
		this.search_type = search_type;
	}

	public String getSearch_vocSpace() {
		return search_vocSpace;
	}

	public void setSearch_vocSpace(String search_vocSpace) {
		this.search_vocSpace = search_vocSpace;
	}

	public String getSearch_voc() {
		return search_voc;
	}

	public void setSearch_voc(String search_voc) {
		this.search_voc = search_voc;
	}

	public TaxoNode getFacet_vocSpaces() {
		return facet_vocSpaces;
	}

	public void setFacet_vocSpaces(TaxoNode facet_vocSpaces) {
		this.facet_vocSpaces = facet_vocSpaces;
	}

	public TaxoNode getFacet_types() {
		return facet_types;
	}

	public void setFacet_types(TaxoNode facet_types) {
		this.facet_types = facet_types;
	}

	public TaxoNode getFacet_vocs() {
		return facet_vocs;
	}

	public void setFacet_vocs(TaxoNode facet_vocs) {
		this.facet_vocs = facet_vocs;
	}

	public SearchParams getParams() {
		return params;
	}

	public void setParams(SearchParams params) {
		this.params = params;
	}

	public List<ResultItem> getResults() {
		return results;
	}

	public void setResults(List<ResultItem> results) {
		this.results = results;
	}
	
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		// int params
		jsonResult.append("\"count\": " + count + ",");
		jsonResult.append("\"offset\": " + offset + ",");
		jsonResult.append("\"limit\": " + limit + ",");
		
		// string params
		jsonResult.append("\"search_query\": " + LovUtil.toJSON(search_query) + ",");
		jsonResult.append("\"search_type\": " + LovUtil.toJSON(search_type)  + ",");
		jsonResult.append("\"search_vocSpace\": " + LovUtil.toJSON(search_vocSpace) + ",");
		jsonResult.append("\"search_voc\": " + LovUtil.toJSON(search_voc) + ",");
		
		// facets
		jsonResult.append("\"facet_vocSpaces\": " + LovUtil.toJSON(facet_vocSpaces) + ",");
		jsonResult.append("\"facet_types\": " + LovUtil.toJSON(facet_types) + ",");
		jsonResult.append("\"facet_vocs\": " + LovUtil.toJSON(facet_vocs) + ",");
		
		// params
		jsonResult.append("\"params\": " + LovUtil.toJSON(params) + ",");
		
		// results
		jsonResult.append("\"results\": " + LovUtil.toJSON(results, true));
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}

}

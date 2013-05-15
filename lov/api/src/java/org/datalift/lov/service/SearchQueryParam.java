package org.datalift.lov.service;


/**
 * Parameters for the search service.
 * @author freddy
 *
 */
public class SearchQueryParam extends LovQueryParam {
	
	// Remplacer par une enum ?
	private final static String QUERY = "q";
	private final static String TYPE = "type";
	private final static String VOC_SPACE = "vocSpace";
	private final static String VOC = "voc";
	private final static String OFFSET = "offset";
	private final static String LIMIT = "limit";

	/** Full text query */
	private String query;
	
	/** Filter query results on their type. e.g. "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property" */
	private String type;
	
	/** Filter query results on a Vocabulary Space an element/vocabulary belongs to. e.g. "http://lov.okfn.org/dataset/lov/lov#WORLD" */
	private String vocSpace;
	
	/** Filter query results on a Vocabulary an element belongs to. e.g. "http://www.w3.org/2003/01/geo/wgs84_pos" */
	private String voc;
	
	/** Offset this number of rows */
	private int offset;
	
	/** Maximum number of rows to return (default: 15) */
	private int limit;
	
	
	public SearchQueryParam() {
		
	}
	
	public SearchQueryParam(String query) {
		this.query = query;
	}
	
	
	@Override
	public String getQueryParameters() {
		StringBuilder sb = new StringBuilder();
		
		sb.append(PARAM_TOKEN);
		sb.append(checkParameter(QUERY, query));
		sb.append(checkParameter(TYPE, type));
		sb.append(checkParameter(VOC_SPACE, vocSpace));
		sb.append(checkParameter(VOC, voc));
		sb.append(checkParameter(OFFSET, offset));
		sb.append(checkParameter(LIMIT, limit));
		
		return sb.substring(0, sb.length() - 1);
	}
	

	public String getQuery() {
		return query;
	}

	public void setQuery(String query) {
		this.query = query;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getVocSpace() {
		return vocSpace;
	}

	public void setVocSpace(String vocSpace) {
		this.vocSpace = vocSpace;
	}

	public String getVoc() {
		return voc;
	}

	public void setVoc(String voc) {
		this.voc = voc;
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
	
}

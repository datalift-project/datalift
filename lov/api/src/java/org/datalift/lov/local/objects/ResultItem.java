package org.datalift.lov.local.objects;

import java.util.ArrayList;
import java.util.List;

import org.datalift.lov.local.LovUtil;

public class ResultItem implements JSONSerializable {
	private String uri = null;
	private String uriPrefixed = null;
	private String vocabulary = null;
	private String vocabularyLOVLink = null;
	private String vocabularyPrefix = null;

	private List<ResultItemType> types = new ArrayList<ResultItemType>(3);
	private List<ResultItemVocSpace> vocSpaces = new ArrayList<ResultItemVocSpace>(
			3);
	private List<ResultItemMatch> matches = new ArrayList<ResultItemMatch>(5);
	// private List<String> types= new ArrayList<String>(3);
	// private List<String> typesPrefixed= new ArrayList<String>(3);
	// private List<String> properties= new ArrayList<String>(5);
	// private List<String> propertiesPrefixed= new ArrayList<String>(5);

	// private List<String> vocabularySpacesTitle= new ArrayList<String>(3);
	// private List<String> vocabularySpacesLOVLink= new ArrayList<String>(3);
	// private List<String> vocabularySpaces= new ArrayList<String>(3);
	// private List<String> values= new ArrayList<String>(5);
	// private List<String> valuesSmall= new ArrayList<String>(5);

	// metrics
	private float score = 0;
	private int score_nbMainLabel = 0;// nb de libellés principaux rdfs:label;
										// dc:title; dce:title; skos:prefLabel
	private int score_nbSecLabel = 0;// nb de descriptions/libs secondaires
										// skos:altLabel; rdfs:comment;
										// dc:description; dce:description
	private boolean isURIContainsSearchWords = false;// présence du searchWords
														// dans l'URI ? ->
														// discutable car URI
														// insignifiante
	private float bestRatioSearchWordsInLabels = 0;// best ratio
													// searchWords.length*nbOccurence
													// / value.length des
													// propriétés
	private int lovNbOcc = 0;// nombre d'occurence de cet élément dans le LOV
	private int lovNbVoc = 0;// nombre de vocabulaires dans le LOV qui font
								// référence à cet élément
	private int lodNbOcc = 0;// nombre d'occurence de cet élément dans le LOD

	public ResultItem() {
	}

	public ResultItem(String uri) {
		this.uri = uri;
	}

	public void computeScore(SearchParams params) {
		float fl_score_nbMainLabel = 0;
		if (params.getMaxScore_nbMainLabel() > 0)
			fl_score_nbMainLabel = Float.parseFloat(score_nbMainLabel + "")
					/ Float.parseFloat(params.getMaxScore_nbMainLabel() + "");
		float fl_score_nbSecLabel = 0;
		if (params.getMaxScore_nbSecLabel() > 0)
			fl_score_nbSecLabel = Float.parseFloat(score_nbSecLabel + "")
					/ Float.parseFloat(params.getMaxScore_nbSecLabel() + "");
		float fl_score_lovNbOcc = 0;
		if (params.getMaxLovNbOcc() > 0)
			fl_score_lovNbOcc = Float.parseFloat(lovNbOcc + "")
					/ Float.parseFloat(params.getMaxLovNbOcc() + "");
		float fl_score_lovNbVoc = 0;
		if (params.getMaxLovNbVoc() > 0)
			fl_score_lovNbVoc = Float.parseFloat(lovNbVoc + "")
					/ Float.parseFloat(params.getMaxLovNbVoc() + "");
		float fl_score_lodNbOcc = 0;
		if (params.getMaxLodNbOcc() > 0)
			fl_score_lodNbOcc = Float.parseFloat(lodNbOcc + "")
					/ Float.parseFloat(params.getMaxLodNbOcc() + "");

		// calcul de la ponderation
		float totalWeight = params.getWeightRatioSearchWordsInLabels()
				+ params.getWeightScoreNbMainLabel()
				+ params.getWeightScoreNbSecLabel()
				+ params.getWeightLovNbOcc() + params.getWeightLovNbVoc()
				+ params.getWeightLodNbOcc();

		if (totalWeight > 0) {
			this.score = (bestRatioSearchWordsInLabels
					* params.getWeightRatioSearchWordsInLabels()
					+ fl_score_nbMainLabel * params.getWeightScoreNbMainLabel()
					+ fl_score_nbSecLabel * params.getWeightScoreNbSecLabel()
					+ fl_score_lovNbOcc * params.getWeightLovNbOcc()
					+ fl_score_lovNbVoc * params.getWeightLovNbVoc() + fl_score_lodNbOcc
					* params.getWeightLodNbOcc())
					/ totalWeight;
		}
	}

	public boolean hasPropertyValue(String property, String value) {
		for (int j = 0; j < matches.size(); j++) {
			if (matches.get(j).getProperty().equals(property)
					&& matches.get(j).getValue().equals(value))
				return true;
		}
		return false;
	}

	public boolean hasType(String type) {
		for (int j = 0; j < types.size(); j++) {
			if (types.get(j).getUri().equals(type))
				return true;
		}
		return false;
	}

	public boolean hasVocSpaceURI(String vocSpaceURI) {
		for (int j = 0; j < vocSpaces.size(); j++) {
			if (vocSpaces.get(j).getUri().equals(vocSpaceURI))
				return true;
		}
		return false;
	}

	public String getVocabularyLOVLink() {
		return vocabularyLOVLink;
	}

	public void setVocabularyLOVLink(String vocabularyLOVLink) {
		this.vocabularyLOVLink = vocabularyLOVLink;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getUriPrefixed() {
		return uriPrefixed;
	}

	public void setUriPrefixed(String uriPrefixed) {
		this.uriPrefixed = uriPrefixed;
	}

	public String getVocabulary() {
		return vocabulary;
	}

	public void setVocabulary(String vocabulary) {
		this.vocabulary = vocabulary;
	}

	public String getVocabularyPrefix() {
		return vocabularyPrefix;
	}

	public void setVocabularyPrefix(String vocabularyPrefix) {
		this.vocabularyPrefix = vocabularyPrefix;
	}

	public float getScore() {
		return score;
	}

	public void setScore(float score) {
		this.score = score;
	}

	public int getScore_nbMainLabel() {
		return score_nbMainLabel;
	}

	public void incrementScore_nbMainLabel() {
		score_nbMainLabel++;
	}

	public void setScore_nbMainLabel(int score_nbMainLabel) {
		this.score_nbMainLabel = score_nbMainLabel;
	}

	public int getScore_nbSecLabel() {
		return score_nbSecLabel;
	}

	public void setScore_nbSecLabel(int score_nbSecLabel) {
		this.score_nbSecLabel = score_nbSecLabel;
	}

	public void incrementScore_nbSecLabel() {
		score_nbSecLabel++;
	}

	public boolean isURIContainsSearchWords() {
		return isURIContainsSearchWords;
	}

	public void setURIContainsSearchWords(boolean isURIContainsSearchWords) {
		this.isURIContainsSearchWords = isURIContainsSearchWords;
	}

	public float getBestRatioSearchWordsInLabels() {
		return bestRatioSearchWordsInLabels;
	}

	public void setBestRatioSearchWordsInLabels(
			float bestRatioSearchWordsInLabels) {
		this.bestRatioSearchWordsInLabels = bestRatioSearchWordsInLabels;
	}

	public int getLovNbOcc() {
		return lovNbOcc;
	}

	public void setLovNbOcc(int lovNbOcc) {
		this.lovNbOcc = lovNbOcc;
	}

	public int getLovNbVoc() {
		return lovNbVoc;
	}

	public void setLovNbVoc(int lovNbVoc) {
		this.lovNbVoc = lovNbVoc;
	}

	public int getLodNbOcc() {
		return lodNbOcc;
	}

	public void setLodNbOcc(int lodNbOcc) {
		this.lodNbOcc = lodNbOcc;
	}

	public List<ResultItemType> getTypes() {
		return types;
	}

	public boolean containsType(String typeURI) {
		for (int i = 0; i < types.size(); i++) {
			if (typeURI.equals(types.get(i).getUri()))
				return true;
		}
		return false;
	}

	public void setTypes(List<ResultItemType> types) {
		this.types = types;
	}

	public List<ResultItemVocSpace> getVocSpaces() {
		return vocSpaces;
	}

	public void setVocSpaces(List<ResultItemVocSpace> vocSpaces) {
		this.vocSpaces = vocSpaces;
	}

	public List<ResultItemMatch> getMatches() {
		return matches;
	}

	public void setMatches(List<ResultItemMatch> matches) {
		this.matches = matches;
	}

	@Override
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		// String properties
		jsonResult.append("\"uri\": " + LovUtil.toJSON(uri) + ",");
		jsonResult.append("\"uriPrefixed\": " + LovUtil.toJSON(uriPrefixed) + ",");
		jsonResult.append("\"vocabulary\": " + LovUtil.toJSON(vocabulary) + ",");
		jsonResult.append("\"vocabularyLOVLink\": " + LovUtil.toJSON(vocabularyLOVLink) + ",");
		jsonResult.append("\"vocabularyPrefix\": " + LovUtil.toJSON(vocabularyPrefix) + ",");
		
		// List properties
		// types
		jsonResult.append("\"types\": " + LovUtil.toJSON(types));
		
		// vocSpaces
		jsonResult.append("\"vocSpaces\": " + LovUtil.toJSON(vocSpaces));
		
		// matches
		jsonResult.append("\"matches\": " + LovUtil.toJSON(matches));
		
		// int, float, boolean properties
		jsonResult.append("\"score\": " + score + ",");
		jsonResult.append("\"score_nbMainLabel\": " + score_nbMainLabel + ",");
		jsonResult.append("\"score_nbSecLabel\": " + score_nbSecLabel + ",");
		jsonResult.append("\"bestRatioSearchWordsInLabels\": " + bestRatioSearchWordsInLabels + ",");
		jsonResult.append("\"lovNbOcc\": " + lovNbOcc + ",");
		jsonResult.append("\"lovNbVoc\": " + lovNbVoc + ",");
		jsonResult.append("\"lodNbOcc\": " + lodNbOcc + ",");
		jsonResult.append("\"uricontainsSearchWords\": " + String.valueOf(isURIContainsSearchWords));
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}
	
}

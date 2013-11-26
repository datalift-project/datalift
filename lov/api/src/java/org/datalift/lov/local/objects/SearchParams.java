package org.datalift.lov.local.objects;

public class SearchParams implements JSONSerializable {
	private int maxScore_nbMainLabel = 0;
	private int maxScore_nbSecLabel = 0;
	private int maxLovNbOcc = 0;
	private int maxLovNbVoc = 0;
	private int maxLodNbOcc = 0;

	private float weightScoreNbMainLabel = 0;
	private float weightScoreNbSecLabel = 0;
	private float weightRatioSearchWordsInLabels = 0;
	private float weightLovNbOcc = 0;
	private float weightLovNbVoc = 0;
	private float weightLodNbOcc = 0;

	public int getMaxScore_nbMainLabel() {
		return maxScore_nbMainLabel;
	}

	public void setMaxScore_nbMainLabel(int maxScore_nbMainLabel) {
		this.maxScore_nbMainLabel = maxScore_nbMainLabel;
	}

	public int getMaxScore_nbSecLabel() {
		return maxScore_nbSecLabel;
	}

	public void setMaxScore_nbSecLabel(int maxScore_nbSecLabel) {
		this.maxScore_nbSecLabel = maxScore_nbSecLabel;
	}

	public int getMaxLovNbOcc() {
		return maxLovNbOcc;
	}

	public void setMaxLovNbOcc(int maxLovNbOcc) {
		this.maxLovNbOcc = maxLovNbOcc;
	}

	public int getMaxLovNbVoc() {
		return maxLovNbVoc;
	}

	public void setMaxLovNbVoc(int maxLovNbVoc) {
		this.maxLovNbVoc = maxLovNbVoc;
	}

	public int getMaxLodNbOcc() {
		return maxLodNbOcc;
	}

	public void setMaxLodNbOcc(int maxLodNbOcc) {
		this.maxLodNbOcc = maxLodNbOcc;
	}

	public float getWeightScoreNbMainLabel() {
		return weightScoreNbMainLabel;
	}

	public void setWeightScoreNbMainLabel(float weightScoreNbMainLabel) {
		this.weightScoreNbMainLabel = weightScoreNbMainLabel;
	}

	public float getWeightScoreNbSecLabel() {
		return weightScoreNbSecLabel;
	}

	public void setWeightScoreNbSecLabel(float weightScoreNbSecLabel) {
		this.weightScoreNbSecLabel = weightScoreNbSecLabel;
	}

	public float getWeightRatioSearchWordsInLabels() {
		return weightRatioSearchWordsInLabels;
	}

	public void setWeightRatioSearchWordsInLabels(
			float weightRatioSearchWordsInLabels) {
		this.weightRatioSearchWordsInLabels = weightRatioSearchWordsInLabels;
	}

	public float getWeightLovNbOcc() {
		return weightLovNbOcc;
	}

	public void setWeightLovNbOcc(float weightLovNbOcc) {
		this.weightLovNbOcc = weightLovNbOcc;
	}

	public float getWeightLovNbVoc() {
		return weightLovNbVoc;
	}

	public void setWeightLovNbVoc(float weightLovNbVoc) {
		this.weightLovNbVoc = weightLovNbVoc;
	}

	public float getWeightLodNbOcc() {
		return weightLodNbOcc;
	}

	public void setWeightLodNbOcc(float weightLodNbOcc) {
		this.weightLodNbOcc = weightLodNbOcc;
	}
	
	@Override
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		jsonResult.append("\"maxScore_nbMainLabel\": " + maxScore_nbMainLabel + ",");
		jsonResult.append("\"maxScore_nbSecLabel\": " + maxScore_nbSecLabel + ",");
		jsonResult.append("\"maxLovNbOcc\": " + maxLovNbOcc + ",");
		jsonResult.append("\"maxLovNbVoc\": " + maxLovNbVoc + ",");
		jsonResult.append("\"maxLodNbOcc\": " + maxLodNbOcc + ",");
		jsonResult.append("\"weightScoreNbMainLabel\": " + weightScoreNbMainLabel + ",");
		jsonResult.append("\"weightScoreNbSecLabel\": " + weightScoreNbSecLabel + ",");
		jsonResult.append("\"weightRatioSearchWordsInLabels\": " + weightRatioSearchWordsInLabels + ",");
		jsonResult.append("\"weightLovNbOcc\": " + weightLovNbOcc + ",");
		jsonResult.append("\"weightLovNbVoc\": " + weightLovNbVoc + ",");
		jsonResult.append("\"weightLodNbOcc\": " + weightLodNbOcc);
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}

}

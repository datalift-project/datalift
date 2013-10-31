package org.silk.interlinker.script;

import javax.xml.bind.annotation.XmlRootElement;


/**
 * Keep every necessary information about the 2 datasource to interlink in order to create the script
 * to perform interlinking
 * @author carlo
 *
 */
@XmlRootElement
public class InterlinkedSourcesInfo {
	private String sourceUrl;
	private String sourceId;
	private String sourceQuery;
	private ComparisonParameters[] sourceComparisonParameters;
	private boolean isSourceLocal;
	
	private String targetUrl;
	private String targetId;
	private String targetQuery;
	private ComparisonParameters[] targetComparisonParameters;
	private boolean isTargetLocal;
	
	private ComparisonSettings[] comparisonSettings;
	private String aggregationSetting;
	private String newSourceUrl;
	
	public String getSourceId(){
		return sourceId;
	}
	
	public void setSourceId(String sourceId){
		this.sourceId = sourceId;
	}
	
	public String getSourceQuery() {
		return sourceQuery;
	}

	public void setSourceQuery(String sourceQuery) {
		this.sourceQuery = sourceQuery;
	}
	
	
	public String getSourceUrl() {
		return sourceUrl;
	}

	public void setSourceUrl(String sourceUrl) {
		this.sourceUrl = sourceUrl;
	}
	

	public ComparisonParameters[] getSourceComparisonParameters() {
		return sourceComparisonParameters;
	}

	public void setSourceComparisonParameters(
			ComparisonParameters[] sourceComparisonParameters) {
		this.sourceComparisonParameters = sourceComparisonParameters;
	}
	
	
	public ComparisonParameters[] getTargetComparisonParameters(){
		return targetComparisonParameters;
	}
	
	
	public String getTargetId(){
		return targetId;
	}
	

	public String getTargetUrl() {
		return targetUrl;
	}

	public void setTargetUrl(String targetUrl) {
		this.targetUrl = targetUrl;
	}

	public String getTargetQuery() {
		return targetQuery;
	}

	public void setTargetQuery(String targetQuery) {
		this.targetQuery = targetQuery;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public void setTargetComparisonParameters(
			ComparisonParameters[] targetComparisonParameters) {
		this.targetComparisonParameters = targetComparisonParameters;
	}
	
	public ComparisonSettings[] getComparisonSettings() {
		return comparisonSettings;
	}

	public void setComparisonSettings(ComparisonSettings[] comparisonSettings) {
		this.comparisonSettings = comparisonSettings;
	}

	public String getAggregationSetting() {
		return aggregationSetting;
	}

	public void setAggregationSetting(String aggregationSetting) {
		this.aggregationSetting = aggregationSetting;
	}
	
	public static class ComparisonParameters{
		private String property;
		private String transformation;
		private String[] stringParam;
		
		public String getProperty(){
			return this.property;
		}
		
		public void setProperty(String newProp){
			this.property = newProp;
		}

		public String getTransformation() {
			return transformation;
		}

		public void setTransformation(String transformation) {
			this.transformation = transformation;
		}

		public String[] getStringParam() {
			return stringParam;
		}

		public void setStringParam(String[] stringParam) {
			this.stringParam = stringParam;
		}
	}
	
	public static class ComparisonSettings{
		private String metric;
		private String threshold;
		private String weight;
		private String[] metricParams;
		
		public String getMetric() {
			return metric;
		}
		public void setMetric(String metric) {
			this.metric = metric;
		}
		public String getThreshold() {
			return threshold;
		}
		public void setThreshold(String thresold) {
			this.threshold = thresold;
		}
		public String getWeight() {
			return weight;
		}
		public void setWeight(String weight) {
			this.weight = weight;
		}
		public String[] getMetricParams() {
			return metricParams;
		}
		public void setMetricParams(String[] metricParams) {
			this.metricParams = metricParams;
		}
		
		
	}

	
	public boolean getIsSourceLocal() {
		return isSourceLocal;
	}

	public void setIsSourceLocal(boolean isSourceLocal) {
		this.isSourceLocal = isSourceLocal;
	}

	public boolean getIsTargetLocal() {
		return isTargetLocal;
	}

	public void setIsTargetLocal(boolean isTargetLocal) {
		this.isTargetLocal = isTargetLocal;
	}


	public String getNewSourceUrl() {
		return newSourceUrl;
	}

	public void setNewSourceUrl(String newSourceUrl) {
		this.newSourceUrl = newSourceUrl;
	}
	
	

}

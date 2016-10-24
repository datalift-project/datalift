package org.datalift.interlinker;

/**
 * @author Bouca Nova Dany
 *
 */
public class Form {
	String sourceEndpoint;
	String sourceId;
	String sourceGraph;
	String sourceVar;
	String sourcePagesize;
	String sourceRestriction;
	String sourceType;
	String []sourceProperties;
	
	String targetEndpoint;
	String targetId;
	String targetGraph;
	String targetVar;
	String targetPagesize;
	String targetRestriction;
	String targetType;
	String []targetProperties;

	String metric;

	String acceptanceFile;
	String acceptanceThreshold;
	String acceptanceRelation;

	String reviewFile;
	String reviewThreshold;
	String reviewRelation;
	
	String execution;
	String output;
	
	String []prefixProperties;

	public String getTargetType() {
		return targetType;
	}
	
	public String getSourceType() {
		return sourceType;
	}
	
	public String getExecution() {
		return execution;
	}

	public String getOutput() {
		return output;
	}

	public String getSourceEndpoint() {
		return sourceEndpoint;
	}

	public String getTargetEndpoint() {
		return targetEndpoint;
	}

	public String getAcceptanceFile() {
		return acceptanceFile;
	}

	public String getReviewFile() {
		return reviewFile;
	}

	public String getSourceId() {
		return sourceId;
	}

	public String getSourceGraph() {
		return sourceGraph;
	}

	public String getSourceVar() {
		return sourceVar;
	}

	public String getSourcePagesize() {
		return sourcePagesize;
	}

	public String getSourceRestriction() {
		return sourceRestriction;
	}

	public String[] getSourceProperties() {
		return sourceProperties;
	}

	public String getTargetId() {
		return targetId;
	}

	public String getTargetGraph() {
		return targetGraph;
	}

	public String getTargetVar() {
		return targetVar;
	}

	public String getTargetPagesize() {
		return targetPagesize;
	}

	public String getTargetRestriction() {
		return targetRestriction;
	}

	public String[] getTargetProperties() {
		return targetProperties;
	}

	public String getMetric() {
		return metric;
	}

	public String getAcceptanceThreshold() {
		return acceptanceThreshold;
	}

	public String getAcceptanceRelation() {
		return acceptanceRelation;
	}

	public String getReviewThreshold() {
		return reviewThreshold;
	}

	public String getReviewRelation() {
		return reviewRelation;
	}

	public String[] getPrefixProperties() {
		return prefixProperties;
	}

	@Override
	public String toString() {
		return "";
	}
}

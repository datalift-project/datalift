package org.datalift.lov.local.objects;

import org.datalift.lov.local.LovUtil;

public class ResultItemVocSpace implements JSONSerializable {

	private String uri = null;
	private String label = null;
	private String lovLink = null;

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLovLink() {
		return lovLink;
	}

	public void setLovLink(String lovLink) {
		this.lovLink = lovLink;
	}

	@Override
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		jsonResult.append("\"uri\": " + LovUtil.toJSON(uri) + ",");
		jsonResult.append("\"label\": " + LovUtil.toJSON(label) + ",");
		jsonResult.append("\"lovLink\": " + LovUtil.toJSON(lovLink));
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}
	
}

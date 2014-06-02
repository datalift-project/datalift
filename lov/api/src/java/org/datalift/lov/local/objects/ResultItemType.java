package org.datalift.lov.local.objects;

import org.datalift.lov.local.LovUtil;

public class ResultItemType implements JSONSerializable {

	private String uri = null;
	private String uriPrefixed = null;

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

	@Override
	public boolean equals(Object o) {
		if (o instanceof ResultItemType) {
			if (((ResultItemType) o).getUri().equals(this.getUri()))
				return true;
		}
		return false;
	}

	@Override
	public int hashCode() {
	    return (this.getUri() != null)? this.getUri().hashCode(): 0;
	}

	@Override
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		jsonResult.append("\"uri\": " + LovUtil.toJSON(uri) + ",");
		jsonResult.append("\"uriPrefixed\": " + LovUtil.toJSON(uriPrefixed));
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}
	
}

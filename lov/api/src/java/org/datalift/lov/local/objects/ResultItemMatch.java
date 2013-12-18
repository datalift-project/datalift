package org.datalift.lov.local.objects;

import org.datalift.lov.local.LovUtil;

public class ResultItemMatch implements JSONSerializable {

	private String property = null;
	private String propertyPrefixed = null;
	private String value = null;
	private String valueShort = null;

	public String getProperty() {
		return property;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	public String getPropertyPrefixed() {
		return propertyPrefixed;
	}

	public void setPropertyPrefixed(String propertyPrefixed) {
		this.propertyPrefixed = propertyPrefixed;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

	public String getValueShort() {
		return valueShort;
	}

	public void setValueShort(String valueShort) {
		this.valueShort = valueShort;
	}
	
	@Override
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		jsonResult.append("\"property\": " + LovUtil.toJSON(property) + ",");
		jsonResult.append("\"propertyPrefixed\": " + LovUtil.toJSON(propertyPrefixed) + ",");
		jsonResult.append("\"value\": " + LovUtil.toJSON(value) + ",");
		jsonResult.append("\"valueShort\": " + LovUtil.toJSON(valueShort));
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}
	
}

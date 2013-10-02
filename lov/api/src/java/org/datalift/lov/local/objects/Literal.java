package org.datalift.lov.local.objects;

public class Literal{

	private String value=null;
	private String language=null;
	private String dataType=null;
	
	public Literal(){}
	public Literal(String value, String language, String dataType){
		this.value=value;
		this.language=language;
		this.dataType=dataType;
	}
	
	
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getLanguage() {
		return language;
	}
	public void setLanguage(String language) {
		this.language = language;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}

}

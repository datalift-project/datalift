package org.datalift.owl.toolkit;

public class BindingDeclaration {

	protected String varName;
	protected String value;	
	
	public BindingDeclaration(String varName, String value) {
		super();
		this.varName = varName;
		this.value = value;
	}	
	
	public String getVarName() {
		return varName;
	}
	
	public void setVarName(String varName) {
		this.varName = varName;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}	
	
}

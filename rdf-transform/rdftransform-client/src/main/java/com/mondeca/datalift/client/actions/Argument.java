package com.mondeca.datalift.client.actions;

import com.mondeca.datalift.client.objects.Resource;

public class Argument {
	private String varName=null;
	private String displayName=null;
	private boolean mandatory=true;
	private int order =-1;
	private String argumentRange=null;
	private boolean isIn=true;
	private Resource resource = null;//may be changed to handle more complex elements
	
	public Argument(String varName,String displayName,boolean mandatory,int order,String argumentRange,boolean isIn){
		this.varName=varName;
		this.displayName=displayName;
		this.mandatory=mandatory;
		this.order=order;
		this.argumentRange=argumentRange;
		this.isIn=isIn;
	}
	
	public Argument(Argument arg){
		this.varName=arg.varName;
		this.displayName=arg.displayName;
		this.mandatory=arg.mandatory;
		this.order=arg.order;
		this.argumentRange=arg.argumentRange;
		this.isIn=arg.isIn;
		this.resource=arg.resource;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}
	public boolean isMandatory() {
		return mandatory;
	}
	public void setMandatory(boolean mandatory) {
		this.mandatory = mandatory;
	}
	public int getOrder() {
		return order;
	}
	public void setOrder(int order) {
		this.order = order;
	}
	public String getVarName() {
		return varName;
	}
	public void setVarName(String varName) {
		this.varName = varName;
	}
	public String getArgumentRange() {
		return argumentRange;
	}
	public void setArgumentRange(String argumentRange) {
		this.argumentRange = argumentRange;
	}
	public boolean isIn() {
		return isIn;
	}
	public void setIn(boolean isIn) {
		this.isIn = isIn;
	}
	public Resource getResource() {
		return resource;
	}
	public void setResource(Resource resource) {
		this.resource = resource;
	}
	
}

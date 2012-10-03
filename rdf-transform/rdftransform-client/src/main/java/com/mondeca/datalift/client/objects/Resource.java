package com.mondeca.datalift.client.objects;

public class Resource {
	public static int TYPE_CLASS=1;
	public static int TYPE_PROPERTY=2;
	
	private String URI=null;
	private String name=null;
	private String URIPrefixed=null;
	private int type=-1;
	
	public Resource(String URI, String name, int type){
		this.URI=URI;
		this.name=name;
		this.type=type;
	}
	
	public Resource(String URI, String name, String URIPrefixed){
		this.URI=URI;
		this.name=name;
		this.URIPrefixed=URIPrefixed;
	}
	
	
	public String getURI() {
		return URI;
	}
	public void setURI(String uRI) {
		URI = uRI;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public int getType() {
		return type;
	}
	public void setType(int type) {
		this.type = type;
	}
	public boolean isClass(){
		if(type==TYPE_CLASS)return true;
		else return false;
	}
	public boolean isProperty(){
		if(type==TYPE_CLASS)return false;
		else return true;
	}

	public String getURIPrefixed() {
		if(URIPrefixed==null)return URI;
		return URIPrefixed;
	}

	public void setURIPrefixed(String uRIPrefixed) {
		URIPrefixed = uRIPrefixed;
	}
	
}

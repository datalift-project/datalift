package com.mondeca.datalift.client.actions;

import java.util.ArrayList;
import java.util.List;

import com.mondeca.datalift.client.objects.Resource;

public class TransformationAction {
	
	public static String propURI= "http://www.w3.org/1999/02/22-rdf-syntax-ns#Property";
	public static String classURI= "http://www.w3.org/2000/01/rdf-schema#Class";
	
	private String name=null;
	private String displayName=null;
	private String imagePath=null;
	private String body=null;
	private List<Argument> hasArgument=new ArrayList<Argument>();
	
	public TransformationAction(String name,String displayName,String imagePath,String body){
		this.name=name;
		this.displayName=displayName;
		this.imagePath=imagePath;
		this.body=body;
	}
	
	public TransformationAction(TransformationAction action){
		this.name=action.name;
		this.displayName=action.displayName;
		this.imagePath=action.imagePath;
		this.body=action.body;
		for (Argument arg : action.getHasArgument()) {
			this.hasArgument.add(new Argument(arg));
		}
	}
	
	public void addResourceToFirstARgumentMatchable(Resource res){
		for (Argument arg : hasArgument) {
			if(res.getType()==Resource.TYPE_CLASS && arg.isIn() && arg.getArgumentRange().equals(classURI)){
				arg.setResource(res);
				return;
			}
			else if(res.getType()==Resource.TYPE_PROPERTY && arg.isIn() && arg.getArgumentRange().equals(propURI)){
				arg.setResource(res);
				return;
			}
		}
	}
	
	public boolean containsResource(Resource res){
		for (Argument arg : hasArgument) {
			if(arg.getResource()!=null && arg.getResource().getURI().equals(res.getURI()))return true;
		}
		return false;
	}
	
	

	public boolean isApplicableOnClass(){
		for (Argument arg : hasArgument) {
			if(arg.isIn() && arg.getArgumentRange().equals(classURI))return true;
		}
		return false;
	}
	public boolean isApplicableOnProperty(){
		for (Argument arg : hasArgument) {
			if(arg.isIn() && arg.getArgumentRange().equals(propURI))return true;
		}
		return false;
	}
	
	public String toText(){
		StringBuilder sb = new StringBuilder();
		sb.append(name+"(");
		boolean first=true;
		for (Argument arg : hasArgument) {
			if(arg.getResource()!=null){
				if(!first){
					sb.append(",");
				}
				else first=false;
				sb.append(arg.getVarName()+"=<"+arg.getResource().getURI()+">");
			}
			else return null;
		}
		sb.append(")");
		return sb.toString();
	}
	
	public boolean isValid(){
		for (Argument arg : hasArgument) {
			if(arg.getResource()==null){
				return false;
			}
		}
		return true;
	}




	public String getName() {
		return name;
	}




	public void setName(String name) {
		this.name = name;
	}




	public String getDisplayName() {
		return displayName;
	}




	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}




	public String getBody() {
		return body;
	}




	public void setBody(String body) {
		this.body = body;
	}




	public List<Argument> getHasArgument() {
		return hasArgument;
	}




	public void setHasArgument(List<Argument> hasArgument) {
		this.hasArgument = hasArgument;
	}




	public String getImagePath() {
		return imagePath;
	}




	public void setImagePath(String imagePath) {
		this.imagePath = imagePath;
	}
	
	
	
	
	
}

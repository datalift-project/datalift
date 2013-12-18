package org.silk.interlinker.script;

import java.net.URI;

import javax.xml.bind.annotation.XmlRootElement;


@XmlRootElement
public class SilkSource {
	private String newSourceName;
	private String targetContext;
	private String newSourceContext;
	private String scriptFilePath;
	private URI project;
	
	
	
	public URI getProject() {
		return project;
	}
	public void setProject(URI project) {
		this.project = project;
	}
	public String getScriptFilePath() {
		return scriptFilePath;
	}
	public void setScriptFilePath(String scriptFilePath) {
		this.scriptFilePath = scriptFilePath;
	}
	public String getTargetContext() {
		return targetContext;
	}
	public void setTargetContext(String targetContext) {
		this.targetContext = targetContext;
	}
	public String getNewSourceContext() {
		return newSourceContext;
	}
	public void setNewSourceContext(String newSourceContext) {
		this.newSourceContext = newSourceContext;
	}
	public String getNewSourceName() {
		return newSourceName;
	}
	public void setNewSourceName(String newSourceName) {
		this.newSourceName = newSourceName;
	}
	
	
	
}

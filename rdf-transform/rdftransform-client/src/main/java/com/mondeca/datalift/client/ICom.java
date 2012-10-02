package com.mondeca.datalift.client;

import java.util.List;

import com.mondeca.datalift.client.actions.TransformationAction;
import com.mondeca.datalift.client.objects.Resource;

public interface ICom {
	
	public void setGraphInformation(String sourceGraphURI, String sourceGraphName, String targetGraphURI, String targetGraphName);
	public void displayError(String error);
	public void displayActionsForResource(Resource actionSubjectResource);
	public void removeActionPanel();
	public List<TransformationAction> getAllActions();
	public String getServerBaseURL();
}

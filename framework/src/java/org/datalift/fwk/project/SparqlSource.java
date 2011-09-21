package org.datalift.fwk.project;

public interface SparqlSource extends RdfSource {

	public String getConnectionUrl();

	public void setConnectionUrl(String connectionUrl);

	public String getRequest();

	public void setRequest(String request);

}

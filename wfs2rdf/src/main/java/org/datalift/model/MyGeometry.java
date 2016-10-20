package org.datalift.model;

import javax.xml.namespace.QName;

public class MyGeometry {

	public String SRS;
	public String WKT;
	public QName type;
	public String gml_id;
	public boolean isEmpty;
	public int dimension;
	public MyGeometry()
	{
		isEmpty=true;
		dimension=2;
	}
}

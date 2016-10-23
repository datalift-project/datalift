package org.datalift.ows.model;

import javax.xml.namespace.QName;
/**
 * A simplified definition of a geometry type 
 * @author Hanane Eljabiri
 *
 */
public class DlGeometry {

	public String SRS;
	public String WKT;
	public QName type;
	public String gml_id;
	public boolean isEmpty;
	public int dimension;
	public DlGeometry()
	{
		isEmpty=true;
		dimension=2;
	}
}

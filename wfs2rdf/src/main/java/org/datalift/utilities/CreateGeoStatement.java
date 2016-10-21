/*
 * Copyright / Copr. IGN 2014
 * Contributor(s) : Faycal Hamdi
 *
 * Contact: hamdi.faycal@gmail.com
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.utilities;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.datalift.fwk.log.Logger;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;

import fr.ign.datalift.model.GeometryProperty;

public class CreateGeoStatement {

	private final static Logger log = Logger.getLogger();
	private ValueFactory vf;
	private Statement geoStatement;
	private List<Statement> aboutGeometry = new ArrayList<Statement>();
	public static Map <String,String> CRS = new HashMap<String, String>();
	static
	{
		CRS.put(null,"http://www.opengis.net/def/crs/EPSG/0/4326");
		InputStream is=null;
		try {
			Properties prop = new Properties();
			String propFileName = "CRS.properties";
			is = CreateGeoStatement.class.getClassLoader().getResourceAsStream(propFileName);
			if (is != null) {
				prop.load(is);
			} else {
				throw new FileNotFoundException("property file '" + propFileName + "' not found in the classpath");
			}
			Enumeration em = prop.keys();
			while (em.hasMoreElements()) {
				String codeEpsg = (String) em.nextElement();
				CRS.put(codeEpsg,prop.getProperty(codeEpsg));
			}
		} catch (Exception e) {
			log.error("An error has occured while attempting to  load the properties file of CRS " + e);
		} finally {
			if(is!=null)
				{
					try {
						is.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
		}
	}
	
	public void createStatement (GeometryProperty gp, ValueFactory vf, 
			URI feature, URI geomFeature, String geoType, String crs){
		this.vf = vf;
		geoStatement = vf.createStatement(feature, vf.createURI(Context.nsGeoSparql+"hasGeometry"), geomFeature);
		Statement geoStatementType= vf.createStatement(geomFeature, vf.createURI(Context.nsRDF2+"type"), vf.createURI(Context.nsGeoSparql+"Geometry"));
		aboutGeometry.add(geoStatement);
		aboutGeometry.add(geoStatementType);
		String crsURI=CRS.get(crs);
		if(crsURI==null)
		{
			crsURI=CRS.get(null); //get default crs: wgs84
		}
		geoStatement = vf.createStatement(geomFeature, GeoSPARQL.ASWKT, 
				vf.createLiteral("<"+crsURI + "> " + gp.getValue(),GeoSPARQL.WKTLITERAL));
		aboutGeometry.add(geoStatement);
	}

	public ValueFactory getVf() {
		return vf;
	}

	public void setVf(ValueFactory vf) {
		this.vf = vf;
	}

	public Statement getGeoStatement() {
		return geoStatement;
	}

	public void setGeoStatement(Statement geoStatement) {
		this.geoStatement = geoStatement;
	}

	public List<Statement> getAboutGeometry() {
		return aboutGeometry;
	}

	public void setAboutGeometry(List<Statement> aboutGeometry) {
		this.aboutGeometry = aboutGeometry;
	}
	protected int getCurrentIndexPoint(GeometryProperty gp, int currentIndexPoint){
		int indexPoint = 0;
		for (int i=0; i < currentIndexPoint; i++){
			indexPoint = indexPoint + gp.getNumPoint(i);
		}
		return indexPoint;
	}
}

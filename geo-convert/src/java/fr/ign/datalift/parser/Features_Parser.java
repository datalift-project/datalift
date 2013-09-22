/*
 * Copyright / Copr. IGN 2013
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

package fr.ign.datalift.parser;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.referencing.CRS;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.xml.sax.SAXException;

import fr.ign.datalift.model.AbstractFeature;
import fr.ign.datalift.model.FeatureProperty;
import fr.ign.datalift.model.GeometryProperty;

public class Features_Parser {

	private ArrayList<fr.ign.datalift.model.AbstractFeature> features;
	private FeatureCollection<SimpleFeatureType, SimpleFeature> fc;
	private fr.ign.datalift.model.AbstractFeature ft;
	public String crs = "EPSG:4326";
	public ArrayList<String> asGmlList = null;

	static Logger log = Logger.getLogger(Features_Parser.class.getName());

	private final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	public void parseGML(String gmlPath) throws IOException, SAXException, ParserConfigurationException {

		GmlParser gmlparser = new GmlParser(gmlPath);
		this.fc = gmlparser.featureCollection;
		this.crs = gmlparser.crs;
		this.asGmlList = gmlparser.asGmlList;

	}

	public void parseSHP(String shpPath, boolean wgs84) throws IOException, SAXException, ParserConfigurationException {

		// extract Features
		ShpParser shpfeatures = new ShpParser(shpPath, wgs84);
		this.fc = shpfeatures.featureSource.getFeatures();
		this.crs = CRS.toSRS(shpfeatures.featureSource.getSchema().getCoordinateReferenceSystem());

	}

	public ArrayList<AbstractFeature> readFeatureCollection() {

		features = new ArrayList<AbstractFeature>();

		for (FeatureIterator<SimpleFeature> j = fc.features(); j.hasNext();) {
			Feature f = j.next();
			ft = new AbstractFeature();
			if (f.getIdentifier() != null) {
				//ft.setName(new String (f.getIdentifier().toString()));
				ft.setName(new StringBuilder(new String(f.getIdentifier().toString().getBytes(),UTF8_CHARSET)).toString());
			}

			Collection<Property> propColl = f.getProperties();
			for (Iterator<Property> iterator = propColl.iterator(); iterator.hasNext();) {
				Property prop = iterator.next();
				if (!prop.getName().toString().equals("metaDataProperty")
						&& !prop.getName().toString().equals("description")
						&& !prop.getName().toString().equals("name")
						&& !prop.getName().toString().equals("boundedBy")
						&& !prop.getName().toString().equals("location")) {

					// checks if the property value is not null
					if (prop.getValue() != null) {

						// check if Property is GeometryProperty
						if (prop.getType() instanceof GeometryTypeImpl) {
							GeometryProperty gp = new GeometryProperty();
							gp.setName(new StringBuilder(new String(prop.getName().toString().getBytes(),UTF8_CHARSET)).toString());
							gp.setValue(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());
							gp.setType(new StringBuilder(new String(prop.getDescriptor().getType()
									.getBinding().getSimpleName().getBytes(),UTF8_CHARSET)).toString());
							ft.addProperty(gp);
						} else {
							FeatureProperty fp = new FeatureProperty();
							fp.setName(new StringBuilder(new String(prop.getName().toString().getBytes(),UTF8_CHARSET)).toString());
							if (prop.getType().toString().contains("Float") || (prop.getType().toString().contains("Double"))) {
								fp.setDoubleValue(Double.parseDouble(prop.getValue().toString()));
								fp.setType("double");
								ft.addProperty(fp);
							}
							if (prop.getType().toString().contains("Integer") || (prop.getType().toString().contains("Long"))) {
								fp.setIntValue(Integer.parseInt(prop.getValue().toString()));
								fp.setType("int");
								ft.addProperty(fp);
							}
							else  {
								fp.setValue(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());
								ft.addProperty(fp);
							}

							if(prop.getName().toString().equals("name")){
								ft.setLabel(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());

							} if(prop.getName().toString().contains("label")){
								ft.setLabel(new StringBuilder(new String(prop.getValue().toString().getBytes(),UTF8_CHARSET)).toString());
							}

						}
					}
				} 

			}

			features.add(ft);
		}

		return features;
	}

}

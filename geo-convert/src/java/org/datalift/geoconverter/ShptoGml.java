/*
 * Copyright / Copr. IGN
 * Contributor(s) : F. Hamdi
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

package org.datalift.geoconverter;

import java.io.File;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.ShpSource;
import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.Source.SourceType;
import static org.datalift.fwk.util.StringUtils.*;

import org.datalift.geoconverter.shp.Ogr2ogr;

/**
 * A {@link ProjectModule project module} that loads the SHP data
 * from a {@link ShpSource SHP file source} into the internal
 * RDF store.
 *
 * @author fhamdi
 */
@Path(ShptoGml.MODULE_NAME)
public class ShptoGml extends BaseConverterModule
{
	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------

	
	/** The prefix for the URI of the project objects. */
	public final static String PROJECT_URI_PREFIX = "project";
	/** The prefix for the URI of the source objects, within projects. */
	public final static String SOURCE_URI_PREFIX  = "source";
	/** The relative path prefix for project objects and resources. */
	private final static String REL_PROJECT_PATH = PROJECT_URI_PREFIX + '/';
	/** The relative path prefix for source objects, within projects. */
	private final static String SOURCE_PATH = "/" + SOURCE_URI_PREFIX  + '/';
	
	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "shptogml";

	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger();

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/** Default constructor. */
	public ShptoGml() {
		super(MODULE_NAME, 800, SourceType.ShpSource);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------

	@GET
	public Response getIndexPage(@QueryParam("project") URI projectId) {
		// Retrieve project.
		Project p = this.getProject(projectId);
		// Display conversion configuration page.
		Map<String, Object> args = new HashMap<String, Object>();
		args.put("it", p);
		args.put("converter", this);
		return Response.ok(this.newViewable("/shptoGml.vm", args))
				.build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response convertShpData(
			@FormParam("project") URI projectId,
			@FormParam("source") URI sourceId)
					throws WebApplicationException {
		Response response = null;
		try {
			// Retrieve project.
			Project p = this.getProject(projectId);
			// Retrieve source.
			ShpSource s = (ShpSource)(p.getSource(sourceId));
            if (s == null) {
                this.throwInvalidParamError("source", sourceId);
            }
            
            // Convert SHP data and load generated GML.
            File inShpFile = new File(Configuration.getDefault().getPublicStorage(),
                                      s.getFilePath());
            File path = inShpFile.getParentFile();
            String rootShp = inShpFile.getName();
            Ogr2ogr shpconvert = new Ogr2ogr();

            // Convert SHP to WGS84
            // copy (libgdal.so.1, libgdaljni.so, libogrjni.so, libosrjni.so and libproj.so) to /usr/lib/
            // export GDAL_DATA=/usr/local/share/gdal/ (this folder must contains ellipsoid.csv and gcs.csv)
            String fileOutShp = rootShp.substring(0, rootShp.lastIndexOf('.')) + "_wgs84.shp";
            File outShpFile = new File(path, fileOutShp);
            if (outShpFile.exists()) {
                outShpFile.delete();
            }
            log.debug("Generating WGS84 projection: {}", outShpFile);
            String[] agrum1 = {"-t_srs", "EPSG:4326", outShpFile.getCanonicalPath(),
                                                      inShpFile.getCanonicalPath() };
            shpconvert.convert(agrum1);
  
            // Convert SHP to GML
            String fileOutGml = fileOutShp.substring(0, fileOutShp.lastIndexOf('.')) + ".gml";
            // Warning: OGR2OGR GML mapping substitutes all '_' with '_' in layer names
            //          for "XML validity" (?). Adjusting GML file name to reflect this.
            fileOutGml = fileOutGml.replace('-', '_');
            File outGmlFile = new File(path, fileOutGml);
            log.debug("Generating GML representation from WGS84 projection: {}", outGmlFile);
            String[] agrum2 = {"-f", "GML", outGmlFile.getCanonicalPath(),
                                            outShpFile.getCanonicalPath() };
            shpconvert.convert(agrum2);

			// Register new transformed source.
			URI sourceUriGml = new URI(projectId.getScheme(), null,
					projectId.getHost(), projectId.getPort(),
					this.getSourceId(projectId.getPath(), fileOutGml),
					null, null);
			String filePathOutGml = this.getProjectFilePath(p.getTitle(), fileOutGml);
			GmlSource srcGml = this.newGmlSource(p, sourceUriGml, fileOutGml, "", filePathOutGml);

			response = this.created(srcGml).build();
		}
		catch (Exception e) {
			this.handleInternalError(e);
		}
		return response;
	}

	private String getProjectFilePath(String projectId, String fileName) {
		StringBuilder buf = new StringBuilder(80);
		buf.append(REL_PROJECT_PATH).append(urlify(projectId));
		if (isSet(fileName)) {
			buf.append('/').append(fileName);
		}
		return buf.toString();
	}

	private String getSourceId(String projectUri, String sourceName) {
		return projectUri + SOURCE_PATH + urlify(sourceName);
	}
}

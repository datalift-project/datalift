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
import java.util.Arrays;
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
import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.geoconverter.usgs.rdf.util.ConfigFinder;
import org.datalift.geoconverter.usgs.rdf.util.GMLConverter;


/**
 * A {@link ProjectModule project module} that loads the GML data
 * from a {@link GmlSource GML file source} into the internal
 * RDF store.
 *
 * @author fhamdi
 */
@Path(GmltoRdf.MODULE_NAME)
public class GmltoRdf extends BaseConverterModule
{
	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------
	
	/** The prefix for the URI of the project objects. */
	public final static String PROJECT_URI_PREFIX = "project";
	/** The prefix for the URI of the source objects, within projects. */
	public final static String SOURCE_URI_PREFIX  = "source";
	
	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "gmltordf";

	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger();

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/** Default constructor. */
	public GmltoRdf() {
		super(MODULE_NAME, 900, SourceType.GmlSource);
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
		return Response.ok(this.newViewable("/gmltoRdf.vm", args))
				.build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response convertGmlData(
			@FormParam("project") URI projectId,
			@FormParam("source") URI sourceId)
					throws WebApplicationException {
		Response response = null;
		try {
			// Retrieve project.
			Project p = this.getProject(projectId);
			// Retrieve source.
			GmlSource s = (GmlSource)(p.getSource(sourceId));
            if (s == null) {
                this.throwInvalidParamError("source", sourceId);
            }
//            StdOutErrLog.tieSystemOutAndErrToLog();

            // Convert GML data and load generated RDF.
            File localFile = new File(Configuration.getDefault().getPublicStorage(),
                                      s.getFilePath());
            File path = localFile.getParentFile();
//            String fileInGml = localFile.getAbsolutePath();
//            String rootGml = fileInGml.substring(0, fileInGml.indexOf("."));
//            String fileOutRdf = rootGml + ".rdf";
//            File rdfExist = new File(fileOutRdf);
            String fileName = localFile.getName();
            fileName = fileName.substring(0, fileName.lastIndexOf('.'));
            File rdfExist = new File(path, fileName + ".rdf");
            if (rdfExist.exists()) {
                log.debug("Deleting existing RDF file: {}", rdfExist);
                rdfExist.delete();
            }
			
            // Make sure the Geometry parser looks for its default
            // configuration files in Datalift configuration directory.
            String dataliftHome = Configuration.getDefault().getProperty(
                                                                "datalift.home");
            File cfgPath = new File(dataliftHome, "conf/geo");
            log.debug("Geometry parser default configuration path: {}", cfgPath);
            // GeometryParser.setDefaultConfigPath(cfgPath);
            ConfigFinder.setPaths(Arrays.asList(path, cfgPath));
            log.debug("Generating {} from {}", rdfExist, localFile);
            GMLConverter converter = new GMLConverter(path,
                                                path, true, true, true, true);
            converter.run();
            
            URI targetGraph = new URI(s.getUri() + "-rdf");
            RdfUtils.upload(rdfExist, Configuration.getDefault()
                                                   .getInternalRepository(),
                            targetGraph, null);
            // Register new transformed RDF source.
            Source out = this.addResultSource(p, s,
                                "RDF mapping of " + s.getTitle(), targetGraph);
            // Display project source tab, including the newly created source.
            response = this.created(out).build();

            log.info("RDF data successfully loaded into \"{}\"", targetGraph);

		}
		catch (Exception e) {
			this.handleInternalError(e);
		}
		return response;
	}

//public final static class StdOutErrLog {
//
//    public static void tieSystemOutAndErrToLog() {
//        System.setOut(createLoggingProxy(System.out));
//        System.setErr(createLoggingProxy(System.err));
//    }
//
//    public static PrintStream createLoggingProxy(final PrintStream realPrintStream) {
//        return new PrintStream(realPrintStream) {
//            public void print(final String string) {
//                realPrintStream.print(string);
//                log.info(string);
//            }
//        };
//    }
//}
}

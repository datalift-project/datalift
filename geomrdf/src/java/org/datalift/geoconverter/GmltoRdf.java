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

package org.datalift.geoconverter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.WordUtils;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.FileSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.GmlSource;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.ShpSource.Crs;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.UriBuilder;
import org.datalift.fwk.util.io.FileUtils;
import org.datalift.fwk.view.TemplateModel;

import static javax.ws.rs.core.MediaType.APPLICATION_XHTML_XML;
import static javax.ws.rs.core.MediaType.TEXT_HTML;
import static org.datalift.fwk.rdf.ElementType.RdfType;
import static org.datalift.fwk.util.StringUtils.*;

import org.openrdf.model.BNode;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import fr.ign.datalift.constants.GeoSPARQL;
import fr.ign.datalift.constants.Geometrie;
import fr.ign.datalift.model.AbstractFeature;
import fr.ign.datalift.model.FeatureProperty;
import fr.ign.datalift.model.GeometryProperty;
import fr.ign.datalift.parser.Features_Parser;

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
		super(MODULE_NAME, 800, SourceType.GmlSource);
	}

	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------

	@GET
	@Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
	public Response getIndexPage(@QueryParam("project") URI projectId) {
		// Retrieve project.
		Project p = this.getProject(projectId);
		// Display conversion configuration page.
		TemplateModel view = this.newView("gmltoRdf.vm", p);
		view.put("converter", this);
		view.put("crs", Crs.values());
		return Response.ok(view).build();
	}

	@POST
	@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
	public Response convertGmlData(
			@FormParam("project") URI projectId,
			@FormParam("source") URI sourceId,
			@FormParam("dest_title") String destTitle,
			@FormParam("dest_graph_uri") URI targetGraph,
			@FormParam("base_uri") URI baseUri,
			@FormParam("dest_type") String targetType,
			@FormParam("crs") String crs)
					throws WebApplicationException {

		Response response = null;
		try {
			// Retrieve project.
			Project p = this.getProject(projectId);
			// Retrieve source.
			GmlSource src = (GmlSource)(p.getSource(sourceId));
			if (src == null) {
				this.throwInvalidParamError("source", sourceId);
			}
			// Create working directory.
			File tmpDir = this.createWorkingDirectory(src);
			// Copy source GmlFile files to working directory.
			File inGmlFile = this.getLocalCopy(src.getGmlFilePath(),
					src.getGmlFileInputStream(), tmpDir);
			this.getLocalCopy(src.getXsdFilePath(),
					src.getXsdFileInputStream(), tmpDir);

			// Convert GML data and load generated RDF triples.
			Features_Parser parser = new Features_Parser();
			if (Crs.valueOf(crs).getValue().equals("EPSG:4326")) {
				parser.parseGML(inGmlFile.getCanonicalPath(), true, Crs.valueOf(crs).getValue());
			}
			else if (Crs.valueOf(crs).getValue().equals("none")) {
				parser.parseGML(inGmlFile.getCanonicalPath(), false, "");
			}
			this.convertToRDF(src, parser.readFeatureCollection(), parser.crs, 
					parser.asGmlList, Configuration.getDefault().getInternalRepository(),
					targetGraph, baseUri, targetType);

			// Register new transformed RDF source.
			Source out = this.addResultSource(p, src,
					"RDF mapping of " + src.getTitle(), targetGraph);

			// Display project source tab, including the newly created source.
			response = this.created(out).build();

		}
		catch (Exception e) {
			this.handleInternalError(e);
		}
		return response;
	}

	private File createWorkingDirectory(FileSource s) {
		String relPath = new File(s.getFilePath()).getParent();
		File tmpDir = new File(Configuration.getDefault().getTempStorage(),
				relPath);
		tmpDir.mkdirs();
		return tmpDir;
	}

	private File getLocalCopy(String path, InputStream in, File localDir)
			throws IOException {
		File f = new File(localDir, new File(path).getName());
		FileUtils.save(in, f);
		return f;
	}


	//-------------------------------------------------------------------------
	// Specific implementation
	//-------------------------------------------------------------------------

	public void convertToRDF(GmlSource src, ArrayList<AbstractFeature> featureList, 
			String crs, ArrayList<String> asGmlList, Repository target, URI targetGraph, 
			URI baseUri, String targetType) {

		final UriBuilder uriBuilder = Configuration.getDefault()
				.getBean(UriBuilder.class);
		final RepositoryConnection cnx = target.newConnection();
		org.openrdf.model.URI ctx = null;
		try {
			final ValueFactory vf =
					new UriCachingValueFactory(cnx.getValueFactory());

			// Clear target named graph, if any.
			if (targetGraph != null) {
				ctx = vf.createURI(targetGraph.toString());
				cnx.clear(ctx);
			}
			// Create URIs for subjects and predicates.
			if (baseUri == null) {
				baseUri = targetGraph;
			}
			String sbjUri  = RdfUtils.getBaseUri(
					(baseUri != null)? baseUri.toString(): null, '/');
			String typeUri = RdfUtils.getBaseUri(
					(baseUri != null)? baseUri.toString(): null, '#');
			// Create target RDF type.
			if (! isSet(targetType)) {
				targetType = uriBuilder.urlify(src.getTitle(), RdfType);
			}
			org.openrdf.model.URI rdfType = null;
			try {
				// Assume target type is an absolute URI.
				rdfType = vf.createURI(targetType);
			}
			catch (Exception e) {
				// Oops, targetType is a relative URI. => Append namespace URI.
				rdfType = vf.createURI(typeUri, targetType);
			}

			Statement statement;
			List<Statement> aboutAttributes = new ArrayList<Statement>();
			List<Statement> aboutGeometry = new ArrayList<Statement>();

			// serialize a featureCollection into RDF
			int count = 0;
			CreateGeoStatement cgs = new CreateGeoStatement();

			for (int i = 0; i < featureList.size(); i++) {
				count = i + 1;
				org.openrdf.model.URI feature = vf.createURI(sbjUri + count);

				statement = vf.createStatement(feature, RDF.TYPE, rdfType);
				aboutAttributes.add(statement);

				ArrayList<FeatureProperty> featureProperties = (ArrayList<FeatureProperty>) featureList.get(i).getProperties();

				for (int j = 0; j < featureProperties.size(); j++) {

					FeatureProperty fp = featureProperties.get(j);

					if (fp instanceof GeometryProperty) {

						GeometryProperty gp = (GeometryProperty)fp;
						String geoType = gp.getType();
						org.openrdf.model.URI geomFeature = vf.createURI(sbjUri, geoType + "_" + count);

						cgs.createStatement(gp, vf, feature, geomFeature, geoType, crs);
						aboutGeometry = cgs.aboutGeometry;

						if (asGmlList != null) {
							statement = vf.createStatement(geomFeature, GeoSPARQL.ASGML, vf.createLiteral(asGmlList.get(j)));
							aboutGeometry.add(statement);
						}


					} else {
						if (fp.getType() != null){
							if (fp.getType().contains("int")) {
								statement = vf.createStatement(feature, vf.createURI(typeUri + fp.getName()), vf.createLiteral(fp.getIntValue()));
								aboutAttributes.add(statement);
							} else {
								statement = vf.createStatement(feature, vf.createURI(typeUri + fp.getName()), vf.createLiteral(fp.getDoubleValue()));
								aboutAttributes.add(statement);
							}
						} else {
							statement = vf.createStatement(feature, vf.createURI(typeUri + fp.getName()), vf.createLiteral(fp.getValue()));
							aboutAttributes.add(statement);
						}
					}
				}
			}

			long startTime = System.currentTimeMillis();
			long duration = -1L;
			long statementCount = 0L;
			int  batchSize = Env.getRdfBatchSize();

			try {
				// Prevent transaction commit for each triple inserted.
				cnx.setAutoCommit(false);
			}
			catch (RepositoryException e) {
				throw new RuntimeException("RDF triple insertion failed", e);
			}

			for (Statement at:aboutAttributes){
				try {
					cnx.add(at, ctx);

					// Commit transaction according to the configured batch size.
					statementCount++;
					if ((statementCount % batchSize) == 0) {
						cnx.commit();
					}
				}
				catch (RepositoryException e) {
					throw new RuntimeException("RDF triple insertion failed", e);
				}
			}

			for (Statement gt:aboutGeometry){
				try {
					cnx.add(gt, ctx);

					// Commit transaction according to the configured batch size.
					statementCount++;
					if ((statementCount % batchSize) == 0) {
						cnx.commit();
					}
				}
				catch (RepositoryException e) {
					throw new RuntimeException("RDF triple insertion failed", e);
				}
			}

			try {
				cnx.commit();
				duration = System.currentTimeMillis() - startTime;
			}
			catch (RepositoryException e) {
				throw new RuntimeException("RDF triple insertion failed", e);
			}

			log.debug("Inserted {} RDF triples into <{}> in {} seconds",
					Long.valueOf(statementCount), targetGraph,
					Double.valueOf(duration / 1000.0));
		}
		catch (TechnicalException e) {
			throw e;
		}

		catch (Exception e) {
			try {
				// Forget pending triples.
				cnx.rollback();
				// Clear target named graph, if any.
				if (ctx != null) {
					cnx.clear(ctx);
				}
			}
			catch (Exception e2) { /* Ignore... */ }

			throw new TechnicalException("gml.conversion.failed", e);
		}
		finally {
			// Commit pending data (including graph removal in case of error).
			try { cnx.commit(); } catch (Exception e) { /* Ignore... */ }
			// Close repository connection.
			try { cnx.close();  } catch (Exception e) { /* Ignore... */ }
		}

	}

	protected String cleanUpString(String str) {
		if (str.contains(":"))
			str = str.substring(str.lastIndexOf(':') + 1);
		return WordUtils.capitalizeFully(str, new char[] { ' ' }).replaceAll(" ", "").trim();
	}


}

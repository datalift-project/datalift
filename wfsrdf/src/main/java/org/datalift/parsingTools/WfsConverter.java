package org.datalift.parsingTools;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.datalift.fwk.project.ShpSource;
import org.datalift.fwk.rdf.Repository;

import fr.ign.datalift.model.AbstractFeature;

import org.openrdf.repository.*;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.sail.memory.MemoryStore;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.FileSource;
import org.datalift.fwk.project.Project;

import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.ShpSource.Crs;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;

import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.UriBuilder;
import org.datalift.fwk.util.io.FileUtils;
import org.datalift.geomrdf.BaseConverterModule;
import org.datalift.geomrdf.CreateGeoStatement;
import org.datalift.geomrdf.TechnicalException;


import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.util.TimeUtils.asSeconds;

import fr.ign.datalift.constants.CRS;
import fr.ign.datalift.model.FeatureProperty;
import fr.ign.datalift.model.GeometryProperty;
import fr.ign.datalift.parser.Features_Parser;



public class WfsConverter {
	
	private final static Logger log = Logger.getLogger();

	
	
	public WfsConverter()
	{
		
	}
	
	public void ConvertFeaturesToRDF(String wfsSource, ArrayList<AbstractFeature> featuresToConvert, 
			org.datalift.fwk.rdf.Repository target, URI targetGraph, URI baseUri, String targetType)
	{

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
				String sbjUri  = "http://localhost:9091/initkiosques/regions-nouvelles-shp/";
				RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '/');
				String typeUri = "http://localhost:9091/initkiosques/regions-nouvelles-shp#";
				RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '#');
				// Create target RDF type.
				if (! isSet(targetType)) {
					targetType = uriBuilder.urlify("tototiti");
					targetType="regions-nouvelles-shp";
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

				for (int i = 0; i < featuresToConvert.size(); i++) {
					count = i + 1;
					org.openrdf.model.URI feature = vf.createURI(sbjUri + count);

					statement = vf.createStatement(feature, RDF.TYPE, rdfType);
					
					aboutAttributes.add(statement);

					ArrayList<FeatureProperty> featureProperties = (ArrayList<FeatureProperty>) featuresToConvert.get(i).getProperties();

					for (int j = 0; j < featureProperties.size(); j++) {

						FeatureProperty fp = featureProperties.get(j);

						if (fp instanceof GeometryProperty) {

							GeometryProperty gp = (GeometryProperty)fp;
							String geoType = gp.getType();
							org.openrdf.model.URI geomFeature = vf.createURI(sbjUri, geoType + "_" + count);

							cgs.createStatement(gp, vf, feature, geomFeature, geoType, "crs");
							aboutGeometry = cgs.getAboutGeometry();

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
					cnx.begin();
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
							cnx.begin();
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

				log.info("Inserted {} RDF triples into <{}> in {} seconds",
						wrap(statementCount), targetGraph,
						wrap(asSeconds(duration)));
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

				throw new TechnicalException("shp.conversion.failed", e);
			}
			finally {
				// Commit pending data (including graph removal in case of error).
				try { cnx.commit(); } catch (Exception e) { /* Ignore... */}
				// Close repository connection.
				try { cnx.close();  } catch (Exception e) { /* Ignore...  */}
			}
			

	}	
		
	

	
	/*public void ConvertFeaturesToRDF(String wfsSource, ArrayList<AbstractFeature> featuresToConvert, 
			org.openrdf.repository.Repository target, URI targetGraph, URI baseUri, String targetType)
	{
		
		String sesameServer = "	http://localhost:9091/openrdf-sesame";
		String repositoryID = "internal";
		org.openrdf.repository.Repository  repo = new HTTPRepository(sesameServer, repositoryID);
		ValueFactory factory;
		RepositoryConnection cnx = null;
		try {
			cnx = repo.getConnection();
			factory = cnx.getValueFactory();
			// pour utiliser un contexte
			org.openrdf.model.URI context = factory.createURI("http://example.org/hanane/");
			org.openrdf.model.URI bob = factory.createURI("http://example.org/hanane");
			org.openrdf.model.URI name = factory.createURI("http://example.org/name");
			Literal bobsName = factory.createLiteral("Hanane");
			Statement nameStatement = factory.createStatement(bob, name, bobsName, context);
			// sinon tu peux cr�er une liste de statements et ins�rer d'un coup
			cnx.add(nameStatement);
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (cnx!=null) cnx.close();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		
		//final UriBuilder uriBuilder = Configuration.getDefault()
			//	.getBean(UriBuilder.class);
		
//		try {
//			target.initialize();
//			final RepositoryConnection cnx = target.getConnection();
//			org.openrdf.model.URI ctx = null;
//			try {
//				final ValueFactory vf =
//						new UriCachingValueFactory(cnx.getValueFactory());
//
//				// Clear target named graph, if any.
//				if (targetGraph != null) {
//					ctx = vf.createURI(targetGraph.toString());
//					cnx.clear(ctx);
//				}
//				// Create URIs for subjects and predicates.
//				if (baseUri == null) {
//					baseUri = targetGraph;
//				}
//				String sbjUri  = "http://localhost:9091/initkiosques/regions-nouvelles-shp/";
//				//RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '/');
//				String typeUri = "http://localhost:9091/initkiosques/regions-nouvelles-shp#";
//				//RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '#');
//				// Create target RDF type.
//				if (! isSet(targetType)) {
//					//targetType = uriBuilder.urlify("tototiti");
//					targetType="regions-nouvelles-shp";
//				}
//				org.openrdf.model.URI rdfType = null;
//				try {
//					// Assume target type is an absolute URI.
//					rdfType = vf.createURI(targetType);
//				}
//				catch (Exception e) {
//					// Oops, targetType is a relative URI. => Append namespace URI.
//					rdfType = vf.createURI(typeUri, targetType);
//				}
//
//				Statement statement;
//				List<Statement> aboutAttributes = new ArrayList<Statement>();
//				List<Statement> aboutGeometry = new ArrayList<Statement>();
//
//			
//
//				// serialize a featureCollection into RDF
//				int count = 0;
//				CreateGeoStatement cgs = new CreateGeoStatement();
//
//				for (int i = 0; i < featuresToConvert.size(); i++) {
//					count = i + 1;
//					org.openrdf.model.URI feature = vf.createURI(sbjUri + count);
//
//					statement = vf.createStatement(feature, RDF.TYPE, rdfType);
//					
//					aboutAttributes.add(statement);
//
//					ArrayList<FeatureProperty> featureProperties = (ArrayList<FeatureProperty>) featuresToConvert.get(i).getProperties();
//
//					for (int j = 0; j < featureProperties.size(); j++) {
//
//						FeatureProperty fp = featureProperties.get(j);
//
//						if (fp instanceof GeometryProperty) {
//
//							GeometryProperty gp = (GeometryProperty)fp;
//							String geoType = gp.getType();
//							org.openrdf.model.URI geomFeature = vf.createURI(sbjUri, geoType + "_" + count);
//
//							cgs.createStatement(gp, vf, feature, geomFeature, geoType, "crs");
//							aboutGeometry = cgs.getAboutGeometry();
//
//						} else {
//							if (fp.getType() != null){
//								if (fp.getType().contains("int")) {
//									statement = vf.createStatement(feature, vf.createURI(typeUri + fp.getName()), vf.createLiteral(fp.getIntValue()));
//									aboutAttributes.add(statement);
//								} else {
//									statement = vf.createStatement(feature, vf.createURI(typeUri + fp.getName()), vf.createLiteral(fp.getDoubleValue()));
//									aboutAttributes.add(statement);
//								}
//							} else {
//								statement = vf.createStatement(feature, vf.createURI(typeUri + fp.getName()), vf.createLiteral(fp.getValue()));
//								aboutAttributes.add(statement);
//							}
//						}
//					}
//				}
//
//				long startTime = System.currentTimeMillis();
//				long duration = -1L;
//				long statementCount = 0L;
//				int  batchSize = Env.getRdfBatchSize();
//
//				try {
//					// Prevent transaction commit for each triple inserted.
//					cnx.begin();
//				}
//				catch (RepositoryException e) {
//					throw new RuntimeException("RDF triple insertion failed", e);
//				}
//
//				for (Statement at:aboutAttributes){
//					try {
//						cnx.add(at, ctx);
//
//						// Commit transaction according to the configured batch size.
//						statementCount++;
//						if ((statementCount % batchSize) == 0) {
//							cnx.commit();
//							cnx.begin();
//						}
//					}
//					catch (RepositoryException e) {
//						throw new RuntimeException("RDF triple insertion failed", e);
//					}
//				}
//
//				for (Statement gt:aboutGeometry){
//					try {
//						cnx.add(gt, ctx);
//
//						// Commit transaction according to the configured batch size.
//						statementCount++;
//						if ((statementCount % batchSize) == 0) {
//							cnx.commit();
//						}
//					}
//					catch (RepositoryException e) {
//						throw new RuntimeException("RDF triple insertion failed", e);
//					}
//				}
//
//				try {
//					cnx.commit();
//					duration = System.currentTimeMillis() - startTime;
//				}
//				catch (RepositoryException e) {
//					throw new RuntimeException("RDF triple insertion failed", e);
//				}
//
//				System.out.println("Inserted {} RDF triples into <{}> in {} seconds"+
//						wrap(statementCount)+ targetGraph+
//						wrap(asSeconds(duration)));
//			}
//			catch (TechnicalException e) {
//				throw e;
//			}
//
//			catch (Exception e) {
//				try {
//					// Forget pending triples.
//					cnx.rollback();
//					// Clear target named graph, if any.
//					if (ctx != null) {
//						cnx.clear(ctx);
//					}
//				}
//				catch (Exception e2) { /* Ignore... */ //}
//
//				throw new TechnicalException("shp.conversion.failed", e);
//			}
//			finally {
//				// Commit pending data (including graph removal in case of error).
//				try { cnx.commit(); } catch (Exception e) { /* Ignore... */}
//				// Close repository connection.
//				try { cnx.close();  } catch (Exception e) { /* Ignore...  */}
//			}
//			
//
//		} catch (RepositoryException e1) {
//			// TODO Auto-generated catch block
//			e1.printStackTrace();
//		}
//		
	

}

package org.datalift.wfs.wfs2.mapping;

import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.TimeUtils.asSeconds;

import java.net.URI;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.fwk.util.Env;
import org.datalift.wfs.TechnicalException;
import org.openrdf.model.Model;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class StatementSaver {
//	private final static Logger log = Logger.getLogger();
//	public RepositoryConnection cnx ;
//	public ValueFactory vf = new ValueFactoryImpl();
//	public org.openrdf.model.URI uriCTX = null;
//	private int statementCount=0;
//	private java.net.URI targetGraph;
//	
//	public void initConnexion( Repository target, java.net.URI targetGraph, java.net.URI baseUri, String targetType)
//	{
//		//init connexion 
//		try {
//			this.targetGraph=targetGraph;
//			this.cnx = target.newConnection();		
//			this.vf =
//					new UriCachingValueFactory(this.cnx.getValueFactory());
//
//			// Clear target named graph, if any.
//			if (this.targetGraph != null) {
//				this.uriCTX = this.vf.createURI(this.targetGraph.toString());
//				this.cnx.clear(this.uriCTX);
//			} 
//		}
//		catch (RepositoryException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		if (baseUri == null) {
//			baseUri = this.targetGraph;
//		}
//		String sbjUri  = RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '/');
//		//"http://localhost:9091/initkiosques/regions-nouvelles-shp/";
//
//		String typeUri = RdfUtils.getBaseUri((baseUri != null)? baseUri.toString(): null, '#');
//		//"http://localhost:9091/initkiosques/regions-nouvelles-shp#";
//
//
//		org.openrdf.model.URI rdfType = null;
//		try {
//			// Assume target type is an absolute URI.
//			rdfType = vf.createURI(targetType);
//		}
//		catch (Exception e) {
//			// Oops, targetType is a relative URI. => Append namespace URI.
//			rdfType = vf.createURI(typeUri, targetType);
//		}
//
//		long startTime = System.currentTimeMillis();
//		long duration = -1L;
//		long statementCount = 0L;
//		int  batchSize = Env.getRdfBatchSize();
//
//		try {
//			// Prevent transaction commit for each triple inserted.
//			cnx.begin();
//		}
//		catch (RepositoryException e) {
//			throw new RuntimeException("RDF triple insertion failed", e);
//		}
//	}
//	public void flush(Model model)
//	{
//		try {
//
//			long startTime = System.currentTimeMillis();
//			long duration = -1L;
//			long statementCount = 0L;
//			int  batchSize = Env.getRdfBatchSize();
//
//			try {
//				// Prevent transaction commit for each triple inserted.
//				cnx.begin();
//			}
//			catch (RepositoryException e) {
//				throw new RuntimeException("RDF triple insertion failed", e);
//			}
//
//			for (Statement at:model){
//				try {
//					cnx.add(at, uriCTX);
//
//					// Commit transaction according to the configured batch size.
//					statementCount++;
//					if ((cnx.size() % batchSize) == 0) {
//						cnx.commit();
//						cnx.begin();
//					}
//				}
//				catch (RepositoryException e) {
//					throw new RuntimeException("RDF triple insertion failed", e);
//				}
//			}
//			try {
//				cnx.commit();
//				duration = System.currentTimeMillis() - startTime;
//			}
//			catch (RepositoryException e) {
//				throw new RuntimeException("RDF triple insertion failed", e);
//
//			}
//
//			log.info("Inserted {} RDF triples into <{}> in {} seconds",
//					wrap(statementCount), targetGraph,
//					wrap(asSeconds(duration)));
//		}
//		catch (TechnicalException e) {
//			throw e;
//
//		}
//		
//		catch (Exception e) {
//			try {
//				// Forget pending triples.
//				cnx.rollback();
//				// Clear target named graph, if any.
//				if (uriCTX != null) {
//					cnx.clear(uriCTX);
//				}
//			}
//			catch (Exception e2) { /* Ignore... */ }
//
//			
//		}
//		finally {
//			// Commit pending data (including graph removal in case of error).
//			try { cnx.commit(); } catch (Exception e) { /* Ignore... */}
//			// Close repository connection.
//			//try { cnx.close();  } catch (Exception e) { /* Ignore...  */}
//		}			
//		
//		model.clear();
//
//	}
//
//
//
////	public void addStatement(Statement at)
////	{
////		try {
////			cnx.add(at, this.uriCTX);
////			statementCount++;
////		} catch (RepositoryException e) {
////			// TODO Auto-generated catch block
////			e.printStackTrace();
////		}
////
////	}
////
////	public void commitToTS()
////	{
////		try {
////			cnx.commit();
////			cnx.begin();
////		} catch (RepositoryException e) {
////			throw new RuntimeException("RDF triple insertion failed", e);
////		}
////		
////	}
//	public void close()
//	{
//		try {
//			cnx.close();
//		} catch (RepositoryException e) {
//			//ignore			
//		}
//	}
}

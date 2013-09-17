package org.datalift.converter.dbdirectmapper;

import net.antidot.semantic.rdf.model.impl.sesame.SesameDataSet;

import org.datalift.fwk.rdf.Repository;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

import static org.datalift.fwk.util.Env.*;

/**
 * Class to handle the triples resulting from the database mapping and to save 
 * them directly to the DataLift RDF Store in batches of configurable size.
 * 
 * @author csuglia
 * 
 */
public class StoreMappedDataSet extends SesameDataSet {
	
	//-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------
	
	/**Datalift repository where the mapped triples can be saved*/
	private Repository internalRepository;
	/**Context used to create the connection*/
	private URI context;
	/**Connection object to make transactions (add triples) to the repository*/
	private RepositoryConnection transactionCnx;
	/**Number of triples to add to the repository before committing the transaction*/
	private int batchSize;
	/**Number of triples added to the repository*/
	private int statementCount;
	
	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------
	/**
	 * Creates a new dataset instance which will have a default batch size
	 * @param repository Datalift RDF Store where the mapped triples can be saved
	 * @param targetUri the base uri of the resulting mapped graph
	 */
	public StoreMappedDataSet(Repository repository, String targetUri){
		this(repository,targetUri,getRdfBatchSize());
	}
	
	/**
	 * Creates a new dataset instance with custom batch size
	 * @param repository Datalift RDF Store where the mapped triples can be saved
	 * @param targetUri the base uri of the resulting mapped graph
	 * @param batchSize triples to add to the store before committing the transaction 
	 */
	public StoreMappedDataSet(Repository repository, String targetUri, int batchSize){
		this.internalRepository = repository;
		RepositoryConnection conn = internalRepository.newConnection();
		this.context = conn.getValueFactory().createURI(targetUri);
		this.batchSize = batchSize;
	}
	
	//-------------------------------------------------------------------------
	// Specific implementation
	//-------------------------------------------------------------------------
	/**
	 * Create a new triple from the subject, predicate and object just mapped and then add it to 
	 * the Datalift repository
	 *  @param s subject
	 *  @param p predicate
	 *  @param o object
	 *  @param ctx uri context
	 */
	@Override
	public void add(Resource s, URI p, Value o, Resource... ctx){
		try {
			//if there is no opened connection (so there are no pending transactions) create a new one
			if(transactionCnx == null){
				transactionCnx = internalRepository.newConnection();
				//this way we will commit the adding transactions, batch after batch
				transactionCnx.setAutoCommit(false);
			}
			ValueFactory myFactory = transactionCnx.getValueFactory();
			//obtain the triple
			Statement st = myFactory.createStatement( s, p,(Value) o);
			if(ctx.length==0){
				// if there is no uri context add the triple to the default one
				transactionCnx.add(st, context);
			}else{
				transactionCnx.add(st, ctx);
			}
			statementCount++;
			if(statementCount % batchSize == 0){
				//we added enoght triples to commit the transaction
				transactionCnx.commit();
			}
		} catch (RepositoryException e) {
			throw new TechnicalException("triples.saving.failed", e);
		}
	}
	
	/**
	 * This method is run to commit the addition of the triples that were not committed previously.
	 * The Repository connector is closed and resetted since there are no more triples to add
	 */
	public void flush(){
		this.statementCount = 0;
		if(transactionCnx != null){
			try {
				transactionCnx.commit();
			} catch (RepositoryException e) {
				throw new TechnicalException("triples.saving.failed", e);
			} finally {
				try {
					transactionCnx.close();
					transactionCnx = null;
				} catch (RepositoryException e) {
					/* Ignore */
				}
			}
		}
	}
	
}

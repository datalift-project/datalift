package org.datalift.owl.toolkit;

import java.util.Collection;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

/**
 * A transaction that wraps a repository. This transaction does a commit every
 * [transactionSize] inserts. This saves _a lot_ of time compared to a connection
 * in antoCommit.
 * 
 * <p>Usage :
 * <code>
 * Repository r = ...;
 * RepositoryTransaction transaction = new RepositoryTransaction(r);
 * transaction.add(...);
 * transaction.add(...);
 * transaction.add(...);
 * transaction.remove(...);
 * transaction.commit();
 * </code>
 * 
 * @author mondeca
 *
 */
public class RepositoryTransaction {

	private int transactionSize = 500;
	private RepositoryConnection connection;	
	
	private int currentTransactionCount = 0;
	
	
	public RepositoryTransaction(RepositoryConnection connection) throws RepositoryException {
		this.connection = connection;
		this.connection.begin();
	}

	public void add(Collection<Statement> sts, Resource... r) throws RepositoryException {
		for (Statement statement : sts) {
			this.add(statement, r);
		}
		this.commit();
	}
	
	public void remove(Collection<Statement> sts) throws RepositoryException {
		for (Statement statement : sts) {
			this.remove(statement);
		}
		this.commit();
	}	
	
	public void add(Statement s, Resource... r) throws RepositoryException {
		connection.add(s,r);
		commitIfNecessary();
	}
	
	public void remove(Statement s) throws RepositoryException {
		connection.remove(s);
		commitIfNecessary();
	}
	
	private void commitIfNecessary() throws RepositoryException {
		this.currentTransactionCount++;
		if(currentTransactionCount == transactionSize) {
			this.commit();
		}		
	}
	
	public void commit() throws RepositoryException {
		this.connection.commit();
		this.connection.begin();
		this.currentTransactionCount = 0;		
	}

	public int getTransactionSize() {
		return transactionSize;
	}

	public void setTransactionSize(int transactionSize) {
		this.transactionSize = transactionSize;
	}

	public RepositoryConnection getConnection() {
		return connection;
	}
	
}

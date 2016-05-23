package org.datalift.parsingTools;

import org.junit.Test;
import org.openrdf.model.Literal;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.ValueFactory;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.http.HTTPRepository;

public class Saver {
	// tu déclares ton repo, pour toi c'était une autre adresse
	String sesameServer = "	http://localhost:9091/openrdf-sesame";
	String repositoryID = "internal";
	Repository repo = new HTTPRepository(sesameServer, repositoryID);
	ValueFactory factory;
	RepositoryConnection cnx;

	@Test
	public void semantizing() throws Exception {
		// TODO Auto-generated method stub
		try {
			cnx = this.repo.getConnection();
			factory = cnx.getValueFactory();
			// pour utiliser un contexte
			URI context = factory.createURI("http://example.org/hanane/");
			URI bob = factory.createURI("http://example.org/hanane");
			URI name = factory.createURI("http://example.org/name");
			Literal bobsName = factory.createLiteral("Hanane");
			Statement nameStatement = factory.createStatement(bob, name, bobsName, context);
			// sinon tu peux créer une liste de statements et insérer d'un coup
			cnx.add(nameStatement);
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				cnx.close();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

}

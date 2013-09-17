package org.datalift.owl.toolkit;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.Repository;
import org.openrdf.repository.object.ObjectConnection;
import org.openrdf.repository.object.ObjectRepository;
import org.openrdf.repository.object.config.ObjectRepositoryFactory;
import org.openrdf.result.Result;
import org.openrdf.rio.RDFFormat;

/**
 * Parses the templates described in the given RDF stream, and returns a list of Template objects.
 * @author thomas
 *
 */
public class TemplateParser {

	public List<Template> parseTemplates(InputStream stream, RDFFormat format) throws TemplateParsingException {
		ArrayList<Template> result = new ArrayList<Template>();

		try {
			Repository delegate = LocalMemoryRepositoryProvider.initNewProvider(stream,format,RDF.NAMESPACE).getRepository();		
			ObjectRepository or = (new ObjectRepositoryFactory()).createRepository(delegate);
			ObjectConnection con = or.getConnection();
			Result<Template> listResult = con.getObjects(Template.class);
		
			while (listResult.hasNext()) {
				Template t = listResult.next();
				for (Argument a : t.getArguments()) {
				}
				result.add(t);
			}
		} catch (Exception e) {
			throw new TemplateParsingException(e);
		}
		
		return result;
	}

}
package net.antidot.semantic.rdf.model.impl.sesame;

import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;

import org.openrdf.rio.RDFFormat;

public interface DataSet {
	public void add(Resource s, URI p, Value o, Resource... contexts);

	public void remove(Resource s, URI p, Value o, Resource... context);
	
	public int getSize();
	
	public void addStatement(Statement s);
	
	public void addURI(String urlstring);
	
	public void addURI(String urlstring, RDFFormat format);
}

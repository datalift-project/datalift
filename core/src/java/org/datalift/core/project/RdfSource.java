package org.datalift.core.project;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;

import javax.persistence.Entity;

import org.datalift.fwk.MediaTypes;
import org.openrdf.model.Statement;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.helpers.StatementCollector;
import org.openrdf.rio.ntriples.NTriplesParser;
import org.openrdf.rio.rdfxml.RDFXMLParser;

import com.clarkparsia.empire.annotation.RdfsClass;

@Entity
@RdfsClass("datalift:rdfSource")
public class RdfSource extends FileSource implements Iterable<Statement>
{
	private Collection<Statement> content = new LinkedList<Statement>();

	@Override
	public Iterator<Statement> iterator() {
		return Collections.unmodifiableCollection(this.content).iterator();
	}
	
	public void init(String storagePath) {
		try {
			super.init(storagePath);
		} catch (FileNotFoundException e1) {
			throw new RuntimeException();
		}
		if (this.getMimeType() != null) {
			RDFParser parser = null;
			if (this.getMimeType().equals(MediaTypes.APPLICATION_RDF_XML)) {
				parser = new RDFXMLParser();
			}
			else if (this.getMimeType().equals(MediaTypes.APPLICATION_N3)) {
				parser = new NTriplesParser();
			}
			if (parser != null) {
				StatementCollector collector = new StatementCollector(content);
				parser.setRDFHandler(collector);
				try {
					parser.parse(this.getReader(), "http://");
				} catch (Exception e) {
					throw new RuntimeException();
				}
			}
		}
	}
}

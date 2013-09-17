package org.datalift.owl.exporter;


import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.Variant;

import static javax.ws.rs.core.Response.Status.*;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.OWL;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.RDFS;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFWriter;
import org.openrdf.rio.rdfxml.util.RDFXMLPrettyWriter;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.RdfNamespace;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.security.SecurityContext;

import static org.datalift.fwk.rdf.RdfFormat.RDF_XML;
import static org.datalift.fwk.util.StringUtils.*;


@Path(AdhocSchemaExporter.MODULE_NAME)
public class AdhocSchemaExporter extends BaseModule
{
    private final static String DC_ELTS = RdfNamespace.DC_Elements.uri;
    public final static URI DC_TITLE   = new URIImpl(DC_ELTS + "title");
    public final static URI DC_DESC    = new URIImpl(DC_ELTS + "description");
    public final static URI DC_CREATOR = new URIImpl(DC_ELTS + "creator");
    public final static URI DC_DATE    = new URIImpl(DC_ELTS + "date");

    public final static String MODULE_NAME = "adhoc-schema";

    private final static List<Variant> contentTypes;

    private final static Logger log = Logger.getLogger();

    static {
        contentTypes = new LinkedList<Variant>();
        for (RdfFormat r : RdfFormat.values()) {
            for (MediaType mimeType : r.getMimeTypes()) {
                contentTypes.add(new Variant(mimeType, null, null));
            }
        }
    }

    public AdhocSchemaExporter() {
        super(MODULE_NAME);
    }

    @GET
    public Response exportSchema(
                            @QueryParam("default-graph-uri") String repository,
                            @QueryParam("named-graph-uri") String namedGraph,
                            @Context Request request)
                                                throws WebApplicationException {
        if (isBlank(namedGraph)) {
            this.sendError(BAD_REQUEST,
                           "Missing query parameter \"named-graph-uri\"");
        }
        // Compute target repository.
        Repository r = null;
        Configuration cfg = Configuration.getDefault();
        if (isSet(repository)) {
            try {
                r = cfg.getRepository(repository);
            }
            catch (Exception e) {
                this.sendError(BAD_REQUEST,
                               "Unknown repository: " + repository);
            }
        }
        else {
            r = SecurityContext.isUserAuthenticated()?
                        cfg.getInternalRepository() : cfg.getDataRepository();
        }
        if (! r.ask("ASK { GRAPH <" + namedGraph + "> { ?s ?p ?o . } }")) {
            this.sendError(BAD_REQUEST, "Unknown graph: " + namedGraph);
        }
        // Compute best matching RDF representation from HTTP Accept header.
        RdfFormat format = RdfFormat.get(
                        request.selectVariant(contentTypes).getMediaType());
        log.debug("Selected output format: {}", format);
        // Compute schema file name.
        String[] elts = namedGraph.split("(/|#)+");
        String fileName = elts[elts.length-1] + ".owl";
        if (format != RDF_XML) {
            fileName += '.' + format.getFileExtension();
        }
        // Build and export RDF schema.
        return Response.ok(new OwlExporter(r, namedGraph, format))
                       .type(format.getMimeType())
                       .header("Content-Disposition",
                               "attachment; filename=" + fileName)
                       .build();
    }

    private final class OwlExporter implements StreamingOutput
    {
        private final Repository repository;
        private final URI namedGraph;
        private final RdfFormat format;

        private RDFWriter writer = null;

        public OwlExporter(Repository repository, String namedGraph,
                                                  RdfFormat format) {
            this.repository = repository;
            this.namedGraph = new URIImpl(namedGraph);
            this.format     = format;
        }

        @Override
        public void write(OutputStream output) throws IOException,
                                                      WebApplicationException {
            final Comparator<URI> uriSorter = new Comparator<URI>() {
                    @Override
                    public int compare(URI o1, URI o2) {
                        return o1.stringValue().compareTo(o2.stringValue());
                    }
                };
            RepositoryConnection cnx = null;
            String ns = null;
            try {
                log.debug("Extracting ad-hoc schema for {}", namedGraph);
                cnx = this.repository.newConnection();
                // Initialize output.
                this.writer = (format == RDF_XML)?
                                            new RDFXMLPrettyWriter(output):
                                            format.newWriter(output);
                this.startDocument();
                // Extract classes.
                Collection<URI> classes = new TreeSet<URI>(uriSorter);
                // The namespace of the first found class defines the ontology.
                TupleQuery q = cnx.prepareTupleQuery(SPARQL,
                        "SELECT DISTINCT ?t WHERE { GRAPH ?u { ?s a ?t . } }");
                q.setBinding("u", namedGraph);
                TupleQueryResult rs = q.evaluate();
                while (rs.hasNext()) {
                    BindingSet b = rs.next();
                    URI u = (URI)(b.getValue("t"));
                    if (ns == null) {
                        ns = u.getNamespace();
                    }
                    classes.add(u);
                }
                rs.close();
                // Extract ontology data.
                URI ontology = new URIImpl(ns.substring(0, ns.length() - 1));
                this.writeComment("Ontology: "+ ontology.getLocalName());
                q = cnx.prepareTupleQuery(SPARQL,
                    "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n" +
                    "PREFIX dct: <http://purl.org/dc/terms/>\n" +
                    "SELECT * WHERE { ?u dc:creator ?creator ; " +
                                       " dct:issued ?date .\n" +
                                    " OPTIONAL { ?u dc:title ?title . }\n" +
                                    " OPTIONAL { ?u dc:description ?desc . }}");
                q.setBinding("u", namedGraph);
                rs = q.evaluate();
                while (rs.hasNext()) {
                    BindingSet b = rs.next();
                    this.write(ontology, RDF.TYPE,   OWL.ONTOLOGY);
                    Value v = b.getValue("title");
                    if (v != null) {
                        this.write(ontology, DC_TITLE, v);
                    }
                    v = b.getValue("desc");
                    if (v != null) {
                        this.write(ontology, DC_DESC, v);
                    }
                    this.write(ontology, DC_CREATOR, b.getValue("creator"));
                    this.write(ontology, DC_DATE,    b.getValue("date"));
                }
                rs.close();
                // Export classes.
                this.writeComment("Classes");
                for (URI u :classes) {
                    this.writeComment("Class: " + u.getLocalName());
                    this.write(u, RDF.TYPE,   OWL.CLASS);
                    this.write(u, RDFS.LABEL, u.getLocalName());
                }
                // Extract properties.
                this.writeComment("Properties");
                Collection<URI> properties = new TreeSet<URI>(uriSorter);
                q = cnx.prepareTupleQuery(SPARQL,
                    "SELECT DISTINCT ?p WHERE {\n" +
                            "GRAPH ?u { ?s ?p ?o .\n" +
                                      " FILTER(STRSTARTS(STR(?p), ?ns)) } }");
                q.setBinding("u",  namedGraph);
                q.setBinding("ns", new LiteralImpl(ns));
                rs = q.evaluate();
                while (rs.hasNext()) {
                    BindingSet b = rs.next();
                    properties.add((URI)(b.getValue("p")));
                }
                rs.close();
                // Extract properties typing data: domain and range.
                q = cnx.prepareTupleQuery(SPARQL,
                    "SELECT ?s ?o ?domain ?range WHERE { \n" +
                        "GRAPH ?u { ?s ?p ?o ; a ?domain .\n" +
                                  " OPTIONAL { ?o a ?range . } } } LIMIT 1");
                for (URI p : properties) {
                    q.setBinding("u", namedGraph);
                    q.setBinding("p", p);
                    rs = q.evaluate();
                    BindingSet b = rs.next();

                    URI type  = null;
                    URI range = null;
                    Value v = b.getValue("o");
                    if (v instanceof Resource) {
                        type  = OWL.OBJECTPROPERTY;
                        range = (URI)(b.getValue("range"));
                    }
                    else if (v instanceof Literal) {
                        type  = OWL.DATATYPEPROPERTY;
                        range = ((Literal)v).getDatatype();
                    }
                    if (type != null) {
                        this.writeComment(type.getLocalName()
                                                    + ": " + p.getLocalName());
                        this.write(p, RDF.TYPE, type);
                        this.write(p, RDFS.LABEL, p.getLocalName());
                        if (range != null) {
                            this.write(p, RDFS.RANGE, range);
                        }
                        this.write(p, RDFS.DOMAIN, b.getValue("domain"));
                    }
                    rs.close();
                }
                this.endDocument();
            }
            catch (IOException e) {
                log.fatal("IO error while exporting schema for {}", e,
                          namedGraph);
                throw e;
            }
            catch (Exception e) {
                log.fatal("Unexpected error while exporting schema for {}", e,
                          namedGraph);
               sendError(INTERNAL_SERVER_ERROR, e.getMessage());
            }
            finally {
                if (cnx != null) {
                    try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
                }
            }
        }

        private void startDocument() throws IOException {
            try {
                this.writer.startRDF();
                this.writer.handleNamespace("rdfs", RDFS.NAMESPACE);
                this.writer.handleNamespace("owl",  OWL.NAMESPACE);
                this.writer.handleNamespace("dc",   DC_ELTS);
            }
            catch (RDFHandlerException e) {
                this.handleError(e);
            }
        }

        private void endDocument() throws IOException {
            try {
                this.writer.endRDF();
            }
            catch (RDFHandlerException e) {
                this.handleError(e);
            }
        }

        private void write(Resource subject, URI predicate, String value)
                                                            throws IOException {
            this.write(subject, predicate, new LiteralImpl(value));
        }

        private void write(Resource subject, URI predicate, Value object)
                                                            throws IOException {
            try {
                this.writer.handleStatement(
                                new StatementImpl(subject, predicate, object));
            }
            catch (RDFHandlerException e) {
                this.handleError(e);
            }
        }

        private void writeComment(String comment) throws IOException {
            try {
                this.writer.handleComment(comment);
            }
            catch (RDFHandlerException e) {
                this.handleError(e);
            }
        }

        private void handleError(RDFHandlerException e) throws IOException {
            if (e.getCause() instanceof IOException) {
                throw (IOException)(e.getCause());
            }
            else {
                throw new IOException(e);
            }
        }
    }
}

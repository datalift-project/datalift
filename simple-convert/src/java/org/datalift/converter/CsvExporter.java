/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
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

package org.datalift.converter;


import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.datatypes.XMLDatatypeUtil;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.model.vocabulary.XMLSchema;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerBase;

import au.com.bytecode.opencsv.CSVWriter;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.TransformedRdfSource;
import org.datalift.fwk.project.CsvSource.Separator;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfException;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.web.Charsets;
import org.datalift.fwk.view.TemplateModel;

import static org.datalift.fwk.MediaTypes.*;
import static org.datalift.fwk.util.PrimitiveUtils.wrap;


/**
 * A {@link ProjectModule project module} that streams the RDF triples
 * of a source in CSV format from the internal repository into the HTTP
 * response, to save them into a file on the user's computer.
 * <p>
 * The first column contains the triple subject URI, the remaining
 * columns contain the value of each distinct predicate discovered in
 * the source.</p>
 *
 * @author lbihanic
 */
@Path(CsvExporter.MODULE_NAME)
public class CsvExporter extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "csvexporter";

    private final static String SOURCE_PREDICATES_QUERY =
                "SELECT DISTINCT ?p WHERE { GRAPH ?g { ?s ?p ?o . } }";
    private final static String EXTRACT_VALUES_QUERY =
                "SELECT ?s ?p ?o WHERE {\n" +
                    "GRAPH ?g { ?s ?p ?o . }\n" +
                "} ORDER BY DESC(?s)";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public CsvExporter() {
        super(MODULE_NAME, 10030, SourceType.TransformedRdfSource);
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
        TemplateModel view = this.newView("csvExporter.vm", p);
        view.put("converter", this);
        view.put("charsets", Charsets.availableCharsets);
        view.put("separators", Separator.values());
        return Response.ok(view).build();
    }

    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response exportSourceAsCsv(@FormParam("project") URI projectId,
                                      @FormParam("source") URI sourceId,
                                      @FormParam("charset") String charset,
                                      @FormParam("separator") String separator)
                                                throws WebApplicationException {
        Response response = null;

        Repository internal = Configuration.getDefault()
                                           .getInternalRepository();
        try {
            // Validate request parameters.
            Charset cs = null;
            try {
                cs = Charset.forName(charset);
            }
            catch (Exception e) {
                this.throwInvalidParamError("charset", charset);
            }
            Separator sep = null;
            try {
                sep = Separator.valueOf(separator);
            }
            catch (Exception e) {
                this.throwInvalidParamError("separator", separator);
            }
            // Retrieve source.
            Project p = this.getProject(projectId);
            TransformedRdfSource s = (TransformedRdfSource)
                                                        (p.getSource(sourceId));
            if (s == null) {
                this.throwInvalidParamError("source", sourceId);
            }
            // Build default file name for downloaded data.
            String name = this.getTerminalName(s.getUri()) + ".csv";
            // Dump selected to directly into the socket.
            StreamingOutput out = new SourceDumpStreamingOutput(internal,
                                                s.getTargetGraph(), cs, sep);
            response = Response.ok(out, APPLICATION_CSV_TYPE)
                               .header("Content-Disposition",
                                       "attachment; filename=" + name)
                               .header("Refresh", "0.1; " + p.getUri())
                               .build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    private final class SourceDumpStreamingOutput implements StreamingOutput
    {
        private final Repository repository;
        private final String namedGraph;
        private final Charset charset;
        private final Separator separator;

        /** Creates a new SourceDumpStreamingOutput. */
        public SourceDumpStreamingOutput(Repository repository,
                                         String namedGraph, Charset charset,
                                         Separator separator) {
            this.repository = repository;
            this.namedGraph = namedGraph;
            this.charset    = charset;
            this.separator  = separator;
        }

        /** {@inheritDoc} */
        @Override
        public void write(OutputStream out)
                                throws IOException, WebApplicationException {
            final CSVWriter w = new CSVWriter(
                                    new BufferedWriter(
                                        new OutputStreamWriter(out, charset),
                                        Env.getFileBufferSize()),
                                    separator.getValue());
            final long t0 = System.currentTimeMillis();
            try {
                log.trace("Preparing CSV export of <{}>", this.namedGraph);

                Map<String,Object> bindings = new HashMap<String,Object>();
                bindings.put("g", URI.create(this.namedGraph));
                // Extract all predicates from the source named graph.
                final List<org.openrdf.model.URI> predicates =
                                        new ArrayList<org.openrdf.model.URI>();
                this.repository.select(SOURCE_PREDICATES_QUERY, bindings,
                    new TupleQueryResultHandlerBase() {
                        @Override
                        public void handleSolution(BindingSet b) {
                            org.openrdf.model.URI p =
                                    (org.openrdf.model.URI)(b.getValue("p"));
                            if (! p.equals(RDF.TYPE)) {
                                predicates.add(p);
                            }
                            // Else: Skip RDF type predicate.
                        }
                    });
                // Sort predicates by local name.
                Collections.sort(predicates,
                     new Comparator<org.openrdf.model.URI>() {
                        @Override
                        public int compare(org.openrdf.model.URI u1,
                                           org.openrdf.model.URI u2) {
                            return u1.getLocalName().compareTo(u2.getLocalName());
                        }
                    });
                log.trace("Found {} predicates to export",
                                                    wrap(predicates.size()));
                // Write header line.
                final String[] data = new String[predicates.size() + 1];
                data[0] = "URI";
                int i = 1;
                for (org.openrdf.model.URI u : predicates) {
                    data[i++] = u.getLocalName();
                }
                w.writeNext(data);
                // Extract source data and output one line per subject.
                this.repository.select(EXTRACT_VALUES_QUERY, bindings,
                    new TupleQueryResultHandlerBase() {
                        private long lineCount = 0L;
                        private org.openrdf.model.URI subject = null;
                        private Map<org.openrdf.model.URI,Value> values =
                                    new HashMap<org.openrdf.model.URI,Value>();

                        @Override
                        public void handleSolution(BindingSet b) {
                            org.openrdf.model.URI s =
                                    (org.openrdf.model.URI)(b.getValue("s"));
                            if (! s.equals(this.subject)) {
                                // Write CSV data for previous subject.
                                this.writeLine();
                                // Switch subject.
                                this.subject = s;
                            }
                            // Store value for predicate.
                            this.values.put(
                                    (org.openrdf.model.URI)(b.getValue("p")),
                                    b.getValue("o"));
                        }

                        @Override
                        public void endQueryResult() {
                            // Write CSV data for last subject.
                            this.writeLine();
                            // Log export elapsed time.
                            long delay = System.currentTimeMillis() - t0;
                            log.debug("Exported {} CSV lines from <{}> in {} seconds",
                                      wrap(lineCount), namedGraph,
                                      wrap(delay / 1000.0));
                        }

                        private void writeLine() {
                            if (! this.values.isEmpty()) {
                                // Format and write CSV data.
                                data[0] = getValue(this.subject);
                                int i = 1;
                                for (org.openrdf.model.URI p : predicates) {
                                    Value v = this.values.get(p);
                                    data[i++] = (v != null)? getValue(v): null;
                                }
                                w.writeNext(data);
                                this.lineCount++;
                                this.values.clear();
                            }
                            // Else: ignore...
                        }
                    });
                w.flush();
            }
            catch (RdfException e) {
                handleInternalError(e);
            }
            finally {
                try { w.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }

        /**
         * Returns a proper string representation for RDF value data
         * types.
         * @param  v   the RDF value
         * @return the string representation of <code>v</code> suitable
         *         for writing to a CSV cell.
         */
        private String getValue(Value v) {
            String s = null;
            if (v instanceof org.openrdf.model.URI) {
                s = ((org.openrdf.model.URI)v).toString();
            }
            else if (v instanceof BNode) {
                s = ((BNode)v).getID();
            }
            else if (v instanceof Literal) {
                Literal l = (Literal)v;
                s = l.getLabel();

                org.openrdf.model.URI t = l.getDatatype();
                if ((t != null) &&
                    ((XMLDatatypeUtil.isIntegerDatatype(t)) ||
                     (XMLDatatypeUtil.isDecimalDatatype(t)) ||
                     (XMLSchema.DOUBLE.equals(t)))) {
                    s = XMLDatatypeUtil.normalize(s, t);
                }
            }
            return s;
        }
    }
}

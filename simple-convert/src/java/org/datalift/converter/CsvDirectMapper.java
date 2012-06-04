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


import java.net.URI;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.util.Calendar.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import static javax.xml.datatype.DatatypeConstants.*;

import org.openrdf.model.Literal;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.Source.SourceType;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.rdf.UriCachingValueFactory;
import org.datalift.fwk.util.Env;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * A {@link ProjectModule project module} that performs CSV to RDF
 * conversion using
 * <a href="http://www.w3.org/TR/2011/WD-rdb-direct-mapping-20110324/">RDF
 * Direct Mapping</a> principles.
 *
 * @author lbihanic
 */
@Path(CsvDirectMapper.MODULE_NAME)
public class CsvDirectMapper extends BaseConverterModule
{
    //-------------------------------------------------------------------------
    // CSV column type mapping enumeration
    //-------------------------------------------------------------------------

    /**
     * CSV column type mapping enumeration.
     */
    public enum Mapping {
        String          ("string"),
        Integer         ("int"),
        Float           ("float"),
        Boolean         ("boolean"),
        Date            ("date"),
        Id              ("id"),
        Automatic       ("auto"),
        Ignore          ("ignore");

        private final String label;

        Mapping(String label) {
            this.label = label;
        }

        /**
         * Return the enumeration value corresponding to the specified
         * string, ignoring case.
         * @param  s   the description type, as a string.
         *
         * @return the description type value or <code>null</code> if
         *         the specified string was not recognized.
         */
        public static Mapping fromString(String s) {
            Mapping v = Automatic;
            if (isSet(s)) {
                for (Mapping t : values()) {
                    if (t.label.equalsIgnoreCase(s)) {
                        v = t;
                        break;
                    }
                }
            }
            return v;
        }
    }

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The name of this module in the DataLift configuration. */
    public final static String MODULE_NAME = "csvdirectmapper";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor. */
    public CsvDirectMapper() {
        super(MODULE_NAME, 100, SourceType.CsvSource);
    }

    //-------------------------------------------------------------------------
    // Web services
    //-------------------------------------------------------------------------

    @GET
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        // Retrieve project.
        Project p = this.getProject(projectId);
        // Display conversion configuration page.
        Map<String, Object> args = new HashMap<String, Object>();
        args.put("it", p);
        args.put("converter", this);
        return Response.ok(this.newViewable("/csvDirectMapper.vm", args))
                       .build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response mapCsvData(@FormParam("project") URI projectId,
                               @FormParam("source") URI sourceId,
                               @FormParam("dest_title") String destTitle,
                               @FormParam("dest_graph_uri") URI targetGraph,
                               @FormParam("base_uri") URI baseUri,
                               @FormParam("true_values") String trueValues,
                               @FormParam("date_format") String dateFormat,
                               MultivaluedMap<String,String> params)
                                                throws WebApplicationException {
        // Note: There a bug in Jersey that cause the MultivalueMap to be
        // empty unless at least one @FormParm annotation is present.
        // see: http://jersey.576304.n2.nabble.com/POST-parameters-not-injected-via-MultivaluedMap-td6434341.html

        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            CsvSource in = (CsvSource)(p.getSource(sourceId));
            // Load datatype mapping for each column.
            Map<String,Mapping> typeMappings = new HashMap<String,Mapping>();
            for (String k : params.keySet()) {
                if (k.startsWith("col_")) {
                    String col = k.substring(4);
                    Mapping m  = Mapping.fromString(params.getFirst(k));
                    log.debug("Type mapping: {} -> {}", col, m);
                    typeMappings.put(col, m);
                }
                // Else: Ignore, not a column type mapping description.
            }
            MappingDesc desc = null;
            try {
                desc = new MappingDesc(typeMappings, trueValues, dateFormat);
            }
            catch (IllegalArgumentException e) {
                this.throwInvalidParamError("date_format", dateFormat);
            }
            // Convert CSV data and load generated RDF triples.
            this.convert(in, Configuration.getDefault().getInternalRepository(),
                             targetGraph, baseUri, desc);
            // Register new transformed RDF source.
            Source out = this.addResultSource(p, in, destTitle, targetGraph);
            // Display project source tab, including the newly created source.
            response = this.created(out).build();
        }
        catch (Exception e) {
            this.handleInternalError(e);
        }
        return response;
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private void convert(CsvSource src, Repository target,
                                        URI targetGraph, URI baseUri,
                                        MappingDesc mapping) {
        final RepositoryConnection cnx = target.newConnection();
        org.openrdf.model.URI ctx = null;
        try {
            final ValueFactory valueFactory =
                            new UriCachingValueFactory(cnx.getValueFactory());

            long t0 = System.currentTimeMillis();
            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
            // Clear target named graph, if any.
            if (targetGraph != null) {
                ctx = valueFactory.createURI(targetGraph.toString());
                cnx.clear(ctx);
            }
            if (baseUri == null) {
                baseUri = targetGraph;
            }
            String root = RdfUtils.getBaseUri(
                                (baseUri != null)? baseUri.toString(): null);
            org.openrdf.model.URI rdfType = valueFactory.createURI(
                                        root.substring(0, root.length() - 1));
            // Build predicates URIs.
            Map<String,org.openrdf.model.URI> predicates =
                                    new HashMap<String,org.openrdf.model.URI>();
            for (String s : src.getColumnNames()) {
                predicates.put(s, valueFactory.createURI(root + urlify(s)));
            }
            // Load triples
            long statementCount = 0L;
            long duration = 0L;
            int  batchSize = Env.getRdfBatchSize();
            int i = 1;                          // Start line numbering at 1.
            Map<org.openrdf.model.URI,Value> statements =
                            new LinkedHashMap<org.openrdf.model.URI,Value>();
            for (Row<String> row : src) {
                statements.clear();
                // Scan columns to map values and build triples.
                org.openrdf.model.URI subject = null;
                for (String s : src.getColumnNames()) {
                    String  v = row.get(s);
                    Mapping m = mapping.getMapping(s);
                    Value value = null;
                    if (isSet(v)) {
                        if (m == Mapping.Id) {
                            subject = valueFactory.createURI(root + urlify(v)); // + "#_";
                        }
                        else {
                            value = this.mapValue(v, valueFactory, m, mapping);                            
                        }
                    }
                    if (value != null) {
                        statements.put(predicates.get(s), value);
                    }
                    // Else: Ignore cell.
                }
                // Auto-generate row URI if no identifier column was defined.
                if (subject == null) {
                    subject = valueFactory.createURI(root + i); // + "#_";
                }
                // Append RDF type triple.
                if (! statements.isEmpty()) {
                    statements.put(RDF.TYPE, rdfType);
                }
                // Save triples into RDF store.
                for (Map.Entry<org.openrdf.model.URI,Value>e :
                                                    statements.entrySet()) {
                    cnx.add(valueFactory.createStatement(
                                    subject, e.getKey(), e.getValue()), ctx);

                    // Commit transaction according to the configured batch size.
                    statementCount++;
                    if ((statementCount % batchSize) == 0) {
                        cnx.commit();
                        // Trace progress.
                        if (log.isTraceEnabled()) {
                            duration = System.currentTimeMillis() - t0;
                            log.trace("Inserted {} RDF triples from {} CSV lines in {} seconds...",
                                      Long.valueOf(statementCount),
                                      Integer.valueOf(i - 1),
                                      Double.valueOf(duration / 1000.0));
                        }
                    }
                }
                i++;
            }
            cnx.commit();
            duration = System.currentTimeMillis() - t0;
            log.debug("Inserted {} RDF triples into <{}> from {} CSV lines in {} seconds",
                      Long.valueOf(statementCount), targetGraph,
                      Integer.valueOf(i - 1), Double.valueOf(duration / 1000.0));
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

            throw new TechnicalException("csv.conversion.failed", e);
        }
        finally {
            // Commit pending data (including graph removal in case of error).
            try { cnx.commit(); } catch (Exception e) { /* Ignore... */ }
            // Close repository connection.
            try { cnx.close();  } catch (Exception e) { /* Ignore... */ }
        }
    }

    private Value mapValue(String s, ValueFactory valueFactory,
                                       Mapping mapping, MappingDesc desc) {
        Value v = null;
        s = s.trim();
        switch (mapping) {
            case Ignore:
                break;
            case String:
                v = this.mapString(s, valueFactory);
                break;
            case Integer:
                v = this.mapInt(s, valueFactory);
                break;
            case Float:
                v = this.mapFloat(s, valueFactory);
                break;
            case Boolean:
                v = this.mapBoolean(s, valueFactory, desc);
                break;
            case Date:
                v = this.mapDate(s, valueFactory, desc, false);
                break;
            default:
                // Automatic mapping on a per-cell basis.
                if ((s.indexOf('.') != -1) || (s.indexOf(',') != -1)) {
                    // Try double.
                    v = this.mapFloat(s, valueFactory);
                }
                if (v == null) {
                    // Try integer.
                    v = this.mapInt(s, valueFactory);
                }
                if ((v == null) && (desc.hasBooleanValues())) {
                    // Try boolean.
                    v = this.mapBoolean(s, valueFactory, desc);
                }
                if ((v == null) && (desc.hasDateFormat())) {
                    // Try date/time.
                    v = this.mapDate(s, valueFactory, desc, true);
                }
                if (v == null) {
                    // Assume string.
                    v = this.mapString(s, valueFactory);
                }
                break;
        }
        return v;
    }

    private Literal mapString(String s, ValueFactory valueFactory) {
        return valueFactory.createLiteral(s);
    }

    private Literal mapInt(String s, ValueFactory valueFactory) {
        Literal v = null;
        try {
            v = valueFactory.createLiteral(Long.parseLong(s));
        }
        catch (Exception e) {
            /* Ignore... */
        }
        return v;
    }

    private Literal mapFloat(String s, ValueFactory valueFactory) {
        Literal v = null;
        try {
            v = valueFactory.createLiteral(
                                    Double.parseDouble(s.replace(',', '.')));
        }
        catch (Exception e) {
            /* Ignore... */
        }
        return v;
    }

    private Literal mapBoolean(String s, ValueFactory valueFactory,
                                         MappingDesc desc) {
        return valueFactory.createLiteral(desc.parseBoolean(s));
    }

    private Literal mapDate(String s, ValueFactory valueFactory,
                                      MappingDesc desc, boolean tentative) {
        Literal v = null;
        try {
            v = valueFactory.createLiteral(desc.parseDate(s, tentative));
        }
        catch (Exception e) {
            /* Ignore... */
        }
        return v;
    }

    private final static class MappingDesc
    {
        private final Map<String,Mapping> mappings;
        private final List<String> trueValues;
        private final DateFormat dateFormat;
        private final DatatypeFactory dateFactory;

        public MappingDesc(Map<String,Mapping> typeMappings,
                           String trueValues, String dateFormat) {
            if ((typeMappings == null) || (typeMappings.isEmpty())) {
                throw new IllegalArgumentException("typeMappings");
            }
            // Type mappings.
            this.mappings = new HashMap<String,Mapping>(typeMappings);
            // Boolean TRUE values.
            List<String> l = Collections.emptyList();
            if (isSet(trueValues)) {
                String[] v = trueValues.trim().split("\\s*,\\s*");
                l = new ArrayList<String>(v.length);
                for (String s : v) {
                    if (isSet(s)) {
                        l.add(s.toLowerCase());
                    }
                }
            }
            this.trueValues = l;
            // Date format.
            DateFormat fmt = null;
            DatatypeFactory df = null;
            if (isSet(dateFormat)) {
                fmt = new SimpleDateFormat(dateFormat);
                GregorianCalendar c = new GregorianCalendar();
                c.clear();
                fmt.setCalendar(c);
                fmt.setLenient(true);   // Best effort!
                try {
                    df = DatatypeFactory.newInstance();
                }
                catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            this.dateFormat  = fmt;
            this.dateFactory = df;
        }

        public Mapping getMapping(String column) {
            return this.mappings.get(column);
        }

        public boolean hasBooleanValues() {
            return (! this.trueValues.isEmpty());
        }

        public boolean parseBoolean(String s) {
            return (isSet(s))? this.trueValues.contains(s.toLowerCase()): false;
        }

        public boolean hasDateFormat() {
            return (this.dateFormat != null);
        }

        public XMLGregorianCalendar parseDate(String s, boolean tentative) {
            XMLGregorianCalendar date = null;
            if ((this.dateFormat != null) && (! isBlank(s))) {
                try {
                    this.dateFormat.parse(s.trim());
                    Calendar c = this.dateFormat.getCalendar();
                    date = this.dateFactory.newXMLGregorianCalendar(
                                this.get(c, YEAR), this.get(c, MONTH),
                                this.get(c, DAY_OF_MONTH),
                                this.get(c, HOUR_OF_DAY), this.get(c, MINUTE),
                                this.get(c, SECOND), this.get(c, MILLISECOND),
                                FIELD_UNDEFINED /* Timezone */);
                }
                catch (Exception e) {
                    if (! tentative) {
                        // Not a blind (auto-detect) conversion attempt.
                        // => Report error.
                        log.warn("Date conversion failed for \"{}\"", e, s);
                    }
                    // Else: Ignore...
                }
            }
            return date;
        }

        private int get(Calendar c, int field) {
            int v = FIELD_UNDEFINED;
            if (c.isSet(field)) {
                v = c.get(field);
                if (field == MONTH) {
                    v++;        // From 0-based Calendar to 1-based XML date.
                }
            }
            return v;
        }
    }
}

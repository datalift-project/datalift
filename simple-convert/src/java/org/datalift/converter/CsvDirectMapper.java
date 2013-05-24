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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static java.util.Calendar.*;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
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
import org.datalift.fwk.util.UriBuilder;

import static org.datalift.fwk.rdf.ElementType.*;
import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.fwk.MediaTypes.*;


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
        URI             ("uri"),
        Automatic       ("auto"),
        Ignore          ("ignore");

        private final String label;

        Mapping(String label) {
            this.label = label;
        }

        /**
         * Returns the mapping label.
         * @return the mapping label.
         */
        public String getLabel() {
            return this.label;
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

    /**
     * The regex pattern to match possible separators in numbers
     * but the default decimal separators ('.').
     */
    private final static Pattern SEPARATORS_PATTERN =
                                            Pattern.compile("[\\s\\u00a0,]+");

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

    /**
     * <i>[Resource method]</i> Displays the module welcome page.
     * @param  projectId   the URI of the datalifting project.
     *
     * @return a JAX-RS response with the page template and parameters.
     */
    @GET
    @Produces({ TEXT_HTML, APPLICATION_XHTML_XML })
    public Response getIndexPage(@QueryParam("project") URI projectId) {
        return this.newProjectView("csvDirectMapper.vm", projectId);
    }

    /**
     * <i>[Resource method]</i> Converts the data of the specified CSV
     * source into RDF triples, loads them in the internal store and
     * creates a new associated RDF source.
     * @param  projectId     the URI of the datalifting project.
     * @param  sourceId      the URI of the source to convert.
     * @param  destTitle     the name of the RDF source to hold the
     *                       converted data.
     * @param  targetGraph   the URI of the named graph to hold the
     *                       converted data, which will also be the URI
     *                       of the created RDF source.
     * @param  baseUri       the base URI to build the RDF identifiers
     *                       from the CSV data.
     * @param  trueValues    the list of values to be regarded as TRUE
     *                       for the columns to convert to booleans.
     * @param  dateFormat    the {@link DateFormat date format} to use
     *                       when converting cells into dates.
     * @param  keyColumn     the CSV column to use as identifier when
     *                       creating RDF object. If not specified, the
     *                       row number is used as identifier.
     * @param  targetType    The URI (absolute or relative to the
     *                       <code>baseUri</code>) of the RDF type to
     *                       assign to the created RDF objects.
     * @param  params        The form parameters, to extract the type
     *                       mapping for each column.
     *
     * @return a JAX-RS response redirecting the user browser to the
     *         created RDF source.
     * @throws WebApplicationException if any error occurred during the
     *         data conversion from CSV to RDF.
     */
    @POST
    @Consumes(APPLICATION_FORM_URLENCODED)
    public Response mapCsvData(
                    @FormParam("project") URI projectId,
                    @FormParam("source") URI sourceId,
                    @FormParam("dest_title") String destTitle,
                    @FormParam("dest_graph_uri") URI targetGraph,
                    @FormParam("base_uri") URI baseUri,
                    @FormParam("true_values") String trueValues,
                    @FormParam("date_format") String dateFormat,
                    @FormParam("key_column") @DefaultValue("-1") int keyColumn,
                    @FormParam("dest_type") String targetType,
                    MultivaluedMap<String,String> params)
                                                throws WebApplicationException {
        // Note: There a bug in Jersey that cause the MultivalueMap to be
        //       empty unless at least one @FormParm annotation is present.
        // See: http://jersey.576304.n2.nabble.com/POST-parameters-not-injected-via-MultivaluedMap-td6434341.html

        Response response = null;
        try {
            // Retrieve project.
            Project p = this.getProject(projectId);
            // Load input source.
            CsvSource in = (CsvSource)(p.getSource(sourceId));
            // Load datatype mapping for each column.
            Mapping[] typeMappings = new Mapping[params.size()];
            for (String k : params.keySet()) {
                if (k.startsWith("col_")) {
                    try {
                        int col = Integer.parseInt(k.substring(4));
                        Mapping m  = Mapping.fromString(params.getFirst(k));
                        if (log.isTraceEnabled()) {
                            log.trace("Type mapping: Column #{} -> {}",
                                                                wrap(col), m);
                        }
                        typeMappings[col] = m;
                    }
                    catch (Exception e) { /* Ignore... */ }
                }
                // Else: Ignore, not a column type mapping description.
            }
            MappingDesc desc = null;
            try {
                desc = new MappingDesc(typeMappings, keyColumn,
                                                     trueValues, dateFormat);
            }
            catch (IllegalArgumentException e) {
                this.throwInvalidParamError("date_format", dateFormat);
            }
            // Convert CSV data and load generated RDF triples.
            this.convert(in, Configuration.getDefault().getInternalRepository(),
                             targetGraph, baseUri, targetType, desc);
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

    /**
     * Returns the available type mappings.
     * @return the available type mappings as an array.
     */
    public Mapping[] getMappings() {
        return Mapping.values();
    }

    private void convert(CsvSource src, Repository target,
                                        URI targetGraph, URI baseUri,
                                        String targetType,
                                        MappingDesc mapping) {
        final UriBuilder uriBuilder = Configuration.getDefault()
                                                   .getBean(UriBuilder.class);
        final RepositoryConnection cnx = target.newConnection();
        org.openrdf.model.URI ctx = null;
        try {
            final ValueFactory valueFactory =
                            new UriCachingValueFactory(cnx.getValueFactory());

            long t0 = System.currentTimeMillis();
            char quote = src.getQuoteCharacter();
            // Prevent transaction commit for each triple inserted.
            cnx.setAutoCommit(false);
            // Clear target named graph, if any.
            if (targetGraph != null) {
                ctx = valueFactory.createURI(targetGraph.toString());
                cnx.clear(ctx);
            }
            // Create URIs for objects and predicates.
            if (baseUri == null) {
                baseUri = targetGraph;
            }
            String objUri  = RdfUtils.getBaseUri(
                            (baseUri != null)? baseUri.toString(): null, '/');
            String typeUri = RdfUtils.getBaseUri(
                            (baseUri != null)? baseUri.toString(): null, '#');
            // Create target RDF type.
            if (! isSet(targetType)) {
                targetType = uriBuilder.urlify(src.getTitle(), RdfType);
            }
            org.openrdf.model.URI rdfType = null;
            try {
                // Assume target type is an absolute URI.
                rdfType = valueFactory.createURI(targetType);
            }
            catch (Exception e) {
                // Oops, targetType is a relative URI. => Append namespace URI.
                rdfType = valueFactory.createURI(typeUri, targetType);
            }
            // Build predicates URIs.
            int n = src.getColumnNames().size();
            org.openrdf.model.URI[] predicates = new org.openrdf.model.URI[n];
            int i = 0;
            for (String s : src.getColumnNames()) {
                predicates[i++] = valueFactory.createURI(
                                    typeUri + uriBuilder.urlify(s, Predicate));
            }
            // Load triples.
            long statementCount = 0L;
            long duration = 0L;
            int  batchSize = Env.getRdfBatchSize();
            i = 1;                              // Start line numbering at 1.
            Map<org.openrdf.model.URI,Value> statements =
                            new LinkedHashMap<org.openrdf.model.URI,Value>();
            for (Row<String> row : src) {
                statements.clear();
                boolean skipRow = false;
                // Scan columns to map values and build triples.
                org.openrdf.model.URI subject = null;
                for (int j=0, max=row.size(); j<max; j++) {
                    Mapping m = mapping.getMapping(j);
                    Value value = null;
                    String v = trimToNull(row.get(j));  // Trim value.
                    if (v != null) {
                        // Handle special case of unmatched closing quote, left
                        // over by CSV parser when followed by padding.
                        if (v.indexOf(quote) == v.length() - 1) {
                            v = v.substring(0, v.length() - 1);
                        }
                        value = this.mapValue(v, valueFactory, m, mapping);
                        if (j == mapping.keyColumn) {
                            subject = valueFactory.createURI(objUri +
                                            uriBuilder.urlify(v, Resource)); // + "#_";
                        }
                    }
                    else {
                        // Skip rows without identifier.
                        if (j == mapping.keyColumn) skipRow = true;
                    }
                    if (value != null) {
                        statements.put(predicates[j], value);
                    }
                    // Else: Ignore cell.
                }
                if (skipRow) continue;

                // Auto-generate row URI if no identifier column was defined.
                if (subject == null) {
                    subject = valueFactory.createURI(objUri + i); // + "#_";
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
                                      wrap(statementCount), wrap(i - 1),
                                      wrap(duration / 1000.0));
                        }
                    }
                }
                i++;
            }
            cnx.commit();
            duration = System.currentTimeMillis() - t0;
            log.debug("Inserted {} RDF triples into <{}> from {} CSV lines in {} seconds",
                      wrap(statementCount), targetGraph,
                      wrap(i - 1), wrap(duration / 1000.0));
        }
        catch (TechnicalException e) {
            throw e;
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
            case URI:
                v = this.mapUri(s, valueFactory);
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

    /**
     * Converts a string into an RDF {@link Literal string literal}.
     * @param  s              the string to map.
     * @param  valueFactory   the {@link ValueFactory value factory} to
     *                        use to allocate the literal.
     * @return a {@link Literal} holding the string value.
     */
    private Literal mapString(String s, ValueFactory valueFactory) {
        return valueFactory.createLiteral(
                                        RdfUtils.removeInvalidDataCharacter(s));
    }

    /**
     * Maps an integer value.
     * @param  s              the string to parse as an integer value.
     * @param  valueFactory   the {@link ValueFactory value factory} to
     *                        use to allocate the integer literal.
     *
     * @return a {@link Literal} holding the extracted integer value.
     */
    private Literal mapInt(String s, ValueFactory valueFactory) {
        Literal v = null;
        try {
            v = valueFactory.createLiteral(
                                    Long.parseLong(this.trimNumSeparators(s)));
        }
        catch (Exception e) {
            /* Ignore... */
        }
        return v;
    }

    /**
     * Maps a floating point value.
     * @param  s              the string to parse as a floating point value.
     * @param  valueFactory   the {@link ValueFactory value factory} to
     *                        use to allocate the decimal literal.
     *
     * @return a {@link Literal} holding the extracted decimal value.
     */
    private Literal mapFloat(String s, ValueFactory valueFactory) {
        Literal v = null;
        try {
            // If the default decimal separator ('.') is absent, assume
            // the user language uses ','.
            if (s.indexOf('.') == -1) {
                int n = s.indexOf(',');
                if ((n != -1) && (n == s.lastIndexOf(','))) {
                    s.replace(',', '.');
                }
                // Else: no comma or more than one. => Ignore.
            }
            // Trim all spaces, tabs and other separator characters.
            s = this.trimNumSeparators(s);
            // Parse decimal value as a double.
            v = valueFactory.createLiteral(Double.parseDouble(s));
        }
        catch (Exception e) {
            /* Ignore... */
        }
        return v;
    }

    /**
     * Maps a boolean value, matching the values for TRUE defined in the
     * mapping descriptor.
     * @param  s              the string to parse as a boolean.
     * @param  valueFactory   the {@link ValueFactory value factory} to
     *                        use to allocate the boolean literal.
     * @param  desc           the value mappings specified by the user.
     *
     * @return a {@link Literal} holding the extracted boolean value.
     */
    private Literal mapBoolean(String s, ValueFactory valueFactory,
                                         MappingDesc desc) {
        return valueFactory.createLiteral(desc.parseBoolean(s));
    }

    /**
     * Maps a date (time or date-time) value using the date format
     * defined in the mapping descriptor.
     * @param  s              the string to parse as a date.
     * @param  valueFactory   the {@link ValueFactory value factory} to
     *                        use to allocate the date literal.
     * @param  desc           the value mappings specified by the user.
     * @param  tentative      whether this call is just an attempt to
     *                        check whether the value contains a date.
     *
     * @return a {@link Literal} holding the extracted date, time or
     *         date-time value.
     */
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

    /**
     * Maps a URI.
     * @param  s              the string to parse as a URI.
     * @param  valueFactory   the {@link ValueFactory value factory} to
     *                        use to allocate the URI.
     *
     * @return a {@link Literal} holding the extracted integer value.
     */
    private Value mapUri(String s, ValueFactory valueFactory) {
        Value v = null;
        try {
            // Use java.net.URI to ensure no illegal character is present.
            v = valueFactory.createURI(URI.create(s).toString());
        }
        catch (Exception e) {
            /* Ignore... */
        }
        return v;
    }

    /**
     * Removes the separators from a string holding a numeric value.
     * @param  s   the string to clean.
     *
     * @return the string, stripped of all separator characters.
     */
    private String trimNumSeparators(String s) {
        return (s == null)? "": SEPARATORS_PATTERN.matcher(s).replaceAll("");
    }

    //-------------------------------------------------------------------------
    // MappingDesc nested class
    //-------------------------------------------------------------------------

    /**
     * A value object describing the parameters for mapping CSV data
     * to RDF values.
     */
    private final static class MappingDesc
    {
        private final static Collection<String> DEFAULT_TRUE_VALUES =
                                    Arrays.asList("true", "yes", "oui", "1");

        private final Mapping[] mappings;
        private final int keyColumn;
        private final Collection<String> trueValues;
        private final boolean booleanValuesDefined;
        private final DateFormat dateFormat;
        private final DatatypeFactory dateFactory;

        public MappingDesc(Mapping[] typeMappings, int keyColumn,
                           String trueValues, String dateFormat) {
            if ((typeMappings == null) || (typeMappings.length == 0)) {
                throw new IllegalArgumentException("typeMappings");
            }
            // Type mappings.
            this.mappings = typeMappings;
            this.keyColumn = keyColumn;
            // Boolean TRUE values.
            this.booleanValuesDefined = isSet(trueValues);
            Collection<String> l = DEFAULT_TRUE_VALUES;
            if (this.booleanValuesDefined) {
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

        public Mapping getMapping(int column) {
            return this.mappings[column];
        }

        public boolean hasBooleanValues() {
            return ((this.booleanValuesDefined) &&
                    (! this.trueValues.isEmpty()));
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

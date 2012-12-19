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

package org.datalift.s4ac.services;

import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.BindingSet;
import org.openrdf.query.BooleanQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;

import static org.openrdf.query.QueryLanguage.SPARQL;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.security.SecurityContext;
import org.datalift.fwk.sparql.AccessController;
import org.datalift.s4ac.TechnicalException;
import org.datalift.s4ac.utils.CRUDType;
import org.datalift.s4ac.utils.QueryType;

import static org.datalift.fwk.rdf.RdfNamespace.S4AC;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.s4ac.services.ACSType.*;
import static org.datalift.s4ac.utils.QueryType.*;


/**
 * An {@link AccessController} implementation that relies of the
 * <a href="http://ns.inria.fr/s4ac">S4AC (Social Semantic SPARQL
 * Security for Access Control)</a> vocabulary for specifying the
 * access control policies.
 *
 * @author lbihanic
 */
public class S4acAccessController extends BaseModule
                                  implements AccessController
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The list of RDF stores upon which access control policies apply. */
    public final static String SECURED_REPOSITORIES     =
                                        "sparql.secured.repositories";
    /** The name of the RDF store to read access control policies from. */
    public final static String SECURITY_REPOSITORY_URI  =
                                        "sparql.security.repository";
    /** A list of files to load access control policies from. */
    public final static String POLICY_FILES = "sparql.security.policy.files";
    /** The base URI for building user context URIs. */
    public final static String USER_CONTEXT_PROPERTY    =
                                        "sparql.security.user.context";
    /** The RDF store to execute access control ASK queries against. */
    public final static String USER_REPOSITORY_PROPERTY =
                                        "sparql.security.user.repository.uri";
    /** The default base URI for user contexts. */
    public final static String DEFAULT_USER_CONTEXT     =
                                        "http://example.com/context/{0}";
    /** The URI of the security repository when none is specified. */
    public final static String ANON_SECURITY_REPOSITORY_URI = "$security";

    private final static String ACCESS_POLICIES_QUERY =
            "PREFIX s4ac: <" + S4AC.uri + ">\n" +
            "SELECT * WHERE { " +
                "?ap  a s4ac:AccessPolicy ; " +
                "     s4ac:appliesTo ?graph ; " +
                "     s4ac:hasAccessConditionSet ?acs ; " +
                "     s4ac:hasAccessPrivilege ?privilege . " +
                "?acs a ?acstype ; " +
                "     s4ac:hasAccessCondition ?ac . " +
                "?ac  s4ac:hasQueryAsk ?ask . " +
                "FILTER(?acstype = <" + CONJUNCTIVE + "> || " +
                       "?acstype = <" + DISJUNCTIVE + ">) }";
    private final static String ALL_GRAPHS_QUERY =
            "SELECT DISTINCT ?g WHERE { GRAPH ?g { ?s ?p ?o. }}";

    private final static Pattern QUERY_TYPE_PATTERN = Pattern.compile(
            "(^|\\s)(" + ASK.name() + '|' + CONSTRUCT.name() + '|' +
                         DESCRIBE.name() + '|' + SELECT.name() + ")\\s",
            Pattern.CASE_INSENSITIVE);

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private static final Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final Set<String> securedRepositories = new HashSet<String>();
    private final Map<String,AccessPolicy> policies =
                                            new HashMap<String,AccessPolicy>();
    private final Set<String> protectedGraphs = new HashSet<String>();
    private final ConcurrentMap<String,PublicGraphs> publicGraphs =
                                new ConcurrentHashMap<String,PublicGraphs>();

    private Repository securityRepository;
    private Repository userRepository;
    private MessageFormat userContext;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public S4acAccessController() {
        super(S4acAccessController.class.getSimpleName());
    }

    //-------------------------------------------------------------------------
    // BaseModule contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(Configuration configuration) {
        // Check if some RDF stores are marked as secured.
        this.securedRepositories.clear();
        String securedRepNames = configuration.getProperty(SECURED_REPOSITORIES);
        if (! isBlank(securedRepNames)) {
            this.securedRepositories.addAll(
                            Arrays.asList(securedRepNames.split("\\s*,\\s*")));
        }
        // Retrieve the user context base URI.
        String ctx = RdfUtils.getBaseUri(
                            configuration.getProperty(USER_CONTEXT_PROPERTY));
        if (isBlank(ctx)) {
            ctx = DEFAULT_USER_CONTEXT;
            if (! this.securedRepositories.isEmpty()) {
                log.warn("User context base URI not set. Using default: <{}>",
                         ctx);
            }
        }
        this.userContext = new MessageFormat(ctx);
        // Connect to or initialize security repository.
        try {
            // Get the policy files to populate the security repository from.
            String policyFiles = configuration.getProperty(POLICY_FILES);
            // Load access control restrictions from security RDF store.
            String securityRepositoryUri = configuration.getProperty(
                                                    SECURITY_REPOSITORY_URI);
            if (securityRepositoryUri != null) {
                // Security repository configured in configuration.
                this.securityRepository = configuration.newRepository(
                                            securityRepositoryUri, null, false);
            }
            else if (! isBlank(policyFiles)) {
                // Create an in-memory repository.
                this.securityRepository = configuration.newRepository(
                            ANON_SECURITY_REPOSITORY_URI, "sail:///", false);
            }
            // Else: No security repository!

            // Load access control policies in security repository.
            if ((this.securityRepository != null) && (! isBlank(policyFiles))) {
                for (String f : policyFiles.split("\\s*,\\s*")) {
                    log.debug("Loading policy file {}...", f);
                    RdfUtils.upload(new File(f), this.securityRepository, null);
                }
            }
        }
        catch (Exception e) {
            throw new TechnicalException("security.repository.acces.error", e);
        }

        if (this.securityRepository != null) {
            // Get the name of the repository against which resolving
            // access control policy ASK queries.
            String userRepositoryUri = configuration.getProperty(
                                                    USER_REPOSITORY_PROPERTY);
            if (userRepositoryUri != null) {
                if (userRepositoryUri.equals(this.securityRepository.name)) {
                    this.userRepository = this.securityRepository;
                    log.info("Resolving access policy ASK queries against security RDF store.");
                }
                else {
                    this.userRepository = configuration.getRepository(
                                                            userRepositoryUri);
                    log.info("Resolving access policy ASK queries against RDF store \"{}\"",
                             this.userRepository.name);
                }
            }
            // Extract access control policies.
            RepositoryConnection cnx = null;
            try {
                this.policies.clear();
                cnx = this.securityRepository.newConnection();
                TupleQueryResult result =
                        cnx.prepareTupleQuery(SPARQL, ACCESS_POLICIES_QUERY)
                           .evaluate();
                while (result.hasNext()) {
                    BindingSet bs = result.next();
                    String uri = bs.getValue("ap").stringValue();
                    AccessPolicy ap = this.policies.get(uri);
                    if (ap == null) {
                        String acstype = bs.getValue("acstype").stringValue();
                        ap = new AccessPolicy(uri, ACSType.get(acstype));
                        this.policies.put(uri, ap);
                    }
                    ap.addGraph(bs.getValue("graph").stringValue());
                    ap.addAsk(bs.getValue("ask").stringValue());
                    ap.addPrivilege(CRUDType.get(
                                    bs.getValue("privilege").stringValue()));
                }
                result.close();

                if (log.isDebugEnabled()) {
                    log.debug("Loaded access policies: {}",
                                                    this.policies.values());
                }
            }
            catch (Exception e) {
                throw new TechnicalException("access.policy.load.error", e);
            }
            finally {
                if (cnx != null) {
                    try { cnx.close(); } catch (Exception e) { /* Ignore ... */ }
                }
            }
            // Check that secured repositories are indeed protected.
            if ((! this.securedRepositories.isEmpty()) &&
                (this.policies.isEmpty())) {
                throw new TechnicalException("access.policies.not.found");
            }
            else {
                log.info("Found {} access policies applicable to RDF stores {}",
                         Integer.valueOf(this.policies.size()),
                         this.securedRepositories);
            }
            // Check that there's at least one repository under access control.
            if (this.securedRepositories.isEmpty()) {
                log.warn("{} configuration parameter not set. " +
                     "Access control policies not applicable to any RDF store.",
                     SECURED_REPOSITORIES);
            }
            // Register all protected named graphs.
            for (AccessPolicy ap : this.policies.values()){
                this.protectedGraphs.addAll(ap.getGraphs());
            }
        }
        else {
            // No security repository.
            log.warn("S4AC deactivated: no security repository defined in configuration.");
        }
    }

    //-------------------------------------------------------------------------
    // AccessController contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean isSecured(Repository repository) {
        return this.securedRepositories.contains(repository.name);
    }

    /** {@inheritDoc} */
    @Override
    public ControlledQuery checkQuery(String query, Repository repository,
                                      List<String> defaultGraphUris,
                                      List<String> namedGraphUris)
                                                    throws SecurityException {
        Set<String> graphs = null;
        // Parse query to extract query type.
        QueryType type = this.getQueryType(query);

        if (this.isSecured(repository)) {
            // Get accessible graphs for the currently logged user.
            String user = SecurityContext.getUserPrincipal();
            if (user != null) {
                graphs = this.evaluatePolicies(user, type,
                                this.getPolicyEvaluationRepository(repository));
            }
            // Public graphs, i.e. the ones upon which there are no access
            // control policies are accessible to anyone.
            graphs.addAll(this.getPublicGraphs(repository));
            if (log.isTraceEnabled()) {
                log.trace("Graphs accessible to {}: {}", user, graphs);
            }
            else {
                log.debug("Found {} graph(s) accessible to {}",
                          Integer.valueOf(graphs.size()), user);
            }
            if (graphs.isEmpty()) {
                // User is not allowed to access any content.
                throw new SecurityException();
            }
            // Update query and graphs data.
            Set<String> filteredNamedGraphs;
            if ((namedGraphUris == null) || (namedGraphUris.isEmpty())) {
                filteredNamedGraphs = graphs;
            }
            else {
                filteredNamedGraphs = new HashSet<String>();
                filteredNamedGraphs.addAll(namedGraphUris);
                filteredNamedGraphs.retainAll(graphs);
            }
            namedGraphUris = new LinkedList<String>(filteredNamedGraphs);
            query = this.restrictQuery(query, filteredNamedGraphs, type);
        }
        // Else: not a secured RDF store.

        return new ControlledQuery(query, type.name(),
                                   defaultGraphUris, namedGraphUris, graphs);
    }

    private Repository getPolicyEvaluationRepository(Repository target) {
        return (this.userRepository != null)? this.userRepository: target;
    }

    private Set<String> evaluatePolicies(String user,
                                         QueryType type, Repository r) {
        Set<String> graphs = new HashSet<String>();

        RepositoryConnection cnx = null;
        try {
            cnx = r.newConnection();

            for (AccessPolicy ap : this.policies.values()) {
                // Check that query CRUD type is allowed.
                if (! ap.hasPrivileges(type.crudType)) continue;
                // Resolve accessible graphs.
                try {
                    boolean isOk = true;
                    if (ap.getAcstype() == ACSType.CONJUNCTIVE) {
                        // set conjonctif, tous les asks doivent etre valides
                        for (String askQuery : ap.getAsks()) {
                            isOk = isOk && this.ask(cnx, askQuery, user);
                            if (isOk == false) break;
                        }
                    }
                    else if (ap.getAcstype() == ACSType.DISJUNCTIVE) {
                        // set disjonjonctif, au moins un ask doit etre valide
                        isOk = false;
                        for (String askQuery : ap.getAsks()) {
                            if (this.ask(cnx, askQuery, user)) {
                                isOk = true;
                                break;
                            }
                        }
                    }
                    if (isOk == true) {
                        log.debug("Access granted for policy <{}>", ap.uri);
                        graphs.addAll(ap.getGraphs());
                    }
                }
                catch (Exception e) {
                    log.fatal("Error evaluating policy <{}>. Denying access.",
                              e, ap.uri);
                    try { cnx.close(); } catch (Exception e1) { /* Ignore... */ }
                    cnx = r.newConnection();
                }
            }
        }
        catch (Exception e) {
            // Ignore and deny any further accesses.
        }
        finally {
            if (cnx != null) {
                try { cnx.close(); } catch (Exception e1) { /* Ignore... */ }
            }
        }
        return graphs;
    }

    private boolean ask(RepositoryConnection cnx, String query, String user) {
        boolean matches = false;
        try {
            BooleanQuery q = cnx.prepareBooleanQuery(SPARQL, query);
            if (isSet(user)) {
                q.setBinding("context", new URIImpl(
                        this.userContext.format(new Object[] { user })));
            }
            matches = q.evaluate();
            log.trace("{} -> {}", query, Boolean.valueOf(matches));
        }
        catch (Exception e) {
            throw new TechnicalException(query, e);
        }
        return matches;
    }

    private String restrictQuery(String query,
                                 Set<String> graphs, QueryType type) {
        StringBuilder from = new StringBuilder(256);
        if (graphs != null) {
            for (String g : graphs){
                from.append("\nFROM <").append(g).append('>');
            }
            from.append('\n');
        }
        if (from.length() != 0) {
            // Search for first WHERE clause.
            int idx1 = query.indexOf("WHERE");
            int idx2 = query.indexOf("where");
            int idx  = (idx1 == -1)? idx2:
                       (idx2 == -1)? idx1: Math.min(idx1, idx2);
            if (idx == -1) {
                // No WHERE clause found.
                if ((type == ASK) || (type == SELECT)) {
                    idx = query.indexOf('{');
                }
                else if (type == CONSTRUCT) {
                    // Look for second '{', the first marking
                    // the triples to construct.
                    idx = query.indexOf('{', Math.max(query.indexOf('{'), 0) + 1);
                }
                // Else: DESCRIBE.

                if (idx == -1) {
                    idx = query.length();       // Append at end.
                }
            }
            if (idx != -1) {
                query = query.substring(0, idx) + from + query.substring(idx);
            }
        }
        return query;
    }

    private QueryType getQueryType(String query) {
        QueryType type = null;
        Matcher m = QUERY_TYPE_PATTERN.matcher(query);
        if (m.find()) {
            String t = m.group(2).toUpperCase();
            type = (CONSTRUCT.name().equals(t))? CONSTRUCT:
                   (ASK.name().equals(t))?       ASK:
                   (DESCRIBE.name().equals(t))?  DESCRIBE:
                   (SELECT.name().equals(t))?    SELECT: UNKNOWN;
        }
        return type;
    }

    private Set<String> getPublicGraphs(Repository r) {
        PublicGraphs g = this.publicGraphs.get(r.name);
        if (g == null) {
            g = new PublicGraphs(3600);         // 1 hour.
            this.publicGraphs.put(r.name, g);
        }
        if (g.needsUpdate()) {
            try {
                Set<String> graphs = this.getAllGraphs(r);
                graphs.removeAll(this.protectedGraphs);
                g.update(graphs);
            }
            catch (Exception e) { /* Ignore and try again later... */ }
        }
        return g.getPublicGraphs();
    }

    private Set<String> getAllGraphs(Repository r) {
       Set<String> graphs = new HashSet<String>();

       RepositoryConnection cnx = null;
       try {
           cnx = r.newConnection();
           TupleQueryResult result =
                               cnx.prepareTupleQuery(SPARQL, ALL_GRAPHS_QUERY)
                                  .evaluate();
           while (result.hasNext()) {
               graphs.add(result.next().getValue("g").stringValue());
           }
       }
       catch (Exception e) {
           throw new TechnicalException(e);
       }
       return graphs;
    }

    //-------------------------------------------------------------------------
    // PublicGraphs nested class
    //-------------------------------------------------------------------------

    private final static class PublicGraphs
    {
        public final long cacheDuration;
        private final Set<String> publicGraphs = new HashSet<String>();
        private long nextRefresh = 0L;

        public PublicGraphs(int cacheDuration) {
            this.cacheDuration = (cacheDuration >= 0)? cacheDuration * 1000L:
                                                       -1L;
        }

        public boolean needsUpdate() {
            return ((this.cacheDuration != -1L) &&
                    (this.nextRefresh < System.currentTimeMillis()));
        }
        public Set<String> getPublicGraphs() {
            return Collections.unmodifiableSet(this.publicGraphs);
        }

        public void update(Set<String> graphs) {
            if (this.cacheDuration != -1L) {
                this.nextRefresh = System.currentTimeMillis() +
                                                            this.cacheDuration;
            }
            this.publicGraphs.clear();
            if (graphs != null) {
                this.publicGraphs.addAll(graphs);
            }
        }
    }
}

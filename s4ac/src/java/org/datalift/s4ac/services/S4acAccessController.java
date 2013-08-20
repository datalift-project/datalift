/*
 * Contact: serena.villata@inria.fr
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 */

package org.datalift.s4ac.services;


import java.io.File;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Collection;
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
import static org.datalift.fwk.util.PrimitiveUtils.wrap;
import static org.datalift.fwk.util.StringUtils.*;
import static org.datalift.s4ac.services.ACSType.*;
import static org.datalift.s4ac.utils.QueryType.*;


/**
 * An {@link AccessController} implementation that relies of the
 * <a href="http://ns.inria.fr/s4ac">S4AC (Social Semantic SPARQL
 * Security for Access Control)</a> vocabulary for specifying the
 * access control policies.
 *
 * @author Serena Villata (INRIA - Sophia-Antipolis)
 * @author Laurent Bihanic (Atos)
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
        String ctx = configuration.getProperty(USER_CONTEXT_PROPERTY);
        if (isBlank(ctx)) {
            ctx = DEFAULT_USER_CONTEXT;
            if (! this.securedRepositories.isEmpty()) {
                log.warn("User context base URI not set. Using default: <{}>",
                         ctx);
            }
        }
        this.userContext = new MessageFormat(ctx);
        // Get the policy files to populate the security repository from.
        String policyFiles = configuration.getProperty(POLICY_FILES);
        // Connect to or initialize security repository.
        try {
            // Load access control restrictions from security RDF store.
            String securityRepositoryUri = configuration.getProperty(
                                                    SECURITY_REPOSITORY_URI);
            if (securityRepositoryUri != null) {
                // Security repository configured in configuration.
                try {
                    // Assume the URI is the name of a configured repository.
                    this.securityRepository = configuration.newRepository(
                                            securityRepositoryUri, null, false);
                }
                catch (IllegalArgumentException e) {
                    // Repository name not declared in configuration.
                    // => Retry assuming the URI is a connection string.
                    this.securityRepository = configuration.newRepository(
                                            null, securityRepositoryUri, false);
                }
            }
            else if (! isBlank(policyFiles)) {
                // Create an in-memory repository to load the access control
                // policies defined in local RDF files.
                this.securityRepository = configuration.newRepository(
                            ANON_SECURITY_REPOSITORY_URI, "sail:///", false);
            }
            // Else: No security repository!
        }
        catch (Exception e) {
            throw new TechnicalException("security.repository.acces.error", e);
        }
        // Load access control policies from security repository.
        if (this.securityRepository != null) {
            // Get the name of the repository against which resolving
            // access control policy ASK queries.
            final String policyQueryStoreMsg =
                                "Resolving access policy ASK queries against ";
            String userRepositoryUri = configuration.getProperty(
                                                    USER_REPOSITORY_PROPERTY);
            if (userRepositoryUri != null) {
                if (userRepositoryUri.equals(this.securityRepository.name)) {
                    this.userRepository = this.securityRepository;
                    log.info(policyQueryStoreMsg + "security RDF store");
                }
                else {
                    this.userRepository = configuration.getRepository(
                                                            userRepositoryUri);
                    log.info(policyQueryStoreMsg + "RDF store \"{}\"",
                             this.userRepository.name);
                }
            }
            else {
                log.info(policyQueryStoreMsg + "query target RDF store");
            }
            // Load access control policy files (if any) in security repository.
            if (! isBlank(policyFiles)) {
                this.loadPolicyFiles(
                                Arrays.asList(policyFiles.split("\\s*,\\s*")));
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
            // Check that there's at least one repository under access control.
            if (this.securedRepositories.isEmpty()) {
                log.warn("{} configuration parameter not set. " +
                     "Access control policies not applicable to any RDF store.",
                     SECURED_REPOSITORIES);
            }
            else {
                // Check that secured repositories are indeed protected.
                if (this.policies.isEmpty()) {
                    throw new TechnicalException("access.policies.not.found");
                }
                else {
                    log.info("Found {} access policies applicable to RDF stores {}",
                         wrap(this.policies.size()), this.securedRepositories);
                }
                // Register all protected named graphs.
                for (AccessPolicy ap : this.policies.values()){
                    this.protectedGraphs.addAll(ap.getGraphs());
                }
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
                                                    wrap(graphs.size()), user);
            }
            if (graphs.isEmpty()) {
                // User is not allowed to access any content.
                log.info("Denied access to RDF store \"{}\" for \"{}\"",
                         repository, user);
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

    /** {@inheritDoc} */
    @Override
    public void refresh() {
        // Invalidate all public graph caches.
        for (PublicGraphs g : this.publicGraphs.values()) {
            g.invalidate();
        }
        log.debug("Public graph cache refreshed");
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

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
                        // Conjunctive set: all ASKs must be valid.
                        for (String askQuery : ap.getAsks()) {
                            isOk = isOk && this.ask(cnx, askQuery, user);
                            if (isOk == false) break;
                        }
                    }
                    else if (ap.getAcstype() == ACSType.DISJUNCTIVE) {
                        // Disjunctive set: at least one ASK shall be valid.
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
                q.setBinding(USER_CONTEXT_VARIABLE, new URIImpl(
                            this.userContext.format(new Object[] { user })));
                
                log.debug("Added Context for user {}", user);
            }
            matches = q.evaluate();
            if (log.isTraceEnabled()) {
                log.trace("{} ({}) -> {}", query, q.getBindings(),
                                                  wrap(matches));
            }
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

    private int loadPolicyFiles(Collection<String> policyFiles) {
        int loadedFiles = 0;
        for (String f : policyFiles) {
            if (isBlank(f)) continue;
            log.debug("Loading policy file \"{}\" into {}...",
                                                    f, this.securityRepository);
            try {
                RdfUtils.upload(new File(f), this.securityRepository, null);
                loadedFiles++;
            }
            catch (IllegalArgumentException e) {
                // Log error and continue processing policy files.
                TechnicalException error = new TechnicalException(
                        "policy.file.access.error", f, e.getLocalizedMessage());
                log.error(error.getLocalizedMessage(), e);
            }
            catch (Exception e) {
                // Log error and continue processing policy files.
                TechnicalException error = new TechnicalException(
                        "policy.file.load.error", f, e.getLocalizedMessage());
                log.error(error.getLocalizedMessage(), e);
            }
        }
        return loadedFiles;
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

        public void invalidate() {
            this.nextRefresh = 0L;
        }
    }
}

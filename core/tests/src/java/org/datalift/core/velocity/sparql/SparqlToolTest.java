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

package org.datalift.core.velocity.sparql;


import java.io.StringWriter;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.log.Log4JLogChute;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.apache.velocity.runtime.resource.util.StringResourceRepositoryImpl;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.impl.LiteralImpl;
import org.openrdf.model.impl.StatementImpl;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.repository.RepositoryConnection;

import static org.apache.velocity.runtime.RuntimeConstants.*;
import static org.apache.velocity.runtime.log.Log4JLogChute.RUNTIME_LOG_LOG4J_LOGGER;
import static org.junit.Assert.*;

import org.datalift.core.DefaultConfiguration;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;

import static org.datalift.core.DefaultConfiguration.*;
import static org.datalift.fwk.rdf.RdfNamespace.DC_Elements;


@RunWith(JUnit4.class)
public class SparqlToolTest
{
    private final static String DC = DC_Elements.uri;
    private final static String DT = "http://www.datalift.org/test/";

    private final static String SUBJECT1 = DT + "1";
    private final static String SUBJECT2 = DT + "2";

    private final static String RDF_STORE = "test";

    private static Logger log;

    private static VelocityEngine engine = null;

    @BeforeClass
    public static void setUpClass() {
        // Configure Datalift with an in-memory RDF store.
        Properties config = new Properties();
        config.put(REPOSITORY_URIS, RDF_STORE);
        config.put(RDF_STORE + REPOSITORY_URL, "sail:///");
        config.put(RDF_STORE + REPOSITORY_DEFAULT_FLAG, "true");
        config.put(PRIVATE_STORAGE_PATH, ".");
        DefaultConfiguration cfg = new DefaultConfiguration(config);
        Configuration.setDefault(cfg);
        cfg.init();
        // Populate Datalift default RDF store with test data.
        populate(cfg.getDefaultRepository());

        log = Logger.getLogger();

        // Configure Velocity engine.
        config = new Properties();
        // Load templates from in-memory strings.
        config.setProperty(RESOURCE_LOADER, "string");
        config.setProperty("string.resource.loader.class",
                           StringResourceLoader.class.getName());
        config.setProperty("string.resource.loader.repository.class",
                           StringResourceRepositoryImpl.class.getName());
        // Log to Log4J.
        config.setProperty(RUNTIME_LOG_LOGSYSTEM_CLASS,
                           Log4JLogChute.class.getName());
        config.setProperty(RUNTIME_LOG_LOG4J_LOGGER, "org.apache.velocity");
        // Start engine.
        engine = new VelocityEngine(config);
        engine.init();
    }

    @Test
    public void simpleAskTest() {
        String query = "ASK { ?s a ?type . }";
        // Do not register any prefix for DC Elements namespace as
        // bindUri() should resolve it from the well-known namespaces
        // (from RdfNamespace) and it's not used elsewhere in the query.
        String template =
                "#if($type)$sparql.bindUri('type',$type)#end" +
                "#if ($sparql.ask('" + query + "'))" +
                "Found!" +
                "#end";
        String page = this.render(null, template);
        log.debug("{} -> {}", query, page);
        assertEquals("Found!", page);

        Map<String,Object> ctx = new HashMap<String,Object>();
        ctx.put("type", "dc:Agent");
        page = this.render(ctx, template);
        log.debug("{} -> \"{}\"", query, page);
        assertEquals("", page);
    }

    @Test
    public void simpleSelectTest() {
        Map<String,Object> ctx = new HashMap<String,Object>();
        List<String> results = new LinkedList<String>();
        ctx.put("c", results);
        ctx.put("q", "SELECT * WHERE { ?s ?p ?o . }");
        String page = this.render(ctx,
                "#foreach($i in $sparql.select($q))\n" +
                "#set($l = \"<$i['s']> <$i['p']> $i['o']\")" +
                "#set($x = $c.add($l))"+
                "$l\n" +
                "#end");
        log.debug("{} ->\n{}", ctx.get("q"), page);
        assertEquals(9, results.size());
    }

    @Test
    public void missingVarSelectTest() {
        Map<String,Object> ctx = new HashMap<String,Object>();
        List<String> results = new LinkedList<String>();
        ctx.put("c", results);
        ctx.put("q", "SELECT * WHERE { ?s a ?t ." +
                                     " OPTIONAL { ?s dc:date ?d . } }");
        ctx.put("u", java.net.URI.create(SUBJECT1));
        String page = this.render(ctx,
                "$sparql.prefix('dc', '" + DC + "')" +
                "$sparql.bind('s', $u)" +
                "#foreach($i in $sparql.select($q))\n" +
                "#set($l = \"<$i['t']> $i['d']\")" +
                "#set($x = $c.add($i['d']))"+
                "$l\n" +
                "#end");
        log.debug("{} ->\n{}", ctx.get("q"), page);
        assertEquals(1, results.size());
        assertNull(results.iterator().next());
    }
    @Test
    public void simpleConstructTest() {
        Map<String,Object> ctx = new HashMap<String,Object>();
        List<Statement> results = new LinkedList<Statement>();
        ctx.put("c", results);
        ctx.put("q", "CONSTRUCT { ?s a dc:Agent . } WHERE { ?s a ?c . }");
        String page = this.render(ctx,
                "$sparql.prefix('dc', '" + DC + "')" +
                "#foreach($i in $sparql.construct($q))" +
                "#set($x = $c.add($i))"+
                "$i\n" +
                "#end");
        log.debug("{} ->\n{}", ctx.get("q"), page);
        assertEquals(2, results.size());
    }

    @Test
    public void describeTest() {
        Map<String,Object> ctx = new HashMap<String,Object>();
        Map<String,Value> results = new HashMap<String,Value>();
        ctx.put("c", results);
        ctx.put("u", java.net.URI.create(SUBJECT1));
        // Do not register any prefix for DC Elements namespace as it's a
        // well-known namespace (from RdfNamespace) and it's not used in the
        // SPARQL query.
        String page = render(ctx,
                "#set($r = $sparql.describe($u))" +
                "$c.put('type', $r.valueOf('a')))"+
                "$c.put('title', $r.valueOf('dc:title')))"+
                "$c.put('description', $r.valueOf('dc:description')))"+
                "$c.put('creator', $r.valueOf('dc:creator')))" +
                "$c.put('notFound', $r.valueOf('dc:unknown')))" +
                "$c.put('creatorOf', $r.resultsFor('" + SUBJECT2 + "').valueOf('dc:creator')))");
        log.trace("{} ->\n{}", SUBJECT1, page);
        assertEquals(DT + "Sample",     results.get("type"));
        assertEquals("sample1",         results.get("title"));
        assertEquals("First test item", results.get("description"));
        assertEquals(SparqlToolTest.class.getSimpleName(),
                                        results.get("creator"));
        assertEquals("",       results.get("notFound"));
        assertEquals(SUBJECT1, results.get("creatorOf"));
    }

    private String render(Map<String,Object> ctx, String template) {
        StringWriter w = new StringWriter(1024);
        // Register template.
        final String KEY = "template";
        StringResourceRepository repo = StringResourceLoader.getRepository();
        repo.putStringResource(KEY, template);
        // Initialize context.
        VelocityContext c = new VelocityContext(ctx);
        c.put("sparql", new SparqlTool());
        // Render template.
        engine.getTemplate(KEY).merge(c, w);
        // Remove template.
        repo.removeStringResource(KEY);

        return w.toString();
    }

    private static void populate(Repository r) {
        RepositoryConnection cnx = r.newConnection();
        try {
            String s = SUBJECT1;
            cnx.add(triple(s, RDF.TYPE, new URIImpl(DT + "Sample")));
            cnx.add(triple(s, DC + "title", "sample1"));
            cnx.add(triple(s, DC + "description", "First test item"));
            cnx.add(triple(s, DC + "creator", SparqlToolTest.class.getSimpleName()));
            s = SUBJECT2;
            cnx.add(triple(s, RDF.TYPE, new URIImpl(DT + "Sample")));
            cnx.add(triple(s, DC + "title", "sample2"));
            cnx.add(triple(s, DC + "description", "Second test item"));
            cnx.add(triple(s, DC + "creator", SparqlToolTest.class.getSimpleName()));
            cnx.add(triple(s, DC + "creator", new URIImpl(SUBJECT1)));
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
        finally {
            Repository.closeQuietly(cnx);
        }
    }

    private static Statement triple(String s, String p, String o) {
        return triple(s, p, new LiteralImpl(o));
    }
    private static Statement triple(String s, String p, Value v) {
        return triple(s, new URIImpl(p), v);
    }
    private static Statement triple(String s, URI p, Value v) {
        return new StatementImpl(new URIImpl(s), p, v);
    }
}

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

package org.datalift.sparql.query;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

import org.datalift.fwk.util.StringUtils;


public class VCardMapping
{
    public final static String VCARD = "http://www.w3.org/2006/vcard/ns#";

    private final static Collection<String> VCARD_PREDICATES =
                                                    Arrays.asList("fn", "adr");
    private final static Collection<String> ADDRESS_PREDICATES =
                                Arrays.asList("street-address", "postal-code",
                                              "locality", "country-name");

    private String varName;
    private final URI srcGraph;
    private final Map<String,String> nsMapping = new HashMap<String,String>();

    public VCardMapping() {
        this(null, null, null);
    }

    public VCardMapping(String srcNs) {
        this(null, srcNs, null);
    }

    public VCardMapping(String varName, String srcNs) {
        this(varName, srcNs, null);
    }

    public VCardMapping(String varName, String srcNs, String srcGraph) {
        this.varName  = varName;
        this.srcGraph = (StringUtils.isSet(srcGraph))? new URIImpl(srcGraph): null;
        if (StringUtils.isSet(srcNs)) {
            this.nsMapping.put("src", srcNs);
        }
    }

    public UpdateQuery map(UpdateQuery query, Map<String,String> mapping) {
        return this.map(query, null, mapping);
    }

    public UpdateQuery map(UpdateQuery query, Resource vcard,
                                              Map<String,String> mapping) {
        vcard = this.mapVCard(query, vcard, mapping);
        this.mapAddress(query, vcard, "Work", mapping);
        return query;
    }

    public Resource mapVCard(UpdateQuery query, Resource vcard,
                                                Map<String,String> mapping) {
        // Ensure query holds all needed RDF namespace prefixes.
        this.addPrefixes(query);
        // If no target node is specified, use default.
        if (vcard == null) {
            vcard = query.variable(this.varName);
        }
        // Map VCard predicates for the subject node.
        query.rdfType(vcard, query.uri(VCARD, "VCard"))
             .map(this.srcGraph, vcard,
                  this.getValues(mapping, query, VCARD_PREDICATES));
        // Return the SPARQL variable associated to the VCard node in query.
        return vcard;
    }

    public Resource mapAddress(UpdateQuery query, Resource vcard,
                               String addrType, Map<String,String> mapping) {
        // Ensure query holds all needed RDF namespace prefixes.
        this.addPrefixes(query);
        // If no target node is specified, use default.
        if (vcard == null) {
            vcard = query.variable(this.varName);
        }
        // As per the VCard ontology, use a blank node to hold the address data.
        Resource addr = query.blankNode();
        query.rdfType(addr, query.uri(VCARD, addrType))
             .triple(vcard, query.uri(VCARD, "adr"), addr)
             .map(this.srcGraph, vcard, addr,
                  this.getValues(mapping, query, ADDRESS_PREDICATES));
        // Return the blank node associated to this VCard address in query.
        return addr;
    }

    public VCardMapping addPrefix(String prefix, String uri) {
        this.nsMapping.put(prefix, uri);
        return this;
    }

    private final UpdateQuery addPrefixes(UpdateQuery query) {
        query.prefix("v", VCARD);
        for (Entry<String,String> e : this.nsMapping.entrySet()) {
            query.prefix(e.getKey(), e.getValue());
        }
        return query;
    }

    private final Map<URI,String> getValues(Map<String,String> mapping,
                                            UpdateQuery query,
                                            Collection<String> predicates) {
        Map<URI,String> m = new LinkedHashMap<URI,String>();
        String prefix = query.prefixFor(VCARD) + ":";
        for (String p : predicates) {
            // VCard predicate absolute URI.
            URI u = query.uri(VCARD, p);
            // Search for predicate in source mapping using its
            // simple name (p), prefixed name and absolute URI.
            this.add(u, m, p, mapping);
            this.add(u, m, prefix + p, mapping);
            this.add(u, m, u.toString(), mapping);
        }
        return m;
    }

    private final boolean add(URI predicate, Map<URI,String> to,
                              String key,    Map<String,String> from) {
        boolean added = false;
        String v = from.get(key);
        if (v != null) {
            to.put(predicate, v);
            added = true;
        }
        return added;
    }
}

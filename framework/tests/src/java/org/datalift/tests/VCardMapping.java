package org.datalift.tests;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.openrdf.model.Resource;
import org.openrdf.model.URI;

import org.datalift.fwk.util.StringUtils;


public class VCardMapping
{
    public final static String VCARD = "http://www.w3.org/2006/vcard/ns#";

    private final static Collection<String> VCARD_PREDICATES =
                Arrays.asList("fn", "adr");
    private final static Collection<String> ADDRESS_PREDICATES =
                Arrays.asList("street-address", "postal-code", "locality",
                              "country-name");

    private final String varName;
    private final Map<String,String> nsMapping = new HashMap<String,String>();

    public VCardMapping(String srcNs) {
        this("s", srcNs);
    }

    public VCardMapping(String varName, String srcNs) {
        this.varName = varName;
        if (StringUtils.isSet(srcNs)) {
            this.nsMapping.put("src", srcNs);
        }
    }

    public UpdateQuery map(UpdateQuery query, Map<String,String> mapping) {
        this.mapVCard(query, mapping);
        this.mapAddress(query, "Work", mapping);
        return query;
    }

    public Resource mapVCard(UpdateQuery query, Map<String,String> mapping) {
        this.addPrefixes(query);

        Variable node = query.variable(this.varName);
        query.rdfType(node, query.uri(VCARD, "VCard"))
             .addStatements(node, this.mapPredicates(mapping, query,
                                                     VCARD_PREDICATES));

        return node;
    }

    public Resource mapAddress(UpdateQuery query, String addrType,
                                                 Map<String,String> mapping) {
        this.addPrefixes(query);

        Variable node = query.variable(this.varName);
        Resource addr = query.blankNode();

        query.rdfType(addr, query.uri(VCARD, addrType))
             .addTriple(node, query.uri(VCARD, "adr"), addr)
             .addStatements(node, addr, this.mapPredicates(mapping, query,
                                                           ADDRESS_PREDICATES));
        return addr;
    }

    public VCardMapping addPrefix(String prefix, String uri) {
        this.nsMapping.put(prefix, uri);
        return this;
    }

    private final UpdateQuery addPrefixes(UpdateQuery query) {
        query.addPrefix("v", VCARD);
        for (Entry<String,String> e : this.nsMapping.entrySet()) {
            query.addPrefix(e.getKey(), e.getValue());
        }
        return query;
    }

    private final Map<URI,String> mapPredicates(Map<String,String> mapping,
                                UpdateQuery query, Collection<String> filter) {
        Map<URI,String> m = new HashMap<URI,String>();
        String prefix = query.prefixFor(VCARD) + ":";
        for (String p : filter) {
            URI u = query.uri(VCARD, p);
            this.addIfFound(mapping, p, m, u);
            this.addIfFound(mapping, prefix + p, m, u);
            this.addIfFound(mapping, VCARD  + p, m, u);
        }
        return m;
    }

    private final void addIfFound(Map<String,String> src, String key,
                                  Map<URI,String> dest, URI destKey) {
        String v = src.get(key);
        if (v != null) {
            dest.put(destKey, v);
        }
    }
}

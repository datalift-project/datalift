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

package org.datalift.tests;


import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
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
                Arrays.asList("street-address", "postal-code", "locality",
                              "country-name");

    private String varName;
    private final URI srcGraph;
    private final Map<String,String> nsMapping = new HashMap<String,String>();

    public VCardMapping(String srcNs) {
        this("s", srcNs);
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
        this.mapVCard(query, mapping);
        this.mapAddress(query, "Work", mapping);
        return query;
    }

    public Resource mapVCard(UpdateQuery query, Map<String,String> mapping) {
        this.addPrefixes(query);

        Variable node = this.getSubject(query);
        query.rdfType(node, query.uri(VCARD, "VCard"))
             .addStatements(node, this.srcGraph, node,
                        this.mapPredicates(mapping, query, VCARD_PREDICATES));

        return node;
    }

    public Resource mapAddress(UpdateQuery query, String addrType,
                                                 Map<String,String> mapping) {
        this.addPrefixes(query);

        Variable node = this.getSubject(query);
        Resource addr = query.blankNode();

        query.rdfType(addr, query.uri(VCARD, addrType))
             .addTriple(node, query.uri(VCARD, "adr"), addr)
             .addStatements(node, this.srcGraph, addr,
                        this.mapPredicates(mapping, query, ADDRESS_PREDICATES));
        return addr;
    }

    public VCardMapping addPrefix(String prefix, String uri) {
        this.nsMapping.put(prefix, uri);
        return this;
    }

    private final Variable getSubject(UpdateQuery query) {
        Variable node = query.variable(this.varName);
        if (this.varName == null) {
            this.varName = node.stringValue();
        }
        return node;
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

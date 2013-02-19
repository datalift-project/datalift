package org.datalift.s4ac.services;


import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.datalift.fwk.util.StringUtils;
import org.datalift.s4ac.utils.CRUDType;


public class AccessPolicy
{
    public final String uri;
    public final ACSType acstype;
    private final Set<String> graphs       = new HashSet<String>();
    private final Set<CRUDType> privileges = new HashSet<CRUDType>();
    private final Set<String> asks         = new HashSet<String>();

        public AccessPolicy(String uri, ACSType type) {
        if (! StringUtils.isSet(uri)) {
            throw new IllegalArgumentException("uri");
        }
        if (type == null) {
            throw new IllegalArgumentException("type");
        }
        this.uri     = uri;
        this.acstype = type;
    }

    public ACSType getAcstype() {
        return acstype;
    }

    public Set<String> getGraphs() {
        return Collections.unmodifiableSet(this.graphs);
    }

    public boolean hasPrivileges(CRUDType type) {
        return this.privileges.contains(type);
    }

    public Set<String> getAsks() {
        return Collections.unmodifiableSet(this.asks);
    }

    public String getUri() {
        return uri;
    }

    public void addPrivilege(CRUDType privilege) {
        this.privileges.add(privilege);
    }

    public void addAsk(String ask) {
        this.asks.add(ask);
    }

    public void addGraph(String g) {
        this.graphs.add(g);
    }

    @Override
    public String toString() {
        return "{ uri: <" + this.uri +
               ">, privileges: " + this.privileges +
               ", askQueries: "  + this.asks +
               ", graphs: "      + this.graphs + " }";
    }
}

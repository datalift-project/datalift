package org.datalift.s4ac.services;


import static org.datalift.fwk.rdf.RdfNamespace.S4AC;


public enum ACSType
{
    DISJUNCTIVE (S4AC.uri + "DisjunctiveAccessConditionSet"),
    CONJUNCTIVE (S4AC.uri + "ConjunctiveAccessConditionSet");

    public final String uri;

    ACSType(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return this.uri;
    }

    public static ACSType get(String uri) {
        return (DISJUNCTIVE.uri.equals(uri))? DISJUNCTIVE:
               (CONJUNCTIVE.uri.equals(uri))? CONJUNCTIVE: null;
    }
}

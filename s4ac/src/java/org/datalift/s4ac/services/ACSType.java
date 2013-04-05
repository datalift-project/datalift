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


import static org.datalift.fwk.rdf.RdfNamespace.S4AC;


/**
 * Types of access condition sets.
 *
 * @author Serena Villata (INRIA - Sophia-Antipolis)
 */
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

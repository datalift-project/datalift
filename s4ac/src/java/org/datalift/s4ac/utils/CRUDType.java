package org.datalift.s4ac.utils;


import static org.datalift.fwk.rdf.RdfNamespace.S4AC;


public enum CRUDType
{
    CREATE      (S4AC.uri + "Create"),
    READ        (S4AC.uri + "Read"),
    UPDATE      (S4AC.uri + "Update"),
    DELETE      (S4AC.uri + "Delete"),
    UNKNOWN     (null);

    public final String uri;

    CRUDType(String uri) {
        this.uri = uri;
    }

    @Override
    public String toString() {
        return this.uri;
    }

    public static CRUDType get(String uri) {
        return (CREATE.uri.equals(uri))? CREATE:
               (READ.uri.equals(uri))?   READ:
               (UPDATE.uri.equals(uri))? UPDATE:
               (DELETE.uri.equals(uri))? DELETE: UNKNOWN;
    }
}

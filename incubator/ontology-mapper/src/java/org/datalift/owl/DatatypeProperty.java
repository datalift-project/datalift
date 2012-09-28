package org.datalift.owl;


public final class DatatypeProperty extends OwlProperty
{
    public DatatypeProperty(String uri, String name) {
        this(uri, name, null);
    }

    public DatatypeProperty(String uri, String name, String desc) {
        super(uri, name, desc);
    }
}

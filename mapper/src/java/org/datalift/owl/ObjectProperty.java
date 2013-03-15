package org.datalift.owl;


public final class ObjectProperty extends OwlProperty
{
    public ObjectProperty(String uri, String name) {
        this(uri, name, null);
    }

    public ObjectProperty(String uri, String name, String desc) {
        super(uri, name, desc);
    }
}

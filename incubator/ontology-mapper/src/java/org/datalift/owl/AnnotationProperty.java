package org.datalift.owl;


public class AnnotationProperty extends OwlProperty
{
    public AnnotationProperty(String uri, String name) {
        this(uri, name, null);
    }

    public AnnotationProperty(String uri, String name, String desc) {
        super(uri, name, desc);
    }
}

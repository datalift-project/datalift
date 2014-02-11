package org.datalift.owl;


import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;


public final class Ontology extends OwlObject
{
    public final Map<String,OwlClass> classes;
    public final Map<String,OwlProperty> properties;

    public Ontology(String uri, String name, String desc,
                    Map<String,OwlClass> classes,
                    Map<String,OwlProperty> properties) {
        super(uri, name, desc);
        if (classes != null) {
            this.classes = Collections.unmodifiableMap(
                            new HashMap<String,OwlClass>(classes));
        }
        else {
            this.classes = Collections.emptyMap();
        }
        if (properties != null) {
            this.properties = Collections.unmodifiableMap(
                            new HashMap<String,OwlProperty>(properties));
        }
        else {
            this.properties = Collections.emptyMap();
        }
    }

    public Collection<OwlClass> classes() {
        return this.classes.values();
    }

    public Collection<OwlProperty> properties() {
        return this.properties.values();
    }

    public Collection<OwlProperty> properties(
                                            Class<? extends OwlProperty> type) {
        Collection<OwlProperty> props = new HashSet<OwlProperty>();
        for (OwlProperty p : this.properties.values()) {
            if (type.isInstance(p)) {
                props.add(p);
            }
        }
        return props;
    }
}

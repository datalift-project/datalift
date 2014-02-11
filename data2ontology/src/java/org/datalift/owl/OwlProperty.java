package org.datalift.owl;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.model.vocabulary.XMLSchema;

import static org.datalift.fwk.util.StringUtils.*;


abstract public class OwlProperty extends OwlObject
{
    private URI type;
    private Set<OwlClass> ranges;
    private Set<OwlClass> domains;

    protected OwlProperty(String uri, String name, String desc) {
        super(uri, name, desc);
    }

    public OwlProperty range(OwlClass clazz) {
        if (clazz != null) {
            if (this.ranges == null) {
                this.ranges = new HashSet<OwlClass>();
            }
            this.ranges.add(clazz);
        }
        return this;
    }

    public OwlProperty domain(OwlClass clazz) {
        if (clazz != null) {
            if (this.domains == null) {
                this.domains = new HashSet<OwlClass>();
            }
            this.domains.add(clazz);
        }
        return this;
    }

    public OwlProperty range(String type) {
        if ((isSet(type)) && (type.startsWith(XMLSchema.NAMESPACE))) {
            this.type = new URIImpl(type);
        }
        return this;
    }

    public URI type() {
        return this.type;
    }

    public Collection<OwlClass> ranges() {
        if (this.ranges != null) {
            return Collections.unmodifiableSet(this.ranges);
        }
        else {
            return Collections.emptySet();
        }
    }

    public Collection<OwlClass> domains() {
        if (this.domains != null) {
            return Collections.unmodifiableSet(this.domains);
        }
        else {
            return Collections.emptySet();
        }
    }

    @Override
    protected StringBuilder toString(StringBuilder b, boolean full) {
        if (this.type != null) {
            b.append(", type=").append(this.type);
        }
        if (full) {
            boolean first = true;
            b.append(", ranges=[");
            for (OwlClass c : this.ranges()) {
                b.append((first)? " ": ", ")
                 .append(c.toString(false));
                first = false;
            }
            b.append("], domains=[");
            first = true;
            for (OwlClass c : this.domains()) {
                b.append((first)? " ": ", ")
                 .append(c.toString(false));
                first = false;
            }
            b.append(']');
        }
        return b;
    }
}

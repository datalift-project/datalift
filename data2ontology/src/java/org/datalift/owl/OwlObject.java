package org.datalift.owl;


import static org.datalift.fwk.util.StringUtils.*;

import org.openrdf.model.URI;
import org.openrdf.model.impl.URIImpl;

import org.datalift.fwk.rdf.RdfNamespace;


abstract public class OwlObject implements Comparable<OwlObject>
{
    public final URI uri;
    public final String name;
    public final String desc;

    public OwlObject(String uri, String name, String desc) {
        if (isBlank(uri)) {
            throw new IllegalArgumentException("uri");
        }
        this.uri = new URIImpl(uri);
        if (! isSet(name)) {
            if (isSet(desc)) {
                name = desc;
                desc = null;
            }
            else {
                RdfNamespace ns = RdfNamespace.findByUri(this.uri.getNamespace());
                name = (ns != null)? ns.prefix + ':' + this.uri.getLocalName():
                                     this.uri.getLocalName();
            }
        }
        this.name = name;
        this.desc = desc;
    }

    public String uri() {
        return this.uri.stringValue();
    }

    public String name() {
        return (this.name != null)? this.name: "";
    }

    public String description() {
        return (this.desc != null)? this.desc: "";
    }

    @Override
    public boolean equals(Object o) {
        return (o instanceof OwlObject)?
                                    this.uri.equals(((OwlObject)o).uri): false;
    }

    @Override
    public int hashCode() {
        return this.uri.stringValue().hashCode();
    }

    @Override
    public String toString() {
        return this.toString(true);
    }

    @Override
    public int compareTo(OwlObject o) {
        return (this.uri.getLocalName().compareTo(o.uri.getLocalName()));
    }

    protected String toString(boolean full) {
        StringBuilder b = new StringBuilder(256);
        if (full) {
            b.append(this.getClass().getSimpleName())
             .append(" { \"").append(this.uri.stringValue())
             .append("\" (").append(this.name)
             .append(" : ").append(this.desc).append(')');
            b = this.toString(b, true).append(" }");
        }
        else {
            b.append(this.uri).append(" (").append(this.name);
            b = this.toString(b, false).append(')');
        }
        return b.toString();
    }

    protected StringBuilder toString(StringBuilder b, boolean full) {
        return b;
    }
}

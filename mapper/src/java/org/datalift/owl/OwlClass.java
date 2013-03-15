package org.datalift.owl;


import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;


public final class OwlClass extends OwlObject
{
    private boolean union = false;
    private Set<OwlClass> parents;
    private Set<OwlClass> subclasses;
    private Set<OwlClass> disjoints;
    private Set<OwlProperty> properties;

    public OwlClass(String uri, String name, String desc) {
        super(uri, name, desc);
    }

    public OwlClass parent(OwlClass parent) {
        return this.parent(parent, false);
    }

    public OwlClass parent(OwlClass parent, boolean union) {
        if (parent != null) {
            if (this.parents == null) {
                this.parents = new HashSet<OwlClass>();
            }
            this.parents.add(parent);
            parent.subclass(this);
            this.union |= union;
        }
        return this;
    }

    public Collection<OwlClass> parents() {
        if (this.parents != null) {
            return Collections.unmodifiableSet(this.parents);
        }
        else {
            return Collections.emptySet();
        }
    }

    public Collection<OwlClass> parents(boolean recurse) {
        return (recurse)? this.parents(new HashSet<OwlClass>()): this.parents();
    }

    private Collection<OwlClass> parents(Collection<OwlClass> results) {
        if (this.parents != null) {
            for (OwlClass c : this.parents) {
                results.add(c);
                c.parents(results);
            }
        }
        return results;
    }

    private OwlClass subclass(OwlClass child) {
        if (child != null) {
            if (this.subclasses == null) {
                this.subclasses = new HashSet<OwlClass>();
            }
            this.subclasses.add(child);
        }
        return this;
    }

    public Collection<OwlClass> subclasses() {
        if (this.subclasses != null) {
            return Collections.unmodifiableSet(this.subclasses);
        }
        else {
            return Collections.emptySet();
        }
    }

    public boolean union() {
        return this.union;
    }

    public OwlClass disjoint(OwlClass c) {
        if (c != null) {
            if (this.disjoints == null) {
                this.disjoints = new HashSet<OwlClass>();
            }
            this.disjoints.add(c);
        }
        return this;
    }

    public Collection<OwlClass> disjoints() {
        if (this.disjoints != null) {
            return Collections.unmodifiableSet(this.disjoints);
        }
        else {
            return Collections.emptySet();
        }
    }

    public OwlClass property(OwlProperty prop) {
        if (prop != null) {
            if (this.properties == null) {
                this.properties = new HashSet<OwlProperty>();
            }
            this.properties.add(prop);
        }
        return this;
    }

    public Collection<OwlProperty> properties() {
        if (this.properties != null) {
            return Collections.unmodifiableSet(this.properties);
        }
        else {
            return Collections.emptySet();
        }
    }

    public Collection<OwlProperty> properties(boolean recurse) {
        return (recurse)? this.properties(new HashSet<OwlProperty>()):
                          this.properties();
    }

    private Collection<OwlProperty> properties(Collection<OwlProperty> props) {
        if (this.properties != null) {
            props.addAll(this.properties);
        }
        if (this.parents != null) {
            for (OwlClass p : this.parents) {
                p.properties(props);
            }
        }
        return props;
    }

    @Override
    protected StringBuilder toString(StringBuilder b, boolean full) {
        if (full) {
            boolean first = true;
            b.append(",\nparents=[");
            for (OwlClass c : this.parents()) {
                b.append((first)? "\n\t": ",\n\t")
                 .append(c.toString(false));
                first = false;
            }
            b.append("],\nproperties=[");
            for (OwlProperty p : this.properties(true)) {
                b.append((first)? "\n\t": ",\n\t")
                 .append(p.toString(false));
                first = false;
            }
            b.append("]\n");
        }
        return b;
    }
}

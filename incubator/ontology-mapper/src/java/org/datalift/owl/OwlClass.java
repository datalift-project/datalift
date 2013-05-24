/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

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

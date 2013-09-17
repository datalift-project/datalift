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

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

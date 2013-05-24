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

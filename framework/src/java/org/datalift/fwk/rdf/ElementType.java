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

package org.datalift.fwk.rdf;


import org.datalift.fwk.util.StringUtils;


/**
 * Types of objects manipulated in RDF statements and SPARQL queries. 
 *
 * @author lbihanic
 */
public enum ElementType
{
    /** RDF resource, typically the subject of a statement */
    Resource,
    /** RDF property */
    Predicate,
    /** Named graph */
    Graph,
    /** RDF type/class */
    RdfType,
    /** RDF value, typically a literal. */
    Value;

    /**
     * Return the enumeration value corresponding to the specified
     * string, ignoring case.
     * @param  s   the description type, as a string.
     *
     * @return the description type value or <code>null</code> if
     *         the specified string was not recognized.
     */
    public static ElementType fromString(String s) {
        ElementType v = null;
        if (StringUtils.isSet(s)) {
            for (ElementType t : values()) {
                if (t.name().equalsIgnoreCase(s)) {
                    v = t;
                    break;
                }
            }
            // Support for legacy URLs for resource descriptions
            if ((v == null) && ("Object".equalsIgnoreCase(s))) {
                v = Resource;
            }
        }
        return v;
    }
}

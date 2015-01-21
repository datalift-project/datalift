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
package org.datalift.fwk.project;


import org.openrdf.model.Statement;

import org.datalift.fwk.util.CloseableIterable;


/**
 * A object describing an abstract RDF source, regardless the actual
 * triple storage (file, local RDF store, SPARQL endpoint...).
 *
 * @author hdevos
 */
public interface RdfSource extends Source, CloseableIterable<Statement>
{
    //-------------------------------------------------------------------------
    // RdfSource contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the URI that was used to parse RDF data or to compute
     * the URIs of resources and predicates of this source, if any.
     * <p>
     * Please note that this base URI may be partial, i.e. it may lack
     * the trailing '/' or '#' characters. In this case, resources URIs
     * are (usually) built by appending as terminal '/' character while
     * predicate URIs use a terminal '#' character.</p>
     *
     * @return the base URI for this source or <code>null</code> if the
     *         URIs within this source do not share any common base.
     */
    public String getBaseUri();
}

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

package org.datalift.core.rdf;


import org.datalift.fwk.Configuration;
import org.datalift.fwk.rdf.Repository;


/**
 * A factory class for instantiating
 * {@link Repository DataLift repositories}.
 * <p>
 * RepositoryFactories are loaded using the
 * {@link java.util.ServiceLoader Java service provider} mechanism.</p>
 * <p>
 * The Java classpath is searched for UTF-8 encoded
 * <i>provider-configuration files</i> named
 * <code>META-INF/services/org.datalift.core.rdf.RepositoryFactory</code>
 * that contains a list of fully-qualified implementation class
 * names.</p>
 *
 * @author lbihanic
 */
public abstract class RepositoryFactory
{
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new repository factory instance.
     */
    protected RepositoryFactory() {
        // NOP
    }

    //-------------------------------------------------------------------------
    // RepositoryFactory contract definition
    //-------------------------------------------------------------------------

    /**
     * Builds a new {@link Repository DataLift repository}.
     * @param  name            the repository name in DataLift
     *                         configuration.
     * @param  url             the repository URL.
     * @param  configuration   the DataLift configuration.
     *
     * @return a ready-to-use {@link Repository} or <code>null</code>
     *         if the configuration for the specified repository can
     *         not be handled by this factory.
     */
    abstract public Repository newRepository(String name, String url,
                                             Configuration configuration);
}

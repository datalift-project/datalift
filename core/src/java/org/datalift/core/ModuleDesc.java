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

package org.datalift.core;


import java.io.File;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.datalift.fwk.Module;


/**
 * A descriptor for a registered module that acts as a cache for
 * module-provided data.
 *
 * @author lbihanic
 */
/* package */ final class ModuleDesc extends PackageDesc
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The module name. */
    public final String name;
    /** The module instance. */
    public final Module module;
    /** The module sub-resources. */
    public final Map<String,Class<?>> ressourceClasses;
    /** Whether the module main class is itself a JAX-RS resource. */
    public final boolean isResource;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new module descriptor.
     * @param  module        the module.
     * @param  classLoader   the classloader for the module.
     * @param  root          the file or directory from which the module
     *                       was loaded, or <code>null</code> if the
     *                       module was loaded from the DataLift WAR.
     *
     * @throws IllegalArgumentException if either <code>module</code>
     *         of <code>classLoader</code> is <code>null</code>.
     */
    @SuppressWarnings("deprecation")
    public ModuleDesc(Module module, ClassLoader classLoader, File root) {
        super(classLoader, root);

        if (module == null) {
            throw new IllegalArgumentException("module");
        }
        this.module      = module;
        this.name        = module.getName();
        Map<String,Class<?>> resources = new TreeMap<String,Class<?>>();
        Map<String,Class<?>> r = module.getResources();
        if (r != null) {
            resources.putAll(r);
        }
        this.ressourceClasses = Collections.unmodifiableMap(resources);
        this.isResource = module.isResource();
    }

    //-------------------------------------------------------------------------
    // ModuleDesc contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the resource associated to the specified path
     * @param  path   the resource path.
     *
     * @return the resource associated to the specified path or
     *         <code>null</code> if none matches.
     */
    public Class<?> get(String  path) {
        return this.ressourceClasses.get( path);
    }

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return this.name;
    }
}

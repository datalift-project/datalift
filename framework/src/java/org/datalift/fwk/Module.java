/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.fwk;


import java.util.Map;


/**
 * Base interface for DataLift application modules.
 * <p>
 * Modules can be packaged as a JAR file or a directory (a deployment
 * unit) and are loaded using the
 * {@link java.util.ServiceLoader Java service provider} mechanism.</p>
 * <p>
 * Each deployment unit shall include a UTF-8 encoded
 * <i>provider-configuration file</i> named
 * <code>META-INF/services/org.datalift.fwk.Module</code> that contains
 * a list of fully-qualified implementation class names.</p>
 * <p>
 * If the module is packaged as a directory, the following
 * sub-directories and files are considered:</p>
 * <dl>
 *  <dt><code>/classes</code></dt>
 *  <dd>The default directory for module classes</dd>
 *  <dt><code>/*.jar</code></dt>
 *  <dd>JAR files containing the module classes, if no
 *      <code>/classes</code> directory is present</dd>
 *  <dt><code>/lib/**&#47;*.jar</code></dt>
 *  <dd>JAR files in the <code>/lib</code> directory tree containing
 *      the module classes and/or third-party libraries</dd>
 * </dl>
 * <p>deployment units can include static resources. Resources in the
 * <code>/public</code> directory are accessible externally over HTTP as
 * "<code>&lt;base URL&gt;/&lt;module name&gt;/&lt;resource name&gt;</code>"
 * (the <code>/public</code> part is removed from the URL). Other
 * static resources can only be accessed internally to the server
 * (e.g. Velocity templates).</p>
 * <p>
 * The DataLift application loads each deployment unit in dedicated a
 * class loader. Hence, modules from the same deployment unit can access
 * each others (i.e. perform Java methods calls) but cannot invoke
 * methods on modules from other deployment units except through
 * framework-defined interfaces such as the
 * {@link org.datalift.fwk.sparql.SparqlEndpoint SPARQL endpoint}
 * interface.</p>
 *
 * @author hdevos
 */
public interface Module extends LifeCycle
{
    /**
     * Returns the name, i.e. the URL path element to access this
     * module.
     * @return the module name as a URL component.
     */
    public String getName();

    /**
     * Returns the resource classes this module makes available.
     * <p>
     * Resource classes shall be JAX-RS resources and offer one of
     * the following two constructors:</p>
     * <ul>
     *  <li>A public no-arg constructor</li>
     *  <li>A public constructor taking the owning module instance
     *      as argument</li>
     * </ul>
     * @return the resource classes as a map using the resource URL
     *         path element as key.
     */
    public Map<String,Class<?>> getResources();

    /**
     * Returns whether this module is itself a JAX-RS root resource.
     * Registering as a root resource enabled a module to process
     * request targeting no resource.
     * @return <code>true</code> if this module is a JAX-RS resource,
     *         <code>false</code> otherwise.
     */
    public boolean isResource();
}

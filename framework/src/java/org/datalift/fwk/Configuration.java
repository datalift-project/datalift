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

package org.datalift.fwk;


import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.MissingResourceException;
import java.util.Properties;

import org.openrdf.repository.RepositoryConnection;

import org.datalift.fwk.rdf.Repository;


/**
 * The DataLift application configuration data.
 * <p>
 * Configuration data are made available to DataLift components through
 * the {@link LifeCycle lifecycle} events. It enables access to the
 * following data:</p>
 * <ul>
 *  <li>{@link #getProperty(String) Named configuration data},</li>
 *  <li>{@link #getRepository(String) Repositories},</li>
 *  <li>{@link #getPublicStorage() Local HTTP-accessible file storage}</li>
 *  <li>{@link #getPrivateStorage() Local private file storage}</li>
 * </ul>
 * <p>
 * This class also allows loading of third-party
 * {@link #loadProperties(String, Class) properties files} and acts as
 * an object directory where DataLift components can
 * register and retrieve objects both by {@link #getBean(Class) class}
 * and by {@link #getBean(String) name}.</p>
 *
 * @author hdevos
 */
public abstract class Configuration
{
    /** Configuration key for the default repository. */
    public final static String DEFAULT_REPOSITORY       = "";
    /** Configuration key for the public data repository. */
    public final static String DATA_REPOSITORY          = "data";
    /** Configuration key for the internal repository. */
    public final static String INTERNAL_REPOSITORY      = "internal";

    /**
     * Return the value of a configuration property as a string.
     * @param  key   the name of the configuration property.
     *
     * @return the value of the configuration property or
     *         <code>null</code> if <code>key</code> is unknown or its
     *         value is not a string.
     */
    abstract public String getProperty(String key);

    /**
     * Return the value of a configuration property as a string.
     * @param  key   the name of the configuration property.
     * @param  def   a default value for the property.
     *
     * @return the value of the configuration property or
     *         <code>def</code> if <code>key</code> is unknown or its
     *         value is not a string.
     */
    abstract public String getProperty(String key, String def);

    /**
     * Returns a list of the configured repositories.
     * @return a list of the configured repositories.
     */
    abstract public Collection<Repository> getRepositories();

    /**
     * Returns the names of the configured repositories.
     * @return the names of the configured repositories.
     */
    abstract public Collection<String> getRepositoryUris();

    /**
     * Return a given repository.
     * @param  uri   the configured name of the repository,
     *               <code>null</code> for the default repository.
     *
     * @return the repository identified by <code>uri</code> in the
     *         configuration.
     * @throws IllegalArgumentException if no repository named
     *         <code>uri</code> was configured.
     */
    abstract public Repository getRepository(String uri);

    /**
     * Returns the default repository.
     * @return the default repository.
     */
    public Repository getDefaultRepository() {
        return this.getRepository(DEFAULT_REPOSITORY);
    }

    /**
     * Returns the public data repository.
     * @return the public data repository.
     */
    public Repository getDataRepository() {
        return this.getRepository(DATA_REPOSITORY);
    }

    /**
     * Returns the internal repository.
     * @return the internal repository.
     */
    public Repository getInternalRepository() {
        return this.getRepository(INTERNAL_REPOSITORY);
    }

    /**
     * Return a connection to a given repository.
     * @param  uri   the configured name of the repository,
     *               <code>null</code> for the default repository.
     *
     * @return a connection to the repository identified by
     *         <code>uri</code> in the configuration.
     * @throws IllegalArgumentException if no repository named
     *         <code>uri</code> was configured.
     * @throws TechnicalException if the connection to the repository
     *         cannot be established.
     */
    public RepositoryConnection getRepositoryConnection(String uri) {
        return this.getRepository(uri).newConnection();
    }

    /**
     * Returns the path of the public file storage.
     * <p>
     * This server file system location shall be used to save the data
     * files of DataLift components that can be accessed remotely. The
     * URL of the public files is:
     * <code>&lt;base URI&gt;/&lt;rel path&gt;</code> where:</p>
     * <ul>
     *  <li><code>&lt;base URI&gt;</code> is the base URI of the
     *      DataLift application (see
     *      {@link javax.ws.rs.core.UriInfo})</li>
     *  <li><code>&lt;rel path&gt;</code> is the file path relative
     *      to the public storage root directory.</li>
     * </ul>
     * @return the path of the public file storage or <code>null</code>
     *         if no such storage is defined.
     */
    abstract public File getPublicStorage();

    /**
     * Returns the path of the private file storage. This storage shall
     * be used to save the data files of DataLift components that are
     * not remotely accessed.
     * @return the path of the private file storage.
     */
    abstract public File getPrivateStorage();

    /**
     * Return the path of the directory where the DataLift application
     * expects to find the {@link Module module} components.
     * <p>
     * A module can be packaged either as a single JAR or as a
     * directory with the following first-level sub-directories:</p>
     * <ul>
     *  <li><code>classes</code>: the module classes, if not packaged
     *   in a JAR file</li>
     *  <li><code>lib</code>: the module classes and/or third-party
     *   libraries</li>
     * </ul>
     *
     * @return Return the path of the module storage directory or
     *         <code>null</code> if loading of third-party modules is
     *         disabled (i.e. modules are only loaded from the
     *         application archive (WAR).
     */
    abstract public File getModulesPath();

    /**
     * Loads a third-party properties file.
     * @param  path    the path of the properties file, absolute or
     *                 relative to the DataLift configuration file.
     * @param  owner   the class on behalf of which loading the file
     *                 or <code>null</code> if the file is not part
     *                 of a DataLift module.
     *
     * @return the loaded properties.
     * @throws IOException if <code>path</code> does not exist, can
     *         not be accessed or is not a valid properties file.
     */
    abstract public Properties loadProperties(String path, Class<?> owner)
                                                            throws IOException;

    /**
     * Retrieves a registered bean by its key.
     * @param  key   the key for the bean to retrieve.
     *
     * @return the registered bean.
     * @throws MissingResourceException if no matching bean was found.
     *
     * @see    #registerBean(String, Object)
     */
    abstract public Object getBean(String key);

    /**
     * Retrieves a registered bean by its class, one of its
     * superclasses or one of the interfaces it implements.
     * @param  clazz   the type of the bean to retrieve.
     *
     * @return the registered bean for the type. If several objects
     *         were registered for the specified type, the first one
     *         (in order of registration) is returned.
     * @throws MissingResourceException if no matching bean was found.
     *
     * @see    #registerBean(Object)
     */
    abstract public <T> T getBean(Class<T> clazz);

    /**
     * Retrieves the list of registered bean for a given class, which
     * can be the bean actual class, one of its superclasses or one of
     * the interfaces it implements.
     * @param  clazz   the type of the beans to retrieve.
     *
     * @return the list of the registered beans for the class, empty
     *         if no bean was found.
     *
     * @see    #registerBean(Object)
     */
    abstract public <T> Collection<T> getBeans(Class<T> clazz);

    /**
     * Registers an object under the specified key
     * @param  key    the key for the object.
     * @param  bean   the object to register.
     *
     * @throws IllegalArgumentException if <code>key</code> or
     *         <code>bean<code> is <code>null</code> or if another
     *         object has already been registered as <code>key</code>.
     */
    abstract public void registerBean(String key, Object bean);

    /**
     * Registers an object by its class, its superclasses and all
     * implemented interfaces.
     * @param  bean   the object.
     *
     * @throws IllegalArgumentException if <code>bean</code> is
     *         <code>null</code>.
     */
    abstract public void registerBean(Object bean);

    /**
     * Removes the specified object from the registry.
     * @param  bean   the object to remove.
     * @param  key    the optional key the object was associated with.
     */
    abstract public void removeBean(Object bean, String key);
}

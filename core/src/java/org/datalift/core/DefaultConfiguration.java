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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Properties;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.TreeMap;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.Repository;

import org.datalift.core.rdf.RepositoryFactory;
import org.datalift.core.util.VersatileProperties;


/**
 * The default DataLift configuration that reads configuration data
 * from a properties file named
 * "<code>datalift-application.properties</code>" present in the
 * classpath.
 *
 * @author hdevos
 */
public class DefaultConfiguration extends Configuration
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The DataLift working directory system property/environment variable. */
    public final static String DATALIFT_HOME = "datalift.home";
    /** The DataLift properties configuration file. */
    public final static String CONFIGURATION_FILE =
                                            "datalift-application.properties";

    /** The property to define the list of repository names. */
    public final static String REPOSITORY_URIS = "datalift.rdf.repositories";
    /** The property suffix for repository URL. */
    public final static String REPOSITORY_URL           = ".repository.url";
    /** The property suffix for repository default flag. */
    public final static String REPOSITORY_DEFAULT_FLAG  = ".repository.default";

    /** The property to define the module directory path. */
    public final static String MODULES_PATH         = "datalift.modules.path";
    /** The property to define the public file storage directory path. */
    public final static String PUBLIC_STORAGE_PATH  =
                                            "datalift.public.storage.path";
    /** The property to define the private file storage directory path. */
    public final static String PRIVATE_STORAGE_PATH =
                                            "datalift.private.storage.path";

    /**
     * Former property suffix for repository URL.
     * <p>
     * Support for this property will be removed in the next release
     * of DataLift.</p>
     */
    private final static String REPOSITORY_HTTP_URL = ".repository.http.url";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /**
     * The configuration data as a properties object. It is initialized
     * to an empty properties map to allow {@link #getProperty(String)}
     * to always resolve system properties and environment variables,
     * even when the loading of the DataLift configuration fails.
     */
    private VersatileProperties props = new VersatileProperties();
    /** The configured repositories. */
    private Map<String,Repository> repositories;
    /** The private (i.e. accessible to server modules only) file storage. */
    private final File privateStorage;
    /** The public (i.e. remotely accessible) file storage. */
    private final File publicStorage;
    /** The module directory. */
    private final File modulesPath;

    /** The registry for beans indexed by name. */
    private final Map<String,Object> beansByName =
                Collections.synchronizedMap(new TreeMap<String,Object>());
    /** The registry for beans indexed by class and interfaces. */
    private final Map<Class<?>,Object> beansByType =
                Collections.synchronizedMap(new HashMap<Class<?>,Object>());

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Default constructor.
     * @param  props   the application runtime environment.
     *
     * @throws TechnicalException if any error occurred loading or
     *         parsing the configuration data.
     */
    public DefaultConfiguration(Properties props) {
        this.props          = this.loadConfiguration(props);
        this.modulesPath    = this.initLocalPath(MODULES_PATH, false, false);
        this.privateStorage = this.initLocalPath(PRIVATE_STORAGE_PATH, true, true);
        this.publicStorage  = this.initLocalPath(PUBLIC_STORAGE_PATH, false, true);

        // Check configuration to warn against potential problems.
        if (this.modulesPath == null) {
            log.warn("No module directory defined. " +
                     "Modules will only be loaded from application WAR");
        }
        if (this.publicStorage == null) {
            log.warn("No public file store defined. " +
                     "No file can be made remotely available.");
        }
    }

    //-------------------------------------------------------------------------
    // Configuration contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String getProperty(String key) {
        return this.props.getProperty(key);
    }

    /** {@inheritDoc} */
    @Override
    public String getProperty(String key, String def) {
        return this.props.getProperty(key, def);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getPropertyNames() {
        Collection<String> names = new HashSet<String>();
        for (Object o : this.props.keySet()) {
            if (o instanceof String) {
                names.add((String)o);
            }
        }
        return Collections.unmodifiableCollection(names);
    }

    /** {@inheritDoc} */
    @Override
    public Collection<Repository> getRepositories(boolean publicOnly) {
        // Defensive copy.
        List<Repository> l = new LinkedList<Repository>();
        // Always place the default (public) repository at the top.
        Repository def = this.getDefaultRepository();
        l.add(def);
        for (Repository r : this.repositories.values()) {
            if ((! r.equals(def)) && ((! publicOnly) || (r.isPublic()))) {
                l.add(r);
            }
        }
        return l;
    }

    /** {@inheritDoc} */
    @Override
    public Collection<String> getRepositoryUris() {
        return this.repositories.keySet();
    }

    /** {@inheritDoc} */
    @Override
    public Repository getRepository(String uri) {
        if (uri == null) {
            uri = "";           // Default repository.
        }
        Repository r = this.repositories.get(uri);
        if (r == null) {
            throw new IllegalArgumentException("No such repository: " + uri);
        }
        return r;
    }

    /** {@inheritDoc} */
    @Override
    public File getPublicStorage() {
        return this.publicStorage;
    }

    /** {@inheritDoc} */
    @Override
    public File getPrivateStorage() {
        return this.privateStorage;
    }

    /** {@inheritDoc} */
    @Override
    public File getModulesPath() {
        return this.modulesPath;
    }

    /** {@inheritDoc} */
    @Override
    public Properties loadProperties(String path, Class<?> owner)
                                                            throws IOException {
        if ((path == null) || (path.length() == 0)) {
            throw new IllegalArgumentException("path");
        }
        return this.loadFromClasspath(path, this.props, owner);
    }

    /** {@inheritDoc} */
    @Override
    public Object getBean(String key) {
        if ((key == null) || (key.length() == 0)) {
            throw new IllegalArgumentException("key");
        }
        Object o = this.beansByName.get(key);
        if (o == null) {
            throw new MissingResourceException(key, null, key);
        }
        log.trace("Retrieved bean {} for key \"{}\"", o, key);
        return o;
    }

    /** {@inheritDoc} */
    @Override
    public <T> T getBean(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz");
        }
        T bean = null;
        Object o = this.beansByType.get(clazz);
        if ((o != null) && (clazz.isAssignableFrom(o.getClass()))) {
            bean = clazz.cast(o);
        }
        else if (o instanceof List<?>) {
            bean = clazz.cast(((List<?>)o).get(0));
        }
        if (bean == null) {
            String name = clazz.getName();
            throw new MissingResourceException(name, null, name);
        }
        log.trace("Retrieved bean {} for class {}", bean, clazz);
        return bean;
    }

    /** {@inheritDoc} */
    @Override
    public <T> Collection<T> getBeans(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException("clazz");
        }
        Collection<T> beans = new LinkedList<T>();

        Object o = this.beansByType.get(clazz);
        if (o instanceof List<?>) {
            // List of objects matching the specified type.
            for (Object x : (List<?>)o) {
                beans.add(clazz.cast(x));
            }
        }
        else if ((o != null) && (clazz.isAssignableFrom(o.getClass()))) {
            // Single bean for the type.
            beans.add(clazz.cast(o));
        }
        // Else: no bean found. => Return an empty list.

        log.trace("Retrieved beans {} for class {}", beans, clazz);
        return beans;
    }

    /** {@inheritDoc} */
    @Override
    public void registerBean(String key, Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("bean");
        }
        if ((key == null) || (key.length() == 0)) {
            throw new IllegalArgumentException("key");
        }
        if (! this.beansByName.containsKey(key)) {
            this.beansByName.put(key, bean);
            log.trace("Registered bean {} with key \"{}\"", bean, key);
        }
        else {
            throw new IllegalArgumentException(
                            "Multiple definitions for bean \"" + key + "\"");
        }
    }

    /** {@inheritDoc} */
    @Override
    public void registerBean(Object bean) {
        if (bean == null) {
            throw new IllegalArgumentException("bean");
        }
        // Register the bean for all its classes and interfaces.
        this.registerForClass(bean, bean.getClass());
    }

    /** {@inheritDoc} */
    @Override
    public void removeBean(Object bean, String key) {
        if (bean == null) {
            throw new IllegalArgumentException("bean");
        }
        // Remove bean entries for all its classes and interfaces.
        this.removeForClass(bean, bean.getClass());
        // Remove bean by key.
        if ((key != null) && (key.length() != 0)
                          && (bean == this.beansByName.get(key))) {
            this.beansByName.remove(key);
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Loads the DataLift configuration, reading the configuration file
     * path from the specified properties.
     * @param  props   the bootstrap properties.
     *
     * @throws TechnicalException if any error occurred accessing or
     *         parsing the DataLift configuration file.
     */
    private VersatileProperties loadConfiguration(Properties props) {
        VersatileProperties config = null;
        try {
            String cfgFilePath = CONFIGURATION_FILE;
            String homePath = this.getProperty(DATALIFT_HOME);
            if (homePath != null) {
                File f = new File(new File(homePath), "conf/" + cfgFilePath);
                if ((f.isFile()) && (f.canRead())) {
                    cfgFilePath = f.getPath();
                }
            }
            // Load configuration.
            config = this.loadFromClasspath(cfgFilePath, props, null);
            log.info("DataLift configuration loaded from {}", cfgFilePath);
        }
        catch (IOException e) {
            TechnicalException error = new TechnicalException(
                            "configuration.not.found", e, CONFIGURATION_FILE);
            log.fatal(error.getMessage(), e);
            throw error;
        }
        return config;
    }

    /**
     * Reads the configuration for the RDF stores and initializes them.
     * @throws TechnicalException if any error occurred initializing
     *         one of the repositories.
     */
    protected void initRepositories(Collection<PackageDesc> packages) {
        // Use an ordered collection to consider the default (core) factories
        // only when all third-party factories have failed.
        Collection<RepositoryFactory> factories =
                                            new LinkedList<RepositoryFactory>();
        // Get default (core-provided) repository factories.
        Collection<RepositoryFactory> defaultFactories =
                this.loadRepositoryFactories(this.getClass().getClassLoader());
        // Build the list of factory classes to ignore.
        Collection<Class<?>> defaultClasses = new HashSet<Class<?>>();
        for (RepositoryFactory f : defaultFactories) {
            defaultClasses.add(f.getClass());
        }
        // Load available repository factories from third-party packages,
        // ignoring default (core-provided) ones, retrieved through
        // classloader parentage.
        if ((packages != null) && (! packages.isEmpty())) {
            for (PackageDesc p : packages) {
                // Find non-default third-party repository factories.
                for (RepositoryFactory f :
                                this.loadRepositoryFactories(p.classLoader)) {
                    if (! defaultClasses.contains(f.getClass())) {
                        log.debug("Found repository connector {} from {}",
                                        f.getClass().getSimpleName(), p.root);
                        factories.add(f);
                    }
                }
            }
        }
        // Append default factories.
        factories.addAll(defaultFactories);

        // Preserve repository configuration declaration order.
        Map<String,Repository> m = new LinkedHashMap<String,Repository>();
        // Connect repositories.
        for (String name : this.getConfigurationEntry(REPOSITORY_URIS, true)
                               .split("\\s*,\\s*")) {
            if (name.length() == 0) continue;           // Ignore...

            try {
                // Get repository connection URL from configuration.
                // Try with former URL property name first.
                String url = this.getConfigurationEntry(
                                            name + REPOSITORY_HTTP_URL, false);
                if (url == null) {
                    // Old URL property not found. => Try with new name.
                    url = this.getConfigurationEntry(
                                            name + REPOSITORY_URL, true);
                }
                else {
                    log.warn("Property {} is deprecated. Use {} instead.",
                             name + REPOSITORY_HTTP_URL, name + REPOSITORY_URL);
                }
                Repository r = null;
                for (RepositoryFactory f : factories) {
                    r = f.newRepository(name, url, this);
                    if (r != null) break;
                }
                if (r == null) {
                    throw new TechnicalException(
                                        "repository.unknown.type", name, url);
                }
                m.put(name, r);
                if (this.props.getBoolean(
                                    name + REPOSITORY_DEFAULT_FLAG, false)) {
                    m.put(DEFAULT_REPOSITORY, r);
                }
            }
            catch (TechnicalException e) {
                log.fatal(e.getMessage(), e);
                throw e;
            }
            catch (Exception e) {
                TechnicalException error = new TechnicalException(
                        "repository.config.error", e, name, e.getMessage());
                log.fatal(error.getMessage(), e);
                throw error;
            }
        }
        if (m.get(DEFAULT_REPOSITORY) == null) {
            TechnicalException error = new TechnicalException(
                                                "repository.missing.default");
            log.fatal(error.getMessage());
            throw error;
        }
        this.repositories = Collections.unmodifiableMap(m);
    }

    /**
     * Loads all available {@link RepositoryFactory} classes using the
     * Java {@link ServiceLoader service provider} mechanism.
     * @param  cl   the classloader to scan for factories.
     *
     * @return the available repository factories.
     */
    private Collection<RepositoryFactory> loadRepositoryFactories(
                                                            ClassLoader cl) {
        Collection<RepositoryFactory> factories =
                                        new LinkedList<RepositoryFactory>();
        // Make a fault-tolerant loading of available factories.
        Iterator<RepositoryFactory> i =
                    ServiceLoader.load(RepositoryFactory.class, cl).iterator();
        boolean hasNext = true;
        do {
            try {
                hasNext = i.hasNext();
                if (hasNext) {
                    factories.add(i.next());
                }
            }
            catch (ServiceConfigurationError e) {
                // Skip factory...
                log.warn("Failed to load {}", e.getMessage());
            }
        }
        while (hasNext);
        // Check that at least one provider was found.
        if (factories.isEmpty()) {
            throw new TechnicalException("repository.missing.factory");
        }
        return factories;
    }

    /**
     * Reads the DataLift configuration for the specified directory
     * property and ensures it exists on the local file system.
     * @param  key        the configuration key for the directory.
     * @param  required   whether the configuration entry is required.
     * @param  create     whether the directory shall be created if it
     *                    does not exist.
     *
     * @return the directory path on the local file system.
     */
    private File initLocalPath(String key, boolean required,
                                           boolean create) {
        File f = null;
        String path = this.getConfigurationEntry(key, required);
        if (path != null) {
            f = new File(path);
            if (! f.exists()) {
                if (create) {
                    if (f.mkdirs() == false) {
                        throw new TechnicalException(
                                            "local.path.creation.failed", f);
                    }
                    else {
                        log.warn("Created storage directory \"{}\" " +
                            "(required for configuration entry \"{}\")", f, key);
                    }
                }
                else {
                    throw new TechnicalException("local.path.not.directory", f);
                }
            }
            else {
                if (! f.isDirectory()) {
                    throw new TechnicalException("local.path.not.directory", f);
                }
            }
        }
        return f;
    }

    /**
     * Loads the specified Java {@link Properties properties file} on
     * behalf of the specified class.
     * @param  filePath   the properties file path, relative to the
     *                    classloader of the owner class
     * @param  defaults   the (optional) parent properties.
     * @param  owner      the class on behalf of which loading the file
     *                    or <code>null</code> to use the default
     *                    class loader.
     *
     * @return the properties, loaded from the file.
     * @throws IOException if any error occurred accessing the file or
     *         parsing the property values.
     */
    private VersatileProperties loadFromClasspath(String filePath,
                                        Properties defaults, Class<?> owner)
                                                            throws IOException {
        VersatileProperties p = (defaults != null)?
                                        new VersatileProperties(defaults):
                                        new VersatileProperties();
        InputStream in = null;
        try {
            if (owner == null) {
                // No owner specified. => Use default classloader.
                owner = this.getClass();
            }
            in = owner.getClassLoader().getResourceAsStream(filePath);
            if (in == null) {
                in = new FileInputStream(filePath);
            }
            p.load(in);
        }
        finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
            }
        }
        return p;
    }

    /**
     * Returns the value of the configuration entry specified by
     * <code>key</code>
     * @param  key        the configuration entry key.
     * @param  required   whether the entry shall be present in
     *                    configuration.
     *
     * @return the value associate with <code>key</code> in this
     *         DataLift configuration or <code>null</code> if
     *         <code>key</code> is not bound.
     * @throws TechnicalException if <code>key</code> is not bound
     *         and <code>required</code> is set to <code>true</code>.
     */
    private String getConfigurationEntry(String key, boolean required) {
        String s = this.props.getProperty(key);
        if ((required) && ((s == null) || (s.length() == 0))) {
            throw new TechnicalException("configuration.missing.property", key);
        }
        return s;
    }

    /**
     * Registers the specified object as a
     * {@link Configuration#getBean(Class) bean} in the DataLift
     * configuration for the specified class, its superclasses and all
     * the interfaces the class implements.
     * @param  o  the object to register.
     * @param  c  the class to associate the object with.
     *
     * @throws TechnicalException if <code>o</code> is not an instance
     *         of <code>c</code>.
     */
    private void registerForClass(Object o, Class<?> c) {
        if ((o != null) && (c != null) && (c != Object.class)) {
            if (! c.isInstance(o)) {
                throw new TechnicalException("inconsistent.object.class",
                                                                o, c.getName());
            }
            this.registerForType(o, c);
            for (Class<?> i : c.getInterfaces()) {
                this.registerForInterface(o, i);
            }
            this.registerForClass(o, c.getSuperclass());
        }
        // Else: ignore.
    }
    private void registerForInterface(Object o, Class<?> c) {
        this.registerForType(o, c);
        for (Class<?> i : c.getInterfaces()) {
            this.registerForInterface(o, i);
        }
    }
    @SuppressWarnings("unchecked")
    private void registerForType(Object o, Class<?> clazz) {
        Object x = this.beansByType.get(clazz);
        if (x == null) {
            // Register bean for the specified type.
            this.beansByType.put(clazz, o);
        }
        else {
            List<Object> l = null;
            if (x instanceof List<?>) {
                // Add new bean to the list.
                l = (List<Object>)x;
            }
            else {
                // Replace existing object by a list and add object to it.
                l = new LinkedList<Object>();
                l.add(x);
                this.beansByType.put(clazz, l);
            }
            if (! l.contains(o)) {
                l.add(o);
            }
        }
        log.trace("Registered bean {} as type {}", o, clazz);
    }

    /**
     * Removes the specified object from the DataLift
     * {@link Configuration#getBean(Class) configuration} for the
     * specified class, its superclasses and all the interfaces the
     * class implements.
     * @param  o  the object to remove.
     * @param  c  the class to which the object shall no longer be
     *            associated with.
     */
    private void removeForClass(Object o, Class<?> c) {
        if ((o != null) && (c != null) && (c != Object.class)) {
            this.removeForType(o, c);
            for (Class<?> i : c.getInterfaces()) {
                this.removeForInterface(o, i);
            }
            this.removeForClass(o, c.getSuperclass());
        }
        // Else: ignore.
    }
    private void removeForInterface(Object o, Class<?> c) {
        this.removeForType(o, c);
        for (Class<?> i : c.getInterfaces()) {
            this.removeForInterface(o, i);
        }
    }
    @SuppressWarnings("unchecked")
    private void removeForType(Object o, Class<?> clazz) {
        Object x = this.beansByType.get(clazz);
        if (x instanceof List<?>) {
            // Remove bean from list.
            List<Object> l = (List<Object>)x;
            l.remove(o);
            if (l.isEmpty()) {
                this.beansByType.remove(clazz);
            }
        }
        else if (o == x) {
            // Remove entry.
            this.beansByType.remove(clazz);
        }
        // Else: not found.
        log.trace("Removed bean {} for type {}", o, clazz);
    }
}

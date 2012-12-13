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
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.io.FileUtils;


/**
 * A descriptor for a DataLift bundle, i.e. the file system artifact
 * (JAR file or directory) that contains DataLift modules or components
 * and the associated classloader.
 *
 * @author lbihanic
 */
public class Bundle
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /**
     * The module sub-directory where to look for classes first. If
     * no present, root-level JAR files will be searched.
     */
    public final static String BUNDLE_CLASSES_DIR = "classes";
    /**
     * The module sub-directory where to look for third-party JAR files.
     * Regardless the presence of this directory, root-level JARs will
     * be loaded.
     */
    public final static String BUNDLE_LIB_DIR     = "lib";

    /** The name of the directory where to store temporary files. */
    private final static String CACHE_DIRECTORY_NAME    = "module-data";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /** A FileFilter to select JAR files. */
    private final static FileFilter jarFilter = new FileFilter() {
            @Override
            public boolean accept(File f) {
                return ((f.isFile()) && (f.getName().endsWith(".jar")));
            }
        };

    private final static Logger log = Logger.getLogger();

    //------------------------------------------------------------------------
    // Instance members
    //------------------------------------------------------------------------

    /** The classloader for the bundle. */
    private final ClassLoader classLoader;
    /**
     * The file or directory of the package, or <code>null</code> if
     * classes are loaded directly from the class path.
     */
    private final File root;

    //------------------------------------------------------------------------
    // Constructors
    //------------------------------------------------------------------------

    /**
     * Creates a new Datalift bundle descriptor for the specified
     * classloader, typically the Datalift web application classloader.
     */
    /* package */ Bundle(ClassLoader classLoader) {
        if (classLoader == null) {
            throw new IllegalArgumentException("classLoader");
        }
        this.root        = null;
        this.classLoader = classLoader;
    }

    /**
     * Creates a new Datalift bundle descriptor from the specified
     * directory or JAR file.
     * @param  f        the directory or JAR file to load the bundle
     *                  classes from, or <code>null</code> if classes
     *                  are loaded directly from the class path.
     * @param  parent   the parent classloader.
     * @param  cfg      the Datalift configuration (optional).
     */
    public Bundle(File f, ClassLoader parent, Configuration cfg) {
        super();
        if ((f == null) || (! (f.exists() && f.canRead()))) {
            throw new IllegalArgumentException("f");
        }
        if (parent == null) {
            throw new IllegalArgumentException("parent");
        }
        if (cfg == null) {
            cfg = Configuration.getDefault();
        }
        this.root        = f;
        this.classLoader = new URLClassLoader(extractFiles(f, cfg), parent);
    }

    //-------------------------------------------------------------------------
    // Bundle contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns whether the bundle was loaded from a JAR file.
     * @return <code>true</code> if the bundle was loaded from its
     *         own JAR file; <code>false</code> otherwise.
     */
    public boolean isJar() {
        return ((this.root != null) && (this.root.isFile()));
    }

    /**
     * Loads the implementations of the specified service from this
     * bundle in a fault tolerant manner, i.e. class loading and
     * instantiation errors on the declared service implementations
     * are ignored.
     * @param  service   the service class or interface.
     *
     * @return the service implementations available from this bundle,
     *         empty if none were found.
     * @see    #loadServices(Class, boolean)
     */
    public <S> Collection<S> loadServices(Class<S> service) {
        return this.loadServices(service, true);
    }

    /**
     * Loads the implementations of the specified service from this
     * bundle.
     * @param  service         the service class or interface.
     * @param  faultTolerant   whether to ignore class loading and
     *                         instantiation errors on the declared
     *                         service implementations.
     *
     * @return the service implementations available from this bundle,
     *         empty if none were found.
     * @throws TechnicalException if some declared implementations can
     *         not be loaded and <code>faultTolerant</code> was set to
     *         <code>false</code>.
     */
    public <S> Collection<S> loadServices(Class<S> service,
                                          boolean faultTolerant)
                                                  throws TechnicalException {
        Collection<S> services = new LinkedList<S>();
        // Make a fault-tolerant loading of available implementations.
        Iterator<S> i = ServiceLoader.load(service, this.classLoader)
                                     .iterator();
        boolean hasNext = true;
        do {
            try {
                hasNext = i.hasNext();
                if (hasNext) {
                    services.add(i.next());
                }
            }
            catch (ServiceConfigurationError e) {
                if (faultTolerant) {
                    // Absorb error and skip this implementation...
                    log.warn("Failed to load implementation of {} from {}: {}",
                             e, service.getName(), this, e.getMessage());
                }
                else {
                    // Map ServiceConfigurationError to exception, as explained
                    // in the doc. of ServiceLoader.iterator() on how to
                    // "write robust code"!
                    throw new TechnicalException("service.load.error", e,
                                    service.getName(), e.getMessage(), this);
                }
            }
        }
        while (hasNext);

        return services;
    }

    /**
     * Finds the resource with the given name.
     * @param  name   the resource name.
     *
     * @return a {@link URL} object for reading the resource, or
     *         <code>null</code> if the resource could not be found.
     */
    public URL getResource(String name) {
        return this.classLoader.getResource(name);
    }

    /**
     * Returns an input stream for reading the specified resource.
     * @param  name   the resource name.
     *
     * @return an input stream for reading the resource, or
     *         <code>null</code> if the resource could not be found.
     */
    public InputStream getResourceAsStream(String name) {
        return this.classLoader.getResourceAsStream(name);
    }

    /**
     * Returns the classloader for this bundle.
     * @return the classloader for this bundle.
     */
    public ClassLoader getClassLoader() {
        return this.classLoader;
    }

    /**
     * Returns the file or directory containing the bundle classes and
     * data.
     * @return the file or directory containing the bundle classes and
     *         data or <code>null</code> if the bundle was loaded from
     *         the class path.
     */
    public File getBundleFile() {
        return this.root;
    }

    //-------------------------------------------------------------------------
    // Object contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public String toString() {
        return (this.root != null)? this.root.getName(): "classpath";
    }

    //-------------------------------------------------------------------------
    // Static utility methods
    //-------------------------------------------------------------------------

    /**
     * Analyzes a module structure to match the expected elements and
     * returns the paths to be added to the module classpath.
     * <p>
     * Recognized elements include:</p>
     * <ul>
     *  <li>For JAR files: the JAR file itself.</li>
     *  <li>For directories:
     *   <dl>
     *    <dt><code>/</code></dt>
     *    <dd>The module root directory</dd>
     *    <dt><code>/classes</code></dt>
     *    <dd>The default directory for module classes</dd>
     *    <dt><code>/*.jar</code></dt>
     *    <dd>JAR files containing the module classes</dd>
     *    <dt><code>/lib/**&#47;*.jar</code></dt>
     *    <dd>All the JAR files in the <code>/lib</code> directory tree
     *        (module classes and third-party libraries)</dd>
     *   </dl></li>
     * </ul>
     * @param  path   the directory or JAR file for the module.
     *
     * @return the URLs of the paths to be added to the module
     *         classpath.
     */
    private static URL[] extractFiles(File path, Configuration cfg) {
        List<URL> urls = new LinkedList<URL>();

        String srcName = path.getName();
        if (path.isDirectory()) {
            // Add bundle root directory to the classpath.
            urls.add(getFileUrl(path, srcName));
            // Look for bundle classes as a directory tree.
            File classesDir = new File(path, BUNDLE_CLASSES_DIR);
            if (classesDir.isDirectory()) {
                urls.add(getFileUrl(classesDir, srcName));
            }
            // Add root-level JAR files to the classpath.
            for (File jar : path.listFiles(jarFilter)) {
                urls.add(getFileUrl(jar, srcName));
            }
            // Look for bundle JAR dependencies in the lib directory.
            File libDir = new File(path, BUNDLE_LIB_DIR);
            if (classesDir.isDirectory()) {
                urls.addAll(findFiles(libDir, jarFilter, srcName));
            }
        }
        else {
            // JAR file. => Add the JAR file itself to the classpath.
            urls.add(getFileUrl(path, srcName));
            // Extract wrapped JARs and add them to classpath.
            JarEntry e = null;
            try {
                File tempJarDir = new File(cfg.getTempStorage(),
                                CACHE_DIRECTORY_NAME + '/' + path.getName());
                JarInputStream in = new JarInputStream(
                                                    new FileInputStream(path));
                boolean embeddedJarsFound = false;
                while ((e = in.getNextJarEntry()) != null) {
                    if (e.getName().endsWith(".jar")) {
                        if (embeddedJarsFound == false) {
                            log.debug("Extracting embedded libraries of {}",
                                                                path.getName());
                            embeddedJarsFound = true;
                        }
                        File f = new File(tempJarDir, e.getName());
                        if (! tempJarDir.exists()) {
                            tempJarDir.mkdirs();
                        }
                        long entryDate = e.getTime();
                        if ((! f.exists()) || (f.lastModified() < entryDate)) {
                            FileUtils.save(in, f, false);
                            if (entryDate > 0) {
                                f.setLastModified(entryDate);
                            }
                        }
                        else {
                            // Local cache is already up to date.
                            log.debug("{}/{} is up-to-date.", path.getName(),
                                                              f.getName());
                        }
                        urls.add(f.toURI().toURL());
                    }
                }
            }
            catch (Exception ex) {
                log.warn("Failed to extract embedded JAR {} from {}", ex,
                         e, path);
            }
        }
        return urls.toArray(new URL[urls.size()]);
    }


    /**
     * Scans a directory tree and returns the files matching the
     * specified filter.
     * @param  root     the root of directory tree.
     * @param  filter   the file filter.
     * @param  source   the source package name.
     *
     * @return the URLs of the matched files.
     */
    private static Collection<URL> findFiles(File root,
                                             FileFilter filter, String source) {
        return findFiles(root, filter, source, new LinkedList<URL>());
    }

    /**
     * Recursively scans a directory tree and returns the files
     * matching the specified filter.
     * @param  root      the root of directory tree.
     * @param  filter    the file filter.
     * @param  source    the source package name.
     * @param  results   the collection to append the matched files to.
     *
     * @return the <code>results</code> collection updated with the
     *         matched files.
     */
    private static Collection<URL> findFiles(File root, FileFilter filter,
                                      String source, Collection<URL> results) {
        List<File> dirs = new LinkedList<File>();
        // Scan first-level directory content.
        for (File f : root.listFiles()) {
            if (filter.accept(f)) {
                results.add(getFileUrl(f, source));
            }
            else if ((f.isDirectory()) && (f.canRead())) {
                // Child directory not handled by filter.
                // => Mark for recursive scan.
                dirs.add(f);
            }
            // Else: ignore...
        }
        // Recursively scan child directories.
        for (File child : dirs) {
            findFiles(child, filter, source, results);
        }
        return results;
    }

    /**
     * Returns the URL of the specified file, compliant with the
     * requirements of {@link URLClassLoader#URLClassLoader(URL[])}.
     * @param  f        the file or directory to convert.
     * @param  source   the source package name.
     *
     * @return the URL of the file.
     * @throws TechnicalException if <code>f</code> if neither a
     *         regular file nor a directory.
     */
    private static URL getFileUrl(File f, String source) {
        URL u = null;
        try {
            if (f.isFile()) {
                u = f.toURI().toURL();
            }
            else if (f.isDirectory()) {
                String uri = f.toURI().toString();
                if (! uri.endsWith("/")) {
                    uri += "/";
                }
                u = new URL(uri);
            }
            else {
                throw new TechnicalException("invalid.file.type", f.getPath());
            }
        }
        catch (MalformedURLException e) {
            // Should never happen...
            throw new UnsupportedOperationException(e.getMessage(), e);
        }
        log.trace("Added resource \"{}\" to bundle \"{}\" classpath", u, source);
        return u;
    }
}

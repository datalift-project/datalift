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

package org.datalift.core.velocity.jersey;


import java.io.File;
import java.io.InputStream;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.collections.ExtendedProperties;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.resource.Resource;
import org.apache.velocity.runtime.resource.loader.FileResourceLoader;
import org.apache.velocity.runtime.resource.loader.JarResourceLoader;
import org.apache.velocity.runtime.resource.loader.ResourceLoader;

import static org.datalift.core.velocity.jersey.VelocityTemplateProcessor.*;


/**
 * A {@link ResourceLoader} to load Velocity templates from the static
 * resources of DataLift modules.
 * <p>
 * The Datalift module shall prefix the name of the Velocity templates
 * with the module name in order to avoid conflicts between modules.
 * The module name prefix is used to select the module JAR or directory
 * and is removed from the template name when searching for the
 * template file.</p>
 * <p>
 * For example, for the module named <code>my-module</code> to render
 * a template the path of which is
 * <code>/templates/myTemplate.vm</code> in the module JAR /
 * directory, the name of the template in the Jersey Viewable object
 * shall be: <code>/my-module/templates/myTemplate.vm</code>.</p>
 * <p>
 * Note: the heading slash (&quot;/&quot;) character is also required
 * as it notifies Jersey not to prefix the template name with name of
 * the JAX-RS resource class.</p>
 *
 * @author lbihanic
 */
public final class ModuleResourceLoader extends ResourceLoader
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    private final static String JAR_URL_SCHEME = "jar:file:";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The configured DataLift modules. */
    private final Map<String,ResourceLoader> modules =
                                        new TreeMap<String,ResourceLoader>();

    //-------------------------------------------------------------------------
    // ResourceLoader contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(ExtendedProperties configuration) {
        log.trace("ModuleResourceLoader: initialization starting");

        this.modules.clear();
        // Load module descriptions from loader configuration.
        // The path property format: module1: "path1, module2: path2, ..."
        for (String m : configuration.getStringArray(LOADER_PATH)) {
            // Don't split() on ':' but look for first occurrence as absolute
            // paths on Windoze have a colon character after the drive name.
            int i = m.indexOf(':');
            if ((i > 0) && (i < (m.length() -1))) {
                String name = m.substring(0, i).trim();
                String path = m.substring(i + 1).trim();

                ResourceLoader loader = null;
                // Allocate resource loader for module.
                File f = new File(path);
                ExtendedProperties p = new ExtendedProperties();
                p.addProperty(LOADER_UPD_INTERVAL,
                        String.valueOf(this.getModificationCheckInterval()));
                if (f.isDirectory()) {
                    // Load module resources from a file system directory.
                    p.addProperty(LOADER_PATH, f.getAbsolutePath());
                    p.addProperty(LOADER_BOM_CHECK,
                                  Boolean.TRUE.toString());
                    loader = new FileResourceLoader();
                }
                else {
                    // Load module resources from a JAR file.
                    p.addProperty(LOADER_PATH,
                                  JAR_URL_SCHEME + f.getAbsolutePath());
                    loader = new JarResourceLoader();
                }
                loader.commonInit(rsvc, p);
                loader.init(p);

                log.debug("ModuleResourceLoader: adding module '" + name + '\'');
                this.modules.put(name, loader);
            }
            else {
                log.warn("ModuleResourceLoader: Ignoring invalid module definition: \""
                         + m + '\'');
            }
        }
        log.trace("ModuleResourceLoader: initialization complete");
    }

    /** {@inheritDoc} */
    @Override
    public InputStream getResourceStream(String source)
                                            throws ResourceNotFoundException {
        InputStream src = null;
        // Resolve module loader from template path.
        ResourceDesc desc = new ResourceDesc(source);
        if (desc.loader != null) {
            if (log.isTraceEnabled()) {
                log.trace("ModuleResourceLoader: Resolving '" + desc.path
                          + "' for module '" + desc.module + '\'');
            }
            // Loader found. => Read template data.
            src = desc.loader.getResourceStream(desc.path);
        }
        if ((src == null) && (log.isDebugEnabled())) {
            log.debug("ModuleResourceLoader: Failed to resolve '"
                      + source + '\'');
        }
        return src;
    }

    /** {@inheritDoc} */
    @Override
    public long getLastModified(Resource resource) {
        long timestamp = 0L;
        // Resolve module loader from template path.
        String fullName = resource.getName();
        ResourceDesc desc = new ResourceDesc(fullName);
        if (desc.loader != null) {
            // Loader found. => Delegate request.
            resource.setName(desc.path);
            timestamp = desc.loader.getLastModified(resource);
            resource.setName(fullName);
        }
        return timestamp;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isSourceModified(Resource resource) {
        long fileLastModified = getLastModified(resource);
        if ((fileLastModified == 0) ||
            (fileLastModified != resource.getLastModified())) {
            // File unreachable or changed
            return true;
        }
        return false;
    }

    //-------------------------------------------------------------------------
    // ResourceDesc nested class
    //-------------------------------------------------------------------------

    /**
     * Parses a template path to extract the module name and resolve
     * the associated loader, if any.
     */
    private final class ResourceDesc
    {
        public final String module;
        public final String path;
        public final ResourceLoader loader;

        public ResourceDesc(String path) {
            int start = (path.charAt(0) == '/')? 1: 0;
            int sep   = path.indexOf('/', start);
            this.module = (sep != -1)? path.substring(start, sep): null;
            this.path   = (sep != -1)? path.substring(sep + 1):
                                       path.substring(start);
            this.loader = (this.module != null)? modules.get(this.module): null;
        }
    }
}

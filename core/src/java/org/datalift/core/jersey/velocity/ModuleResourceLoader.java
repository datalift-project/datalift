package org.datalift.core.jersey.velocity;


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

import static org.datalift.core.jersey.velocity.VelocityTemplateProcessor.*;


/**
 * A {@link ResourceLoader} to load Velocity templates from the static
 * resources of DataLift modules.
 *
 * @author lbihanic
 */
public class ModuleResourceLoader extends ResourceLoader
{
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
                                  "jar:file:" + f.getAbsolutePath());
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
            if (log.isDebugEnabled()) {
                log.debug("ModuleResourceLoader: Resolving '" + desc.path
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

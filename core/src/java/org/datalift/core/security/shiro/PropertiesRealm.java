package org.datalift.core.security.shiro;


import java.util.Properties;

import org.datalift.core.ApplicationLoader;
import org.datalift.core.util.VersatileProperties;


/**
 * An extension to Shiro's PropertiesRealm to add the capability of
 * resolving system properties / environment variables in
 * {@link #setResourcePath(String) resource paths}.
 *
 * @author lbihanic
 */
public class PropertiesRealm extends org.apache.shiro.realm.text.PropertiesRealm
{
    /**
     * {@inheritDoc}
     * <p>
     * Adds support for resolving variables (in the form
     * <code>${&lt;var&gt;}</code>) in path value.</p>
     */
    @Override
    public void setResourcePath(String path) {
        final String resourcePathKey = "resourcePath";
        // Use DataLift configuration to resolve potential variable
        // references in resource path definition.
        VersatileProperties props = new VersatileProperties(
                new Properties() {
                    @Override
                    public String getProperty(String key) {
                        return ApplicationLoader.getConfiguration()
                                                .getProperty(key);
                    }
                
                    @Override
                    public String getProperty(String key, String def) {
                        return ApplicationLoader.getConfiguration()
                                                .getProperty(key, def);
                    }
                });
        props.setProperty(resourcePathKey, path);
        // Set the resource path, resolving variable references if any.
        super.setResourcePath(props.getProperty(resourcePathKey));
    }
}

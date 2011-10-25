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

package org.datalift.core.security.shiro;


import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Pattern;

import javax.ws.rs.Path;

import org.apache.shiro.config.Ini;
import org.apache.shiro.config.IniFactorySupport;
import org.apache.shiro.config.Ini.Section;
import org.apache.shiro.util.CollectionUtils;
import org.apache.shiro.web.filter.mgt.DefaultFilter;
import org.apache.shiro.web.servlet.IniShiroFilter;

import static org.apache.shiro.config.IniSecurityManagerFactory.*;
import static org.apache.shiro.web.config.IniFilterChainResolverFactory.*;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.Module;
import org.datalift.fwk.log.Logger;

import static org.datalift.fwk.util.StringUtils.*;


/**
 * An extension to {@link IniShiroFilter} to append the DataLift
 * security properties to the filter Shiro configuration.
 * <p>
 * The DataLift security properties are dynamically build from the
 * the list of {@link Configuration#getBeans(Class) registered}
 * {@link Module modules} and the
 * {@link Module#allowsAnonymousAccess() security}
 * {@link Module#getAuthorizedRoles() requirements} of each module.
 *
 * @see    Module#allowsAnonymousAccess()
 * @see    Module#getAuthorizedRoles()
 *
 * @author lbihanic
 */
public final class ShiroSecurityFilter extends IniShiroFilter
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /**
     * The name of the property defining the default security filters
     * that shall automatically be applied to every DataLift module,
     * e.g. "<code>ssl</code>".
     */
    public final static String SECURITY_DEFAULT_FILTERS_KEY =
                                                    "security.default.filters";

    /**
     * The name of the {@link OptionalBasicHttpAuthFilter} in Shiro
     * configuration.
     */
    private final static String SHIRO_OPT_BASIC_AUTH_FILTER = "optAuthcBasic";
    /** The item separator in Shiro filter list. */
    private final static String SHIRO_FILTER_SEPARATOR      = ", ";
    /**
     * The pattern for extracting Shiro configuration properties from
     * the DataLift configuration.
     */
    private final static Pattern SECURITY_PROPERTIES_EXTRACTOR =
            Pattern.compile("((.+(Realm|Matcher))|securityManager)((\\..+?)*?)",
                            Pattern.CASE_INSENSITIVE);

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // IniShiroFilter contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    protected void configure() throws Exception {
        // IniShiroFilter.configure() start >>>>>
        Ini ini = this.loadIniFromConfig();
        if (CollectionUtils.isEmpty(ini)) {
            log.info("Null or empty configuration specified via 'config' init-param.  " +
                    "Checking path-based configuration.");
            ini = this.loadIniFromPath();
        }
        if (CollectionUtils.isEmpty(ini)) {
            log.info("Null or empty configuration specified via '" + CONFIG_INIT_PARAM_NAME + "' or '" +
                    CONFIG_PATH_INIT_PARAM_NAME + "' filter parameters.  Trying the default " +
                    IniFactorySupport.DEFAULT_INI_RESOURCE_PATH + " file.");
            ini = IniFactorySupport.loadDefaultClassPathIni();
        }
        // <<<< end IniShiroFilter.configure()

        // Append DataLift configuration to the found configuration (if any).
        ini = this.appendDataLiftConfiguration(ini);

        // IniShiroFilter.configure() start >>>>>
        Map<String, ?> objects = applySecurityManager(ini);
        applyFilterChainResolver(ini, objects);
        // <<<< end IniShiroFilter.configure()
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    /**
     * Dynamically builds the DataLift security configuration from the
     * list of registered modules and their security constraints.
     * @param  shiroCfg   the Shiro configuration, as read from the
     *                    filter configuration (web.xml) or filesystem
     *                    or <code>null</code> if non was found.
     *
     * @return the configuration augmented with the DataLift security
     *         properties.
     */
    private Ini appendDataLiftConfiguration(Ini shiroCfg) {
        if (shiroCfg == null) {
            shiroCfg = new Ini();
        }
        final Ini ini = shiroCfg;
        // Ensure [main] section is present.
        Section s = ini.getSection(MAIN_SECTION_NAME);
        if (s == null) {
            s = ini.addSection(MAIN_SECTION_NAME);
        }
        // Append all security properties to [main] section.
        Configuration cfg = Configuration.getDefault();
        for (String p : cfg.getPropertyNames()) {
            if (SECURITY_PROPERTIES_EXTRACTOR.matcher(p).matches()) {
                s.put(p, cfg.getProperty(p));
            }
        }
        // Add Optional HTTP Basic authentication filter to [main] section.
        s.put("optAuthcBasic", OptionalBasicHttpAuthFilter.class.getName());
        // Ensure [urls] section is present.
        s = ini.getSection(URLS);
        if (s == null) {
            s = ini.addSection(URLS);
        }
        // Sort module paths, longest path first, as declaration order matters.
        Map<String,Module> paths = new TreeMap<String,Module>(
                new Comparator<String>() {
                    @Override
                    public int compare(String s1, String s2) {
                        // Order strings: longest path first.
                        int n = this.count('/', s2) - this.count('/', s1);
                        if (n == 0) {
                            // Same path length. => Longest string first.
                            n = s2.length() - s1.length(); 
                        }
                        if (n == 0) {
                            // Same length. => Use alphabetical order.
                            n = s1.compareTo(s2);
                        }
                        return n;
                    }

                    private int count(char c, String s) {
                        int count = 0;
                        for (int i=0, max=s.length(); i<max; i++) {
                            if (s.charAt(i) == c) count++;
                        }
                        return count;
                    }
                });
        // Build module paths, replacing JAX-RS path variables with wildcard.
        for (Module m : Configuration.getDefault().getBeans(Module.class)) {
            Path p = m.getClass().getAnnotation(Path.class);
            String path = (p != null)?
                            p.value().replaceAll("\\{.*}", "*"): // Subst. vars.
                            m.getName();        // Use module name as path.
            // Module URL path = "/<@path value or name>/**".
            StringBuilder buf = new StringBuilder(80);
            if (path.charAt(0) != '/') {
                buf.append('/');
            }
            buf.append(path);
            if (path.charAt(path.length()-1) != '/') {
                buf.append('/');
            }
            path = buf.append("**").toString();
            log.debug("URL {} -> module {}", path, m.getName());
            paths.put(path, m);
        }
        // Get DataLift default security filters (applicable to all modules).
        String defaultFilters = s.get(SECURITY_DEFAULT_FILTERS_KEY);
        defaultFilters = (isBlank(defaultFilters))? "":
                                    SHIRO_FILTER_SEPARATOR + defaultFilters;
        // Configure security filters for all module URLs in [urls] section.
        for (Map.Entry<String,Module> e : paths.entrySet()) {
            String path = e.getKey();
            Module m = e.getValue();
            if (! s.containsKey(path)) {
                // Set authentication filter.
                String filters = ((m.allowsAnonymousAccess())?
                                                SHIRO_OPT_BASIC_AUTH_FILTER:
                                                DefaultFilter.authcBasic.name())
                                    + defaultFilters;
                // Append required roles, if any.
                Collection<String> roles = m.getAuthorizedRoles();
                if ((roles != null) && (! roles.isEmpty())) {
                    filters += SHIRO_FILTER_SEPARATOR
                                        + DefaultFilter.roles.name()
                                        + '[' + join(roles, ", ") + ']';
                }
                s.put(e.getKey(), filters);
            }
            else {
                // Module configuration manually specified.
                // => Do not override.
                log.debug("Custom filter configuration found for module {}: {}",
                          m.getName(), s.get(path));
            }
        }
        log.debug("Starting Shiro security manager with: {}",
                new Object() {
                    @Override
                    public String toString() {
                        StringBuilder buf = new StringBuilder(2048);
                        for (String n : ini.getSectionNames()) {
                            Section s = ini.getSection(n);
                            buf.append('\n').append(n).append(": { ");
                            for (Map.Entry<String,String> p : s.entrySet()) {
                                buf.append("\n\t")
                                   .append(p.getKey()).append(" = \"")
                                   .append(p.getValue()).append('"');
                            }
                            buf.append("\n}");
                        }
                        return buf.toString();
                    }
                });
        return ini;
    }
}

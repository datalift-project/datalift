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

package org.datalift.wrapper;


import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import com.centerkey.utils.BareBonesBrowserLaunch;

import org.datalift.wrapper.PathSpec.Directories;

import jargs.gnu.CmdLineParser;
import jargs.gnu.CmdLineParser.Option;

import static org.datalift.wrapper.OsType.*;
import static org.datalift.wrapper.PathSpec.Directories.*;


/**
 * A wrapper to run DataLift in standalone mode.
 * <p>
 * This wrapper starts a Jetty servlet container and deploys the
 * DataLift web application well as the Sesame RDF engine.</p>
 * <p>
 * This wrapper has no dependency on the DataLift framework,
 * application or third-party libraries.</p>
 *
 * @author lbihanic
 */
public final class Wrapper
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** The default HTTP port for the DataLift standalone server. */
    public final static int DEFAULT_HTTP_PORT = 9091;
    /**
     * The system property/environment variable defining the DataLift
     * working directory.
     */
    public final static String DATALIFT_HOME = "datalift.home";
    /** The system property defining the DataLift installation directory. */
    public final static String DATALIFT_ROOT = "datalift.root";
    /** The system property defining the DataLift listening port. */
    public final static String DATALIFT_PORT = "datalift.port";
    /** The system property defining the location of the DataLift log files. */
    public final static String DATALIFT_LOG_PATH = "datalift.log.path";
    /**
     * The system property/environment variable defining the directory
     * where Sesame stores the persistent repository data.
     */
    private final static String SESAME_HOME =
                                        "info.aduna.platform.appdata.basedir";
    /** The system property defining the location of the Sesame log files. */
    private final static String SESAME_LOG_PATH = "info.aduna.logging.dir";
    /** The system property defining where Jetty is being run. */
    private final static String JETTY_HOME = "jetty.home";

    // Directory paths for OpenRDF Sesame repositories.
    private final static String SESAME_REPOSITORIES_DIR = "repositories";
    private final static String LINUX_REPOSITORIES_PATH =
                            SESAME_REPOSITORIES_DIR + "/openrdf-sesame";
    private final static String OTHER_REPOSITORIES_PATH =
                            SESAME_REPOSITORIES_DIR + "/OpenRDF Sesame";

    private final static String LOCALHOST = "localhost";
    private final static String WAR_EXTENSION = ".war";
    private final static String JAR_EXTENSION = ".jar";

    // Constants for well-known Java system properties.
    private final static String JAVA_CURRENT_DIR_PROP   = "user.dir";
    private final static String JAVA_TEMP_DIR_PROP      = "java.io.tmpdir";

    private final static Comparator<String> CONTEXT_PATH_COMPARATOR =
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
            };

    private final static FileFilter WEB_APP_FILE_FILTER =
            new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return ((f.isDirectory()) ||
                            (f.isFile() && f.getName().toLowerCase()
                                                      .endsWith(WAR_EXTENSION)));
                }
            };

    private final static FileFilter JAR_FILE_FILTER =
            new FileFilter() {
                @Override
                public boolean accept(File f) {
                    return ((f.isDirectory()) ||
                            (f.isFile() && f.getName().toLowerCase()
                                                      .endsWith(JAR_EXTENSION)));
                }
            };

    //-------------------------------------------------------------------------
    // Main method
    //-------------------------------------------------------------------------

    /**
     * The wrapper main method to run Datalift from the command line.
     * <p>
     * Usage: java org.datalift.wrapper.Wrapper [{-e,--external}] 
     *        [{-h,--home-dir} &lt;directory&gt;]
     *        [{-p,--port} &lt;port&gt;]
     *        [{-r,--root-app} &lt;root_webapp_name&gt;]" +
     *        [{-w,--welcome-page} &lt;welcome_page&gt;] [install_dir]</p>
     * @param  args   the command-line arguments (see above).
     *
     * @throws Exception if any error occurred while running the
     *         Datalift application.
     */
    public static void main(String[] args) throws Exception
    {
        File dataliftRoot = null;
        int httpPort = DEFAULT_HTTP_PORT;
        String homeDir = null;
        String rootWebAppPath = "datalift";
        String welcomePage = "";
        // On MAC OS, avoid the annoying popup asking the user to allow
        // the Java app to accept all incoming connections by forcing the
        // HTTP server to listen only to loopback interface.
        boolean loopbackOnly = (CURRENT_OS == MacOS);
        try {
            // Parse command-line arguments.
            CmdLineParser parser  = new CmdLineParser();
            Option portOption     = parser.addIntegerOption('p', "port");
            Option externalOption = parser.addBooleanOption('e', "external");
            Option homeDirOption  = parser.addStringOption('h',  "home-dir");
            Option rootAppOption  = parser.addStringOption('r',  "root-app");
            Option mainPageOption = parser.addStringOption('w',  "welcome-page");
            parser.parse(args);
            // 1. HTTP listening port.
            httpPort = ((Integer)(parser.getOptionValue(portOption,
                                new Integer(httpPort)))).intValue();
            // 2. Network interfaces to listen to.
            loopbackOnly = ! ((Boolean)(parser.getOptionValue(externalOption,
                                new Boolean(! loopbackOnly)))).booleanValue();
            // 3. Datalift home directory.
            homeDir = (String)(parser.getOptionValue(homeDirOption,
                                System.getProperty(DATALIFT_HOME)));
            // 4. Root web application.
            rootWebAppPath = (String)(parser.getOptionValue(rootAppOption,
                                                            rootWebAppPath));
            if (rootWebAppPath.endsWith(WAR_EXTENSION)) {
                rootWebAppPath = rootWebAppPath.substring(0,
                            rootWebAppPath.length() - WAR_EXTENSION.length());
            }
            // 5. Welcome page
            welcomePage = (String)(parser.getOptionValue(mainPageOption,
                                                         welcomePage));
            // Parse other arguments.
            String[] otherArgs = parser.getRemainingArgs();
            // 3. DataLift installation directory.
            String runDir = (otherArgs.length > 0)? otherArgs[0]:
                                    System.getProperty(JAVA_CURRENT_DIR_PROP);
            // Validate installation directory.
            dataliftRoot = new File(runDir);
            if (! ((dataliftRoot.exists()) && (dataliftRoot.isDirectory()))) {
                throw new FileNotFoundException(args[0]);
            }
        }
        catch (Exception e) {
            System.err.println(e.toString());
            System.err.println("Usage: java " + Wrapper.class.getName() +
                               " [{-e,--external}]" +
                               " [{-h,--home-dir} <directory>]" +
                               " [{-p,--port} <port>]" +
                               " [{-r,--root-app} <root_webapp_name>]" +
                               " [{-w,--welcome-page} <welcome_page>]" +
                               " [install_dir]");
            System.exit(2);
        }
        // Check (user-specific) runtime environment.
        PathSpec env = (homeDir == null)?
                            PathSpec.getPathSpec(rootWebAppPath):
                            PathSpec.getPathSpec((".".equals(homeDir))?
                                            dataliftRoot: new File(homeDir));
        try {
            // Install user-specific configuration, if needed.
            installUserEnv(env, dataliftRoot);
        }
        catch (IOException e) {
            System.err.println("Failed to create Datalift user environment (\""
                               + env.getPath() + "\"): " + e.toString());
            // Oops! Can't create a user-specific runtime environment.
            // => Run DataLift at the executable location.
            env = PathSpec.getPathSpec(dataliftRoot);
            installUserEnv(env, dataliftRoot);
        }
        File dataliftHome = env.getPath();
        // Set Datalift runtime configuration properties.
        System.setProperty(DATALIFT_ROOT, dataliftRoot.getCanonicalPath());
        System.setProperty(DATALIFT_HOME, dataliftHome.getCanonicalPath());
        System.setProperty(DATALIFT_PORT, String.valueOf(httpPort));
        // Group all log files in a single location.
        String logPath = env.getPath(LOGS).getCanonicalPath();
        System.setProperty(DATALIFT_LOG_PATH, logPath);
        System.setProperty(SESAME_LOG_PATH, logPath);
        // Set Sesame repositories location.
        if (System.getProperty(SESAME_HOME) == null) {
            File sesameHome = new File(dataliftHome, SESAME_REPOSITORIES_DIR);
            System.setProperty(SESAME_HOME, sesameHome.getCanonicalPath());
        }
        // On Windows and Linux/Gnome 2.x systems, gather proxy configuration
        // from the system (it's the default value for Mac OS X).
        if ((CURRENT_OS != MacOS) &&
            (System.getProperty("http.proxyHost") == null)) {
            // No HTTP proxy explicitly set. => Ask Java to check system config.
            System.setProperty("java.net.useSystemProxies",
                                                    Boolean.TRUE.toString());
        }
        // Set Jetty runtime configuration properties.
        System.setProperty(JETTY_HOME, dataliftRoot.getCanonicalPath());
        System.setProperty(JAVA_TEMP_DIR_PROP,
                                        env.getPath(TEMP).getAbsolutePath());
        // Create Jetty server.
        final Server httpServer = new Server(httpPort);
        httpServer.setSendServerVersion(false);     // No Server HTTP header.
        // Check the network interfaces to listen to.
        if (loopbackOnly) {
            // Listen only to the loopback interface.
            for (Connector c : httpServer.getConnectors()) {
                if (c instanceof SocketConnector) {
                    c.setHost(LOCALHOST);
                }
            }
        }
        // Check installation directory structure.
        File webappDir = new File(dataliftRoot, WEBAPPS.path);
        if (! webappDir.isDirectory()) {
            System.err.println("Invalid directory: \"" + dataliftRoot
                               + "\": not a Datalift installation directory");
            System.exit(2);
        }
        // Sort webapps, longest path first, as declaration order matters.
        Map<String,File> webapps = new TreeMap<String,File>(
                                                    CONTEXT_PATH_COMPARATOR);
        // Search for web applications, both as WARs and directories,
        // in both Datalift installation and user runtime environment.
        findWebApps(webappDir, webapps);
        if (! env.getPath(WEBAPPS).equals(webappDir)) {
            webappDir = env.getPath(WEBAPPS);
            if (webappDir.isDirectory()) {
                findWebApps(webappDir, webapps);
            }
        }
        // Check for root web app, if any.
        webappDir = webapps.remove("/" + rootWebAppPath);
        if (webappDir != null) {
            webapps.put("", webappDir);
        }
        // Check for user-provided JARs to be made available to web apps.
        String extraClasspath = join(
                            getExtraClassPathEntries(env.getPath(LIB)), ";");
        // Register web applications.
        for (Map.Entry<String,File> e : webapps.entrySet()) {
            String path = e.getKey();
            File webapp = e.getValue();

            WebAppContext ctx = new WebAppContext();
            ctx.setContextPath(path);
            ctx.setWar(webapp.getPath());
            if (! webapp.isDirectory()) {
                ctx.setTempDirectory(new File(env.getPath(WORK), path));
            }
            if (extraClasspath.length() != 0) {
                ctx.setExtraClasspath(extraClasspath);
            }
            httpServer.addHandler(ctx);
            System.out.println("Deploying \"" + webapp.getName() +
                               "\" with context path \"" + path + '"');
        }
        // Ensure unused local variables can be garbage-collected.
        env = null;
        webapps = null;
        webappDir = null;
        // Start server.
        httpServer.setStopAtShutdown(true);
        httpServer.start();
        System.out.println("DataLift server started on port " +
                                                Integer.valueOf(httpPort));
        // Open new browser window on user's display.
        BareBonesBrowserLaunch.openUrl(
                    "http://" + LOCALHOST + ':' + httpPort + '/' + welcomePage);
        // Wait for server termination.
        httpServer.join();
        System.exit(0);
    }

    private static Collection<File> getExtraClassPathEntries(File... dirs) {
        Collection<File> extraJars = new LinkedList<File>();
        for (File dir : dirs) {
            for (File f : dir.listFiles(JAR_FILE_FILTER)) {
                extraJars.add(f);
            }
        }
        return extraJars;
    }

    private static void installUserEnv(PathSpec env, File source)
                                                            throws IOException {
        if (env == null) {
            throw new IllegalArgumentException("env");
        }
        for (Directories d : Directories.values()) {
            if (d  != CONFIG) {
                createDirectory(env.getPath(d));
            }
            // Else: CONFIG case is handled below, to copy config. files.
        }
        // Copy runtime templates: configuration...
        File target = env.getPath(CONFIG);
        if (! target.exists()) {
            copy(new File(source, CONFIG.path), target);
        }
        // and empty Sesame repositories.
        target = env.getPath((CURRENT_OS == Other)? LINUX_REPOSITORIES_PATH:
                                                    OTHER_REPOSITORIES_PATH);
        if (! target.exists()) {
            copy(new File(source, OTHER_REPOSITORIES_PATH), target);
        }
    }

    private static boolean createDirectory(File path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path");
        }
        boolean created = false;
        // Create directory if it does not exist.
        if (! path.exists()) {
            if (! path.mkdirs()) {
                reportPathCreationFailure(path);
            }
            created = true;
        }
        // Check that path leads to a write-accessible directory.
        if (! (path.isDirectory() && path.canWrite())) {
            reportPathCreationFailure(path);
        }
        return created;
    }

    private static void copy(File from, File to) throws IOException {
        if ((from == null) || (! from.canRead())) {
            throw new IllegalArgumentException("from");
        }
        if (to == null) {
            throw new IllegalArgumentException("to");
        }
        if (from.isDirectory()) {
            if (! (to.exists() || to.mkdirs())) {
                reportPathCreationFailure(to);
            }
            try {
                for (String f : from.list()) {
                    copy(new File(from, f), new File(to, f));
                }
            }
            catch (IOException e) {
                delete(to);
            }
        }
        else if (from.isFile()) {
            copyFile(from, to);
        }
        // Else: neither a directory nor a regular file. => Ignore...
    }

    private static void copyFile(File from, File to) throws IOException {
        if ((from == null) || (! from.canRead())) {
            throw new IllegalArgumentException("in");
        }
        if (to == null) {
            throw new IllegalArgumentException("out");
        }
        final long chunkSize = 64 * 1024;       // 64 KB
        boolean copyFailed   = false;

        FileInputStream  in  = null;
        FileOutputStream out = null;
        try {
            in  = new FileInputStream(from);
            out = new FileOutputStream(to);
            FileChannel chIn  = in.getChannel();
            FileChannel chOut = out.getChannel();

            long start = 0L;
            long end   = chIn.size();
            while (end != 0L) {
                long l = Math.min(end, chunkSize);
                l = chIn.transferTo(start, l, chOut);
                if (l == 0L) {
                    // Should at least copy one byte!
                    throw new IOException(
                                    "Copy stalled after " + start + " bytes");
                }
                start += l;
                end   -= l;
            }
            chOut.force(true);  // Sync. data to disk.
            chOut.close();      // Errors on close report data write error.
            out.flush();
            out.close();
            out = null;
        }
        catch (IOException e) {
            copyFailed = true;
            throw e;
        }
        finally {
            if (in != null) {
                try { in.close(); } catch (Exception e) { /* Ignore... */ }
            }
            if (out != null) {
                try { out.close(); } catch (Exception e) { /* Ignore... */ }
            }
            if (copyFailed) { to.delete(); }
        }
    }

    private static void delete(File f) {
        if ((f != null) && (f.exists())) {
            if (f.isDirectory()) {
                // Delete children, files and sub-directories.
                for (File e : f.listFiles()) {
                    delete(e);
                }
            }
            // Delete target file or (now empty) directory.
            f.delete();
        }
        // Else: Ignore...
    }

    private static void reportPathCreationFailure(File path)
                                                            throws IOException {
        throw new IOException("Failed to create directory: " + path);
    }

    private static void findWebApps(File dir, Map<String,File> webapps) {
        findWebApps(dir, webapps, dir);
    }

    private static void findWebApps(File dir, Map<String,File> webapps,
                                    File root) {
        for (File f : dir.listFiles(WEB_APP_FILE_FILTER)) {
            if ((f.isDirectory()) && (! new File(f, "WEB-INF").isDirectory())) {
                // Directory found but no WEB-INF sub-directory is present.
                // => Search it for web apps recursively.
                findWebApps(f, webapps, root);
            }
            webapps.put(getContextPath(f, root), f);
        }
    }

    private static String getContextPath(File f, File root) {
        String path = f.getAbsolutePath().substring(
                                        root.getAbsolutePath().length() + 1);
        int i = path.indexOf(WAR_EXTENSION);
        if (i > 0) {
            // Remove ending .war extension.
            path = path.substring(0, i);
        }
        return "/" + path;
    }

    public static String join(Collection<?> c, String sep) {
        if (sep == null) {
            throw new IllegalArgumentException("sep");
        }
        String s = "";
        if ((c != null) && (! c.isEmpty())) {
            StringBuilder sb = new StringBuilder();
            for (Object element : c) {
                if (element != null) {
                    sb.append(element).append(sep);
                }
            }
            if (sb.length() != 0) {
                // Remove last separator
                sb.setLength(sb.length() - sep.length());
                s = sb.toString();
            }
        }
        return s;
    }
}

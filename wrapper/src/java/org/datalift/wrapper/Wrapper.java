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
import java.util.LinkedList;
import java.util.List;

import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.webapp.WebAppContext;

import com.centerkey.utils.BareBonesBrowserLaunch;

import jargs.gnu.CmdLineParser.Option;

import jargs.gnu.CmdLineParser;

import static org.datalift.wrapper.OsType.*;


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
    /** The default HTTP port for the DataLift standalone server. */
    public final static int DEFAULT_HTTP_PORT = 9091;
    /** The DataLift working directory system property/environment variable. */
    public final static String DATALIFT_HOME = "datalift.home";
    /** The system property defining the DataLift installation directory. */
    public final static String DATALIFT_ROOT = "datalift.root";
    /** The system property defining the DataLift listening port. */
    public final static String DATALIFT_PORT = "datalift.port";
    /** The system property defining the location of the DataLift log files. */
    public final static String DATALIFT_LOG_PATH = "datalift.log.path";
    /** The Sesame repository directory system property variable. */
    public final static String SESAME_HOME =
                                        "info.aduna.platform.appdata.basedir";

    private final static String SESAME_REPOSITORIES_DIR = "repositories";
    private final static String WEBAPPS_DIR = "webapps";

    private final static String MAC_DATALIFT_NAME = "DataLift";
    private final static String MAC_APPL_DATA_PATH =
                            "Library/Application Support/" + MAC_DATALIFT_NAME;
    private final static String MAC_APPL_CACHE_PATH =
                            "Library/Caches/" + MAC_DATALIFT_NAME;
    private final static String MAC_APPL_LOGS_PATH =
                            "Library/Logs/" + MAC_DATALIFT_NAME;
    private final static String MAC_WEB_APPS_PATH =
                            MAC_APPL_CACHE_PATH + "/webapps";

    private final static String WIN_DATALIFT_NAME = MAC_DATALIFT_NAME;
    private final static String WIN_APPL_DATA_PATH =
                            "Application Data/" + WIN_DATALIFT_NAME;

    private final static String OTHER_DATALIFT_NAME   = ".datalift";
    private final static String OTHER_APPL_DATA_PATH  = OTHER_DATALIFT_NAME;
    private final static String OTHER_APPL_CACHE_PATH = "temp";
    private final static String OTHER_APPL_LOGS_PATH  = "logs";
    private final static String OTHER_WEB_APPS_PATH   = "work";

    public static void main(String[] args) throws Exception
    {
        File dataliftRoot = null;
        int httpPort = DEFAULT_HTTP_PORT;
        // On MAC OS, avoid the annoying popup asking the user to allow
        // the Java app to accept all incoming connections by forcing the
        // HTTP server to listen only to loopback interface.
        boolean loopbackOnly = (CURRENT_OS == MacOS);
        try {
            // Parse command-line arguments.
            CmdLineParser parser = new CmdLineParser();
            Option portOption = parser.addIntegerOption('p', "port");
            Option externalOption = parser.addBooleanOption('e', "external");
            parser.parse(args);
            // 1. HTTP listening port.
            httpPort = ((Integer)(parser.getOptionValue(portOption,
                                new Integer(httpPort)))).intValue();
            // 2. Network interfaces to listen to.
            loopbackOnly = ! ((Boolean)(parser.getOptionValue(externalOption,
                                new Boolean(! loopbackOnly)))).booleanValue();
            // Parse other arguments.
            String[] otherArgs = parser.getRemainingArgs();
            // 3. DataLift installation directory.
            String runDir = (otherArgs.length > 0)?
                                otherArgs[0]: System.getProperty("user.dir");
            // Validate installation directory.
            dataliftRoot = new File(runDir);
            if (! ((dataliftRoot.exists()) && (dataliftRoot.isDirectory()))) {
                throw new FileNotFoundException(args[0]);
            }
        }
        catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println("Usage: " + Wrapper.class.getSimpleName() +
                               " [{-p,--port} port]" +
                               " [{-e,--external}]" +
                               " [install_dir]");
            System.exit(2);
        }
        System.setProperty(DATALIFT_ROOT, dataliftRoot.getCanonicalPath());
        System.setProperty(DATALIFT_PORT, String.valueOf(httpPort));
        System.setProperty("jetty.home",  dataliftRoot.getCanonicalPath());
        // Check (user-specific) runtime environment.
        String homeDir = System.getProperty(DATALIFT_HOME);
        File dataliftHome = (homeDir == null)? getUserEnv(): new File(homeDir);
        boolean defaultHome = (homeDir == null);
        try {
            // Install user-specific configuration, if needed.
            installUserEnv(dataliftHome, dataliftRoot, defaultHome);
        }
        catch (IOException e) {
            // Oops! Can't create a user-specific runtime environment.
            // => Run DataLift at the executable location.
            dataliftHome = dataliftRoot;
            installUserEnv(dataliftRoot, dataliftRoot, true);
        }
        System.setProperty(DATALIFT_HOME, dataliftHome.getCanonicalPath());
        // Set Sesame repositories location.
        if (System.getProperty(SESAME_HOME) == null) {
            File sesameHome = new File(dataliftHome, SESAME_REPOSITORIES_DIR);
            System.setProperty(SESAME_HOME, sesameHome.getCanonicalPath());
        }
        // Set DataLift log files location.
        File logPath = ((CURRENT_OS == MacOS) && defaultHome)?
                                getUserPath(MAC_APPL_LOGS_PATH):
                                new File(dataliftHome, OTHER_APPL_LOGS_PATH);
        createDirectory(logPath);
        System.setProperty(DATALIFT_LOG_PATH, logPath.getCanonicalPath());
        // Set (and create) Jetty working directory.
        File jettyWorkDir = ((CURRENT_OS == MacOS) && defaultHome)?
                                getUserPath(MAC_WEB_APPS_PATH):
                                new File(dataliftHome, OTHER_WEB_APPS_PATH);
        createDirectory(jettyWorkDir);
        // Set (and create) Jetty temporary directory.
        File tempDir = ((CURRENT_OS == MacOS) && defaultHome)?
                                getUserPath(MAC_APPL_CACHE_PATH):
                                new File(dataliftHome, OTHER_APPL_CACHE_PATH);
        createDirectory(tempDir);
        System.setProperty("java.io.tmpdir", tempDir.getAbsolutePath());
        // Create Jetty server.
        final Server httpServer = new Server(httpPort);
        httpServer.setSendServerVersion(false);     // No Server HTTP header.
        if (loopbackOnly) {
            for (Connector c : httpServer.getConnectors()) {
                if (c instanceof SocketConnector) {
                    c.setHost("localhost");
                }
            }
        }
        // Register web applications.
        FileFilter webappFilter = new FileFilter() {
                public boolean accept(File f) {
                    return ((f.isDirectory()) ||
                            (f.isFile() && (f.getName().endsWith(".war"))));
                }
            };
        List<File> webapps = new LinkedList<File>();
        for (File f : new File(dataliftRoot, WEBAPPS_DIR).listFiles(webappFilter)) {
            webapps.add(f);
        }
        for (File f : new File(dataliftHome, WEBAPPS_DIR).listFiles(webappFilter)) {
            webapps.add(f);
        }
        for (File webapp : webapps) {
            WebAppContext ctx = new WebAppContext();
            String path = webapp.getName();
            int i = path.indexOf(".war");
            if (i > 0) {
                // Remove ending .war extension.
                path = path.substring(0, i);
            }
            ctx.setContextPath("/" + path);
            ctx.setWar(webapp.getPath());
            if (! webapp.isDirectory()) {
                ctx.setTempDirectory(new File(jettyWorkDir, path));
            }
            httpServer.addHandler(ctx);
            System.out.println(webapp.getName() + " deployed as: " + path);
        }
        // Start server.
        httpServer.setStopAtShutdown(true);
        httpServer.start();
        System.out.println("DataLift server started on port " +
                                                Integer.valueOf(httpPort));
        // Open new browser window on user's display.
        BareBonesBrowserLaunch.openUrl(
                        "http://localhost:" + httpPort + "/datalift/sparql");
        // Wait for server termination.
        httpServer.join();
        System.exit(0);
    }

    private static File getUserEnv() {
        // Build user-specific DataLift execution environment path
        // depending on local OS type.
        File userEnv = null;
        if (CURRENT_OS == Windows) {
            String appDataPath = System.getenv("APPDATA");
            userEnv = (appDataPath != null)?
                            new File(new File(appDataPath), WIN_DATALIFT_NAME):
                            getUserPath(WIN_APPL_DATA_PATH);
        }
        else {
            userEnv = getUserPath((CURRENT_OS == MacOS)? MAC_APPL_DATA_PATH:
                                                         OTHER_APPL_DATA_PATH);
        }
        return userEnv;
    }

    private static File getUserPath(String path) {
        if (path == null) {
            throw new IllegalArgumentException("path");
        }
        if (path.charAt(0) == '/') {
            // Path shall be relative to user home directory.
            path = path.substring(1);
        }
        return new File(new File(System.getProperty("user.home")), path);
    }

    private static void installUserEnv(File path,
                                       File source, boolean defaultHome)
                                                            throws IOException {
        if (path != null) {
            createDirectory(path);
            // Create working directory, if they do not exist yet...
            createDirectory(new File(path, "modules"));
            createDirectory(new File(path, "storage/public"));
            createDirectory(new File(path, WEBAPPS_DIR));
            if (! ((CURRENT_OS == MacOS) && defaultHome)) {
                createDirectory(new File(path, "logs"));
                createDirectory(new File(path, "temp"));
            }

            if ((source != null) && (! source.equals(path))) {
                // Copy runtime templates: configuration...
                File target = new File(path, "conf");
                if (! target.exists()) {
                    copy(new File(source, "conf"), target);
                }
                // and empty Sesame repositories.
                target= new File(path, SESAME_REPOSITORIES_DIR);
                if (! target.exists()) {
                    copy(new File(source, SESAME_REPOSITORIES_DIR), target);
                }
            }
        }
        // Else: ignore...
    }

    private static void createDirectory(File path) throws IOException {
        if (path == null) {
            throw new IllegalArgumentException("path");
        }
        // Create directory if it does not exist.
        if (! (path.exists() || path.mkdirs())) {
            reportPathCreationFailure(path);
        }
        // Check that path leads to a write-accessible directory.
        if (! (path.isDirectory() && path.canWrite())) {
            reportPathCreationFailure(path);
        }
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

    public final static void copyFile(File in, File out) throws IOException {
        if ((in == null) || (! in.canRead())) {
            throw new IllegalArgumentException("in");
        }
        if (out == null) {
            throw new IllegalArgumentException("out");
        }
        final long chunkSize = 64 * 1024;       // 64 KB
        boolean copyFailed   = false;

        FileChannel chIn  = null;
        FileChannel chOut = null;
        try {
            chIn  = new FileInputStream(in).getChannel();
            chOut = new FileOutputStream(out).getChannel();

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
        }
        catch (IOException e) {
            copyFailed = true;
            throw e;
        }
        finally {
            if (chIn != null) {
                try { chIn.close(); } catch (Exception e) { /* Ignore... */ }
            }
            if (copyFailed) { out.delete(); }
        }
    }

    public final static void delete(File f) {
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
}

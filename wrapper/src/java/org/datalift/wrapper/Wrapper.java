
package org.datalift.wrapper;


import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import com.centerkey.utils.BareBonesBrowserLaunch;


public final class Wrapper
{
    /** The default HTTP port for the DataLift standalone server. */
    public final static int DEFAULT_HTTP_PORT = 9091;
    /** The DataLift working directory system property/environment variable. */
    public final static String DATALIFT_HOME = "datalift.home";
    /** The system property defining the DataLift installation directory. */
    public final static String DATALIFT_ROOT = "datalift.root";
    /** The Sesame repository directory system property variable. */
    public final static String SESAME_HOME =
                                        "info.aduna.platform.appdata.basedir";
    private final static String SESAME_REPOSITORIES_DIR = "repositories";

    private final static String MAC_OSX_OS_NAME = "Mac OS";
    private final static String WINDOWS_OS_NAME = "Windows";

    public static void main(String[] args) throws Exception
    {
        // Check command-line arguments:
        // 1. DataLift installation directory.
        String runDir = System.getProperty("user.dir");
        if (args.length > 0) {
            runDir = args[0];
        }
        File dataliftRoot = new File(runDir);
        if (! ((dataliftRoot.exists()) && (dataliftRoot.isDirectory()))) {
            throw new FileNotFoundException(args[0]);
        }
        System.setProperty(DATALIFT_ROOT, dataliftRoot.getCanonicalPath());
        // 2. HTTP listening port.
        int httpPort = DEFAULT_HTTP_PORT;
        if (args.length > 1) {
            try {
                httpPort = Integer.parseInt(args[1]);
            }
            catch (Exception e) { /* Ignore... */ }
        }
        // Check (user-specific) runtime environment.
        String homeDir = System.getProperty(DATALIFT_HOME);
        File dataliftHome = (homeDir == null)? getUserEnv(): new File(homeDir);
        try {
            // Install user-specific configuration, if needed.
            installUserEnv(dataliftHome, dataliftRoot);
        }
        catch (IOException e) {
            // Oops! Can't create a user-specific runtime environment.
            // => Run DataLift at the executable location.
            dataliftHome = dataliftRoot;
            installUserEnv(dataliftRoot, dataliftRoot);
        }
        System.setProperty(DATALIFT_HOME, dataliftHome.getCanonicalPath());
        // Set Sesame repositories location.
        if (System.getProperty(SESAME_HOME) == null) {
            File sesameHome = new File(dataliftHome, SESAME_REPOSITORIES_DIR);
            System.setProperty(SESAME_HOME, sesameHome.getCanonicalPath());
        }
        // Set (and create) Jetty working directory.
        System.setProperty("jetty.home", dataliftRoot.getPath());
        File jettyWorkDir = new File(dataliftHome, "work");
        jettyWorkDir.mkdirs();
        // Set (and create) Jetty temporary directory.
        File jettyTempDir = new File(dataliftHome, "temp");
        jettyTempDir.mkdirs();
        System.setProperty("java.io.tmpdir", jettyTempDir.getAbsolutePath());
        // Create Jetty server.
        final Server httpServer = new Server(httpPort);
        // Register web applications.
        FileFilter webappFilter = new FileFilter() {
                public boolean accept(File f) {
                    return ((f.isDirectory()) ||
                            (f.isFile() && (f.getName().endsWith(".war"))));
                }
            };
        File webappDir = new File(dataliftRoot, "webapps");
        for (File webapp : webappDir.listFiles(webappFilter)) {
            WebAppContext ctx = new WebAppContext();
            String path = webapp.getName();
            int i = path.indexOf('.');
            if (i > 0) {
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
        // Build user-specific DataLift configuration path
        // depending on local OS type.
        String userConfigPath = ".datalift";

        String osName = System.getProperty("os.name");
        if (osName.startsWith(MAC_OSX_OS_NAME)) {
            userConfigPath = "Library/Application Support/DataLift";
        }
        else if (osName.startsWith(WINDOWS_OS_NAME)) {
            userConfigPath = "Application Data/DataLift";
        }
        // Else: Assume Unix or Linux...

        return new File(new File(System.getProperty("user.home")),
                        userConfigPath);
    }

    private static void installUserEnv(File path, File source)
                                                            throws IOException {
        if (path != null) {
            createDirectory(path);
            // Create working directory, if they do not exist yet...
            createDirectory(new File(path, "logs"));
            createDirectory(new File(path, "modules"));
            createDirectory(new File(path, "storage/public"));
            createDirectory(new File(path, "temp"));
            createDirectory(new File(path, "work"));

            if ((source != null) && (! source.equals(path))) {
                // Copy runtime templates: configuration...
                copy(new File(source, "conf"), new File(path, "conf"));
                // and empty Sesame repositories.
                copy(new File(source, SESAME_REPOSITORIES_DIR),
                     new File(path,   SESAME_REPOSITORIES_DIR));
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

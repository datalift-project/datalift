
package org.datalift.wrapper;


import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;

import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;

import com.centerkey.utils.BareBonesBrowserLaunch;


public final class Wrapper
{
    /** The default HTTP port for the DataLift standalone server. */
    public final static int DEFAULT_HTTP_PORT = 9091;
    /** The DataLift working directory system property/environment variable. */
    public final static String DATALIFT_HOME = "datalift.home";
    /** The Sesame repository directory system property variable. */
    public final static String SESAME_HOME =
                                        "info.aduna.platform.appdata.basedir";
    private final static String SESAME_REPOSITORIES_DIR = "repositories";

    public static void main(String[] args) throws Exception
    {
        // DataLift home directory
        String runDir = System.getProperty("user.dir");
        if (args.length > 0) {
            runDir = args[0];
        }
        File dataliftHome = new File(runDir);
        if (! ((dataliftHome.exists()) && (dataliftHome.isDirectory()))) {
            throw new FileNotFoundException(args[0]);
        }
        if (System.getProperty(DATALIFT_HOME) == null) {
            System.setProperty(DATALIFT_HOME, dataliftHome.getCanonicalPath());
        }
        if (System.getProperty(SESAME_HOME) == null) {
            System.setProperty(SESAME_HOME, dataliftHome.getCanonicalPath()
                                            + '/' + SESAME_REPOSITORIES_DIR);
        }
        // HTTP listening port
        int httpPort = DEFAULT_HTTP_PORT;
        if (args.length > 1) {
            try {
                httpPort = Integer.parseInt(args[1]);
            }
            catch (Exception e) { /* Ignore... */ }
        }
        // Set (and create) Jetty working directory.
        System.setProperty("jetty.home", dataliftHome.getPath());
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
        File webappDir = new File(dataliftHome, "webapps");
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
}

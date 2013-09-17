package org.datalift.wrapper;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;


/**
 * A utility class to wrap OS-specific distribution of application data
 * files. When an application creates a per-user runtime environment,
 * some operating systems (Windows, Mac OS X) expect this environment
 * to have a precise structure.
 *
 * @author lbihanic
 */
public class PathSpec
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    /** Pre-defined directory names, common to most JEE app. servers. */
    public enum Directories
    {
        CONFIG("conf"),
        LIB("lib"),
        LOGS("logs"),
        MODULES("modules"),
        STORAGE("storage/public"),
        TEMP("temp"),
        WEBAPPS("webapps"),
        WORK("work");

        public final String path;

        Directories(String path) {
            if ((path == null) || (path.trim().length() == 0)) {
                throw new IllegalArgumentException("path: " + path);
            }
            this.path = path;
        }
    }

    // The Java system property containing the user's home directory path.
    private final static String JAVA_HOME_DIR_PROP      = "user.home";
    // Mac-specific user runtime environment location and structure.
    private final static String MAC_APPL_DATA_PATH      =
                                                "Library/Application Support";
    private final static String MAC_APPL_CACHE_PATH     = "Library/Caches";
    private final static String MAC_APPL_LOGS_PATH      = "Library/Logs";
    // Windows-specific user runtime environment location.
    private final static String WIN_APPL_DATA_ENV_VAR   = "APPDATA";
    private final static String WIN_APPL_DATA_PATH      = "Application Data";

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    /** The runtime environment root path. */
    private final File basePath;
    /** The registered runtime environment paths. */
    private final Map<String,File> paths = new HashMap<String,File>();

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new runtime environment for the specified application
     * name in the user's home directory.
     * @param  appName   the application name.
     *
     * @throws IOException if any error occurred while creating
     *         the runtime environment.
     */
    private PathSpec(String appName) throws IOException {
        this(getUserDir(), appName);
    }

    /**
     * Creates a new runtime environment for the specified application
     * name in the specified directory.
     * @param  basePath   the base directory.
     * @param  appName    the application name.
     *
     * @throws IOException if any error occurred while creating
     *         the runtime environment.
     */
    private PathSpec(File basePath, String appName) throws IOException {
        this.basePath = (appName == null)? basePath:
                                           new File(basePath, appName);
        if (! (this.basePath.isDirectory() && this.basePath.canWrite())) {
            if (! this.basePath.mkdirs()) {
                throw new FileNotFoundException(this.basePath.toString());
            }
        }
    }

    //-------------------------------------------------------------------------
    // PathSpec contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the runtime environment root path.
     * @return the runtime environment root path.
     */
    public File getPath() {
        return this.basePath;
    }

    /**
     * Return the directory path associated to the specified name in
     * the runtime environment. If <code>key</code> was not previously
     * registered, the resulting directory is a sub-directory of the
     * runtime environment named <code>key</code>. If the directory
     * does not exist, it is <strong>not</strong> created.
     * @param  key   the path name, as a predefined key.
     *
     * @return the directory path associated to the specified name
     */
    public File getPath(Directories key) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        return this.getPath(key.path);
    }

    /**
     * Return the directory path associated to the specified name in
     * the runtime environment. If <code>key</code> was not previously
     * registered, the resulting directory is a sub-directory of the
     * runtime environment named <code>key</code>. If the directory
     * does not exist, it is <strong>not</strong> created.
     * @param  key   the path name.
     *
     * @return the directory path associated to the specified name
     */
    public File getPath(String key) {
        File f = this.paths.get(key);
        if ((f == null) && (key != null) && (key.trim().length() != 0)) {
            f = new File(this.basePath, key);
            this.paths.put(key, f);
        }
        return f;
    }

    /**
     * Associates (or removes) the specified path with the specified
     * name.
     * @param key    the path name, as a predefined key. 
     * @param path   the path to register. If <code>path</code> is
     *               <code>null</code>, the entry is removed.
     */
    protected final void register(Directories key, File path) {
        if (key == null) {
            throw new IllegalArgumentException("key");
        }
        this.paths.put(key.path, path);
    }

    /**
     * Associates (or removes) the specified path with the specified
     * name.
     * @param key    the path name.
     * @param path   the path to register. If <code>path</code> is
     *               <code>null</code>, the entry is removed.
     */
    protected final void register(String key, File path) {
        if ((key == null) || (key.length() == 0)) {
            throw new IllegalArgumentException("key");
        }
        if (path != null) {
            this.paths.put(key, path);
        }
        else {
            this.paths.remove(key);
        }
    }

    //-------------------------------------------------------------------------
    // Factory methods
    //-------------------------------------------------------------------------

    /**
     * Returns the user's runtime environment for the specified
     * application name and current operating system.
     * @param  appName   the application name.
     *
     * @return the user's runtime environment for the specified
     *         application name.
     * @throws IOException if any error occurred while creating
     *         the runtime environment.
     */
    public static PathSpec getPathSpec(String appName) throws IOException {
        return getPathSpec(appName, OsType.CURRENT_OS);
    }

    /**
     * Returns the user's runtime environment for the specified
     * application name and operating system type.
     * @param  appName   the application name.
     * @param  os        the operating system type.
     *
     * @return the user's runtime environment for the specified
     *         application name.
     * @throws IOException if any error occurred while creating
     *         the runtime environment.
     */
    public static PathSpec getPathSpec(String appName, OsType os)
                                                            throws IOException {
        PathSpec p = null;
        // OS-specific runtime environment structure definition
        switch (os) {
            // On Mac OS X, well-behaved apps split files in various
            // sub-directories of the user's Library folder
            // (Application Support, Caches, Logs...).
            case MacOS:
                p = new MasOsPathSpec(appName);
                break;

            // On Windows, well-behaved apps place their user-specific
            // configuration and runtime data in the directory pointed to
            // by the %APPDATA% env. variable or, if it is not defined, in
            // "Application Data" under the user's home directory.
            case Windows:
                p = new PathSpec(getWindowsBasePath(), capitalize(appName));
                break;

            // For Linux and UNIX systems, use ~/.datalift.
            default:
                p = new PathSpec("." + appName.toLowerCase());
                break;
        }
        return p;
    }

    /**
     * Returns a runtime environment wrapping the specified directory.
     * @param  path   the runtime environment root directory.
     *
     * @return a runtime environment wrapping the specified directory.
     * @throws IOException if any error occurred while creating
     *         the runtime environment.
     */
    public static PathSpec getPathSpec(File path) throws IOException {
        return new PathSpec(path, null);
    }

    //-------------------------------------------------------------------------
    // Utility methods
    //-------------------------------------------------------------------------

    private static File getUserDir(String... paths) {
        // Retrieve the user's home directory path.
        File f = new File(System.getProperty(JAVA_HOME_DIR_PROP));
        if (! (f.isDirectory() && f.canWrite())) {
            throw new IllegalArgumentException(
                                    new FileNotFoundException(f.getPath()));
        }
        // Append request sub-directory paths.
        if ((paths != null) && (paths.length != 0)) {
            StringBuilder sb = new StringBuilder();
            for (String p : paths) {
                if ((p != null) && (p.length() != 0)) {
                    if (p.charAt(0) == '/') {
                        // Path shall be relative to the base directory.
                        p = p.substring(1);
                    }
                    sb.append(p);
                    if (! p.endsWith(File.separator)) {
                        sb.append(File.separator);
                    }
                }
            }
            sb.setLength(sb.length() - 1);
            f = new File(f, sb.toString());
        }
        return f;
    }

    /**
     * Returns the base directory path for application data on Windows
     * (XP, Vista, Seven...) systems.
     * @return the base directory path for application data on Windows.
     */
    private static File getWindowsBasePath() {
        String appDataPath = System.getenv(WIN_APPL_DATA_ENV_VAR);
        return (appDataPath != null)? new File(appDataPath):
                                      getUserDir(WIN_APPL_DATA_PATH);
    }

    /**
     * Capitalizes the specified string.
     * @param  s   the string to capitalize.
     *
     * @return the capitalized string.
     */
    private static String capitalize(String s) {
        if (s != null) {
            StringBuilder b = new StringBuilder(s.length());
            boolean toCap = true;
            for (char c : s.toCharArray()) {
                if (Character.isWhitespace(c)){
                    toCap = true;
                }
                else if (toCap) {
                    c = Character.toUpperCase(c);
                    toCap = false;
                }
                b.append(c);
            }
            s = b.toString();
        }
        return s;
    }

    //-------------------------------------------------------------------------
    // MasOsPathSpec sub-class
    //-------------------------------------------------------------------------

    /**
     * A {@link PathSpec} subclass to implement (Mac) OS X specific
     * distribution of application data in the user's Library folder.
     * A sub-directory named from the application will be created in
     * Application Support, Logs and Caches.
     */
    private final static class MasOsPathSpec extends PathSpec
    {
        public MasOsPathSpec(String appName) throws IOException {
            // Put application data files in Library/Application Support.
            super(getUserDir(MAC_APPL_DATA_PATH), capitalize(appName));
            // Put temporary files under Library/Caches.
            this.register(Directories.TEMP,
                    getUserDir(MAC_APPL_CACHE_PATH, this.getPath().getName()));
            // Put log files under Library/Logs.
            this.register(Directories.LOGS,
                    getUserDir(MAC_APPL_LOGS_PATH, this.getPath().getName()));
            // Uncompress webapp WARs in Library/Caches.
            this.register(Directories.WORK,
                          new File(this.getPath(Directories.TEMP),
                                   Directories.WEBAPPS.path));
        }
    }
}

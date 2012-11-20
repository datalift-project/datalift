package org.datalift.geoconverter.usgs.rdf.util;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

import org.datalift.fwk.log.Logger;


public class ConfigFinder
{
    private static ThreadLocal<ConfigFinder> finder = new ThreadLocal<ConfigFinder>()
            {
                @Override
                protected ConfigFinder initialValue() {
                    return new ConfigFinder();
                }
            };
    private final static Logger log = Logger.getLogger();

    private final Collection<File> paths = new LinkedList<File>();

    public File find(String name) {
        File target = null;
        log.debug("Looking for \"{}\"...", name);
        for (File dir : this.paths) {
            File f = new File(dir, name);
            if (f.exists() && f.canRead()) {
                target = f;
                break;
            }
        }
        log.debug("Found file: {}", target);
        return target;
    }

    public static File findFile(String name) {
        return finder.get().find(name);
    }
    public static void setPaths(Collection<File> l) {
        ConfigFinder f = finder.get();
        f.paths.clear();
        f.paths.addAll(l);
    }
    public static Collection<File> getPaths() {
        return finder.get().paths;
    }
}

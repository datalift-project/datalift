package org.datalift.geoconverter.usgs.gml2rdf.gui;

import java.io.File;
/**
 * Filter class with specialized filters for GML and configuration files.
 * @author Andrew Bulen
 */
public class Filters {

	/** FileFilter for selecting only GML files and folders. */
	public static final javax.swing.filechooser.FileFilter gmlFilter =
		new javax.swing.filechooser.FileFilter() {
	    public final boolean accept(final File file) {
	        if (file.getName().contains(".gml") || file.isDirectory()
	        	|| file.getName().contains(".xml")) {
	        	return true;
	        } else {
	        	return false;
	        }
	    }
		public final String getDescription() {
			return "GML Files and Directories";
		}
	};
	/** FileFilter for selecting only configuration files. */
	public static final javax.swing.filechooser.FileFilter configFileFilter =
		new javax.swing.filechooser.FileFilter() {
		public final boolean accept(final File file) {
			return (file.getName().endsWith(".conf")
					|| file.isDirectory());
		}
		public final String getDescription() {
			return "Configuration Files";
		}
	};
	/** FilenameFilter for choosing configuration files. */
	public static final java.io.FilenameFilter configFilter =
		new java.io.FilenameFilter() {
			public final boolean accept(final File dir,
					final String name) {
				return name.endsWith(".conf");
			}
	};
	/** FilenameFilter for choosing GML files. */
	public static final java.io.FilenameFilter gmlFileFilter =
		new java.io.FilenameFilter() {
			public final boolean accept(final File dir,
					final String name) {
			return (name.endsWith(".gml") || name.endsWith(".xml"));
		}
	};
}

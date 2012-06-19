package org.datalift.geoconverter.usgs.gml2rdf.gui;

import java.io.File;
import javax.swing.JFileChooser;


/**
 * Java File Chooser for selecting only GML Files and folders.
 * @author Andrew Bulen
 */
public class GMLFileChooser extends JFileChooser {

	/**
	 * constructor
	 * initializes the file chooser to select only GML files
	 * and directories.
	 */
	public GMLFileChooser() {
		setFileFilter(Filters.gmlFilter);
		setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		setDialogTitle("Select GML file or directory of GML files");
	}
	/**
	 * displays the file chooser GUI and returns the selected file.
	 * @return selected file or Exception upon cancel or window close
	 * @throws Exception invalid file selection
	 */
	public final File getFile() throws Exception {
		int retVal = showOpenDialog(null);
		if (retVal == JFileChooser.APPROVE_OPTION) {
			return getSelectedFile();
		} else {
			throw new Exception("Selection Error: " + retVal);
		}
	}
}

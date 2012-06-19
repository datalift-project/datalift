package org.datalift.geoconverter.usgs.gml2rdf.gui;

import java.io.File;
import javax.swing.JFileChooser;

/**
 * Java File Chooser set to select only directories.
 * @author Andrew Bulen
 */
public class DirectoryChooser extends JFileChooser {
	/**
	 * constructor.
	 * initializes the file chooser to select only directories.
	 */
	public DirectoryChooser() {
		setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		setDialogTitle("Select Output Directory");
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

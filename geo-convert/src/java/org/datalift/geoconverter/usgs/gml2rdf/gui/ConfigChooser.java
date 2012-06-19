package org.datalift.geoconverter.usgs.gml2rdf.gui;

import java.io.File;

import javax.swing.JFileChooser;
/**
 * Configuration File Chooser.
 * Graphical directory viewer that displays only valid configuration
 * file types.
 * @author Andrew Bulen
 */
public class ConfigChooser extends JFileChooser {
	/**
	 * default constructor.
	 * initializes the file chooser to display only directories and
	 * configuration files.
	 */
	public ConfigChooser() {
		setFileFilter(org.datalift.geoconverter.usgs.gml2rdf.gui.Filters.configFileFilter);
		setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		setDialogTitle("Select Configuration File");
	}
	/**
	 * displays the file chooser GUI and returns the selected file.
	 * @return selected file or Exception upon cancel or window close
	 * @throws Exception invalid file selected
	 */
	public final File getFile() throws Exception{
		int retVal = showOpenDialog(null);
		if (retVal == JFileChooser.APPROVE_OPTION) {
			return getSelectedFile();
		} else {
			throw new Exception("Selection Error: " + retVal);
		}
	}
}

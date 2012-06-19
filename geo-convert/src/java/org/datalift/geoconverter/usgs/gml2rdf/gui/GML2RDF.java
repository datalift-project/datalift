package org.datalift.geoconverter.usgs.gml2rdf.gui;


import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.WindowConstants;

import org.datalift.geoconverter.usgs.rdf.util.GMLConverter;



/**
 * Main GUI class for the GML to RDF conversion program.
 * creates a Graphical User Interface that allows the user to select:
 * 	- input and output locations
 * 	- which parsers to run (feature, GML, geometry, spatial relations)
 * 	- configuration files for editing
 * Also runs the GMLConverter functions and displays any output from the
 * conversion to the console pane
 * @author Andrew Bulen
 */
public class GML2RDF {
	// gui variables
	/** frame containing the conversion GUI. */
	private JFrame m_gui;
	/** content panel of the GUI. */
	private Container m_c;
	/** Buttons for Running the program. */
	private JButton m_GMLSelect, m_N3Select, m_Convert, m_EditConfig;
	/** Input file and Output Directory Text Fields. */
	private JTextField m_GMLFile, m_N3Directory;
	/** Text area for displaying any printed output. */
	private JTextArea m_Console;
	/** Label for Check boxes. */
	private JTextField m_checkLabel =
		new JTextField("Select which properties to convert");
	/** Check boxes for determining which aspects to convert. */
	private JCheckBox m_GMLCheck, m_FeatureCheck,
					m_GeometryCheck, m_RelationCheck;
	/** boolean values associated with check boxes. */
	private boolean m_cGML=true, m_cFeatures=true,
				m_cGeometry=true, m_cRelation=true;
	/** Pane containing the Console. */
	private JScrollPane m_consolePane;
	/** printstream used to print output to Console area. */
	private PrintStream m_cPrintStream;
	/** layout of the GUI. */
	private SpringLayout m_sLayout = new SpringLayout();
	/** position and size of the GUI. */
	private int m_x=100, m_y=100, m_w=525, m_h=400;
	/** height of each line in the GUI. */
	private int m_lineHeight = 25;
	/** width of Check box fields. */
	private int m_checkWidth = 150;
	/** border between GUI objects. */
	private int m_border = 10;
	// Converter variables
	/** file chooser for selecting input GML Files. */
	private GMLFileChooser m_gc = new GMLFileChooser();
	/** directory chooser for selecting output N3 directory. */
	private DirectoryChooser m_n3c = new DirectoryChooser();
	/** default directory selected by file chooser. */
	private String m_currentDir = "./sampleGML";
	/** input GML File and output N3 directory. */
	private File m_gmlFile = null, m_n3Dir = null;
	/**
	 * main function
	 * creates the GML2RDF GUI and initializes the JFrame.
	 * @param args command line inputs
	 */
	public static void main(final String[] args) {
		GML2RDF converterGUI = new GML2RDF();
		JFrame frame = converterGUI.init();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
	}
	/**
	 * default constructor
	 * creates the GUI Frame and initializes the size, and layout.
	 */
	public GML2RDF() {
		m_gui = new JFrame("GML to RDF Converter");
		m_gui.setBounds(m_x, m_y, m_w, m_h);
		m_c = m_gui.getContentPane();
		m_c.setLayout(m_sLayout);
	}
	/**
	 * initializes the GUI and returns the JFrame object.
	 * @return initialized JFrame
	 */
	public final JFrame init() {
		this.initButtons();
		this.initTxtAreas();
		this.initCheckBoxes();
		this.initFrame();
		this.initLayout();
		m_cPrintStream = new PrintStream(new FilteredStream(
				new ByteArrayOutputStream()));
		System.setOut(m_cPrintStream);
		m_gui.setVisible(true);
		return m_gui;
	}
	/** adds all objects to the content Panel of the Frame. */
	private void initFrame() {
		m_c.add(m_GMLFile);
		m_c.add(m_GMLSelect);
		m_c.add(m_N3Directory);
		m_c.add(m_N3Select);
		m_c.add(m_Convert);
		m_c.add(m_consolePane);
		m_c.add(m_checkLabel);
		m_checkLabel.setEditable(false);
		m_checkLabel.setBorder(BorderFactory.createEmptyBorder());
		m_c.add(m_GMLCheck);
		m_c.add(m_FeatureCheck);
		m_c.add(m_GeometryCheck);
		m_c.add(m_RelationCheck);
		m_c.add(m_EditConfig);
	}
	/** sets up the visual layout of the Frame. */
	private void initLayout() {
		// position GMLFile textField
		m_sLayout.putConstraint(SpringLayout.WEST, m_GMLFile,
				m_border, SpringLayout.WEST, m_c);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_GMLFile,
				(m_h/10), SpringLayout.NORTH, m_c);
		// position GMLSelect Button
		m_sLayout.putConstraint(SpringLayout.WEST, m_GMLSelect,
				m_border, SpringLayout.EAST, m_GMLFile);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_GMLSelect,
				(m_h/10), SpringLayout.NORTH, m_c);
		// position N3Directory textField
		m_sLayout.putConstraint(SpringLayout.WEST, m_N3Directory,
				m_border, SpringLayout.WEST, m_c);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_N3Directory,
				m_border, SpringLayout.SOUTH, m_GMLFile);
		// position N3Select Button
		m_sLayout.putConstraint(SpringLayout.WEST, m_N3Select,
				m_border, SpringLayout.EAST, m_N3Directory);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_N3Select,
				m_border, SpringLayout.SOUTH, m_GMLSelect);
		// position Convert Button
		m_sLayout.putConstraint(SpringLayout.EAST, m_Convert,
				-m_border, SpringLayout.EAST, m_c);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_Convert,
				(4*m_h/10), SpringLayout.NORTH, m_c);
		// position Edit config button
		m_sLayout.putConstraint(SpringLayout.EAST, m_EditConfig,
				-m_border, SpringLayout.EAST, m_c);
		m_sLayout.putConstraint(SpringLayout.SOUTH, m_EditConfig,
				-m_border, SpringLayout.NORTH, m_Convert);
		// position console text area
		m_sLayout.putConstraint(SpringLayout.WEST, m_consolePane,
				m_border, SpringLayout.WEST, m_c);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_consolePane,
				(m_h/2), SpringLayout.NORTH, m_c);
		m_sLayout.putConstraint(SpringLayout.EAST, m_consolePane,
				-m_border, SpringLayout.EAST, m_c);
		m_sLayout.putConstraint(SpringLayout.SOUTH, m_consolePane,
				-m_border, SpringLayout.SOUTH, m_c);
		// position checklabel
		m_sLayout.putConstraint(SpringLayout.NORTH, m_checkLabel,
				m_border, SpringLayout.SOUTH, m_N3Directory);
		m_sLayout.putConstraint(SpringLayout.WEST, m_checkLabel,
				m_border, SpringLayout.WEST, m_c);
		// position FeatureCheck
		m_sLayout.putConstraint(SpringLayout.WEST, m_FeatureCheck,
				m_border, SpringLayout.WEST, m_c);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_FeatureCheck,
				m_border, SpringLayout.SOUTH, m_checkLabel);
		// position GMLCheck
		m_sLayout.putConstraint(SpringLayout.WEST, m_GMLCheck,
				m_border, SpringLayout.EAST, m_FeatureCheck);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_GMLCheck,
				m_border, SpringLayout.SOUTH, m_checkLabel);
		// position GeometryCheck
		m_sLayout.putConstraint(SpringLayout.WEST, m_GeometryCheck,
				m_border, SpringLayout.WEST, m_c);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_GeometryCheck,
				m_border, SpringLayout.SOUTH, m_FeatureCheck);
		// position RelationCheck
		m_sLayout.putConstraint(SpringLayout.WEST, m_RelationCheck,
				m_border, SpringLayout.EAST, m_GeometryCheck);
		m_sLayout.putConstraint(SpringLayout.NORTH, m_RelationCheck,
				m_border, SpringLayout.SOUTH, m_FeatureCheck);
	}
	/**
	 * initializes the Buttons and adds action listeners.
	 */
	private void initButtons() {
		m_GMLSelect = new JButton("Select GML File(s)");
		m_GMLSelect.addActionListener(m_GMLSelectListener);
		m_GMLSelect.setPreferredSize(new Dimension(150, 25));
		m_N3Select = new JButton("Select N3 Directory");
		m_N3Select.addActionListener(m_N3SelectListener);
		m_N3Select.setPreferredSize(new Dimension(150, 25));
		m_Convert = new JButton("Convert");
		m_Convert.addActionListener(m_ConvertListener);
		m_EditConfig = new JButton("Edit Configuration");
		m_EditConfig.setPreferredSize(new Dimension(150, 25));
		m_EditConfig.addActionListener(m_EditConfigListener);
	}
	/**
	 * initializes all text areas contained in the GUI.
	 */
	private void initTxtAreas() {
		m_GMLFile = new JTextField(" ");
		m_GMLFile.setPreferredSize(new Dimension(325, 25));
		m_N3Directory = new JTextField(" ");
		m_N3Directory.setPreferredSize(new Dimension(325, 25));
		m_Console = new JTextArea();
		m_Console.setEditable(false);
		m_Console.setAutoscrolls(true);
		m_consolePane = new JScrollPane(m_Console);
		m_consolePane.setAutoscrolls(true);
	}
	/**
	 * creates the check boxes and adds Item Listeners to update
	 * which functions to run.
	 */
	private void initCheckBoxes() {
		m_GMLCheck = new JCheckBox("GML", m_cGML);
		m_GMLCheck.setPreferredSize(new Dimension(
				m_checkWidth, m_lineHeight));
		m_GMLCheck.addItemListener(new ItemListener() {
			public final void itemStateChanged(final ItemEvent e) {
				m_cGML = (e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		m_FeatureCheck = new JCheckBox("Features", m_cFeatures);
		m_FeatureCheck.setPreferredSize(new Dimension(m_checkWidth,
				m_lineHeight));
		m_FeatureCheck.addItemListener(new ItemListener() {
			public final void itemStateChanged(final ItemEvent e) {
				m_cFeatures = (e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		m_GeometryCheck = new JCheckBox("Geometric Data", m_cGeometry);
		m_GeometryCheck.setPreferredSize(new Dimension(m_checkWidth,
				m_lineHeight));
		m_GeometryCheck.addItemListener(new ItemListener() {
			public final void itemStateChanged(final ItemEvent e) {
				m_cGeometry = (e.getStateChange() == ItemEvent.SELECTED);
			}
		});
		m_RelationCheck = new JCheckBox("Relational Data",m_cRelation);
		m_RelationCheck.setPreferredSize(new Dimension(m_checkWidth,
				m_lineHeight));
		m_RelationCheck.addItemListener(new ItemListener() {
			public final void itemStateChanged(final ItemEvent e) {
				m_cRelation = (e.getStateChange() == ItemEvent.SELECTED);
			}
		});
	}
	/**
	 * selects a configuration file and creates a new configuration
	 * editor for the selected file.
	 */
	private final ActionListener m_EditConfigListener = new ActionListener() {
		public final void actionPerformed(final ActionEvent e) {
			if (e.getActionCommand().equals("Edit Configuration")) {
				ConfigChooser cc = new ConfigChooser();
				cc.setCurrentDirectory(new File("./config"));
				try {
					ConfigurationEditor ce = new ConfigurationEditor(cc.getFile());
					Thread ceThread = new Thread(ce);
					ceThread.start();
				} catch (Exception err) { /* Ignore... */ }
			}
		}
	};
	/**
	 * opens the GML File Chooser and updates the GML file and text field.
	 */
	private final ActionListener m_GMLSelectListener = new ActionListener() {
		public final void actionPerformed(final ActionEvent e) {
			if (e.getActionCommand().equals("Select GML File(s)")) {
				try {
					m_gc.setCurrentDirectory(new File(m_currentDir));
					m_gmlFile = m_gc.getFile();
					m_GMLFile.setText(m_gmlFile.getPath());
					m_currentDir = m_gmlFile.getParent();
				} catch (Exception err) { /* Ignore... */ }
			}
		}
	};
	/**
	 * opens the output directory chooser and updates the
	 * N3Directory File and text field.
	 */
	private final ActionListener m_N3SelectListener = new ActionListener() {
		public final void actionPerformed(final ActionEvent e) {
			if (e.getActionCommand().equals("Select N3 Directory")) {
				try {
					m_n3c.setCurrentDirectory(new File(m_currentDir));
					m_n3Dir = m_n3c.getFile();
					m_N3Directory.setText(m_n3Dir.getPath());
				} catch (Exception err) { /* Ignore... */ }
			}
		}
	};
	/**
	 * creates a new Thread and runs the GML Converter with
	 * current configurations.
	 */
	private final ActionListener m_ConvertListener = new ActionListener() {
		public final void actionPerformed(final ActionEvent e) {
			if (e.getActionCommand().equals("Convert")) {
				if (m_gmlFile != null && m_n3Dir != null) {
					try {
						GMLConverter converter = new GMLConverter(m_gmlFile,
						 m_n3Dir, m_cFeatures, m_cGML,m_cGeometry, m_cRelation);
						Thread convert = new Thread(converter);
						convert.start();
					} catch (Exception err) {
						System.out.println(err.getMessage());
					}
				}
			}
		}
	};
	/**
	 * Class for printing data to the Console text area.
	 * @author Andrew Bulen
	 */
	class FilteredStream extends FilterOutputStream {
		/**
		 * constructor.
		 * @param aStream output stream to which strings are written
		 */
        public FilteredStream(final OutputStream aStream) {
            super(aStream);
        }
        /**
         * override write byte array.
         * @param b array of bytes to write to console
         */
        public void write(final byte[] b) {
            String aString = new String(b);
            m_Console.append(aString);
        }
        /**
         * override write integer.
         * @param b integer value to add to console
         */
        public void write(final int b) {
        	String aString = "" + b;
        	m_Console.append(aString);
        }
        /**
         * override write from offset to length.
         * @param b byte array being written from
         * @param off offset position to start in the array
         * @param len length of array to write
         */
        public void write(final byte[] b, final int off, final int len) {
        	String aString = new String(b, off, len);
        	m_Console.append(aString);
        }
    }

}

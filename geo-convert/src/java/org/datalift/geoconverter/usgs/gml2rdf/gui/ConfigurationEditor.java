package org.datalift.geoconverter.usgs.gml2rdf.gui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.datalift.geoconverter.usgs.rdf.util.Config;
import org.datalift.geoconverter.usgs.rdf.util.FeatureType;

/**
 * GUI used to alter the configurations for a given feature type.
 * creates a user friendly interface for altering aspects of a feature types.
 * configuration including
 * 		- feature type name
 * 		- unique identifying attribute
 * 		- aspects of each attributes conversion
 * 			* Resource/Literal		* resource namespace
 * 			* attribute parent		* attribute predicate
 * 		- add/remove attributes from the feature type
 * @author Andrew Bulen
 */
public class ConfigurationEditor extends JFrame implements Runnable {

	/** feature type being edited. */
	private FeatureType m_ft;
	/** label representing the feature type. */
	private String m_featureID;
	/** GUI content panel. */
	private Container m_c = getContentPane();
	/** layout of the GUI. */
	private SpringLayout m_layout = new SpringLayout();
	/** feature name and Unique ID text fields. */
	private JTextField m_featureName,m_uid;
	/** label of the feature name. */
	private JTextField m_fnLabel = new JTextField("Feature Type Name");
	/** label of the Unique Identifier. */
	private JTextField m_uidLabel = new JTextField("Unique Identifier");
	/** list of Attributes of the feature type. */
	private LinkedList<Attribute> m_attributes = new LinkedList<Attribute>();
	/** border width between GUI content. */
	private int m_border = 5;
	/** Button used to save the edited configuration. */
	private JButton m_save = new JButton("Save");
	/** creates another attribute associated with the feature type. */
	private JButton m_addAttribute = new JButton("Add Attribute");
	/** thread running the configuration editor. */
	private Thread m_current;
	/** number of spaces of attribute input text fields. */
	private int m_attIDsize=15,m_rlSize=1,m_nsSize=25,m_paSize=10,m_prSize=25;
	/**
	 * initialization constructor.
	 *  - sets the feature ID and feature type and initializes the interface
	 * @param fID the label representing the feature type
	 * @param ftype the feature type being edited
	 */
	public ConfigurationEditor(final String fID, final FeatureType ftype) {
		m_featureID = fID;
		m_ft = ftype;
		m_featureName = new JTextField(m_ft.name(), m_prSize);
		m_uid = new JTextField("OBJECTID", m_prSize);
		m_fnLabel.setEditable(false);
		m_uidLabel.setEditable(false);
		m_save.addActionListener(saveListener);
		m_addAttribute.addActionListener(addAttListener);
		m_c.setLayout(m_layout);
		this.setTitle(fID);
	}
	/**
	 * File constructor.
	 *  - loads a configuration from the input file and initializes the
	 *  interface with the values loaded from the configuration file.
	 * @param configFile configuration file being edited
	 */
	public ConfigurationEditor(final File configFile) {
		m_featureID = configFile.getName().substring(0,
				configFile.getName().indexOf(".conf"));
		m_ft = new FeatureType();
		try {
			m_ft.loadFromFile(configFile.getPath());
			m_featureName = new JTextField(m_ft.name(), m_prSize);
			m_uid = new JTextField(m_ft.uidField(), m_prSize);
		} catch (Exception err) { err.printStackTrace(); }
		m_fnLabel.setEditable(false);
		m_uidLabel.setEditable(false);
		m_save.addActionListener(saveListener);
		m_addAttribute.addActionListener(addAttListener);
		m_c.setLayout(m_layout);
		this.setTitle(configFile.getName().toString().substring(0,
			configFile.getName().toString().indexOf(".conf")));
	}
	/**
	 * run function implemented from the Runnable interface.
	 * Initializes the GUI and waits for interrupt from user selection of
	 * close/save and disposes of the JFrame
	 */
	public final void run() {
		m_c.add(m_featureName);
		m_c.add(m_fnLabel);
		m_c.add(m_uid);
		m_c.add(m_uidLabel);
		m_c.add(m_save);
		m_c.add(m_addAttribute);
		this.createLabel();
		this.attributesToFrame();
		this.setLayout();
		this.setVisible(true);
		this.setBounds(0, 0, m_attributes.get(0).m_width,
			m_attributes.size() * m_attributes.get(0).m_height + 150);
		try {
			m_current = Thread.currentThread();
			m_current.join();
		} catch (InterruptedException e) {
			this.dispose();
		}
	}
	/**
	 * creates the visual layout of the GUI using the SpringLayout.
	 */
	private void setLayout() {
		m_layout.putConstraint(SpringLayout.NORTH, m_fnLabel, m_border,
				SpringLayout.NORTH, m_c);
		m_layout.putConstraint(SpringLayout.WEST, m_fnLabel, m_border,
				SpringLayout.WEST, m_c);
		m_layout.putConstraint(SpringLayout.WEST, m_featureName, m_border,
				SpringLayout.EAST, m_fnLabel);
		m_layout.putConstraint(SpringLayout.NORTH, m_featureName, m_border,
				SpringLayout.NORTH, m_c);
		m_layout.putConstraint(SpringLayout.NORTH, m_uidLabel, m_border,
				SpringLayout.NORTH, m_c);
		m_layout.putConstraint(SpringLayout.WEST, m_uidLabel, m_border,
				SpringLayout.EAST, m_featureName);
		m_layout.putConstraint(SpringLayout.NORTH, m_uid, m_border,
				SpringLayout.NORTH, m_c);
		m_layout.putConstraint(SpringLayout.WEST, m_uid, m_border,
				SpringLayout.EAST, m_uidLabel);
		m_layout.putConstraint(SpringLayout.EAST, m_save, -m_border,
				SpringLayout.EAST, m_c);
		m_layout.putConstraint(SpringLayout.SOUTH, m_save, -m_border,
				SpringLayout.SOUTH, m_c);
		m_layout.putConstraint(SpringLayout.NORTH, m_attributes.get(0), m_border,
				SpringLayout.SOUTH, m_featureName);
		m_layout.putConstraint(SpringLayout.WEST, m_attributes.get(0), m_border,
				SpringLayout.WEST, m_c);
		m_layout.putConstraint(SpringLayout.SOUTH, m_addAttribute, -m_border,
				SpringLayout.SOUTH, m_c);
		m_layout.putConstraint(SpringLayout.WEST, m_addAttribute, m_border,
				SpringLayout.WEST, m_c);
		for (int i = 1; i < m_attributes.size(); i++) {
			Attribute a = m_attributes.get(i);
			m_layout.putConstraint(SpringLayout.NORTH, a, 1,
				SpringLayout.SOUTH, m_attributes.get(i - 1));
			m_layout.putConstraint(SpringLayout.WEST, a,
				m_border, SpringLayout.WEST, m_c);
			m_layout.putConstraint(SpringLayout.EAST, a,
				-m_border, SpringLayout.EAST, m_c);
		}
	}
	/**
	 * Creates a Label attribute that displays the column names for
	 * attribute data members.
	 */
	private void createLabel() {
		Config confLabel = new Config(true, "Namespace", "Parent",
				"Predicate");
		Attribute label = new Attribute("Attribute ID", confLabel);
		label.m_ID.setEditable(false);
		label.m_ID.setBackground(Color.BLACK);
		label.m_ID.setForeground(Color.YELLOW);
		label.m_RL.setEditable(false);
		label.m_RL.setBackground(Color.BLACK);
		label.m_RL.setForeground(Color.YELLOW);
		label.m_namespace.setEditable(false);
		label.m_namespace.setBackground(Color.BLACK);
		label.m_namespace.setForeground(Color.YELLOW);
		label.m_parent.setEditable(false);
		label.m_parent.setBackground(Color.BLACK);
		label.m_parent.setForeground(Color.YELLOW);
		label.m_predicate.setEditable(false);
		label.m_predicate.setBackground(Color.BLACK);
		label.m_predicate.setForeground(Color.YELLOW);
		label.remove(label.m_remove);
		m_attributes.add(label);
		m_c.add(label);
	}
	/**
	 * creates an Attribute for each attribute from the feature type
	 * and adds them to the configuration editor.
	 */
	private void attributesToFrame() {
		Set<String> keys = m_ft.getAttributes().keySet();
		for (String attID : keys) {
			Attribute att = new Attribute(attID,
					m_ft.getAttributeConfig(attID));
			m_attributes.add(att);
			m_c.add(att);
			att.setVisible(true);
		}
	}
	/**
	 * Panel for grouping individual Attribute configurations.
	 * Creates the configuration fields for a single attribute of the
	 * feature type and updates the configuration according to user input
	 * @author Andrew Bulen
	 */
	private class Attribute extends JPanel {
		/** the current configuration of the attribute. */
		private Config m_conf;
		/** the ID field associated with the attribute. */
		private String m_attID;
		/** the input text fields for each aspect of the configuration. */
		private JTextField m_ID, m_RL, m_namespace, m_parent, m_predicate;
		/** attribute layout. */
		private SpringLayout m_aLayout = new SpringLayout();
		/** border between attribute GUI items. */
		private int m_pad = 5;
		/** size of the Attribute. */
		private int m_width=1050,m_height=20;
		/** removes the current attribute from the feature type. */
		private JButton m_remove = new JButton("Remove");
		/**
		 * initialization constructor.
		 * initializes the identifier for the attribute and its current
		 * configuration
		 * @param id label representing the attribute
		 * @param config configuration associated with the attribute
		 */
		public Attribute(final String id, final Config config) {
			m_attID = id;
			m_conf = config;
			this.setLayout(m_aLayout);
			this.initTextFields();
			this.setLayout();
			this.setVisible(true);
			this.setPreferredSize(new Dimension(m_width, m_height));
			m_remove.addActionListener(removeListener);
		}
		/**
		 * initializes the interface for the identifier,
		 * resource/literal, namespace, parent and predicate
		 * values based on the Attributes Config member.
		 */
		private void initTextFields() {
			m_ID = new JTextField(m_attID, m_attIDsize);
			m_ID.setEditable(false);
			if (m_conf.getIsResource()) {
				m_RL = new JTextField("R", m_rlSize);
			} else {
				m_RL = new JTextField("L", m_rlSize);
			}
			m_namespace = new JTextField(m_conf.getNamespace(), m_nsSize);
			m_parent = new JTextField(m_conf.getParent(), m_paSize);
			m_predicate = new JTextField(m_conf.getPredicate(), m_prSize);

			this.add(m_ID);
			this.add(m_RL);
			this.add(m_namespace);
			this.add(m_parent);
			this.add(m_predicate);
			this.add(m_remove);
			m_remove.setPreferredSize(new Dimension(100, m_height));
		}
		/**
		 * creates the visual layout of the Attribute panel using
		 * the SpringLayout.
		 */
		private final void setLayout() {
			// set ID
			m_aLayout.putConstraint(SpringLayout.WEST, m_ID, 0,
					SpringLayout.WEST, this);
			// set RL
			m_aLayout.putConstraint(SpringLayout.WEST, m_RL,
					m_pad, SpringLayout.EAST, m_ID);
			// set namespace
			m_aLayout.putConstraint(SpringLayout.WEST, m_namespace,
					m_pad, SpringLayout.EAST, m_RL);
			// set parent
			m_aLayout.putConstraint(SpringLayout.WEST, m_parent,
					m_pad, SpringLayout.EAST, m_namespace);
			// set predicate
			m_aLayout.putConstraint(SpringLayout.WEST, m_predicate,
					m_pad, SpringLayout.EAST, m_parent);
			// set remove button
			m_aLayout.putConstraint(SpringLayout.WEST, m_remove,
					m_pad, SpringLayout.EAST, m_predicate);
		}
		/**
		 * retrieves the text from the user interface and updates the
		 * changes in the various fields of the configuration.
		 * @return the updated configuration
		 */
		public final Config getConfig(){
			if (m_RL.getText().equalsIgnoreCase("R")) {
				m_conf.setIsResource(true);
			} else {
				m_conf.setIsResource(false);
			} // if namespace is empty add space
			if (m_namespace.getText().isEmpty()) {
				m_conf.setNamespace(" ");
			} else {
				m_conf.setNamespace(m_namespace.getText());
			} // if parent is the feature Name and feature name has
			// changed update attribute
			if (m_parent.getText().equals(m_ft.name())
				&& !m_ft.name().equals(m_featureName.getText())) {
				m_conf.setParent(m_featureName.getText());
			} else {
				m_conf.setParent(m_parent.getText());
			} // if predicate is empty add space
			if (m_predicate.getText().isEmpty()) {
				m_conf.setPredicate(" ");
			} else {
				m_conf.setPredicate(m_predicate.getText());
			}
			return m_conf;
		}
		/**
		 * Gets the identifier for the current attribute.
		 * @return label associated with attribute
		 */
		public final String getID() {
			this.m_attID = m_ID.getText();
			return m_attID;
		}
		/**
		 * removes the current attribute from the feature type and
		 * adjusts the GUI.
		 */
		public final void remove(){
			int index = m_attributes.indexOf(this);
			if (index != (m_attributes.size() - 1)) {
				m_layout.putConstraint(SpringLayout.NORTH,
					m_attributes.get(index + 1), 1, SpringLayout.SOUTH,
					m_attributes.get(index - 1));
			}
			m_attributes.remove(this);
			m_c.remove(this);
			m_c.validate();
			m_c.repaint();
		}
		/**
		 * Action listener for removing the attribute.
		 */
		private final ActionListener removeListener = new ActionListener() {
			public final void actionPerformed(final ActionEvent e) {
				if (e.getActionCommand().equals("Remove")) {
					remove();
				}
			}
		};
	}
	/**
	 * Action Listener for saving the updated configuration to file.
	 */
	private final ActionListener saveListener = new ActionListener() {

		public final void actionPerformed(final ActionEvent e) {
			if (e.getActionCommand().equals("Save")) {
				FeatureType ftype = new FeatureType(
						m_featureName.getText().trim());
				ftype.setIDField(m_uid.getText().trim());
				Iterator<Attribute> attIter = m_attributes.iterator();
				// skip label attribute
				if (attIter.hasNext()) {
					attIter.next();
				}
				while (attIter.hasNext()) {
					Attribute att = attIter.next();
					ftype.addAttribute(att.getID(), att.getConfig());
				}
				try {
					ftype.toFile("./config/" + m_featureID + ".conf");
					m_current.interrupt();
				} catch (Exception err) { err.printStackTrace(); }
			}
		}
	};
	/**
	 * Action Listener for adding a new attribute to the feature type.
	 */
	private final ActionListener addAttListener = new ActionListener(){
		public final void actionPerformed(final ActionEvent e) {
			if (e.getActionCommand().equals("Add Attribute")) {
				Config conf = new Config();
				Attribute newAtt = new Attribute("AttributeID", conf);
				newAtt.m_ID.setEditable(true);
				m_attributes.add(newAtt);
				m_c.add(newAtt);
				m_layout.putConstraint(SpringLayout.NORTH, newAtt, 1,
					SpringLayout.SOUTH, m_attributes.get(m_attributes.size() - 2));
				m_c.validate();
				m_c.repaint();
			}
		}
	};
}

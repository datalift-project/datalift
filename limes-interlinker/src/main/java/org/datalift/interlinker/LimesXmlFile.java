package org.datalift.interlinker;

import java.io.File;
import java.io.StringWriter;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.ImmutableMap;

/**
 * @author Bouca Nova Dany
 *
 */
public class LimesXmlFile {
	static final String FILENAME = "tmp.xml";
	private Document doc;
	private DocumentBuilderFactory dbFactory;
	private DocumentBuilder dBuilder;
	private TransformerFactory transformerFactory;
	private Transformer transformer;

	private Element rootElement;

	private Map<String, String> sourceElements;
	private Map<String, String> targetElements;
	private Map<String, String> acceptanceElements;
	private Map<String, String> reviewElements;
	private Form form;

	public LimesXmlFile(Form form) {
		this.form = form;

		sourceElements = ImmutableMap.<String, String>builder()
				.put("ID", form.getSourceId())
				.put("ENDPOINT", form.getSourceEndpoint())
				.put("VAR", form.getSourceVar())
				.put("PAGESIZE", form.getSourcePagesize())
				.put("RESTRICTION", form.getSourceRestriction())
				.put("PROPERTY", "SOURCE")
				.put("TYPE", form.getSourceType())
				.build();
		targetElements = ImmutableMap.<String, String>builder()
				.put("ID", form.getTargetId())
				.put("ENDPOINT", form.getTargetEndpoint())
				.put("VAR", form.getTargetVar())
				.put("PAGESIZE", form.getTargetPagesize())
				.put("RESTRICTION", form.getTargetRestriction())
				.put("PROPERTY", "TARGET")
				.put("TYPE", form.getTargetType())
				.build();
		acceptanceElements = ImmutableMap.<String, String>builder()
				.put("THRESHOLD", form.getAcceptanceThreshold())
				.put("FILE", form.getAcceptanceFile())
				.put("RELATION", form.getAcceptanceRelation())
				.build();
		reviewElements = ImmutableMap.<String, String>builder()
				.put("THRESHOLD", form.getReviewThreshold())
				.put("FILE", form.getReviewFile())
				.put("RELATION", form.getReviewRelation())
				.build();
	}

	public void createDocument() {
		try {
			this.dbFactory = DocumentBuilderFactory.newInstance();
			this.dBuilder = dbFactory.newDocumentBuilder();
			this.doc = dBuilder.newDocument();
			this.transformerFactory = TransformerFactory.newInstance();
			this.transformer = transformerFactory.newTransformer();

			//transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_SYSTEM, "limes.dtd");

		} catch (ParserConfigurationException e) {
			throw new RuntimeException("Document creation failed", e);
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException("Document creation failed", e);
		}
	}

	public boolean isDocumentCreated() {
		return doc.hasChildNodes();
	}
	
	public void SaveDocument(String name) {
		DOMSource src = new DOMSource(this.doc);
		StreamResult result = new StreamResult(new File(name));
		try {
			transformer.transform(src, result);
		} catch (TransformerException e) {
			throw new RuntimeException("Document transformation failed", e);
		}
	}
	public void deleteDocument(String name) {
		File file = new File(name);
		
		if (file.exists())
			file.delete();		
	}

	public void createRoot(String s) {
		this.rootElement = this.doc.createElement(s);
		this.doc.appendChild(rootElement);
	}

	public void createPrefixes(String[] prefixes) {
		for (int i = 0; i < prefixes.length; i++) {

			Element prefix = this.doc.createElement("PREFIX");
			this.rootElement.appendChild(prefix);

			Element namespace = this.doc.createElement("NAMESPACE");
			namespace.appendChild(this.doc.createTextNode(prefixes[i]));
			prefix.appendChild(namespace);

			Element label = this.doc.createElement("LABEL");
			label.appendChild(this.doc.createTextNode(prefixes[++i]));
			prefix.appendChild(label);

		}
	}

	public void createParentChild(String s, Map<String, String> map) {
		Element elem = this.doc.createElement(s);
		if (map != sourceElements && map != targetElements)
			addContent(this.doc, elem, map);	
		else
			addSources(this.doc, elem, map);
		this.rootElement.appendChild(elem);
	}

	public void createSingleChild(String s, String fill) {
		Element elem = this.doc.createElement(s);
		elem.appendChild(this.doc.createTextNode(fill));
		this.rootElement.appendChild(elem);
	}

	private void addContent(Document doc, Element root, String tag, String content) {
		Element elem = this.doc.createElement(tag);
		elem.appendChild(this.doc.createTextNode(content));
		root.appendChild(elem);
	}
	
	private void addContent(Document doc, Element root, Map<String, String> map) {

		for (Map.Entry<String, String> entry : map.entrySet()) {
			Element elem = this.doc.createElement(entry.getKey());
			elem.appendChild(this.doc.createTextNode(entry.getValue()));
			root.appendChild(elem);
		}
	}

	public String print() {
		DOMSource src = new DOMSource(this.doc);
		StreamResult consoleResult = new StreamResult(System.out);

		try {
			this.transformer.transform(src, consoleResult);
		} catch (TransformerException e) {
			throw new RuntimeException("Document transformation failed", e);
		}
		return null;
	}

	@Override
	public String toString() {
		try {
			DOMSource domSource = new DOMSource(this.doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);

			this.transformer.transform(domSource, result);
			
			return writer.toString();
		} catch (Exception ex) {
			throw new RuntimeException("Error converting to String", ex);
		}
	}

	public void formatLimesFile() {
		createPrefixes(form.getPrefixProperties());
		createParentChild("SOURCE", sourceElements);
		createParentChild("TARGET", targetElements);
		createSingleChild("METRIC", form.getMetric());
		createParentChild("ACCEPTANCE", acceptanceElements);
		createParentChild("REVIEW", reviewElements);
		createSingleChild("EXECUTION", form.getExecution());
		createSingleChild("OUTPUT", form.getOutput());
	}

	private void addSources(Document doc, Element root, Map<String, String> map) {
		for (Map.Entry<String, String> entry : map.entrySet()) {
			if (entry.getKey().equals("PROPERTY") && entry.getValue().equals("SOURCE")) {
				for (String tmp : form.getSourceProperties())
					addContent(this.doc, root, "PROPERTY", tmp);
			} else if (entry.getKey().equals("PROPERTY") && entry.getValue().equals("TARGET")) {
				for (String tmp : form.getTargetProperties())
					addContent(this.doc, root, "PROPERTY", tmp);
			} else {
				addContent(this.doc, root, entry.getKey(), entry.getValue());
			}
		}
	}	
	
}

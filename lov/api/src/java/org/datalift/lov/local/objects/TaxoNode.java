package org.datalift.lov.local.objects;

import java.util.ArrayList;
import java.util.List;

import org.datalift.lov.local.LovUtil;

public class TaxoNode implements JSONSerializable {
	private String uri = null;
	private String label = null;
	private List<TaxoNode> children = new ArrayList<TaxoNode>();
	private int count = 0;

	public TaxoNode() {
	}

	// constructeur par copie
	public TaxoNode(TaxoNode tn) {
		this.uri = tn.getUri();
		this.label = tn.getLabel();
		this.count = 0;// Attention à ceci
		for (int i = 0; i < tn.getChildren().size(); i++) {
			this.children.add(new TaxoNode(tn.getChildren().get(i)));
		}
	}

	public TaxoNode(String uri, String label) {
		this.uri = uri;
		this.label = label;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public List<TaxoNode> getChildren() {
		return children;
	}

	public void setChildren(List<TaxoNode> children) {
		this.children = children;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public void add(TaxoNode child) {
		this.getChildren().add(child);
	}
	
	@Override
	public String toJSON() {
		StringBuilder jsonResult = new StringBuilder();
		
		// beginning of json
		jsonResult.append("{");
		
		// simple properties
		jsonResult.append("\"uri\": " + LovUtil.toJSON(uri) + ",");
		jsonResult.append("\"label\": " + LovUtil.toJSON(label) + ",");
		
		// children
		jsonResult.append("\"children\": " + LovUtil.toJSON(children));
		
		// count
		jsonResult.append("\"count\": " + count);
		
		// end of json
		jsonResult.append("}");
		
		return jsonResult.toString();
	}
}

package org.datalift.owl.toolkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrdf.rio.RDFHandler;

public class Script {

	protected String script;
	
	public Script(String script) {
		super();
		this.script = script;
	}

	public String getPrefixes() {
		int lastPrefixDeclaration = script.lastIndexOf("PREFIX");
		if(lastPrefixDeclaration < 0) {
			return "";
		} else {
			int endOfLastPrefixDeclaration = script.indexOf(">", lastPrefixDeclaration);
			return script.substring(0,endOfLastPrefixDeclaration+1);
		}
	}
	
	public List<ScriptItem> getScriptItems() {
		int beginOfScriptItems = this.getPrefixes().length();
		List<String> itemsString = Arrays.asList(
				this.script.substring(beginOfScriptItems, this.script.length()).split(";")
		);
		List<ScriptItem> result = new ArrayList<ScriptItem>();
		for (String aScriptString : itemsString) {
			result.add(new ScriptItem(aScriptString));
		}
		return result;
	}
	
	public void execute(SesameSPARQLExecuter executer, RDFHandler handler) 
	throws SPARQLExecutionException {
		List<ScriptItem> items = this.getScriptItems();
		for (ScriptItem anItem : items) {
			anItem.execute(this, executer, handler);
		}
	}
	
}
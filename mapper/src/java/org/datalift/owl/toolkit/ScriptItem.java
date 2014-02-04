package org.datalift.owl.toolkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.openrdf.query.MalformedQueryException;
import org.openrdf.rio.RDFHandler;

public class ScriptItem {

	protected String scriptItem;
	protected String templateName;
	protected List<BindingDeclaration> bindings;
	
	public ScriptItem(String scriptItem) {
		this.scriptItem = scriptItem;
		this.parse();
	}
	
	private void parse() {
		String withoutComment = this.stripComments(scriptItem);
		//System.out.println("'"+scriptItem+"' / '"+withoutComment+"'");
		this.templateName = withoutComment.substring(0, withoutComment.indexOf("(")).trim();			

		String bindingsDeclarationsString = withoutComment.substring(withoutComment.indexOf("(")+1, withoutComment.lastIndexOf(")")).trim();			
		List<String> bindingsDeclarations = Arrays.asList(bindingsDeclarationsString.split(","));
		
		this.bindings = new ArrayList<BindingDeclaration>();
		if(bindingsDeclarations.size() > 0) {
			for (String aBindingDecl : bindingsDeclarations) {
				BindingDeclaration bdecl = null;
				if(aBindingDecl.contains("=")) {
					String[] bdeclArray = aBindingDecl.split("=");
					bdecl = new BindingDeclaration(bdeclArray[0].trim(),bdeclArray[1].trim());
				} else {
					bdecl = new BindingDeclaration(null, aBindingDecl.trim());
				}
				this.bindings.add(bdecl);
			}
		}
	}
	
	public void execute(Script parent, SesameSPARQLExecuter executer, RDFHandler handler)
	throws SPARQLExecutionException {
		//System.out.println("Execuring script item : "+this.toString());
		TemplateCallSparqlHelper helper = new TemplateCallSparqlHelper(this, parent.getPrefixes());
		try {
			switch(TemplateRegistry.getTemplate(this.getTemplateName()).getSparqlQueryType()) {
			case CONSTRUCT : {
				executer.executeConstruct(new DelegatingConstructSPARQLHelper(helper, handler));
				break;
			}
			case SELECT : {
				// TODO
				break;
			}
			case ASK : {
				// TODO
				break;
			}
			case UPDATE : {
				//System.out.println("Will execute an update");
				executer.executeUpdate(helper);
				break;
			}
			}
		} catch (MalformedQueryException e) {
			e.printStackTrace();
		}
	}
	
	private String stripComments(String original) {
		StringBuffer result = new StringBuffer();
		List<String> lines = Arrays.asList(original.split("\n"));
		for (String string : lines) {
			if(!string.trim().startsWith("#")) {
				result.append(string+"\n");
			}
		}
		
		return result.toString();
	}

	public String getTemplateName() {
		return templateName;
	}

	public List<BindingDeclaration> getBindings() {
		return bindings;
	}

	public String getScriptItem() {
		return scriptItem;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(" ScriptItem name : "+templateName+" with bindings :"+"\n");
		for (BindingDeclaration aBd : getBindings()) {
			sb.append("  "+aBd.getVarName()+":"+aBd.getValue());
		}
		return sb.toString();
	}
	
}

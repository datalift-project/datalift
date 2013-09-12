package org.datalift.owl.toolkit;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openrdf.model.Statement;
import org.openrdf.model.Value;
import org.openrdf.model.vocabulary.RDF;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFHandlerException;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.helpers.RDFHandlerBase;
import org.openrdf.rio.turtle.TurtleParser;

public class TemplateCallSparqlHelper extends SPARQLHelper {

	protected String prefixes;
	protected ScriptItem item;
	
	public TemplateCallSparqlHelper(ScriptItem item) {
		super(new TemplateCallSparqlBuilder(item.getTemplateName()));
		this.item = item;
	}
	
	public TemplateCallSparqlHelper(ScriptItem item, String prefixes) {
		super(new TemplateCallSparqlBuilder(item.getTemplateName()));
		this.item = item;
		this.prefixes = prefixes;
	}

	@Override
	public Map<String, Value> getBindings() {
		Map<String, Value> result = new HashMap<String, Value>();
		
		try {			
			// recupere le template
			Template t = TemplateRegistry.getTemplate(this.item.getTemplateName());
			
			// si on ne trouve pas le template, exception
			if(t == null) {
				throw new TemplateParsingException("Unknown template : "+item.getTemplateName());
			}
			
			// récupère les paramètres de l'appel du template
			List<BindingDeclaration> bindings = item.getBindings();
			
			for(int i=0;i<bindings.size();i++) {
				String key;
				if(bindings.get(i).getVarName() == null) {
					Argument p = t.getArgumentWithOrder(i);
					if(p == null) {
						throw new TemplateParsingException("No parameter found with order : "+i+" in template "+t.getName());
					}
					key = p.getVarName();
				} else {
					key = bindings.get(i).getVarName();
					
					// si le parametre n'appartient pas au template, renvoyer une exception
					if(!t.containsArgument(bindings.get(i).getVarName())) {
						throw new TemplateParsingException("Unknown template parameter : "+bindings.get(i).getVarName());
					}
				}
				
				Value value = (new ValueParser()).parse(bindings.get(i).value);
				
				result.put(key, value);
			}
		} catch (TemplateParsingException e) {
			// TODO : what to do with this ?
			e.printStackTrace();
		}
		
		return result;
	}
	
	class ValueParser {
		
		protected Value result;
		
		public ValueParser() {
		}
		
		public Value parse(String value) {
			TurtleParser parser = (TurtleParser)RDFParserRegistry.getInstance().get(RDFFormat.N3).getParser();
			parser.setRDFHandler(new RDFHandlerBase() {
				@Override
				public void handleStatement(Statement st) throws RDFHandlerException {
					result = st.getObject();
				}				
			});
			
			this.result = null;
			try {
				//System.out.println("'"+((prefixes == null)?"":prefixes+" ")+"\n"+"_:x a "+value+" ."+"'");
				parser.parse(new StringReader(((prefixes == null)?"":prefixes+" ")+"\n"+"_:x a "+value+" ."), RDF.NAMESPACE);
			} catch (Exception e) {
				// Should never happen
				e.printStackTrace();
			}	
			return result;
		}		
	}
	
}
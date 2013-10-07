package org.datalift.lov.local;

import java.util.List;

import org.datalift.lov.local.objects.JSONSerializable;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;

public class LovUtil {
	
	public final static String LOV_CONTEXT = "http://lov.okfn.org/datalift/local/lov_aggregator";
	public final static String LOV_CONTEXT_SPARQL = "<" + LOV_CONTEXT + ">";
	
	public static void closeQuietly(Object connection) {
		
		if(connection != null && connection instanceof RepositoryConnection){
			try {
				((RepositoryConnection) connection).close();
			} catch (RepositoryException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
	}
	
	public static String toJSON(String attribute) {
		
//		return attribute == null ? "null" : attribute.replace("\"", "&quote;");
//		return attribute == null ? "null" : StringEscapeUtils.escapeJavaScript(attribute);
		return attribute == null ? "null" : encodeJSON(attribute);

	}
	
	public static String toJSON(JSONSerializable serializable) {
		return serializable == null ? "null" : serializable.toJSON();
	}
	
	public static String toJSON(List<? extends JSONSerializable> list) {
		return toJSON(list, false);
	}
	
	public static String toJSON(List<? extends JSONSerializable> list, boolean lastItem) {
		if (list == null) {
			return "null";
		}
		else {
			StringBuilder jsonResult = new StringBuilder();
			
			jsonResult.append("[");
			
			int nbItem = list.size();
			int currentItem = 0;
			for (JSONSerializable item : list) {
				++currentItem;
				jsonResult.append(item.toJSON());
				if ( currentItem < nbItem) {
					jsonResult.append(",");
				}
			}
			
			jsonResult.append("]");
			if ( ! lastItem ) {
				jsonResult.append(",");
			}
			
			return jsonResult.toString();
		}
	}
	
	 private static String encodeJSON(String string) {
         if (string == null || string.length() == 0) {
             return "\"\"";
         }

         char         c = 0;
         int          i;
         int          len = string.length();
         StringBuilder sb = new StringBuilder(len + 4);
         String       t;

         sb.append('"');
         for (i = 0; i < len; i += 1) {
             c = string.charAt(i);
             switch (c) {
             case '\\':
             case '"':
                 sb.append('\\');
                 sb.append(c);
                 break;
             case '/':
 //                if (b == '<') {
                     sb.append('\\');
 //                }
                 sb.append(c);
                 break;
             case '\b':
                 sb.append("\\b");
                 break;
             case '\t':
                 sb.append("\\t");
                 break;
             case '\n':
                 sb.append("\\n");
                 break;
             case '\f':
                 sb.append("\\f");
                 break;
             case '\r':
                sb.append("\\r");
                break;
             default:
                 if (c < ' ') {
                     t = "000" + Integer.toHexString(c);
                     sb.append("\\u" + t.substring(t.length() - 4));
                 } else {
                     sb.append(c);
                 }
             }
         }
         sb.append('"');
         return sb.toString();
     }
	
}

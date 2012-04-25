package org.datalift.tests;


import java.util.HashMap;
import java.util.Map;


public class VCardMappingTest
{
    public static void main(String[] args) throws Exception {
        final String SRC_GRAPH =
            "http://localhost:9091/datalift/project/kiosque/source/kiosques-ouverts-aaa-paris-csv-rdf-1";
        final String SRC_NS = SRC_GRAPH + '/';

        UpdateQuery q = null;
        if (args.length != 0) {
            if ("insert".equalsIgnoreCase(args[0])) {
                q = new InsertQuery(SRC_GRAPH);
            }
            else if ("delete".equalsIgnoreCase(args[0])) {
                q = new DeleteQuery(SRC_GRAPH);
            }
        }
        if (q == null) {
            q = new ConstructQuery();
        }

        Map<String,String> m = new HashMap<String,String>();
        m.put("fn", "concat(\"Kiosque \",adresse)");
        m.put("street-address", "adresse");
        m.put("country-name", "\"France\"");
        m.put("locality", "\"Paris\"");
        m.put("postal-code", "arrdt");

        q = new VCardMapping(null, SRC_NS, SRC_GRAPH).map(q, m);
        System.out.println(q);
    }
}

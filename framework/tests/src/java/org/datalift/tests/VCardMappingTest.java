package org.datalift.tests;


import java.util.HashMap;
import java.util.Map;


public class VCardMappingTest
{
    public static void main(String[] args) throws Exception {
        final String SRC_GRAPH =
            "http://localhost:9091/datalift/project/kiosque/source/kiosques-ouverts-aaa-paris-csv-rdf-1";
        final String SRC_NS = SRC_GRAPH + '/';

        Map<String,String> m = new HashMap<String,String>();
        m.put("fn", "concat(\"Kiosque \",adresse)");
        m.put("street-address", "adresse");
        m.put("country-name", "\"France\"");
        m.put("locality", "\"Paris\"");
        m.put("postal-code", "arrdt");

        // UpdateQuery q = new VCardMapping(SRC).map(new ConstructQuery(), m);
        UpdateQuery q = new VCardMapping(null, SRC_NS, SRC_GRAPH).map(new ConstructQuery(), m);
        System.out.println(q);
    }
}

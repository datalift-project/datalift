package org.datalift.tests;


import java.util.HashMap;
import java.util.Map;


public class VCardMappingTest
{
    public static void main(String[] args) throws Exception {
        final String SRC =
            "http://localhost:9091/datalift/project/kiosque/source/kiosques-ouverts-aaa-paris-csv-rdf-1/";

        Map<String,String> m = new HashMap<String,String>();
        m.put("fn", "concat(\"Kiosque \",adresse)");
        m.put("street-address", "adresse");
        m.put("country-name", "\"France\"");
        m.put("locality", "\"Paris\"");
        m.put("postal-code", "arrdt");

        // UpdateQuery q = new VCardMapping(SRC).map(new ConstructQuery(), m);
        UpdateQuery q = new VCardMapping(SRC).map(new InsertQuery(SRC), m);
        System.out.println(q);
    }
}

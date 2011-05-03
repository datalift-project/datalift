package org.datalift.core.velocity.i18n;


import java.util.LinkedList;
import java.util.List;
import java.util.Properties;


public class BundleList
{
    /* package */ final static String KEY = BundleList.class.getName();

    private final List<Properties> properties = new LinkedList<Properties>();

    public void addProperties(Properties propertie) {
        this.properties.add(propertie);
    }

    public String get(String key) {
        String value = key;
        for (Properties p : this.properties) {
            if (p.getProperty(key) != null) {
                value = p.getProperty(key);
                break;
            }
        }
        return value;
    }
}

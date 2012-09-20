package org.datalift.fwk.util.web;

import java.nio.charset.Charset;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

import org.datalift.fwk.i18n.LocaleComparable;


/**
 * Utility class to facilitate the display of a character set
 * selection input on a web user interface.
 */
public class Charsets
{
    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    /**
     * The character sets supported by the local JVM, sorted by
     * relevance first and then alphabetically.
     */
    public final static List<String> availableCharsets;
    /** The UTF-8 character set. */
    public final static Charset UTF_8 = Charset.forName("UTF-8"); 

    //-------------------------------------------------------------------------
    // Class initializer
    //-------------------------------------------------------------------------

    static {
        List<LocaleComparable<Charset>> l1 =
                                    new ArrayList<LocaleComparable<Charset>>();
        Collator c = Collator.getInstance();
        for (Charset cs : Charset.availableCharsets().values()) {
            if (cs.isRegistered()) {
                l1.add(new LocaleComparable<Charset>(cs.displayName(), cs, c));
            }
        }
        Collections.sort(l1, new Comparator<LocaleComparable<Charset>>() {
                private Pattern[] prefixes = new Pattern[] {
                                        Pattern.compile("ISO-8859-1"),
                                        Pattern.compile("UTF-.[^E]*"),
                                        Pattern.compile("ISO-8859-.*"),
                                        Pattern.compile("US-ASCII"),
                                        Pattern.compile("windows-12.*") };
                @Override
                public int compare(LocaleComparable<Charset> o1,
                                   LocaleComparable<Charset> o2) {
                    int n = 0;
                    int i1 = this.getPrefix(o1.key);
                    int i2 = this.getPrefix(o2.key);
                    if (i1 == i2) {
                        n = o1.compareTo(o2);
                    }
                    else {
                        n = i1 - i2;
                    }
                    return n;
                }

                private int getPrefix(String key) {
                    for (int i=0; i<prefixes.length; i++) {
                        if (prefixes[i].matcher(key).matches()) return i;
                    }
                    return prefixes.length;
                }
            });
        List<String> l2 = new ArrayList<String>(l1.size());
        for (LocaleComparable<Charset> cs : l1) {
            l2.add(cs.data.displayName());
        }
        availableCharsets = Collections.unmodifiableList(l2);
    }

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /** Default constructor, private on purpose. */
    private Charsets() {
        throw new UnsupportedOperationException();
    }
}

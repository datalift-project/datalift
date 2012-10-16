/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

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

    /** The character set name for the UTF-8 encoding. */
    public final static String UTF8_CHARSET = "UTF-8";
    /** The UTF-8 character set. */
    public final static Charset UTF_8 = Charset.forName(UTF8_CHARSET);

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

package org.datalift.fwk.i18n;


import java.util.AbstractList;
import java.util.Collection;
import java.util.Locale;
import java.util.RandomAccess;


/**
 * The user's preferred locales, extracted from the HTTP request
 * "<code>Accept-Language</code>" header.
 *
 * @author lbihanic
 */
public final class PreferredLocales extends AbstractList<Locale>
                                    implements RandomAccess
{
    private final Locale[] locales;

    public PreferredLocales(Collection<? extends Locale> c) {
        super();
        if (c == null) {
            throw new IllegalArgumentException("c");
        }
        this.locales = c.toArray(new Locale[c.size()]);
    }

    @Override
    public Locale get(int index) {
        return this.locales[index];
    }

    @Override
    public int size() {
        return this.locales.length;
    }
}

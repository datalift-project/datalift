package org.datalift.fwk.util.web;


import java.net.URI;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.i18n.PreferredLocales;
import org.datalift.fwk.log.Logger;


public class MainMenu extends Menu
{
    private final static MainMenu instance = new MainMenu();
    private static boolean registered = false;

    private final static Logger log = Logger.getLogger();

    private MainMenu() {
    }

    public static MainMenu get() {
        if (! registered) {
            try {
                Configuration.getDefault().registerBean(instance);
                registered = true;
            }
            catch (Exception e) {
                log.warn("Failed to register MainMenu instance " +
                         "to Datalift configuration", e);
            }
        }
        return instance;
    }

    public static class EntryDesc extends MenuEntry
    {
        private final URI uri;
        private final HttpMethod method;
        private final String label;
        private final String bundleName;
        private final Class<?> owner;
        private final int position;

        public EntryDesc(String uri, String label, String bundleName,
                                     Object owner, int position) {
            this(uri, HttpMethod.GET, label, bundleName, owner, position);
        }

        public EntryDesc(String uri, HttpMethod method, String label,
                         String bundleName, Object owner, int position) {
            this(URI.create(uri), method, label, bundleName, owner.getClass(),
                 position);
        }

        public EntryDesc(URI uri, HttpMethod method, String label,
                         String bundleName, Class<?> owner, int position) {
            super();
            if (uri == null) {
                throw new IllegalArgumentException("uri");
            }
            if (method == null) {
                throw new IllegalArgumentException("method");
            }
            if ((label == null) || (label.length() == 0)) {
                throw new IllegalArgumentException("label");
            }
            if (position < 0) {
                throw new IllegalArgumentException("position ("
                                    + position + ") shall not be negative");
            }
            this.uri = uri;
            this.method = method;
            this.label = label;
            this.bundleName = bundleName;
            this.owner = (owner != null)? owner: this.getClass();
            this.position = position;
        }

        @Override
        public URI getUri() {
            return this.uri;
        }

        @Override
        public HttpMethod getMethod() {
            return this.method;
        }

        @Override
        public String getLabel() {
            return (this.bundleName != null)?
                PreferredLocales.get().getBundle(this.bundleName, this.owner)
                                      .getString(this.label): this.label;
        }

        @Override
        public int getPosition() {
            return this.position;
        }

        @Override
        public URI getIcon() {
            return null;
        }
    }
}

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

package org.datalift.core.velocity.i18n;


import java.util.LinkedList;
import java.util.List;
import java.util.Properties;


/* package */ final class BundleList
{
    public final static String KEY = BundleList.class.getName();

    private final List<Bundle> bundles = new LinkedList<Bundle>();

    public void add(Bundle properties) {
        this.bundles.add(properties);
    }

    public String getValue(String key) {
        String value = key;
        for (Bundle b : this.bundles) {
            String v = b.get(key);
            if (v != null) {
                value = v;
                break;
            }
        }
        return value;
    }

    public static Bundle newBundle(Properties values, Bundle parent) {
        return new BundleImpl(values, parent);
    }

    public static interface Bundle
    {
        public String get(String key);
    }

    private final static class BundleImpl implements Bundle
    {
        private final Properties values;
        private final Bundle defaults;

        public BundleImpl(Properties values, Bundle defaults) {
            super();
            this.values = values;
            this.defaults = defaults;
        }

        @Override
        public String get(String key) {
            String v = this.values.getProperty(key);
            if ((v == null) && (this.defaults != null)) {
                v = this.defaults.get(key);
            }
            return v;
        }
    }
}

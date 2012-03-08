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

package org.datalift.wrapper;


/**
 * An enumeration of the operating systems handled by the DataLift
 * wrapper.
 *
 * @author lbihanic
 */
public enum OsType {
    MacOS("Mac OS"),
    Windows("Windows"),
    Other("");

    /** The current OS. */
    public static OsType CURRENT_OS = fromName(System.getProperty("os.name"));

    /** The prefix for this OS in the OS name string system property. */
    private final String prefix;

    /**
     * Creates a new enum value.
     * @param  prefix   the prefix to match in the OS name.
     */
    OsType(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Return the enum value corresponding to the specified OS name.
     * @param  name   the OS name to match.
     * @return the recognized OS type or {@link #Other} if the OS was
     *         not recognized.
     */
    public static OsType fromName(String name) {
        if (name == null) {
            throw new IllegalArgumentException("name");
        }
        OsType found = Other;
        for (OsType e : OsType.values()) {
            if (name.startsWith(e.prefix)) {
                found = e;
                break;
            }
        }
        return found;
    }
}

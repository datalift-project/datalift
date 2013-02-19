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

package org.datalift.fwk;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;


abstract public class FileStore
{
    abstract public boolean isLocal();
    abstract public File getFile(String path);

    public boolean exists(String path) {
        return this.exists(this.getFile(path));
    }
    abstract public boolean exists(File file);

    public InputStream getInputStream(String path) throws IOException {
        return this.getInputStream(this.getFile(path));
    }
    abstract public InputStream getInputStream(File file) throws IOException;

    public void save(File from, String targetPath) throws IOException {
        this.save(from, this.getFile(targetPath));
    }
    public void save(File from, File to) throws IOException {
        InputStream in = this.getInputStream(from);
        try {
            this.save(in, to);
        }
        finally {
            try { in.close(); } catch (Exception e) { /* Ignore... */ }
        }
    }
    public void save(InputStream from, String targetPath) throws IOException {
        this.save(from, this.getFile(targetPath));
    }
    abstract public void save(InputStream from, File to) throws IOException;

    public void read(String path, File to) throws IOException {
        this.read(this.getFile(path), to);
    }
    abstract public void read(File from, File to) throws IOException;

    public boolean delete(String path) {
        return this.delete(this.getFile(path));
    }
    abstract public boolean delete(File file);
}

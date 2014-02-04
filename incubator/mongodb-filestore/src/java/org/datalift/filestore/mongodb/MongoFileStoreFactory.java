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

package org.datalift.filestore.mongodb;


import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.gridfs.GridFS;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.FileStore;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.io.FileStoreFactory;

import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A {@link FileStoreFactory} implementation that provides MongoDB
 * GridFS-based file storage.
 *
 * @author lbihanic
 */
public final class MongoFileStoreFactory extends FileStoreFactory
{
    public static final String MONGO_URI_PREFIX = "mongodb://";

    //-------------------------------------------------------------------------
    // Class members
    //-------------------------------------------------------------------------

    private final static Logger log = Logger.getLogger();

    //-------------------------------------------------------------------------
    // FileStoreFactory contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public FileStore getFileStore(String path, Configuration cfg) {
        FileStore fs = null;
        if (! isBlank(path) && (path.startsWith(MONGO_URI_PREFIX))) {
            try {
                MongoClientURI uri = new MongoClientURI(path);
                MongoClient client = new MongoClient(uri);
                DB db = client.getDB(uri.getDatabase());
                String bucket = uri.getCollection();
                GridFS gridFs = (! isBlank(bucket))? new GridFS(db, bucket):
                                                     new GridFS(db);
                fs = new MongoFileStore(uri, gridFs);
            }
            catch (Exception e) {
                log.warn("MongoDB file store initialization failed for {}", e,
                         path);
            }
        }
        return fs;
    }
}

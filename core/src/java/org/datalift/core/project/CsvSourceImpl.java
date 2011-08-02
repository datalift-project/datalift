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

package org.datalift.core.project;


import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import au.com.bytecode.opencsv.CSVReader;

import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.util.StringUtils;


@Entity
@RdfsClass("datalift:csvSource")
public class CsvSourceImpl extends BaseFileSource<String[]>
                           implements CsvSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:separator")
    private String separator;
    @RdfProperty("datalift:titleRow")
    private boolean titleRow = false;

    private transient List<String[]> grid = null;
    private transient List<String> headers = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public CsvSourceImpl() {
        super(SourceType.CsvSource);
    }

    public CsvSourceImpl(String uri) {
        super(SourceType.CsvSource, uri);
    }

    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public void init(File docRoot, URI baseUri) throws IOException {
        super.init(docRoot, baseUri);

        InputStream in = this.getInputStream();
        if (in != null) {
            CSVReader reader = new CSVReader(
                            new InputStreamReader(in, "ISO-8859-1"),
                            Separator.valueOf(this.separator).getValue());
            this.grid = Collections.unmodifiableList(reader.readAll());

            Iterator<String[]> it = this.grid.iterator();
            if (it.hasNext()) {
                String[] firstRow = it.next();
                if (! this.titleRow) {
                    // Generate generic column names (A, B... Z, AA, AB...).
                    for (int i=0; i<firstRow.length; i++) {
                        firstRow[i] = getColumnName(i);
                    }
                }
                this.headers = Collections.unmodifiableList(
                                                    Arrays.asList(firstRow));
            }
            else {
                this.headers = Collections.emptyList();
            }
            // Else: empty file.
        }
    }
    
    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final boolean hasTitleRow() {
        return this.titleRow;
    }

    public String getSeparator() {
        return this.separator;
    }
    
    public List<String> getColumnsHeader() {
        if (this.headers == null) {
            throw new IllegalStateException();
        }
        return this.headers;
    }

    public final Iterator<String[]> iterator() {
        if (this.grid == null) {
            throw new IllegalStateException();
        }
        Iterator<String[]> i = this.grid.iterator();
        if ((this.titleRow) && (i.hasNext())) {
            // Skip title row.
            i.next();
        }
        return i;
    }
    
    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    public void setTitleRow(boolean titleRow) {
        this.titleRow = titleRow;
    }

    public void setSeparator(String separator) {
        if (! StringUtils.isSet(separator)) {
            throw new IllegalArgumentException("separator");
        }
        this.separator = separator;
    }

    public static String getColumnName(int n) {
        StringBuilder s = new StringBuilder();
        for (; n >= 0; n = n / 26 - 1) {
            s.insert(0, (char)(n % 26 + 65));
        }
        return s.toString();
    }
}

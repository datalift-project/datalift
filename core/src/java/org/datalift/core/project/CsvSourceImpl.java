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
    public enum Separator {
        comma(','), semicolon(';'), tab('\t');

        protected final char value;

        Separator(char s) {
            this.value = s;
        }

        public char getValue() {
            return value;
        }
    }

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
        super();
    }

    public CsvSourceImpl(String uri) {
        super(uri);
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
            CSVReader reader = new CSVReader(new InputStreamReader(in),
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

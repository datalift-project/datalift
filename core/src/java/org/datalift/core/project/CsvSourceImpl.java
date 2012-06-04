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


import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.persistence.Entity;

import com.clarkparsia.empire.annotation.RdfProperty;
import com.clarkparsia.empire.annotation.RdfsClass;

import au.com.bytecode.opencsv.CSVReader;

import org.datalift.core.TechnicalException;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Row;
import org.datalift.fwk.util.CloseableIterator;

import static org.datalift.fwk.util.StringUtils.isSet;


/**
 * Default implementation of the {@link CsvSource} interface.
 *
 * @author hdevos
 */
@Entity
@RdfsClass("datalift:csvSource")
public class CsvSourceImpl extends BaseFileSource
                           implements CsvSource
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    @RdfProperty("datalift:separator")
    private String separator;
    @RdfProperty("datalift:quote")
    private String quote;
    @RdfProperty("datalift:titleRow")
    private boolean titleRow = false;

    private transient List<String> headers = null;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new CSV source.
     */
    public CsvSourceImpl() {
        super(SourceType.CsvSource);
    }

    /**
     * Creates a new CSV source with the specified identifier and
     * owning project.
     * @param  uri       the source unique identifier (URI) or
     *                   <code>null</code> if not known at this stage.
     * @param  project   the owning project or <code>null</code> if not
     *                   known at this stage.
     *
     * @throws IllegalArgumentException if either <code>uri</code> or
     *         <code>project</code> is <code>null</code>.
     */
    public CsvSourceImpl(String uri, Project project) {
        super(SourceType.CsvSource, uri, project);
    }

    //-------------------------------------------------------------------------
    // FileSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public final String getEncoding() {
        String enc = super.getEncoding();
        return (isSet(enc))? enc: DEFAULT_ENCODING;
    }

    //-------------------------------------------------------------------------
    // CsvSource contract support
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public boolean hasTitleRow() {
        return this.titleRow;
    }

    /** {@inheritDoc} */
    @Override
    public void setTitleRow(boolean titleRow) {
        this.titleRow = titleRow;
    }

    /** {@inheritDoc} */
    @Override
    public String getSeparator() {
        return this.separator;
    }

    /** {@inheritDoc} */
    @Override
    public void setSeparator(String separator) {
        if (! isSet(separator)) {
            throw new IllegalArgumentException("separator");
        }
        this.separator = separator;
    }

    /** {@inheritDoc} */
    @Override
    public char getQuoteCharacter() {
        return this.quote2char(this.quote);
    }

    /** {@inheritDoc} */
    @Override
    public String getQuote() {
        return this.quote;
    }

    /** {@inheritDoc} */
    @Override
    public void setQuote(String quote) throws IllegalArgumentException {
        this.quote = this.quote2string(quote);
    }

    /** {@inheritDoc} */
    @Override
    public int getColumnCount() {
        return this.getColumnNames().size();
    }

    /** {@inheritDoc} */
    @Override
    public List<String> getColumnNames() {
        this.init();
        return this.headers;
    }

    /** {@inheritDoc} */
    @Override
    public final CloseableIterator<Row<String>> iterator() {
        this.init();
        try {
            return new RowIterator(this.newReader());
        }
        catch (IOException e) {
            throw new TechnicalException(null, e);
        }
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private void init() {
        if (this.headers == null) {
            CSVReader reader = null;
            try {
                reader = this.newReader();

                String[] firstRow = reader.readNext();
                if ((! this.titleRow) && (firstRow != null)) {
                    // Generate generic column names (A, B... Z, AA, AB...).
                    for (int i=0; i<firstRow.length; i++) {
                        firstRow[i] = this.getColumnName(i);
                    }
                }
                this.headers = Collections.unmodifiableList(
                    Arrays.asList((firstRow != null)? firstRow: new String[0]));
            }
            catch (IOException e) {
                throw new TechnicalException("file.read.error", e,
                                            this.getFilePath(), e.getMessage());
            }
            finally {
                if (reader != null) {
                    try { reader.close(); } catch (IOException e) { /* Ignore... */ }
                }
            }
        }
        // Else: Already initialized.
    }

    private CSVReader newReader() throws IOException {
        return new CSVReader(new InputStreamReader(this.getInputStream(),
                                                   this.getEncoding()),
                             Separator.valueOf(this.separator).getValue(),
                             this.getQuoteCharacter());
    }

    private String getColumnName(int n) {
        StringBuilder s = new StringBuilder();
        for (; n >= 0; n = n / 26 - 1) {
            s.insert(0, (char)(n % 26 + 65));
        }
        return s.toString();
    }

    private char quote2char(String s) {
        char c = DEFAULT_QUOTE_CHAR;
        if (isSet(s)) {
            c = s.charAt(0);
            if (s.length() != 1) {
                if (s.charAt(0) == '\\') {                
                    s = s.toLowerCase();
                    int i = -1;
                    try {
                        // Check for character escaping
                        if (s.startsWith("\\0x")) {
                            i = Integer.parseInt(s.substring(3), 16);
                        }
                        else if ((s.startsWith("\\u")) || (s.startsWith("\\x"))) {
                            i = Integer.parseInt(s.substring(2), 16);
                        }
                        else if (s.startsWith("\\0")) {
                            i = Integer.parseInt(s.substring(2), 8);
                        }
                    }
                    catch (NumberFormatException e) { /* Ignore... */ }

                    if (i != -1) {
                        c = (char)i;
                    }
                    else if ("\\t".equals(s)) {
                        c = '\t';
                    }
                    else if ("\\n".equals(s)) {
                        c = '\n';
                    }
                    else if ("\\r".equals(s)) {
                        c = '\r';
                    }
                }
                else if ("tab".equals(s)) {
                    c = '\t';
                }
                else if ("quote".equals(s)) {
                    c = '"';
                }
                else if ("nul".equals(s)) {
                    c = '\0';
                }
                else if ("vt".equals(s)) {
                    c = '\013';
                }
            }
        }
        return c;
    }

    private String quote2string(String quote) throws IllegalArgumentException {
        String q = null;
        if (isSet(quote)) {
            q = Character.toString(quote.charAt(0));

            if (quote.length() > 1) {
                String s = quote.toLowerCase();
                if (s.charAt(0) == '\\') {
                    // Check for character escaping
                    int i = -1;
                    if (s.startsWith("\\0x")) {
                        i = Integer.parseInt(s.substring(3), 16);
                    }
                    else if ((s.startsWith("\\u")) || (s.startsWith("\\x"))) {
                        i = Integer.parseInt(s.substring(2), 16);
                    }
                    else if (s.startsWith("\\0")) {
                        i = Integer.parseInt(s.substring(2), 8);
                    }
                    if ((i != -1) || ("\\t".equals(s)) || ("\\n".equals(s))
                                                       || ("\\r".equals(s))) {
                        q = s;
                    }
                    // Else: Quote char already set to backslash.
                }
                else if ("tab".equals(s)) {
                    q = Character.toString('\t');
                }
                else if ("quote".equals(s)) {
                    q = Character.toString('"');
                }
                else if (("nul".equals(s)) || ("vt".equals(s))) {
                    q = s;
                }
                // Else: Ignore additional character and only consider first.
            }
        }
        return q;
    }

    //-------------------------------------------------------------------------
    // RowIterator nested class
    //-------------------------------------------------------------------------

    /**
     * An {@link Iterator} over the data rows read from a CSV file.
     */
    private final class RowIterator implements CloseableIterator<Row<String>>
    {
        private final CSVReader reader;
        private final Map<String,Integer> keyMapping;
        private Row<String> nextRow = null;
        private boolean closed = false;

        /**
         * Create a new row iterator.
         * @param  reader   the provider of CSV data.
         *
         * @throws IOException if any error occurred accessing the CSV
         *                     content.
         */
        public RowIterator(CSVReader reader) throws IOException {
            this.reader = reader;
            Map<String,Integer> m = new LinkedHashMap<String,Integer>();
            int i = 0;
            for (String s : headers) {
                m.put(s, Integer.valueOf(i++));
            }
            this.keyMapping = Collections.unmodifiableMap(m);
            if (hasTitleRow()) {
                // Skip title row to exclude it from actual data.
                this.getNextRow();
            }
            this.nextRow = this.getNextRow();
        }

        /** {@inheritDoc} */
        @Override
        public boolean hasNext() {
            return (this.nextRow != null);
        }

        /** {@inheritDoc} */
        @Override
        public Row<String> next() {
            if (this.nextRow != null) {
                Row<String> current = this.nextRow;
                try {
                    this.nextRow = this.getNextRow();
                }
                catch (IOException e) {
                    throw new TechnicalException(null, e);
                }
                return current;
            }
            else {
                throw new NoSuchElementException();
            }
        }

        /** {@inheritDoc} */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /** {@inheritDoc} */
        @Override
        public void close() {
            if (! this.closed) {
                this.closed = true;
                try {
                    this.reader.close();
                }
                catch (IOException e) { /* Ignore... */ }
            }
            // Else: Already closed.
        }

        /**
         * Ensures resources are released even when
         * {@link #close()} has not been invoked by user class.
         */
        @Override
        protected void finalize() {
            this.close();
        }

        /**
         * Reads the new line from the CSV data.
         * @return a {@link Row} object wrapping the read data or
         *         <code>null</code> if the end of data was reached.
         * @throws IOException if any error occurred accessing the CSV
         *                     content.
         */
        private Row<String> getNextRow() throws IOException {
            Row<String> row = null;
            try {
                final String[] data = this.reader.readNext();
                if (data != null) {
                    row = new StringArrayRow(data, headers, this.keyMapping);
                }
            }
            finally {
                if (row == null) {              // EOF or error.
                    this.close();
                }
            }
            return row;
        }
    }

    //-------------------------------------------------------------------------
    // StringArrayRow nested class
    //-------------------------------------------------------------------------

    /**
     * A Row implementation wrapping an array of strings.
     * <p>
     * <i>Implementation notes</i>: this class is marked as public on
     * purpose. Otherwise the Velocity template engine fails to access
     * methods of this class.</p>
     */
    public final static class StringArrayRow implements Row<String>
    {
        private final String[] data;
        private final List<String> headers;
        private final Map<String,Integer> keyMapping;

        /**
         * Creates a new Row backed by a string array.
         * @param  data         the backing string array.
         * @param  keyMapping   the mapping between keys and array
         *                      indices.
         */
        public StringArrayRow(String[] data, List<String> headers,
                                             Map<String,Integer> keyMapping) {
            this.data = data;
            this.headers = headers;
            this.keyMapping = keyMapping;
        }

        /** {@inheritDoc} */
        @Override
        public int size() {
            return Math.min(this.data.length, this.keyMapping.size());
        }

        /** {@inheritDoc} */
        @Override
        public List<String> keys() {
            return this.headers;
        }

        /** {@inheritDoc} */
        @Override
        public String get(String key) {
            String v = null;
            Integer i = this.keyMapping.get(key);
            if (i != null) {
                v = this.get(i.intValue());
            }
            return v;
        }

        /** {@inheritDoc} */
        @Override
        public String getString(String key) {
            return this.get(key);
        }

        /** {@inheritDoc} */
        @Override
        public String get(int index) {
            if ((index < 0) || (index > this.keyMapping.size())) {
                throw new ArrayIndexOutOfBoundsException(index);
            }
            String v = null;
            if (index < this.data.length) {
                v = this.data[index];
            }
            // Else: Row has fewer data than announced in header => return null.

            return v;
        }

        /** {@inheritDoc} */
        @Override
        public String getString(int index) {
            return this.get(index);
        }

        /** {@inheritDoc} */
        @Override
        public Iterator<String> iterator() {
            return new Iterator<String>() {
                private int curPos = 0;

                @Override
                public boolean hasNext() {
                    return (this.curPos < size());
                }

                @Override
                public String next() {
                    if (! this.hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return get(this.curPos++);
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return Arrays.asList(this.data).toString();
        }
    }
}

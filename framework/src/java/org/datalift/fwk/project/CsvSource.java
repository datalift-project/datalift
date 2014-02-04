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

package org.datalift.fwk.project;


import java.util.List;

import org.datalift.fwk.util.CloseableIterable;


/**
 * A file source object describing a CSV (character separated values)
 * file.
 *
 * @author hdevos
 */
public interface CsvSource extends FileSource, CloseableIterable<Row<String>>
{
    /**
     * Supported separator characters.
     */
    public enum Separator {
        comma(','), semicolon(';'), tab('\t'), pipe('|'), space(' ');

        protected final char value;

        Separator(char s) {
            this.value = s;
        }

        public char getValue() {
            return value;
        }
    }

    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    public final static String DEFAULT_ENCODING = "ISO-8859-1";
    public final static char DEFAULT_QUOTE_CHAR = '"';

    //-------------------------------------------------------------------------
    // Methods
    //-------------------------------------------------------------------------

    /**
     * Returns whether the first line of the file contains column
     * headings.
     * @return <code>true</code> if the first line of the file contains
     *         column heading; <code>false</code> otherwise.
     */
    public boolean hasTitleRow();

    /**
     * Returns the row of the file that contains the column headings.
     * @return the row of the file that contains the column headings,
     *         <code>0</code> if no title row is present.
     */
    public int getTitleRow();

    /**
     * Defines the row that contains the column headings.
     * @param  titleRow   the row number (starting at 1), any non
     *                    positive number (0, -1...) indicating that no
     *                    title row is present.
     */
    public void setTitleRow(int titleRow);

    /**
     * Returns the separator character.
     * @return the separator character.
     */
    public String getSeparator();

    /**
     * Sets the separator character.
     * @param  sep    the separator character.
     */
    public void setSeparator(String sep);

    /**
     * Returns the quote character.
     * @return the quote character.
     */
    public char getQuoteCharacter();

    /**
     * Returns the quote character as a string.
     * @return the quote character as a string.
     */
    public String getQuote();

    /**
     * Sets the quote character.
     * @param  quote    the quote character as a string, to allow
     *                  expressing character values that are not
     *                  legal XML data character such as NUL, vertical
     *                  tab, etc.
     */
    public void setQuote(String quote);

    /**
     * Returns the first row of data to take into account when
     * importing data form this source, thus excluding the title row
     * (if any).
     * @return the first row to take into account.
     */
    public int getFirstDataRow();

    /**
     * Sets the first row of data to take into account when importing
     * data form this source, thus excluding the title row (if any).
     * @param  row   the first row to take into account, starting
     *               at 1.
     */
    public void setFirstDataRow(int row);

    /**
     * Returns the last row to take into account when importing
     * data form this source.
     * @return the last row to take into account, <code>0</code>
     *         if unspecified.
     */
    public int getLastDataRow();

    /**
     * Sets the last row to take into account when importing
     * data form this source.
     * @param  row   the last row to take into account, any non
     *               positive number (0, -1...) indicating the last
     *               row present in the file.
     */
    public void setLastDataRow(int row);

    /**
     * Returns the number of data columns present in the file.
     * @return the number of data columns.
     */
    public int getColumnCount();

    /**
     * Returns the column headings, building Excel-like names
     * (A, B ..., Z, AA, AB ...) if the file has no title row.
     * @return the column headings.
     */
    public List<String> getColumnNames();
}

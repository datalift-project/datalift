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

package org.datalift.fwk.util;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

import org.datalift.fwk.util.StringUtils;


@RunWith(JUnit4.class)
public class StringUtilsTest
{
    private final static String I18N_SAMPLE =
                        "¿ Iñtërnâtiônàlîzætiøn\u00a0: ç'Æßt \"dùr\" (éê) ?";
    private final static String TRUNCATE_SAMPLE =
                        "aBc dEf gHi jKl mNo pQr sTu vWx yZ";

    @Test
    public void testUrlifyOk() throws Exception {
        String out = StringUtils.urlify(I18N_SAMPLE);
        assertEquals("internationalizaetion-c-aesst-dur-ee", out);
    }

    @Test
    public void testUrlifyTruncate() throws Exception {
        String out = StringUtils.urlify(TRUNCATE_SAMPLE, 10);
        assertEquals("abc-def-gh", out);
    }

    @Test
    public void testCamelCaseOk() throws Exception {
        String out = StringUtils.toCamelCase(I18N_SAMPLE);
        assertEquals("internationalizaetionCAesstDurEe", out);
    }

    @Test
    public void testCamelCaseTruncate() throws Exception {
        String out = StringUtils.toCamelCase(TRUNCATE_SAMPLE, true, 10);
        assertEquals("AbcDefGhiJ", out);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testUrlifyNull() throws Exception {
        StringUtils.urlify(null);
    }
}

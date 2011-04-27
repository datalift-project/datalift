package org.datalift.fwk.util;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

import org.datalift.fwk.util.StringUtils;


@RunWith(JUnit4.class)
public class StringUtilsTest
{
    @Test
    public void testUrlify() throws Exception {
        String in = "(Æß) - Récupération requête \"Noël\" à l'œuvre";
        String out = StringUtils.urlify(in);
        assertEquals("aess-recuperation-requete-noel-a-l-oeuvre", out);
    }
}

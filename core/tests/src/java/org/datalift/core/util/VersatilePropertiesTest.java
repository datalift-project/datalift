/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project, 
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.core.util;


import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.JUnit4;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import org.datalift.fwk.util.Base64;


@RunWith(JUnit4.class)
public class VersatilePropertiesTest
{
    private VersatileProperties sut = null;

    @Before
    public void setUp() throws Exception {
        this.sut = new VersatileProperties();
    }

    @Test
    public void testSetGetProperty() throws Exception {
        String value = "Hello, world";

        this.sut.setProperty("x", value);
        assertSame(value, this.sut.getProperty("x"));
        assertSame(value, this.sut.get("x"));

        assertSame(value, this.sut.getProperty("y", value));

        this.sut.put("x", new Object());
        assertEquals(value, this.sut.getProperty("x", value));
    }

    @Test
    public void testResolveVariables() throws Exception {
        String[] parts = new String[] { "Hello", ", ", "world" };
        String value = parts[0] + parts[1] + parts[2];

        this.sut.setProperty("x", parts[0]);
        this.sut.setProperty("y", parts[2]);
        assertEquals(value, this.sut.resolveVariables("${x}"+parts[1]+"${y}"));
        assertEquals(parts[0]+parts[1]+"${z}",
                            this.sut.resolveVariables("${x}"+parts[1]+"${z}"));
        assertEquals(parts[0]+parts[1]+System.getProperty("user.name"),
                     this.sut.resolveVariables("${x}"+parts[1]+"${user.name}"));
    }

    @Test
    public void testResolveExpressions() throws Exception {
        String value = "Hello";
        this.sut.setProperty("x", value);

        this.sut.setProperty("foo", "${t:-${x}, you}");
        assertEquals(value + ", you", this.sut.getProperty("foo"));

        String expected = value + ", me";
        this.sut.setProperty("foo", "${t:=${x}, me}");
        assertEquals(expected, this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${t}");
        assertEquals(expected, this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${#t}");
        assertEquals(expected.length(), this.sut.getInt("foo", -1));

        this.sut.setProperty("foo", "${z:?${x}, you}");
        try {
            this.sut.getProperty("foo");
            fail("Exception expected");
        }
        catch (Exception e) {
            assertTrue(e instanceof RuntimeException);
            assertEquals(value + ", you", e.getMessage());
        }

        this.sut.setProperty("foo", "${t:+${x}, you}");
        assertEquals(value + ", you", this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${z:+${x}, you}");
        // assertNull(this.sut.getProperty("foo"));

        value = "unhappy code";
        this.sut.setProperty("t", value);
        this.sut.setProperty("foo", "${t:2}");
        assertEquals(value.substring(2), this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${t: " + (2 - value.length()) + "}");
        assertEquals(value.substring(2), this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${t:2:7}");
        assertEquals(value.substring(2, 7), this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${t: " + (2 - value.length()) + ":7}");
        assertEquals(value.substring(2, 7), this.sut.getProperty("foo"));

        this.sut.setProperty("foo", "${t:z}");
        assertEquals("${t:z}", this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${t\\:z}");
        assertEquals("${t\\:z}", this.sut.getProperty("foo"));
        this.sut.setProperty("foo", "${\\#t}");
        assertEquals("${\\#t}", this.sut.getProperty("foo"));
    }

    @Test
    public void testResolvePropertyValue() throws Exception {
        String[] parts = new String[] { "Hello", ", ", "world" };
        String value = parts[0] + parts[1] + parts[2];

        this.sut.setProperty("x", parts[0]);
        this.sut.setProperty("y", parts[2]);

        String raw = "${x}" + parts[1] + "${y}";
        this.sut.setProperty("foo", raw);
        assertEquals(value, this.sut.getProperty("foo"));
        assertSame(raw, this.sut.get("foo"));

        raw = "${x}" + parts[1] + "${z}";
        this.sut.setProperty("foo", raw);
        assertEquals(parts[0] + parts[1] + "${z}", this.sut.getProperty("foo"));
        assertSame(raw, this.sut.get("foo"));

        raw = "${x}" + parts[1] + "${user.name}";
        this.sut.setProperty("foo", raw);
        assertEquals(parts[0] + parts[1] + System.getProperty("user.name"),
                     this.sut.getProperty("foo"));
        assertSame(raw, this.sut.get("foo"));
    }

    @Test
    public void testOverrideBySystemProperties() throws Exception {
        String homeDir  = "C:\\Documents and Settings\\bguedes";
        String vmName   = ".Net CLR";
        String vmVendor = "M$";

        this.sut.setProperty("user.home",      homeDir);
        this.sut.setProperty("home.dir",       homeDir);
        this.sut.setProperty("java.vm.name",   vmName);
        this.sut.setProperty("java.vm.vendor", vmVendor);

        String system = System.getProperty("user.home");
        assertSame(system, this.sut.get("user.home"));
        assertEquals(system, this.sut.getProperty("user.home"));

        system = System.getProperty("java.vm.name");
        assertSame(system, this.sut.get("java.vm.name"));
        assertEquals(system, this.sut.getProperty("java.vm.name"));

        system = System.getProperty("java.vm.vendor");
        assertSame(system, this.sut.get("java.vm.vendor"));
        assertEquals(system, this.sut.getProperty("java.vm.vendor"));

        this.sut.setProperty("x",
             "${user.home}/${home.dir}: ${java.vm.vendor} ${java.vm.name}");
        assertNull(System.getProperty("x"));
        assertNull(System.getProperty("home.dir"));
        assertEquals(this.sut.getProperty("x"),
                     System.getProperty("user.home") + '/' + homeDir + ": " +
                     System.getProperty("java.vm.vendor") + ' ' +
                     System.getProperty("java.vm.name"));
    }

    @Test
    public void testRemoveOnNullValue() throws Exception {
        String value = "Hello";

        this.sut.setProperty("x", value);
        assertSame(value, this.sut.getProperty("x"));

        this.sut.setProperty("x", null);
        assertNull(this.sut.getProperty("x"));
        assertFalse(this.sut.containsKey("x"));
    }

    @Test
    public void testSetGetBigDecimal() throws Exception {
        String value = "9876543210.987654321";
        BigDecimal o = new BigDecimal(value);

        this.sut.putBigDecimal("x", o);
        assertEquals(value, this.sut.getProperty("x"));
        assertEquals(o, this.sut.getBigDecimal("x", null));
        assertEquals(o, this.sut.getBigDecimal("y", o));
        assertNull(this.sut.getBigDecimal("y", null));

        this.sut.setProperty("x", value);
        assertEquals(o, this.sut.getBigDecimal("x", null));

        this.sut.setProperty("x", "Invalid value");
        assertEquals(o, this.sut.getBigDecimal("x", o));
    }

    @Test
    public void testSetGetBigInteger() throws Exception {
        String value = "9876543210";
        BigInteger o = new BigInteger(value);

        this.sut.putBigInteger("x", o);
        assertEquals(value, this.sut.getProperty("x"));
        assertEquals(o, this.sut.getBigInteger("x", null));
        assertEquals(o, this.sut.getBigInteger("y", o));
        assertNull(this.sut.getBigInteger("y", null));

        this.sut.setProperty("x", value);
        assertEquals(o, this.sut.getBigInteger("x", null));

        this.sut.setProperty("x", "Invalid value");
        assertEquals(o, this.sut.getBigInteger("x", o));
    }

    @Test
    public void testSetGetBoolean() throws Exception {
        this.sut.putBoolean("x", true);
        assertEquals("true", this.sut.getProperty("x"));
        assertTrue(this.sut.getBoolean("x", false));
        assertTrue(this.sut.getBoolean("y", true));

        this.sut.setProperty("x", "tRuE");
        assertTrue(this.sut.getBoolean("x", false));
        this.sut.setProperty("x", "yEs");
        assertTrue(this.sut.getBoolean("x", false));
        this.sut.setProperty("x", "1");
        assertTrue(this.sut.getBoolean("x", false));

        this.sut.setProperty("x", "FalSe");
        assertFalse(this.sut.getBoolean("x", true));
        this.sut.setProperty("x", "nO");
        assertFalse(this.sut.getBoolean("x", true));
        this.sut.setProperty("x", "0");
        assertFalse(this.sut.getBoolean("x", true));
    }

    @Test
    public void testSetGetByteArray() throws Exception {
        String value = "$#@&� aZeRtYuIoP A��E���I��O��U�� (){}[]";
        byte[] o = value.getBytes();

        this.sut.putByteArray("x", o);
        assertEquals(Base64.encode(o, null), this.sut.getProperty("x"));
        assertTrue(Arrays.equals(o, this.sut.getByteArray("x", null)));
        assertSame(o, this.sut.getByteArray("y", o));
        assertTrue(this.sut.getByteArray("y", null) == null);

        this.sut.setProperty("x", Base64.encode(o, null));
        assertTrue(Arrays.equals(o, this.sut.getByteArray("x", null)));

        this.sut.setProperty("x", "Not a Base64-encoded value");
        assertSame(o, this.sut.getByteArray("x", o));
    }

    @Test
    public void testSetGetDouble() throws Exception {
        String value = "98765.4321";
        double o = Double.parseDouble(value);

        this.sut.putDouble("x", o);
        assertEquals(value, this.sut.getProperty("x"));
        assertEquals(o, this.sut.getDouble("x", Double.NaN), 1.0e-6);
        assertEquals(o, this.sut.getDouble("y", o), 1.0e-3);

        this.sut.setProperty("x", value);
        assertEquals(o, this.sut.getDouble("x", Double.NaN), 1.0e-6);

        this.sut.setProperty("x", "Invalid value");
        assertEquals(o, this.sut.getDouble("x", o), 1.0e-6);
    }

    @Test
    public void testSetGetInt() throws Exception {
        String value = "1234567890";
        int o = Integer.parseInt(value);

        this.sut.putInt("x", o);
        assertEquals(value, this.sut.getProperty("x"));
        assertEquals(o, this.sut.getInt("x", Integer.MAX_VALUE));
        assertEquals(o, this.sut.getInt("y", o));

        this.sut.setProperty("x", value);
        assertEquals(o, this.sut.getInt("x", Integer.MAX_VALUE));

        this.sut.setProperty("x", "Invalid value");
        assertEquals(o, this.sut.getInt("x", o));
    }

    @Test
    public void testSetGetLong() throws Exception {
        String value = "9876543210";
        long o = Long.parseLong(value);

        this.sut.putLong("x", o);
        assertEquals(value, this.sut.getProperty("x"));
        assertEquals(o, this.sut.getLong("x", Long.MAX_VALUE));
        assertEquals(o, this.sut.getLong("y", o));

        this.sut.setProperty("x", value);
        assertEquals(o, this.sut.getLong("x", Long.MAX_VALUE));

        this.sut.setProperty("x", "Invalid value");
        assertEquals(o, this.sut.getLong("x", o));
    }

    @Test
    public void testSetGetString() throws Exception {
        String value = "Hello, world";

        this.sut.putString("x", value);
        assertSame(value, this.sut.getProperty("x"));
        assertSame(value, this.sut.getString("x", "foo"));
        assertSame(value, this.sut.getString("y", value));

        this.sut.setProperty("x", value);
        assertEquals(value, this.sut.getString("x", "foo"));

        this.sut.put("x", new Object());
        assertEquals(value, this.sut.getString("x", value));
    }

    @Test
    public void testSetNullKey() throws Exception {
        try {
            this.sut.setProperty(null, "Illegal key");
            fail("Exception expected");
        }
        catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }

        try {
            this.sut.put(null, "Illegal key");
            fail("Exception expected");
        }
        catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testGetNullKey() throws Exception {
        try {
            this.sut.getProperty(null);
            fail("Exception expected");
        }
        catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }

        try {
            this.sut.get(null);
            fail("Exception expected");
        }
        catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }

    @Test
    public void testPutNullValue() throws Exception {
        try {
            this.sut.put("x", null);
            fail("Exception expected");
        }
        catch (Exception e) {
            assertTrue(e instanceof NullPointerException);
        }
    }
}


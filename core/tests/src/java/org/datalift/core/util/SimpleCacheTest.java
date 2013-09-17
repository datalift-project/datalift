package org.datalift.core.util;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;


@RunWith(JUnit4.class)
public class SimpleCacheTest
{
    @Test
    public void expiryTest() {
        final String key = "foo";
        final String value = "bar";
        SimpleCache<String,String> c = new SimpleCache<String,String>(5, 1);
        c.put(key, value);
        this.wait(0.5);
        assertEquals(value, c.get(key));
        this.wait(0.7);
        assertNull(c.get(key));
    }

    private void wait(double sec) {
        if (sec > 0.0) {
            try {
                Thread.sleep((long)(sec * 1000L));
            }
            catch (Exception e) { /* Ignore... */ }
        }
    }
}

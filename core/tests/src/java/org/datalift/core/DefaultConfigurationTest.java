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

package org.datalift.core;


import java.io.File;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.TreeMap;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import static org.junit.Assert.*;

import org.datalift.fwk.TechnicalException;

import static org.datalift.core.DefaultConfiguration.*;


@RunWith(JUnit4.class)
public class DefaultConfigurationTest
{
    private final static String RDF_STORE = "test";
    private final static String BEAN = "aBean";

    private Properties props = new Properties();

    @Before
    public void setUp() {
        this.props.put(DATALIFT_HOME, "tests");
        this.props.put(REPOSITORY_URIS, RDF_STORE);
        this.props.put(RDF_STORE + REPOSITORY_URL, "sail:///");
        this.props.put(RDF_STORE + REPOSITORY_DEFAULT_FLAG, "true");
        this.props.put(PRIVATE_STORAGE_PATH, ".");
    }

    @Test
    public void minimalConfigurationTest() throws Exception {
        DefaultConfiguration cfg = new DefaultConfiguration(this.props);
        cfg.init();
        assertNull("No public filestore shall be available",
                   cfg.getPublicStorage());
        assertEquals("Private filestore shall be local",
                     new File("x").getCanonicalFile(),
                     cfg.getPrivateStorage().getFile("x").getCanonicalFile());
        assertEquals("Temp. storage shall be the JVM temp. storage",
                     new File(System.getProperty("java.io.tmpdir")),
                     cfg.getTempStorage());
    }

    @Test(expected=TechnicalException.class)
    public void missingPrivateStorageTest() {
        this.props.remove(PRIVATE_STORAGE_PATH);
        new DefaultConfiguration(this.props).init();
        fail(PRIVATE_STORAGE_PATH + " property is mandatory");
    }

    @Test(expected=TechnicalException.class)
    public void invalidPublicStorageTest() {
        this.props.put(PUBLIC_STORAGE_PATH, "toto");
        new DefaultConfiguration(this.props).init();
        fail(PUBLIC_STORAGE_PATH + " directory shall exist");
    }

    @Test
    public void beanRetrievalTest() {
        DefaultConfiguration cfg = new DefaultConfiguration(this.props);
        cfg.init();
        Map<String,String> bean = new TreeMap<String,String>();
        bean.put(RDF_STORE, RDF_STORE);         // Ensure Map.equals() fails.
        // Register an retrieve bean by name.
        cfg.registerBean(BEAN, bean);
        assertEquals(bean, cfg.getBean(BEAN));
        // Register and retrieve bean by type (implemented interface and
        // super class).
        cfg.registerBean(bean);
        assertEquals(bean, cfg.getBean(NavigableMap.class));
        assertEquals(1, cfg.getBeans(NavigableMap.class).size());
        assertEquals(bean, cfg.getBean(Map.class));
        // Multiple beans implementing the same interface.
        cfg.registerBean(new Properties());
        assertEquals(1, cfg.getBeans(NavigableMap.class).size());
        assertEquals(2, cfg.getBeans(Map.class).size());
    }

    @Test(expected=MissingResourceException.class)
    public void namedBeanRetrievalError() {
        DefaultConfiguration cfg = new DefaultConfiguration(this.props);
        cfg.init();
        cfg.getBean(BEAN);
        fail("Missing bean should throw exception.");
    }

    @Test(expected=MissingResourceException.class)
    public void beanByTypeRetrievalError() {
        DefaultConfiguration cfg = new DefaultConfiguration(this.props);
        cfg.init();
        assertTrue("No bean of type Map shall be present",
                   cfg.getBeans(Map.class).isEmpty());
        cfg.getBean(Map.class);
        fail("Missing bean should throw exception.");
    }

    @Test
    public void beanRemovalTest() {
        DefaultConfiguration cfg = new DefaultConfiguration(this.props);
        cfg.init();
        Map<String,String> bean = new TreeMap<String,String>();
        bean.put(RDF_STORE, RDF_STORE);         // Ensure Map.equals() fails.
        // Register an retrieve bean by name.
        cfg.registerBean(BEAN, bean);
        assertEquals(bean, cfg.getBean(BEAN));
        cfg.removeBean(bean, BEAN);
        try {
            cfg.getBean(BEAN);
            fail("Bean should have been removed.");
        }
        catch (MissingResourceException e) { /* OK */ }
        // Register and retrieve bean by type (implemented interface and
        // super class).
        cfg.registerBean(bean);
        assertEquals(bean, cfg.getBean(NavigableMap.class));
        assertEquals(bean, cfg.getBean(Map.class));
        cfg.removeBean(bean, null);
        assertTrue("No bean of type NavigableMap shall be present",
                   cfg.getBeans(NavigableMap.class).isEmpty());
        assertTrue("No bean of type Map shall be present",
                   cfg.getBeans(Map.class).isEmpty());
        try {
            cfg.getBean(Map.class);
            fail("Bean should have been removed.");
        }
        catch (MissingResourceException e) { /* OK */ }
    }
}

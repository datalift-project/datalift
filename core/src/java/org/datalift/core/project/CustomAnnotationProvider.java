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

package org.datalift.core.project;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import com.clarkparsia.empire.annotation.RdfsClass;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.util.EmpireAnnotationProvider;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.datalift.fwk.log.Logger;
import org.datalift.fwk.util.StringUtils;


public final class CustomAnnotationProvider implements EmpireAnnotationProvider
{
    public final static String ANNOTATED_CLASSES_PROP = "annotated.classes";

    private final static Logger log = Logger.getLogger();

    private final Map<Class<? extends Annotation>,Collection<Class<?>>>
            annotatedClasses = new HashMap<Class<? extends Annotation>,Collection<Class<?>>>();

    @Inject
    public CustomAnnotationProvider(@Named("ec") EmpireConfiguration cfg) {
        String v = cfg.get(ANNOTATED_CLASSES_PROP);
        if (StringUtils.isBlank(v)) {
            throw new IllegalArgumentException("Missing property \""
                                        + ANNOTATED_CLASSES_PROP
                                        + "\" in Empire configuration");
        }
        for (String c : v.split("\\s*,\\s*")) {
            try {
                this.register(Class.forName(c));
            }
            catch (Exception e) {
                throw new RuntimeException("Failed to load class \"" +
                                           c + "\": " + e.getMessage(), e);
            }
        }
        // Force Javassist to take web application classes into account.
//        javassist.ClassPool.getDefault().insertClassPath(
//                                new javassist.ClassClassPath(this.getClass()));
    }

    @Override
    public Collection<Class<?>> getClassesWithAnnotation(
                                Class<? extends Annotation> annotation) {
        Collection<Class<?>> l = Collections.emptySet();
        if (this.annotatedClasses.containsKey(annotation)) {
            l = this.annotatedClasses.get(annotation);
        }
        log.debug("Annotation: {} -> {}", annotation.getName(), l);
        return l;
    }

    public void register(Class<?> c) {
        if (c.isAnnotationPresent(RdfsClass.class)) {
            this.register(c, RdfsClass.class);
        }
        else if (c.isAnnotationPresent(NamedQuery.class)) {
            this.register(c, NamedQuery.class);
        }
        else if (c.isAnnotationPresent(NamedQueries.class)) {
            this.register(c, NamedQueries.class);
        }
        else if (c.isAnnotationPresent(NamedNativeQuery.class)) {
            this.register(c, NamedNativeQuery.class);
        }
        else if (c.isAnnotationPresent(NamedNativeQueries.class)) {
            this.register(c, NamedNativeQueries.class);
        }
        // Else: ignore...
    }

    private void register(Class<?> c,
                          Class<? extends Annotation> annotation) {
        Collection<Class<?>> l = this.annotatedClasses.get(annotation);
        if (l == null) {
            l = new LinkedList<Class<?>>();
            this.annotatedClasses.put(annotation, l);
        }
        l.add(c);
        log.trace("Registered {} as annotated for {}",
                                            c.getName(), annotation.getName());
    }
}

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

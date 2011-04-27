package org.datalift.core.project;


import java.net.URI;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.Query;

import com.clarkparsia.empire.annotation.RdfsClass;
import com.clarkparsia.empire.impl.RdfQuery;


public abstract class GenericRdfJpaDao<T>
{
    protected final Class<? extends T> persistentClass;
    protected final String rdfType;

    protected final EntityManager entityMgr;


    public GenericRdfJpaDao(final Class<? extends T> persistentClass,
                            final EntityManager entityMgr) {
        if (persistentClass == null) {
            throw new IllegalArgumentException("persistentClass");
        }
        RdfsClass rdfsClass = persistentClass.getAnnotation(RdfsClass.class);
        if (rdfsClass == null) {
            throw new IllegalArgumentException(persistentClass.getName());
        }
        this.persistentClass = persistentClass;
        this.rdfType = rdfsClass.value();
        this.entityMgr = entityMgr;
    }

    @SuppressWarnings("unchecked")
    public Collection<T> getAll() {
        List<T> results = new LinkedList<T>();

        Query query = this.entityMgr.createQuery(
                       "where { ?result rdf:type " + this.rdfType + " . }");
        query.setHint(RdfQuery.HINT_ENTITY_CLASS, this.persistentClass);
        for (Object p : query.getResultList()) {
            results.add((T)p);
        }
        return results;
    }

    public T find(URI id) {
        return this.entityMgr.find(this.persistentClass, id);
    }

    public T get(URI id) {
        T entity = this.entityMgr.find(this.persistentClass, id);
        if (entity == null) {
            throw new EntityNotFoundException(id.toString());
        }
        return entity;
    }

    public void persist(T entity) {
        this.entityMgr.persist(entity);
    }

    public T save(T entity) {
        return this.entityMgr.merge(entity);
    }

    public void delete(T entity) {
        this.entityMgr.remove(entity);
    }

    public void delete(URI id) {
        this.delete(this.get(id));
    }

    @SuppressWarnings("unchecked")
    protected List<T> executeQuery(String query) {
        Query q = this.entityMgr.createQuery(query);
        q.setHint(RdfQuery.HINT_ENTITY_CLASS, this.persistentClass);
        return (List<T>)(q.getResultList());
    }
}

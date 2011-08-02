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

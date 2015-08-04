/*
 * Copyright / Copr. 2010-2014 Atos - Public Sector France -
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


import java.net.URI;
import java.util.Collection;
import java.util.List;


public interface GenericRdfDao
{
    /**
     * Retrieves all instances of the specified persistent class present
     * in the RDF store.
     * @param  entityClass   the persistent class of the objects to
     *                       retrieve.
     *
     * @return a collection (possibly empty) containing all persistent
     *         instances found in the underlying storage.
     * @throws IllegalArgumentException if <code>entityClass</code> is
     *         <code>null</code> or can not be mapped to a known
     *         persisted RDF type.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public <C> Collection<? extends C> getAll(Class<C> entityClass);

    /**
     * Search for an instance of the specified persistent class in
     * the RDF store. The instance may not exists.
     * @param  entityClass   the persistent class of the object to
     *                       retrieve.
     * @param  id            the RDF object identifier, as a URI.
     *
     * @return the object read from the RDF store or <code>null</code>
     *         if no object with the specified identifier was found.
     * @throws IllegalArgumentException if <code>entityClass</code> or
     *         <code>id</code> is <code>null</code> or
     *         <code>entityClass</code> can not be mapped to a known
     *         persisted RDF type.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public <C> C find(Class<C> entityClass, URI id);

    /**
     * Retrieves an instance of the specified persistent class from
     * the RDF store. The instance shall exist otherwise an error is
     * returned.
     * @param  entityClass   the persistent class of the object to
     *                       retrieve.
     * @param  id            the RDF object identifier, as a URI.
     *
     * @return the object read from the RDF store.
     * @throws ObjectNotFoundException if no object with the specified
     *         identifier was found.
     * @throws IllegalArgumentException if <code>entityClass</code> or
     *         <code>id</code> is <code>null</code> or
     *         <code>entityClass</code> can not be mapped to a known
     *         persisted RDF type.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public <C> C get(Class<C> entityClass, URI id);

    /**
     * Makes the specified RDF object persistent and saves its initial
     * state in the RDF store.
     * @param  entity   the object to be saved into the RDF store.
     *
     * @throws IllegalArgumentException if <code>entity</code> is
     *         <code>null</code>.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public void persist(Object entity);

    /**
     * Saves the specified RDF object, merging its new state with the
     * content of the RDF store.
     * @param  entity   the object to be saved into the RDF store.
     *
     * @throws IllegalArgumentException if <code>entity</code> is
     *         <code>null</code>.
     * @throws ObjectNotFoundException if no object matching
     *         <code>entity</code> was found in the RDF store.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public <C> C save(C entity);

    /**
     * Deletes the specified RDF object.
     * @param  entityClass   the persistent class of the object to
     *                       delete.
     * @param  id            the RDF object identifier, as a URI.
     *
     * @throws ObjectNotFoundException if no object with the specified
     *         identifier was found.
     * @throws IllegalArgumentException if <code>entityClass</code> or
     *         <code>id</code> is <code>null</code> or
     *         <code>entityClass</code> can not be mapped to a known
     *         persisted RDF type.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public <C> void delete(Class<C> entityClass, URI id);

    /**
     * Deletes the specified RDF object.
     * @param  entity   the object to be deleted from the RDF store.
     *
     * @throws IllegalArgumentException if <code>entity</code> is
     *         <code>null</code>.
     * @throws ObjectNotFoundException if no object matching
     *         <code>entity</code> was found in the RDF store.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public void delete(Object entity);

    /**
     * Executes the specified JPA-modified SPARQL query on the RDF
     * store and returns the collection of matched objects.
     * @param  query         the JPA-modified SPARQL query.
     * @param  entityClass   the persistent class of the objects to
     *                       retrieve.
     *
     * @return the collection of matched objects, possibly empty.
     * @throws IllegalArgumentException if <code>query</code> is
     *         <code>null</code> or empty.
     * @throws IllegalArgumentException if <code>query</code> is
     *         <code>null</code> or empty, <code>entityClass</code> is
     *         <code>null</code> or can not be mapped to a known
     *         persisted RDF type.
     * @throws PersistenceException if any error occurred accessing the
     *         RDF store.
     */
    public <C> List<C> executeQuery(String query, Class<C> entityClass);
}

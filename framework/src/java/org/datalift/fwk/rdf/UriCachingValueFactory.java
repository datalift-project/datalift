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

package org.datalift.fwk.rdf;


import java.util.LinkedHashMap;
import java.util.Map;

import javax.xml.datatype.XMLGregorianCalendar;

import org.openrdf.model.BNode;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.model.ValueFactory;
import org.openrdf.model.impl.ValueFactoryImpl;

import org.datalift.fwk.log.Logger;


/**
 * A {@link ValueFactory} decorator that manages a cache of the created
 * URIs to avoid allocating again and again the same URI objects, e.g.
 * URIs of RDF subjects or properties.
 * <p>
 * @author lbihanic
 *
 */
public final class UriCachingValueFactory implements ValueFactory
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------

    public final static int DEFAULT_CACHE_SIZE = 1024;

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final ValueFactory wrappedFactory;
    private final Map<String,URI> uriCache;

    private long callCount = 0L;
    private long cacheHits = 0L;

    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    public UriCachingValueFactory() {
        this(null);
    }

    public UriCachingValueFactory(ValueFactory wrappedFactory) {
        this(wrappedFactory, DEFAULT_CACHE_SIZE);
    }

    public UriCachingValueFactory(ValueFactory wrappedFactory,
                                                        final int cacheSize) {
        if (wrappedFactory == null) {
            wrappedFactory = ValueFactoryImpl.getInstance();
        }
        this.wrappedFactory = wrappedFactory;
        this.uriCache = new LinkedHashMap<String,URI>(cacheSize, 0.75f, true)
            {
                @Override
                protected boolean removeEldestEntry(
                                                Map.Entry<String,URI> eldest) {
                    return this.size() > cacheSize;
                }
            };
    }

    //-------------------------------------------------------------------------
    // Overridden ValueFactory methods
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public URI createURI(String uri) {
        this.callCount++;

        URI u = this.uriCache.get(uri);
        if (u == null) {
            u = this.wrappedFactory.createURI(uri);
            this.uriCache.put(uri, u);
        }
        else {
            this.cacheHits++;
        }
        return u;
    }

    /** {@inheritDoc} */
    @Override
    public URI createURI(String namespace, String localName) {
        return this.createURI(namespace + localName);
    }

    //-------------------------------------------------------------------------
    // Delegated ValueFactory methods
    //-------------------------------------------------------------------------

    /** {@inheritDoc} */
    @Override
    public BNode createBNode() {
        return this.wrappedFactory.createBNode();
    }

    /** {@inheritDoc} */
    @Override
    public BNode createBNode(String nodeID) {
        return this.wrappedFactory.createBNode(nodeID);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(boolean value) {
        return this.wrappedFactory.createLiteral(value);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(byte value) {
        return this.wrappedFactory.createLiteral(value);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(double value) {
        return this.wrappedFactory.createLiteral(value);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(float value) {
        return this.wrappedFactory.createLiteral(value);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(int value) {
        return this.wrappedFactory.createLiteral(value);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(long value) {
        return this.wrappedFactory.createLiteral(value);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(short value) {
        return this.wrappedFactory.createLiteral(value);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(String label, String language) {
        return this.wrappedFactory.createLiteral(label, language);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(String label, URI datatype) {
        return this.wrappedFactory.createLiteral(label, datatype);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(String label) {
        return this.wrappedFactory.createLiteral(label);
    }

    /** {@inheritDoc} */
    @Override
    public Literal createLiteral(XMLGregorianCalendar calendar) {
        return this.wrappedFactory.createLiteral(calendar);
    }

    /** {@inheritDoc} */
    @Override
    public Statement createStatement(Resource subject, URI predicate,
                                     Value object, Resource context) {
        return this.wrappedFactory.createStatement(subject, predicate,
                                                            object, context);
    }

    /** {@inheritDoc} */
    @Override
    public Statement createStatement(Resource subject,
                                     URI predicate, Value object) {
        return this.wrappedFactory.createStatement(subject, predicate, object);
    }

    //-------------------------------------------------------------------------
    // UriCachingValueFactory contract definition
    //-------------------------------------------------------------------------

    /** Logs the cache statistics. */
    public void displatStats() {
        Logger.getLogger().info("Cache hits: {}/{} ({}%)",
                    Long.valueOf(this.cacheHits), Long.valueOf(this.callCount),
                    Double.valueOf(
                        ((this.cacheHits * 10000) / this.callCount) / 100.0));
    }
}

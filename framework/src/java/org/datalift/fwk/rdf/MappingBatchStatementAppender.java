package org.datalift.fwk.rdf;


import java.net.URISyntaxException;

import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Statement;
import org.openrdf.model.URI;
import org.openrdf.model.Value;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFHandler;

import org.datalift.fwk.util.UriMapper;

import static org.datalift.fwk.rdf.RdfUtils.*;


/**
 * An {@link RDFHandler} implementation that inserts triples into an
 * RDF store in batches of configurable size, allowing to
 * {@link UriMapper translate URIs} on the fly. It also sanitizes the
 * string literal values by
 * {@link RdfUtils#removeInvalidDataCharacter(String) removing invalid
 * characters} that may be present in N3 or Turtle RDF files.
 *
 * @author lbihanic
 */
public final class MappingBatchStatementAppender extends BatchStatementAppender
{
    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------

    private final UriMapper mapper;

    /**
     * Creates a new statement appender with the default batch size.
     * @param  cnx           the connection to the RDF store.
     * @param  targetGraph   the named graph to which the inserted
     *                       triples shall belong or <code>null</code>
     *                       to insert the triples in the default graph.
     * @param  mapper        an optional URI mapper to translate URIs
     *                       while processing triples.
     */
    public MappingBatchStatementAppender(RepositoryConnection cnx,
                                         URI targetGraph,
                                         UriMapper mapper) {
        super(cnx, targetGraph);
        this.mapper = mapper;
    }

    /**
     * Creates a new statement appender with the specified batch size.
     * @param  cnx           the connection to the RDF store.
     * @param  targetGraph   the named graph to which the inserted
     *                       triples shall belong or <code>null</code>
     *                       to insert the triples in the default graph.
     * @param  mapper        an optional URI mapper to translate URIs
     *                       while processing triples.
     * @param  batchSize     the size of triple batches, as a number of
     *                       triples.
     */
    public MappingBatchStatementAppender(RepositoryConnection cnx,
                                         URI targetGraph,
                                         UriMapper mapper, int batchSize) {
        super(cnx, targetGraph, batchSize);
        this.mapper = mapper;
    }

    //-------------------------------------------------------------------------
    // BatchStatementAppender contract support
    //-------------------------------------------------------------------------

    @Override
    protected void addStatement(Statement stmt) throws RepositoryException {
        Resource s = stmt.getSubject();
        org.openrdf.model.URI p = stmt.getPredicate();
        // Check that literal values are valid.
        Value o = this.checkStringLitteral(stmt.getObject());

        if (mapper != null) {
            // Map URIs.
            s = (Resource)(this.mapValue(s));
            p = this.mapUri(p);
            o = this.mapValue(o);
        }
        this.cnx.add(s, p, o, this.targetGraph);
    }

    //-------------------------------------------------------------------------
    // Specific implementation
    //-------------------------------------------------------------------------

    private Value checkStringLitteral(Value v) {
        if (v instanceof Literal) {
            Literal l = (Literal)v;
            if (l.getDatatype() == null) {
                String s = l.stringValue();
                if (! isValidStringLiteral(s)) {
                    v = valueFactory.createLiteral(
                                removeInvalidDataCharacter(s), l.getLanguage());
                }
            }
        }
        return v;
    }

    private Value mapValue(Value v) {
        return (v instanceof org.openrdf.model.URI)?
                                    this.mapUri((org.openrdf.model.URI)v): v;
    }

    private org.openrdf.model.URI mapUri(org.openrdf.model.URI u) {
        try {
            return this.valueFactory.createURI(
                        this.mapper.map(new java.net.URI(u.stringValue()))
                                   .toString());
        }
        catch (URISyntaxException e) {
            // Should never happen.
            throw new RuntimeException(e);
        }
    }
}

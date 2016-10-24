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

package org.datalift.fwk.project;

import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.datalift.fwk.util.StringUtils.isBlank;


/**
 * A source of data, external (file, URL, database query...) or
 * internal (named graph, SPARQL query...).
 *
 * @author hdevos
 */
public interface Source
{
    //-------------------------------------------------------------------------
    // SourceType enumeration
    //-------------------------------------------------------------------------

    /** The supported DataLift source types. */
    public static final class SourceType
    {
        private final static Map<String,SourceType> srcTypes =
                                            new HashMap<String,SourceType>();

        public static final SourceType RdfFileSource =
                new SourceType("datalift.rdf.file.source",
                               "RdfFileSource", "source.rdf.file.label");
        public static final SourceType CsvSource =
                new SourceType("datalift.csv.file.source",
                               "CsvSource", "source.csv.file.label");
        public static final SourceType SqlQuerySource =
                new SourceType("datalift.sql.query.source",
                               "SqlQuerySource", "source.sql.query.label");
        public static final SourceType SqlDatabaseSource =
                new SourceType("datalift.sql.db.source",
                               "SqlDatabaseSource", "source.sql.db.label");
        public static final SourceType TransformedRdfSource =
                new SourceType("datalift.internal.source",
                               "TransformedRdfSource", "source.internal.label");
        public static final SourceType SparqlSource =
                new SourceType("datalift.sparl.query.source",
                               "SparqlSource", "source.sparql.query.label");
        public static final SourceType XmlSource =
                new SourceType("datalift.xml.file.source",
                               "XmlSource", "source.xml.file.label");
        public static final SourceType ShpSource =
                new SourceType("datalift.shp.file.source",
                               "ShpSource", "source.shp.file.label");
        public static final SourceType GmlSource =
                new SourceType("datalift.gml.file.source",
                               "GmlSource", "source.gml.file.label");
        public static final SourceType WfsSource =
                new SourceType("datalift.wfs.service.source",
                		 "WfsSource", "source.wfs.service.label");
                              
        public static final SourceType SosSource =
                new SourceType("datalift.sos.service.source",
                		 "SosSource","source.sos.service.label");


        private final String id;
        private final String name;
        private final String label;

        private SourceType(String id, String name, String label) {
            if (isBlank(id)) {
                throw new IllegalArgumentException("id");
            }
            if (isBlank(name)) {
                throw new IllegalArgumentException("name");
            }
            if (isBlank(label)) {
                throw new IllegalArgumentException("label");
            }
            this.id    = id;
            this.name  = name;
            this.label = label;
            srcTypes.put(id, this);
        }

        public final String getId() {
            return this.id;
        }

        public final String getName() {
            return this.name;
        }

        public final String getLabel() {
            return this.label;
        }

        public final String name() {
            return this.getName();
        }

        /** {@inheritDoc} */
        @Override
        public String toString() {
            return this.name();
        }

        /** {@inheritDoc} */
        @Override
        public int hashCode() {
            return this.id.hashCode();
        }

        /** {@inheritDoc} */
        @Override
        public boolean equals(Object o) {
            return (o instanceof SourceType) &&
                   (this.id.equals(((SourceType)o).id));
        }

        public static SourceType getType(String id) {
            return getType(id, null);
        }

        public static SourceType getType(String id, String label) {
            SourceType t = srcTypes.get(id);
            if ((t == null) && (! isBlank(label))) {
                t = new SourceType(id, id, label);
            }
            return t;
        }

        public static Collection<SourceType> values() {
            return Collections.unmodifiableCollection(srcTypes.values());
        }
    }

    //-------------------------------------------------------------------------
    // Source contract definition
    //-------------------------------------------------------------------------

    /**
     * Returns the source identifier as a URI.
     * @return the source identifier.
     */
    public String getUri();

    /**
     * Returns the source title.
     * @return the source title.
     */
    public String getTitle();

    /**
     * Sets the source title.
     * @param  title   the source title string.
     */
    public void setTitle(String title);

    /**
     * Returns the source description.
     * @return the source description.
     */
    public String getDescription();

    /**
     * Sets the source description.
     * @param  description   the source description string.
     */
    public void setDescription(String description);

    /**
     * Returns the origin URL for this source.
     * @return the origin URL.
     */
    public String getSourceUrl();

    /**
     * Sets the origin URL for this source.
     * @param  url   the origin URL.
     */
    public void setSourceUrl(String url);

    /**
     * Returns the {@link SourceType source type}.
     * @return the source type.
     */
    public SourceType getType();

    /**
     * Returns the {@link Project} object this source belongs to.
     * @return the owning project.
     */
    public Project getProject();

    /**
     * Deletes all resources associated to this source (uploaded files,
     * temporary files, named graphs...).
     */
    public void delete();
}

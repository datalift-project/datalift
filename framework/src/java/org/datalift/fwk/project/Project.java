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


import java.net.URI;
import java.util.Collection;
import java.util.Date;

import org.datalift.fwk.prov.Event;
import org.datalift.fwk.replay.Workflow;


/**
 * A data-lifting project.
 *
 * @author hdevos
 */
public interface Project
{
    public String getUri();

    public String getTitle();
    public void setTitle(String t);

    public String getOwner();

    public String getDescription();
    public void setDescription(String d);

    public void add(Source source);
    public Collection<Source> getSources();
    public Source getSource(URI uri);
    public Source getSource(String uri);
    public void remove(Source source);

    public Date getModificationDate();
    public void setModificationDate(Date date);

    public URI getLicense();
    public void setLicense(URI license);

    public Collection<Ontology> getOntologies();
    public void addOntology(Ontology ontology);
    public Ontology getOntology(String title);
    public Ontology removeOntology(String title);
    
    public void addEvent(Event event);
    public void removeEvent(Event event);
    public Collection<Event> getEvents();
    
    public void addWorkflow(Workflow workflow);
    public Collection<Workflow> getWorkflows();
    public Workflow getWorkflow(URI uri);
    public void removeWorkflow(URI uri);
}

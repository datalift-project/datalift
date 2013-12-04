/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *                  A. Valensi
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

import java.util.Date;


/**
 * An event is used to know what was happened to transform an entity A to an 
 * entity B.
 * @author avalensi
 */
public interface Event {

    //-------------------------------------------------------------------------
    // Instance members
    //-------------------------------------------------------------------------
	
	/**
	 * Base event URI.
	 * 
	 * TODO: change for a dynamic string.
	 */
	public static final String BASE_EVENT_URI = 
			"http://www.datalift.org/project/event"; 

    //-------------------------------------------------------------------------
	// Event contract definition
    //-------------------------------------------------------------------------

	/**
	 * Get the {@link URI} of the {@link Event}.
	 * 
	 * @return the {@link URI} of the {@link Event}.
	 */
    public String getUri();

    /**
     * Get the parameters of the {@link Event}. If it is a {@link Task}, the 
     * parameters are used to execute it.
     * 
     * @return the parameters of the {@link Event}.
     */
	public String getParameters();
	
	/**
     * Set the parameters of the {@link Event}. If it is a {@link Task}, the 
     * parameters are used to execute it.
	 * 
	 * @param parameters
	 */
	public void setParameters(String parameters);
	
	/**
	 * Get starting {@link Date} of the {@link Event}.
	 * 
	 * @return the starting {@link Date}.
	 */
	public Date getStartedAtTime();
	
	/**
	 * Set starting {@link Date} of the {@link Event}.
	 * 
	 * @param startedAtTime is the starting date.
	 */
	public void setStartedAtTime(Date startedAtTime);
	
	/**
	 * Get ending {@link Date} of the {@link Event}.
	 * 
	 * @return the ending {@link Date}.
	 */
	public Date getEndedAtTime();
	
	/**
	 * Set ending {@link Date} of the {@link Event}.
	 * 
	 * @param endedAtTime the ending {@link Date}.
	 */
	public void setEndedAtTime(Date endedAtTime);
	
	/**
	 * Get the {@link User} who initiated the {@link Event}.
	 * 
	 * @return the {@link User} who initiated the {@link Event}.
	 */
	public User getWasAssociatedWith();

	/**
	 * Set the {@link User} who initiated the {@link Event}.
	 * 
	 * @param wasAssociatedWith
	 */
	public void setWasAssociatedWith(User wasAssociatedWith);
	
	/**
	 * Get the {@link Entity} which use this {@link Event}.
	 * 
	 * @return the {@link Entity};
	 */
	public Entity getUsed();

	/**
	 * Set the {@link Entity} which use this {@link Event}.
	 * 
	 * @param used
	 */
	public void setUsed(Entity used);
	
	/**
	 * In case of the {@link Event} is created from another {@link Event}, get 
	 * this other {@link Event}.
	 * 
	 * @return the creator Event.
	 */
	public Event getWasInformedBy();
	
	/**
	 * In case of the {@link Event} is created from another {@link Event}, set 
	 * this other {@link Event}.
	 * 
	 * @return the creator Event.
	 */
	public void setWasInformedBy(Event wasInformedBy);
}

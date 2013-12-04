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

/**
 * ProcessingTask is an event used to execute the task of a 
 * {@link TransformationModule}.
 * 
 * @author avalensi
 */
public interface ProcessingTask extends Runnable, Event {

    //-------------------------------------------------------------------------
	// Enum
    //-------------------------------------------------------------------------	
	
	public enum EventStatus {
		NEW,
		RUNNING,
		FAIL,
		COMPLETE
	}

    //-------------------------------------------------------------------------
	// ProcessingTask contract definition
    //-------------------------------------------------------------------------
	
	/**
	 * Return the id of the class which will execute the {@link ProcessingTask}.
	 * 
	 * @return transformationId
	 */
	public String getTransformationId();

	/**
	 * Get the {@link EventStatus} of the {@link ProcessingTask}.
	 * 
	 * @return The {@link EventStatus}.
	 */
	public EventStatus getEventStatus();

	/**
	 * Set the {@link EventStatus} of the {@link ProcessingTask}.
	 * 
	 * @param eventStatus
	 */
	public void setEventStatus(EventStatus eventStatus);

	/**
	 * Add a parameter into the {@link ProcessingTask} to use it in the 
	 * execution of the {@link TransformationModule}.
	 * saveParam() must be call to save the parameter in the 
	 * {@link ProcessingTask}.
	 * 
	 * <pre>
	 * {@code
	 * this.addParam("param1", obj1);
	 * this.addParam("param2", obj2);
	 * this.saveParams();
	 * }
	 * </pre>
	 * 
	 * @param name is key of the parameter.
	 * @param param is the parameter.
	 */
	public void addParam(String name, Object param);

	/**
	 * It is use to save the parameters. It must be called after calling 
	 * addParam().
	 * 
	 * <pre>
	 * {@code
	 * this.addParam("param1", obj1);
	 * this.addParam("param2", obj2);
	 * this.saveParams();
	 * }
	 * </pre>
	 */
	public void saveParams();

	/**
	 * It is used to load the parameters. It must be called before getting a
	 * param using with getParam().
	 * 
	 * <pre>
	 * {@code
	 * this.loadParams();
	 * String  obj1 = this.getParam("param1");
	 * Integer obj2 = this.getParam("param2");
	 * }
	 * </pre>
	 */
	public void loadParams() throws Exception;

	/**
	 * It is used to get a parameter. It must be called after loadParams().
	 * 
	 * <pre>
	 * {@code
	 * this.loadParams();
	 * String  obj1 = this.getParam("param1");
	 * Integer obj2 = this.getParam("param2");
	 * }
	 * </pre>
	 * 
	 * @param name is the key to get a param.
	 */
	public Object getParam(String name);

}

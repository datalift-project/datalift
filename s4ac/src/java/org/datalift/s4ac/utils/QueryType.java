/*
 * Contact: serena.villata@inria.fr
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 * 
 * @author Serena Villata (INRIA - Sophia-Antipolis)
 * 
 */


package org.datalift.s4ac.utils;


public enum QueryType
{
    ASK         (CRUDType.READ),
    CONSTRUCT   (CRUDType.READ),
    SELECT      (CRUDType.READ),
    UPDATE      (CRUDType.UPDATE),
    DELETE      (CRUDType.DELETE),
    DESCRIBE    (CRUDType.READ),
    DROP        (CRUDType.DELETE),
    INSERT      (CRUDType.CREATE),
    LOAD        (CRUDType.CREATE),
    CLEAR       (CRUDType.DELETE),
    CREATE      (CRUDType.CREATE),
    ADD         (CRUDType.CREATE),
    MOVE        (CRUDType.UPDATE),
    COPY        (CRUDType.UPDATE),
    UNKNOWN     (CRUDType.UNKNOWN);

    public final CRUDType crudType;

    QueryType(CRUDType crudType) {
        this.crudType = crudType;
    }
}

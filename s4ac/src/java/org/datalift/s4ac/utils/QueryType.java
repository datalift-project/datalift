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

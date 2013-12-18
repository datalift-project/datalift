package org.datalift.interlinker.sparql;

public interface DataSource {
	/** Binding for the default subject var in SPARQL. */
    public static final String SUBJECT_BINDER = "s";
    /** Binding for the default predicate var in SPARQL. */
    public static final String PREDICATE_BINDER = "p";
    /** Binding for the default object var in SPARQL. */
    public static final String OBJECT_BINDER = "o";
    /** Binding for the default subject var in SPARQL when we want to run the query on the data source */
    public static final String SOURCE_SUBJ_BINDER="ss";
    /** Binding for the default subject var in SPARQL when we want to run the query on the data target */
    public static final String TARGET_SUBJ_BINDER="ts";
    
    public SparqlCursor query(String context, String[] columns, String[] whereConditions);
}

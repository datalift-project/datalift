package org.datalift.s4ac.resources;



import java.util.Set;

import org.datalift.s4ac.utils.CRUDType;
import org.datalift.s4ac.utils.QueryAnalyser;
import org.datalift.s4ac.utils.QueryType;

public class SecuredSparqlQuery {
	
	private QueryType qryType;
	private CRUDType crudType;
	private String qrycontent;
	

	public SecuredSparqlQuery(String qrycontent, Set<String> ng)  {
		this.qrycontent = qrycontent;
		setRestrictions(ng);
	}

	public void analyse(){
		qryType = QueryAnalyser.analyse(qrycontent);
	}
	
	public void setRestrictions(Set<String> namedGraphs){

		QueryAnalyser.analyse(qrycontent);
		
    	String from="";
    	for(String graph : namedGraphs){
//    		from += "\nFROM NAMED <"+graph+">\nFROM <"+graph+"> ";
    		from += "\nFROM <"+graph+"> ";
    	}
    	
    	String start=new String(), where=new String();
    	int idx;
    	if(qrycontent.lastIndexOf("WHERE")>-1) {
    		idx = qrycontent.lastIndexOf("WHERE");
    		start = qrycontent.substring(0, idx-1);
    		where = qrycontent.substring(idx);
    	}
    	else if(qrycontent.lastIndexOf("where")>-1){
    		idx = qrycontent.lastIndexOf("where");
    		start = qrycontent.substring(0, idx-1);
    		where = qrycontent.substring(idx);
    	}
    	
    	qrycontent = start + from + "\n"+ where;


	}
	
	public QueryType getQryType() {
		return qryType;
	}	
	
	public CRUDType getCrudType() {
		if(this.crudType ==null){
			switch(this.qryType){
				case INSERT:			this.crudType=CRUDType.CREATE; 	break;
//				case INSERT_DATA:		this.crudType=CRUDType.CREATE; 	break;
//				case INSERT_DATA_GRAPH: this.crudType=CRUDType.CREATE; 	break;
				case LOAD: 				this.crudType=CRUDType.CREATE; 	break;
				case CONSTRUCT: 		this.crudType=CRUDType.READ; 	break;
				case SELECT: 			this.crudType=CRUDType.READ; 	break;
				case ASK:				this.crudType=CRUDType.READ; 	break;
				case UPDATE: 			this.crudType=CRUDType.UPDATE; 	break;
				case DELETE:			this.crudType=CRUDType.DELETE; 	break;
//				case DELETE_DATA:		this.crudType=CRUDType.DELETE; 	break;
//				case DELETE_WHERE:		this.crudType=CRUDType.DELETE; 	break;
//				case DELETE_DATA_GRAPH: this.crudType=CRUDType.DELETE; 	break;
				case DROP: 				this.crudType=CRUDType.DELETE; 	break;
				case CLEAR: 			this.crudType=CRUDType.DELETE; 	break;
				case CREATE: 			this.crudType=CRUDType.DELETE; 	break;
				case ADD: 				this.crudType=CRUDType.UNKNOWN; break;
				case MOVE: 				this.crudType=CRUDType.UNKNOWN; break;
				case COPY: 				this.crudType=CRUDType.UNKNOWN; break;
				case UNKNOWN: 			this.crudType=CRUDType.UNKNOWN; break;
				default: 				this.crudType=CRUDType.UNKNOWN;

			}
		}
		return crudType;
	}
	public String getQrycontent() {
		return this.qrycontent;
	}
	
	
}

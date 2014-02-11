settings = {
	/**
	 * USER SETTINGS
	 *
	 * These represent the main configuration options
	 * 
	 * dateFormat: allowed values are "dd/mm/yyyy" or "mm/dd/yyyy"
	 * timeFormat: allowed value is "HH:MM"
	 **/
	
	user: "Admin",
	password: "pwd",
	language: "en",
	dateFormat: "dd/mm/yyyy", 
	timeFormat: "HH:MM", 
	
	/**
	 * RDF ENDPOINT SETTINGS
	 *
	 * They represent the main configuration options about the
	 * data permanent storing.
	 *
	 * RDFInterface: specifies the type of RDF backend. The only allowed value is "sparql";
	 * policyNamedGraphURI: is the URI of the named graph used to store the policies and
	 *						the access conditions. It is used only if backend is a triple
	 * 						store (RDFInterface set to "sparql");
	 *						
	 * SPARQLEndPoint: allows to specify the triple store used. Valid values are "sesame", "fuseki",
	 *				   "virtuoso";
	 * SPARQLEndPointInfo: are the specific settings for the triple store. They are taken into account if RDFInterface
	 * 					   is set to "sparql";
	 * 		host: default is localhost for any triple store;
	 *   	port: default is 8080 for sesame under tomcat and 3030 for fuseki;
	 *		dataset: indicates the dataset name to use. Must be equal to name configuration parameter 
	 *				 of fuseki or to repository configuration parameter of sesame;
	 *		testDataset: indicates the dataset name to use when testing policy (to avoid to pollute
	 *					 real dataset). Must be equal to name configuration parameter 
	 *				     of fuseki or to repository configuration parameter of sesame;
	 *		
	 *		sesame parameters (used only with sesame trilpe store)
	 *	
	 *		sesameServerLocation: specifis the path of the HTTP server for sesame triple store
	 *						eg if your sesame server is 'http://localhost:8080/openrdf-sesame'
	 *						then serverLocation is 'openrdf-sesame'
	 *						Used only for sesame;
	 *		sesameInfer (optional): specifies whether inferred statements should be included in the query evaluation. 
	 *						   Inferred statements are included by default. Values other than "true" (ignoring case) 
	 *						   restricts the query evluation to explicit statements only. 
	 *
	 *		fuseki parameters (used only with fuseki triple store)
	 *
	 *		fusekiServiceQuery: is the SPARQL query service path for SPARQL 1.1 query language  
	 *					  queries (only for fuseki). It is part of the URL to query the 
	 *					  dataset to read. eg a select query would be send to the URL
	 *					  http://localhost:3030/books/query (serviceQuery set to 'query');
     *  	fusekiServiceUpdate: is the SPARQL query service path for update (SPARQL 1.1 update) queries, 
	 *					   eg a insert query would be send to the URL
	 *					   http://localhost:3030/books/update (serviceQuery set to 'update');
     * 
	 **/
	 
	// RDFInterface: "sparql",
	// policyNamedGraphURI: "http://museum.example.org/policies/",
	// SPARQLEndPoint: "fuseki",
	// SPARQLEndPointInfo: {
	// 	host: "localhost",
	//     port: "3030",
	// 	dataset: "museum",
	// 	testDataset: "museum-sandbox",
	// 	//uncomment this to prevent inferred statement in the query result
	// 	//infer: "false",
	// 	sesameServerLocation: "openrdf-sesame",
	// 	fusekiServiceQuery: "query" ,
 //        fusekiServiceUpdate: "update" ,
	// },
	

	RDFInterface: "sparql",
	policyNamedGraphURI: "http://museum.example.org/policies/",
	SPARQLEndPoint: "sesame",
	SPARQLEndPointInfo: {
		host: "localhost",
	    port: "8080",
		dataset: "museum",
		testDataset: "museum-sandbox",
		//uncomment this to prevent inferred statement in the query result
		//infer: "false",
		sesameServerLocation: "openrdf-sesame",
		fusekiServiceQuery: "query" ,
        fusekiServiceUpdate: "update" ,
	},






	/**
	 * RDF DATA SETTINGS
	 *
	 * These represent the main configuration options about the
	 * data and the dataset
	 *
	 * defaultPrefix: default namespace used by the client if no others specified in a policy
	 * defaultBase: default base URL if no others specified in the policy
	 * accessConditionType: specify the way access condition are expressed. Use "sparql" for
	 *						express them as SPARQL ASK query (only value allowed).
     **/
     
	defaultPrefix: "http://museum.example.org/data/",
	defaultBase: "http://museum.example.org/data/",
	accessConditionType: "sparql",
	
	/**
	 * SHI3LD SETTINGS
	 *
	 * They represent the main configuration options about the
	 * shi3ld framework.
	 *
	 * shi3ldURI: URI where is possible to submit requests to the backend
	 * 			via shi3ld.
	 * 
     **/
	
	shi3ldURI: "http://localhost:8080/shi3ld/api/endpoint",
}
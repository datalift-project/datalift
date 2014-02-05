/**
 * policies route
 */
  
 exports.Policies = {};
 var Policies = exports.Policies;
 
 var sparqlClient = require('../lib/sparqlClient');
 var rdf = require('rdf/lib/rdf'); 
 
 /**
  * manage creation (POST) or update (PUT) of a policy
  **/
 Policies.create = function (req, res) {
    
  console.log("policy save request...");
  console.log("req method: " + req.method);
  var uri = req.query.policy;
  console.log("policy uri: " + uri);
  console.log("content-type of the request: " + req.get('Content-Type'));
  console.log(req.is('text/*'));
  var turtleString = req.text;
  
  console.log("body of the client request (txtparser): " + req.text);
  console.log("checking RDFInterface configuration...");
  
  if (settings.RDFInterface == undefined) {
  	console.error("shi3ld policy manager not properly configured: missing RDFInterface");
  }
  console.log("RDFInterface: " + settings.RDFInterface);
  if (settings.RDFInterface == "sparql") {
    console.log("tslength:" + turtleString.length);
    console.log("policy in turtle: " + turtleString);
  	
  	//callback function is the same for both POST and PUT method
  	var callback = function (status) {
		//fuseki answer 204 (no body if ok) or  error code if errors occurs
		if (status == 204) {
			//update acCnt to avoid name clashes
			storage.acCnt = req.cookies.acCnt;
			console.log('acCnt in cookie: ' + storage.acCnt);
			fs.writeFile('resources/storage.js', 'storage = ' + JSON.stringify(storage), function (err) {
			  //TBD what to do if write fails?
			  if (err) throw err;
			  console.log('storage updated, now is: ' + JSON.stringify(storage));
			});
		} 
		res.status(status).send();
	}
	
  	if(req.method == "POST") {
  		//if 1st time that policy is saved => check name uri is not already in use
  		console.log("POST request");
  		console.log("policy is being created...");
	  	exist(uri, function (ask){
	  		if (ask != 200){
		  		//signal error to client
		  		res.status(ask).send();
		  		return ;
		  	}
			query = Policies.generateInsertQuery(turtleString);
			sparqlClient.SparqlClient.update(query, callback);
			
	  	});
	  	return;
  	}
  	
  	console.log("PUT request");
  	console.log("policy is being updated...");
  	//if updating a policy overwrite old policy
  	query = generateDeleteInsertQuery(turtleString, uri);
	sparqlClient.SparqlClient.update(query, callback);
	 
  }else{
	//LDP
  }
 } 
 
 function exist(uri, callback) {
  	var query;
  	
  	query = generateAskQuery(uri);
  	
   	sparqlClient.SparqlClient.ask(query, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result) {
   		if(err) {
   			//if error returned by sparql endpoint send a response with 500 to client 
   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
   			callback(500);
   			return;
   		}
   		if(result == true) {
   			//in case of name conflict 409 send to client
   			console.log("Policy already existing. Sending 409 to client");
   			callback(409);
   			return;
   		}
   		console.log("Policy not yet present. Storing it in the triple store");
   		callback(200);
   	});
 }
 
 
 function generateAskQuery(uri) {
  	var query;
   	
   	console.log("generating ask query...");
   	
   	query = "ASK{\n" +
   			" GRAPH <" + settings.policyNamedGraphURI + "> {\n" +
   				"<" + uri + "> ?p ?o\n" +
   			" }\n}";
   		
   	return query;
  }
  
 Policies.generateInsertQuery = function (turtleString){
  	var query, graph;
  	
  	console.log("generating insert query...");
  	graph = sparqlClient.SparqlClient.turtleToSPARQLPrefixes(turtleString);  	
   	
   	query= graph.prefixes +
			   "\nINSERT DATA\n" +
				"{ GRAPH <" + settings.policyNamedGraphURI + ">{\n" +
  					graph.turtleCore +
  				"}\n" +
  				"}";
  				
	console.log("sparql query: " + query);
	return query;
 }
 
 //Update meand delete all the triples with subjetc equal the policy receivesd as paramenter
 //and with subject the acs of that policy and rewrite only triples in body of request
 //to update the policy is resent entirely an completely rewritten.
 //access condition triples are not deleted for matter of modularity:
 //may be usede in other policies or may be reused in future also if now temporary not
 //included in any policy
 //but if to update than update any triple with the ac to update as subject (the update
 //of the ac affects also other policies that share same ac)
 function generateDeleteInsertQuery (turtleString, policyUri){
  	var query, graph, acs, acTriples, acTriplesWhere, i;
  	
  	console.log("generating delete insert query...");
  	acs = getPolicyAccessConditions(turtleString, policyUri);
  	acTriples = "";
  	acTriplesWhere = "";
  	for (i = 0; i < acs.length; i++) {
  		acTriples += ("  <" + acs[i] + "> ?p" + (i + 2) + " ?o" + (i + 2) + ".\n");
  		acTriplesWhere += ("\n  OPTIONAL { <" + acs[i] + "> ?p" + (i + 2) + " ?o" + (i + 2) + ".}");
  	}
  	  	
  	graph = sparqlClient.SparqlClient.turtleToSPARQLPrefixes(turtleString);  	
   	//keep trace of original text policy
	query = graph.prefixes +
			"DELETE {\n	" +
			" GRAPH <" + settings.policyNamedGraphURI + ">{\n" +
			"	<" + policyUri + "> ?p ?o." +
				" <" + policyUri + "> s4ac:hasAccessConditionSet ?acs." +
				" ?acs ?p1 ?o1." +
				acTriples +
			"\n}\n} INSERT {\n" +
			" GRAPH <" + settings.policyNamedGraphURI + ">{\n" +
  					graph.turtleCore +
  			"}\n}\nWHERE {" +
  			" GRAPH <" + settings.policyNamedGraphURI + ">{\n" +
				" <" + policyUri + "> ?p ?o." +
				//find triples with subect acs
				" OPTIONAL {\n" +
				"  <" + policyUri + "> s4ac:hasAccessConditionSet ?acs." +
				"  ?acs ?p1 ?o1.\n}" +
				acTriplesWhere +
			"\n}\n}";
	console.log("sparql query: " + query);
	
	return query;
 }
 
 //return an array that has as element array of trilpes for a particular
 //access condition in the policy
 function getPolicyAccessConditions(turtleString, policyUri) {
 	var env, triples, acs, i, acTriples;
  	var parsedGraph = new rdf.TripletGraph;
 	var turtleParser = new rdf.TurtleParser();
 	
  	acs = new Array();
 	console.log("parsing to get retrieve access condition...");
  	turtleParser.parse(turtleString, undefined, undefined, undefined, parsedGraph);
  	env =  turtleParser.environment;
  	console.log("parsing complete");
  	console.log("parsed graph:");
  	//parsedGraph.toNT();
  	
  	triples = parsedGraph.match(policyUri, env.prefixes.get("s4ac") + "hasAccessConditionSet", null);
  	if (triples.length == 0) {
  		console.log("No access condition set in the policy found");
  		return acs;
  	}
  	
  	triples = parsedGraph.match(triples[0].object, env.prefixes.get("s4ac") + "hasAccessCondition", null);
  	for (i = 0; i < triples.length; i++) {
  		acs[i] = triples[i].object;
  	}
  	
  	return acs;
 }
 
 /**
  * manage retriaval of the paginated list of policies and of a single policy
  */
 Policies.read = function(req,res){
	  var queryStr;
	  //eval(fs.readFileSync('config.js', encoding="ascii"));
	  console.log("looking for policies...");
	  
	  if (settings.RDFInterface == undefined){
	  	console.error("shi3ld policy manager not properly configured: missing RDFInterface");
	  	return;
	  }
	  console.log("RDFInterface: " + settings.RDFInterface);
	  if (settings.RDFInterface == "sparql"){
	  	if (req.query.policy) {
	  		retrievePolicy(req, res);
	  	} else {
	  		listPolicies(req, res);	  
		}
	  	
	  }else{
		//LDP
	  }
 }
 
 //returns a list of policy to the client (with pagination of resuts from backend)
 function listPolicies(req, res) {

	console.log("Counting policies...");
  	queryStr = generateCountQuery();
    console.log("count query: " + queryStr);
    
    sparqlClient.SparqlClient.select(queryStr, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
   		var count;
   		//keep track of targets in the current page 
		//in order to retrieve the details for them
		//(number of triples and label)
		//Point to targets object by using target uri as key 
		var targetsInPage = {};
		
   		if(err){
   			//if error returned by sparql endpoint send a response with 500 to client 
   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
   			res.status(500).send();
   			return;
   		}
   		if((result)){
   			count = result[0].cnt;
   			
		    queryStr = generateSelectQuery(req.query.limit, req.query.offset);
		    console.log("select query: " + queryStr);
		    
		    sparqlClient.SparqlClient.select(queryStr, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
		   		
			   	var policyHeaders, totalRecord;
			   		
			   	if(err){
			   		//if error returned by sparql endpoint send a response with 500 to client 
			   		console.log("Sparql endpoint return an error code in response. Sending 500 to client");
			   		res.status(500).send();
			   		return;
			   	}
			  	if((result)){
			 		console.log("select result: " + JSON.stringify(result));
			   		policyHeaders = new Array();
			   		//manage disalignment from sparql result from select and policies in client-side
			 		/*lastUri =*/ getHeaders(policyHeaders, targetsInPage, req.query.limit, result);
					console.log("policyHeaders: " + JSON.stringify(policyHeaders));
					if (policyHeaders.length > 0) {
						retrieveTargetDetails(targetsInPage, policyHeaders, count, req, res);
						return;
					}
					//send response in case of no policy
					res.send({
			   			totalPages: Math.ceil(count / req.query.limit),
			   			totalRecords: count,
			   			//lastUri: lastUri,
			   			data: policyHeaders
			   		});
					return;
			   	}
			   	//should be unuseful
			   	//res.status(204).send();
					   	 	
			});
   			
   		}
   		
   	});
}

function generateSelectQuery(limit, offset){
	var query;
	
	query = "PREFIX s4ac: <http://ns.inria.fr/s4ac/v2#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n\n" +
			
			"SELECT ?policyUri ?policyLabel ?target ?privilege\n" +
			"WHERE { GRAPH <" + settings.policyNamedGraphURI + "> {\n" +
				"?policyUri rdf:type s4ac:AccessPolicy .\n" +
				"OPTIONAL{?policyUri rdfs:label ?policyLabel}\n" +
				"OPTIONAL{?policyUri s4ac:appliesTo ?target}\n" +
				"OPTIONAL{?policyUri s4ac:hasAccessPrivilege ?privilege}\n" +
				"{\nSELECT ?policyUri\n" +
				 	"WHERE {\n" + 
				 		"GRAPH <" + settings.policyNamedGraphURI + "> {\n" +
				    		"?policyUri rdf:type s4ac:AccessPolicy .\n" +
				    	"\n}" +
				    "\n}" +
				    "ORDER BY ?policyUri LIMIT " + limit + " OFFSET " + offset +		    
				"}\n" + 
			"}\n}";
	return query;
			
}

function generateCountQuery(){
	var query;
	
	query = "PREFIX s4ac: <http://ns.inria.fr/s4ac/v2#>\n" +
			"SELECT (COUNT (*) as ?cnt)\n" +
			"WHERE {\n" +
			" GRAPH <" + settings.policyNamedGraphURI + "> {?s a s4ac:AccessPolicy }\n}";
	return query;
			
}

function getHeaders (headers, targetsInPage, limit, result) {

	var i, policyHeader, doubled, k, target;
	
	i = 0;	
	//iterates over the resulting records
   	while ( (i < result.length) && (headers.length < limit) ) {
		policyHeader = {
			label: "",
			targets: [],
			privileges: []
		};
		policyHeader.uri = result[i].policyUri;
		//either 1 label or 0 (no more than 1 label)
		if (result[i].policyLabel) {
			policyHeader.label = result[i].policyLabel;
		}
		//consider all pairs (target, privilege) for the last inserted policy uri 
		while ((i < result.length) && (result[i].policyUri == policyHeader.uri)) {
			if (result[i].target) {
				if (!targetsInPage[result[i].target]) {
					target = {uri: result[i].target};
					policyHeader.targets.push(target);
					targetsInPage[result[i].target] = target;
				} else {
					policyHeader.targets.push(targetsInPage[result[i].target]);
				}
				//consider all privileges for the last inserted policy uri
				while (
					(i < result.length) && 
					(result[i].target == policyHeader.targets[policyHeader.targets.length - 1].uri)	
					&& (result[i].policyUri == policyHeader.uri)				
				) {
					if (result[i].privilege) {
						doubled = false;
						for (k = 0; k < policyHeader.privileges.length; k++) {
							if(policyHeader.privileges[k] == result[i].privilege){
								doubled = true;
								break;
							}
						}
						if (!doubled) {
							policyHeader.privileges.push(result[i].privilege);
						}
					}
					i ++;
				}
			} else {
				//if no target check privilege & move to next row
				if (result[i].privilege) {
					doubled = false;
					for (k = 0; k < policyHeader.privileges.length; k++) {
						if(policyHeader.privileges[k] == result[i].privilege){
							doubled = true;
							break;
						}
					}
					if (!doubled) {
						policyHeader.privileges.push(result[i].privilege);
					}
				}
				i ++;
			}				
		}
		console.log("policyHeader: " + JSON.stringify(policyHeader));
		console.log("i: " + i);
		headers.push(policyHeader);	
	}
}

Policies.generateTargetsQueryByUri = function  (targets) {
	var emptyTargetUriList = true;
	
	var query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"SELECT DISTINCT ?g ?label (COUNT (*) AS ?tripleCnt)\n" +
			"WHERE {\n" +
			" GRAPH ?g {?s ?p ?o OPTIONAL { ?g rdfs:label ?label}\n}\n";
			
	for (var prop in targets) {
		if(emptyTargetUriList) {
		 query += " FILTER(\n(?g = <" + prop + ">)";
		 emptyTargetUriList = false;
		 continue;
		}
		query += "\n|| (?g = <" + prop + ">)";
	}
	
	if (emptyTargetUriList == true) {
		throw new Error('empty targets object');
	}
	
	query += ")\n}\n" +
			"GROUP BY ?g ?label\n" +
			"ORDER BY ?label ?g\n";
			
	return query;
}

function retrieveTargetDetails (targets, policyHeaders, count, req, res) {
	var query;
	
	try {
		query = Policies.generateTargetsQueryByUri(targets);
	} catch (err) {
		//targets was empty ie all the policy stored had no target
		console.log("None of the policy has at least one target");
		console.log("sending data to client");
		res.send({
   			totalPages: Math.ceil(count / req.query.limit),
   			totalRecords: count,
   			data: policyHeaders
   		});
		return;
	}
	
	sparqlClient.SparqlClient.select(query, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
   			
	   	if(err){
	   		//if error returned by sparql endpoint send a response with 500 to client 
	   		console.log("Sparql endpoint return an error code in response. Sending 500 to client");
	   		res.status(500).send();
	   		return;
	   	}
	  	if((result)){
	  		//if at this point at in the triple store should be present at least one target
	  		//since at least a policy has a target
	 		console.log("select result: " + JSON.stringify(result));
	   		Policies.getTargetDetails(targets,result);
	   		console.log("policyHeaders: " + JSON.stringify(policyHeaders));
	   		res.send({
	   			totalPages: Math.ceil(count / req.query.limit),
	   			totalRecords: count,
	   			//lastUri: lastUri,
	   			data: policyHeaders
	   		});
			return;
	   	}
	   	//res.status(204).send();
			   	 	
	});
			
	return;
			
}

Policies.getTargetDetails = function (targets, result) {
	//checking result[0].g because if no targets, the response from sparql endpoint 
	//has just a row with only the tripleCnt var (set to 0) 
	if ((!result) || (result.length == 0) || (!result[0].g)) {
		//may be all of the policies had a non existent target (wrong URI)
		console.log("no targets found");
		return;
	}
	for (i = 0; i < result.length; i++ ) {
		targets[result[i].g].label = "";
   		if(result[i].label){
   			targets[result[i].g].label = result[i].label;
   		}
   		targets[result[i].g].tripleCnt = result[i].tripleCnt;
   	}
   	if (targets.length != result.length) {
   		//log situation but not do anything (responsability of data admin)
   		console.log("some of the policies had a non existent target (bad target URI)");
   	}
   	return;
}
 
 //server returns a single policy to the client (policy selected by the uri encoded in the request query string) 
 function retrievePolicy(req, res) {
	console.log("retriving policy with uri " + req.query.policy);
  	queryStr = Policies.generateConstructQuery(req.query.policy);
    console.log("count query: " + queryStr);
    
    sparqlClient.SparqlClient.construct(queryStr, FAVORITE_CONSTRUCT_RESULT_CONTENT, function (err, result){
   		
   		if(err){
   			//if error returned by sparql endpoint send a response with 500 to client 
   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
   			res.status(500).send();
   			return;
   		}
   		if((result)){
   			res.type("text/turtle")
   			res.send(result);
   		}
   		
   	});
 }
 
 /*
  * Return the construct query to retrieve a policy
  * Parameters:
  * uris - may be an array of URIs or a single URI
  **/
 Policies.generateConstructQuery = function (uris) {
 	var query, i, uri;
 	
 	if (uris instanceof Array) {
 		uri = "?policy";
 	} else {
 		uri = "<" + uris  + ">";
 	}
 	
	query = "PREFIX : <" + settings.defaultPrefix + ">\n" +
			"PREFIX s4ac: <http://ns.inria.fr/s4ac/v2#>\n" +
			"PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
			"CONSTRUCT {\n" +
				
				uri + " rdf:type s4ac:AccessPolicy.\n" +
				uri + " rdfs:label ?label.\n" +
				uri + " s4ac:appliesTo ?target.\n" +
				uri + " s4ac:hasAccessPrivilege ?privilege.\n" +
				uri + " s4ac:hasAccessConditionSet ?acs.\n" +
				
				"?acs rdf:type ?acsType.\n" +
				"?acs s4ac:hasAccessCondition ?ac.\n" +
			    
			    "?ac rdfs:label ?acLabel.\n" +
			    "?ac rdf:type ?acType.\n" +
			    "?ac s4ac:hasQueryAsk ?query.\n" +
			"}\n" +
			"WHERE { GRAPH <" + settings.policyNamedGraphURI + "> {\n" +
				uri + " rdf:type s4ac:AccessPolicy.\n" +
				"OPTIONAL {" + uri + " rdfs:label ?label.}\n" +
				"OPTIONAL {" + uri + " s4ac:appliesTo ?target.}\n" +
				"OPTIONAL {" + uri + " s4ac:hasAccessPrivilege ?privilege.}\n" +
				"OPTIONAL {" + uri + " s4ac:hasAccessConditionSet ?acs.}\n" +
				//get the ac set of this policy (only the one of this policy)
				"OPTIONAL {\n" +
				  uri + " s4ac:hasAccessConditionSet ?acs.\n" +
				  "?acs rdf:type ?acsType.\n" +
				"}\n" +
				//get the acs of the ac set of this policy (only the acs of the ac set of this policy)
				"OPTIONAL {\n" +
				  uri + " s4ac:hasAccessConditionSet ?acs.\n" +
				  "?acs rdf:type ?acsType.\n" +
				  "?acs s4ac:hasAccessCondition ?ac.\n" +
				"}\n" +
				//get the query of the acs of that ac set (only the ones of acs of the ac set of this policy)
			    "OPTIONAL {\n" +
			      uri + " s4ac:hasAccessConditionSet ?acs.\n" +
				  "?acs rdf:type ?acsType.\n" +
				  "?acs s4ac:hasAccessCondition ?ac.\n" +
				  //if more types or type is not s4ac:AccessCondition
				  //selected as is - it's responsibility if who edit the rdf
				  //if not GUI used
				  "OPTIONAL {?ac rdf:type ?acType.}\n" +
				  "OPTIONAL {?ac rdfs:label ?acLabel.}\n" +
			      "OPTIONAL{?ac s4ac:hasQueryAsk ?query.}\n" +
			    "}\n";
			    
	if (uris instanceof Array) {
		query += "FILTER( ";
		for (i = 0; i < uris.length; i++ ) {
			query += (i != 0 ? "|| " : "\n");
			query += "(?policy = <" + uris[i] + ">) \n";
			
		}   	
		query += ")\n";
	}		
	query += "}\n}";
			
	return query;
 }

/**
 * manage deletion of a policy
 **/
Policies.del = function (req, res) {
  console.log("policy delete request...");
  console.log("req method: " + req.method);
  var uri = req.query.policy;
  console.log("policy uri: " + uri);
  
  console.log("checking RDFInterface configuration...");
  if (settings.RDFInterface == undefined) {
  	console.error("shi3ld policy manager not properly configured: missing RDFInterface");
  }
  console.log("RDFInterface: " + settings.RDFInterface);
  if (settings.RDFInterface == "sparql") {
    console.log("policy is being deleted...");
  	query = generateDeleteQuery(uri);
	
	sparqlClient.SparqlClient.update(query, function(status) {
		//fuseki answer 204 (no body if ok) or  error code if errors occurs 
		res.status(status).send();
	});
	 
  }else{
	//LDP
  }
  
}

//query to delete all the triples with subjetc equal the policy receivesd as paramenter
//and with subject the acs of that policy
//access condition triples are not deleted for matter of modularity:
//may be usede in other policies or may be reused in future also if now temporary not
//included in any policy
function generateDeleteQuery(policyUri) {
	var query;
	
	query = "PREFIX s4ac: <http://ns.inria.fr/s4ac/v2#>" +
			"DELETE {\n	" +
			" GRAPH <" + settings.policyNamedGraphURI + ">{\n" +
			"	<" + policyUri + "> ?p ?o." +
				" <" + policyUri + "> s4ac:hasAccessConditionSet ?acs." +
				" ?acs ?p1 ?o1." +
  			"}\n}\nWHERE {" +
  			" GRAPH <" + settings.policyNamedGraphURI + ">{\n" +
				" <" + policyUri + "> ?p ?o." +
				//find triples of with acs as subject
				" OPTIONAL {\n" +
				"  <" + policyUri + "> s4ac:hasAccessConditionSet ?acs." +
				//optional not necessary because if acs is subject is no triples
				//no triples about acs to delete
				"  ?acs ?p1 ?o1.\n}" +
			"\n}\n}";
	console.log("sparql query: " + query);
	
	return query;
}
 
  
  
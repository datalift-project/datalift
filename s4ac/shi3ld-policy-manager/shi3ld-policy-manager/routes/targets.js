/**
 * targets route
 */
exports.Targets = {};
var Targets = exports.Targets;
var sparqlClient = require('../lib/sparqlClient'); 

N_TRIPLES_RET = 3;


function generateSelectQuery(offset, limit) {
	var query;
	query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"SELECT ?g ?label (COUNT (*) AS ?tripleCnt)\n" +
			"WHERE {\n" +
			" GRAPH ?g {?s ?p ?o OPTIONAL { ?g rdfs:label ?label}\n}\n" +
			" FILTER(! (?g = <" + settings.policyNamedGraphURI + ">))\n}\n" +
			"GROUP BY ?g ?label\n" +
			"ORDER BY ?label ?g\n" +
			"LIMIT " + limit + " OFFSET " + offset;
	return query;
			
}

//Targets.generateSelectQuery = generateSelectQuery;

function generateCountQuery() {
	var query;
	
	query = "SELECT (COUNT (DISTINCT ?g) as ?cnt)\n" +
			"WHERE {\n" +
			" GRAPH ?g {?s ?p ?o}\n" +
			" FILTER(! (?g = <" + settings.policyNamedGraphURI + ">))\n}\n";
	
	return query;
			
}

function getTargets(result) {
	var i, targets = new Array();
	
	if ((result.length == 1) && (!result[0].g)) {
		//if no targets in the triple store, the query return a row wih the triple count=0
		//and other field null -> send to client a empty vectors
		return targets;
	}
   	for (i = 0; i < result.length; i++ ) {
   		targets[i] = {};
   		targets[i].uri = result[i].g;
   		targets[i].label = "";
   		if(result[i].label){
   			targets[i].label = result[i].label;
   		}
   		targets[i].tripleCnt = result[i].tripleCnt;
   	}
   	return targets;
}

Targets.list = function(req, res){
  
  console.log("RDFInterface: " + settings.RDFInterface);
  if (settings.RDFInterface == "sparql"){
  	
  	if (req.query.target && req.query.triples) {
  		getTarget(req, res);
  	} else {
  		listTargets(req, res);
  	}
  	
  }else{
	//LDP
  }
  
};

/*function generatePreviewQuery(targetUri, turtle, offset, limit){
	var query;
	query = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
			"SELECT (COUNT (*) AS ?tripleCnt)\n" +
			"WHERE {\n" +
			  " GRAPH <" + targetUri + "> {?s ?p ?o OPTIONAL { ?g rdfs:label ?label}\n}\n" +
			  "   " + turtle +
			  "\n}\n" +
			"GROUP BY ?g ?label\n" +
			"ORDER BY ?label ?g\n" +
			"LIMIT " + limit + " OFFSET" + offset;
	return query;
			
}*/

function generateNestedQuery(query, cntVarName) {
	var lastBracket= query.lastIndexOf("}");
	var wherePos = query.search(/WHERE\s*{/);
	var coreStart = wherePos + query.substr(wherePos).indexOf("{");
	
	if(query.search(/SELECT\s*\*/) == -1){
		query= query.substring(0, wherePos)+ "?" + cntVarName + " " + query.substr(wherePos);
		coreStart += cntVarName.length + 2;
		lastBracket += cntVarName.length + 2;
	}
	
	//generate a random number so that var used to hold the tot number of triples is unique
	//(low probability to have a var with same name)
	
	var nested = query.substring(0, coreStart + 1) +
				"{\nSELECT (COUNT(*) AS ?"+ cntVarName +") WHERE {\n" + 
					query.substring(coreStart + 1, lastBracket)+
				"}\n}" +
				query.substring(coreStart + 1, query.length);
	
	return nested;
}

function getTarget (req, res) {
	var cntVarName = "triplesNum" + Math.floor((Math.random()*100000)+1); 
		
	console.log("querying triple store...");
	console.log("generating select query to count triples...");
  	nestedQuery = generateNestedQuery(req.query.triples, cntVarName);
  	console.log("nested query: " + nestedQuery);
  	
    sparqlClient.SparqlClient.select(nestedQuery, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result) {
   		if(err){
   			//if error returned by sparql endpoint send a response with 500 to client 
   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
   			res.status(500).send();
   			return 500;
   		}
   		count = 0;
   		rows = new Array();
   		if((result)){
   			if (result.length > 0) {
   				count = result[0][cntVarName];
   			}
   			for(i =  0; (i < N_TRIPLES_RET) && (i < result.length); i++){
   				delete result[i][cntVarName];
   				rows.push(result[i]);
   			}   			
		}
		res.send({
   			triplesNum: count, 
   			data: rows
   		});
		return;
   	});
}

function listTargets (req, res) {
	var queryStr, count;
	
	console.log("looking for targets...");
	console.log("generating select query to count named graphs...");
  	queryStr = generateCountQuery();
  	console.log("count query: " + queryStr);
    
    sparqlClient.SparqlClient.select(queryStr, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
   		if(err){
   			//if error returned by sparql endpoint send a response with 500 to client 
   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
   			res.status(500).send();
   			return 500;
   		}
   		if((result)){
   			count = result[0].cnt;
   			console.log("generating select query...");
		    console.log("limit param: " + req.query.limit);
		    console.log("offset param: " + req.query.offset);
		    queryStr = generateSelectQuery(req.query.offset, req.query.limit);
		    console.log("select query: " + queryStr);
		    
		    sparqlClient.SparqlClient.select(queryStr, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
		   		var i, targets;
		   		if(err){
		   			//if error returned by sparql endpoint send a response with 500 to client 
		   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
		   			res.status(500).send();
		   			return;
		   		}
		   		if((result)){
		   			var targets = getTargets(result);
		   			res.send({
		   				totalPages: Math.ceil(count / req.query.limit),
		   				totalRecords: count,
		   				data: targets
		   			});
		   			return;
		   		}
		   		res.status(204).send();
		   	});
   		}
   	});
}

Targets.add = function (req, res) {
	var queryStr;
	
	console.log("adding a new target...");
	console.log("targetUri: " + req.query.target);
	console.log("body: " + req.body);
	console.log("body: " + JSON.stringify(req.body));
  	
  	exist(req.body.uri, function (ask){
	  		if (ask != 200){
		  		//signal error to client
		  		res.status(ask).send();
		  		return ;
		  	}
		  	
			console.log("generating insert query...");
			queryStr = generateInsertQuery(req.body.uri, req.body.label, req.body.prefixes, req.body.triples);
		  	console.log("insert query: " + queryStr);
		    
		    sparqlClient.SparqlClient.update(queryStr, function(status) {
		    	//fuseki answer 204 (no body if ok) or  error code if errors occurs 
		    	if (status == 204) {	
		    		queryStr = generateNewGraphTriplesCntQuery (req.body.uri);
		    		
					sparqlClient.SparqlClient.select(queryStr, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
				   		var targets;
				   		if(err){
				   			//if error returned by sparql endpoint send a response with 500 to client 
				   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
				   			res.status(500).send();
				   			return;
				   		}
				   		if((result) && (result.length > 0)){
				   			res.status(200);
				   			res.type("application/json");
				   			res.send({
				   				uri: req.body.uri,
				   				label: req.body.label,
				   				tripleCnt: result[0].cnt
				   			});
				   			return;
				   		}
		
				   	});
		    	} else{
		    	//forward the error if any occurred
		    		res.status(status).send();
		    	}
			});
			
	  	});
}

function generateAskQuery(uri) {
  	var query;
   	
   	console.log("generating ask query...");
   	
   	query = "ASK{\n" +
   			" GRAPH <" + uri + "> {\n" +
   				"?s ?p ?o\n" +
   			" }\n}";
   		
   	return query;
}

//used to create a new graph and also to overwrite an existing one
//(by dropping the old and recreating a new one with same name)
//if you want overwrite an optional parameter set to "overwrite" is used
function generateInsertQuery(targetUri, targetLabel, prefixes, graphPattern) {
	var query = prefixes +
				"INSERT {\n" +
				"	GRAPH<" + targetUri + "> {\n" +
				"		<" + targetUri + "> rdfs:label " + "\"" +targetLabel + "\"." +
				
				"	" +	graphPattern +
				"\n}\n} WHERE {\n" + 
				"	" + graphPattern + 
				"\n}";
	//check optional argument
	if ((arguments[4]) && (arguments[4] == "overwrite")) {
		query = "DROP GRAPH <" + targetUri + ">;\n" + query;
	}
	return query;
}

function generateNewGraphTriplesCntQuery (targetUri) {
	var query = "SELECT (COUNT (*) as ?cnt)\n" +
				"WHERE {\n" +
				" GRAPH <" + targetUri + ">{?s ?p ?o}\n" +
				"}\n";
	return query;
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
   			console.log("Target already existing. Sending 409 to client");
   			callback(409);
   			return;
   		}
   		console.log("Target not yet present. Storing it in the triple store");
   		callback(200);
   	});
 }
 
 
 Targets.overwrite = function (req, res) {
	var queryStr;
	
	console.log("adding a new target...");
	console.log("targetUri: " + req.query.target);
	console.log("body: " + req.body);
	console.log("body: " + JSON.stringify(req.body));
  	
  	console.log("generating insert query...");
	queryStr = generateInsertQuery(req.body.uri, req.body.label, req.body.prefixes, req.body.triples, "overwrite");
  	console.log("insert query: " + queryStr);
    
    sparqlClient.SparqlClient.update(queryStr, function(status) {
    	//fuseki answer 204 (no body if ok) or  error code if errors occurs 
    	if (status == 204) {	
    		queryStr = generateNewGraphTriplesCntQuery(req.body.uri);
    		
			sparqlClient.SparqlClient.select(queryStr, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
		   		var i, targets;
		   		if(err){
		   			//if error returned by sparql endpoint send a response with 500 to client 
		   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
		   			res.status(500).send();
		   			return;
		   		}
		   		if((result) && (result.length > 0)){
		   			res.status(200);
		   			res.type("application/json");
		   			res.send({
		   				uri: req.body.uri,
		   				label: req.body.label,
		   				tripleCnt: result[0].cnt
		   			});
		   			return;
		   		}

		   	});
    	} else {
    		//forward the error if any occurred
    		res.status(status).send();
    	}
	});
}
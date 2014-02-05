/**
 * Module to manage API to test policies.
 * Two call allowed:
 * 
 * POST to create the dataset to execute query to test policies
 * GET to execute the query (by means of Shi3ld) and return results to client
 */
exports.TestPolicies = {};
var TestPolicies = exports.TestPolicies;
var sparqlClient = require('../lib/sparqlClient');
var request = require('request');
var events = require('events');
var policies = require('./policies');

//max post body is a server parameter (dependent on machine memory)
//default is 2mb both in tomcat than php
//with 60 chars per uri, 17476 targets in a single response
TARGET_PAGE_SIZE = 50;

TestPolicies.initTestDataset = function (req, res) {
	//TBD manage pagination -> iterate incr offset by page size up to first empty
	//resp (offset X skips previous X results). so avoid count of tot result
	var policiesToTest = req.body.policiesToTest;
	
	if (!policiesToTest) {
		console.log("Request with no policies to test. Sending 400 to client");
		res.status(400).send();
		return;
	}
	
	var queryStrTargets = generateSelectQuery(0, TARGET_PAGE_SIZE);
	var queryStrPolicies = policies.Policies.generateConstructQuery(policiesToTest);
	var queryDrop = "DROP ALL";
	
	var Wait = function() { this.cnt = 0; };
	Wait.prototype = new events.EventEmitter;
	
	var done = new Wait();
	done.on('returned', function (status) {
		this.cnt++;
		if (String(status).charAt(0) != "2") {
			if (status != 404) {
				res.status(500).send();				
			} else {
				res.status(status).send();
			}
			this.removeAllListeners();
			return;
		}
		if (this.cnt == 2) {
			res.status(204).send();
			this.removeAllListeners();
		}
	});	
	
	sparqlClient.SparqlClient.update(queryDrop, function(status) {
		
		sparqlClient.SparqlClient.select(queryStrTargets, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
	   		if (err) {
	   			//if error returned by sparql endpoint send a response with 500 to client 
	   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
	   			res.status(500).send();
	   			return;
	   		}
	   		if (result) {
	   			queryStrTargets = generateInitDatasetQuery(result, {drop: true});
	   			
	   			sparqlClient.SparqlClient.update(queryStrTargets, function(status) {
	   				done.emit('returned', status);
	   			}, {test: true});
	   			
	   		} else {
	   			done.emit('returned', 204);
	   		}
	   	});
	   	
	   	sparqlClient.SparqlClient.construct(queryStrPolicies, FAVORITE_CONSTRUCT_RESULT_CONTENT, function (err, result){
	   		if (err) {
	   			//if error returned by sparql endpoint send a response with 500 to client 
	   			console.log("Sparql endpoint return an error code in response. Sending 500 to client");
	   			res.status(500).send();
	   			return;
	   		}
	   		if (result) {
	   			queryStrPolicies = policies.Policies.generateInsertQuery(result);//generateLoadPoliciesQuery(result);
	   			
	   			sparqlClient.SparqlClient.update(queryStrPolicies, function(status) {
	   				done.emit('returned', status);
	   			}, {test: true});
	   			
	   		} else {
	   			//if no policies to test send not found to client
	   			done.emit('returned', 404);
	   		}
	   	});
	   	
	}, {test: true});
}

function generateSelectQuery(offset, limit) {
	var query;
	query = "SELECT DISTINCT ?g\n" +
			"WHERE {\n" +
			" GRAPH ?g {?s ?p ?o}\n" +
			" FILTER(?g != <" + settings.policyNamedGraphURI + ">)\n" +
			"}LIMIT " + limit + " OFFSET " + offset;
			
	console.log("Query to select all the named graphs but the policy named graph: " + query);		
	return query;			
}

function generateInitDatasetQuery(result) {
	var i, query = "INSERT DATA {\n" +
					"<http://example.org/default-graph/s> <http://example.org/default-graph.org/p> <http://example.org/default-graph.org/o>.";
	
	for (i = 0; i < result.length; i++) {
		query += "	GRAPH <" + result[i].g + ">{\n" +
				"		<" + result[i].g + "/s> <" + result[i].g + "/p> <" + result[i].g + "/o>.\n" +
				"	}\n";
	}
	query += "}";
	
	console.log("Query to initialize the dataset: " + query);
	return query;
}

TestPolicies.queryViaShi3ld = function (req, res) {
	var context, query, contextQuery;
	
	context = req.body.context;
	query = req.body.query;
	console.log("TestPolicies.queryViaShi3ld");
	console.log("data query: " + query);
	
	if ((!context) || (!query)) {
		console.log("Request with query or context missing. Sending 400 to client");
		res.status(400).send();
		return;
	}
	console.log("Generating INSERT DATA query from client context...");
	contextQuery = generateContextQuery(context);
	if (contextQuery == -1) {
		console.log("Missing triple <contextIRI> rdf:type prissmaContext in the context. Sending 400 to client");
		res.status(400).send();
		return;
	}
	console.log("Sending request to Shi3ld...");
	request({
	  uri: settings.shi3ldURI,
	  method: "POST",
	  form: {
		ctx: contextQuery,
		query: query
	  }
	}, function(error, response, body) {
		var testResult;
		
		console.log("Shi3ld response received");
		if ((error) || String(response.statusCode).charAt(0) != "2") {
			//forward shield err or mask it?
			res.status(500).send();
		}
		if (!response) {
			console.log("No answer from Shi3ld");
			//set code to Gateway Timeout
			res.status(504).send();
			return;
		}
		console.log("status code: " + response.statusCode);
		console.log("body: ");
	    console.log(body);
	    
	    if((!body) || (body.length == 0)) {
	    	console.log("test failed. Sending 401 to the client");
	    	res.status(401).send();
	    	return;
	    }
	    
	    processSelectResult(body, res);
	    
	});
	console.log("waiting response from Shi3ld...");
}

function generateContextQuery(turtleStr) {
	var query, str, start, end;
	var contextGraph = sparqlClient.SparqlClient.turtleToSPARQLPrefixes(turtleStr);
	
	//Find context IRI (subject of 'rdf:type prissma:Context' triple) to be substituted with <UNIQUE_ID#ctx> as shi3ld expect
	//(shi3ld expect in insert query a context IRI indicated by <UNIQUE_ID#ctx> and substitute UNIQUE_ID with context named graph IRI)
	str = contextGraph.turtleCore.replace(/\s+/g, " ");
	str = str.replace(/ a\sprissma:Context/g, "rdf:type prissma:Context");
	str = str.replace(/<http:\/\/www.w3.org\/1999\/02\/22-rdf-syntax-ns#type>\sprissma:Context/g, "rdf:type prissma:Context");
	
	end = str.search("rdf:type prissma:Context");
	if (end == -1) return -1;
	str = str.substring(0, end);
	str = str.substr(str.lastIndexOf(".") + 1).replace(" ", "").split(" ");
	
	contextGraph.turtleCore = contextGraph.turtleCore.replace(new RegExp(str[0], "g"), "<UNIQUE_ID#ctx>");
	   	
   	query= contextGraph.prefixes +
			"\nINSERT DATA {\n" +
			"GRAPH <UNIQUE_ID> {\n" +
				contextGraph.turtleCore +
  			"\n}\n}";
  			
	console.log("context query: " + query);
	return query;
}

function processSelectResult(shi3ldSelectResp, res) {
	
	var subjects, i, affectedNamedGraphsCnt;
	var affectedNamedGraphs, query, testResult, affectedNamedGraphMap;
	
	subjects = shi3ldSelectResp.substr(0, shi3ldSelectResp.length - 1).split("\n");
	affectedNamedGraphsCnt = subjects.length;
	affectedNamedGraphs = new Array();
	affectedNamedGraphMap =  {};
	
	for (i = 0; i < affectedNamedGraphsCnt; i++) {
		subjects[i] = subjects[i].substr(0, subjects[i].length - 2);
		affectedNamedGraphs.push({uri: subjects[i]});
		affectedNamedGraphMap[subjects[i]] = affectedNamedGraphs[affectedNamedGraphs.length - 1];
	}
	
	try {
		query = policies.Policies.generateTargetsQueryByUri(affectedNamedGraphMap);
	} catch (err) {
		console.log("affectedNamedGraphs was empty. Sending 500 to client");
		res.status(500).send();
		return;
	}
	
	sparqlClient.SparqlClient.select(query, FAVORITE_SPARQL_RESULT_CONTENT, function (err, result){
   			
	   	if(err){
	   		//if error returned by sparql endpoint send a response with 500 to client 
	   		console.log("Sparql endpoint return an error code in response. Sending 500 to client");
	   		res.status(500).send();
	   		return;
	   	}
	   	
	  	if((result) && (result.length > 0)){
	  		console.log("select result: " + JSON.stringify(result));
	   		policies.Policies.getTargetDetails(affectedNamedGraphMap, result);
	   		
	   		res.status(200)
	   		.set({'Content-Type': 'application/json'})
	   		.json({
				affectedNamedGraphsCnt: affectedNamedGraphsCnt,
				affectedNamedGraphs: affectedNamedGraphs,
			})
			.send();
	    	
	    	return;
	   }
	   
	   console.log("No targets details found. Sending 500 to client");	
	   res.status(500).send();	   	 	
	});
	
}


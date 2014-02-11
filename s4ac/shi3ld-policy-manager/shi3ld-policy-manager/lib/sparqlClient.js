/*
 * sparql client module
 * 
 * TBD now info stored 
 */
exports.SparqlClient = {};

//The SparqlClient module defines the public interface to the sparql client component.
var SparqlClient = exports.SparqlClient;



var request = require("request");

// function to send an update query to a sparql endpoint
// queryStr: query to execute
// options(optional parameter):{
//	postType: "url-encoded" or "directly"
//	using-graph-uri: [] array of uris 
//	using-named-graph-uri:[] array of uris
// }
// if not provided default post-type is directly and no using-named-graph-uri neither using-graph-uri
// ONLY DEFAULT CASE MANAGED
// test: if true execute query on test dataset 	 
SparqlClient.update = function(queryStr, callback, options){
	var updateUri;
//	set default options
	var contentType = "application/x-www-form-urlencoded";
	var test = false;
	
	if ((queryStr == undefined) || (queryStr == null)){
		throw ("SparqlClient.update: queryStr parameter missing");
	}
	
	if (arguments.length == 3){
		options = arguments[2];
		if (options.postType) {
			contentType = options.postType;
		}
		if (options.test) {
			test = true;
		}
		//TO DO manage other options
	}
	
	console.log("SparqlClient.update");
	console.log("queryStr: " + queryStr);
	updateUri = getRDFInterfaceUri("update", null, test);
	
	if (contentType == "application/x-www-form-urlencoded") {
		queryStr = getRDFInterfaceEncodedForm(queryStr);
	}
	console.log("generating HTTP request...");
	
	request({
	  uri: updateUri,
	  method: "POST",
	  headers: {
          'Content-Type': contentType,
          'Content-Length': queryStr.length
      },
      body: queryStr //only string literal or buffer as parameter
      //console.log("body: "+body);
	}, function(error, response, body) {
		console.log("sparql response received");
		if (!response) {
			console.log("No answer from SPARQL endpoint");
			//set code to Gateway Timeout
			return callback(504);
		}
		console.log("status code: " + response.statusCode);
		console.log("body: ");
	    console.log(body);
	    //at now if syntax error in query (due to error in policy from client)
	    //just send to the client a response with same error code received from sparql endpoint and no body
	    callback(response.statusCode);
	});
	console.log("waiting sparql response...");
}

//Generate the URL percent encoded parameters string
function getRDFInterfaceEncodedForm(query) {
	switch (settings.SPARQLEndPoint) {
		//update param defined by w3c sparql 1.1 protocol
		//better keep it separated? eg in sesame queryLn (sparql is the default)
		case "fuseki":
		case "sesame":
			return "update=" + encodeURIComponent(query);
	}
	
}

SparqlClient.ask = function (queryStr, resultContentType, callback, options){
	var askUri;
	var test = false;
	
	if (options) {
		if (options.test) {
			test = options.test;
		}
	}
	
	if ((queryStr == undefined) || (queryStr == null)){
		throw ("SparqlClient.ask: queryStr parameter missing");
	}
	if ((arguments.length == 2)){
		if ((typeof arguments[1] != 'function')){
			throw ("SparqlClient.ask: callback function parameter missing");
		}
		callback = arguments[1];
		resultContentType = "unspec";
	}
	console.log("SparqlClient.ask");
	console.log("queryStr: " + queryStr);
	console.log("resultContentType: " + resultContentType);
	askUri = getRDFInterfaceUri("query", resultContentType);
	console.log("generating HTTP request...");
	
	console.log("ask uri:" + askUri);
	askUri += ("query=" + encodeURIComponent(queryStr));
	console.log("ask request uri:" + askUri);
	
	//request.get('http://google.com/img.png')
	request.get(askUri, function(error, response, body) {
		console.log("sparql response received");
		//console.log(response);
		if (!response){
			console.log("No answer from SPARQL endpoint");
			return callback(504, null);
		}
		console.log("response: " + JSON.stringify(response));
		console.log("status code: " + response.statusCode);
		console.log("body: ");
		console.log(body);
		//if sparql endpoint response has status code = 4xx or 5xx signal error to client as internal error
		//in this case some problem in sparql endpoint or in connection with it since ask query contain just the uri
		//to be assigned to the new policy (no sparql syntax error possible)
		if ((String(response.statusCode).charAt(0) == "4") || (String(response.statusCode).charAt(0) == "5")){
			return callback(504, null);
		}
		parseQueryResult(resultContentType, "ask", body, callback);
	});
	console.log("waiting sparql response...");
} 

SparqlClient.select = function (queryStr, resultContentType, callback, options){
	var selectUri;
	var test = false;
	
	if (options) {
		if (options.test) {
			test = options.test;
		}
	}
	
	if ((queryStr == undefined)||(queryStr == null)){
		throw ("SparqlClient.select: queryStr parameter missing");
	}
	if ((arguments.length == 2)){
		if ((typeof arguments[1] != 'function')){
			throw ("SparqlClient.select: callback function parameter missing");
		}
		callback = arguments[1];
		resultContentType = "unspec";
	}
	console.log("SparqlClient.select");
	console.log("queryStr: " + queryStr);
	console.log("resultContentType: " + resultContentType);
	selectUri = getRDFInterfaceUri("query", resultContentType);
	console.log("generating HTTP request...");
	
	console.log("fuseki uri:" + selectUri);
	selectUri += ("query=" + encodeURIComponent(queryStr));
	console.log("select request uri:" + selectUri);
	
	//request.get('http://google.com/img.png')
	request.get(selectUri, function(error, response, body) {
		console.log("sparql response received");
		//console.log(response);
		if (!response){
			console.log("No answer from SPARQL endpoint");
			return callback(504, null);
		}
		//console.log("response: " + JSON.stringify(response));
		console.log("status code: " + response.statusCode);
		console.log("body: ");
		console.log(body);
		//if sparql endpoint response has status code = 4xx or 5xx signal error to client as internal error
		//in this case some problem in sparql endpoint or in connection with it since select query contain just the uri
		//to be assigned to the new policy (no sparql syntax error possible)
		if ((String(response.statusCode).charAt(0) == "4") || (String(response.statusCode).charAt(0) == "5")){
			return callback(504, null);
		}
		parseQueryResult(resultContentType, "select", body, callback);
	});
	console.log("waiting sparql response...");
}

SparqlClient.construct = function (queryStr, resultContentType, callback, options){
	var constructUri;
	var test = false;
	
	if (options) {
		if (options.test) {
			test = options.test;
		}
	}
	
	if ((queryStr == undefined)||(queryStr == null)){
		throw ("SparqlClient.construct: queryStr parameter missing");
	}
	if ((arguments.length == 2)){
		if ((typeof arguments[1] != 'function')){
			throw ("SparqlClient.construct: callback function parameter missing");
		}
		callback = arguments[1];
		resultContentType = "unspec";
	}
	console.log("SparqlClient.construct");
	console.log("queryStr: " + queryStr);
	console.log("resultContentType: " + resultContentType);
	constructUri = getRDFInterfaceUri("query", resultContentType, test);
	console.log("generating HTTP request...");
	
	console.log("fuseki uri:" + constructUri);
	constructUri += ("query=" + encodeURIComponent(queryStr));
	console.log("select request uri:" + constructUri);
	
	request.get(constructUri, function(error, response, body) {
		console.log("sparql response received");
		//console.log(response);
		if (!response){
			console.log("No answer from SPARQL endpoint");
			return callback(504, null);
		}
		//console.log("response: " + JSON.stringify(response));
		console.log("status code: " + response.statusCode);
		console.log("body: ");
		console.log(body);
		//if sparql endpoint response has status code = 4xx or 5xx signal error to client as internal error
		//in this case some problem in sparql endpoint or in connection with it
		if ((String(response.statusCode).charAt(0) == "4") || (String(response.statusCode).charAt(0) == "5")){
			return callback(504, null);
		}
		callback(null, body);
	});
	console.log("waiting sparql response...");
} 

//allow to contact properly the actual sparql endpoint
//by basing on settings given in config file 
//sparqlAPI: used to distinguish SPARQL read query from update (and construct proper URI in both cases) 
//	read-only queries (SPARQL query language) and update ones (SPARQL update)
//resultContentType: specify the body content. XML vs JSON for ask and select,
//	RDF serialization (Turtle, RDF/XML ecc) in construct. Currently only supported
//	JSON in ask and select and Turtle in construct
function getRDFInterfaceUri(sparqlAPI, resultContentType, test) {
	
	switch (settings.SPARQLEndPoint) {
		case "fuseki":			
			return getFusekiUri(sparqlAPI, resultContentType, test);
		case "sesame":
			return getSesameUri(sparqlAPI, resultContentType, test);
	}
	
}

function getFusekiUri(sparqlAPI, resultContentType, test){
	var service, serviceName;
	
	if (test) {
		serviceName = settings.SPARQLEndPointInfo.testDataset;
	} else {
		serviceName = settings.SPARQLEndPointInfo.dataset;
	}
	if (sparqlAPI == "update"){
		service=settings.SPARQLEndPointInfo.fusekiServiceUpdate;
	}else{
		if(sparqlAPI == "query"){
			service=settings.SPARQLEndPointInfo.fusekiServiceQuery + "?"
			//if (!resultContentType){
			switch (resultContentType) {
				case "json":
					service += (resultContentTypeParameters.JSON + "&");
					break;
				case "turtle":
					service += (resultContentTypeParameters.CONSTRUCT_TTL + "&");
			}
			//}
		} else {
			if(sparqlAPI != "GSP"){
				throw ("SparqlClient.getRDFInterfaceUri: bad sparqlAPI parameter (must be one of 'update','query', 'GSP')");
			}
			service=settings.SPARQLEndPointInfo.serviceReadGraphStore;
		}
	}
	return "http://" + settings.SPARQLEndPointInfo.host + ":" +
			settings.SPARQLEndPointInfo.port + "/" + 
			serviceName + "/" + 
			service;
}

function getSesameUri(sparqlAPI, resultContentType, test){
	var parameters = "/statements";
	var repository;
	if (test) {
		repository = settings.SPARQLEndPointInfo.testDataset;
	} else {
		repository = settings.SPARQLEndPointInfo.dataset;
	}
	if(sparqlAPI == "query"){
			parameters = "?"
			switch (resultContentType){
				case "json":
					parameters += (resultContentTypeParameters.JSON + "&");
					break;
				case "turtle":
					parameters += (resultContentTypeParameters.CONSTRUCT_TTL + "&");
			}
			if (settings.SPARQLEndPointInfo.sesameInfer) {
				parameters += ("infer=" + settings.SPARQLEndPointInfo.sesameInfer + "&");
			}
	}
	//management of GSP case URI (if needed) to be added
	return "http://" + settings.SPARQLEndPointInfo.host + ":" +
			settings.SPARQLEndPointInfo.port + "/" + 
			settings.SPARQLEndPointInfo.sesameServerLocation + 
			"/repositories/" + repository +
			parameters;
			
}

function parseQueryResult(resultContentType, sparqlOperation, body, callback){
	
	var result, item, i;
	
	console.log("parsing query result...");
	switch (resultContentType){
		//TBD if resultContentType unspec base on contet type in the resp
		//TBD manage xml result 
		case "json":
			if(sparqlOperation == "ask") {
				console.log("query result in json");
				console.log("query result: " + JSON.stringify(body.boolean));
				body=JSON.parse(body);
				//console.log("query result: " + JSON.stringify(body.boolean));
				callback(null, body.boolean);
			} else if (sparqlOperation == "select") {
				//return a vector of obj with a property for each var in select, indexed by the var name
				var result = new Array();
				var bindings;
				console.log("query result in json");
				//console.log("query result: " + JSON.stringify(body));
				body=JSON.parse(body);
				//console.log("query result: " + JSON.stringify(body));
				//TBD if empty response bindings is present?
				bindings = body.results.bindings;
				for (i = 0; i < bindings.length; i++) {
					item = {};
					for(prop in bindings[i]) {
						item[ prop ] = bindings[i][prop].value;
					}
					result[i] = item;
				}
				console.log("query result: " + JSON.stringify(result));
				callback(null, result);
			}
			break;
	}
	
}

//resceive a turtle graph and return a obj with prefixes in sparql syntax
 //and the rest of the graph
 SparqlClient.turtleToSPARQLPrefixes = function (turtleString){
 	var st, end, turtleCore, uriEnd, str;
    var endDirective, prefixes = "";
 	
 	while((st = turtleString.search("@prefix"))!= -1){
  	    turtleString = turtleString.replace("@prefix","PREFIX");
  		end=turtleString.search("@prefix");
  		if(end == -1){
	  		//search end of triple
	  		str = turtleString.substring(st,turtleString.length-1);
  			console.log(str);
  			//find end of last prefix declared
  			//find dot not in uri (ie after >)
  			uriEnd = str.indexOf(">");
  			end = uriEnd + st + 1;
  			str = turtleString.substring(end);
  			console.log(str);
  			endDirective = str.indexOf(".");
  			end = end + endDirective + 1;
  			str = turtleString.substring(end);
  			console.log(str);
  		}
  		prefixes += turtleString.substring(st, end);
  	}
  	
  	turtleCore = turtleString.substring(end);
  	console.log("turtle core: " + turtleCore);
  	console.log("prefixes: " + prefixes);
  	prefixes = prefixes.replace(/>\s*\./g,">");
  	
  	return {
  		prefixes: prefixes,
  		turtleCore: turtleCore,
  	};
 }
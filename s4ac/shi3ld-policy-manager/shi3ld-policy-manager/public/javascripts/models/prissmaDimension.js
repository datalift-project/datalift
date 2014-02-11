/**
 * Model that contains function to manage the addition of dimension according to prissma model
 * It allow to generate both ask query statements and turtle triples.
 */

define([
 'underscore',
 'backbone',
 '/javascripts/lib/node-rdf-master/libBrowser/rdf.js',
 '/javascripts/lib/node-rdf-master/libBrowser/TurtleParser.js'], 
 function( _, Backbone, rdf, parser) {
 
   var PrissmaDimModel = Backbone.Model.extend({
	 addTimeAc: function(ac, queryStr, format) {
			
			if ((this.attributes.sparqlAC) && (queryStr)) {
				return this.addTimeInAskQuery(ac, queryStr, format);
			} else {
				this.addTimeInContext(ac, format);
			}
			
		 },
		 
		 addTimeInAskQuery: function (ac, queryStr, format) {
		 	var timeInt;
		  	
		  	if(queryStr.search("prissma:environment") == -1){
				queryStr += "?context prissma:environment ?env.\n";
				queryStr += "?env rdf:type prissma:Environment.\n";
			}
			
			queryStr += "?env ao:time ?time.\n";
			queryStr += "?time rdf:type time:Interval.\n"
			
			timeInt = this.getTimeInterval(ac, format);
						
			queryStr += "?time tl:start \"" + timeInt.date.toISOString() + "\"^^xsd:dateTime.\n" +
						"?time tl:duration \"" + timeInt.duration + "S\"^^xsd:duration.\n";
						
			console.log("ask query: " + queryStr);
			
			return queryStr;	
		  },
		  
		  addTimeInContext: function (ac, format) {
		  	var timeInt;
		  	
		  	if(!_.findWhere(this.indexedGraph.toArray(), {predicate: "prissma:environment"})) {
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:context",
					"prissma:environment",
					"_:env ."
				));
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:env",
					"rdf:type",
					"prissma:Environment ."
				));
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:env",
					"ao:time",
					"_:time ."
				));
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:time",
					"rdf:type",
					"time:Interval ."
				));
			}
		  	
		  	timeInt = this.getTimeInterval(ac, format);
		  	
		  	this.indexedGraph.add(rdf.environment.createTriple(
				"_:time",
				"tl:start",
				"\""  + timeInt.date.toISOString() + "\"^^xsd:dateTime ."
			));
			this.indexedGraph.add(rdf.environment.createTriple(
				"_:time",
				"tl:duration",
				"\""  + timeInt.duration + "S\"^^xsd:duration ."
			));
		  },
		  
		  getTimeInterval: function (ac, format) {
		  	var y, m, d, timeInt = {};
		  	
		  	y = ac.dateFrom.substring(6, ac.dateFrom.length);
			
			if(format == "dd/mm/yyyy"){
				d = ac.dateFrom.substring(0, 2);
		    	m = ac.dateFrom.substring(3, 5);
			}else{
				m = ac.dateFrom.substring(0, 2);
		    	d = ac.dateFrom.substring(3, 5);
			}	
			
			//NB month expressed as 0 - 11 instead 1-12 in js Date constructor
			timeInt.date = new Date(y, m - 1, d, ac.timeFrom.substring(0, 2), ac.timeFrom.substring(3,5), 0);
			y = ac.dateTo.substring(6, ac.dateFrom.length);
			if(format == "dd/mm/yyyy"){
				d = ac.dateTo.substring(0, 2);
		    	m = ac.dateTo.substring(3, 5);
			}else{
				m = ac.dateTo.substring(0, 2);
		    	d = ac.dateTo.substring(3, 5);
			}
			
			timeInt.duration = Date.UTC(y, m - 1, d, ac.timeTo.substring(0, 2), ac.timeTo.substring(3, 5), 0) - 
					 Date.UTC(timeInt.date.getFullYear(), timeInt.date.getMonth(), timeInt.date.getDate(), timeInt.date.getHours(), timeInt.date.getMinutes() );
			//convert in sec (precision issue supposed to not be crytical)
			timeInt.duration /= 1000;
						
			return timeInt;
		  },
		  
		  addOutdoorAc: function(ac, queryStr) {
		  	
		  	console.log("policy.addOutdoorAc");
		  	if ((this.attributes.sparqlAC) && (queryStr)) {
				return this.addOutdoorInAskQuery(ac, queryStr);
			} else {
				this.addOutdoorInContext(ac);
			}
		 },
		  
		 addOutdoorInAskQuery: function (ac, queryStr) {
		 	if (queryStr.search("prissma:environment") == -1) {
				queryStr += "?context prissma:environment ?env.\n?env rdf:type prissma:Environment.\n";
			}
			
			if(queryStr.search("prissma:currentPOI") == -1){
				queryStr += "?env prissma:currentPOI ?poi.\n?poi rdf:type prissma:POI.\n";	
			}
			
			queryStr += "?poi geo:lat \"" + ac.lat + "\".\n" +
						"?poi geo:lon \"" + ac.lon + "\".\n" +
						"?poi prissma:radius \"" + ac.radius + "\".\n";
			
			console.log("ask query: " + queryStr);
			return queryStr;
		 },
		 
		 addOutdoorInContext: function (ac) {
		 	
		  	if(!_.findWhere(this.indexedGraph.toArray(), {predicate: "prissma:environment"})) {
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:context",
					"prissma:environment",
					"_:env ."
				));
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:env",
					"rdf:type",
					"prissma:Environment ."
				));
			}
			
			if(!_.findWhere(this.indexedGraph.toArray(), {predicate: "prissma:currentPOI"})) {
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:env",
					"prissma:currentPOI",
					"_:poi ."
				));
			}
			
			this.indexedGraph.add(rdf.environment.createTriple(
				"_:poi",
				"rdf:type",
				"prissma:POI ."
			));
			this.indexedGraph.add(rdf.environment.createTriple(
				"_:poi",
				"geo:lat",
				"\"" + ac.lat + "\" ."
			));
			this.indexedGraph.add(rdf.environment.createTriple(
				"_:poi",
				"geo:lon",
				"\"" + ac.lon + "\" ."
			));
			this.indexedGraph.add(rdf.environment.createTriple(
				"_:poi",
				"prissma:radius",
				"\"" + ac.radius + "\" ."
			));
			
		 },
		  
		 addUserAc: function(userAc, queryStr, keywords){
			console.log("policy.addUserAc");
			
			if ((this.attributes.sparqlAC) && (queryStr)) {
				return this.addUserInAskQuery(userAc, queryStr, keywords);
			} else {
				this.addUserInContext(userAc, keywords);
			}
		 },
		 
		 addUserInAskQuery: function (userAc, queryStr, keywords) {
		 	var i;
		
			if(queryStr) {
				queryStr += "?context prissma:user ?consumer.\n"
				queryStr += "?consumer rdf:type foaf:Person.\n"
				for (i = 0; i < userAc.keywords.length; i++) {
					//filters in query not managed (ie impossible to generate a query with a filter clause)
					if (userAc.keywords[i].value.indexOf("\"") != -1) {
						queryStr += "?consumer " + userAc.keywords[i].prefix + ":" + userAc.keywords[i].localName + " " + userAc.keywords[i].value + ".\n";
					} else {
						queryStr += "?consumer " + userAc.keywords[i].prefix + ":" + userAc.keywords[i].localName + " <" + userAc.keywords[i].value + ">.\n";
					}
				}
				
				console.log("ask query: " + queryStr);
				return queryStr;
			}
			
		 },
		 
		 addUserInContext: function (userAc, keywords) {
		 	var i;
		 	
			this.indexedGraph.add(rdf.environment.createTriple(
				"_:context",
				"prissma:user",
				"_:consumer ."
			));
			this.indexedGraph.add(rdf.environment.createTriple(
				"_:consumer",
				"rdf:type",
				"foaf:Person ."
			));
			for (i = 0; i < userAc.keywords.length; i++) {
				//filters in query not managed (ie impossible to generate a query with a filter clause)
				if (userAc.keywords[i].value.indexOf("\"") != -1) {
					this.indexedGraph.add(rdf.environment.createTriple(
						"_:consumer",
						userAc.keywords[i].prefix + ":" + userAc.keywords[i].localName,
						userAc.keywords[i].value + " ."
					));
				} else {
					this.indexedGraph.add(rdf.environment.createTriple(
						"_:consumer",
						userAc.keywords[i].prefix + ":" + userAc.keywords[i].localName,
						"<" + userAc.keywords[i].value + "> ."
					));
				}
			}
		 },
	  
		 addDevEnvAc: function(ac, queryStr, keywords, type){
			
			console.log("policy.addDevEnvAc");
			if ((this.attributes.sparqlAC) && (queryStr)) {
				return this.addDevEnvInAskQuery(ac, queryStr, keywords, type);
			} else {
				this.addDevEnvInContext(ac, keywords, type);
			}
		 },
		 
		 addDevEnvInAskQuery: function (ac, queryStr, keywords, type) {
		 	var triples, i, tripleToPrepend, k;
				
			if(queryStr) {
				triples = "";
				if (type == "env") {
					if (queryStr.search("prissma:environment") == -1) {
						queryStr += "?context prissma:environment ?env.\n?env rdf:type prissma:Environment.\n";
					}
				} else {
					queryStr += "?context prissma:device ?dev.\n?dev rdf:type prissma:Device.\n"
				}
				for (i = 0; i < ac.keywords.length; i++) {
					
					tripleToPrepend = ac.keywords[i].prepend.split(".");
					for (k = 0; k < tripleToPrepend.length - 1; k++) {
						tripleToPrepend[k] = tripleToPrepend[k].replace(/ a /g, " rdf:type ");
						if ((triples.search(tripleToPrepend[k].replace(/\?/g, "\\\?")) == -1)
							//in case of environment if currentPOI and CurrentPOI already present
							&& (queryStr.search(tripleToPrepend[k].replace(/\?/g, "\\\?")) == -1)
						) {
							triples += tripleToPrepend[k] + "."
						}
					}
					
					if (ac.keywords[i].value.indexOf("\"") != -1) {
						triples += tripleToPrepend[k] + " " + ac.keywords[i].prefix + ":" + ac.keywords[i].localName + " " + ac.keywords[i].value + ".\n";
					} else {
						triples += tripleToPrepend[k] + " " + ac.keywords[i].prefix + ":" + ac.keywords[i].localName + " <" + ac.keywords[i].value + ">.\n";
					}
					
				}
				queryStr += triples.replace(/^\s*\n/gm, "");
								
				console.log("ask query: " + queryStr);
				return queryStr;
			}
		 },
		 
		 addDevEnvInContext: function (ac, keywords, type) {
		 	var triples, i, tripleToPrepend, k, found;
		 	
		 	if (type == "env") {
				if(!_.findWhere(this.indexedGraph.toArray(), {predicate: "prissma:environment"})){
					this.indexedGraph.add(rdf.environment.createTriple(
						"_:context",
						"prissma:environment",
						"_:env ."
					));
					this.indexedGraph.add(rdf.environment.createTriple(
						"_:env",
						"rdf:type",
						"prissma:Environment ."
					));
				}
			} else {
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:context",
					"prissma:device",
					"_:dev ."
				));
				this.indexedGraph.add(rdf.environment.createTriple(
					"_:dev",
					"rdf:type",
					"prissma:Device ."
				));
			}
		 	for (i = 0; i < ac.keywords.length; i++) {
					
				tripleToPrepend = ac.keywords[i].prepend.replace(/\?/g, "_:").replace(/\n/g, "").replace(/ a /g, " rdf:type ").split(".");
				for (k = 0; k < tripleToPrepend.length - 1; k++) {
					tripleToPrepend[k] = tripleToPrepend[k].split(" ");
					found = _.findWhere(this.indexedGraph.toArray(), {
							subject: tripleToPrepend[k][0],
							predicate: tripleToPrepend[k][1],
							object: tripleToPrepend[k][2] + " ."
					});
					if (!found) {
						this.indexedGraph.add(rdf.environment.createTriple(
							tripleToPrepend[k][0],
							tripleToPrepend[k][1],
							tripleToPrepend[k][2] + " ."
						));
					}
				}
				if (ac.keywords[i].value.indexOf("\"") != -1) {
					this.indexedGraph.add(rdf.environment.createTriple(
						tripleToPrepend[k].replace(/ /g, ""),
						ac.keywords[i].prefix + ":" + ac.keywords[i].localName,
						ac.keywords[i].value + " ."
					));
				} else {
					this.indexedGraph.add(rdf.environment.createTriple(
						tripleToPrepend[k].replace(/ /g, ""),
						ac.keywords[i].prefix + ":" + ac.keywords[i].localName,
						"<" + ac.keywords[i].value + "> ."
					));
				}
				
			}
		 },

  });
  
  return PrissmaDimModel;

});
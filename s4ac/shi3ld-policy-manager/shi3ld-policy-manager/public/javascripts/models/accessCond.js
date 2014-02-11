 /**
 * access coondition model
 * used to provide an infrastructure for views that deal with js object instead of 
 * query (since query is object of a triple)
 */
 
 define([
 'underscore',
 'backbone', 
 '/javascripts/lib/node-rdf-master/libBrowser/rdf.js',
 '/javascripts/lib/node-rdf-master/libBrowser/TurtleParser.js'], 
 function( _, Backbone, rdf, parser) {
  var AccessCondModel = Backbone.Model.extend((function() {
  
  	  /////////////////////////////////////////////////////
	  //              PRIVATE PROPERTIES
	  ////////////////////////////////////////////////////
	  
	  
		
		var loadPrefixes = function (sparqlPrefixes){
			
			var start, end, prefix, tokens, uri, run;
			
			run = true;
			queryEnv = new rdf.RDFEnvironment();
		  	start = sparqlPrefixes.search(/PREFIX/i);
		  	
		  	while(run){
		  		end = sparqlPrefixes.slice(start + 6).search(/PREFIX/i);
		  		if (end == -1) {
		  			end = sparqlPrefixes.length;
		  			run = false;
		  		} else {
		  			end +=start+6;
		  		}
		  		tokens = sparqlPrefixes.substring(start, end);
		  		prefix = tokens.replace(/PREFIX\s*/i,"");
		  		
				prefix = prefix.substring(0, prefix.indexOf(":"));
		  		start = tokens.indexOf("<") + 1;
		  		uri = tokens.substring(start, tokens.indexOf(">"));
		  		
		  		queryEnv.setPrefix(prefix, uri);
		  		
		  		start = end;
		  	}
		  	
		  	return queryEnv;
		};
		
		
		//user dimension represented in the query is a one-level tree (n-child)
		//with URIs or literals at leaf levels.
		var parseUser = function (env, tripletGraph, keywords) {
			var i, j, triples, userVar = null;
			var user = {
		  		keywords: []
		  	};
		  	
		  	console.log("AccessCondModel.parseUser");
		  	
			triples = tripletGraph.match(null, env.prefixes.get("prissma") + "user", null);
			if (triples.length > 0) {
				userVar = triples[0].object;
			}
			if (userVar == null) {
				//no user in access condition
				console.log("no user dimension in the access condition");
				return undefined;
			}
			
			parseUsrTriples(env, user, keywords, tripletGraph, userVar);
			return user;
		};
		
		var parseUsrTriples = function(env, user, keywords, g, userVar) {
			var triples;
			
			triples = g.match(userVar, null, null);
			//avoid triples with rdf:type predicate
			triples = _.reject(triples, function(triple){ return triple.predicate == env.prefixes.get("rdf") + "type"; });
			
			_.each(triples, function (triple) {
				var keyword = _.find(keywords.models, function(model){return model.get("uri") == triple.predicate});
				var obj = {};
				obj.label = keyword.get("label");
				if (triple.object.nodeType) {
					//is a literal
					obj.value = '"' + triple.object + '"'
				} else {
					//is a uri
					obj.value = triple.object;
				}
				obj.uri = keyword.get("uri");
				obj.prefix = keyword.get("prefix");
				obj.localName = keyword.get("localName");
				user.keywords.push(obj);
			}, user);
			
		}
		
		var parseDevEnv = function (env, tripletGraph, keywords, type) {
			var i, j, triples, acVar = null, poiVar;
			var ac = {
		  		keywords: []
		  	};
		  	console.log("AccessCondModel.parseDev");
		  	if (type == "dev") {
				triples = tripletGraph.match(null, env.prefixes.get("prissma") + "device", null);
				if (triples.length > 0) {
					acVar = triples[0].object;
				}
			} else {
				triples = tripletGraph.match(null, env.prefixes.get("prissma") + "environment", null);
				if (triples.length > 0) {
					acVar = triples[0].object;
					//remove triples related to time (considered a part)
					var triplesToSkip = tripletGraph.match(acVar, env.prefixes.get("ao") + "time", null);
					triplesToSkip = triplesToSkip.concat(tripletGraph.match(null, env.prefixes.get("geo") + "lat", null));
					triplesToSkip = triplesToSkip.concat(tripletGraph.match(null, env.prefixes.get("geo") + "lon", null));
					triplesToSkip = triplesToSkip.concat(tripletGraph.match(null, env.prefixes.get("prissma") + "radius", null));
					_.each(triplesToSkip, function(triple) {						
						tripletGraph.remove(triple);
					});
				}
			}
			if (acVar == null) {
				console.log("no " + type + " dimension in the access condition");
				return undefined;
			}
			
			parseDevEnvTriples(env, ac, keywords, tripletGraph, acVar, new Array(), null);
			
			_.each(ac.keywords, function (keyword) {
				//type of ?dev and ?env already present
				if (type == "dev") {
					keyword.prepend = keyword.prepend.replace("?" + acVar.value + " rdf:type prissma:Device.\n", "");
				} else {
					keyword.prepend = keyword.prepend.replace("?" + acVar.value + " rdf:type prissma:Environment.\n", "");
					poiVar = tripletGraph.match(acVar, env.prefixes.get("prissma") + "currentPOI", null);
					if ((poiVar) && (poiVar.length > 0)) {
						poiVar = poiVar[0].object;
						keyword.prepend = keyword.prepend.replace(new RegExp(poiVar.value, "g"), "poi")
					}
					//keyword.prepend = keyword.prepend.replace(/\?env prissma:currentPOI \?b[0-9]+./, "?env prissma:currentPOI ?poi.");
				}
				//remove '.\n' at the beginning and keep name of ?dev or ?env var
				keyword.prepend = keyword.prepend.replace(new RegExp(acVar.value, "g"), type).substr(2);
			})
			//may be environment is present but only for time interval and currentPOI (lat lon radius)
			if (ac.keywords.length == 0) return undefined;
			return ac;
		};
		
		//dev structure is a tree with more level
		//URI and literals present only at leaf level (n-child)
		//it is parsed exploting triples at each level from move to a node towords deeper level
		//at leaf levels literals, URIs or owl classes (object of rdf:type)
		//Generating the prepend for a leaf means generate the path from father of the leaf up to root
		
		// toBePrepended keeps track of Json models of the property that are related to same path up to root
		//inorder to prepend the current node to all the path that came from a leaf that is URI or literals
		
		//prevPredicate allow to distinguish to wich node is append a property that could be reffered to more eg (model)
		var parseDevEnvTriples = function(env, ac, keywords, g, subject, toBePrepended, prevPredicate) {
			var triples, i, leaf, obj, k;
			var typeTriple = null;
			if ((!subject.nodeType) || (subject.nodeType() != "BlankNode")) {
				//leaf reach and is URI or literals
				return true;
			}
			triples = g.match(subject, null, null);
			for (i = 0; i < triples.length; i++) {
				if (triples[i].predicate == (env.prefixes.get("rdf") + 'type')) {
					typeTriple = triples[i];
					continue;
				}
				var children = new Array();
				leaf = parseDevEnvTriples(env, ac, keywords, g, triples[i].object, children, triples[i].predicate);
				//toBePrepended = toBePrepended.concat(children);
				_.each(children, function (child) {
					toBePrepended.push(child);
				});
				//check if is a leaf and is not an owl class (predicate is not rdf:type)
				if (leaf == true) {
					obj = {};
					var keyword = _.find(keywords.models, function(model) {
						var res = (model.get("uri") == triples[i].predicate);
						if (prevPredicate)
							res = res && (model.get("prepend").search(_getTurtleIri(prevPredicate, env)) != -1)
						return res;
					});
					obj.label = keyword.get("label");
					if (triples[i].object.nodeType) {
						//is a literal
						obj.value = '"' + triples[i].object + '"'
					} else {
						//is a uri
						obj.value = triples[i].object;
					}
					obj.uri = keyword.get("uri");
					obj.prefix = keyword.get("prefix");
					obj.localName = keyword.get("localName");
					obj.prepend = ".\n?" + subject;
					//keep track of index of obj in the keyword array so that when node processing
					//finished prepend field could be updated
					toBePrepended.push(ac.keywords.length);
					ac.keywords.push(obj);
					continue;
				}
				for (k = 0; k < children.length; k++) {
					//if is not the father keep track also of the property
					ac.keywords[children[k]].prepend = _getTurtleIri(triples[i].predicate, env) + " ?" + _getTurtleIri(triples[i].object.value, env) + ac.keywords[children[k]].prepend;
					ac.keywords[children[k]].prepend = ".\n?" + subject + " " + ac.keywords[children[k]].prepend;
				}								
			}
			//add the type triple if any
			if(typeTriple) {
				for (k = 0; k < toBePrepended.length; k++) {
					//if is not the father keep track also of the property
					ac.keywords[toBePrepended[k]].prepend = ".\n?" + subject + " " +
					                                         _getTurtleIri(typeTriple.predicate, env) + " " +
					                                         _getTurtleIri(typeTriple.object, env) +
					                                         ac.keywords[toBePrepended[k]].prepend;
				}
			}
			return false;			
		}
		
		//format parameter not dateFormat since same name of the library function
		var parseTime = function (ac, tripletGraph, env, timeFormat, format) {
			var i, j, triples, timeVar = null;
			var d, m, y, duration;
			//NBB CHECK ERRORS
			
		  	console.log("AccessCondModel.parseTime");
		  	triples = tripletGraph.match(null, env.prefixes.get("ao") + "time", null);
			if (triples.length > 0) {	
				timeVar = triples[0].object;				
			}
			if (timeVar == null) {
				console.log("no time in the environment dimension of the access condition");
				return undefined;
			}
			
			ac.time = "time";
			triples = tripletGraph.match(timeVar, env.prefixes.get("tl") + "start", null);
			ac.dateFrom = dateFormat( triples[0].object.value, format);
			ac.timeFrom = dateFormat(triples[0].object.value, timeFormat)
			
			y = ac.dateFrom.substring(6, ac.dateFrom.length);
			if(format == "dd/mm/yyyy"){
				d = ac.dateFrom.substring(0,2);
		    	m = ac.dateFrom.substring(3,5);
			} else {
				m = ac.dateFrom.substring(0,2);
		    	d = ac.dateFrom.substring(3,5);
			}
			var date = new Date(y, m - 1, d, ac.timeFrom.substring(0, 2), ac.timeFrom.substring(3,5), 0);
			
			triples = tripletGraph.match(null, env.prefixes.get("tl") + "duration", null);
			duration = parseInt(triples[0].object) * 1000;
			date.setTime(date.getTime() + duration);
			ac.dateTo = date.format(format);
			ac.timeTo = date.format(timeFormat);
			return ac;
		};
		
		var parseOutdoor = function (ac, tripletGraph, env) {
			var i, j, triples, outdoorVar = null;
			var lat, lon;
			//NBB CHECK ERRORS
			
		  	console.log("AccessCondModel.parseOutdoor");
		  	triples = tripletGraph.match(null, env.prefixes.get("prissma") + "currentPOI", null);
			if (triples.length > 0) {
				outdoorVar = triples[0].object;				
			}
			if (outdoorVar == null) {
				//no location in access condition
				console.log("no location in the environment dimension of the access condition");
				return undefined;
			}
			
			//NB in outdoor dimension there are other ways to represent the location possible with prissma 
			//vocabulary not managed by GUI (eg POICategory)
			//than also if query is ok wrt prissma model GUI can't represent it.
			triples = tripletGraph.match(null, env.prefixes.get("geo") + "lat", null);
			if (triples.length == 0) {
				return ac;
			}
			lat = triples[0].object.value;
			triples = tripletGraph.match(null, env.prefixes.get("geo") + "lon", null);
			if (triples.length == 0) {
				return ac;
			}
			lon = triples[0].object.value;
			ac.outdoor = "outdoor";
			triples = tripletGraph.match(null, env.prefixes.get("prissma") + "radius", null);
			if (triples.length == 0) {
				return ac;
			}
			//only if all the 3 parameters (radius, lat and lon) are found assign an outdoor dim to the accessCondModel
			ac.radius = triples[0].object.value;
			ac.lat = lat;
			ac.lon = lon;
			ac.outdoor = "outdoor";
			return ac;
		};
	  
	  	var _getTurtleIri = function(uri, env) {
	  		//var env = this.env;
	        for (var prefix in env.prefixes){
		      	if((!(typeof env.prefixes[prefix] == "function")) && uri.search(env.prefixes.get(prefix)) != -1) {
		      		uri = uri.replace(env.prefixes.get(prefix),prefix + ":");
		      		prefixFound = true;
		      		break;
		      	}
	       }
	       if(!prefixFound) {
	      	uri = "<" + uri + ">";
	       }
	       
	       return uri;  
	    };
	    
	  /////////////////////////////////////////////////////
	  //              PUBLIC PROPERTIES
	  ////////////////////////////////////////////////////
	  
	  return {
		
		//Function to get an accesCond model from an ask query or a turtle graph
		//options.accessCondition may be the ask query in case of sparql condition
		//or the whole turtle graph in case of context
		
		parse: function (options) {
			var ac, query, parseStr;
			
			console.log("accessConditionModel.parse");
			console.log("initial model: " + JSON.stringify(this));
			ac = {};
			  	
		  	query = options.accessCondition;
		  	query = query.toString()
		  	query = query.replace(/\bask{\b/, /\bASK\s{\b/);
		  	query = query.replace(/\bask\b/, /\bASK\b/);
		  	
		  	if (query.search(/\bASK\b/) != -1) {
		  		query = query.split("ASK");
		  		//assumed only 1 ask clause
			  	query[1] = query[1].replace(/\?/g, "_:");
			  	//the library used had problems with the '.' at the end of blank node
			  	query[1] = query[1].replace(/\.(\s|$)/g, " . ");
			  	//unwrap from external pairs of curly backets
			  	//assumed only 1 pair of curly brackets ie ask {...}
			  	//inner brackets cause problem to the parser since it expect turtle text (no curly brackets) 
			  	query[1] = query[1].substring(query[1].indexOf("{") + 1, query[1].lastIndexOf("}"));			  	
		  		this.env = loadPrefixes(query[0]);
		  		parseStr = query[1];
		  		var turtleParser = new parser.Turtle(this.env);
		  	} else {
		  		//in case of context the whole graph serialization is passed
		  		//the env can be directly obtained by the parsing since @prefix directives are present
		  		parseStr = query;
		  		var turtleParser = new parser.Turtle();
		  	}
		  	var tripletGraph = new rdf.TripletGraph;	
			turtleParser.parse(parseStr, undefined, this.attributes.defaultBase, undefined, tripletGraph);
			this.env = turtleParser.environment;
			
		  	if((parseStr.search("prissma:user") != -1) || (parseStr.search(this.env.prefixes.get("prissma") + "user") != -1)) {
		  		ac.user = parseUser(this.env, tripletGraph, this.attributes.keywordsUsr);
		  		console.log("ac after parseUser: " + JSON.stringify(ac));
		  	}
		  	if((parseStr.search("prissma:device") != -1) || (parseStr.search(this.env.prefixes.get("prissma") + "device") != -1)) {
		  		ac.dev = parseDevEnv(this.env, tripletGraph, this.attributes.keywordsDev, "dev");
		  		console.log("ac after parseDev: " + JSON.stringify(ac));
		  	}	
		  	if((parseStr.search("ao:time") != -1) || (parseStr.search(this.env.prefixes.get("ao") + "time") != -1)) {
		  		ac.time = "time";
		  		ac = parseTime(ac, tripletGraph, this.env, this.attributes.timeFormat, this.attributes.dateFormat);
		  		console.log("ac after parseTime: " + JSON.stringify(ac)); 
		  	}
		  	if((parseStr.search("prissma:currentPOI") != -1) || (parseStr.search(this.env.prefixes.get("prissma") + "currentPOI") != -1)) {
		  		this.attributes.outdoor = "outdoor";
		  		ac = parseOutdoor(ac, tripletGraph, this.env);
		  		console.log("ac after parseOutdoor: " + JSON.stringify(ac));  
		  	}
		  	//must be last since remove other env triples from graphs
		  	if((parseStr.search("prissma:environment") != -1) || (parseStr.search(this.env.prefixes.get("prissma") + "environment") != -1)) {
		  		ac.env = parseDevEnv(this.env, tripletGraph, this.attributes.keywordsEnv, "env");
		  		console.log("ac after parseEnv: " + JSON.stringify(ac));
		  	}	  	
		  		  	
		  	this.set({ac: ac});	
		  	console.log("model after parsing of query: " + JSON.stringify(ac));	  
		  
		},
		
	    
	    initialize: function (options) {
	    	if ((options) && (options.accessCondition)) {
	    		this.parse(options);
	    	} else {
	    		this.attributes.ac = {};
	    		console.log("empty ac");
	    	}
	    },
	    
	    getTurtleIri: function (uri) {
	    	return _getTurtleIri(uri, this.env);
	    }
	
	  };
	  
  })());
  
  return AccessCondModel;

});
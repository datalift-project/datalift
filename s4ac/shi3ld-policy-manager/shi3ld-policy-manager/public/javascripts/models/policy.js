/**
 * policy model
 * main structures
 * indexedGraph: keep the policy triples readable and compact (used to show policy in editor and to serialize). triples accessible by idx only
 * tripletGraph: allow match function to retrieve triple by any combination of subject,object, predicate. used to manipulate policy in GUI
 * triple2idx: allow to map a triple in triple graph to the corresponding index in indexedGraph (to keep indexedGraph consistent with tripletGraph)
 * cnt: counter of triples in order to automatic assign the index to the triples
 */
 
 define([
 'underscore',
 'backbone',
 'models/accessCond',
 'models/graph', 
 '/javascripts/lib/node-rdf-master/libBrowser/rdf.js',
 '/javascripts/lib/node-rdf-master/libBrowser/TurtleParser.js'], 
 function( _, Backbone, AccessCondModel, GraphModel, rdf, parser) {
  var PolicyModel = GraphModel.extend((function() {
  
  	  /////////////////////////////////////////////////////
	  //              PRIVATE PROPERTIES
	  ////////////////////////////////////////////////////
  		
  	  var initAskQuery = function(){
	  	return "PREFIX prissma: <http://ns.inria.fr/prissma/v2#>\n" + 
				"PREFIX hard: <http://www.w3.org/2007/uwa/context/hardware.owl#>\n" +
				"PREFIX dcn: <http://www.w3.org/2007/uwa/context/deliverycontext.owl#>\n" +
				"PREFIX foaf: <http://xmlns.com/foaf/0.1/>\n" +
				"PREFIX common: <http://www.w3.org/2007/uwa/context/common.owl#>\n" +
				"PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>\n" +
				"PREFIX geo: <http://www.w3.org/2003/01/geo/wgs84_pos#>\n" +
				"PREFIX ao: <http://purl.org/ontology/ao/core#>\n" +
				"PREFIX soft: <http://www.w3.org/2007/uwa/context/software.owl#>\n" + 
				"PREFIX tl: <http://purl.org/NET/c4dm/timeline.owl#>\n" +
				"PREFIX rel: <http://purl.org/vocab/relationship/>\n" +
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
				"PREFIX net: <http://www.w3.org/2007/uwa/context/network.owl#>\n" +
				"PREFIX web: <http://www.w3.org/2007/uwa/context/web.owl#>\n" +
				"PREFIX push: <http://www.w3.org/2007/uwa/context/push.owl#>\n" +
				"PREFIX java: <http://www.w3.org/2007/uwa/context/java.owl#>\n" +
				"PREFIX time: <http://www.w3.org/2006/time#>\n\n" +
				"ASK {\n?context rdf:type prissma:Context. \n";
	  };
	  
	  /////////////////////////////////////////////////////
	  //              PUBLIC PROPERTIES
	  ////////////////////////////////////////////////////
	  
	  return {
	  
	  	//create the graph and put the triples that in any case will be in it
  	    createPolicyIndexedGraph: function(policyWizard) {
		  	g = this.initIndexGraph();
		  	var trimedName = policyWizard.get("name").replace(/\s/g, "-");
		  	
			g.add(rdf.environment.createTriple(
				":" + trimedName ,
				"rdf:" + 'type',
				"s4ac:" + 'AccessPolicy.'
			));
			g.add(rdf.environment.createTriple(
				":" + trimedName ,
				"rdfs:" + 'label',
				"\"" + policyWizard.get("name") + "\"."
			));
			if(policyWizard.get("targets")) {
				_.each(policyWizard.get("targets"), function(target) {
					g.add(rdf.environment.createTriple(
						":" + trimedName ,
						"s4ac:" + 'appliesTo',
						"<" + target.uri + ">."
					));
			    });
			}
			g.add(rdf.environment.createTriple(
				":" + trimedName ,
				"s4ac:" + 'hasAccessConditionSet',
				//to get an unique id for the acs prepend policy name
				":" + trimedName + '-ACS.'
			));
			
			g.add(rdf.environment.createTriple(
				":" + trimedName + '-ACS',
				"rdf:" + 'type',
				"s4ac:" + 'ConjunctiveAccessConditionSet.'
			));
			
			return g;
	  	},
	  
	    createPolicyTripletGraph: function(policyWizard){
		  	var g = new rdf.TripletGraph;
		  	var t;
		  	
		  	var trimedName = policyWizard.get("name").replace(/\s/g, "-");
		  	
		  	t = rdf.environment.createTriple(
				this.env.prefixes.get("") + trimedName ,
				this.env.prefixes.get("rdf") + 'type',
				this.env.prefixes.get("s4ac") + 'AccessPolicy'
			);
			g.add(t);
			var v = t.subject+t.predicate+t.object;
			console.log(JSON.stringify(this.triple2idx));
			console.log("t2idx: " + this.triple2idx[t.subject+t.predicate+t.object]);
			console.log("t2idx: " + this.triple2idx[v]);
			this.triple2idx[t.subject + t.predicate + t.object] = {};
			this.triple2idx[t.subject + t.predicate + t.object].idx = this.cnt++;
			
			t = rdf.environment.createTriple(
				this.env.prefixes.get("") + trimedName ,
				this.env.prefixes.get("rdfs") + 'label',
				rdf.environment.createLiteral(policyWizard.get("name"), null, null)
			);
			g.add(t);
			this.triple2idx[t.subject + t.predicate + t.object] = {};
			this.triple2idx[t.subject + t.predicate + t.object].idx = this.cnt++;
			
			if(policyWizard.get("targets")) {
				_.each(policyWizard.get("targets"), function(target) {
					t = rdf.environment.createTriple(
						this.env.prefixes.get("") + trimedName ,
						this.env.prefixes.get("s4ac") + 'appliesTo',
						target.uri
					);
					g.add(t);
					this.triple2idx[t.subject + t.predicate + t.object] = {};
					this.triple2idx[t.subject + t.predicate + t.object].idx = this.cnt++;
				}, this);
			}
			t = rdf.environment.createTriple(
				this.env.prefixes.get("") + trimedName ,
				this.env.prefixes.get("s4ac") + 'hasAccessConditionSet',
				//to get an unique id for the acs prepend policy name
				this.env.prefixes.get("") + trimedName + '-ACS'
			);
			g.add(t);
			this.triple2idx[t.subject + t.predicate + t.object] = {};
			this.triple2idx[t.subject + t.predicate + t.object].idx = this.cnt++;
			
			t = rdf.environment.createTriple(
				this.env.prefixes.get("") + trimedName + '-ACS',
				this.env.prefixes.get("rdf") + 'type',
				this.env.prefixes.get("s4ac") + 'ConjunctiveAccessConditionSet'
			);
			g.add(t);
			this.triple2idx[t.subject + t.predicate + t.object] = {};
			this.triple2idx[t.subject + t.predicate + t.object].idx = this.cnt++;
			
			return g;
	  	},
	  
		//by basing on content received by wizard IE wizard model
		//generate the corresponding graphs. 
		generatePolicy: function(policyWizard){
		  	var queryStr, ac, i, tripleCnt, privileges;
		  	
		  	console.log("policyModel.generatePolicy");
		  	console.log("policyWizard: " + JSON.stringify(policyWizard));
		  	
		  	var triples = new Array();
		  	var privileges = policyWizard.get("privileges");
		  	var trimedName = policyWizard.get("name").replace(/\s/g, "-");
		  	
		  	for (tripleCnt = 0; tripleCnt < privileges.length; tripleCnt++) {
			  	
			  	if (privileges[tripleCnt]) {
				  	triples[tripleCnt] = rdf.environment.createTriple(
						this.env.prefixes.get("") + trimedName,
						this.env.prefixes.get("s4ac") + 'hasAccessPrivilege',
						//since corresponding ac may be reused in diff policy unique name
						//not related to policy with this pattern {prefix}/AC-{id}
						this.env.prefixes.get("s4ac") + privileges[tripleCnt]
					);
					this.indexedGraph.add(rdf.environment.createTriple(
						":" + trimedName ,
						"s4ac:" + 'hasAccessPrivilege',
						"s4ac:" + privileges[tripleCnt] + '.'
					));
				}
			}
		  	
		  	ac = policyWizard.get("ac");
		  	//console.log("ac local: " + JSON.stringify(ac));
		  	if((ac == undefined) || (jQuery.isEmptyObject(ac))){
		  		//no access condition defined
		  		console.log("no acs!");
		  		this.tripletGraph.importArray(triples);
		  		return;
		  	}
			
		  	triples[tripleCnt++] = rdf.environment.createTriple(
				this.env.prefixes.get("") + trimedName + "-ACS",
				this.env.prefixes.get("s4ac") + 'hasAccessCondition',
				//since corresponding ac may be reused in diff policy unique name
				//not related to policy with this pattern {prefix}/-{cnt}
				this.env.prefixes.get("") + "AC-" + policyWizard.get("acCnt") 
			);
			
			this.indexedGraph.add(rdf.environment.createTriple(
				":" + trimedName + "-ACS",
				"s4ac:" + 'hasAccessCondition',
				":" + "AC-" + policyWizard.get("acCnt") + "."
			));
			
			triples[tripleCnt++] = rdf.environment.createTriple(
				this.env.prefixes.get("") + "AC-" + policyWizard.get("acCnt"),
				this.env.prefixes.get("rdfs") + 'label',
				//since corresponding ac may be reused in diff policy unique name
				//not related to policy with this pattern {prefix}/-{cnt}
				rdf.environment.createLiteral("AC " + policyWizard.get("acCnt"), null, null)
			);
			
			this.indexedGraph.add(rdf.environment.createTriple(
				":" + "AC-" + policyWizard.get("acCnt"),
				"rdfs:" + 'label',
				"\"" + "AC " + policyWizard.get("acCnt") + "\"."
			));
			
			if(this.attributes.sparqlAC){
				console.log("ac has query");
				queryStr = this.generateAskQuery(ac, policyWizard);			
			}
			else{
				//generate condition as context graph (triples)
			}
			
			triples[tripleCnt++] = rdf.environment.createTriple(
				//since corresponding ac may be reused in diff policy unique name
				//not related to policy with this pattern {prefix}/-{cnt}
				this.env.prefixes.get("") + "AC-" + policyWizard.get("acCnt"),
				this.env.prefixes.get("rdf") + 'type',
				this.env.prefixes.get("s4ac") + 'AccessCondition'
			);
			this.indexedGraph.add(rdf.environment.createTriple(
				":" + "AC-" + policyWizard.get("acCnt"),
				"rdf:" + 'type',
				"s4ac:" + 'AccessCondition.'
			));
		  	
		  	if(queryStr != null){
			  	triples[tripleCnt++] = rdf.environment.createTriple(
					this.env.prefixes.get("") + "AC-" + policyWizard.get("acCnt"),
					this.env.prefixes.get("s4ac") + 'hasQueryAsk',
					rdf.environment.createLiteral(queryStr, null, null)
				);
				this.indexedGraph.add(rdf.environment.createTriple(
					":" + "AC-" + policyWizard.get("acCnt"),
					"s4ac:" + 'hasQueryAsk',
					"\"\"\"" + queryStr + "\"\"\"."
				));
				
			}else{
				//manage context in case of LDP if necessary
			}
			
		  	for(i = 0; i < triples.length; i++){
		  		this.triple2idx[triples[i].subject + triples[i].predicate + triples[i].object] = {};
		  		this.triple2idx[triples[i].subject + triples[i].predicate + triples[i].object].idx = this.cnt++;
			}
			this.tripletGraph.importArray(triples);
			
		},
		
		//custom url
		url: function () { 
			return '/policies/?policy=' + this.attributes.id;
		},	
	    
	    sync: function(method, model, options) {
	    	
	    	console.log("PolicyModel.sync");
		   
	    	var methodMap = {
		    	'create': 'POST',
		    	'update': 'PUT',
		    	'patch':  'PATCH',
		    	'delete': 'DELETE',
		    	'read':   'GET'
		  	};
			var type = methodMap[method];
		 	var params = {
				type:         type,
				contentType:  'text/turtle',
				dataType:     'text',
				processData:  true
			};
			
			if (!options.url) {
	      		params.url = _.result(model, 'url') || urlError();
	    	}
	    	
	    	if (options.data == null && model && (method === 'create' || method === 'update' || method === 'patch')) {
	      		console.log("preparing request");
	      		params.contentType = 'text/turtle';
	      		params.data = this.indexedGraph.toNT();
	      		console.log("policy indexed graph:");
		    	console.log(this.indexedGraph.toNT());
		    	console.log("request contentType:");
		    	console.log("request body:");
		    	console.log(params.data);
	    	}
	    	
	    	console.log("whole request (params obj): ");
	    	console.log(JSON.stringify(params));
	    	
	    	var xhr = options.xhr = Backbone.ajax(_.extend(params, options));
	    	model.trigger('request', model, xhr, options);
	    	console.log(xhr);
	    	return xhr;
		},
		
	    initialize: function(options) {
	    	//add toISOString into old browsers
	    	if ( !Date.prototype.toISOString ) {
				  this.addIsoString();
			}
	    	console.log("PolicyModel.initialize");
	    	console.log("options.policyWizard: " + JSON.stringify(options.policyWizard));
	    	
	    	this.triple2idx = {};
	    	this.cnt = 0;
	    	
	    	this.on('loadACS', this.loadACS, this);
	    		
	    	if(options.policyWizard){
	    		//policy created by wizard
    			this.env = this.initEnv();
				var trimedName = options.policyWizard.get("name").replace(/\s/g, "-");
				this.set({id: encodeURIComponent(this.env.prefixes.get("") + trimedName)});
				
		    	this.indexedGraph = this.createPolicyIndexedGraph(options.policyWizard);
		    	console.log("indexed g created");
		    	this.tripletGraph = this.createPolicyTripletGraph(options.policyWizard);
		    	console.log("triplet g created");
		    	this.generatePolicy(options.policyWizard);
	    	  	console.log("gen policy");
	    	  	
	    	  	console.log("triple2idx:" + JSON.stringify(this.triple2idx));
		    	console.log("policy model(indexed graph):");
			    console.log(this.indexedGraph.toNT());
			    console.log("policy model(triplet graph):");
			    console.log(this.tripletGraph.toNT());
			    this.set({dateFormat : options.policyWizard.get("dateFormat")});
	    		this.set({timeFormat : options.policyWizard.get("timeFormat")});
			    
	    	}else{
	    		this.set({id: encodeURIComponent(options.uri)});
	    		this.env = this.initEnv();
	    	}
	    	
	    	this.unset("policyWizard");    	
	    },
	    
	    //add at the bottom because if user has edited it directly keep order
	    //no easy way to detect if managed only by GUI
	    add: function(triple) {
	    	console.log("policyModel.add");
	    	
	    	this.indexedGraph.add(this.env.createTriple(
				triple.subject,
				triple.predicate,
				triple.object + "."
			));
	    	triple = this.normalizeTriple(triple);
	    	
	    	this.tripletGraph.add(this.env.createTriple(
				triple.subject,
				triple.predicate,
				triple.object
			));
			this.triple2idx[triple.subject + triple.predicate + triple.object] = {};
	    	this.triple2idx[triple.subject + triple.predicate + triple.object].idx = this.cnt++;
	    	
	    	console.log("indexed:");
	    	console.log(this.indexedGraph.toNT());
	    	console.log("triplet:");
	    	console.log(this.tripletGraph.toNT());
	    },
	    	    
	    generateAskQuery: function(ac, acUtil) {
	    	var queryStr = initAskQuery();
			if (ac.user) {
				console.log("user in ac");
				//TBD manage context graph case
		  		queryStr = this.addUserAc(ac.user, queryStr, acUtil.keywords || acUtil.get("keywords"));
		  	}
			if (ac.time) {
				console.log("time in ac");
				//TBD manage context graph case
		  		queryStr = this.addTimeAc(ac, queryStr, acUtil.dateFormat || acUtil.get("dateFormat"));
		  	}
		  	if (ac.outdoor) {
				console.log("outdoor in ac");
				//TBD manage context graph case
		  		queryStr = this.addOutdoorAc(ac, queryStr);
		  	}
		  	if (ac.env) {
		  		console.log("env in ac");
				//TBD manage context graph case
		  		queryStr = this.addDevEnvAc(ac.env, queryStr, acUtil.keywordsEnv || acUtil.get("keywordsEnv"), "env");
		  	}
		  	if (ac.dev) {
		  		console.log("dev in ac");
				//TBD manage context graph case
		  		queryStr = this.addDevEnvAc(ac.dev, queryStr, acUtil.keywordsDev || acUtil.get("keywordsDev"), "dev");
		  	}
		  	
		  	queryStr += "}";
		  	
		  	return queryStr;
	    },
	    
	    isNew: function () {
	    	return this.get("newModel");
	    },
	    
	    toTurtleGraph: function (NTgraph) {
	    	var object, i;
	    	var g = new rdf.IndexedGraph;
	    	var pol, acSet, acs, polLen, acSetLen, acsLen;
	    	var polUri, acSetUri, acsUris;
	    	
	    	polUri = this.tripletGraph.match(null, this.env.prefixes.get("rdf") + "type", this.env.prefixes.get("s4ac") + "AccessPolicy");
	    	if(polUri.length != 1) {
	    		console.log("error on policyUri");
	    		return;
	    	}
	    	pol = new Array();
	    	polUri = polUri[0].subject;
	    	
	    	acSetUri = this.tripletGraph.match(polUri, this.env.prefixes.get("s4ac") + "hasAccessConditionSet", null);
	    	if(acSetUri.length != 1) {
	    		console.log("error on acSetUri");
	    		return;
	    	}
	    	acSet = new Array();
	    	acSetUri = acSetUri[0].object;
	    	
	    	acsUris = this.tripletGraph.match(null, this.env.prefixes.get("rdf") + "type", this.env.prefixes.get("s4ac") + "AccessCondition");
	    	
	    	for (i = 0; i < acsUris.length; i++) {
	    		acsUris[i] = acsUris[i].subject;
	    	}
	    	acs = new Array();
	    	others = new Array();
	    	//add prefixes
	    	for (var prefix in this.env.prefixes){
	    		if (!(typeof this.env.prefixes[prefix] == "function")) {
		    		g.add(rdf.environment.createTriple(
						"@prefix",
						prefix + ":",
						"<" + this.env.prefixes.get(prefix) + ">."
					));
				}
	    	}
	    	var triples = NTgraph.toArray();
	    	var l = triples.length;
	    	for(i = 0; i < l; i++) {
	    		
	    		if (triples[i].object.nodeType){
	    			if (triples[i].object.value.indexOf("\n") == -1) {
	    				object = '"' + triples[i].object.value + '"';
	    			} else {
	    				object = '"""' + triples[i].object.value + '"""';
	    			}
	    			if(triples[i].object.datatype) {
	    				object += triples[i].object.datatype;
	    			}
	    		} else {
	    			object = this.getTurtleIri(String(triples[i].object));
	    		}
	    		if (triples[i].subject == polUri) {
	    			pol.push(rdf.environment.createTriple(
						this.getTurtleIri(String(triples[i].subject)),
						this.getTurtleIri(String(triples[i].predicate)),
						object + "."
					));
	    		} else if (triples[i].subject == acSetUri){
	    			acSet.push(rdf.environment.createTriple(
						this.getTurtleIri(String(triples[i].subject)),
						this.getTurtleIri(String(triples[i].predicate)),
						object + "."
					));
	    		} else if (_.indexOf(acsUris, triples[i].subject) != -1){
	    			acs.push(rdf.environment.createTriple(
						this.getTurtleIri(String(triples[i].subject)),
						this.getTurtleIri(String(triples[i].predicate)),
						object + "."
					));	    			
	    		} else {
	    			others.push(rdf.environment.createTriple(
						this.getTurtleIri(String(triples[i].subject)),
						this.getTurtleIri(String(triples[i].predicate)),
						object + "."
					));
	    		}
	    	}
	    	g.importArray(_.sortBy(pol,  function (item){
		    	return item.predicate;
		    }).reverse());
		    g.importArray(_.sortBy(acSet, function (item){
		    	return item.predicate + item.object;
		    }).reverse());
		    g.importArray(_.sortBy(acs, 'subject').reverse());
		    g.importArray(others);
		    
		    return g;
	    },
	    
	    loadACS: function (options) {
	    	var ac, t, that = this;
	    	this.set({acs: options.collection});
	    	    	
	    	//assumed one access condition set
	    	var acSet = this.tripletGraph.match(null, this.env.prefixes.get("s4ac") + 'hasAccessConditionSet', null);
	    	var tmp = this.tripletGraph.match(acSet[0].object, this.env.prefixes.get("s4ac") + 'hasAccessCondition', null)
	    	//actually each ac should have rdf:type s4ac:AccessCondition but accept also if type missing and rely on triple
	    	//that add ac to acs.
	    	_.each(this.tripletGraph.match(acSet[0].object, this.env.prefixes.get("s4ac") + 'hasAccessCondition', null), 
	    		function (triple) {
	    			t = that.tripletGraph.match(triple.object, that.env.prefixes.get("s4ac") + 'hasQueryAsk', null);
	    			if ((!t) || (t.length == 0)) {
	    				return;
	    			} 
	    			var model = new AccessCondModel({
	    				uri: triple.object,
	    				accessCondition: t[0].object,
	    				defaultBase: that.attributes.defaultBase,
	    				dateFormat: that.attributes.dateFormat,
	    				timeFormat: that.attributes.timeFormat,
	    				keywordsUsr: options.keywordsUsr,
	    				keywordsDev: options.keywordsDev,
	    				keywordsEnv: options.keywordsEnv,
	    				type: "query"
	    			});
	    			
	    			t = that.tripletGraph.match(triple.object, that.env.prefixes.get("rdfs") + 'label', null);
	    			var label ="";
	    			//assumed 1 label only (actually may be more eg. one per language)
	    			if ((t) && (t.length > 0)) {
	    				label = t[0].object;
	    			}
	    			model.set({label: label});
	    			options.collection.add(model);
	    	});
	    	options.collection.trigger('sync');
	    },
	    
	    resetTriple2Idx: function () {
		  	var i, arr, triple;
		  	
		  	arr = this.indexedGraph.toArray();
		  	this.triple2idx = {};
		  	
		  	for (this.cnt = 0; this.cnt < arr.length; this.cnt++) {
		  		triple = {};
				triple = this.normalizeTriple(arr[this.cnt]);
				this.triple2idx[triple.subject + triple.predicate + triple.object] = {};
				this.triple2idx[triple.subject + triple.predicate + triple.object].idx = this.cnt; 
		   }
		},
	  
	 
	
	  };
	  
  })());
  
  return PolicyModel;

});
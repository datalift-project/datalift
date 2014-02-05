/**
 * New node file
 */
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
  var ContextModel = GraphModel.extend((function() {
  
  	  /////////////////////////////////////////////////////
	  //              PRIVATE PROPERTIES
	  ////////////////////////////////////////////////////
  		
  	  
	  
	  
	  /////////////////////////////////////////////////////
	  //              PUBLIC PROPERTIES
	  ////////////////////////////////////////////////////
	  
	  return {
	  	
	    initialize: function(options) {
	    	//add toISOString into old browsers
	    	if ( !Date.prototype.toISOString ) {
				  this.addIsoString();
			}
	    	console.log("ContextModel.initialize");
	    	this.env = this.initEnv({isContext: true});
	    	
	    	this.indexedGraph = this.initIndexGraph(this, options.policyWizard);
	    	
	    	this.cnt = 0;
    	  	
    	  	var acModel = new AccessCondModel({
				defaultBase: this.attributes.defaultBase,
				dateFormat: this.attributes.dateFormat,
				timeFormat: this.attributes.timeFormat,
				keywordsUsr: options.keywordsUsr,
				keywordsDev: options.keywordsDev,
				keywordsEnv: options.keywordsEnv,
				type: "context"
			});
	    	this.set({context: acModel});
	    	
	    },
	    
	    //same function used both to serialize the context and to desirialize
	    toTurtleGraph: function (options) {
	    	var triples, acStr, i;
	    			
      		if ((!options) || (!options.serialization)) {
      			acStr = "";
      			triples = this.indexedGraph.toArray();
      			
      			for (var prefix in this.env.prefixes){
		    		if (!(typeof this.env.prefixes[prefix] == "function")) {
			    		acStr += "@prefix " + prefix + ":<" + this.env.prefixes.get(prefix) + "> .\n"
					}
		    	}
      			
      			for(i = 0; i < triples.length; i++) {
		    		if ((triples[i].object.nodeType) && (triples[i].object.nodeType() != "BlankNode")){
		    			if (triples[i].object.value.indexOf("\n") == -1) {
		    				object = '"' + triples[i].object.value + '"';
		    			} else {
		    				object = '"""' + triples[i].object.value + '"""';
		    			}
		    			if(triples[i].object.datatype) {
		    				object += "^^" + triples[i].object.datatype;
		    			}
		    		} else {
		    			object = this.getTurtleIri(String(triples[i].object));
		    		}
		    		acStr += this.getTurtleIri(String(triples[i].subject)) + " "
							+ this.getTurtleIri(String(triples[i].predicate)) + " "
							+ object + " .\n";
		    	}
		    	
		    	this.attributes.context.parse({accessCondition: acStr});
      			
      		} else {
      			//since overwrite old graph could not be used in policy with context
		    	//(need to select triples not related to context)
		    	this.indexedGraph = new rdf.IndexedGraph;
		    	
		    	//add prefixes
		    	for (var prefix in this.env.prefixes){
		    		if (!(typeof this.env.prefixes[prefix] == "function")) {
			    		this.indexedGraph.add(rdf.environment.createTriple(
							"@prefix",
							prefix + ":",
							"<" + this.env.prefixes.get(prefix) + ">."
						));
					}
		    	}
      		
      			this.generateContext(this.attributes.context.get("ac"), {
	      			keywords: this.attributes.context.get("keywordsUsr"),
	      			keywordsDev: this.attributes.context.get("keywordsDev"),
	      			keywordsEnv:  this.attributes.context.get("keywordsEnv"),
	      			dateFormat: this.attributes.context.get("dateFormat")
	      		});
      		}
      		
      		return this.indexedGraph;
	    },
	    
	    generateContext: function (ac, acUtil) {
	    	var turtleStr = "";
	    	
	    	this.indexedGraph.add(rdf.environment.createTriple(
				"_:context",
				"rdf:type",
				"prissma:Context ."
			));
	    	
	    	if (ac.user) {
				console.log("user in ac");
				this.addUserAc(ac.user, turtleStr, acUtil.keywords);
		  	}
			if (ac.time) {
				console.log("time in ac");
				this.addTimeAc(ac, turtleStr, acUtil.dateFormat);
		  	}
		  	if (ac.outdoor) {
				console.log("outdoor in ac");
				this.addOutdoorAc(ac, turtleStr);
		  	}
		  	if (ac.env) {
		  		console.log("env in ac");
				this.addDevEnvAc(ac.env, turtleStr, acUtil.keywordsEnv, "env");
		  	}
		  	if (ac.dev) {
		  		console.log("dev in ac");
				this.addDevEnvAc(ac.dev, turtleStr, acUtil.keywordsDev, "dev");
		  	}
		  	
	    },
	    
	  };
	  
  })());
  
  return ContextModel;

});
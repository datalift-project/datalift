/**
 * graph model
 * has some useful function for graph management and context/query parsing that both policy and context model use.
 */
 
 define([
 'underscore',
 'backbone',
 'models/prissmaDimension',
 '/javascripts/lib/node-rdf-master/libBrowser/rdf.js',
 '/javascripts/lib/node-rdf-master/libBrowser/TurtleParser.js'], 
 function( _, Backbone, PrissmaDimModel, rdf, parser) {
  var GraphModel = PrissmaDimModel.extend({
  
  	  initEnv: function(options) {
  	  	var env = new rdf.RDFEnvironment();
  	  	
  	  	//TBD get prefixes from server so if change no problem!
  	  	env.setPrefix("s4ac", "http://ns.inria.fr/s4ac/v2#");
  	  	env.setPrefix("prissma", "http://ns.inria.fr/prissma/v2#");
    	env.setPrefix("", this.attributes.defaultPrefix);
    	env.setPrefix("rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    	//the following prefix not important in sparql case since in ask query (that has its own prefix declaration)
    	//needed in ldp (condition is a graph)
    	if (!this.attributes.sparqlAC) {
			env.setPrefix("hard", "http://www.w3.org/2007/uwa/context/hardware.owl#");
	    	env.setPrefix("dcn", "http://www.w3.org/2007/uwa/context/deliverycontext.owl#");
	    	env.setPrefix("foaf", "http://xmlns.com/foaf/0.1/");
	    	env.setPrefix("common", "http://www.w3.org/2007/uwa/context/common.owl#");
	    	env.setPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
	    	env.setPrefix("ao", "http://purl.org/ontology/ao/core#");
	    	env.setPrefix("soft", "http://www.w3.org/2007/uwa/context/software.owl#");
			env.setPrefix("geo", "http://www.w3.org/2003/01/geo/wgs84_pos#");
			env.setPrefix("rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			env.setPrefix("rel", "http://purl.org/vocab/relationship/");
			env.setPrefix("net", "http://www.w3.org/2007/uwa/context/network.owl#");
			env.setPrefix("web", "http://www.w3.org/2007/uwa/context/web.owl#");
			env.setPrefix("push", "http://www.w3.org/2007/uwa/context/push.owl#");
			env.setPrefix("java", "http://www.w3.org/2007/uwa/context/java.owl#");
			env.setPrefix("time", "http://www.w3.org/2006/time#");
			env.setPrefix("tl", "http://purl.org/NET/c4dm/timeline.owl#");
			env.setPrefix("xsd", "http://www.w3.org/2001/XMLSchema#");
		}
		return env;
		
 	  },
 	  
 	  initIndexGraph: function(options) {
	  	
	  	var g = new rdf.IndexedGraph;
	  	
	  	//triples for the prefixes
		g.add(rdf.environment.createTriple(
			"@prefix",
			":",
			"<" + this.env.prefixes.get("") + ">."
		));
		this.cnt++;
		//add prefix used only for policies
	  	g.add(rdf.environment.createTriple(
			"@prefix",
			"s4ac:",
			"<" + this.env.prefixes.get("s4ac") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"rdf:",
			"<" + this.env.prefixes.get("rdf") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"rdfs:",
			"<" + this.env.prefixes.get("rdfs") + ">."
		));
		this.cnt++;
		
		if (!this.attributes.sparqlAC) {
			this.addAccessConditionPrefixes(g);
		}  	
		
		return g;
	  },
	  
	  addAccessConditionPrefixes: function (g) {
	  	g.add(rdf.environment.createTriple(
			"@prefix",
			"prissma:",
			"<" + this.env.prefixes.get("prissma") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"hard:",
			"<" + this.env.prefixes.get("hard") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"dcn:",
			"<" + this.env.prefixes.get("dcn") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"foaf:",
			"<" + this.env.prefixes.get("foaf") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"common:",
			"<" + this.env.prefixes.get("common") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"xsd:",
			"<" + this.env.prefixes.get("xsd") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"ao:",
			"<" + this.env.prefixes.get("ao") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"soft:",
			"<" + this.env.prefixes.get("soft") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"geo:",
			"<" + this.env.prefixes.get("geo") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"rel:",
			"<" + this.env.prefixes.get("rel") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"net:",
			"<" + this.env.prefixes.get("net") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"web:",
			"<" + this.env.prefixes.get("web") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"push:",
			"<" + this.env.prefixes.get("push") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"java:",
			"<" + this.env.prefixes.get("java") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"time:",
			"<" + this.env.prefixes.get("time") + ">."
		));
		this.cnt++;
		g.add(rdf.environment.createTriple(
			"@prefix",
			"tl:",
			"<" + this.env.prefixes.get("tl") + ">."
		));
		this.cnt++;
		},	  
	  
	 normalizeTriple: function (triple) {
	  	var prefix, local, v, t, tmpObj;
  		t = {};
    	if(triple.subject) {
	    	if(triple.subject.charAt(0) == "<"){
	    		t.subject = triple.subject.replace("<","");
	    		t.subject = t.subject.replace(">","");
	    	} else {
		    	v = triple.subject.split(":");
		    	prefix = v[0];
		    	local = v[1];
		    	t.subject = this.env.prefixes.get(prefix) + local;
		    }
		} else {
			t.subject = triple.subject;
		}
		if(triple.object) {
			//eliminate the dot at the triple separator ('.', ',', ';' ) if any  
    		if ((triple.object[triple.object.length -1] == ".") 
    			|| (triple.object[triple.object.length -1] == ",")
    			|| (triple.object[triple.object.length -1] == ";")
    		) {
    			//use another wariables to avoid change the indexed graph (original triples are referenced)
    			tmpObj = triple.object.substring(0, triple.object.length - 1);
    		} else {
    			tmpObj = triple.object;
    		}
		    if(tmpObj.charAt(0) == "<"){
	    		t.object = tmpObj.replace("<", "");
	    		t.object = t.object.replace(">", "");
	    	} else if ((tmpObj.charAt(0) != "\"") && (tmpObj.charAt(0) != "'")) {
		    	v = tmpObj.split(":");
		    	prefix = v[0];
		    	local = v[1];
		    	t.object = this.env.prefixes.get(prefix) + local;
	    	} else {
	    		//eliminate the literals wrapper (", ', """) if any
	    		if (tmpObj.substr(0,3) == '"""') {
	    			//is a string on multiple lines (ie wrapped with """)
	    			t.object = tmpObj.substring(3, tmpObj.length - 3);
	    		} else if (tmpObj[0] == '"') {
	    			t.object = tmpObj.substring(1, tmpObj.length - 1);
	    		} else {
	    			t.object = tmpObj;
	    		}
	    	}
	    } else {
	    	t.object = triple.object;
	    }
    	//assumed with prefix or null
    	if (triple.predicate) {
	    	v = triple.predicate.split(":");
	    	prefix = v[0];
	    	local = v[1];
	    	t.predicate = this.env.prefixes.get(prefix) + local;
    	} else {
    		t.predicate = triple.predicate;
    	}
    	return t;
    	
	 },
	   
	//parse called also if no body in the response
	//reception raise error for http-code != 20x
	parse: function(response){
		console.log("PolicyModel.parse");
		
		console.log("response: " + response); //response is the body of the actual response
		//avoid to parse when resp is 204
		if(response) {
			//use a tmp graph so if there are errors (eg from editor) you can keep old graphs
			var tmpGraph = new rdf.TripletGraph;
			//use a new environment in the parser since all prefixes should be in the response
			var turtleParser = new parser.Turtle();
			turtleParser.parse(response, undefined, this.attributes.defaultBase, undefined, tmpGraph);
			
			//if parsing a new policy without errors create new graphs
			this.indexedGraph = new rdf.IndexedGraph;
			this.tripletGraph = tmpGraph;
			
			var turtleParser = new parser.Turtle();
			turtleParser.parse(response, undefined, this.attributes.defaultBase, undefined, this.indexedGraph);
			
			console.log("indexedGraph from parse:\n" + this.indexedGraph.toNT());
			//set the environment to the last parsed
			this.env =  turtleParser.environment;
			
			//put indexed graph in a human readable form (turtle syntax)
			this.indexedGraph = this.toTurtleGraph(this.indexedGraph);
			
			console.log("indexedGraph:\n" + this.indexedGraph.toNT());
			console.log("tripletGraph:\n" + this.tripletGraph.toNT());
			
			if (this.resetTriple2Idx) this.resetTriple2Idx();
			
		}
	},
    	    
    getTripletGraph: function () {
    	return this.tripletGraph;
    },
    
    getText: function() {
    	return this.indexedGraph.toNT();
    },
    
    getEnvironment: function() {
    	return this.env;
    },
    
    //to be overridden in extended model
    toTurtleGraph: function () {
    },
    
    getFullURI: function (uri) {
    	if(uri.charAt(0) == "<"){
    		uri = uri.replace("<","");
    		uri = uri.replace(">","");
    	} else {
	    	v = uri.split(":");
	    	prefix = v[0];
	    	local = v[1];
	    	if ((!this.env.prefixes.get(prefix)) || (this.env.prefixes.get(prefix) == "")) {
	    		//error or just let full URI?
	    		console.log("unknown prefix");
	    		return uri;
	    	}
	    	uri = this.env.prefixes.get(prefix) + local;
	    }
	    return uri;
    },
    
    
	addIsoString: function () {
				     
	    function pad(number) {
	      var r = String(number);
	      if ( r.length === 1 ) {
	        r = '0' + r;
	      }
	      return r;
	    }
	  
	    Date.prototype.toISOString = function() {
	      return this.getUTCFullYear()
	        + '-' + pad( this.getUTCMonth() + 1 )
	        + '-' + pad( this.getUTCDate() )
	        + 'T' + pad( this.getUTCHours() )
	        + ':' + pad( this.getUTCMinutes() )
	        + ':' + pad( this.getUTCSeconds() )
	        + '.' + String( (this.getUTCMilliseconds()/1000).toFixed(3) ).slice( 2, 5 )
	        + 'Z';
	    };
	   
	},
	
	getTurtleIri: function(uri) {
	    	var prefixFound = false;
	        for (var prefix in this.env.prefixes){
	        	if((typeof this.env.prefixes[prefix] != "function") && (uri.search(this.env.prefixes.get(prefix)) != -1)) {
		      		uri = uri.replace(this.env.prefixes.get(prefix),prefix + ":");
		      		prefixFound = true;
		      		break;
		      	}
	       }
	       if((prefixFound == false) && (uri.search("_:") != 0)) {
	      	uri = "<" + uri + ">";
	       }
	       
	       return uri;  
	},
	
    remove: function(triple) {
    	var i;
    	
    	console.log("policyModel.remove");
    	
    	triple = this.normalizeTriple(triple);
    	
    	var triples = this.tripletGraph.match(triple.subject, triple.predicate, triple.object);
    	for (i = 0; i < triples.length; i++) {
	    	this.tripletGraph.remove(triples[i]);
	    	//sometimes parser keep the dot at the end on triple -> try both 
	    	var el = this.triple2idx[triples[i].subject + triples[i].predicate + triples[i].object]
	    			|| this.triple2idx[triples[i].subject + triples[i].predicate + triples[i].object+ '.']
	    			|| this.triple2idx[triples[i].subject + triples[i].predicate + triples[i].object.value];
	    	var t = this.indexedGraph.get(el.idx);
	    	
	    	//to remove from index graph just set triple to ""
	    	t.subject = "";
	    	t.predicate = "";
	    	t.object = "";
		}
    	console.log("indexed:");
    	console.log(this.indexedGraph.toNT());
    	console.log("triplet:");
    	console.log(this.tripletGraph.toNT());
    },
    
    //to preserve the order as much as possible
    update: function(tripleOld, tripleNew) {
    	console.log("policyModel.update");
    	console.log("tripleOld: " + JSON.stringify(tripleOld));
    	console.log("tripleNew: " + JSON.stringify(tripleNew));
    	
    	var tripleOldNorm = this.normalizeTriple(tripleOld);
    	var tripleNewNorm = this.normalizeTriple(tripleNew);
    	
    	console.log("tripleOldNorm: " + JSON.stringify(tripleOldNorm));
    	console.log("tripleNewNorm: " + JSON.stringify(tripleNewNorm));
    	
    	var triples = this.tripletGraph.match(
    		tripleOldNorm.subject, 
    		tripleOldNorm.predicate, 
    		tripleOldNorm.object
    	);
    	this.tripletGraph.remove(triples[0]);
    	this.tripletGraph.add(this.env.createTriple(
			tripleNewNorm.subject,
			tripleNewNorm.predicate,
			tripleNewNorm.object
		));
		
		var el = this.triple2idx[triples[0].subject + triples[0].predicate + triples[0].object];
		var t = this.indexedGraph.get(el.idx);
		
		t.subject = tripleNew.subject;
		t.predicate = tripleNew.predicate;
		t.object = tripleNew.object + ".";
		
		//now index of triple in index graph is the same but adressed by another triple
		this.triple2idx[tripleNewNorm.subject + tripleNewNorm.predicate + tripleNewNorm.object] = {};
		this.triple2idx[tripleNewNorm.subject + tripleNewNorm.predicate + tripleNewNorm.object].idx = el.idx;
		//remove old key
		delete el.idx;
		delete el;
		
		console.log("t: " + JSON.stringify(t));
    	console.log("indexed:");
    	console.log(this.indexedGraph.toNT());
    	console.log("triplet:");
    	console.log(this.tripletGraph.toNT());
    },
    
    //return triples with absolute uris
    getTriple: function(triple) {
    	triple = this.normalizeTriple(triple);
    	return this.tripletGraph.match(triple.subject, triple.predicate, triple.object);
    },
	      
  });
  
  return GraphModel;

});
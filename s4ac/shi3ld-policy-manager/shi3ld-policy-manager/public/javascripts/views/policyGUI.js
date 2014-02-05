/**
 * GUI view of a policy
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'text!templates/policyGUI.html',
  '/javascripts/lib/node-rdf-master/libBrowser/rdf.js',
  '/javascripts/lib/node-rdf-master/libBrowser/TurtleParser.js', 
  'collections/keywords',
  'views/accessCond',
  'views/targets',
  'models/accessCond'
  ], function($, _, Backbone, policyGUITemplate, rdf, parser, KeywordCollection, AccessCondView, TargetListView, AccessCondModel){
	
	var PolicyGUIView = Backbone.View.extend((function() {
	
		/////////////////////////////////////////////////////
	    //              PRIVATE PROPERTIES
	    ////////////////////////////////////////////////////
	    
	    var generateHTML = function(g, env, acList, keywordsUsr, keywordsDev, keywordEnv) {
		  var privileges = {};
		  var acMatched, acs, ac, acsType; 
		  var target = null;
		  
		  var targetMatched = g.match(null, env.prefixes.get("s4ac") + 'appliesTo', null)
		  //either 0 or 1 triple with appliesTo property by a logic pt of view
		  //in editor more can be specified -> TBD manage that case!
		  if ((targetMatched) && (targetMatched.length != 0)) {
		  	target = targetMatched[0].object;
		  }
		  
		  var read = g.match(null, null, env.prefixes.get("s4ac") + 'Read');
		  //NB assumed just once if present (because otherwise 2 triples are the same)
		  //TBD verify if parse avoid triple duplication!
		  if((read) && (read[0])){
		  	console.log("read privilege found");
		  	privileges['read'] = true;
		  }
		  var update = g.match(null, null, env.prefixes.get("s4ac") + 'Update');
		  if((update) && (update[0])){
		  console.log("update privilege found");
		  	privileges['update'] = true;
		  }
		  var del = g.match(null, null, env.prefixes.get("s4ac") + 'Delete');
		  if((del) && (del[0])){
		  	console.log("delete privilege found");
		  	privileges['delete'] = true;
		  }
		  var create = g.match(null, null, env.prefixes.get("s4ac") + 'Create');
		  if((create) && (create[0])){
		  	console.log("create privilege found");
		  	privileges['create'] = true;
		  }
		  acMatched = g.match(null, null, env.prefixes.get("s4ac") + 'DisjunctiveAccessConditionSet');
		  if((acMatched) && (acMatched[0])){
		  	acsType = 'any';
		  }else{
		  	//acs considered always present
		  	//only exception is if policy created just by starting to write in editor
		  	//and user move to GUI when no triple about acs written. In this case added 
		  	//a conjunctive acs by default 
		  	acsType = "all"; 
		  	
		  }
		  console.log("s4ac: " + env.prefixes.get("s4ac"));
		  acMatched = g.match(null, env.prefixes.get("s4ac") + 'hasQueryAsk', null);
		  console.log("acMatched: " + JSON.stringify(acMatched));
		  
		  console.log("acsType: " + acsType);
		  console.log("privileges:" + JSON.stringify(privileges));
		  console.log("target: " + target);
		  
		  			
		  return {
		  	target: target,
		  	acsType: acsType,
		  	privileges: privileges
		  };
	    };
	    
		/////////////////////////////////////////////////////
	    //              PUBLIC PROPERTIES
	    ////////////////////////////////////////////////////
		
		return {
			
			events: {
		      'click #browseTargetGui': 'browseTargets',
		      'click #savePolicy': 'savePolicy',
		      'click #r, #c, #d, #u': 'togglePrivilege',
		      'click #addAC': 'addAc',
		      'change #acsType': 'setAcs',
		    },
			
			template: _.template(policyGUITemplate),
		    
		    initialize: function(options) {
		      var prefixFound = false;
		      
		      console.log("policyGui.init");
			  console.log("options: " + JSON.stringify(options));
			  this.model = options.model;
		      
		      //TBD CHECK ERROR
		      this.policyUri = this.model.getTriple({
		      	subject: null,
		      	predicate: "rdf:type",
		      	object: "s4ac:AccessPolicy"
		      });
		      
		      //transform the absolute uri in the prefixed uri or the uri according to turtle syntax ie <uri>
		      this.policyUri = this.model.getTurtleIri(this.policyUri[0].subject);
		      
		      this.listenTo(this.model, 'destroy', this.remove);
		      this.listenTo(this.model, 'addTargetOn', function() {
		      	//set event handler for policyGui after rendering target for the first time to ignore initial
	        	//addition from policy headers
		      	this.listenTo(this.model, 'addTarget',this.setTarget);
		      });
		      
		      this.acs = options.acs;
		      this.acs.on('add', this.addOne, this);
		      
		      this.acs.on('sync', function (){
		      	$('.ac-tab').on('show', $.proxy(function (e) {
		      		if(e.relatedTarget) {
		      			this.acs.at(e.relatedTarget.id).trigger('update');
		      			this.acs.at(e.relatedTarget.id).trigger('close');
		      		}
		      		this.acs.at(e.target.id).trigger('open');
		      	}, this));
		      }, this);
		      
		      this.acs.on('remove', function(options){
		      	var html;
		      	//remove active tab link
		      	this.$acList.children('.active').remove();
		      	this.$acTab.children('#tab-' + options.index).remove();
		      	if (this.acs.reject(function (model){return model.get("deleted")}).length == 0) {
		      		html = "<div id=\"noAcs\" class=\"hero-unit inset embossed\">" +
							"	<h2>No access conditions for the policy</h2>" +
							"</div>";
		      		this.$acList.before(html)
		      	} else {
		      		//open next access condition or the first if was the last
		      		var idx = (options.index + 1) %  this.acs.length;
		      		//find first el not deleted
		      		while( this.acs.at(idx).get("deleted") == true) {
		      			idx = (idx + 1) %  this.acs.length;
		      		}
		      		this.$acList.children().children('#' + idx).trigger('click');
		      	}
		      	
		      }, this);
		      
		      this.totAcs = options.totAcs; 
		      this.keywordsUsr = options.keywordsUsr;
		      this.keywordsDev = options.keywordsDev;
		      this.keywordsEnv = options.keywordsEnv
		      this.targetDetails = options.targetDetails
		    },
		
		    // Re-renders the titles of the todo item.
		    render: function(options) {
		      console.log("policyGui.render");
			  
			  var html = generateHTML(
			  	this.model.getTripletGraph(),
			  	this.model.getEnvironment(), 
			  	this.acList, 
			  	this.keywordsUsr, 
			  	this.keywordsDev,
			  	this.keywordsEnv
			  );
			  //console.log("acList: " + JSON.stringify(this.acList));
			  //console.log("options: " + JSON.stringify(options));
		      this.$el.html( this.template( html ) );
		      
		      
		      this.$targets = $('#targets');
		      //TBD MANAGE REMOVE OF TARGETS in POLICY CHANGE
		      if (this.targetView) {
		      	this.targetView.close();
		      }
		      this.stopListening(this.model, 'addTarget');
		      this.targetView = new TargetListView({
				backend: this.parentView.parentView.backend,
				model: this.model
			  });
		      this.$targets.html(this.targetView.render().el);
		      this.targetView.initPagination({
		      	targetDetails: this.targetDetails,
		      	model: this.model,
		      });
		      
		      this.$acList = $('#acList');
		      this.$acsType = $('#acsType');
		      this.$acTab = $('#ac-content');
		      this.oldAcsType = this.$acsType.val();
		     
		      return this;
		    },
		    
		    updateTargetDetails: function() {
		    	var triples = this.model.getTriple({
			      	subject: this.policyUri,
			      	predicate: "s4ac:appliesTo",
			      	object: null
			    });
			    
			    var targetDetails = new Array();
			    _.each(triples, function(triple){
			    	var t = _.findWhere(this.targetDetails, {uri: triple.object});
			    	if (t) {
			    		targetDetails.push(t);
			    	} else {
			    		//if target added by hand -> set uri and signal that triple cnt & label are not known
			    		targetDetails.push({uri: triple.object, tripleCnt: "unknown", label: ""});
			    	}
			    }, this);
			    var tmp = _.intersection(this.targetDetails, targetDetails)
			    if (tmp.length != this.targetDetails.length) {
			    	//arrays are equals
			    	this.model.set({modified: true})
			    }
			    this.targetDetails = targetDetails;
		    },
		    
		    browseTargets: function() {
		      //this.parentView.parentView.showTargets(this);
		      this.model.trigger('showTargets', this);
		    },
		    
		    setTarget: function(options) {
		      var triples;
		      
		      this.model.add({
			      subject: this.policyUri,
			      predicate: "s4ac:" + 'appliesTo',
			      object: "<" + options.target.get("uri") + ">"
		      });
		      
			  this.targetDetails.push(_.clone(options.target.attributes));
			  this.model.set({modified: true});
		    },
		    
		    togglePrivilege: function (e) {
		      console.log("policyGui.togglePrivilege");
		      if($(e.target).hasClass('btn-inverse')) {
		      	$(e.target).removeClass('btn-inverse');
		   	  	$(e.target).addClass('btn-info');
		   	  	this.model.add({
		   	  		subject: this.policyUri,
		   	  		predicate: "s4ac:" + 'hasAccessPrivilege',
		   	  		object: "s4ac:" + $(e.target).text()
		   	  	});
		   	  } else {
		   	    $(e.target).removeClass('btn-info');
		   	  	$(e.target).addClass('btn-inverse');
		   	  	this.model.remove({
		   	  		subject: this.policyUri,
		   	  		predicate: "s4ac:" + 'hasAccessPrivilege',
		   	  		object: "s4ac:" + $(e.target).text()
		   	  	});
		   	  }
		   	  
		   	  this.model.set({modified: true});
		   	  
		    },
		    
		    addOne : function ( item, options ) {
	     	  var html, activeClass = "";
		      var view = new AccessCondView({
		      	model: item,
		      	keywordsUsr: this.keywordsUsr,
	      		keywordsDev: this.keywordsDev,
	      		keywordsEnv: this.keywordsEnv,
		      });
		      
		      var txt;
		      if (item.get("label")) {
		      	item.get("label").length > 15 ? (txt = item.get("label").substring(0, 15) + "...") : (txt = item.get("label"));
		      } else {
		      	item.get("uri").length > 15 ? (txt = "..." + item.get("uri").substring(item.get("uri").length - 15, item.get("uri").length) ) : (txt = item.get("uri"));
		      }
		      var notDel = this.acs.reject(function (model){return model.get("deleted")}).length;
		      if(notDel == 1) {
		      	//is first element inserted in the collection
		      	activeClass="class=\"active\"";
		      	$('#noAcs').remove();
		      }
		      $('#acList').append(
		      	"<li " + activeClass + " data-toggle-tabac-" + (this.acs.length -1) + 
  				"=\"tooltip\" title=\"" + item.get("uri") + "\" ><a href=\"#tab-" + (this.acs.length -1) + 
  				"\" data-toggle=\"tab\" id=\"" + (this.acs.length -1) + "\" class=\"ac-tab\" onclick=\"return false;\">" + txt + "</a></li>"
		      );
		      $("[data-toggle-tabac-" + (this.acs.length -1) + "='tooltip']").tooltip();
		      $('#ac-content').append(
		      	view.render({
		      		//backend: this.backend,
		      		cnt: this.acs.length -1,
		      		notDel: notDel
		      	}).el
		      
		      );
		      view.$dimView = $('#dim-view' + (this.acs.length -1));
		      if((this.acs.length == 1) && (!this.acs.at(0).has("deleted"))) {
     		  	view.showDrivenFreeText();
     		  }
		      $("[data-toggle-dim='tooltip']").tooltip({placement: 'bottom'});
		      
		    },
		    
		   addAc: function() {
		    	 
		    	var triples = this.model.getTriple({
			      	subject: this.policyUri,
			      	predicate: "s4ac:hasAccessConditionSet",
			      	object: null
			    });
			    var acsUri = this.model.getTurtleIri(triples[0].object);
		    	this.model.add({
			      	subject:  acsUri,
			      	predicate: "s4ac:hasAccessCondition",
			      	object: ":AC-" + this.totAcs
		      	});
		    	this.model.add({
			      	subject:  ":AC-" + this.totAcs,
			      	predicate: "rdf:type",
			      	object: "s4ac:AccessCondition"
		      	});
		    	this.model.add({
			      	subject:  ":AC-" + this.totAcs,
			      	predicate: "rdfs:label",
			      	object: "\"AC " + this.totAcs + "\""
		      	});
		      	
		      	var model = new AccessCondModel({
    				dateFormat: this.model.get("dateFormat"),
    				timeFormat: this.model.get("timeFormat"),
    				defaultBase: this.model.get("defaultBase"),
    				uri: this.model.getFullURI(":AC-" + this.totAcs),
    				label: "AC " + this.totAcs,
    				ac: {},
    				modified: true,
    			});
		      	
		      	this.acs.add(model);
		      	this.totAcs++;
		    	this.model.set({modified: true});
		    	//NB actually the following operation you need to be sure addOne
		    	//function has finished to be executed (so you are sure element are in the DOM)
		    	$('.ac-tab').off('show');
		    	$('.ac-tab').on('show', $.proxy(function (e) {
		    		if (e.relatedTarget) {
		    			this.acs.at(e.relatedTarget.id).trigger('update');
		    			this.acs.at(e.relatedTarget.id).trigger('close');
		    		}
		      		this.acs.at(e.target.id).trigger('open');
		      	},this));
		      	if (this.acs.length > 1) {
		      		this.$acList.children().last().children().trigger('click');
		      	}
		      	
		    },
		    
		    writeConditions: function (options) {
		    	var  idx;
		    	
		    	//write changes that could be done in last view (since update of model 
		    	//triggered when dimension moving from)
		    	if (this.acs.length > 0) {
		    		idx = this.$acList.children('.active').children().attr('id');
		    		if (idx) {
		    			this.acs.at(idx).trigger('update');
		    		}
		    	}
    			if ((options) && (options.moveToEditor) && (options.moveToEditor == true)) {
    				idx = this.$acList.children('.active').children().attr('id');
		    		if (idx) {
    					this.acs.at(this.$acList.children('.active').children().attr('id')).trigger('close');
    				}
    			}	
		    	var acListModified = this.acs.where({modified: true});
		    	acListModified = _.reject(acListModified, function (model){return model.get("deleted")});
		    	
		    	_.each(acListModified, function (ac) {
		    		var tUri, query, triples;
		    		
		    		query = this.model.generateAskQuery(ac.get("ac"), {
	          			keywords: this.keywordsUsr,
	          			keywordsDev: this.keywordsDev,
	          			keywordsEnv: this.keywordsEnv,
	          			dateFormat: this.model.get("dateFormat")
	          		});
	          		tUri = this.model.getTurtleIri(ac.get("uri"))
	          		triples = this.model.getTriple({
				      	subject: tUri,
				      	predicate: "s4ac:hasQueryAsk",
				      	object: null
			        });
			        
			        if (triples.length == 0) {
			      	  this.model.add({
				      	subject: tUri,
				      	predicate: "s4ac:hasQueryAsk",
				      	object: "\"\"\"" + query + "\"\"\""
			      	  });
			      	} else {
			      	
				      this.model.update({
				      	subject: tUri,
				      	predicate: "s4ac:hasQueryAsk",
				      	//the query in the graphs is a literals -> convert it in a 
				      	//turtle string since interface wants turtle string 
				      	object:  "\"\"\"" + triples[0].object + "\"\"\""
				      },
				      {
				      	subject: tUri,
				      	predicate: "s4ac:hasQueryAsk",
				      	object: "\"\"\"" + query + "\"\"\""
				      });
		  			}
		  			
		  			this.model.set({modified: true});
		  			ac.set({modified: false});
		  			
		  		}, this);
		  		
		  		var toDelete = this.acs.where({deleted: true});
		  		
		  		_.each(toDelete, function (ac){
		  			var tUri;
		  			
		  			tUri = this.model.getTurtleIri(ac.get("uri"))
		  			this.model.remove({
				      	subject: tUri,
				      	predicate: null,
				      	object: null
			      	});
			      	this.model.remove({
				      	subject: null,
				      	predicate: null,
				      	object: tUri
			      	});
			      	
			      	ac.collection.remove(ac, {silent: true});
			      	this.model.set({modified: true});
		  		}, this);
		    	
		    },
		    
		    setAcs: function () {
		    	
		    	var triples = this.model.getTriple({
			      	subject: this.policyUri,
			      	predicate: "s4ac:hasAccessConditionSet",
			      	object: null
			    });
			    var acsUri = this.model.getTurtleIri(triples[0].object);
			    this.model.set({modified: true})
			    
			    if (this.$acsType.val() == "ANY") {			   
		    		this.model.update({
				      	subject: acsUri,
				      	predicate: "rdf:type",
				      	object: "s4ac:ConjunctiveAccessConditionSet"
				      },
				      {
				      	subject: acsUri,
				      	predicate: "rdf:type",
				      	object: "s4ac:DisjunctiveAccessConditionSet"
				    });
		    		return;
		    	}
		    	this.model.update({
			      	subject: acsUri,
			      	predicate: "rdf:type",
			      	object: "s4ac:DisjunctiveAccessConditionSet"
			      },
			      {
			      	subject: acsUri,
			      	predicate: "rdf:type",
			      	object: "s4ac:ConjunctiveAccessConditionSet"
			    });
		    },
		    
		    close: function () {
		    	this.acs.reset();
		    	this.acs.off();
		    	$('.ac-tab').off();
		    	if (this.targetView) {
			      this.targetView.close();
			    }
			    //if writing is async problems may raise 
			    $.cookie('acCnt', this.totAcs);
		    	this.remove();
		    }
		    
	};
	   
  })());

	return PolicyGUIView;

});
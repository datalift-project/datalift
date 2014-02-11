/**
 * policy panel view to manage a single policy
 * has references to policyGui and PolicyText view
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'views/tabbedTextGui',
  'views/policyGUI',
  'views/policyText',
  'models/policy',
  'flinteditor',
  'text!templates/policyPanel.html'
  ], function($, _, Backbone, TabbedTextGuiView, PolicyGUIView, PolicyTextView, PolicyModel, FlintEditor, policyPanelTemplate){

  var PolicyPanelView = TabbedTextGuiView.extend({

    events: {
      'click #editTab, #designTab': 'toggleView',
      'click #savePolicy': 'savePolicy',
    },
    
    // Add a single policy by creating a the view for it
    toggleView: function(e) {
    	console.log("appView.toggleView");
    	if(this.currTab == "design"){
    		this.currTab = "text";
    		if(this.viewGui){
    			if (this.acs.length > 0) {
    				this.viewGui.writeConditions({moveToEditor: true});
    			}
    			this.viewTxt.generatePolicyText({preventTextChangeEv: true});
    		}
    		return;
    	}
    	
    	this.currTab = "design";
    	try {
			this.model.parse(this.flintEd.getEditor().getCodeEditor().getValue());
		} catch (err) {
			this.showMsg(this.htmlSyntaxErr(err.message));
    		this.currTab = "text";
			//to prevent next events (support of IE from v.9)
			e.stopPropagation();
			return;
		}	
   		this.viewGui.updateTargetDetails();
   		this.viewGui.render({targetDetails: this.targetDetails});
   		this.acs.reset();
   		
   		this.model.trigger('loadACS',{
	    	collection: this.acs,
	    	keywordsUsr: this.parentView.usrVocPropCollection,
		   	keywordsDev: this.parentView.devVocPropCollection,
		   	keywordsEnv: this.parentView.envVocPropCollection,
	    });
    	
    	this.updatePolicyLabel();		      
    	
    },
    
    savePolicy: function(options) {
    	  var that = this;
    	  console.log("PolicyGuiView.savePolicy");
    	  if (this.viewGui) {
    	  	this.viewGui.updateTargetDetails();
    	  	this.viewGui.writeConditions();
    	  }
    	  if ((this.model.get("modified") != true)
    	  	&& (this.model.get("newModel") != true)
    	  ) {
    	  	this.showMsg(this.HTML_SAVE_SUCCESS);
    	  	return;
    	  }
    	  if(this.viewGui) {
    	  	if (
    	  		(this.model.get("newModel") != true)
    	  		&& (this.currTab == "design")
    	  		&& (this.acs.length > 0)) {
    	  		//avoided if is initial save (just after creation) or if saving from editor or if there are no access conditions
    	 		this.viewGui.writeConditions();
    	 	}
    	 	//update acCnt (also if there are no acs, cnt incremented each time an ac is added)
    	 	$.cookie('acCnt', this.viewGui.totAcs);
    	 	if (this.currTab == "text") {
    	 		try {
	    			this.model.parse(this.flintEd.getEditor().getCodeEditor().getValue());
	    		} catch (err) {
	    			this.showMsg(this.htmlSyntaxErr(err.message));
	    			return;
	    		}
    	 	}
    	 	Backbone.trigger('loading');
    	 	this.model.save(null, {
    			success: function (model, res) {
    				console.log("save operation succeeded");
    				that.updatePolicyLabel();
    				that.showMsg(that.HTML_SAVE_SUCCESS);
    				model.set({
    					newModel: false,
    					modified: false
    				});
    				model.trigger('addPolicy');
    				model.trigger(options.nextEvent, {
    					policyClosed: true,
    					toOpen: options.toOpen,
    					hide: options.hide,
    					policiesToTest: options.policiesToTest
    				});
    				if (options.closeOnSuccess){
    					that.close();
    					delete that.parentView.policyPanelView;
    				}
    			},
    			error: function (model, res) {
    				Backbone.trigger('loadingEnd');
    				console.log("save operation failed");
    				console.log("error code: " + res.status);
    				if (res.status == 409) {
    					console.log("policy uri already in use");
    					that.showAlert({
    						type: "nameConflict",
    						nextEvent: options.nextEvent,
    						toOpen: options.toOpen,
    						hide: options.hide,
    						policiesToTest: options.policiesToTest
    					});
    					return;    					
    				}
    				that.showMsg(that.HTML_SAVE_ERR);
    				//remember next event because if save fails at the
    				//end of wizard
    				that.errNextEvent = options.nextEvent
    			}
    		});
    	  } else {
    		console.log("no policies to save");
    	  }
    },
    
    policyChange: function(options) {
    	console.log("policyPanel.policyChange");
    	console.log("policy model: " + JSON.stringify(options.model));
    	
    	//if was keeping event due to errors in savings delete it
    	//and also free wizard views
    	delete this.errNextEvent;
    	
    	//stop listening on events on model of policy closed if not first policy
    	//in the session
    	if (this.model) {
    		this.model.trigger('closeWizard');
    		this.stopListening(this.model);
    	}
    	if (this.viewGui) {
    		this.viewGui.close();
    	}
    	this.model = options.model;
    	this.acs = new Backbone.Collection();
    	this.listenTo(this.model, 'closePolicy', this.closePolicy);
    	this.listenTo(this.model, 'savePolicy', this.savePolicy);
    	this.listenTo(this.model, 'overWritePolicy', this.overWritePolicy);
    	
    	this.updatePolicyLabel();
    	this.targetDetails = options.targetDetails;
    	this.viewGui = new PolicyGUIView({
    		model: this.model, 
    		acs: this.acs,
    		totAcs: $.cookie('acCnt'),
    		keywordsUsr: this.parentView.usrVocPropCollection,
	      	keywordsDev: this.parentView.devVocPropCollection,
	      	keywordsEnv: this.parentView.envVocPropCollection,
	      	targetDetails: this.targetDetails
    	});
    	this.viewGui.parentView = this;
    	//initially viewGui need to be attached to an existing element
    	this.$designTab.html(this.viewGui.el);
    	this.viewGui.render();
    	this.model.trigger('loadACS',{
    		collection: this.acs,
    		keywordsUsr: this.parentView.usrVocPropCollection,
	      	keywordsDev: this.parentView.devVocPropCollection,
	      	keywordsEnv: this.parentView.envVocPropCollection,
    	});
    	
    	//not remove viewTxt since directly hold in tab div
    	//and in addition flint initialized with policyPanel
    	//so remove only when removing policy panel 
		this.viewTxt = new PolicyTextView({model: this.model});
		this.viewTxt.parentView = this;
		this.viewTxt.generatePolicyText();
    },
    
    render: function() {
    	this.$el.html( this.template({type: "policy"}) );
		return this;
    },
    
    updatePolicyLabel: function(options) {
    	var policyUri, triples;
    	
    	console.log("policyPanel.render");
		
		triples = this.model.getTriple({
	     	subject: null,
	     	predicate: "rdf:type",
	     	object: "s4ac:AccessPolicy"
	    });
	    if(triples.length == 0) {
	  		//policy has no uri (when saving give a name)
	  		return;
	  	}
	  	policyUri =  triples[0].subject;
		triples = this.model.getTriple({
	     	subject: this.model.getTurtleIri(policyUri),
	     	predicate: "rdfs:label",
	     	object: null
	    });
	    
	    if(triples.length != 0) {
	    	//use the label if any
	  		this.$name.text(triples[0].object);
	  	} else {
	  		this.$name.text(policyUri);
	  	}
		
    },
    
    overWritePolicy: function (options) {
    	var that = this;
        console.log("policyPanel.overwrite");
        //so raise a put rq instead of a post rq
    	this.model.set({newModel: false});
    	Backbone.trigger('loading');
    	this.model.save( null, {
			success: function (model, res) {
				console.log("save operation succeeded");
				model.set({modified: false});
				//if policy to be overwritten was not saved before
				//of error and now raise previously missed event
				if ((that.errNextEvent) && (!options.nextEvent)) {
					options.nextEvent = that.errNextEvent;
					delete that.errNextEvent;
				}
				model.trigger(options.nextEvent, {
					policyClosed: true,
					toOpen: options.toOpen,
					hide: options.hide
				});
				that.showMsg(that.HTML_SAVE_SUCCESS);
			},
			error: function (model, res) {
				console.log("save operation failed");
				console.log("error code: " + res.status);
				that.showMsg(that.HTML_SAVE_ERR);				
			}
    	});
    },
    
    closePolicy: function (options) {
    	//to close the policy without save
    	if (options.policyClosed) {
    		delete this.parentView.policyPanelView;
    		this.close();
    		return;
    	}
    	//write conditions so that last changes are in the model
    	if(this.viewGui) {
    		this.viewGui.writeConditions();
    		this.viewGui.updateTargetDetails();
    	}
 		if ((this.model.get("modified")) || (this.model.get("newModel"))) {
 			this.showAlert({
 				type: "saveAlert",
 				nextEvent: options.nextEvent,
 				toOpen: options.toOpen,
 				closeOnSuccess: true,
 				hide: options.hide,
 				policiesToTest: options.policiesToTest
 			});
 			return;
 		}
 		delete this.parentView.policyPanelView;
 		this.close();
 		//if already saved (remote and local copy are synchronized)
 		this.model.trigger(options.nextEvent, {
 			policyClosed: true,
 			toOpen: options.toOpen,
 			policiesToTest: options.policiesToTest
 		});
    },
	
  });
  
  return PolicyPanelView;
  
});
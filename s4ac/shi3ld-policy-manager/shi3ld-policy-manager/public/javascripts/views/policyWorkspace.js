/**
 * View to manage policies
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'models/policy',
  'views/policyCollectionManagement',
  'views/wizardController',
  'views/policyPanel',
  'views/nameConflict',
  'jqcookies',
  'bootstrap'
  ], function(
  	$,
  	_, 
  	Backbone, 
  	PolicyModel, 
  	PolicyCollectionManagementView,
  	WizardControllerView, 
  	PolicyPanelView, 
  	NameConflictView
  ) {

  var PolicyWorkspaceView = PolicyCollectionManagementView.extend({
  
  	type: 'workspace',
  	
  	initialize: function(options) {
    	this.MSG_TIMEOUT = options.MSG_TIMEOUT
    	
        this.backend = $.cookie('backend');
	    this.accessConditionType = $.cookie('accessConditionType');
	    this.defaultPrefix = $.cookie('defaultPrefix');
	    this.defaultBase = $.cookie('defaultBase');
	    this.dateFormat	= $.cookie('dateFormat');
	    this.timeFormat	= $.cookie('timeFormat');
	    
	    this.usrVocPropCollection = options.usrVocPropCollection
		this.devVocPropCollection = options.devVocPropCollection,
		this.envVocPropCollection = options.envVocPropCollection
	    
	    this.$spinner = $('#spinner');
	    this.wizardCtrlView = new WizardControllerView({parentView: this});
		_.bindAll(this, 'createPolicy', 'showAlert', 'notifyError');
				 	
	},
    
	showWizard: function (options) {
		if ((this.policyPanelView)) {
    		//options passed in wizard but without policyClosed, if 
    		//function called by event handler of new policy means called 
    		//by save policy dialog (ie user has already chosen to save or not)
    		if((!options) || (!options.policyClosed)) {
				this.model.trigger('closePolicy', {nextEvent: "newPolicy"});
				return;
			} 
		}
		this.wizardCtrlView.showWizard();
	},
	
	showAlert: function (options) {
		var model;
		if (options.model) {
			//in case of deletion of a policy model to destroy is in the options
			model = options.model;
		}else if (((options.targetsViewModel) && (!options.targetsViewModel.models))
			|| (options.cancelWizard)
		) {
			//the view is called for a target name clash and during wizard flow
			//(in wizard flow point to the wizardModel)
			model = this.wizardCtrlView.wizardModel;
		} else {
			//is called for a policy or for a target from policyGui (view of targets
			//when called from policyGui is still in a modal(than wizard = true) but
			//his model is the collection of targets of the policy instead of the wizard model)
			model = this.model
		}
    	//propagate nextEvent on alertView
    	//since may be alrtView call again policyPanelView (to save)
    	//or directly raise nextEvent (to close current and create/open next without save)
    	var view= new NameConflictView({
    		model: model,
    		nextEvent: options.nextEvent,
    		toOpen: options.toOpen,
    		type: options.type,
    		closeOnSuccess: options.closeOnSuccess
    	});
    	
    	view.parentView = this;
		view.render(options);
		 
		var $modalEl = $("#nameConflictModal");
		 
		$modalEl.html(view.el);
		$modalEl.modal();
    },
	
	createPolicy: function() {
		var sparlqAC;
		
    	console.log("AppView.createPolicy");
    	console.log('policyWizard: ' + JSON.stringify(this.wizardCtrlView.wizardModel));
    	console.log('name: ' + this.wizardCtrlView.wizardModel.get("name"));
	    console.log('accessConditionType: ' + this.accessConditionType);
    	
    	if(this.accessConditionType == "sparql") {
       		//this.wizardCtrlView.wizardModel.set({sparqlAC: true});
       		sparqlAC = true;
       	}
       	//if an access condition is defined pass the counter and update it
       	if (this.wizardCtrlView.wizardModel.get("ac")) {
       		var acCnt = $.cookie('acCnt');
       		this.wizardCtrlView.wizardModel.set({acCnt: acCnt++});
       		$.cookie('acCnt', acCnt);
       	}
    	console.log("defaultPrefix: " + this.defaultPrefix);
    	this.wizardCtrlView.wizardModel.set({defaultPrefix: this.defaultPrefix});
    	//hide polPanel to not open after wizard
    	//$('#policyPanel').hide();
    	this.initPolicyModel({
	   		policyWizard: this.wizardCtrlView.wizardModel,
	 		defaultBase: this.defaultBase,
	 		defaultPrefix: this.defaultPrefix,
 			newModel: true,
 			sparqlAC: sparqlAC
     	});
    	 
    	this.initPolicy({targets: this.wizardCtrlView.wizardModel.get("targets")});
    	
    	this.listenTo(this.model, 'sync', function(){this.$spinner.modal('hide');});
    	this.listenToOnce(this.model, 'closeWizard', function() {
    		//destroy the model and raise event to free wizard
    		this.wizardCtrlView.wizardModel.destroy();
    	});
    	this.listenTo(this.model, 'rename', this.wizardCtrlView.wizardView.showModal);
		      
    	this.policyPanelView.savePolicy({
    		nextEvent: 'closeWizard',
    	});
    },
		
	openPolicy: function (options) {
		if (this.policyPanelView) {
			//event chain to eventually save policy if one if open
			if((!options) || (!options.policyClosed)) {
				this.model.trigger('closePolicy', {
					nextEvent: "openPolicy",
					toOpen: options.toOpen
				});
				return;
			}
		}
		if(this.accessConditionType == "sparql") {
       		//this.wizardCtrlView.wizardModel.set({sparqlAC: true});
       		sparqlAC = true;
       	}
		this.initPolicyModel({
			uri: options.toOpen.get("uri"),
			defaultBase: this.defaultBase,
			defaultPrefix: this.defaultPrefix,
			dateFormat: this.dateFormat,
			timeFormat: this.timeFormat,
			sparqlAC: sparqlAC
		});
		
    	var that = this;
    	this.$spinner.modal('show');
    	this.model.fetch({
    		success: function () {
    			that.$spinner.modal('hide');
	          	that.initPolicy({targets: options.toOpen.get("targets")});
	        },
    		error: function (res) {
    			that.$spinner.modal('hide');
    			that.policyCollection.trigger('showError', {onePolicyLoadErr: true});
    		}
        });
    	   	
	},
	
	initPolicyModel: function (options) {
		var policyModel = new PolicyModel(options);
    	//stop listening on events on model of policy closed if not first policy
    	//in the session
    	if (this.model) {
    		this.stopListening(this.model);
    	}
    	this.model = policyModel;
	},
	
	initPolicy: function (options) {
				
		this.listenTo(this.model, 'newPolicy', this.showWizard);
    	this.listenTo(this.model, 'openPolicy', this.openPolicy);
    	this.listenTo(this.model, 'addGraph', this.wizardCtrlView.showNewGraphView);
    	
    	this.listenTo(this.model, 'addPolicy', function() {
    		this.policyCollection.trigger('addPolicy', this.model);
    	});
    	
    	this.listenTo(this.model, 'selectOldPolicy', function() {
    		this.policyCollection.trigger('selectOldPolicy', this.model);
    	});
    	
    	this.listenTo(this.model, 'saveAlert', this.showAlert);
    	
    	this.listenTo(this.model, 'showTargets', function (view) {
    		this.wizardCtrlView.showTargets(view);
    	});
    	
    	this.listenTo(this.model, 'policyTest', function(options) {
    		this.policyCollection.trigger('policyTest', options);
    	});
    	
    	this.policyPanelView = new PolicyPanelView({MSG_TIMEOUT: this.MSG_TIMEOUT});
    	this.$policyPanel.html(this.policyPanelView.render().el);
    	this.policyPanelView.initFlint({bigTextArea: true});
    	this.policyPanelView.parentView = this;	    	  	
    	
    	
    	this.policyPanelView.policyChange({
    		model: this.model,
    		targetDetails: options.targets,
    	});
	},
	
	deletePolicy: function(model, collection, options) {
		if ((!options) || (options.deletePolicy != true)) {
			//is raise from paginator to change page -> no operation done
			return;
		}
		var uri = encodeURIComponent(model.get("uri"));
		if ((this.model) && (this.model.id == uri)) {
    		this.stopListening(this.model);
    		this.model.destroy();
    		delete this.model;
    		if(this.policyPanelView) {
    			this.policyPanelView.close();
    			delete this.policyPanelView;
    		}
    		return;
    	}
    	var DelModel = Backbone.Model.extend({
    		url: '/policies/?policy=' + uri,
    		isNew: function (){return false;} 
    	});
    	var that = this;
    	var m = new DelModel();
    	//err if fail
    	m.destroy({
    		error: function () {
    			that.policyCollection.trigger('showError', {deleteErr: true});
    		}
    	});
	},
	
	close: function() {
		if(this.policyPanelView) {
    			this.policyPanelView.close();
    			delete this.policyPanelView;
    	}
    	this.wizardCtrlView.remove();
    	this.policyListView.remove();
    	this.policyCollection.remove({silent: true});
	},
	
	notifyError: function(options){
		this.$spinner.modal('hide');
		$('i.gray-out').attr('style', 'opacity: 0.4; filter: alpha(opacity=40);background-color: #000;');
		
		this.undelegateEvents();
		this.parentView.undelegateEvents();
		this.stopListening(this.policyCollection, 'showWizard');
		
		this.policyCollection.trigger('showError');
	},
	
	/*togglePolicyVisibility: function (options) {
		if ((this.$policyPanel.attr('display') != "none") && ((!options) || (!options.policyClosed))) {
			this.model.trigger('closePolicy', {
				nextEvent: "togglePolicyVisibility",
				hide: true
			});
			return;
		}				
		this.$policyPanel.toggle();
	}*/
	
	closePolicy: function() {
		this.model.trigger('closePolicy', {});
	}
	
  });
  
  return PolicyWorkspaceView;
  
});
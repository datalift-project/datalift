/**
 * overall **AppView** is the top-level piece of UI.
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'collections/keywords',
  'collections/policyHeaders',
  'views/policyWorkspace',
  'views/policyTest',
  'jqcookies',
  'dateformat',
  'bootstrap'
  ], function(
  	$,
  	_, 
  	Backbone, 
  	KeywordCollection,
  	PolicyHeaderCollection,
  	PolicyWorkspaceView,
  	PolicyTestView) {

  var AppView = Backbone.View.extend({

    el: '#policyManagerApp',
    
    events: {
      'click #policyWorkspaceBarBtn': 'showPolicyWorkspace',
      'click #testPoliciesBarBtn': 'testPolicies',
    },
    
    initialize: function(options) {
        //LITERALS
    	//MSG_TIMEOUT: time after which user signalation messages about itreraction with server fade out (eg policy save) 
    	this.MSG_TIMEOUT = 5000;
    	this.URI_MAX_LEN = 40;
    	this.LABEL_MAX_LEN = 40;
    	
    	var that = this;
    	this.$spinner = $('#spinner');
    	this.$page = $('#page');
    	
	    _.bindAll( this, 'completeInit', 'initKeywords');
	    this.checkCookies(this.initKeywords, this.completeInit);
        
	},
	
	completeInit: function () {
		var that = this;
		//global event for showing loading bar since may be needed in many different views
		//to avoid to pass events from many views and models
		this.listenTo(Backbone, 'loading', function () { 
			that.$spinner.modal('show');
		});
		//To hide loading bar when no models shared with some view
		this.listenTo(Backbone, 'loadingEnd', function () { 
			that.$spinner.modal('hide');
		});
		$("[data-toggle='tooltip']").tooltip();
		
		this.policyWorkspaceView = new PolicyWorkspaceView({
			usrVocPropCollection: this.usrVocPropCollection,
			devVocPropCollection: this.devVocPropCollection,
			envVocPropCollection: this.envVocPropCollection,
			MSG_TIMEOUT: this.MSG_TIMEOUT
		});
		
		this.$page.html(this.policyWorkspaceView.render().el);
		this.collection = new PolicyHeaderCollection();
		this.listenTo(this.collection, 'policyTest', this.testPolicies);
		this.policyWorkspaceView.initPolicyCollection({collection: this.collection});
		this.policyWorkspaceView.parentView = this;
    },
	
	checkCookies: function(callback, callbackArgs) {
		if ((!$.cookie('backend'))
	    		|| (!$.cookie('accessConditionType'))
	    		|| (!$.cookie('defaultPrefix'))
	    		|| (!$.cookie('defaultBase'))
	    		|| (!$.cookie('dateFormat'))
	    		|| (!$.cookie('timeFormat'))
	    		|| (!$.cookie('acCnt'))
	    ) {
	    	this.notifyError();
	    	return;
	    }
	   	this.backend = $.cookie('backend');
    	if ((this.backend != "sparql") && (this.backend != "ldp")) {
    		this.notifyError();
    		return;
    	}
	    
	    this.accessConditionType = $.cookie('accessConditionType');
    	if ((this.accessConditionType != "sparql") && (this.accessConditionType != "rdf")) {
    		this.notifyError();
    		return;
    	}
    	
    	this.defaultPrefix = $.cookie('defaultPrefix');
    	this.defaultBase = $.cookie('defaultBase');
    	
    	this.dateFormat = $.cookie('dateFormat');
    	if ((this.dateFormat != "dd/mm/yyyy") && (this.dateFormat != "mm/dd/yyyy")) {
    		this.notifyError({startup: true});
    		return;
    	}
		this.timeFormat = $.cookie('timeFormat');
    	if ((this.timeFormat != "HH:MM")) {
    		this.notifyError();
    		return;
    	}
    	var acCnt = $.cookie('acCnt');
    	//Number constructor accept also float so first check if is a number then check that is ainteger number
    	if ((!acCnt) || (isNaN(Number(acCnt))== true) || (parseInt(acCnt) != Number(acCnt))) {
    		this.notifyError();
    		return;
    	}
    	
    	callback(callbackArgs);
	},
	
	initKeywords: function (callback) {
		var that = this;
		
		this.usrVocPropCollection = new KeywordCollection({suffix: "user-dim"});
		this.usrVocPropCollection.fetch({
          success: function () {
              console.log("user vocabularies ready");        	
          },
          error: function () {
          	that.notifyError();
          }
        });
        
		this.devVocPropCollection = new KeywordCollection({suffix: "dev-dim"});
		this.devVocPropCollection.fetch({
          success: function () {
              console.log("device vocabularies ready");        	
          },
          error: function () {
          	that.notifyError();
          }
        });
        
        this.envVocPropCollection = new KeywordCollection({suffix: "env-dim"});
		this.envVocPropCollection.fetch({
          success: function () {
              console.log("environment vocabularies ready");        	
          },
          error: function () {
          	that.notifyError();
          }
        });
        
        callback();
	},
	
	testPolicies: function (options) {
		//if already in policy test view do nothing
		if (!this.policyWorkspaceView) {
			return;
		}
		if (this.policyWorkspaceView.policyPanelView) {
    		//ask for saving the open policy if any
    		if((!options) || (!options.policyClosed)) {
				this.policyWorkspaceView.model.trigger('closePolicy', {
					nextEvent: "policyTest",
					policiesToTest: options.policiesToTest	
				});
				return;
			} 
		}
		this.policyWorkspaceView.close();
		delete this.policyWorkspaceView;
		this.policyTestView = new PolicyTestView({
			MSG_TIMEOUT: this.MSG_TIMEOUT,
			usrVocPropCollection: this.usrVocPropCollection,
			devVocPropCollection: this.devVocPropCollection,
			envVocPropCollection: this.envVocPropCollection
		});
		this.$page.html(this.policyTestView.render().el);
		this.policyTestView.initComponents(options);
		
		this.collection = new PolicyHeaderCollection();
		this.policyTestView.initPolicyCollection({collection: this.collection});
		
		$(window).resize();
	},
	
	showPolicyWorkspace: function() {
		//if already in policy workspace view do nothing
		if ((!this.policyTestView)) {
			return;
		}
		this.policyTestView.close();
		delete this.policyTestView;
		this.policyWorkspaceView = new PolicyWorkspaceView({
			MSG_TIMEOUT: this.MSG_TIMEOUT,
			usrVocPropCollection: this.usrVocPropCollection,
			devVocPropCollection: this.devVocPropCollection,
			envVocPropCollection: this.envVocPropCollection
		});
		this.$page.html(this.policyWorkspaceView.render().el);
		this.collection = new PolicyHeaderCollection();
		this.listenTo(this.collection, 'policyTest', this.testPolicies);
		this.policyWorkspaceView.initPolicyCollection({collection: this.collection});
	},
	
	notifyError: function(options){
		this.$spinner.modal('hide');
		$('i.gray-out').attr('style', 'opacity: 0.4; filter: alpha(opacity=40);background-color: #000;');
		this.undelegateEvents();
		this.stopListening();
		
		this.$page.html(
			"<div class=\"alert alert-error alert-text-center\">\n" +
				"<h2>A network connection problem occurred</h2>\n" +
				"<a class=\"btn btn-danger\" onclick=\"window.location.reload()\">Retry</a>" +
		  	"</div>\n"
		);
	},
	
  });
  
  return AppView;
  
});
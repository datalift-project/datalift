/**
 * Main view for testing policies 
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'models/context',
  'views/contextPanel',
  'views/policyCollectionManagement',
  'views/targets',
  'flinteditor',
  'jqcookies',
  'bootstrap'
  ], function(
  	$,
  	_, 
  	Backbone, 
  	ContextModel,
  	ContextPanelView,
  	PolicyCollectionManagementView,
  	TargetListView, 
  	FlintEditor) {

  var PolicyTestView = PolicyCollectionManagementView.extend({
  
  	type: 'test',
  	
  	events: {
  		'click #runTestBtn': 'runTest',
  		'click #initDSBtn': 'initDataset',
  		'click .togglePolicySelection': 'updatePoliciesToTest',
  	},
  	
  	initialize: function(options) {
  		//LITERALS
    	this.MSG_TIMEOUT = options.MSG_TIMEOUT
    	this.HTML_INIT_DATASET_ERR = "<div class=\"alert alert-error alert-text-center\">\n" +
        								"<strong>An error occurred while initializing the test dataset</strong>\n" +
        								"Retry later" +
        				  			"</div>\n";
    	this.HTML_RUN_TEST_ERR = "<div class=\"alert alert-error alert-text-center\">\n" +
        								"<strong>An error occurred while running the test</strong>\n" +
        								"Retry later" +
        				  			"</div>\n";
        this.HTML_NO_POLICIES_ERR = "<div class=\"alert alert-error alert-text-center\">\n" +
        								"Select at least a policy to test\n" +
        				  			"</div>\n";
        this.HTML_NO_TEST_DS_INIT_ERR = "<div class=\"alert alert-error alert-text-center\">\n" +
        									"Initialization of test dataset required\n" +
        				  				"</div>\n";
        this.HTML_TEST_FAILURE = "<div class=\"alert alert-error alert-text-center\">\n" +
        							"<strong>Access denied</strong>\n" +
        				  		 "</div>\n";
         this.HTML_TEST_SUCCESS = "<div class=\"alert alert-success alert-text-center\">\n" +
        							"<strong>Access granted</strong>\n" +
        				  		 "</div>\n";			  		 
       	
        this.backend = $.cookie('backend');
	    this.accessConditionType = $.cookie('accessConditionType');
	    this.defaultPrefix = $.cookie('defaultPrefix');
	    this.defaultBase = $.cookie('defaultBase');
	    this.dateFormat	= $.cookie('dateFormat');
	    this.timeFormat	= $.cookie('timeFormat');
	    
	    this.usrVocPropCollection = options.usrVocPropCollection
		this.devVocPropCollection = options.devVocPropCollection,
		this.envVocPropCollection = options.envVocPropCollection
	    
	    this.model = new ContextModel({
	    	defaultPrefix: this.defaultPrefix,
	    	defaultBase: this.defaultBase,
			dateFormat: this.dateFormat,
			timeFormat: this.timeFormat,
			keywordsUsr: this.usrVocPropCollection,
			keywordsDev: this.devVocPropCollection,
			keywordsEnv: this.envVocPropCollection,
		});
		
		this.policiesToTest = new Array();
		this.DSInitialized = false;
		
		_.bindAll(this, 'request', 'renderTestResult');
	},
	
	initComponents: function (options) {
		var  that = this;
    	
    	var flintConfig = {
			"interface" : {
				"toolbar" : true,
				"menu" : true
			},
			"namespaces" : [
			]
		}
					
		this.$contextPanel = $('#contextPanel');
		this.$testResult = $('#testResult');
		
		this.contextView = new  ContextPanelView({MSG_TIMEOUT: this.MSG_TIMEOUT});
		this.$contextPanel.html(this.contextView.render().el)
		this.contextView.parentView = this;
		this.contextView.initFlint();
		this.contextView.initTabs({model: this.model});
    	
    	this.flintEd = new FlintEditor("testQuery", "sparql/images", flintConfig);
		this.initialQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
	      					"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
	      					"PREFIX : <"+ this.defaultPrefix +">\n" +
	      					"SELECT * \nWHERE{\n?s ?p ?o\n}";
	    this.flintEd.getEditor().getCodeEditor().setValue(this.initialQuery);
    	
		this.$runTestBtn = $('#runTestBtn');
		this.$initDSBtn = $('#initDSBtn');
		
		if ((options) && (options.policiesToTest) && (options.policiesToTest.length > 0)) {
			this.policiesToTest = options.policiesToTest;
			this.initDataset();
		}
    },
    
    initDataset: function (options) { 
    	var that = this;
    	
    	if (this.policiesToTest.length == 0) {
    		this.notifyError(this.HTML_NO_POLICIES_ERR);
    		return;
    	}
    	
    	this.$el.find($('button')).button('loading')
		
    	this.request({
    		url: 'data',
    		data: {policiesToTest: this.policiesToTest},
    		success: function () {
    			that.DSInitialized = true;
    		},
    		error: function () {that.notifyError(that.HTML_INIT_DATASET_ERR);}
    	});
    },
    
    runTest: function () {
    	var ctx, triples, that = this;
    	
    	if (!this.DSInitialized) {
    		this.notifyError(this.HTML_NO_TEST_DS_INIT_ERR);
    		return;
    	}
    	
    	if(this.contextView.currTab == "design") {
    		this.model.get("context").trigger('update');
    		this.model.toTurtleGraph({serialization: true});
			this.contextView.viewTxt.generatePolicyText({preventTextChangeEv: true});	
    	} else {
    		try {
				this.model.parse(this.contextView.flintEd.getEditor().getCodeEditor().getValue());
				this.model.toTurtleGraph({serialization: true});
				triples = this.model.getTriple({
			      	subject: null,
			      	predicate: "rdf:type",
			      	object: "prissma:Context"
			    });
				if (triples.length == 0) {
					throw new Error("A triple {Context IRI} rdf:type prissma:Context is required")
				}
			} catch (err) {
				this.contextView.showMsg(this.contextView.htmlSyntaxErr(err.message));
	    		return;
			}
    	}
    	
    	this.request({
    		
    		url: 'context',
    		data: {
	      		query: this.flintEd.getEditor().getCodeEditor().getValue(),
	      		context: this.model.getText()
	      	},
    		success: function(model, res) {
    			that.renderTestResult({
    				res: res
    			});
    		},
    		error: function (model, res) {
    			if (res.status == 401) {
    				that.renderTestResult({err: 401});
    				return;
    			}
    			that.notifyError(that.HTML_RUN_TEST_ERR);
    		}
    	});
    	
    },
    
    updatePoliciesToTest: function (e) {
    	
    	if($(e.target).prop('checked') == true) {
    		this.policiesToTest.push($(e.target).closest('tr').attr('id'));
    	} else {
    		this.policiesToTest.splice(this.policiesToTest.indexOf($(e.target).closest('tr').attr('id')), 1);
    	}
    	
    },
    
	close: function() {
		
	},
	
	notifyError: function (html) {
	
		if (html == this.HTML_RUN_TEST_ERR) {
    		this.$runTestBtn.before(html);
    	}else if (html == this.HTML_NO_POLICIES_ERR) {
    		this.$initDSBtn.before(html);
    		this.$runTestBtn.before(html);
    	} else {
    		this.$runTestBtn.before(html);
    	}
    	
    	$(".alert").alert();
    	
    	setTimeout(function () {
			$(".alert-error").alert('close');
		}, this.MSG_TIMEOUT);
		
    },
    
    request: function (options) {
    	var that = this;
    	
    	var Model = Backbone.Model.extend({
      		url:'/policy-testing/' + options.url,
      	});
      	
      	var model = new Model(options.data);
      	model.save(null,{
      		success: function (model, res) {
      			that.$el.find($('button')).button('reset');
      			if (options.success) options.success(model, res);
      		},
      		error: function (model, res) {
      			//use button reset gives problems with add class (class added but then removed in a method callback)
      			if (res.status != 401) {
	      			that.$runTestBtn.html('Run test').addClass('disabled');
	      			that.$runTestBtn.attr('disabled', true);
	      			that.$initDSBtn.html('Initialize dataset').addClass('disabled');
	      			that.$initDSBtn.attr('disabled', true);
      			}
      			if (options.error) options.error(model, res);
      		}
      	});
    },
    
    renderTestResult: function (options) {
    	
    	if (options.err){
    		this.$testResult.html(this.HTML_TEST_FAILURE);
    	} else {
    		this.$testResult.html(this.HTML_TEST_SUCCESS);
    		this.$testResult.append("<p><strong>Named graphs affected:</strong> " + options.res.affectedNamedGraphsCnt + "</p>");
    	
	    	this.targetsView = new TargetListView({
				backend: this.backend,
				model: this.model,
				test: true,
			 });
		    this.$testResult.append(this.targetsView.render().el);
		    this.targetsView.initPagination({
		      	targetDetails: options.res.affectedNamedGraphs,
		      	model: this.model,
		    });
		}
    }
	
  });
  
  return PolicyTestView;
  
});
/**
 * view to manage targets (presentation in a paginated way)
 * responsible of the list of targets and of the pagination
 */
 define([
  'underscore',
  'backbone',
  'collections/targets',
  'collections/policyTargets',
  'text!templates/selectTarget.html',
  'views/targetView',
  'views/paginationView'
  ], function(_, Backbone, TargetCollection, PolicyTargetCollection, selectTemplate, TargetView, PaginationView){
  
	 var TargetListView = Backbone.View.extend({
	 
	 	  template: _.template(selectTemplate),
	 	  
	 	  events: {
	 	  	'click #selectBtn': 'closeSucc',
	 	  	'click #cancelBtn': 'close',
	 	  	'click .highlight': 'selectRow',
	 	  	'click #addGraph': 'addGraph',
	 	  	'keydown': 'closeOnEsc',
	 	  },
	 	  
	 	  initialize: function(options) {
		    this.backend = options.backend;
		    this.model = options.model;
		    this.test = options.test;
		    
		    //used to distinguish if the view is inside the modal or in the policyGUI view
		    //wizard not the best name since it's possible to have the browse target view
		    //(that is in a modal also from policyGUI) while for access condition views
		    //modals where used for wizard only (same name since same meaning but in this case
		    //a bit confusing)
		    this.wizard = options.wizard
		    this.$modal = $('#browseTargets');
		    this.targetOffset = 0;
		    
		    this.listenTo(this.model, 'overWriteTarget', function() {
		    	this.targets.trigger('overWriteTarget', {overwrite: true});
		    });
		  },
	 	  
	      render: function () {
	      	this.$el.html(this.template({
	            backend: this.backend,
	            wizard: this.wizard,
	            test: this.test
	        }));
			this.$errorAlert = $('#errorAlert');
	        return this;
	      },
	      
	      initPagination: function (options) {
	      	var that = this;
	      	
	      	var pageLoadError = function(collection, res) {
	      		Backbone.trigger('loadingEnd');
        		that.notifyError();
        	};
	      	if (this.wizard == true) {
	      		this.$pagination  = $('#pagination-target');
	      		this.targets = new TargetCollection();
	      	} else {
	      		this.$pagination  = $('#pagination-target-GUI');
	      		this.targets = new PolicyTargetCollection();
	      		//in case of adding a graph from policyGUI there is the target view (of targets of the policy) that open a target view
	      		//that has as model the collection of target of the first target view (of targets of the policy)
	      		this.targets.on('addGraph', this.addGraph, this);
	      	
	      	}
	        
	        this.targets.on('add', this.addOne, this);
	        this.targets.on('reset', this.addAll, this);
	      	
	        this.targets.on('sync', function noTargets() {
	        	Backbone.trigger('loadingEnd');
	      		var html = "<div id=\"noTarget\" class=\"hero-unit inset embossed\"><h2>No named graphs defined yet</h2></div>";
				if (this.targets.info().totalRecords == 0) {
					this.$pagination.before(html);
				}
				this.targets.off('sync', noTargets);
				this.$noTarget = $('#noTarget');
	      	}, this);
	      	
	      	//in both case listen to the collection to propagate events up to
	      	//appView
	      	this.listenTo(this.targets, 'saveAlert', function (options) {
        		this.model.trigger('saveAlert', options);
        	});
	      	
	      	if (this.wizard == true) {
		        this.targets.pager({
		        	error: pageLoadError
		        });
		        Backbone.trigger('loading');
		        this.paginationView = new PaginationView({
		        	collection: this.targets,
		        	paginated: this.$pagination,
		        	errorInPageLoad: pageLoadError
		        });
		        //listen for this event only if is the wizard view (target view in the policy deals with
	        	//targets of the policy only)
	        	this.listenTo(this.targets, 'newTarget', this.addTargetToPagedList);
		    } else {
		    	this.targets.reset(options.targetDetails);
		    	this.model = options.model;
		    	this.targets.on('remove', this.removeTarget, this);
		    }
	        
	        $("[data-toggle-seltarget='tooltip']").tooltip({placement: "bottom"});
	        $("[data-toggle-seltarget-sx='tooltip']").tooltip({placement: "left"});
	      },
	      
	      addOne : function ( item ) {
	      	  var txt;
	      	  
	      	  if (this.wizard != true) {
	      	  	var view = new TargetView({model:item});
	      	  	
	      	  	if (this.targetCnt == 0) {
			      	if (this.$noTarget) {
			      		this.$noTarget.remove();
			      	}
			      	this.backend == "sparql" ? txt = "Named Graphs" : txt = "Resources";
			      	html = "<table id=\"targetTable-GUI\" class=\"table table-striped table-hover table-condensed\">\n" +
		      	 			"<tr><th>#</th><th>" + txt + "</th><th>Number of triples</th><th></th></tr>\n" +
		      		   "</table>\n";
		      		this.$page = $('#targetPage-GUI');
		      		this.$page.find('#pagination-target-GUI').before(html);
		      		//now not paginated
		      		//this.targets.trigger('newPage');
		      	}
		      	
	        	this.$page.find('#targetTable-GUI').children().last().append(
			       view.render({
			      		targetCnt:  this.targetCnt,
			      		targetOffset: this.targetCnt,
			      		wizard: this.wizard,
			      		test: this.test
			      	}).el
			    );
			    this.targetCnt ++;
			    this.model.trigger('addTarget', {target: item})
			    return;
	      	  }
		      var view = new TargetView({model:item});
		      
		      var targetsInfo = this.targets.info();
		     // console.log("targetInfo: " + JSON.stringify(targetsInfo));
		      var perPage = parseInt(targetsInfo.perPage);
		      
		      //add the policy table if is the first item of the curr page
		      if (this.targetCnt == undefined) {
		      	if (this.$noTarget) {
		      		this.$noTarget.remove();
		      	}
		      	this.backend == "sparql" ? txt = "Named Graphs" : txt = "Resources";
		      	html = "<table id=\"targetTable\" class=\"table table-striped table-hover table-condensed\">\n" +
		      	 			"<tr><th>#</th><th>" + txt + "</th><th>Number of triples</th></tr>\n" +
		      		   "</table>\n";
		      	this.$page = $('#targetPage');
		      	this.$page.find('#pagination-target').before(html);//this.$el.html(html);
		      	this.targets.trigger('newPage');
		      } else if ((this.targets.totalRecords % perPage) == 0) {
		      		//if 1st item of the page raise event to render pagination view in order to update pages
		      		//used when added a new policy
		      		this.targets.trigger('newPage');
		      }
		      
		      var currentPage = parseInt(targetsInfo.currentPage);
		      
		      //this.prevPage used to properly manage the cnt (1st col of the table)
		      //so that when moving forward and backward in the pages enumeration is correct
		      //also if last page is not complete
		      if ((!this.prevPage) || (currentPage == this.prevPage)) {
		      	this.targetCnt =  perPage * (currentPage - 1) + this.targetOffset;
		      } else {
		      	this.targetCnt =  perPage * (currentPage - 1)
		      	//reset offset in case last page is not complete
		      	this.targetOffset = 0;
		      }
		      this.prevPage = currentPage;
		      var targetBase = perPage * (currentPage - 1);
		      
		      this.$page.find('#targetTable').children().last().append(
		      	view.render({
		      		targetCnt:  this.targetCnt,
		      		targetOffset: this.targetOffset,
		      		wizard: this.wizard
		      	}).el
		      );
		      
		      //not efficient (initialize all every time one is added)
		      $("[data-toggle-targetPage-"+ this.targetOffset +"='tooltip']").tooltip();
		      
		      this.targetOffset = (this.targetOffset + 1) % parseInt(targetsInfo.perPage);
		      //console.log("targetOffset: " + this.targetOffset);
		      
		  },
		  
		  //only for target view inside policyGui (when reset event occur)
		  addAll: function() {
	        this.targetCnt = 0;
	        this.$page = $('#targetPage-GUI');
	        this.$noTarget = $('#noTarget-GUI');
	        
	        this.targets.each(function (item) {
	        	this.addOne(item);
	        }, this);
	        $("[data-toggle-targetPage-"+ this.targetOffset +"='tooltip']").tooltip();
	        this.model.trigger('addTargetOn');
	      },
		  
		  notifyError: function () {
		  	//error only in case of wizard since pagination of policy targets is local
		  	$('#pagination-target').append("<div id=\"errorAlert\" class=\"alert alert-error\">An error occurred while loading targets. Retry later</div>");
		  },
	      
	      close: function () {
	        this.$modal.modal('hide');
	        this.targets.off();
	        this.targets.remove({silent: true});
	        this.remove();
	      },
	      
	      closeSucc: function () {
	      	console.log("target idx: " + this.selectedId);
	      	var target = this.targets.findWhere({uri: this.selectedId});
			console.log("target record selected: " + JSON.stringify(target));
		    if (!this.model.models) {
	      		//model is a model -> set policyWizard
	      		var targets = new Array();
				if (this.model.get("targets")) {
		      		var targets = targets.concat(this.model.get("targets"));
		      	}
		      	console.log("targets: " + JSON.stringify(this.model.get("targets")));
		      	if (!_.findWhere(targets, {uri: target.get("uri")})) {
		      		targets.push(_.clone(target.attributes));
		      		this.model.set({targets: targets});
		      	}	      	
		      	this.close();
		      	return;
	      	}
	      	//model is a collection -> add to the collection of targets of the policy
	      	//if not already present
	      	var v = target.get("uri")
	      	var v1 = this.model.findWhere({uri: target.get("uri")})
	      	if (!this.model.findWhere({uri: target.get("uri")})) {
	      		this.model.add(_.clone(target.attributes));
	      	}
	      	this.close();
	      },
	      
	      selectRow: function(e) {
	      	this.$selected = $(e.target).closest('tr');
	      	this.selectedId = this.$selected.attr('id');
	      	this.$selected.addClass('info').siblings().removeClass('info');
	      },
	      
	      addGraph: function (options) {
	      	if (this.wizard == true) {
	      		this.model.trigger('addGraph', {
	      			parentView: this,
	      			targetsViewModel: this.model
	      		});
	      	} else {
	      		this.model.trigger('addGraph', options);
	      	}
	      },
	      
	      addTargetToPagedList: function (target) {
	      	if (this.targets.info().totalPages > 0) {
		     	//reset counters in the table
		     	this.targetOffset = 0;
		     	delete this.prevPage
		     	this.targets.goTo(this.targets.info().lastPage, {
		     		error: this.paginationView.errorInPageLoading
	      	 	});
	      	 } else {
	      	 	this.targets.goTo(this.targets.info().currentPage, {
		     		error: this.paginationView.errorInPageLoading
	      	 	});
	      	 }
	      },
	      
	      //only for target view inside policyGuiView
	      removeTarget: function (model) {
	      	//target uniquely identifies the triple (its uri is unique)
	      	this.model.remove({
	   	  		subject: null,
	   	  		predicate: "s4ac:appliesTo",
	   	  		object: "<" + model.get("uri") + ">"
		   	});
		   	
		   	if (this.targets.length == 0) {
		   		
		   		html = "<div id=\"noTarget-GUI\" class=\"hero-unit inset embossed\"><h2>No ";
		   		this.backend == 'sparql' ? (html += "named graphs") : (html += "resources"); 
		   		html += " for the policy</h2></div>";
		   		
		   		this.$pagination.before(html);
		   		this.$noTarget = $('#noTarget-GUI');
		   		//removal only possible for targets of a policy
		   		this.$page.find('#targetTable-GUI').remove();
		   	}	
		   	this.targetCnt --;
	      },
	      
	      closeOnEsc: function (e) {
			if (e.keyCode == 27) {
				this.close();
			}
		  }
	      
	  });
	  
	  return TargetListView;
 });
 
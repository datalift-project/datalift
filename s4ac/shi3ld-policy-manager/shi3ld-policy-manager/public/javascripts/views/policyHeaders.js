/**
 * View to manage the list of policies and the pagination of the list
 */
define([
  'underscore',
  'backbone',
  'text!templates/policyHeaders.html',
  'views/policyHeader',
  'views/paginationView'
  ], function(_, Backbone, policyHeadersTemplate, PolicyHeaderView, PaginationView){
  
	 var PolicyListView = Backbone.View.extend({
	 	  template: _.template(policyHeadersTemplate),
	 	  
	 	  events: {
	 	  	'click .highlight': 'selectRow',
	 	  	'click .btn-danger': 'reloadPage',
	 	  	'click #newPolicyBarBtn': 'showWizard',
	 	  },
	 	  
	 	  initialize: function(options) {
		    this.backend = options.backend;
		    this.policyHeaderOffset = 0;
		    this.type = options.type;
		     
		    //LITERLAS
		    this.MSG_TIMEOUT = options.MSG_TIMEOUT;
		    this.HTML_NO_POLICY = "<div id=\"noPolicy\"><h1  class=\"hero-unit inset embossed\">No policies defined yet<h1></div>";
		    this.HTML_POLICY_PAGE = "<table id=\"policyPage\" class=\"table table-striped table-hover table-condensed\">\n" +
		      	 						"<tr><th>#</th><th>Name</th><th>Targets</th><th>Privileges</th><th></th>" +
		      	 						"</tr>\n</table>\n";
		    this.HTML_ERR = "<div class=\"alert alert-error alert-text-center\">\n" +
        						"<strong>An error occurred while loading policies</strong>\n" +
        						"<a class=\"btn btn-danger\">Retry</a>" +
        				  	"</div>\n";
		  },
	 	  
	      render: function (options) {
	      	this.$el.html(this.template({type: this.type}));
	      	return this;
	      },
	      
	      initPagination: function (options) {
	      	var that = this;
	      	var errorInPageLoading = function(collection, res) {
	      		Backbone.trigger('loadingEnd');
        		collection.trigger('error');
        	};
        	this.$pagination = $('#pagination')
		    
	      	this.policyHeaders = options.policyHeaders;
	        this.policyHeaders.on('add', this.addOne, this);
	        this.policyHeaders.pager({
	        	error: errorInPageLoading
	        });
	        
	        this.paginationView = new PaginationView({
	        	collection: this.policyHeaders,
	        	paginated: $('#pagination'),
	        	errorInPageLoading: errorInPageLoading
	        });
	        
	        this.listenTo(this.policyHeaders, 'addPolicy', this.addPolicy);
	        //if error is external but error showed in this view 
	        //(eg in case of user view error while loading vocabulary)
	        this.listenTo(this.policyHeaders, 'showError', this.showErrorMsg);
	        
	        this.listenTo(this.policyHeaders, 'selectOldPolicy', function () { 
				this.$oldPolicy.addClass('info').siblings().removeClass('info');	
			});
	        
	        this.policyHeaders.on('sync', function noPolicy() {
	      		if (this.policyHeaders.info().totalRecords == 0) {
					this.$el.append(this.HTML_NO_POLICY);
				}
				this.policyHeaders.off('sync', noPolicy);
				this.$noPolicy = $('#noPolicy');
	      	}, this);
	      	
	        this.policyHeaders.on('remove', function (model, collection, options) {
	        	var page;
	        	if ((!options) || (options.deletePolicy != true)) {
					//event is raised from paginator to change page -> don't delete any page
					return;
				}
	        	if((this.policyHeaders.length == 0) 
	        		&& (this.policyHeaders.info().totalPages == 1)
	        	) {
	        		//deletion of last policy
		    		this.$page.remove();
	    			delete this.$page;
	    			//empty the aside tag 
	    			this.paginationView.$el.html('');
	    			this.$pagination.after(this.HTML_NO_POLICY);
					this.$noPolicy = $('#noPolicy');
					//so no policy div is removed and table is added
					delete this.policyHeaderCnt;
		    	} else {
		    		this.policyHeaderOffset = 0;
				    delete this.prevPage
				    if(this.policyHeaders.length == 0) {
						//deletion of last policy of the last page
						page = this.policyHeaders.info().currentPage -1;
		      	 	} else {
		      	 		page = this.policyHeaders.info().currentPage;
		      	 	}
		    		this.policyHeaders.goTo(page, {
			     		error: this.paginationView.errorInPageLoading
		      	 	});
		    	}
		    	      		
	      	}, this);
	      	
	      	$('[data-toggle-headers=tooltip]').tooltip();
	      },
	      
	      //always triggered by event and not called by the view itself to avoid
	 	  //since sometimes need to showMsg for external error and sometimes for internal
	 	  //so it is avoided double renders of same msg. 
	      showErrorMsg: function(options) {
	      	if(options) {
	      		if(options.onePolicyLoadErr) {
	      			this.$el.append(
    					"<div class=\"alert alert-error fade in\">" +
			             "<button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>" +
			             "<strong>An error occured while loading the policy.</strong> Retry later" +
			            "</div>"
    				);
	      		} else {
	      			this.$el.append(
    					"<div class=\"alert alert-error fade in\">" +
			             "<button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>" +
			             "<strong>An error occured while deleting the policy.</strong> Retry later" +
			            "</div>"
    				);
	      		}
	      		setTimeout(function () {
					$(".alert-error").alert('close');
				}, this.MSG_TIMEOUT);
				return;
	      	}
	      	if((options) && (options.onePolicyLoadErr)) {
	      		this.$el.append(
    					"<div class=\"alert alert-error fade in\">" +
			             "<button type=\"button\" class=\"close\" data-dismiss=\"alert\">&times;</button>" +
			             "<strong>An error occured while loading the policy.</strong> Retry later" +
			            "</div>"
    				);
    				
	      		
	      	}
	      	if (this.$page) {
    			this.$page.remove();
    			delete this.$page;
    		}
    		if(this.$errorMsg) return;
    		this.$pagination.before(this.HTML_ERR);
    		this.$errorMsg = $('.alert-error');
	      },
	      
	      //this.prevPage used to managed properly the cnt int the policy list
	      //so that is reset when moving to previous page and restar from correct
	      //number considering the current page
	      addOne : function ( item ) {
	     	  var html;
		      var view = new PolicyHeaderView({model:item});
		      var policyHeadersInfo = this.policyHeaders.info();
		      //console.log("policyHeadersInfo: " + JSON.stringify(policyHeadersInfo));
		      var perPage = parseInt(policyHeadersInfo.perPage);
		      //add the policy table if is the first item of the curr page
		      if (this.policyHeaderCnt == undefined) {
		      	if (this.$noPolicy) {
		      		this.$noPolicy.remove();
		      	}
		      	this.$pagination.before(this.HTML_POLICY_PAGE);//$(this.HTML_POLICY_PAGE).insertAfter(this.$el.children('h5'));
		      	this.$page = $('#policyPage');
		      	//this.pagination.total
		      	this.policyHeaders.trigger('newPage')
		      } else if ((this.policyHeaders.totalRecords % perPage) == 1) {
		      		//if 1st item of the page raise event to render pagination view in order to update pages
		      		//used when added a new policy
		      		this.policyHeaders.trigger('newPage');
		      }
		      
		      var currentPage = parseInt(policyHeadersInfo.currentPage);
		      
		      if ((!this.prevPage) || (currentPage == this.prevPage)) {
		      	this.policyHeaderCnt =  perPage * (currentPage - 1) + this.policyHeaderOffset;
		      } else {
		      	this.policyHeaderCnt =  perPage * (currentPage - 1)
		      	//reset offset in case last page is not complete
		      	this.policyHeaderOffset = 0;
		      }
		      this.prevPage = currentPage;
		      var policyHeaderBase = perPage * (currentPage - 1);
		      
		      this.$page.append(
		      	view.render({
		      		policyHeaderCnt:  this.policyHeaderCnt,
		      		policyHeaderOffset: this.policyHeaderOffset,
		      		backend: this.backend,
		      		type: this.type
		      	}).el
		      	
		      );
		      
		      $("[data-toggle-policyheaderpage-" + this.policyHeaderOffset + "='tooltip']").tooltip().css("white-space","pre-wrap");
		      
		      this.policyHeaderOffset = (this.policyHeaderOffset + 1) % parseInt(policyHeadersInfo.perPage);
		      //console.log("policyHeaderOffset: " + this.policyHeaderOffset);
		      var targetsInfo = this.policyHeaders.info();
		       console.log("Info: " + JSON.stringify(targetsInfo));
		  },
		  
		  addPolicy: function (model) {
		  	 var that = this;
		  	 
		     if (this.policyHeaders.info().totalPages > 0) {
		     	//reset counters in the table
		     	this.policyHeaderOffset = 0;
		     	delete this.prevPage
		     	this.policyHeaders.goTo(this.policyHeaders.info().lastPage, {
		     		error: this.paginationView.errorInPageLoading
	      	 	});
	      	 } else {
	      	 	//only for first policy insert
	      	 	this.policyHeaders.goTo(this.policyHeaders.info().currentPage, {
		     		error: this.paginationView.errorInPageLoading
	      	 	});
	      	 }
		  },
	      
	      selectRow: function(e) {
	      	this.$oldPolicy = this.$selected;
	      	this.$selected = $(e.target).closest('tr');
	      	if((this.selectedId) && (this.selectedId == this.$selected.attr('id'))) {
	      		this.policyHeaders.trigger('closePolicy');
	      		delete this.selectedId
	      		return;
	      	}
	      	this.selectedId = this.$selected.attr('id');
	      	this.$selected.addClass('info').siblings().removeClass('info');	
	      	
	      	this.policyHeaders.trigger('openPolicy', { toOpen: this.policyHeaders.findWhere({uri: this.selectedId}) });
	      
	      },
	      
	      reloadPage: function () {
	      	window.location.reload();
	      },
	      
	      showWizard: function() {
	      	this.policyHeaders.trigger('showWizard');
	      },
	      
	  });
	  
	  return PolicyListView;
 });
 
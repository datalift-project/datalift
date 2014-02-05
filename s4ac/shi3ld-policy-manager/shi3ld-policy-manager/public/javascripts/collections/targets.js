/**
 * target paginated collection
 */
 define([
  'underscore',
  'backbone',
  'models/target',
  'paginator'
  ], function(_, Backbone, TargetModel){
 
	var TargetCollection = Backbone.Paginator.requestPager.extend({
		  paginator_core: {
		      // the type of the request (GET by default)
		      type: 'GET',
		
		      // the type of reply (jsonp by default)
		      dataType: 'json',
		
		      // the URL (or base URL) for the service
		      // if you want to have a more dynamic URL, you can make this a function
		      // that returns a string
		      url: '/targets?'
		  },
		    
		  paginator_ui: {
		      // the lowest page index your API allows to be accessed
		      firstPage: 1,
		
		      // which page should the paginator start from
		      // (also, the actual page the paginator is on)
		      currentPage: 1,
		
		      // how many items per page should be shown
		      perPage: 3,//20,
		
		      // a default number of total pages to query in case the API or
		      // service you are using does not support providing the total
		      // number of pages for us.
		      // 10 as a default in case your service doesn't return the total
		      totalPages: 10
		  },
		  
		  server_api: {
		      
		      // number of items to return per request/page
		      'limit': function() { return this.perPage },
		
		      // how many results the request should skip ahead to
		      // customize as needed. For the Netflix API, skipping ahead based on
		      // page * number of results per page was necessary.
		      'offset': function() { return (this.currentPage - 1) * this.perPage }		
		      
		  },
		  
	  	parse: function (response) {
	  		this.totalRecords = response.totalRecords;
	  		this.totalPages = response.totalPages;
			return response.data;
		}

		  
	});
	
	return TargetCollection;
 });
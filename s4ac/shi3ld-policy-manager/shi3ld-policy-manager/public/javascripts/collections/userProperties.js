/**
 * paginated collection of user properties
 */
 define([
  'underscore',
  'backbone',
  'models/keyword'
  ], function(_, Backbone, KeywordModel){
 
	var UserPropCollection = Backbone.Paginator.clientPager.extend({

        model: model,
		
		paginator_ui: {
	      // the lowest page index your API allows to be accessed
	      firstPage: 1,
	
	      // which page should the paginator start from
	      // (also, the actual page the paginator is on)
	      currentPage: 1,
	
	      // how many items per page should be shown
	      perPage: 10,
	
	      // a default number of total pages to query in case the API or
	      // service you are using does not support providing the total
	      // number of pages for us.
	      // 10 as a default in case your service doesn't return the total
	      totalPages: 10,
	
	      // The total number of pages to be shown as a pagination
	      // list is calculated by (pagesInRange * 2) + 1.
	      pagesInRange: 4
	    },
	return UserPropCollection;
 });

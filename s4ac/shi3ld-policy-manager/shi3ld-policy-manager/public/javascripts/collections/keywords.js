/**
 * user vocabularies keyword collection
 */
define([
  'underscore',
  'backbone',
  'models/keyword'
  ], function(_, Backbone, KeywordModel){
 
	var KeywordCollection = Backbone.Collection.extend({
	      initialize: function(options) {
		      this.url = '/vocabularies/' + options.suffix
		    },
	});
	
	return KeywordCollection;
 });
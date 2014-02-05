/**
 * keyword for user vocabularies model
 */
define(['underscore', 'backbone'], function(_, Backbone) {
	var Keyword = Backbone.Model.extend({
	      urlRoot: '/keywords'
	});
	    
	return Keyword;

});
/**
 * device vocabularies keyword collection
 */
define([
  'underscore',
  'backbone',
  'models/keywordDev'
  ], function(_, Backbone, KeywordDevModel){
 
	var KeywordDevCollection = Backbone.Collection.extend({
	      url: '/keywords'
	});
	
	return KeywordDevCollection;
 });
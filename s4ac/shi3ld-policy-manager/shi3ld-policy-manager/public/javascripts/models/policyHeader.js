/**
 * policy header model
 * summirize some info about a policy
 * to present in initial collection
 * in order to lazy download the actual policy content
 * only if needed (same idea of mail header in a mailbox)
 *
 * of course contain the policy uri (used to GET the policy
 * from server) other info (eg target) ok in sparql endpoint but in ldp
 * depends on web api.
 */
 
 define(['underscore', 'backbone'], function(_, Backbone) {
	var PolicyHeader = Backbone.Model.extend({
	      //urlRoot: '/policies'//diff uri than policy model but same root may cause problems?
	      url: function() {
	      	return '/policies/?policy=' + encodeURIComponent(this.attributes.uri);
	      }
	});
	    
	return PolicyHeader;

});

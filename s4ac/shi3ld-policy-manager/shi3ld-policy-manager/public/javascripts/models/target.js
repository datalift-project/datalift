/**
 * target model
 */
 define(['underscore', 'backbone'], function(_, Backbone) {
	var Target = Backbone.Model.extend({
	      url: function () { return '/targets/?target=' + encodeURIComponent(this.attributes.uri);},
	      
	      isNew: function () {
	        if (this.has("newModel")) {
	    		return this.get("newModel");
	    	}
	    	//if newModel not set considered new
	    	return true;
	      },
	});
	    
	return Target;

});
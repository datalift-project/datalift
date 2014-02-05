/**
 * view on a page of target paginated collection
 */
define([
  'underscore',
  'backbone',
  //'collections/targets',
  'text!templates/target.html'
  ], function(_, Backbone, targetTemplate){
  
	 var TargetView = Backbone.View.extend({
	 	  
	 	  tagName : 'tr',
	 	  events: {
	 	  	'click .target-del': 'removeTarget',
	 	  },
	 	  
	 	  template: _.template(targetTemplate),
	 	  
	 	  initialize: function(options) {
		    this.model = options.model;
		    this.listenTo(this.model, 'remove', this.remove);
		    this.listenTo(this.model, 'change', this.render);
		  },
	 	  
	      render: function (options) {
	      	//if render called with change to access
	      	//actual options you need the second parameter
	      	if(arguments.length == 2) {
	      		options = arguments[1];
	      	}
      	 	this.$el.html(this.template({
            	target: this.model,
            	targetCnt: options.targetCnt,
            	targetOffset: options.targetOffset,
            	wizard: options.wizard,
            	test: options.test
            }));
     		
     		this.$el.attr("id", this.model.get("uri"));
     		this.$el.addClass("highlight");
     		
	        return this;
	      },
	      
	      removeTarget: function () {
	      	this.model.destroy();
	      }
	      	      
	  });
	  
	  return TargetView;
 });
 
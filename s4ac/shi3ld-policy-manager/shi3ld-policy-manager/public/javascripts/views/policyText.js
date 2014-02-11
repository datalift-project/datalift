/**
 * Text view of a contex or a policy
 */
define([
  'jquery',
  'underscore',
  'backbone'
  ], function($, _, Backbone){
	
	var PolicyTextView = Backbone.View.extend({
		el: '#tab2',

	    initialize: function(options) {
	     	this.model = options.model;
	     	this.listenTo(Backbone, 'textChanged', this.signalModelChange);
	    	this.preventTextChangeEv = true;
	    },
	
	    render: function() {
		 $(window).resize();
		 $(".flint-editor-tab-selected").trigger("click");
	    },
	    
	    generatePolicyText: function(options) {
	    	console.log("policyText.generatePolicyText");
	    	//this.initialText = this.model.getText();
	    	this.parentView.flintEd.getEditor().getCodeEditor().setValue(this.model.getText());
	    	if ((options) && (options.preventTextChangeEv == true)) {
	    		this.preventTextChangeEv = true;
	    	}
	    },
	    
	    signalModelChange: function(){
	    	console.log("model.getText");
	    	console.log(this.model.getText());
	    	console.log("editor.getValue");
	    	console.log(this.parentView.flintEd.getEditor().getCodeEditor().getValue());
	    	if((this.$el.hasClass('active')) 
	    		&& (this.preventTextChangeEv == false)) {
	    		
	    		//textChange event on editor not due to loading text from model
	    		this.model.set({modified: true});
	    		
	    	}
	    	this.preventTextChangeEv = false;	
	    },
	    
	    close: function () {
	    	this.remove();
	    }
		
  });

  return PolicyTextView;

});
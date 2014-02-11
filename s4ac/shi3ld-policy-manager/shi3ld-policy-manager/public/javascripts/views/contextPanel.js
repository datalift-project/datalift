/**
 * view to manage the context definition
 * has references to accessCond and PolicyText view
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'views/tabbedTextGui',
  'views/accessCond',
  'views/policyText',
  'models/context',
  'flinteditor',
  'text!templates/policyPanel.html'
  ], function($, _, Backbone, TabbedTextGuiView, AccessCondView, PolicyTextView, ContextModel, FlintEditor, policyPanelTemplate) {

  var ContextPanelView = TabbedTextGuiView.extend({
	events: {
      'click #editTab, #designTab': 'toggleView',
    },
    
    toggleView: function(e) {
    
    	console.log("appView.toggleView");
    	if(this.currTab == "design"){
    		this.currTab = "text";
    		if(this.viewGui){
    			this.model.get("context").trigger('update');
    			this.model.get("context").trigger('close');	
    			this.model.toTurtleGraph({serialization: true});
    			this.viewTxt.generatePolicyText({preventTextChangeEv: true});
    		}
    		return;
    	}
    	
    	this.currTab = "design";
    	try {
			this.model.parse(this.flintEd.getEditor().getCodeEditor().getValue());
		} catch (err) {
			this.showMsg(this.htmlSyntaxErr(err.message));
    		this.currTab = "text";
			//to prevent next events (support of IE from v.9)
			e.stopPropagation();
			return;
		}
		this.model.get("context").trigger('open');
    },
    
    render: function() {
    	this.$el.html( this.template({type: "context"}) );
		return this;
    },
    
    initTabs: function (options) {
    	
    	this.model = options.model;
    	
    	this.viewGui = new AccessCondView({
	      	model: this.model.get("context"),
	      	keywordsUsr: this.parentView.usrVocPropCollection,
	  		keywordsDev: this.parentView.devVocPropCollection,
	  		keywordsEnv: this.parentView.envVocPropCollection,
	    });
	    this.viewGui.parentView = this;
	    
	    this.$designTab.append(
	      	this.viewGui.render({
	      		cnt: 0,
	      		notDel: 1,
	      		ctx: true
	      	}).el      
	    );
	    this.viewGui.$dimView = $('#dim-view0');
	    this.viewGui.showDrivenFreeText();
	    
	    $("[data-toggle-dim='tooltip']").tooltip({placement: 'bottom'});
	    
	    this.viewTxt = new PolicyTextView({model: this.model});
		this.viewTxt.parentView = this;
		this.viewTxt.generatePolicyText();
    }
	
  });
  
  return ContextPanelView;
  
});
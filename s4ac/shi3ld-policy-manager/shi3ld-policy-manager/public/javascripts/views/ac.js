/**
 * access condition view
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'text!templates/ac.html',
  'bootstrap'
  ], function($, _, Backbone, acTemplate, Bootstrap){
	
	var AcView = Backbone.View.extend((function() {
		var updateWizardModel = function (model){
			
			console.log("acView.updateWizardModel");
	    	console.log("model before: " + JSON.stringify(model));
	    	console.log("ac before: " + JSON.stringify(model.get("ac")));
			//signal that there are outdoor info in the wizard model
			//the value is the AC name
	   		var ac = model.get("ac") || {};
	   		delete ac.name;
	   		ac.name = $('#acName').val();
	   		console.log(JSON.stringify(ac));
		   	model.set({ac: ac});
		    console.log("creating policy");
	    	console.log("ac after: " + JSON.stringify(model.get("ac")));
	    	console.log("model after: " + JSON.stringify(model));  	
		};
		
		return {
			type: "blank",
		
		    // Cache the template function for the view.
		    template: _.template(acTemplate),
		
		    // The DOM events specific to this view.
		    events: {
		      'click #acModalNext': 'nextView',
		      'click #acModalFinish': 'createPolicy',
		      'click #acModalCancel': 'cancelPolicy',
		      'click #acModalPrev': 'prevView',
		      'click #acModalOk': 'addAc'
		    },
	
		    initialize: function(options) {
		      this.model = options.model;
		      this.modal = $('#acModal');
		      this.listenTo(this.model, 'destroy', this.remove);
		      
		    },
		
		    render: function(options) {
		      console.log("timeAc.render")
				console.log("options.isLast: " + options.isLastView);
		    	if(options.isLastView){
		      		this.isLastView = "true";
		      	}else{
		      		this.isLastView = "false";
		      	}
		      	console.log("isLast: " + this.isLastView);
		      this.$el.html(this.template({disabled: this.isLastView, wizard: options.wizard}));
		      return this;
		    },
		
			cancelPolicy: function() {
			  this.modal.modal('hide');
			  //delete model only if view open in wizard
			  if($('#acModalcancel').text() == "Close") {
		      	this.model.destroy();
		      	this.parentView.parentView.freeView();
		      } else {
		      	this.parentView.parentView.parentView.freeView();
		      }
		    },
		    
		    createPolicy: function() {
		    	console.log($('#acName').val());
		     	$('#errorTextAc').text("");
			  	if ((! $('#acName').val()) || ($('#acName').val().replace(/\s+/g, '') == "")) {
			  		console.log("name missing");
			  		$('#errorTextAc').text("Name is required");
			  		$('#txtAcCtrlGrp').addClass("error");
			  		return;
			  	}
			  	if (( $('#acName').val().match(/[^-a-zA-Z0-9.~_]/g) != null)) {
			  		console.log("name invalid");
			  		$('#errorTextAc').text("Name can contain only letters, digits, or one of the following symbol: {'.', '-', '_', '~'}");
			  		$('#txtAcCtrlGrp').addClass("error");
			  		return;
			  	}
	      	    updateWizardModel(this.model);
		      	this.modal.modal('hide'); 
			    this.parentView.createPolicy({policyWizard: this.model});
		        this.model.destroy();
		        this.parentView.parentView.freeView();
		        return;
		    },
					
			nextView: function(){
				
			  	$('#errorTextAc').text("");
			  	if ((!  $('#acName').val()) ||  ($('#acName').val().replace(/\s+/g, '') == "")) {
			  		console.log("Name is missing");
			  		$('#errorTextAc').text("Name is required");
			  		$('#txtAcCtrlGrp').addClass("error");
			  		return;
			  	}
			  	if (( $('#acName').val().match(/[^-a-zA-Z0-9.~_]/g) != null)) {
			  		console.log("Name invalid");
			  		$('#errorTextAc').text("Name can contain only letters, digits, or one of the following symbol: {'.', '-', '_', '~'}");
			  		$('#txtAcCtrlGrp').addClass("error");
			  		return;
			  	}
			  	updateWizardModel(this.model);
		  	   this.modal.modal('hide');
			   //pass its handler to parent view (appView) that creates new wizard view
			   //so next view know the previous one (to call render in case back btn pressed)
//			   switch(this.type){
//				case "time": 
//					this.parentView.showTimeView(this);
//					break;
//				case "outdoor":
//					this.parentView.showOutdoorView(this);
//					break;
//				case "dev":
//					this.parentView.showDevView(this);
//					break;
//				case "user":
//					this.parentView.showUserView(this);
//					break;
//				case "blank":
					this.parentView.parentView.showUserView('blank',this);
//					break;
//			   }
			   return;
			    //validator.showErrors();
			},
			
			prevView: function(){
				updateWizardModel(this.model);
				this.modal.modal('hide');
				//retrieve app view (next is enabled only in case of wizard for 
				//blank policy, ie view order is app->wizard->usr->time->outdoor)
				this.parentView.showModal();
			},
			
			showModal: function(){
				$('#errorTextAc').text("");
				var ac = this.model.get('ac');
				if ((ac) && (ac.name)) {
					$('#acName').val(ac.name);
					$('#txtAcCtrlGrp').removeClass("error");
				}
				this.modal.modal();
			},
			
			addAc: function () {
				$('#errorTextAc').text("");
			  	if ((!  $('#acName').val()) ||  ($('#acName').val().replace(/\s+/g, '') == "")) {
			  		console.log("Name is missing");
			  		$('#errorTextAc').text("Name is required");
			  		$('#txtAcCtrlGrp').addClass("error");
			  		return;
			  	}
			  	if (( $('#acName').val().match(/[^-a-zA-Z0-9.~_]/g) != null)) {
			  		console.log("Name invalid");
			  		$('#errorTextAc').text("Name can contain only letters, digits, or one of the following symbol: {'.', '-', '_', '~'}");
			  		$('#txtAcCtrlGrp').addClass("error");
			  		return;
			  	}
			  	this.modal.modal('hide');
				this.parentView.addAc({name: $('#acName').val()})
				this.parentView.parentView.parentView.freeView();
				this.remove();
			}
			 
	  };
	})());

	return AcView;

});
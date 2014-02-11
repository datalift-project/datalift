/**
 * New node file
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'text!templates/wizard.html',
  'views/wizard',
  'bootstrap'
  ], function($, _, Backbone, wizardTemplate, WizardView){
	
	var NewPolicyMainView = WizardView.extend((function() {
		
		/////////////////////////////////////////////////////
		//              PRIVATE PROPERTIES
		////////////////////////////////////////////////////
		
		var createWizardModel = function (model, ms) {
			var privileges = new Array();
			
			console.log("creating policy");
	      	console.log('name: ' + $('#inputName').val());
	      	console.log('target: ' + $('#target').val());
	      	//console.log('target: ' + view.targetUri);
	      	model.set({
	      		name: $('#inputName').val(),
	      		targets: ms.getSelectedItems()
	      	},{silent: true});
	      	if ($('#rModal').hasClass("active")) {
	      		privileges.push("Read");
	      	}
	      	if ($('#uModal').hasClass("active")) {
	      		privileges.push("Update");
	      	}
	      	if ($('#cModal').hasClass("active")) {
	      		privileges.push("Create");
	      	}
	      	if ($('#dModal').hasClass("active")) {
	      		privileges.push("Delete");
	      	}
	      	model.set({
	      		privileges: privileges
	      	});
	      	console.log("model wizard: " + JSON.stringify(model));	      	
		};
		
		/////////////////////////////////////////////////////
		//              PUBLIC PROPERTIES
		////////////////////////////////////////////////////
		
		return {
			type: "blank",
		
		    // Cache the template function for the view.
		    template: _.template(wizardTemplate),
		
		    events: {
		      'click #newPolicyNext': 'nextView',
		      'click #newPolicyFinish': 'createPolicy',
		      'click #newPolicyCancel': 'cancelPolicy',
		      'click #selectTargetBtn': 'selectTargetShow',
		      'keydown': 'closeOnEsc',
		    },
	
		    initialize: function(options) {
		      this.model = options.model;
		      this.backend = options.backend;
		      this.$modal = $('#newPolicy');
			  
		      this.listenTo(this.model, 'destroy', function () {
		      	if(this.$modal) {
		      		//if wizard is open (as happens while waiting for saving operation)
		      		//before removing the view properly close (hide) the modal
		      		this.$modal.modal('hide');
		      		this.$modal.off();
		      	}
		      	$('#inputName').off();
		      	this.remove();
		      });
		      
		      this.listenTo(this.model, 'change:targets', this.setTarget);
		      
		    },
		
		    render: function() {
		      this.$el.html(this.template({
			      backend: this.backend,
			      wizard: this.model
			  }));
		      return this;
		    },
		    
		    createPolicy: function() {
		     	$('#errorText').text("");
			  	if ((! $('#inputName').val()) || ($('#inputName').val().replace(/\s+/g, '') == "")) {
			  		console.log("name missing");
			  		$('#errorText').text("Name is required");
			  		$('#nameCtrlGrp').addClass("error");
			  		return;
			  	}
			  	/*if (($('#inputName').val().match(/[^-a-zA-Z0-9.~_]/g) != null)) {
			  		console.log("name invalid");
			  		$('#errorText').text("Named can contain only letters, digits, or one of the following symbol: {'.', '-', '_', '~'}");
			  		$('#nameCtrlGrp').addClass("error");
			  		return;
			  	}*/
	      	    createWizardModel(this.model, this.$ms);
		      	this.$modal.modal('hide'); 
		      	//create policy also destroy wizard model
		      	this.model.trigger('create');
			    
			    return;
		    },
		     
		    selectTargetShow: function() {
		      //set the model targets to selected input in case some were deleted
		      //so if added again consistency is guarantee (if not model is consistent
		      //but no the magic suggest input)
		      this.model.set({
		      	targets:this.$ms.getSelectedItems(),
		      },
		      {
		      	silent: true,
		      });
		      
		      this.parentView.showTargets(this);
			},
			
			initAutocomplete: function($modal) {
				var that = this;
				this.$ms = $( "#target" )
				.magicSuggest({
					maxSelection: null,
					hideTrigger: true,
				    width: 276,//$('#inputName').css('width'),
				    maxSelection: null,
				    data:[],
				    allowFreeEntries: false,
				    noSuggestionText: 'Click on the folder to select a ' + (this.backend == 'sparql' ? 'named graph' : 'resource') ,
				    emptyText: 'Click on the folder to select a ' + (this.backend == 'sparql' ? 'named graph' : 'resource') ,
				    valueField: 'uri',
				    selectionRenderer: function (item) {
				     	var txt;
				     	if (item.label) {
				     		item.label.length > 30 ? txt  = item.label.substr(0, 30) + '...' : txt = item.label;
				     		//iterm.name = item.label
				     	} else {
				     		item.uri.length > 20 ? txt  = '...' + item.uri.substr(item.uri.length -20, 20) : txt = item.uri;
				     		//item.name = item.uri
				     	}
				     	return txt;			     	
				    }
				});
				$(this.$ms).on('focus' , function(e) {
					this.input.attr('disabled', 'disabled');
					this.input.css('color', '#949293');
				});
				$(this.$ms).trigger('focus');  		
			},
			
			//show selected target label (if any, target uri otherwise) in the proper input
			setTarget: function(){
			  console.log("wizardMainView.setTarget");
			  var targets = this.model.get("targets");
			  this.$ms.addToSelection(targets[targets.length - 1]);
			},
					
			nextView: function(){
				
			  	$('#errorText').text("");
			  	if ((! $('#inputName').val()) || ($('#inputName').val().replace(/\s+/g, '') == "")) {
			  		console.log("name missing");
			  		$('#errorText').text("Name is required");
			  		$('#nameCtrlGrp').addClass("error");
			  		$('#inputName').trigger('focus');
			  		$('#inputName').on('blur', function (){
			  			if(($('#inputName').val()) && ($('#inputName').val().replace(/\s+/g, '') != "")) {
			  				$('#errorText').text("");
			  				$('#nameCtrlGrp').removeClass("error");
			  			}
			  		});
			  		return;
			  	}
			  	
		  	   createWizardModel(this.model, this.$ms);
			   this.$modal.modal('hide');
			   this.model.trigger('showDrivenFreeText', {
			   	isLast: "false",
			   	parentView: this,
			   	type: "user",
			   });
			   
			   return;
			},
			
	  };
	})());

	return NewPolicyMainView;

});
/**
 * outdoor place view
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'text!templates/outdoor.html',
  'views/wizard',
  'addresspicker'
  ], function($, _, Backbone, outdoorTemplate, WizardView){
	
	var OutdoorWizardView = WizardView.extend((function() {
	
		/////////////////////////////////////////////////////
		//              PRIVATE PROPERTIES
		////////////////////////////////////////////////////
		
		//to manage different flow by basing on selection
		//actually not used
		var isLastView;
		
		var setOutdoor = function (ac, view) {
			ac.outdoor = "outdoor"
			var position = view.$map.addresspicker( "marker").getPosition();
	   		ac.lat = position.lat();
    		ac.lon = position.lng();
    		ac.radius = view.$radius.val();
		};
		
		/////////////////////////////////////////////////////
		//              PUBLIC PROPERTIES
		////////////////////////////////////////////////////
		
		return {
			
		    // Cache the template function for the view.
		    template: _.template(outdoorTemplate),
		
		    // The DOM events specific this view.
		    events: {
		      'click #outdoorModalNext': 'nextView',
		      'click #outdoorModalCancel': 'cancelPolicy',
		      'click #outdoorModalFinish': 'createPolicy',
		      'click #outdoorModalPrev': 'prevView',
		      'click #outdoorModalOk': 'addDim',
		      'keydown': 'closeOnEsc',
		    },
			
			initialize: function(options) {
		      	this.model = options.model;
		      	
		      	this.$modal = $('#outdoorModal');
		      	
		      	this.wizard = options.wizard;
		      	
		      	if (options.wizard == true) {
			      	this.listenTo(this.model, 'destroy', function () {
				      	if(this.$modal) {
				      		//if wizard is open (as happens while waiting for saving operation)
				      		//before removing the view properly close (hide) the modal
				      		this.$modal.modal('hide');
				      		this.$modal.off();
				      	}
				      	this.$map.off();
				      	this.$radius.off();
				      	this.remove();
				    });
				} else {
		      		this.listenTo(this.model, 'update', this.updateModel);
		      	}
		    },
			
		    render: function(options) {
		    	console.log("outdoorView.render")
				console.log("options.isLast: " + options.isLastView);
		    	if(options.isLastView){
		      		this.isLastView = "true";
		      	}else{
		      		this.isLastView = "false";
		      	}
		      	console.log("isLast: " + this.isLastView);
		    	this.$el.html(this.template({
		    		disabled: isLastView,
		    		wizard: this.wizard,
		    		name: this.model.get("name")
		    	}));
		    	return this;
		    },
		
		    createPolicy: function() {
		    	this.updateWizardModel();
		    	console.log("model: " + JSON.stringify(this.model));
		    	this.$modal.modal('hide');
		    	console.log("outdoorView.createPolicy");
		    	console.log("isLast: " + this.isLastView);
		    	this.model.trigger('create');
		    },
			
			nextView: function(){
				this.updateWizardModel();
				this.$modal.modal('hide');
				//retrieve app view (next is enabled only in case of wizard for 
				//blank policy, ie view order is app->wizard->ac->usr->time->outdoor)
				//this.parentView.parentView.parentView.parentView.parentView.showDevView(this);
				this.model.trigger('showDrivenFreeText', {
					parentView: this,
					type: "env",
					isLast: true,
				});
			},
			
			initPickers: function(){
				var map, radius,that;
				
				if (this.wizard == true) {
					map = "#map_canvas-wiz";
					radius = "#radiusTxt-wiz";
					this.$map = $( "#addresspicker_map-wiz" );
					this.$radius = $("#radiusTxt-wiz");
				} else {
					map = "#map_canvas";
					radius = "#radiusTxt";
					this.$map = $( "#addresspicker_map" );
					this.$radius = $("#radiusTxt");
				}
				
				var addresspickerMap = this.$map.addresspicker({
					regionBias: "en",
					elements: {
					    map:     map,
					    radius: radius
				  }	
				});
			
				var gmarker = addresspickerMap.addresspicker( "marker");
				addresspickerMap.addresspicker( "updatePosition");
				
				that = this;
				$(map).one('click', function (e) {
					that.$map.addresspicker( "showDistance" );
					that.model.set({modified: true});
				});
				
				this.$map.on('change', function () {
					that.model.set({modified: true});
				});
				this.$radius.on('change', function () {
					that.model.set({modified: true});
				});
			},
			
			setPickersByModel : function() {
				var ac = this.model.get("ac");
				
				if (!ac.outdoor) {
					console.log("no location dimension in the condition");
					return;
				}
				//TDB signal if position from triple is broken
				//now if not radius used default of 1000 set in init pickers
				if (ac.radius) {
					//need to set initial value consistently for the radius input
					this.$radius.val(ac.radius);
					var gcircle = this.$map.addresspicker( "circle");
					var val = parseInt(ac.radius, 10);
					if (isNaN(val)) {
						console.log("radius is not an integer number");
						return;
					}
					gcircle.setRadius(val);
				}
				//change position only if both lat and lot available otherwise
				if ((ac.lat) && (ac.lon)) {
					var gmarker = this.$map.addresspicker( "marker");
					var latLng = new google.maps.LatLng(ac.lat, ac.lon)
					gmarker.setPosition(latLng);
					this.$map.addresspicker( "updatePosition");
					this.$map.addresspicker( "updateMap")
				}
				this.$map.addresspicker( "showDistance" );
			},
			
			updateWizardModel: function (){
				var ac;
				
				console.log("outdoorView.updateWizardModel");
		    	console.log("model before: " + JSON.stringify(this.model));
		    	console.log("ac before: " + JSON.stringify(this.model.get("ac")));
				//signal that there are outdoor info in the wizard model
				//the value is the AC name
		   		ac = this.model.get("ac") || {};
		   		delete ac.outdoor;
		   		delete ac.lat;
		   		delete ac.lon;
		   		delete ac.radius;
		   		if (this.$map.addresspicker( "marker").getVisible() == false) {
		   			console.log("outdoor skipped");   			
		      	} else {
		      		setOutdoor(ac, this);		   		
		    		this.model.set({ac: ac});
		    		console.log("creating policy");
		    		console.log("ac after: " + JSON.stringify(this.model.get("ac")));
		    		console.log("model after: " + JSON.stringify(this.model));
		      	}
			      	
			},
		
			  
		};
			
	})());

	return OutdoorWizardView;

});
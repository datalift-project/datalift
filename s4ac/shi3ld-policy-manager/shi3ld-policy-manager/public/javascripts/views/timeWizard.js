/**
 * New node file
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'text!templates/time.html',
  'views/wizard',
  'datepicker',
  'timepicker',
  'dateformat'
  ], function($, _, Backbone, timeTemplate, WizardView){
	
	var TimeWizardView = WizardView.extend((function() {
	
		/////////////////////////////////////////////////////
		//              PRIVATE PROPERTIES
		/////////////////////////////////////////////////////
		
		//to manage different flow by basing on selection
		//actually not used
		var isLastView;
		
		var setTime = function (ac, view) {
		
			ac.time = "time"
   			ac.dateFrom = view.$dp1Txt.val();
    		ac.dateTo = view.$dp2Txt.val();
	       	ac.timeFrom = view.$tp1Txt.val();
    		ac.timeTo = view.$tp2Txt.val();
    		
		};
		
		/////////////////////////////////////////////////////
		//              PUBLIC PROPERTIES
		////////////////////////////////////////////////////
		
		return {
			
		    template: _.template(timeTemplate),
		
		    events: {
		      'click #timeModalNext': 'nextView',
		      'click #timeModalCancel': 'cancelPolicy',
		      'click #timeModalFinish': 'createPolicy',
		      'click #timeModalPrev': 'prevView',
		      'click #timeModalOk': 'addDim',
		      'click #timeModalSetNow, #timeModalSetNow-wiz': 'setFieldsToNow',
		      'keydown': 'closeOnEsc',
		    },
			
			initialize: function(options) {
		      this.model = options.model;
		      
		      this.$modal = $('#timeModal');
		      this.wizard = options.wizard;
		      
		      if (options.wizard == true) {
			      this.listenTo(this.model, 'destroy', function () {
			      		if(this.$modal) {
			      		//if wizard is open (as happens while waiting for saving operation)
			      		//before removing the view properly close (hide) the modal
			      			this.$modal.modal('hide');
			      			this.$modal.off();
			      		}
			      		this.$dp1.off();
		      			this.$dp2.off();
		      			this.$tp1Txt.off();
		      			this.$tp2Txt.off();
			      		this.remove();
			      });
			  } else {
	      	  	this.listenTo(this.model, 'update', this.updateModel);
		      }
    		},
			
		    render: function(options) {
		    	console.log("timeView.render")
				console.log("options.isLast: " + options.isLastView);
		    	if(options.isLastView){
		      		this.isLastView = "true";
		      	}else{
		      		this.isLastView = "false";
		      	}
		      	console.log("isLast: " + this.isLastView);
		    	
		    	this.$el.html(this.template({
		    		disabled: isLastView,
		    		date: options.date, 
		    		dateFormat: this.model.get("dateFormat"),
		    		timeFormat: this.model.get("timeFormat"),
		    		wizard: this.wizard,
		    		name: this.model.get("name")
		    	}));
		    	
		    	return this;
		    },
		    
		    createPolicy: function() {
		    	this.updateWizardModel();
		    	console.log("model: " + JSON.stringify(this.model));
		    	this.$modal.modal('hide');
		    	console.log("timeView.createPolicy");
		    	console.log("isLast: " + this.isLastView);
		    	this.model.trigger('create');
		    },
		     
		    nextView: function(e){
				this.updateWizardModel();
				this.$modal.modal('hide');
				this.model.trigger('showOutdoor', 'notLast', this);
			},
			
			initPickers: function(options){
				if (this.wizard) {
				  	this.$dp1 = $('#dp1-wiz');
				  	this.$dp2 = $('#dp2-wiz');
				  	this.$dp1Txt = $('#dp1Txt-wiz');
				  	this.$dp2Txt = $('#dp2Txt-wiz'); 
				  	this.$tp1Txt = $('#tp1Txt-wiz');
				  	this.$tp2Txt = $('#tp2Txt-wiz');
				} else {
				  	this.$dp1 = $('#dp1');
				  	this.$dp2 = $('#dp2');
				  	this.$dp1Txt = $('#dp1Txt');
				  	this.$dp2Txt = $('#dp2Txt'); 
				  	this.$tp1Txt = $('#tp1Txt');
				  	this.$tp2Txt = $('#tp2Txt');
				}
				
				var nowTemp = new Date();
			    var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate(), 0, 0, 0, 0);
			    var checkin, checkout;
			    var that = this;    
				
				$("[data-toggle-timeModal='tooltip']").tooltip({placement: "right"});
			
			    checkin = this.$dp1.datepicker({
			      format: that.model.get('dateFormat'),
			      onRender: function(date) {
			        return date.valueOf() < now.valueOf() ? 'disabled' : '';
			      }
			    }).on('changeDate', function(ev) {
			      that.model.set({modified: true});
			      if (ev.date.valueOf() > checkout.date.valueOf()) {
			        var newDate = new Date(ev.date)
			        newDate.setDate(newDate.getDate());
			        checkout.setValue(newDate);
			      }
			      that.updateTimePickers();
			      checkin.hide();
			      that.$dp2[0].focus();
			    }).data('datepicker');
			    
			    checkout = this.$dp2.datepicker({
			      format: that.model.get('dateFormat'),
			      onRender: function(date) {
			        return date.valueOf() < checkin.date.valueOf() ? 'disabled' : '';
			      }
			    }).on('changeDate', function(ev) {
			      that.model.set({modified: true});
			      if ((!that.$dp1Txt.val()) || 
			      	(that.$dp1Txt.val().replace(/^\s+|\s+$/g,'') == "")
			      ) {
			      	checkin.setValue(checkout.date);
			      }
			      
			      that.updateTimePickers();
			      checkout.hide();
			      
			    }).data('datepicker');
			    
			    this.$tp1Txt.timepicker({
					minuteStep: 1,
					defaultTime: false,
					showMeridian: false
				}).on('changeTime.timepicker', function(e) {
					that.model.set({modified: true});
					that.updateDatePickers();
					if ((!that.$tp2Txt.val()) || 
			      		(that.$tp2Txt.val().replace(/^\s+|\s+$/g,'') == "")
			      	) {
			      		that.$tp2Txt.val(now.format(that.model.get('timeFrom')))
						that.$tp2Txt.timepicker('setTime', e.time.value);
					} else {
						if ((that.$dp1Txt.val() == that.$dp2Txt.val())
							&& (that.$tp2Txt.val() < e.time.value)
						) {
							that.$tp2Txt.val(e.time.value);
							that.$tp2Txt.timepicker('setTime', e.time.value);
						}
					}
				});
			
				this.$tp2Txt.timepicker({
					minuteStep: 1,
					defaultTime: false,
					showMeridian: false
				}).on('changeTime.timepicker', function(e) {
					that.model.set({modified: true});
					that.updateDatePickers();
					if ((!that.$tp1Txt.val()) || 
			      		(that.$tp1Txt.val().replace(/^\s+|\s+$/g,'') == "")
			      	) {
			      		that.$tp1Txt.val(e.time.value/*now.format(that.model.get('timeFrom'))*/)
						that.$tp1Txt.timepicker('setTime', e.time.value);
					} else {
						if ((that.$dp1Txt.val() == that.$dp2Txt.val())
							&& (that.$tp1Txt.val() > e.time.value)
						) {
							$(this).val(that.$tp1Txt.val());
							$(this).timepicker('setTime', that.$tp1Txt.val())
						}
					}
					
				});
			},
						
			setFieldsToNow: function() {
				var now = new Date();
				
				this.model.set({modified: true});
				this.$dp1.datepicker('setValue', now.format(this.model.get("dateFormat"))).datepicker('place');
				this.$tp1Txt.timepicker('setTime', now.format(this.model.get("timeFormat")));
				this.$dp2.datepicker('setValue', now.format(this.model.get("dateFormat"))).datepicker('place');
				this.$tp2Txt.timepicker('setTime', now.format(this.model.get("timeFormat")));
				
			},
			
			setPickersByModel : function() {
				var ac = this.model.get("ac");
				
				if (!ac.time) {
					console.log("no time dimension in the condition");
					return;
				}
				
				this.$dp1.datepicker('setValue', ac.dateFrom).datepicker('place');
				this.$tp1Txt.timepicker('setTime', ac.timeFrom);
				this.$dp2.datepicker('setValue', ac.dateTo).datepicker('place');
				this.$tp2Txt.timepicker('setTime', ac.timeTo);
				//reset model modified flag (since when generating events on pickers may be changed)
				this.model.set({modified: false});
			},
			
			updateTimePickers: function () {
				//refresh current time
				var nowTemp = new Date();
			    var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate(), 0, 0, 0, 0);
			    var dateFrom = this.$dp1Txt.val();
			    var dateTo = this.$dp2Txt.val();
			
				if ((!this.$tp1Txt.val()) || 
			      	(this.$tp1Txt.val().replace(/^\s+|\s+$/g,'') == "")
			    ) {
			      
			      	if ((!this.$tp2Txt.val()) || 
				      	(this.$tp2Txt.val().replace(/^\s+|\s+$/g,'') == "")
				    ) {
			      		//both timepickers not set
			      		
			      		//first set val to prevent raise other event
			      		this.$tp1Txt.val(now.format(this.model.get("timeFormat")));
			      		this.$tp2Txt.val(now.format(this.model.get("timeFormat")));
			      		this.$tp1Txt.timepicker('setTime', now.format(this.model.get("timeFormat")));
			      		this.$tp2Txt.timepicker('setTime', now.format(this.model.get("timeFormat")));
			      	
			      	} else {
			      	
			      		//only t1 not set
			      		this.$tp1Txt.val(now.format(this.model.get("timeFormat")));
			      		this.$tp1Txt.timepicker('setTime', now.format(this.$tp2Txt.val()));
			      	
			      	}
			      	
			      
			    }
			      
			    if ((!this.$tp2Txt.val()) || 
				   	(this.$tp2Txt.val().replace(/^\s+|\s+$/g,'') == "")
				) {
			    	//only t2 not set
			    	this.$tp2Txt.val(now.format(this.model.get("timeFormat")));
			    	this.$tp2Txt.timepicker('setTime', now.format(this.$tp1Txt.val()));
			    }
			    //if same date (and before was not) set timeTo = timeFrom if timeTo < timeFrom
	      		if ((dateFrom == dateTo) && (this.$tp2Txt.val() < this.$tp1Txt.val()) &&
	      			(dateFrom) && (dateTo) && (this.$tp2Txt.val()) && (this.$tp2Txt.val()) &&
	      			(dateFrom.replace(/^\s+|\s+$/g,'') != "") && (dateTo.replace(/^\s+|\s+$/g,'') != "")
	      			 && (this.$tp2Txt.val().replace(/^\s+|\s+$/g,'') != "") 
	      			 && (this.$tp2Txt.val().replace(/^\s+|\s+$/g,'') != "")
	      			
	      		) {
	      			this.$tp2Txt.val(this.$tp1Txt.val());
			    	this.$tp2Txt.timepicker('setTime', this.$tp1Txt.val());
	      		}
			    return null;
			},
			
			updateDatePickers: function () {
				var nowTemp = new Date();
			    var now = new Date(nowTemp.getFullYear(), nowTemp.getMonth(), nowTemp.getDate(), 0, 0, 0, 0);
			    var dateFrom = this.$dp1Txt.val();
			    var dateTo = this.$dp2Txt.val();
			    
			  	if ((!dateFrom) || 
			      	(dateFrom.replace(/^\s+|\s+$/g,'') == "") 
		      	) {
		      		this.$dp1Txt.val(now.format(this.model.get("dateFormat")))
		      		this.$dp1.datepicker('setValue', now).datepicker('place');
		      		//dp2 updated automatically only if old data was before that dp1 date
		      		if ((!this.$dp2Txt.val()) || 
			      		(this.$dp2Txt.val().replace(/^\s+|\s+$/g,'') == "") 
		      		) {
		      			this.$dp2Txt.val(now.format(this.model.get("dateFormat")))
		      			this.$dp2.datepicker('setValue', now).datepicker('place');
		      		}
		      	}
	      		if ((!dateTo) || 
		      		(dateTo.replace(/^\s+|\s+$/g,'') == "") 
	      		) {
	      			//var date = new Date (dateFormat($('#dp1Txt').val, default))
	      			this.$dp2Txt.val(now.format(this.model.get("dateFormat")))
	      			this.$dp2.datepicker('setValue', dateFrom).datepicker('place');
	      		}
	      		
		      	return null;
			},
			
			updateWizardModel: function (){
				var ac;
				console.log("timeView.updateWizardModel");
		    	console.log("model before: " + JSON.stringify(this.model));
		    	console.log("ac before: " + JSON.stringify(this.model.get("ac")));
				//signal that there are time info in the wizard model
				//the value is the AC name
		   		ac = this.model.get("ac") || {};
		   		delete ac.time;
		   		delete ac.dateFrom;
		   		delete ac.dateTo;
		   		delete ac.timeFrom;
		   		delete ac.timeTo;
		   		//field all empty or all filled
		   		if(this.$dp1Txt.val()){
			   		setTime(ac, this);
		    		this.model.set({ac: ac});
		    		console.log("creating policy");
		    		console.log("ac after: " + JSON.stringify(this.model.get("ac")));
		    		console.log("model after: " + JSON.stringify(this.model));
		      	}else{
		      		console.log("time skipped");
		      	}
			      	
			},
			    
		};
			
	})());

	return TimeWizardView;

});
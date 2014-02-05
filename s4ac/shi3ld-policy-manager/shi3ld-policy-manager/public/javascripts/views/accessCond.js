/**
 * access condition view
 */

 /**
 * view for a single policy header
 */
define([
  'underscore',
  'backbone',
  'text!templates/accessCondition.html',
  'views/timeWizard',
  'views/drivenFreeText',
  'views/outdoorWizard',
  //'views/devWizard'
  ], function(_, Backbone, accessConditionTemplate, TimeView, DrivenFreeTextView, OutdoorView/*, DevView*/){
  
	 var AccessCondView = Backbone.View.extend({
	 	  
	 	  events: {
		      'click #timeDim': 'showTime',
		      'click #userDim, #envDim, #devDim': 'showDrivenFreeText',
		      'click #outdoorDim': 'showOutdoor',
		      'click .ac-del': 'close',
		  },
	 	  
	 	  template: _.template(accessConditionTemplate),
	 	  
	 	  initialize: function(options) {
		    this.model = options.model;
		    this.keywordsUsr = options.keywordsUsr;
		    this.keywordsDev = options.keywordsDev;
		    this.keywordsEnv = options.keywordsEnv;
		    
		    this.listenTo(this.model, 'remove', this.remove);
		    this.listenTo(this.model, 'close', function () {
		    	if (this.currView) {
		    		this.currView.remove();
		    		delete this.currView;
		    	}
		    });
		    this.listenTo(this.model, 'update', function () {
		    	if (this.currView) {
			    	this.currView.updateWizardModel();
				}
		    });
		    this.listenTo(this.model, 'open', this.showDrivenFreeText);
		    
		  },
	 	  
	      render: function (options) {
      	 	this.$el.html(this.template({
      	 		acCnt: options.cnt,
      	 		ctx: options.ctx
      	 	}));
     		
     		this.$el.addClass('tab-pane');
     		this.$el.attr('id', 'tab-' + options.cnt)
     		if ((options.cnt == 0) || (options.notDel == 1)) {
     			this.$el.addClass('active');
     		}
     		return this;
	      },
	      
	      showTime: function () {
	      	
			if (this.currView) {
				//remove the current view in order to remove DOM element
				//and also the event handler set
				this.currView.updateWizardModel();
				this.currView.remove();
			}
			this.currView = new TimeView({
				model: this.model,
				wizard: false,
				dateFormat: this.model.get("dateFormat"),
				timeFormat: this.model.get("timeFormat")
			});
	    	
			this.currView.render({});
			this.$dimView.html(this.currView.el);
			this.currView.initPickers();
			this.currView.setPickersByModel();
			
			return false; 
		  },
		  
		  showDrivenFreeText: function (e) {
	      	var keywords, type;
			
			if (this.currView) {
				//remove the current view in order to remove DOM element
				//and also the event handler set
				this.currView.updateWizardModel();
				this.currView.remove();
			}
			if ((!e) || (e.target.id == 'userDim')){
				keywords = this.keywordsUsr;
				type = "user";
			} else if (e.target.id == 'envDim') {
				keywords = this.keywordsEnv;
				type = "env";
			} else {
				keywords = this.keywordsDev;
				type = "dev";
			}
			this.currView = new DrivenFreeTextView({
				model: this.model,
				wizard: false,
				keywords: keywords,
				type: type
			});
	    	
			this.currView.render({});
			this.$dimView.html(this.currView.el);
			this.currView.initAutocomplete({
				callback: this.currView.setAutocompleteByModel
			});
			
			return false; 
		  },
		  
		  showOutdoor: function() {
		  	
			if (this.currView) {
				//remove the current view in order to remove DOM element
				//and also the event handler set
				this.currView.updateWizardModel();
				this.currView.remove();
			}
			this.currView = new OutdoorView({
				model: this.model,
				wizard: false,
			});
	    	
			this.currView.render({});
			this.$dimView.html(this.currView.el);
			this.currView.initPickers();
			this.currView.setPickersByModel();
			
			return false; 
		  },
		  
		  close: function () {
		  	if (this.currView) {
	    		this.currView.remove();
	    	}
	    	this.model.set({deleted: true});
	    	var index = this.model.collection.indexOf(this.model);
	    	//simulate a remove event but actuall deletion will be on write
	    	//condition (since tabs managed by position in acs)
	    	this.model.collection.trigger('remove', {index: index});
		  }
		  
			      
	  });
	  
	  return AccessCondView;
 });
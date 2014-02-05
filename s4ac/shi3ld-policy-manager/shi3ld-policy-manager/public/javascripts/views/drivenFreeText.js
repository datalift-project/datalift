/**
 * View used to suggest properties to user by means of free text
 * used for user, device and environment
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'text!templates/drivenFreeText.html',
  'views/wizard',
  'suggest'
  ], function($, _, Backbone, drivenFreeTextTemplate, WizardView){
	
	var drivenFreeTextView = WizardView.extend((function() {
	
		/////////////////////////////////////////////////////
		//              PRIVATE PROPERTIES
		////////////////////////////////////////////////////
	
		var setAc = function (ac, items, view, type) {
			
			var i, keyword;
			
			ac[type] = {};
	   		ac[type].keywords = new Array();
	   		
	   		if (!items[items.length - 1].value) {
	   			view.$txtErr.text("A value for the last property is required");
  				view.$txtCtrl.addClass("error");
   				console.log("error: missing last condition");
   				delete ac[type];
   				return false; 
	   		}
	   		for(i = 0; i < items.length; i++) {
	   			keyword = _.omit(items[i], 'text', 'id');
	   			ac[type].keywords.push(keyword);
	   		}
	   		
	   		return true;
	   		
		};
		
		var toAbsURI = function (relUri, defaultBase){
			var protocol = relUri.split(":");
			protocol = protocol[0];
			if ((protocol) && (protocol != "") && (relUri.indexOf(":") != -1)) return relUri;
			return defaultBase + relUri;
		};
		
		var loadSuggestions = function (keywords, view) {
			var availableTags;
	      	console.log("keywords.fetch.success");
	        availableTags = new Array();
	      	_.each(keywords.models, function(keyword) {
	      		//generate the JSON obj of the magic suggest
	      		var tag = _.clone(keyword.attributes);
	      		//magic suggest need an id property
	      		tag.id = tag.uri;
	      		//used to generate text rendered in the magic suggest of each selected element	      		
	      		tag.text = tag.label;
	      		this.push(tag);
	      	}, availableTags);
	      	console.log("availableTags: " + availableTags);
	     	
	     	var width = view.$el.find($('.alert-info')).width() + 48;
			if (width  == 48) width = 615;
			var  ms = view.$txt
			.magicSuggest({
				//resultAsString: true,
				maxSelection: null,
				hideTrigger: true,
			    width: width,
			    maxSelection: null,
			    sortOrder: 'text',
			    displayField: 'text',
			    minChars: 2,
			    data: availableTags,
			    renderer: function(v){
				    return v.text + '<span class="label pull-right">' + v.prefix + '</span>';
			    }
			});
			view.$txt.one('hint', function (e) { 
				view.$txt.tooltip({
					'title': 'Insert a URI (or free text between quotes, e.g. "Alice")',
					'trigger': 'manual'
				}).tooltip('show'); 
			});
			view.$txt.one('hintDestroy', function (e) { view.$txt.tooltip('destroy'); });
			
			var customRender = function (e) {
				var uri;
				var arr = ms.getSelectedItems();
				
				if ((arr) && (arr.length>0)) {
					if ((arr.length>1) && (!arr[arr.length -2].value)) {
						if (arr[arr.length - 1].text.indexOf('"') == -1) {
							uri = toAbsURI(arr[arr.length - 1].text, view.model.get("defaultBase"));
						} else {
							//is a literal
							uri = arr[arr.length - 1].text;
						}
						arr[arr.length -2].text = arr[arr.length - 2].text + uri;
						arr[arr.length -2].value = uri;
						ms.removeFromSelection(arr[arr.length - 1], true);
						view.$txt.trigger('hintDestroy');
						return;
					}
					//property part (ie before ':')
					//render element only if is not a deletion
					if (!arr[arr.length - 1].value) {
						//if is not a property of a vocabulary prevent input
						if(!arr[arr.length - 1].uri) {
							ms.removeFromSelection(arr[arr.length - 1], true);
							return;
						}
						arr[arr.length -1].text = arr[arr.length - 1].text + ": ";
						var lastItem = view.$el.find($('.ms-sel-item')).last();
						//make it appearing as string
						lastItem.addClass("ms-sel-text");
						//remove close btn
						lastItem.children().remove();
						lastItem.text(lastItem.text() + ": ");
						view.$txt.trigger('hint');
						console.log(JSON.stringify(arr))
					}	
				}
				if ((e.type == 'selectionchange')
					&& ($(document.activeElement).attr('id') == "ms-input-0")
				) {
					view.model.set({modified: true});
				}
								
		    };
		    $(ms).on('selectionchange', customRender); 
			$(ms).on('blur', customRender);
			$(ms).on('ms-close-btnclick', function(){view.model.set({modified: true});})
			
	  		view.txt = ms;
		};
		
		/////////////////////////////////////////////////////
		//              PUBLIC PROPERTIES
		////////////////////////////////////////////////////
		
		return {
		    template: _.template(drivenFreeTextTemplate),
		
		    events: {
		      'click #userModalNext, #envModalNext': 'nextView',
		      'click #userModalCancel, #devModalCancel, #envModalCancel': 'cancelPolicy',
		      'click #userModalFinish, #devModalFinish, #envModalFinish': 'createPolicy',
		      'click #userModalPrev, #devModalPrev, #envModalPrev': 'prevView',
		      'click #userModalOk , #devModalOk, #envModalOk': 'updateDim',
		      'keydown': 'closeOnEsc',
		    },
			
			initialize: function(options) {
		      this.model = options.model;
		      this.wizard = options.wizard;
		      this.keywords = options.keywords;
		      this.type = options.type;
		      this.$modal = $('#' + this.type + 'Modal');
		      
		      if (options.wizard == true) {
		      	this.listenTo(this.model, 'destroy', function () {
			      	if(this.$modal) {
			      		//if wizard is open (as happens while waiting for saving operation)
			      		//before removing the view properly close (hide) the modal
			      		this.$modal.modal('hide');
			      		this.$modal.off();
			      	}
			      	$(this.txt).off();
			      	this.remove();
			    });
		      } else {
		      	this.listenTo(this.model, 'update', this.updateModel);
		      }
		      //since may be called as callback passed in a options object may have wrong context
		      //so be sure the context is the view.
		      _.bindAll(this, 'setAutocompleteByModel', 'initAutocomplete');
		    },
			
		    render: function(options) {
		    	var that = this;
		    	console.log( this.type + "View.render")
				console.log("options.isLast: " + options.isLastView);
		    	if(options.isLastView) {
		      		this.isLastView = "true";
		      	} else {
		      		this.isLastView = "false";
		      	}
		      	console.log("isLast: " + this.isLastView);
		      	this.$el.html(this.template({
		      		disabled: this.isLastView,
		      		wizard: this.wizard,
		      		name: this.model.get("name"),
		      		type: this.type,
		      	}));
		      			      	
		      	return this;
		    },
		    
		    createPolicy: function() {
		    	if(this.updateWizardModel()) {
			    	console.log(this.type + "View.createPolicy");
			    	console.log("model: " + JSON.stringify(this.model));
			    	this.$modal.modal('hide');
			    	console.log("isLast: " + this.isLastView);
			    	//model destroy called by appView after creating the policy
			    	this.model.trigger('create');
		        }
		    },
			
			nextView: function() {
				console.log(this.type + "View.nextView");
				if(this.updateWizardModel()) {					
					console.log("model: " + JSON.stringify(this.model));
					this.$modal.modal('hide');
					if (this.type == "user") {
			      		this.model.trigger('showTime', 'notLast', this);
			      	}
			      	else if (this.type == "env"){
						this.model.trigger('showDrivenFreeText', {
							parentView: this,
							type: "dev",
							isLast: true,
						});
					}
				}
			},
			
			//$modal optional (needed only if the view is contained in a modal)
			initAutocomplete: function(options) {
				var that = this;
				
				if (this.wizard == true) {
					this.$txt = $('#' + this.type + 'Txt-wiz');
					this.$txtCtrl = $('#' + this.type + 'TxtCtrlGrp-wiz');
					this.$txtErr = $('#' + this.type + 'ErrorText-wiz');
				} else {
					this.$txt = $('#' + this.type + 'Txt');
					this.$txtCtrl = $('#' + this.type + 'TxtCtrlGrp');
					this.$txtErr = $('#' + this.type + 'ErrorText');
				} 
				
			   	loadSuggestions(this.keywords, this);
		    	if((options) && (options.$modal)) {
		    		options.$modal.modal();
		    	}
		    	if ((options) && (options.callback)) {
		           	options.callback();
		        }
			},
				
			setAutocompleteByModel : function() {
				var ac = this.model.get("ac");
				if (!ac[this.type]) {
					console.log("no " + this.type + " dimension in the condition");
					return;
				}
				
				for(i = 0; i < ac[this.type].keywords.length; i++) {
					var item = {
							label: ac[this.type].keywords[i].label,
							text: ac[this.type].keywords[i].label + ": " + ac[this.type].keywords[i].value,
							value: ac[this.type].keywords[i].value,
							uri: ac[this.type].keywords[i].uri,
							localName: ac[this.type].keywords[i].localName,
							prefix: ac[this.type].keywords[i].prefix,
							id: ac[this.type].keywords[i].uri
						};
						if (ac[this.type].keywords[i].prepend) {
							item.prepend = ac[this.type].keywords[i].prepend;
						}
						this.txt.addToSelection(item);				    
				}
				
			},
			
			updateWizardModel: function () {
				var res, ac;
				console.log(this.type + "freeTextView.updateWizardModel");
				//signal that there are time info in the wizard model
				//the value is the AC name
				console.log("model before: " + JSON.stringify(this.model));
		    	console.log("ac before: " + JSON.stringify(this.model.get("ac")));
		   		ac = this.model.get("ac") || {};
		   		delete ac[this.type];
		   		var items = this.txt.getSelectedItems();
		   		if((items) && (items.length >0)) {
		   			res = setAc(ac, items, this, this.type)
			   		console.log(JSON.stringify(ac));
			   		this.model.set({ac: ac});
			   		console.log("model after: " + JSON.stringify(this.model));
			   		return res;
			   	}
		      	console.log( this.type + " skipped");
		      	return true;
			},
			
			setModelModified: function () {
				this.model.set({modified: true});
			}
			    
		};
	})());

	return drivenFreeTextView;

});
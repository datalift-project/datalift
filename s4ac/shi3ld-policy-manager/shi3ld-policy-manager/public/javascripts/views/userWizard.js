/**
 * user view
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'collections/keywords',
  'text!templates/user.html',
  'suggest'
  ], function($, _, Backbone, KeywordCollection, userTemplate){
	
	var UserWizardView = Backbone.View.extend((function() {
	
		/////////////////////////////////////////////////////
		//              PRIVATE PROPERTIES
		////////////////////////////////////////////////////
	
		var updateWizardModel = function (view, ms) {
			var res, ac;
			console.log("userView.updateWizardModel");
			//signal that there are time info in the wizard model
			//the value is the AC name
			console.log("model before: " + JSON.stringify(view.model));
	    	console.log("ac before: " + JSON.stringify(view.model.get("ac")));
	   		ac = view.model.get("ac") || {};
	   		delete ac.user;
	   		var items = ms.getSelectedItems();
	   		if((items) && (items.length >0)) {
	   			res = setUser(ac, items, view)
		   		console.log(JSON.stringify(ac));
		   		view.model.set({ac: ac});
		   		console.log("model after: " + JSON.stringify(view.model));
		   		return res;
		   	}
	      	console.log("user skipped");
	      	return true;
		};
		
		var setUser = function (ac, items, view) {
			
			var i, keyword;
			
			ac.user = {};
	   		ac.user['keywords'] = new Array();
	   		
	   		if (!items[items.length - 1].value) {
	   			view.$txtUserErr.text("A value for the last property is required");
  				view.$txtUserCtrl.addClass("error");
   				console.log("error: missing last condition");
   				delete ac.user;
   				return false; 
	   		}
	   		for(i = 0; i < items.length; i++) {
	   			keyword = _.omit(items[i], 'text', 'id');
	   			ac.user['keywords'].push(keyword);
	   		}
	   		
	   		return true;
	   		
		};
		
		/*var isIRI = function (str) {
		  return true;
		  //TBD change regexp (not valid)
			//var pattern = new RegExp('([a-z0-9+.-]+):(?://(?:((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*)@)?((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)(?::(\d*))?(/(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?|(/?(?:[a-z0-9-._~!$&'()*+,;=:@]|%[0-9A-F]{2})+(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?)(?:\?((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?(?:#((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?$');		  if(!pattern.test(str)) {
		  if(!pattern.test(str)) {  
		    console.log('bad iri: ' + str);
		    return false;
		  }
		  return true;
		};
		
		var split = function ( val ) {
      	    //space is the separator
			return val.split( /\s+/ );
		};
		
		var extractLast = function ( term ) {
		 	return split( term ).pop();
		}*/
		
		//prepend default base uri if a relative is used with criteria
		//of parser used (if keep relative first time policy is parsed
		//uri will change from relative to positive)
		var toAbsURI = function (relUri, defaultBase){
			var protocol = relUri.split(":");
			protocol = protocol[0];
			if ((protocol) && (protocol != "") && (relUri.indexOf(":") != -1)) return relUri;
			return defaultBase + relUri;
		};
		
		var loadSuggestions = function (keywords, view) {
			var availableTags;
	      	console.log("keywords.fetch.success");
	        //create array with 2 default keyword to manage 
	        //the condition allow only if is a particular user
	        //or if is not a particular user (ie forbid to that user)
	      	availableTags = new Array(/*"is", "isNot"*/);
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
	     	
			var  ms = view.$txtUser
			.magicSuggest({
				//resultAsString: true,
				maxSelection: null,
				hideTrigger: true,
			    width: 615,//525,
			    maxSelection: null,
			    sortOrder: 'text',
			    displayField: 'text',
			    minChars: 2,
			    data: availableTags,
			    renderer: function(v){
				    return v.text + '<span class="label pull-right">' + v.prefix + '</span>';
			    }
			});
			view.$txtUser.one('hint', function (e) { 
				view.$txtUser.tooltip({
					'title': 'Insert a URI (or free text between quotes, e.g. "Alice")',
					'trigger': 'manual'
				}).tooltip('show'); 
			});
			view.$txtUser.one('hintDestroy', function (e) { view.$txtUser.tooltip('destroy'); });	
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
						view.$txtUser.trigger('hintDestroy');
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
						var lastItem = /*view.$txtUser.find*/$('.ms-sel-item').last();
						//make it appearing as string
						lastItem.addClass("ms-sel-text");
						//remove close btn
						lastItem.children().remove();
						lastItem.text(lastItem.text() + ": ");
						view.$txtUser.trigger('hint');
						console.log(JSON.stringify(arr))
						//$("#userModalHelper").text("Insert a uri or a literal (literal between \" \")");
						//$("#listOfUsrProp").hide();
					}	
				} 
								
		    };
		    $(ms).on('selectionchange', customRender); 
			$(ms).on('blur', customRender); 
			
	  		view.txtUser = ms;
	  		//if first time assign it to appView to store it 
	  		//(if not 1st time keywords is view.parentView.parentView.parentView.usrVocPropCollection)
	  		//NB to avoid constraint in the vizard flow (order/number of view) use an event
	  		if ((view.parentView) && (view.parentView.parentView))
	  			view.parentView.parentView.usrVocPropCollection = keywords;
		};
		
		/////////////////////////////////////////////////////
		//              PUBLIC PROPERTIES
		////////////////////////////////////////////////////
		
		return {
		    // Cache the template function for the view.
		    template: _.template(userTemplate),
		
		    // The DOM events specific this view.
		    events: {
		      'click #userModalNext': 'nextView',
		      'click #userModalCancel': 'cancelPolicy',
		      'click #userModalFinish': 'createPolicy',
		      'click #userModalPrev': 'prevView',
		      'click #userModalOk': 'updateDim',
		      'keydown': 'closeOnEsc',
		    },
			
			initialize: function(options) {
		      this.model = options.model;
		      this.$modal = $('#userModal');
		      this.wizard = options.wizard;
		      this.keywords = options.keywordsUsr;
		      
		      if (options.wizard == true) {
		      	this.listenTo(this.model, 'destroy', function () {
			      	if(this.$modal) {
			      		//if wizard is open (as happens while waiting for saving operation)
			      		//before removing the view properly close (hide) the modal
			      		this.$modal.modal('hide');
			      		this.$modal.off();
			      	}
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
		    	console.log("userView.render")
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
		      		name: this.model.get("name")
		      	}));
		      			      	
		      	return this;
		    },
		
			cancelPolicy: function() {
			  this.$modal.modal('hide');
		      if(this.wizard == true) {
		      	this.model.destroy();		      	
		      } else {
		      	//use event to avoid parent views structure
		      	this.parentView.parentView.parentView.freeViews();
		      }
		      
		    },
		    
		    createPolicy: function() {
		    	if(updateWizardModel(this, this.txtUser)) {
			    	console.log("userView.createPolicy");
			    	console.log("model: " + JSON.stringify(this.model));
			    	this.$modal.modal('hide');
			    	console.log("isLast: " + this.isLastView);
			    	//model destroy called by appView after creating the policy
			    	this.model.trigger('create');
		        }
		    },
			
			nextView: function() {
				console.log("userView.nextView");
				if(updateWizardModel(this, this.txtUser)) {					
					console.log("model: " + JSON.stringify(this.model));
					this.$modal.modal('hide');
					//retrieve app view (next is enabled only in case of wizard for 
					//blank policy, ie view order is app->wizard->usr->time)
					//this.parentView.parentView.showTimeView(this);
					this.model.trigger('showTime', 'notLast', this);
				}
			},
			
			prevView: function() {
				if(updateWizardModel(this, this.txtUser) == true) {
					console.log("model: " + JSON.stringify(this.model));
					this.$modal.modal('hide');
					//retrieve app view (next is enabled only in case of wizard for 
					//blank policy, ie view order is app->wizard->usr->time)
					this.parentView.showModal();
				}
			},
			
			showModal: function() {
				var txt = "";
				var i;
				var usr = this.model.get("ac");
				
				console.log("userView.showWizard");
				console.log("model: " + JSON.stringify(this.model));
				if ((usr) && (usr.user)) {
				    usr = usr.user;
					console.log("user found: " + JSON.stringify(usr));
				}
				this.$modal.modal();
			},
			
			//$modal optional (needed only if the view is contained in a modal)
			initAutocomplete: function(options) {
				var that = this;
				
				if (this.wizard == true) {
					this.$txtUser = $('#txtUser-wiz');
					this.$txtUserCtrl = $('#txtUserCtrlGrp-wiz');
					this.$txtUserErr = $('#errorTextUser-wiz');
				} else {
					this.$txtUser = $('#txtUser');
					this.$txtUserCtrl = $('#txtUserCtrlGrp');
					this.$txtUserErr = $('#errorTextUser');
				} 
				
				/*if((!this.parentView)||(!this.parentView.parentView)||(!this./parentView.parentView.usrVocPropCollection)){ 
			      	var keywords = new KeywordCollection({suffix: "user-dim"});
			      	//see how to pass parameters!
			      	keywords.fetch({
			          success: function (keywords) {
			           loadSuggestions(keywords, that);
			           if((options) && (options.$modal)) {
			           	options.$modal.modal();
			           }
			           if ((options) && (options.callback)) {
			           	options.callback();
			           }
			           	
			          },
			          error: function () {
			          	that.model.trigger('error');
			          }
			        });
			    } else {*/
			    	loadSuggestions(this.keywords,this);
			    	if((options) && (options.$modal)) {
			    		options.$modal.modal();
			    	}
			    	if ((options) && (options.callback)) {
			           	options.callback();
			        }
			    //}
			},
				
			updateModel: function (options) {
				updateWizardModel(this, this.txtUser);
			},
			
			setAutocompleteByModel : function() {
				var ac = this.model.get("ac");
				if (!ac.user) {
					console.log("no user dimension in the condition");
					return;
				}
				
				var usr = ac.user;
				for(i = 0; i < usr.keywords.length; i++) {
					//for (var prop in usr.keywords[i]) {
						this.txtUser.addToSelection({
							label: usr.keywords[i].label,
							text: usr.keywords[i].label + ": " + usr.keywords[i].value,
							value: usr.keywords[i].value,
							uri: usr.keywords[i].uri,
							localName: usr.keywords[i].localName,
							prefix: usr.keywords[i].prefix,
							id: usr.keywords[i].uri
						});				    
					//}
				}
				
			},
			
			closeOnEsc: function (e) {
				if (e.keyCode == 27) {
					this.model.trigger('saveAlert', {
						cancelWizard: true,
						type: 'confirmCancel',
					});
				} 
			}
			    
		};
	})());

	return UserWizardView;

});
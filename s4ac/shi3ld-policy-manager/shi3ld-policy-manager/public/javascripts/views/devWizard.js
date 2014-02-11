/**
 * device view
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'collections/keywords',
  'text!templates/dev.html',
  'jqueryui'
  ], function($, _, Backbone, KeywordCollection, devTemplate){
	
	var DevWizardView = Backbone.View.extend((function() {
		
		/////////////////////////////////////////////////////
		//              PRIVATE PROPERTIES
		////////////////////////////////////////////////////
		
		var updateWizardModel = function (view, ms) {
			var ac, res;
			
			console.log("devView.updateWizardModel");
			//signal that there are time info in the wizard model
			//the value is the AC name
			console.log("model before: " + JSON.stringify(view.model));
	    	console.log("ac before: " + JSON.stringify(view.model.get("ac")));
	   		ac = view.model.get("ac") || {};
	   		delete ac.dev;
	   		
	   		var items = ms.getSelectedItems();
	   		
	   		if((items) && (items.length >0)) {
		   		res = setDev(ac, items, view);
		   		view.model.set({ac: ac});
		   		console.log("model after: " + JSON.stringify(view.model));
		   		return res;
	      	}
	      	console.log("dev skipped");
	      	return true;
		};		
		
		var setDev = function (ac, items, view) {
			var i, keyword;
			
			ac.dev = {};
	   		ac.dev['keywords'] = new Array();
		   	
		   	if (!items[items.length - 1].value) {
	   			view.$txtDevErr.text("A value for the last property is required");
  				view.$txtDevCtrl.addClass("error");
   				console.log("error: missing last condition");
   				delete ac.dev;
   				return false; 
	   		}
	   		
	   		for(i = 0; i < items.length; i++) {
	   			keyword = _.omit(items[i],'text','id');
	   			ac.dev['keywords'].push(keyword);
	   		}
	   		
	   		return true;
	   		
		};
		
		/*var isIRI = function (str) {
		  return true;
		  //TBD change regexp (not valid)
		  //var pattern = new RegExp('([a-z0-9+.-]+):(?://(?:((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*)@)?((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)(?::(\d*))?(/(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?|(/?(?:[a-z0-9-._~!$&'()*+,;=:@]|%[0-9A-F]{2})+(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?)(?:\?((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?(?:#((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?$');
		  //var regexUri = /^([a-z0-9+.-]+):(?://(?:((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*)@)?((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)(?::(\d*))?(/(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?|(/?(?:[a-z0-9-._~!$&'()*+,;=:@]|%[0-9A-F]{2})+(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)?)(?:\?((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?(?:#((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*))?$/i;
/*composed as follows:
^
([a-z0-9+.-]+): #scheme
(?:
// #it has an authority:
(?:((?:[a-z0-9-._~!$&'()*+,;=:]|%[0-9A-F]{2})*)@)? #userinfo
((?:[a-z0-9-._~!$&'()*+,;=]|%[0-9A-F]{2})*)	#host
(?::(\d*))? #port
(/(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)? #path
|
#it doesn't have an authority:
(/?(?:[a-z0-9-._~!$&'()*+,;=:@]|%[0-9A-F]{2})+(?:[a-z0-9-._~!$&'()*+,;=:@/]|%[0-9A-F]{2})*)? #path
)
(?:
\?((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*)	#query string
)?
(?:
#((?:[a-z0-9-._~!$&'()*+,;=:/?@]|%[0-9A-F]{2})*)	#fragment
)?
$
*/
		 /* if(!pattern.test(str)) {
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
			
			var availableTags = new Array();
          	_.each(keywords.models, function(keyword) {
          		//keyword.set({id: keyword.get("label")});
	      		var tag = _.clone(keyword.attributes);
	      		//magic suggest needs an id field
	      		tag.id = keyword.get("uri");
	      		//used to render tags keeping original original value for keyword properties
	      		tag.text = keyword.get("label");
	      		this.push(tag);
          	}, availableTags);
          	console.log("availableTags: " + availableTags);
            
			var  ms = view.$txtDev
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
				    return v.label + '<span class="label pull-right">' + v.prefix + '</span>';
			    }
			});
			view.$txtDev.one('hint', function (e) { view.$txtDev.tooltip({
				'title': 'Insert a URI or a name (name between quotes eg. "Alice")',
				'trigger': 'manual'
			})
			.tooltip('show'); });
			view.$txtDev.one('hintDestroy', function (e) { view.$txtDev.tooltip('destroy'); });	
			var customRender = function (e) {
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
						ms.removeFromSelection(arr[arr.length -1], true);
						view.$txtDev.trigger('hintDestroy');
						return;
					}
					//property part (ie before ':')
					//render element only if is not a deletion
					if (!arr[arr.length -1].value) {
						//if is not a property of a vocabulary prevent input
						if(!arr[arr.length -1].uri) {
							ms.removeFromSelection(arr[arr.length -1], true);
							return;
						}
						arr[arr.length -1].text = arr[arr.length -1].text + ": ";
						var lastItem = /*view.$txtUser.find*/$('.ms-sel-item').last();
						//make it appearing as string
						lastItem.addClass("ms-sel-text");
						//remove close btn
						lastItem.children().remove();
						lastItem.text(lastItem.text() + ": ");
						view.$txtDev.trigger('hint');
					}	
				} 
								
		    };
		    $(ms).on('selectionchange', customRender); 
			$(ms).on('blur', customRender); 
			
			view.txtDev = ms;		
			//if first time assign it to appView to store it 
	  		//(if not 1st time keywords is view.parentView.parentView.parentView.usrVocPropCollection)
	  		//NB to avoid constraint in the vizard flow (order/number of view) use an event
	  		if ((view.parentView) && (view.parentView.parentView))
	  			view.parentView.parentView.parentView.parentView.parentView.devVocPropCollection = keywords;
		};
		
		/////////////////////////////////////////////////////
		//              PUBLIC PROPERTIES
		////////////////////////////////////////////////////
		
		return {
		    // Cache the template function for the view.
		    template: _.template(devTemplate),
		
		    // The DOM events specific this view.
		    events: {
		      'click #devModalCancel': 'cancelPolicy',
		      'click #devModalFinish': 'createPolicy',
		      'click #devModalPrev': 'prevView',
		      'keydown': 'closeOnEsc',
		    },
			
			initialize: function(options) {
		      this.model = options.model;
		      this.$modal = $('#devModal');
		      this.$dev = $("#txtDev");
		      this.wizard = options.wizard; 
		      this.keywords = options.keywordsDev;
		      
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
		      } else{
		      	this.listenTo(this.model, 'update', this.updateModel);
		      }
		      _.bindAll(this, 'setAutocompleteByModel', 'initAutocomplete');
		    },
			
		    render: function(options) {
		    	var that = this;
		    	console.log("devView.render")
				console.log("options.isLast: " + options.isLastView);
		    	
		    	if(options.isLastView) {
		      		this.isLastView = "true";
		      	} else {
		      		this.isLastView = "false";
		      	}
		      	
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
		      	this.parentView.parentView.parentView.parentView.parentView.freeView();
		      }	     
		    },
		    
		    createPolicy: function() {
		    
		    	if(updateWizardModel(this, this.txtDev)) {
			    	console.log("devView.createPolicy");
			    	console.log("model: " + JSON.stringify(this.model));
			    	this.$modal.modal('hide');
			    	console.log("isLast: " + this.isLastView);
			    	//model destroy called by appView after creating the policy
			    	this.model.trigger('create');
		        }
		    },
			
			//now is last view of wizard but may be useful if order changes
			nextView: function(){
			},
			
			prevView: function(){
				if(updateWizardModel(this, this.txtDev) == true) {
					console.log("model: " + JSON.stringify(this.model));
					this.$modal.modal('hide');
					//retrieve app view (next is enabled only in case of wizard for 
					//blank policy, ie view order is app->wizard->usr->time)
					this.parentView.showModal();
				}
			},
			
			showModal: function(){
				var txt = "";
				var i;
				var dev = this.model.get("ac");
				
				console.log("devView.showWizard");
				console.log("model: " + JSON.stringify(this.model));
				if ((dev) && (dev.dev)) {
				    dev = dev.dev;
					console.log("dev found: " + JSON.stringify(dev));
				}
				this.$modal.modal();
			},
			
			initAutocomplete: function(options) {
				var that = this; 
				if (this.wizard == true) {
					this.$txtDev = $('#txtDev-wiz');
					this.$txtDevCtrl = $('#txtDevCtrlGrp-wiz');
					this.$txtDevErr = $('#errorTextDev-wiz');
				} else {
					this.$txtDev = $('#txtDev');
					this.$txtDevCtrl = $('#txtDevCtrlGrp');
					this.$txtDevErr = $('#errorTextDev');
				}
				
				/*if((!this.parentView) || (!this.parentView.parentView) ||
					(!this.parentView.parentView.parentView.devVocPropCollection)) {
			      	
			      	var keywords = new KeywordCollection({suffix: "dev-dim"});
			      	
			      	keywords.fetch({
			          success: function (keywords) {
			          	console.log("keywords.fetch.success");
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
			
			setAutocompleteByModel : function() {
				var ac = this.model.get("ac");
				if (!ac.dev) {
					console.log("no user dimension in the condition");
					return;
				}
				
				var dev = ac.dev;
				for(i = 0; i < dev.keywords.length; i++) {
					this.txtDev.addToSelection({
							label: dev.keywords[i].label,
							text: dev.keywords[i].label + ": " + dev.keywords[i].value,
							value: dev.keywords[i].value,
							uri: dev.keywords[i].uri,
							localName: dev.keywords[i].localName,
							prefix: dev.keywords[i].prefix,
							id: dev.keywords[i].uri,
							prepend: dev.keywords[i].prepend
						});				    
				}
				
			},
			
			updateModel: function (options) {
				updateWizardModel(this, this.txtDev);
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

	return DevWizardView;

});
 /**
 * view for the whole wizard flow (conteiner of the various wizard views)
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'models/wizard',
  'views/newPolicyMain',
  'views/timeWizard',
  'views/targets',
  'views/drivenFreeText',
  'views/outdoorWizard',
  'views/newGraph',
  'bootstrap'
  ], function(
  	$,
  	 _,
  	Backbone,
  	WizardModel,
  	NewPolicyMainView, 
  	TimeView, 
  	TargetListView, 
  	DrivenFreeTextView, 
  	OutdoorView, 
  	NewGraphView){
	
	var WizardControllerView = Backbone.View.extend({
		
		initialize: function(options) {
	    	this.$newPolicy =  $("#newPolicy");
	    	this.$newGraph = $("#newGraph");
	    	this.$browseTargets = $("#browseTargets");
	    	this.$timeModal = $("#timeModal");
	    	this.$outdoorModal = $("#outdoorModal");
	    	this.parentView = options.parentView;
	    	_.bindAll(this, 'showNewGraphView')
	    },
	
	    showWizard: function() {
	    	var that = this;    	
	    	
	    	this.wizardModel = new WizardModel({
	    		dateFormat: this.parentView.dateFormat,
	    		timeFormat: this.parentView.timeFormat,
	    		defaultBase: this.parentView.defaultBase,
	    	});
	    	
	    	this.listenTo(this.wizardModel, 'destroy', this.freeWizard);
	    	this.listenTo(this.wizardModel, 'create', this.parentView.createPolicy);
	    	this.listenTo(this.wizardModel, 'showDrivenFreeText', this.showDrivenFreeTextView);
	    	this.listenTo(this.wizardModel, 'showTime', this.showTimeView);
	    	this.listenTo(this.wizardModel, 'showOutdoor', this.showOutdoorView);
	    	this.listenTo(this.wizardModel, 'error', this.parentView.notifyError);
	    	this.listenTo(this.wizardModel, 'addGraph', this.showNewGraphView);
	    	this.listenTo(this.wizardModel, 'saveAlert', this.parentView.showAlert);
	    	this.listenTo(this.wizardModel, 'closeWizard', function() {
	    		//destroy the model and raise event to free wizard
	    		this.wizardModel.destroy();
	    	});
	    	
	    	this.wizardView = new NewPolicyMainView({
	    		model: this.wizardModel,
	    		backend: this.parentView.backend
	    	});
	    	this.wizardView.parentView = this;
			this.wizardView.render();
	
			var $modalEl = this.$newPolicy;
			$modalEl.html(this.wizardView.el);
			this.wizardView.initAutocomplete();
			$("[data-toggle-wizard='tooltip']").tooltip().css("white-space","pre-wrap");
			that.wizardView.$modal.on('shown', function () {
				$('#inputName').trigger('focus');
			 });
			$modalEl.modal();	
	    },
	    
	    showNewGraphView: function (options) {
	   		var view = new NewGraphView({
				defaultPrefix: this.parentView.defaultPrefix,
				targets: options.parentView.targets,
				targetsViewModel: options.targetsViewModel,
			});
	    	view.parentView = options.parentView;
			view.render();
					 
			var $modalEl = this.$newGraph;
			 
			$modalEl.html(view.el);
			$('.modal-body').attr('style','max-height: 800px;')
			view.init();
			$modalEl.modal();
	   	},
	    	
		//the modal that guess the view is in app view but new modal view actually is a child view of current modal
		//if new view set as child of curr modals, new modal can modify father attribute accessing parentView
		showTargets: function(wizardParentView){
			//if parent view is policyGui pass the collection of targets
			if ((wizardParentView.targetView) && (wizardParentView.targetView.targets)) {
				model = wizardParentView.targetView.targets;
			} else {
				model = wizardParentView.model
			}
			var view= new TargetListView({
				backend: this.parentView.backend,
				model: model,
				wizard: true
			});
	    	view.parentView = wizardParentView;
			view.render();
					 
			var $modalEl = this.$browseTargets;
			 
			$modalEl.html(view.el);
			view.initPagination();
			$modalEl.modal();
		},
	
	//	receives 0 or 1 or 2 param:
	//  parentView (always) - to keep track of previous view and render it if back btn pressed 
	//  flag optional - to indicate that no other views will be rendered after 
	//  				(to manage kind of wizard by basing on policy type)
	//  				if present it will be 1st param.	
		showTimeView: function(){
			var isLast, $modalEl, wizardParentView, wizard;
			var that = this;
			
			$modalEl = this.$timeModal;
			if(this.timeView){
				//if modal has content view already 
				//existing than just show modal
				this.timeView.showModal();
				return;
			}
			console.log("type:" + arguments[0])
			if(arguments.length == 2){
				wizardParentView = arguments[1];
				isLast = false;
			}else{
				wizardParentView = arguments[0];
				isLast = true;
			}
			
			this.timeView = new TimeView({
				model: wizardParentView.model,
				wizard: true,
			});
	    	this.timeView.parentView = wizardParentView;
	    	console.log("dateFormat: " + this.parentView.dateFormat);
	    	this.wizardModel.set({format: this.parentView.dateformat})
	    	console.log("isLast:" + isLast);
	    	var now = dateFormat(new Date(), this.parentView.dateFormat);
	    	console.log("now: " + now);
			
			this.timeView.render({
				isLastView: isLast,
				date:  now 
			});
			 
			$modalEl.html(this.timeView.el);
			//when time picker div added on DOM initialize them		
			this.timeView.initPickers();
			$modalEl.on('shown', function(e) {
		        that.timeView.$dp1Txt.trigger('focus');
		     });
			$modalEl.modal();  
		},
		
		showDrivenFreeTextView: function(options){
			var isLast, $modalEl, wizardParentView, wizard;
			var that = this;
			var view;
			
			if ((this.userView) && (options.type == "user")) {
				this.userView.showModal();
				return;
			}
			if ((this.devView) && (options.type == "dev")) {
				this.devView.showModal();
				return;
			}
			if ((this.envView) && (options.type == "env")) {
				this.envView.showModal();
				return;
			}
			
			$modalEl = $("#" + options.type + "Modal");
			console.log("type:" + arguments[0])
			    	
	    	console.log("isLast:" + options.isLast)
	    	viewOptions = {
				model: options.parentView.model,
				wizard: true,
				type: options.type
			}
			if (options.type == "user") {
				viewOptions.keywords = this.parentView.usrVocPropCollection;
			} else if (options.type == "dev") {
				viewOptions.keywords = this.parentView.devVocPropCollection;
			} else {
				viewOptions.keywords = this.parentView.envVocPropCollection;
			}
				
			view = new DrivenFreeTextView(viewOptions);
	    	
	    	view.parentView = options.parentView;
			view.render({
				isLastView: options.isLast, 
			});
			 
			$modalEl.html(view.el);
			$modalEl.on('shown', function(e) {
		        that.userView.$el.find('input').trigger('focus');
		    });
			//view need to manage the modal
			view.initAutocomplete({$modal: $modalEl});
			if (options.type == "user") this.userView = view;
			else if (options.type == "dev") this.devView = view;
			else this.envView = view;
		},
		
		showOutdoorView: function(){
			var isLast, $modalEl, wizardParentView, wizard;
			var that = this;
			
			$modalEl = this.$outdoorModal;
			if(this.outdoorView){
				//if modal has content view already 
				//existing than just show modal
				this.outdoorView.showModal();
				return;
			}
			console.log("type:" + arguments[0])
			if(arguments.length == 2){
				wizardParentView = arguments[1];
				isLast = false;
			}else{
				wizardParentView = arguments[0];
				isLast = true;
			}
			
			console.log("isLast:" + isLast)
	    	
			this.outdoorView = new OutdoorView({
				model: wizardParentView.model,
				wizard: true,
			});
	    	this.outdoorView.parentView = wizardParentView;
	    	    	
			this.outdoorView.render({
				isLastView: isLast, 
			});
			$modalEl.html(this.outdoorView.el);
			this.outdoorView.initPickers();
			//if done inside outdoor view in initialize as others wizard view
			//map not works properly
			$modalEl.on('shown', function() {
				//should not access directly an el of another view
				that.outdoorView.$map.trigger('focus');
				that.outdoorView.$map.addresspicker( "updateMap" );
			});
			$modalEl.modal(); 
			
		},
		
		freeWizard: function(){
			console.log("freeing wizard");
			this.stopListening(this.wizardModel);
			this.stopListening(this.model,'rename');
			delete this.wizardModel;
			//trigger close policy because if name conflict on a policy
	        //and user don't want to overwrite than cancel need to close the policy
	        //(since actually is not saved)
	      	if (this.parentView.model) {
	      		//policyClosed passed since not to be saved (user is cancelling wizard)
	      		this.parentView.model.trigger('closePolicy', {policyClosed: true});
	      	}
			this.freeViews();
		},
		
		freeViews: function() {
			
			if(this.timeView) {
				delete this.timeView;
			}
			if(this.userView) {
				delete this.userView;
			}
			if(this.devView) {
				delete this.devView;
			}
			if(this.outdoorView) {
				delete this.outdoorView;
			}
			if(this.envView) {
				delete this.envView;
			}
			delete this.wizardView;
			
		} ,
			
	});

	return WizardControllerView;

});

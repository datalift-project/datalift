/**
 * Abstract view to be extended
 * Contains general function to manage policy collection
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'views/policyHeaders',
  'text!templates/policyCollectionManagement.html',
  'bootstrap'
  ], function(
  	$,
  	_, 
  	Backbone, 
  	PolicyHeadersView,
  	policyCollectionManagementTemplate 
  ) {
  	
  var PolicyCollectionManagementView = Backbone.View.extend({
  	
  	template: _.template(policyCollectionManagementTemplate),
  	
  	render: function () {
		this.$el.html(this.template({type: this.type}));
		return this;
	},
	
	initPolicyCollection: function (options) {
	
		this.policyCollection = options.collection;
		this.listenTo(this.policyCollection, 'error',this.notifyError);
		this.policyListView = new PolicyHeadersView({
			backend: this.backend,
			MSG_TIMEOUT: this.MSG_TIMEOUT,
			type: this.type
		});
		
		this.$policyList = $('#policyList');		
		this.$policyList.html(this.policyListView.render().el);
		this.policyListView.initPagination({policyHeaders: this.policyCollection})
		
		if (this.type == 'workspace') {
			this.$policyPanel = $('#policyPanel');
			this.listenTo(this.policyCollection, 'showWizard', this.showWizard);
			this.listenTo(this.policyCollection, 'openPolicy', this.openPolicy);
			this.listenTo(this.policyCollection, 'closePolicy', this.closePolicy);
			this.listenTo(this.policyCollection, 'remove', this.deletePolicy);
			
			this.listenTo(this.policyCollection, 'sync', function () { 
				this.$spinner.modal('hide');
			});
			
			this.listenTo(this.policyCollection, 'saveAlert', this.showAlert);
		} else {
			this.listenToOnce(this.policyCollection, 'sync', function () {
				if (this.policiesToTest.length > 0) {
					this.policyCollection.findWhere({uri: this.policiesToTest[0]}).trigger('selectPolicy');
				}
			});
		}
	},
  
  });
  
  return PolicyCollectionManagementView;
  
});
/**
 * Abstract view to collect method to be inherithed by all wizard views
 */
define([
  'jquery',
  'underscore',
  'backbone',
  'bootstrap'
  ], function($, _, Backbone){
	
	var WizardView = Backbone.View.extend({
		
		closeOnEsc: function (e) {
			//esc pressed
			if (e.keyCode == 27) {
				this.model.trigger('saveAlert', {
					cancelWizard: true,
					type: 'confirmCancel',
				});
			}
			//tab pressed
			/*if (e.keyCode == 9) {
				this.tabindex = (this.tabindex + 1) % 10;
				this.tabindex range [0-9], tabindex attr range [1-10]
				$('[tabindex=' + (this.tabindex + 1) + ']').trigger('focus');
				alert($('[tabindex=' + (this.tabindex + 1) + ']').attr('id'));
			}*/
		},
		
		cancelPolicy: function() {
		  this.$modal.modal('hide');
	      this.model.destroy();
	    },
	    
    	prevView: function(e){
    		var move;
    		if ((e.target.id == "timeModalPrev") || (e.target.id == "outdoorModalPrev")){
				this.updateWizardModel();
				move = true
			} else {
				move = this.updateWizardModel();
			}
			if (move) {
				this.$modal.modal('hide');
				this.parentView.showModal();
			}
		},
			
		showModal: function() {
			if (this.$modal) {
				this.$modal.modal();
				return;
			}
			//owerwrite modal make it lose proper DOM element on this.$modal so find with jquery
			//because hidden and showed later (ie moved on DOM because is the way bootstrap managed modals)
			$('#newPolicy').modal();
		},
						
	});

	return WizardView;

});
 
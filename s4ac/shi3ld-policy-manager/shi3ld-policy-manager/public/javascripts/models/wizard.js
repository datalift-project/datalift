/**
 * wizard model
 * cache wizard info in order to create a policy model (and view) only if
 * finish button pressed (ie if creation actually confirmed)
 * Model the choise done by user in wizard so that policy will be created according to it
 */
 
 define(['underscore', 'backbone'], function(_, Backbone) {
  var WizardModel = Backbone.Model.extend({
		
    // Remove this model from *localStorage* and delete its view.
    clear: function() {
      this.destroy();
      //this.view.remove();
    }

  });
  
  return WizardModel;

});
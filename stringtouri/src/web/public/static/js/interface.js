$(document).ready(function(){

	var successcolor = "#690";
	var defaultcolor = "#999";
	var errorcolor = "#933";
	var noneEN = "None";
	var noneFR = "Aucune";

	var form = $("#linkage-form");
	var submit = $("#convert-submit");
	var cancel = $("#convert-cancel");
	var theirds = $("#their-dataset");
	var ourclass = $("#our-class");
	var theirclass = $("#their-class");
	var ourpredicate = $("#our-predicate");
	var theirpredicate = $("#their-predicate");

	// Stubs
	var datasets = [
		"datasetsone",
		"datasetstwo",
		"datasetsthree",
		"datasetsfour",
		"test-bis"
	];

	var ourclasses = [
		"ourclassesone",
		"ourclassestwo",
		"ourclassesthree",
		"ourclassesfour",
		"geo:Commune"
	];

	var theirclasses = [
		"theirclassesone",
		"theirclassestwo",
		"theirclassesthree",
		"theirclassesfour"
	];

	var ourpredicates = [
		"ourpredicatesone",
		"ourpredicatestwo",
		"ourpredicatesthree",
		"ourpredicatesfour",
		"geo:nom"
	];

	var theirpredicates = [
		"theirpredicatesone",
		"theirpredicatestwo",
		"theirpredicatesthree",
		"theirpredicatesfour"
	];

	function errorState(field) {
		field.addClass("ui-state-error-text");
		field.css("border-color",errorcolor);
		field.next("p").addClass("ui-state-error-text");
		return false;
	}

	function okState(field, color) {
		field.removeClass("ui-state-error-text");
		field.css("border-color",color);
		field.next("p").removeClass("ui-state-error-text");
		return true;
	}

	function validateMandatory(field, values) {
		var valid;
		if(jQuery.inArray(field.val().trim(), values) != -1) {
			valid = okState(field, successcolor);
		}
		else {
			valid = errorState(field);
		}
		return valid;
	}

	function validateOptional(field, values) {
		var v = field.val().trim();
		var valid;
		if(jQuery.inArray(v, values) != -1) {
			valid = okState(field, successcolor);
		}
		else if ( !v.length || v == noneEN || v == noneFR) {
			valid = okState(field, defaultcolor);
		}
		else {
			valid = errorState(field);
		}
		return valid;
	}
	
	submit.button();
	cancel.button();

	theirds.autocomplete({source: datasets, minLength: 0, delay: 0});
	theirds.blur(function() {validateMandatory(theirds, datasets);});
	ourclass.autocomplete({source: ourclasses, minLength: 0, delay: 200});
	ourclass.blur(function() {validateOptional(ourclass, ourclasses);});
	theirclass.autocomplete({source: theirclasses, minLength: 0, delay: 200});
	theirclass.blur(function() {validateOptional(theirclass, theirclasses);});
	ourpredicate.autocomplete({source: ourpredicates, minLength: 0, delay: 300});
	ourpredicate.blur(function() {validateMandatory(ourpredicate, ourpredicates);});
	theirpredicate.autocomplete({source: theirpredicates, minLength: 0, delay: 300});
	theirpredicate.blur(function() {validateMandatory(theirpredicate, theirpredicates);});

	form.submit(function(){
		return false;
	});
});
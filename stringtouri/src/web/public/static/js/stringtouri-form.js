/*
 * An interface for a {@link ProjectModule project module} that replaces RDF object fields
 * from a RDF data by URIs to RDF entities.
 * Autocompletion using jQuery, custom-made form validation.
 * 
 * @author tcolas
 */

$(document).ready(function() {

	var ourds = $("#our-dataset");
	var theirds = $("#their-dataset");
	var ourclass = $("#our-class");
	var theirclass = $("#their-class");
	var ourpredicate = $("#our-predicate");
	var theirpredicate = $("#their-predicate");

	/*
	* Applies the ui-state-error style to the container of {@param field}.
	* @param {object} field The DOM element which contains the error.
	*/
	function errorState(field) {
		field.removeClass("ui-state-success");
		field.parent().addClass("ui-state-error");
		field.next("p").contents().first()
			.removeClass("ui-icon-help ui-icon-check")
			.addClass("ui-icon-alert")
			.show();
	}

	/*
	* Applies the ui-state-success style to the container of {@param field}.
	* @param {object} field The DOM element which is correct.
	*/
	function successState(field) {
		field.parent().removeClass("ui-state-error");
		field.addClass("ui-state-success");
		field.next("p").contents().first()
			.removeClass("ui-icon-help ui-icon-alert")
			.addClass("ui-icon-check")
			.show();
	}

	/*
	* Rolls back the container of {@param field} to its default styling.
	* @param {object} field The DOM element which has a "default" value.
	*/
	function defaultState(field) {
		field.removeClass("ui-state-success");
		field.parent().removeClass("ui-state-error");
		field.next("p").contents().first()
			.removeClass("ui-icon-help ui-icon-alert ui-icon-check")
			.hide();
	}

	/*
	* Checks if {@param str} is empty, null or undefined.
	* @param {string} str The string to be checked.
	* @return {bool} True if real string, false if empty ("") or null/undefined.
	*/
	function isEmpty(str) {
		return (!str || 0 === str.length);
	}

	/*
	* Checks if {@param val} isn't empty and is in the array {@param values}.
	* @param {string} val The string to be checked.
	* @param {array} values The array of strings where {@param val} must be.
	* @return {bool} True if val is in the array, false otherwise.
	*/
	function isValid(val, values) {
		return !isEmpty(val) && jQuery.inArray(val, values) != -1;
	}

	/*
	* Checks if {@param val} is empty or has a value equal to "none".
	* @param {string} val The string to be checked.
	* @return {bool} True if val is empty OR is equal to none (eg. "None").
	*/
	function isEmptyOptional(val) {
		return isEmpty(val) || val === none;
	}

	/*
	* Checks if the mandatory input {@param field} is valid by looking for its
	* value inside a given array.
	* @param {object} field The DOM element to validate.
	* @param {array} values The array of strings where the value must be.
	* @return {bool} True if {@param field} is valid, ie. is in {@param values}.
	*/
	function validateMandatory(field, values) {
		var str = field.val().trim();
		var valid = isValid(str, values);
		if (valid) {
			successState(field);
		}
		else {
			errorState(field);
		}
		return valid;
	}

	/*
	* Checks if the mandatory input {@param field} is valid by looking for its value inside a given array.
	* @param {object} field The DOM element to validate.
	* @param {array} values The array of strings where the value must be.
	* @return {bool} True if {@param field} is valid, ie. is in {@param values} OR {@param field} is empty.
	*/
	function validateOptional(field, values) {
		var str = field.val().trim();
		var valid = isValid(str, values);
		if (valid) {
			successState(field);
		}
		else if (isEmptyOptional(str)) {
			valid = true;
			defaultState(field);
		}
		else {
			errorState(field);
		}
		return valid;
	}

	/*
	* Checks if all mandatory and optional fields are valid. Optional fields
	* don't have to be filled. Mandatory fields are tested separately to make
	* use of the error state highlight.
	* @return {bool} True if all of the form's fields are valid.
	*/
	function validateAll() {
		// We have to check fields separately in order to mark the errors.
		var od = validateMandatory(ourds, datasets);
		var td = validateMandatory(theirds, datasets);
		var op = validateMandatory(ourpredicate, ourpredicates);
		var tp = validateMandatory(theirpredicate, theirpredicates);
		
		var ret = od && td && op && tp
			&& validateOptional(ourclass, ourclasses)
			&& validateOptional(theirclass, theirclasses);

		if(ret) {
			$("#convert-submit").attr("disabled", true);
		}
		return ret;
	}
	
	$("#convert-submit, #convert-cancel").button();

	ourds.autocomplete({source: datasets, minLength: 0, delay: 0});
	ourds.blur(function() {validateMandatory(ourds, datasets);});
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

	$("#linkage-form").submit(function(){return validateAll();});

});

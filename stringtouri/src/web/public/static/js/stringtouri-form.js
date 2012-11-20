/*
 * An interface for a {@link ProjectModule project module} that replaces RDF object fields
 * from a RDF data by URIs to RDF entities.
 * Autocompletion using jQuery, custom-made form validation.
 *
 * @author tcolas
 */

(function($) {
	$.widget("ui.combobox", {
		_create: function() {
			var input,
				self = this,
				select = this.element.hide(),
				selected = select.children(":selected"),
				value = selected.val() ? selected.text() : "",
				wrapper = this.wrapper = $( "<span>")
					.addClass("ui-combobox")
					.css("width","100%")
					.insertAfter(select);

			input = $("<input>")
				.appendTo(wrapper)
				.val(value)
				// Necessary modifications for my module to work.
				.attr("name", select.attr("name"))
				.attr("id", select.attr("id"))
				.attr("tabindex", select.attr("tabindex"))
				.attr("placeholder", select.children(":first-child").val())
				.attr("data-example", select.attr("data-example"))
				.addClass("ui-state-default ui-combobox-input")
				.autocomplete({
					delay: 0,
					minLength: 0,
					source: function(request, response) {
						var matcher = new RegExp($.ui.autocomplete.escapeRegex(request.term), "i");
						response(select.children("option").map(function() {
							var text = $(this).text();
							if (this.value && (!request.term || matcher.test(text)))
								return {
									label: text.replace(
										new RegExp(
											"(?![^&;]+;)(?!<[^<>]*)(" +
											$.ui.autocomplete.escapeRegex(request.term) +
											")(?![^<>]*>)(?![^&;]+;)", "gi"
										), "<strong>$1</strong>"),
									value: text,
									option: this
								};
						}));
					},
					select: function(event, ui) {
						ui.item.option.selected = true;
						self._trigger("selected", event, {
							item: ui.item.option
						});
					},
					change: function(event, ui) {
						if (!ui.item) {
							var matcher = new RegExp("^" + $.ui.autocomplete.escapeRegex($(this).val()) + "$", "i"),
								valid = false;
							select.children("option").each(function() {
								if ($(this).text().match(matcher)) {
									this.selected = valid = true;
									return false;
								}
							});
						}
					}
				})
				.addClass("ui-widget ui-widget-content ui-corner-left")
				.css({
					background : "none white",
					color : "#333",
					"font-weight" : "normal",
					});

			input.data("autocomplete")._renderItem = function(ul, item) {
				return $("<li></li>")
					.data("item.autocomplete", item)
					.append("<a>" + item.label + "</a>")
					.appendTo(ul);
			};

			$("<a>")
				.attr("tabIndex", -1)
				.attr("title", "Show All Items")
				.appendTo(wrapper)
				.css({background : "#fff", "font-size": "80%"})
				.button({ label : '&#9660;' })
				.removeClass("ui-corner-all")
				.addClass("ui-corner-right ui-combobox-toggle")
				.click(function() {
					// close if already visible
					if (input.autocomplete("widget").is(":visible")) {
						input.autocomplete("close");
						return;
					}

					// work around a bug (likely same cause as #5265)
					$(this).blur();

					// pass empty string as value to search for, displaying all results
					input.autocomplete("search", "");
					input.focus();

					select.attr("name", "");
					select.attr("id", "");
					select.attr("tabindex", -10)
				});
		},

		destroy: function() {
			this.wrapper.remove();
			this.element.show();
			$.Widget.prototype.destroy.call(this);
		}
	});
})(jQuery);

$(function() {

	$(".hidden-field-js").hide();

	$("#.multiple-choices a, .multiple-choices input").button();
	$(".multiple-choices").buttonset();
	$("select").combobox();

	var $ourds = $(".target-js .dataset-js input");
	var $theirds = $(".source-js .dataset-js input");
	var $ourclass = $(".target-js .class-js input");
	var $theirclass = $(".source-js .class-js input");
	var $ourpredicate = $(".target-js .predicate-js input");
	var $theirpredicate = $(".source-js .predicate-js input");

	$ourds.blur(function() {window.setTimeout(validateMandatory, 100, $ourds, datasets);});
	$theirds.blur(function() {window.setTimeout(validateMandatory, 100, $theirds, datasets);});
	$ourclass.blur(function() {window.setTimeout(validateOptional, 100, $ourclass, ourclasses);});
	$theirclass.blur(function() {window.setTimeout(validateOptional, 100, $theirclass, theirclasses);});
	$ourpredicate.blur(function() {window.setTimeout(validateMandatory, 100, $ourpredicate, ourpredicates);});
	$theirpredicate.blur(function() {window.setTimeout(validateMandatory, 100, $theirpredicate, theirpredicates);});

	/*
	* Applies the ui-state-error style to the container of {@param field}.
	* @param {object} field The DOM element which contains the error.
	*/
	function errorState(field) {
		field.removeClass("ui-state-success");
		field.parent().parent().addClass("ui-state-error");
		field.parent().next(".info").contents().first()
			.removeClass("ui-icon-help ui-icon-check")
			.addClass("ui-icon-alert")
			.show();
	}

	/*
	* Applies the ui-state-success style to the container of {@param field}.
	* @param {object} field The DOM element which is correct.
	*/
	function successState(field) {
		field.parent().parent().removeClass("ui-state-error");
		field.addClass("ui-state-success");
		field.parent().next(".info").contents().first()
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
		field.parent().parent().removeClass("ui-state-error");
		field.parent().next(".info").contents().first()
			.removeClass("ui-icon-help ui-icon-alert ui-icon-check")
			.hide();
	}



	function isEmpty(str) {
		return (!str || 0 === str.length);
	}

	function isValid(val, values) {
		return !isEmpty(val) && jQuery.inArray(val, values) != -1;
	}

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

	function validateAll() {
		// We have to check fields separately in order to mark the errors.
		var od = validateMandatory($ourds, datasets);
		var td = validateMandatory($theirds, datasets);
		var op = validateMandatory($ourpredicate, ourpredicates);
		var tp = validateMandatory($theirpredicate, theirpredicates);

		// Require user confirmation if the data is going to be modified permanently.
		var ret = od && td && op && tp
					 && validateOptional($ourclass, ourclasses)
					 && validateOptional($theirclass, theirclasses);

		return ret;
	}

	/* Display all help fields */
	$("#convert-help").click(function (event) {
		$('.help-js').slideToggle(50);
		event.preventDefault();
	});

	/* Display all example fields and populate inputs */
	$("#convert-example").click(function (event) {
		if(!$('.example-js').is(":visible")) {
			$('input:text').each(function () {
				// We save our placeholders and current value.
				$(this).attr('data-placeholder', $(this).attr('placeholder'));
				$(this).attr('placeholder', $(this).attr('data-example'));
				$(this).attr('data-val', $(this).val());
				$(this).val('');
				$(this).attr('disabled',true);
				successState($(this));
			});
		}
		else {
			$('input:text').each(function () {
				$(this).attr('placeholder', $(this).attr('data-placeholder'));
				$(this).val($(this).attr('data-val'));
				$(this).attr('disabled',false);
				defaultState($(this));
			});

		}
		
		$('.example-js').slideToggle(50);
		$('input:submit').attr('disabled', !$('input:submit').attr('disabled'));
		event.preventDefault();
	});

	$("#linkage-form").submit(function(){
		var update = $("input:radio[name=update]:checked").val();
		var ok = validateAll()
					&& (update === "preview"
					|| (update === "new" && !isEmpty($("#newpredicate").val()))
					|| confirm(confirmationMessage));

		if (ok) {
			// To avoid forms being sent multiple times.
			$("#convert-submit").attr("disabled", true);
		}

	return ok;
	});

});

/*
 * An interface for a {@link ProjectModule project module} that uses Silk to
 * generate links between two datasets.
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
					select.attr("tabindex", -10);
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

	$(".address-js select, .property-first-js select, .property-secund-js select, .property-third-js select").combobox();
	$(".measure-js .num-js select").combobox();

	$("#file-submit, #form-submit, .cancel-button, #form-help").button();
	$(".multiple-choices").buttonset();
	$('.add-compare-js, .remove-compare-js').button();

		/*
	* Applies the ui-state-error style to the container of {@param field}.
	* @param {object} field The DOM element which contains the error.
	*/
	function errorState(field) {
		field.removeClass("ui-state-success");
		field.parent().parent().addClass("ui-state-error");
		field.parent().next(".info").children('span')
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
		field.parent().next(".info").children('span')
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
		field.parent().next(".info").children('span')
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
	* Checks if the optional input {@param field} is valid by looking for its value inside a given array.
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

	function validateUnknown(field) {
		var str = field.val().trim();
		if (!isEmptyOptional(str)) {
			valid = true;
			successState(field);
		}
		else {
			defaultState(field);
		}
		return valid;
	}

	/*
	* Validation for the script upload form. Only checks filename.
	* @return True if fields are full and file extension is XML.
	*/
	$("#file-form").submit(function(){
		// We only have access to the file name (incl. extension).
		var configFile = $("#configFile").val().trim();
		var linkSpecId = $("#linkSpecId").val().trim();
		var configFileExt = configFile.substr(configFile.lastIndexOf(".") + 1);

		return !isEmpty(configFile) && !isEmpty(linkSpecId) && configFileExt === "xml";
	});

	/**
	* Adds more comparison fields.
	* TODO : 'd be nice to dinamically change required and placeholder
	* attribute to make them mandatory.
	* @return Always false.
	*/
	$(".add-compare-js").click(function() {
		if ($(".compare-js.secund-js").is(":visible")) {
			$('.compare-js.third-js').show();
			$('.measure-js.third-js').show();
			$(".add-compare-js").hide();
		}
		else {
			$('.compare-js.secund-js').show();
			$('.measure-js.secund-js').show();
			$(".remove-compare-js").show();
			$('.aggregation-js').parent().show();
		}
		
		return false;
	});

	/**
	* Removes the new comparison fields.
	* TODO : 'd be nice to dynamically change required and placeholder
	* attribute to make them mandatory.
	* @return Always false.
	*/
	$(".remove-compare-js").click(function() {
		if ($(".compare-js.third-js").is(":visible")) {
			$('.compare-js.third-js').hide();
			$('.measure-js.third-js').hide();
			$('.add-compare-js').show();
		}
		else {
			$('.compare-js.secund-js').hide();
			$('.measure-js.secund-js').hide();
			$(".remove-compare-js").hide();
			$('.aggregation-js').parent().hide();
		}
		
		return false;
	});

	/*
	* Displays optional fields and information according to the selected value.
	* @return Does not return anything.
	*/
	$('.metric-js').change(function() {
		var val = this.value;
		if (!isEmpty(val)) {
			// Shows the optional fields according to the value.
			$(this).parent().nextAll(".hidden-field-js").hide();
			$(this).parent().nextAll("." + val + "-js").show();
			// Shows the description for the chosen distance measure.
			$(".metric-info-js").children().hide();
			$("#" + val + "-info-js").show();
		}
	});

	/*
	* Displays optional fields according to the selected value.
	* @return Does not return anything.
	*/
	$('.transform-js').change(function() {
		var val = this.value;
		if (!isEmpty(val)) {
			// Shows the optional fields according to the value.
			$(this).parent().nextAll(".hidden-field-js").hide();
			$(this).parent().nextAll("." + val + "-js").show();
		}
	});

	/*
	* Hides fields according to the selected value.
	* @return Does not return anything.
	*/
	$('.aggregation-js').change(function() {
		var val = this.value;
		if (!isEmpty(val) && (val === "max" || val === "min")) {
			$(".weight-js").hide();
		}
		else {
			$(".weight-js").show();
		}
	});

	/* Displays the help fields. */
	$("#form-help").click(function (event) {
		$('.help-js').slideToggle(50);
		event.preventDefault();
	});

	$("#script-form").submit(function(){
		//TODO Client-side validation.
		return true;
	});



	var $sourceAddress = $(".source .address-js input");
	var $targetAddress = $(".target .address-js input");
	var $sourceQuery = $("#sourceQuery");
	var $targetQuery = $("#targetQuery");
	var $sourcePropertyFirst  = $(".source .property-first-js input");
	var $sourcePropertySecund = $(".source .property-secund-js input");
	var $sourcePropertyThird  = $(".source .property-third-js input");
	var $targetPropertyFirst  = $(".target .property-first-js input");
	var $targetPropertySecund = $(".target .property-secund-js input");
	var $targetPropertyThird  = $(".target .property-third-js input");
	var $thresholdFirst  = $("#thresholdFirst");
	var $thresholdSecund = $("#thresholdSecund");
	var $thresholdThird  = $(".measure-js .num-js input");
	var $weightFirst  = $("#weightFirst");
	var $weightSecund = $("#weightSecund");
	var $weightThird  = $("#weightThird");

	$sourceAddress.blur(function() {window.setTimeout(validateMandatory, 100, $sourceAddress, datasets);});
	$targetAddress.blur(function() {window.setTimeout(validateMandatory, 100, $targetAddress, datasets);});
	$sourcePropertyFirst.blur(function() {window.setTimeout(validateMandatory, 100, $sourcePropertyFirst, predicates);});
	$targetPropertyFirst.blur(function() {window.setTimeout(validateMandatory, 100, $targetPropertyFirst, predicates);});
	$sourcePropertySecund.blur(function() {window.setTimeout(validateMandatory, 100, $sourcePropertySecund, predicates);});
	$targetPropertySecund.blur(function() {window.setTimeout(validateMandatory, 100, $targetPropertySecund, predicates);});
	$sourcePropertyThird.blur(function() {window.setTimeout(validateMandatory, 100, $sourcePropertyThird, predicates);});
	$targetPropertyThird.blur(function() {window.setTimeout(validateMandatory, 100, $targetPropertyThird, predicates);});
	$thresholdFirst.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$thresholdSecund.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$thresholdThird.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$weightFirst.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$weightSecund.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$weightThird.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
});

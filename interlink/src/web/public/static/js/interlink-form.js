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

$(document).ready(function() {
	$("#file-submit, #form-submit, .cancel-button, #form-help").button();
	$(".multiple-choices").buttonset();
	$('.add-compare-js, .remove-compare-js').button();

	$(".hidden-field-js").hide();

	/*
	* Checks if {@param str} is empty, null or undefined.
	* @param {string} str The string to be checked.
	* @return {bool} True if real string, false if empty ("") or null/undefined.
	*/
	function isEmpty(str) {
		return (!str || 0 === str.length);
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

	var $sourceAddress = $("#sourceAddress");
	var $targetAddress = $("#targetAddress");
	var $sourceQuery = $("#sourceQuery");
	var $targetQuery = $("#targetQuery");
	var $sourcePropertyFirst  = $("#sourcePropertyFirst");
	var $sourcePropertySecund = $("#sourcePropertySecund");
	var $sourcePropertyThird  = $("#sourcePropertyThird");
	var $targetPropertyFirst  = $("#targetPropertyFirst");
	var $targetPropertySecund = $("#targetPropertySecund");
	var $targetPropertyThird  = $("#targetPropertyThird");
	var $thresholdFirst  = $("#thresholdFirst");
	var $thresholdSecund = $("#thresholdSecund");
	var $thresholdThird  = $("#thresholdThird");
	var $weightFirst  = $("#weightFirst");
	var $weightSecund = $("#weightSecund");
	var $weightThird  = $("#weightThird");
	
	$sourceAddress.autocomplete({source: datasets, minLength: 0, delay: 0});
	$targetAddress.autocomplete({source: datasets, minLength: 0, delay: 0});
	$sourceQuery.autocomplete({source: queries, minLength: 0, delay: 0});
	$targetQuery.autocomplete({source: queries, minLength: 0, delay: 0});
	$sourcePropertyFirst.autocomplete({source: predicates, minLength: 0, delay: 0});
	$sourcePropertySecund.autocomplete({source: predicates, minLength: 0, delay: 0});
	$sourcePropertyThird.autocomplete({source: predicates, minLength: 0, delay: 0});
	$targetPropertyFirst.autocomplete({source: predicates, minLength: 0, delay: 0});
	$targetPropertySecund.autocomplete({source: predicates, minLength: 0, delay: 0});
	$targetPropertyThird.autocomplete({source: predicates, minLength: 0, delay: 0});
	$thresholdFirst.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$thresholdSecund.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$thresholdThird.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$weightFirst.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$weightSecund.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
	$weightThird.autocomplete({source: zeroToOne, minLength: 0, delay: 0});
});

/*!
 * Common Javascript functions for simple converters.
 */

function getTargetSrcName(parentSrc, count) {
	if (parentSrc.match(/\(RDF #[0-9-]+\)/)) {
		return parentSrc.replace(/\((RDF #[0-9-]+)\)/, "(\$1-" + count + ")");
	}
	else {
		return parentSrc + " (RDF #" + count + ")";
	}
}

function getTargetGraphUri(parentUri, count) {
	var sep = "-";
	if (parentUri.endsWith("/")) sep = "";
	return parentUri + encodeURIComponent(sep + count);
}

function getTargetBaseUri(parentUri, count) {
	var sep = "-";
	if (parentUri.endsWith("/")) sep = "";
	var suffix = "";
	var n = parseInt(count, 10);
	if (isNaN(n) || n > 1) {
		suffix = encodeURIComponent(sep + count);
	}
	return parentUri.replace(/\/project\//g, "/")
	                .replace(/\/source\//g, "/") + suffix;
}


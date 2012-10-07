/*
 * An interface for a {@link ProjectModule project module} that replaces RDF object fields
 * from a RDF data by URIs to RDF entities.
 * New RDF triples shown using jquery's jqgrid plugin and L. Bihanic's jqgrid.common code.
 *
 * @author tcolas
 */

$(document).ready(function() {

	$("#goto_workspace").button();

	htmlTableTojQGrid();
	jQuery("#grid").jqGrid('setColProp', '#', { sorttype:'int' });
});

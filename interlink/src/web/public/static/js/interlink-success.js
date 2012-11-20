/*
 * An interface for a {@link ProjectModule project module} that uses Silk to
 * generate links between two datasets.
 * Autocompletion using jQuery, custom-made form validation.
 * 
 * @author tcolas
 */

$(document).ready(function() {

	$("#goto_workspace").button();

	htmlTableTojQGrid();
	jQuery("#grid").jqGrid('setColProp', '#', { sorttype:'int' });
});

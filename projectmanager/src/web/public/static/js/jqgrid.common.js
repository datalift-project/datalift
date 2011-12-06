var gridData = new Array();
var searchData = new Array();
var gridHeader = new Array();

function getHeaderColumns() {
	var cols = $(".dl-grid-header-column");
	for ( var i = 0; cols[i]; i++) {
		alert(cols[i].innerHTML);
		gridHeader[i] = normalizeHeaderName(cols[i].innerHTML);
	}
}

function normalizeHeaderName(colTitle) {
	return colTitle.replace(/ /g, "_").replace(/&/g, "&amp;").replace(/</g,
			"&lt;").replace(/>/g, "&gt;");
}

function gridSearch(searchString) {
	searchData = [];
	if ($("#searchColumn").val() == "all") {
		searchAllColumns(searchString);
	} else {
		searchOneColumn(searchString, $("#searchColumn").val());
	}
	$("#grid").jqGrid('clearGridData', true);
	$("#grid").jqGrid('setGridParam', {
		data : searchData
	}).trigger("reloadGrid");

}

function searchAllColumns(searchString) {
	if (searchString != "") {
		var c = 0;
		var obj = gridHeader;
		for ( var i = 0; i < gridData.length; i++) {
			for ( var k = 0; k < obj.length; k++) {
				var toCmp = gridData[i][obj[k]];
				if ((toCmp != undefined)
						&& (toCmp.toLowerCase().indexOf(searchString) != -1)) {
					searchData[c] = gridData[i];
					c++;
					k = obj.length;
				}
			}
		}
	} else
		searchData = gridData;
}

function searchOneColumn(searchString, searchColumn) {
	if (searchString != "") {
		var p = normalizeHeaderName(searchColumn);
		var c = 0;
		for ( var i = 0; i < gridData.length; i++) {
			var toCmp = gridData[i][p];
			if ((toCmp != undefined)
					&& (toCmp.toLowerCase().indexOf(searchString) != -1)) {
				searchData[c] = gridData[i];
				c++;
			}
		}
	} else
		searchData = gridData;
}

function htmlTableTojQGrid(lineColumn) {
		getHeaderColumns();
		jQuery.extend(jQuery.jgrid.defaults, {
			caption: $('#searchnav').html(),
			rowNum: 1000000,
			height: 450,
			search:{
				caption: '#i18n("grid.search.value.label")'
			},
		});
		var params = { 
	 			datatype: "local", 
	 			pager: $('#pagernav'), 
	 			rowNum: 200,
				rowList: [50,100,200,500,1000,2000],
		}
		if (lineColumn == true) {
			params.colModel = [{ 
				name: '#i18n("source.grid.row.heading")', 
				index: '#i18n("source.grid.row.heading")', 
				sorttype:'int', }];
		}
		tableToGrid("#grid", params);
		$("#grid").trigger("reloadGrid");
		gridData = $("#grid").getRowData();
		// Function called when modifying search text
		$("#gridsearch").keyup(function () {
			gridSearch($(this).val().toLowerCase());
		});
		// Function called when modifying search Column
		$("#searchColumn").change(function() {
			gridSearch($("#gridsearch").val().toLowerCase());
		});
}
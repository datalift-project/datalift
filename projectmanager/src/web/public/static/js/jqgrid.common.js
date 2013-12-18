var grid;

function getBasicTableParameters(){
	return {
	 	pager: '#pagernav', 
	 	caption: $('#searchnav').html(),
	 	rowNum: 50,
	 	rownumbers: true,
		rowList: [50,100,200,500,1000,2000,5000],
		ignoreCase: true,
		gridview: true,
		autowidth: true,
		shrinkToFit: true,
		height: '450'
	};
}

function optionalLinkFormatter(cellvalue, options, rowObject){
	if(cellvalue.substring(0,5)=="http:"){
		return "<a href=" + cellvalue + ">" + cellvalue + "</a>";
	}else{
		return cellvalue;
	}
}


function getTableParameters(columns, content){
	var tableColModel = [];
	for(var i = 0; i < columns.length ;i++){
		tableColModel[i]= [];
		tableColModel[i].name = columns[i];
	}
	var emptyTableParams = getBasicTableParameters();
	var contentTableParams = {
		colModel: tableColModel,
		colNames: columns,
		data: content,
		datatype: "local"
	};
	var merged = jQuery.extend(emptyTableParams, contentTableParams);
	return merged;
	
}


function stringToArray(arrString){
	var values = arrString.substring(1,arrString.length-1);
	return values.split(", ");
}


function gridSearch(searchString, column) {
	var postDataHandler = grid.jqGrid("getGridParam","postData");
	var optionSelected = $("#searchColumn :selected").index();
	if (optionSelected == 0) {
		searchAllColumns(searchString,postDataHandler);
	} else {
		searchOneColumn(searchString, column, postDataHandler);
	}
	grid.trigger("reloadGrid",[{page:1,current:true}]);
}


function searchAllColumns(searchString,postDataHandler) {
	var searchFilters = {groupOp: "OR", rules: []};
	$("#searchColumn option").each(
		function(){	
			if($(this).val() != "all"){
				var rule = new Object();
				rule.field = $(this).text();
				rule.op = "cn";
				rule.data = searchString;
				searchFilters.rules.push(rule);
			}
		}
	);
	jQuery.extend(postDataHandler,
	{
		filters: searchFilters
	});
	grid.jqGrid('setGridParam', { search: true, postData: postDataHandler });
}

function searchOneColumn(searchStr, searchColumn, postDataHandler) {
	jQuery.extend(postDataHandler,
		{
			filters: '',
			searchField: searchColumn,
			searchOper: 'cn',
			searchString: searchStr
		});
	grid.jqGrid('setGridParam', { search: true, postData: postDataHandler });
}

//Show the table, set the proper width and enable the search functionalities
function enableTable(table) {
	grid = table;
	$("#results").show();
	grid.setGridWidth($("#results").width());
	// Function called when clearing the text or pressing enter
	$("#gridsearch").bind("search", function(){
		runSearch();
	});		
	// Function called when text is entered
	$("#gridsearch").keyup(function(){
		runSearch();
	});
	// Function called when modifying search Column
	$("#searchColumn").change(function() {
		runSearch();
	});
}

function runSearch(){
	gridSearch($("#gridsearch").val(), $("#searchColumn").val());
}
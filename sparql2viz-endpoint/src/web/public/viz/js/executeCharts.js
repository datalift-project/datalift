//-- TO DO (REMOVE) --//
//alert("${baseUri}/sparql/viz");
/**
 * Return the position of the first 
 * element in Array who contains the string
 *
 * @method isExist
 * @param {String} str
 * @param {Array} arr
 * @return {Number} position of element, else -1
 * @autor Zakaria Khattabi
 **/
function isExist(str, arr){
	return arr.indexOf(str);
}
/**
 * Return the language
 *
 * @method getLang
 * @return {String} "FR"/"EN"
 * @autor Zakaria Khattabi
 **/
function getLang(){
	var userLang = "";
	($('#print').val()=="Print")?userLang="EN":userLang="FR";
	return userLang;
}
/**
 * Return the average of a table like [[],[],...,[]] 
 * element in Array who contains the string
 *
 * @method avg
 * @param {Array} tab
 * @param {Number} index
 * @return {Number} a float
 * @autor Zakaria Khattabi
 **/
function avg(tab, index){
	var sum=0.0;
	var length=tab.length;
	for (var i=0; i<length; i++) sum+=parseFloat(tab[i][index]);
	//alert(typeof tab[0][index]);
	return sum/length;
}
/**
 * Parse string table to table number
 *
 * @method toNbr
 * @param {Array} tab
 * @return {Array} table parsing to number
 * @autor Zakaria Khattabi
 **/
function toNbr(tab){
	for (var i=0; i<tab[0].length; i++){
		for (var j=0; j<tab.length; j++){
			tab[i][j] = parseFloat(tab[i][j]);
		}
	}
	return tab;
}
/**
 * Launch all charts of Sgvizler
 *
 * @method launchSgvizler
 * @param {Object} data (JSON result)
 * @param {Number} key
 * @autor Zakaria Khattabi
 **/
function launchSgvizler(data ,key){
	$("#graphs_get_description").html("<div id=\"sgVzl\" data-sgvizler-query=\""+$("#query").val().trim()+"\" data-sgvizler-chart=\""+data.graphs[key].dataSgvizlerChart+"\" style=\"width:100%;height:100%;\"></div>");
	//Launching sgvizler
	var sparqlQueryString = $("#query").val().trim(),
	containerID = "sgVzl",
	Q = new sgvizler.Query(),
	sel = $('#default-graph-uri-visu').val(),
	var url = "http://localhost:9091/sparql?default-graph-uri="+sel;
	//Note that default values may be set in the sgvizler object.
	Q.query(sparqlQueryString)
	.endpointURL(url)
	.endpointOutputFormat("json")                    // Possible values 'xml', 'json', 'jsonp'.
	.chartFunction(data.graphs[key].dataSgvizlerChart)  // The name of the function to draw the chart.
	.chartHeight(Math.floor($("#graphs_get_description").height()))
	.chartWidth(Math.floor($("#graphs_get_description").width()))
	.draw(containerID)
	.loglevel("2");
}
/**
 * Launch the chart of Leaflet
 *
 * @method launchLeaflet
 * @autor Zakaria Khattabi
 **/
function launchLeaflet(){
	// $("#graphs_get_description").html("<div id=\"map\" style=\"width:100%;height:100%;\"></div>");
	visu.createContainer("graphs_get_description", "map");
	var t = [],
	q = $("#query").val(),
	sel = $('#default-graph-uri-visu').val();
	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					t.push(new Array());
					$.each(val, function(k, sub) {
						t[key].push(sub.value);
					});
				});

				var avLat = avg(t, 0);
				var avLon = avg(t, 1);

				var map = L.map('map').setView([avLat, avLon], 13);

				L.tileLayer('https://api.tiles.mapbox.com/v4/{id}/{z}/{x}/{y}.png?access_token={accessToken}', {
					attribution: 'Map data &copy; <a href="http://openstreetmap.org">OpenStreetMap</a> contributors, <a href="http://creativecommons.org/licenses/by-sa/2.0/">CC-BY-SA</a>, Imagery © <a href="http://mapbox.com">Mapbox</a>',
					maxZoom: 18,
					id: 'dlift.b4e0fe33',
					accessToken: 'pk.eyJ1IjoiZGxpZnQiLCJhIjoiMGRhOTQyM2NmNjkyN2E5ZDliYjdlYjhmZjk0MmRkMzQifQ.Jr2TI6X6zPRjpQAsYhX_PQ'
				}).addTo(map);

				for (var i = 0; i < t.length; i++) {
					marker = new L.marker([t[i][0], t[i][1]])
					.bindPopup(t[i][2])
					.addTo(map);
				}
			}
		},
	});
}
/**
 * Launch the chart of bubble of Highcharts
 *
 * @method launchHBubblesChart
 * @autor Zakaria Khattabi
 **/
function launchHBubblesChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					// var d = {};
					var temp = [];
						$.each(val, function(k, sub) {
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									// d[k] = subSub;
									temp.push(subSub);
								}
							});
						});
					// t.push(d);
					t.push(temp);
				});

				console.log(t);
				toNbr(t);

				$(function () {
					$('#container').highcharts({
						title: {
							text: 'HBubblesChart',
							x: -20 //center
						},

						chart: {
							type: 'bubble',
							zoomType: 'xy'
						},

						series: [{
							data: t
						}, ]
					});
				});
			}
		}
	});
}
/**
 * Launch the chart of lines labels of Highcharts
 *
 * @method launchHlinesLabelsChart
 * @autor Zakaria Khattabi
 **/
function launchHlinesLabelsChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					// var d = {};
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									// d[k] = subSub;
									temp.push(subSub);
								}									
							});
						});
					// t.push(d);
					t.push(temp);
				});

				console.log(t);
				
				var tab = [];
				for(var i=0; i<t[0].length; i++){
					var d = {};
					var v = [];
					for(var j=0; j<t.length; j++){
						v.push(parseFloat(t[j][i]));
					}
					d["data"] = v;
					d["name"] = varNames[i];
					tab.push(d);
				}
				console.log(tab);
				
				$(function() {
					$('#container').highcharts({
						title: {
							text: 'HlinesLabelsChart',
							x: -20 //center
						},
						chart: {
							type: 'line'
						},
						plotOptions: {
							line: {
								dataLabels: {
									enabled: true
								},
								enableMouseTracking: false
							}
						},
						series: tab
					});
				});
			}
		}
	});
}
/**
 * Launch the chart of lines of Highcharts
 *
 * @method launchHLinesChart
 * @autor Zakaria Khattabi
 **/
function launchHLinesChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					// var d = {};
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									// d[k] = subSub;
									temp.push(subSub);
								}									
							});
						});
					// t.push(d);
					t.push(temp);
				});

				console.log(t);
				
				var tab = [];
				for(var i=0; i<t[0].length; i++){
					var d = {};
					var v = [];
					for(var j=0; j<t.length; j++){
						v.push(parseFloat(t[j][i]));
					}
					d["data"] = v;
					d["name"] = varNames[i];
					tab.push(d);
				}
				console.log(tab);
				
				$(function() {
					$('#container').highcharts({
						title: {
							text: 'HLinesChart',
							x: -20 //center
						},
						legend: {
							layout: 'vertical',
							align: 'right',
							verticalAlign: 'middle',
							borderWidth: 0
						},
						series: tab
					});
				});
			}
		}
	});
}
/**
 * Launch the chart of logarithmic of Highcharts
 *
 * @method launchHLogarithmicChart
 * @autor Zakaria Khattabi
 **/
function launchHLogarithmicChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});

				console.log(t);
				
				var tab = [];
				for(var i=0; i<t[0].length; i++){
					var d = {};
					var v = [];
					for(var j=0; j<t.length; j++){
						v.push(parseFloat(t[j][i]));
					}
					d["data"] = v;
					d["name"] = varNames[i];
					tab.push(d);
				}
				console.log(tab);
				
				$(function() {
					$('#container').highcharts({
						title: {
							text: 'HLogarithmicChart'
						},

						xAxis: {
							tickInterval: 1
						},

						yAxis: {
							type: 'logarithmic',
							minorTickInterval: 0.1
						},

						tooltip: {
							headerFormat: '<b>{series.name}</b><br />',
							pointFormat: 'x = {point.x}, y = {point.y}'
						},

						series: tab
					});
				});
			}
		}
	});
}
/**
 * Launch the chart of times series of Highcharts
 *
 * @method launchHTimesSeries
 * @autor Zakaria Khattabi
 **/
function launchHTimesSeries(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});

				console.log(t);
				
				var tab = [];
				for(var i=0; i<1; i++){
					var d = {};
					var v = [];
					for(var j=0; j<t.length; j++){
						v.push(parseFloat(t[j][i]));
					}
					d["data"] = v;
					d["type"] = "area";
					d["pointInterval"] = 24 * 3600 * 1000;
					d["name"] = varNames[i];
					tab.push(d);
				}
				console.log(tab);
				
				$(function() {
					$('#container').highcharts({
						title: {
							text: 'HTimesSeries'
						},
						chart: {
							zoomType: 'x'
						},
						xAxis: {
							type: 'datetime',
							minRange: 14 * 24 * 3600000 // fourteen days
						},
						legend: {
							enabled: false
						},
						plotOptions: {
							area: {
								fillColor: {
									linearGradient: { x1: 0, y1: 0, x2: 0, y2: 1},
									stops: [
										[0, Highcharts.getOptions().colors[9]],
										[1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
									]
								},
								marker: {
									radius: 2
								},
								lineWidth: 1,
								states: {
									hover: {
										lineWidth: 1
									}
								},
								threshold: null
							}
						},
						series: tab
					});
				});
			}
		}
	});
}
/**
 * Launch the chart of OpenLayers map chart
 *
 * @method launchOMapChart
 * @autor Zakaria Khattabi
 **/
function launchOMapChart(){
	visu.createContainer("graphs_get_description", "map");
	
	(function(){
		var t = [];
		var q = $("#query").val();
		var sel = $('#default-graph-uri-visu').val();
		
		$.ajax({
			url : '${baseUri}/sparql',
			type : 'GET',
			data : {
				'default-graph-uri' : sel,
				'query' : q,
			},
			dataType : 'json',
			error : function(result, status, error){
				if(getLang()=="EN"){
				alert("Failure to recovery parameters");
				} else {
					alert("Echec de récupération des paramètres");
				}
			},
			success : function(json, status){
				var nVars = json.head.vars.length;
				if (nVars>1 || nVars<4){
					$.each(json.results.bindings, function(key, val) {
						t.push(new Array());
						$.each(val, function(k, sub) {
							t[key].push(sub.value);
						});
					});
					console.log(t);

					var avLat = avg(t, 0);
					var avLon = avg(t, 1);
					
					//create empty vector
					var vectorSource = new ol.source.Vector({ 
					});
					
					// create a bunch of icons and add to source vector
					for (var i=0;i<t.length;i++){
						var iconFeature = new ol.Feature({
						  geometry: new  
							ol.geom.Point(ol.proj.transform([parseFloat(t[i][1]), parseFloat(t[i][0])], 'EPSG:4326','EPSG:3857')),
						  name: ' ' + i,
						});
						vectorSource.addFeature(iconFeature);
					}
					//create the style
					var iconStyle = new ol.style.Style({
					  image: new ol.style.Icon(/** @type {olx.style.IconOptions} */ ({
						anchor: [0.5, 46],
						anchorXUnits: 'fraction',
						anchorYUnits: 'pixels',
						// opacity: 0.75,
						src: '${baseUri}/images/legacy-vizu/icons/Map-Marker.png'
					  }))
					});

					//add the feature vector to the layer vector, and apply a style to whole layer
					var vectorLayer = new ol.layer.Vector({
					  source: vectorSource,
					  style: iconStyle
					});
					var layerSat = new ol.layer.Tile({
						source: new ol.source.BingMaps({
									key: 'Ak-dzM4wZjSqTlzveKz5u0d4IQ4bRzVI309GxmkgSVr1ewS6iPSrOvOKhA-CJlm3',
									imagerySet: 'AerialWithLabels'	
						})	
					});
					var map = new ol.Map({
					  layers: [layerSat, vectorLayer],
					  target: document.getElementById('map'),
					  view: new ol.View({
						center: [parseFloat(avLat), parseFloat(avLon)],
						// center: [0, 0],
						zoom: 3
					  })
					});
				}
			}
		});
	})();
}
/**
 * Show the sub-group for each category who it's selected
 *
 * @method showSubGroupBy
 * @autor Zakaria Khattabi
 **/
function showSubGroupBy(){
	if ($("#selectGroupBy").val()==""){
		// $("#subGroupBy").css("display","none");
		// $('#subGroupBy select option:eq(0)').prop('selected', true);
		var appElement = document.querySelector('[ng-app=filterApp]');
		var $scope = angular.element(appElement).scope();
		$scope.$apply(function(){
			$scope.subs = [];
		});
		console.log($('#subGroupBy select option:eq(0)').val());
	} else {
		$("#subGroupBy").css("display","inline-block");
		
		var appElement = document.querySelector('[ng-app=filterApp]');
		var scope = angular.element(appElement).scope();
		scope.$apply(function(){
			$.ajax({
				url : '${baseUri}/listgraphsinformations.js/',
				type : 'GET',
				dataType : 'json',
				success : function(json, status){
					var val = $("#selectGroupBy").val();
					scope.subs = #i18n("sparql.endpoint.getSubGategories");
					// console.log($('#subGroupBy select option:eq(0)').val());
				}
			});
		});
	}
}
/**
 * Enable to show just graph controls
 * TODO :
 * 		To delete
 *
 * @autor Zakaria Khattabi
 **/
$("#enableGraphControl").click(
	function(){
		$('#graphs_content').css('display','none');
		$("#control_graphs_content").css("display","block");
	}
);
/**
 * Launch the times series of Highcharts
 *	TODO : to modify
 *	TODO : to delete
 *
 * @method launchHTMSeriesChart
 * @autor Zakaria Khattabi
 **/
function launchHTMSeriesChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});

				console.log(t);
				
				var tt = [];
				for(var i=0; i<t[0].length; i++){
					var temp = [];
					for(var j=0; j<t.length; j++){
						temp.push(parseFloat(t[j][i]));
					}
					tt.push(temp);
				}
				
				console.log(tt);
				
				var seriesOptions = [];
				for(var i=0; i<tt.length; i++){
					var d = {};
					d["data"] = tt[i];
					// d["type"] = "area";
					// d["pointInterval"] = 24 * 3600 * 1000;
					d["name"] = varNames[i];
					seriesOptions.push(d);
				}
				console.log(seriesOptions);
				console.log("tt 0 : "+tt[0]);

				$(function() {
					// $('#container').highcharts('StockChart', {
						// plotOptions: {
							// series: {
								// compare: 'percent'
							// }
						// },

						// series: [{name:"x", data:tt[0]}]
						// series: seriesOptions
					// });
					
					
					 var chart = new Highcharts.StockChart({
						chart: {
							renderTo: 'container'
						},
						series: [{
							name: 'USD to EUR',
							data: [1,2,3]
						}]
					});
				});
			}
		}
	});
}
/**
 * Launch bar basic chart of Highcharts
 *
 * @method launchHBarBasicChart
 * @autor Zakaria Khattabi
 **/
function launchHBarBasicChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				var tt = [];
				for(var i=0; i<t[0].length; i++){
					var temp = [];
					for(var j=0; j<t.length; j++){
						temp.push(parseFloat(t[j][i]));
					}
					tt.push(temp);
				}	
				console.log(tt);		
				var seriesOptions = [];
				for(var i=0; i<tt.length; i++){
					var d = {};
					d["data"] = tt[i];
					d["name"] = varNames[i];
					seriesOptions.push(d);
				}
				console.log(seriesOptions);		
				configHBarBasic(seriesOptions);
			}
		}
	});
}
/**
 * Configuration to launch bar basic chart
 *
 * @method configHBarBasic
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configHBarBasic(seriesOptions){
	$(function () {
		$('#container').highcharts({
			chart: {
				type: 'bar'
			},
			title: {
				text: 'HBarBasicChart'
			},
			plotOptions: {
				bar: {
					dataLabels: {
						enabled: true
					}
				}
			},
			legend: {
				layout: 'vertical',
				align: 'right',
				verticalAlign: 'top',
				x: -40,
				y: 80,
				floating: true,
				borderWidth: 1,
				backgroundColor: ((Highcharts.theme && Highcharts.theme.legendBackgroundColor) || '#FFFFFF'),
				shadow: true
			},
			credits: {
				enabled: false
			},
			series: seriesOptions
		});
	});
}
/**
 * Launch basic column chart
 *
 * @method launchHBasicColumnChart
 * @autor Zakaria Khattabi
 **/
function launchHBasicColumnChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				var tt = [];
				for(var i=0; i<t[0].length; i++){
					var temp = [];
					for(var j=0; j<t.length; j++){
						temp.push(parseFloat(t[j][i]));
					}
					tt.push(temp);
				}					
				console.log(tt);
				
				var seriesOptions = [];
				for(var i=0; i<tt.length; i++){
					var d = {};
					d["data"] = tt[i];
					d["name"] = varNames[i];
					seriesOptions.push(d);
				}
				console.log(seriesOptions);
				
				configHBasicColumn(seriesOptions);
			}
		}
	});
}
/**
 * Configuration to launch bar basic chart
 *
 * @method configHBasicColumn
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configHBasicColumn(seriesOptions){
	$(function () {
		$('#container').highcharts({
			chart: {
				type: 'column'
			},
			title: {
				text: 'HBasicColumn Chart'
			},
			series: seriesOptions
		});
	});	
}
/**
 * Launch column range chart
 *
 * @method launchColumnRangeChart
 * @autor Zakaria Khattabi
 **/
function launchColumnRangeChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				for(var i=0; i<t[0].length; i++){
					for(var j=0; j<t.length; j++){
						t[j][i] = parseFloat(t[j][i]);
					}
				}					
				console.log(t);
				
				var seriesOptions = [];
				var d = {};
				d["data"] = t;
				d["name"] = varNames[0];
				seriesOptions.push(d);
				console.log(seriesOptions);
				
				configHColumnRange(seriesOptions);
			}
		}
	});
}
/**
 * Configuration to launch bar basic chart
 *
 * @method configHColumnRange
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configHColumnRange(seriesOptions){
	$(function () {
		$('#container').highcharts({
			chart: {
				type: 'columnrange',
				inverted: true
			},
			title: {
				text: 'HColumnRange Chart'
			},
			plotOptions: {
				columnrange: {
					dataLabels: {
						enabled: true,
						formatter: function () {
							return this.y;
						}
					}
				}
			},
			legend: {
				enabled: false
			},
			series: seriesOptions
		});
	});
}
/**
 * Launch semi circle donut chart
 *
 * @method launchHSemiCircleDonutChart
 * @autor Zakaria Khattabi
 **/
function launchHSemiCircleDonutChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				for(var i=0; i<t[0].length; i++){
					for(var j=0; j<t.length; j++){
						if(i==1){
							t[j][i] = parseFloat(t[j][i]);
						} else {
							t[j][i] = t[j][i];
						}
					}
				}			
				console.log(t);
				
				var seriesOptions = [];
				var d = {};
				d["data"] = t;
				d["name"] = varNames[0];
				d["innerSize"] = '50%';
				d["type"] = 'pie';
				seriesOptions.push(d);
				console.log(seriesOptions);
				
				configSemiCircleDonutChart(seriesOptions);
			}
		}
	});
}
/**
 * Configuration semi circle donuts chart
 *
 * @method configSemiCircleDonutChart
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configSemiCircleDonutChart(seriesOptions){
	$(function () {
		$('#container').highcharts({
			chart: {
				plotBackgroundColor: null,
				plotBorderWidth: 0,
				plotShadow: false
			},
			title: {
				text: 'Semi circle donut',
				align: 'center',
				verticalAlign: 'middle',
				y: 40
			},
			tooltip: {
				pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
			},
			plotOptions: {
				pie: {
					dataLabels: {
						enabled: true,
						distance: -50,
						style: {
							fontWeight: 'bold',
							color: 'white',
							textShadow: '0px 1px 2px black'
						}
					},
					startAngle: -90,
					endAngle: 90,
					center: ['50%', '75%']
				}
			},
			series: seriesOptions
		});
	});
}
/**
 * Launch pie gradient fill chart
 *
 * @method launchHPieGradientFillChart
 * @autor Zakaria Khattabi
 **/
function launchHPieGradientFillChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				var seriesOptions = [];
				for(var i=0; i<t.length; i++){
					var d = {};
					d["y"] = parseFloat(t[i][1]);
					d["name"] = t[i][0];
					seriesOptions.push(d);
				}
				console.log(seriesOptions);
				configPieGradientFill(seriesOptions);
			}
		}
	});
}
/**
 * Configuration pie gradient fill
 *
 * @method configSemiCircleDonutChart
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configPieGradientFill(seriesOptions){
	$(function () {
		// Radialize the colors
		Highcharts.getOptions().colors = Highcharts.map(Highcharts.getOptions().colors, function (color) {
			return {
				radialGradient: {
					cx: 0.5,
					cy: 0.3,
					r: 0.7
				},
				stops: [
					[0, color],
					[1, Highcharts.Color(color).brighten(-0.3).get('rgb')] // darken
				]
			};
		});
		// Build the chart
		$('#container').highcharts({
			chart: {
				plotBackgroundColor: null,
				plotBorderWidth: null,
				plotShadow: false,
				type: 'pie'
			},
			title: {
				text: 'Pie gradient fill'
			},
			tooltip: {
				pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
			},
			plotOptions: {
				pie: {
					allowPointSelect: true,
					cursor: 'pointer',
					dataLabels: {
						enabled: true,
						format: '<b>{point.name}</b>: {point.percentage:.1f} %',
						style: {
							color: (Highcharts.theme && Highcharts.theme.contrastTextColor) || 'black'
						},
						connectorColor: 'silver'
					}
				}
			},
			series: [{
				name: "Pie gradient fill",
				colorByPoint: true,
				data:seriesOptions
			}]
		});
	});
}
/**
 * Launch 3D pie chart
 *
 * @method launchH3DPieChart
 * @autor Zakaria Khattabi
 **/
function launchH3DPieChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();
	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				for(var i=0; i<t[0].length; i++){
					for(var j=0; j<t.length; j++){
						if(i==1){
							t[j][i] = parseFloat(t[j][i]);
						} else {
							t[j][i] = t[j][i];
						}
					}
				}			
				console.log(t);
				
				var seriesOptions = [];
				var d = {};
				d["data"] = t;
				d["name"] = varNames[0];
				d["innerSize"] = '50%';
				d["type"] = 'pie';
				seriesOptions.push(d);
				console.log(seriesOptions);			
				config3DPieChart(seriesOptions);
			}
		}
	});
}
/**
 * Configuration 3D pie chart
 *
 * @method config3DPieChart
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function config3DPieChart(seriesOptions){
	$(function () {
		$('#container').highcharts({
			chart: {
				type: 'pie',
				options3d: {
					enabled: true,
					alpha: 45,
					beta: 0
				}
			},
			title: {
				text: 'Browser market shares at a specific website, 2014'
			},
			tooltip: {
				pointFormat: '{series.name}: <b>{point.percentage:.1f}%</b>'
			},
			plotOptions: {
				pie: {
					allowPointSelect: true,
					cursor: 'pointer',
					depth: 35,
					dataLabels: {
						enabled: true,
						format: '{point.name}'
					}
				}
			},
			series:seriesOptions
		});
	});
}
/**
 * Launch polar chart of Highcharts
 *
 * @method launchH3DPieChart
 * @autor Zakaria Khattabi
 **/
function launchHPolarChart(){
	var t = [];
	var type = ["column", "line", "area"]
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				var tt = [];
				for(var i=0; i<t[0].length; i++){
					var temp = [];
					for(var j=0; j<t.length; j++){
						temp.push(parseFloat(t[j][i]));
					}
					tt.push(temp);
				}	
				console.log(tt);
				
				var seriesOptions = [];
				for(var i=0; i<tt.length; i++){
					var d = {};
					d["data"] = tt[i];
					d["name"] = varNames[i];
					d["type"] = type[i];
					if(type[i]=="column")
						d["pointPlacement"] = 'between';
					seriesOptions.push(d);
				}
				console.log(seriesOptions);	
				configHPolar(seriesOptions);
			}
		}
	});
}
/**
 * Configuration polar chart of highcharts
 *
 * @method config3DPieChart
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configHPolar(seriesOptions){
	$(function (){
		$('#container').highcharts({
			chart: {
				polar: true
			},
			title: {
				text: 'Polar Chart'
			},
			pane: {
				startAngle: 0,
				endAngle: 360
			},
			xAxis: {
				tickInterval: 45,
				min: 0,
				max: 360,
				labels: {
					formatter: function () {
						return this.value + '°';
					}
				}
			},
			plotOptions: {
				series: {
					pointStart: 0,
					pointInterval: 45
				},
				column: {
					pointPadding: 0,
					groupPadding: 0
				}
			},
			series: seriesOptions
		});
	});
}
/**
 * Launch spider web chart of Highcharts
 *
 * @method launchHSpiderwebChart
 * @autor Zakaria Khattabi
 **/
function launchHSpiderwebChart(){
	var t = [];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});
				console.log(t);
				
				var tt = [];
				for(var i=0; i<t[0].length; i++){
					var temp = [];
					for(var j=0; j<t.length; j++){
						if(j==0)
							temp.push(t[j][i]);
						else
							temp.push(parseFloat(t[j][i]));
					}
					tt.push(temp);
				}	
				console.log(tt);
				
				var seriesOptions = [];
				for(var i=1; i<tt.length; i++){
					var d = {};
					d["data"] = tt[i];
					d["name"] = varNames[i];
					d["pointPlacement"] = 'on';
					seriesOptions.push(d);
				}
				console.log(seriesOptions);
				console.log(tt[0]);
				
				configSpiderweb(seriesOptions, tt[0]);
			}
		}
	});
}
/**
 * Configuration of spider web of highcharts
 *
 * @method configSpiderweb
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configSpiderweb(seriesOptions, cat){
	$(function () {
		$('#container').highcharts({
			chart: {
				polar: true,
				type: 'area'
			},
			title: {
				text: 'Spiderweb',
				x: -80
			},
			pane: {
				size: '80%'
			},
			xAxis: {
				categories: cat,
				tickmarkPlacement: 'on',
				lineWidth: 0
			},
			yAxis: {
				gridLineInterpolation: 'polygon',
				lineWidth: 0,
				min: 0
			},
			tooltip: {
				shared: true,
				pointFormat: '<span style="color:{series.color}">{series.name}: <b>{point.y:,.0f}</b><br/>'
			},
			series: seriesOptions
		});
	});
}
/**
 * Launch tree map levels chart of Highcharts
 *
 * @method launchHTreeMapLevelsChart
 * @autor Zakaria Khattabi
 **/
function launchHTreeMapLevelsChart(){
	var t = [];
	var type = ["name", "parent", "value"];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();

	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});					
				for(var i=0; i<t.length; i++){
					for(var j=0; j<t[0].length; j++){
						if(j==2)
							t[i][j] = parseInt(t[i][j]);
						else
							t[i][j] = t[i][j];
					}
				}
				console.log(t);	
				var seriesOptions = [];
				var parents = [];
				for(var i=0; i<t.length; i++){
					var d = {};
					for(var j=0; j<t[0].length; j++){
						if(j == 1 && isExist(t[i][j], parents)==-1){
							d["id"] = t[i][j];
							d["color"] = '#'+Math.floor(Math.random()*16777215).toString(16);
							parents.push(t[i][j]);
						}
					}
					seriesOptions.push(d);
				}
				console.log("parents : "+parents);
				for(var i=0; i<t.length; i++){
					var d = {};
					for(var j=0; j<t[0].length; j++){
						d[type[j]] = t[i][j];
					}
					seriesOptions.push(d);
				}
				console.log(seriesOptions);					
				configTreeMapLevels(seriesOptions);
			}
		}
	});
}
/**
 * Configuration of tree map levels of highcharts
 *
 * @method configTreeMapLevels
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configTreeMapLevels(seriesOptions){
	$(function () {
		$('#container').highcharts({
			series: [{
				type: "treemap",
				layoutAlgorithm: 'stripes',
				alternateStartingDirection: true,
				levels: [{
					level: 1,
					layoutAlgorithm: 'sliceAndDice',
					dataLabels: {
						enabled: true,
						align: 'left',
						verticalAlign: 'top',
						style: {
							fontSize: '15px',
							fontWeight: 'bold'
						}
					}
				}],
				data:seriesOptions 
			}],
			title: {
				text: 'HTreemap level'
			}
		});
	});
}
/**
 * Launch tree map chart of Highcharts
 *
 * @method launchHTreeMapChart
 * @autor Zakaria Khattabi
 **/
function launchHTreeMapChart(){
	var t = [];
	var type = ["name", "value", "colorValue"];
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();
	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			var nVars = json.head.vars.length;
			var varNames = [];
			if(json.results.bindings.length==0)
				alert("no results to show");
			if (nVars>1 || nVars<4){
				$.each(json.results.bindings, function(key, val) {
					var temp = [];
						$.each(val, function(k, sub) {
							varNames.push(k);
							$.each(sub, function(subK, subSub) {
								if(subK == "value"){
									temp.push(subSub);
								}									
							});
						});
					t.push(temp);
				});					
				for(var i=0; i<t.length; i++){
					for(var j=0; j<t[0].length; j++){
						if(j==0)
							t[i][j] = t[i][j];
						else
							t[i][j] = parseInt(t[i][j]);
					}
				}
				console.log(t);
				
				var seriesOptions = [];
				for(var i=0; i<t.length; i++){
					var d = {};
					for(var j=0; j<t[0].length; j++){
						d[type[j]] = t[i][j];
					}
					seriesOptions.push(d);
				}
				console.log(seriesOptions);					
				configTreeMap(seriesOptions);
			}
		}
	});
}
/**
 * Configuration of tree map of highcharts
 *
 * @method configTreeMap
 * @param {Object} seriesOptions
 * @autor Zakaria Khattabi
 **/
function configTreeMap(seriesOptions){
	$(function () {
		$('#container').highcharts({
			colorAxis: {
				minColor: '#FFFFFF',
				maxColor: Highcharts.getOptions().colors[0]
			},
			series: [{
				type: "treemap",
				layoutAlgorithm: 'squarified',
				data: seriesOptions
			}],
			title: {
				text: 'HTreemap'
			}
		});
	});
}
/**
 * Launch OpenLayers of Highcharts
 *
 * @method launchOL
 * @autor Zakaria Khattabi
 **/
function launchOL(){
	var q = $("#query").val();
	var sel = $('#default-graph-uri-visu').val();
	$.ajax({
		url : '${baseUri}/sparql',
		type : 'GET',
		data : {
			'default-graph-uri' : sel,
			'query' : q,
		},
		dataType : 'json',
		error : function(result, status, error){
			if(getLang()=="EN"){
				alert("Failure to recovery parameters");
			} else {
				alert("Echec de récupération des paramètres");
			}
		},
		success : function(json, status){
			visu.createContainer("graphs_get_description", "map");
			execute(json);
		}
	});
}
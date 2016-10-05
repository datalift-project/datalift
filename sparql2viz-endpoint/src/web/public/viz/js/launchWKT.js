//@author : Zakaria Khattabi
//Script for launching wkt map with Open Layer

function execute(data){
	(function(){
		var WKT = []; // Array contains lists of all WKT polygons extrated from SPARQL request
		var colorSelect = "#4ff7f7";
		var raster = new ol.layer.Tile({
						source: new ol.source.OSM()
					});
		var dynamicLayers = []; // Array contains all dynamics layers
		var layers = []; // Array contains all layers
		layers[0] = raster;	//First layer the default OSM map
		var varNames = [];
		if(data.results.bindings.length==0) {
			alert("no results to show");
		} else {
			varNames = data.head.vars;
			$.each(data.results.bindings, function(key, val) {	
				WKT.push(val['poly']['value']);
			});
		}

		// Define default style for polygons
		var style = new ol.style.Style({ 
			fill: new ol.style.Fill({
				color: colorSelect+"",
			}),
			stroke: new ol.style.Stroke({
				width: 5,
				color: "#0094ff"
			})
		});

		//Iterate dynamicly in all present polygons
		$.each(WKT,function(index,value){
			var feature = transformOP(value);
			var vector = createVector(feature, style);
			//on ajoute chaque couche de polygones dans le tableau des couches
			dynamicLayers.push(vector);
			layers.push(vector);
		});
		
		// Map configuration
		var map = new ol.Map({
			layers: layers,
			target: 'map',
			view: new ol.View({
				center: ol.proj.transform([0,0],'EPSG:4326','EPSG:3857'),
			zoom:4 
			})
		});
		// center the map in the existing vector
		var extent = ol.extent.createEmpty();
		dynamicLayers.forEach(function(layer) {
			ol.extent.extend(extent, layer.getSource().getExtent());
		});
		map.getView().fitExtent(extent, map.getSize());
		//Function to transformate WKT geometries to the good referentiel
		function transformOP(WKT) {
			var format = new ol.format.WKT();
			var feature = format.readFeature(WKT);
			feature.getGeometry().transform('EPSG:4326', 'EPSG:3857');
			return feature;
		}
		//Function to create vector for each polygon
		function createVector(feature, style){
			var vector = new ol.layer.Vector({
				style:style,
				source: new ol.source.Vector({
					features: [feature] 
				})
			});
			vector.setOpacity(0.5);
			return vector;
		}
	})();
}
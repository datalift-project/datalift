//@author : Zakaria Khattabi
//Script for launching wkt map with Open Layer

function execute(data){
	(function(){
		var WKT = [];
		var colorSelect = "#4ff7f7";
		var raster = new ol.layer.Tile({
						source: new ol.source.OSM()
					})
		var layers = new Array; //tableau contenant l'ensemble des couches
		layers[0] = raster;	//la première couche inseré est la carte 
		var WKT = new Array;  //tableau contenant l'ensemble des polygones extraits par SPARQL

		var varNames = [];
		if(data.results.bindings.length==0)
			alert("no results to show");
		else
			$.each(data.results.bindings, function(key, val) {
					$.each(val, function(k, sub) {
						varNames.push(k);
						$.each(sub, function(subK, subSub) {
							if(subK == "value"){
								WKT.push(subSub);
							}									
						});
					});
			});
		console.log(WKT);

		// on définit le style par défaut pour les polygones
		var style = new ol.style.Style({ 
			fill: new ol.style.Fill({
				color: colorSelect+"",
			}),
			stroke: new ol.style.Stroke({
				width: 5,
				color: "#0094ff"
			})
		});

		var coor = toCoor(WKT[0]);
		var minX = [], minY = [], maxX = [], maxY = [];
		// on boucle dynamiquement sur l'ensemble des polygones presents
		// afin de déterminer l'emplacement de l'ensemble sur la carte
		$.each(WKT,function(index,value){
			var coor = toCoor(value);
			minX.push(Math.min.apply(null, coor.x));
			minY.push(Math.min.apply(null, coor.y));
			maxX.push(Math.max.apply(null, coor.x));
			maxY.push(Math.max.apply(null, coor.y));
		});
		var minTotX = Math.min.apply(null, minX);
		var minTotY = Math.min.apply(null, minY);
		var maxTotX = Math.max.apply(null, maxX);
		var maxTotY = Math.max.apply(null, maxY);
		var centreX = minTotX+(maxTotX-minTotX)/2;
		var centreY = minTotY+(maxTotY-minTotY)/2;

		//on boucle dynamiquement sur l'ensemble des polygones presents
		$.each(WKT,function(index,value){
			//Fonction transformation des geometries WKT sous le bon referentiel
			var feature = transformOP(value);
			//Fonction creation vector pour chaque polygone 
			var vector = createVector(feature, style);
			//on ajoute chaque couche de polygones dans le tableau des couches
			layers.push(vector);
		});
		
		// paramètres de la carte
		var map = new ol.Map({
		  layers: layers,
		  target: 'map',
		  view: new ol.View({
			center: ol.proj.transform([parseFloat(centreX),parseFloat(centreY)],'EPSG:4326','EPSG:3857'),
			zoom:4 
		  })
		});
		console.log(map);
		function transformOP(WKT) {
			var format = new ol.format.WKT();
			var feature = format.readFeature(WKT);
			feature.getGeometry().transform('EPSG:4326', 'EPSG:3857');
			return feature;
		}
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

		function toCoor(WKT){
			var pointD = WKT.split(/\({3}|\){3}/g);	// tableau pour decomposition par regex
			var pointD2 = JSON.stringify(pointD);
			pointD = pointD2.split(/[^0-9\.]/g);
			pointD = pointD.filter(function(e){return e});
			
			var C = {};
			C.x=[];
			C.y=[];
			for(var a=0;a < pointD.length;a++){
				if (a%2 == 0){
					C.x.push(parseFloat(pointD[a]));
				}else{
					C.y.push(parseFloat(pointD[a]));	
				}		
			}
			return C;
		}
	})();
}
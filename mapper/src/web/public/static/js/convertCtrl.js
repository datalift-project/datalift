function ConvertCtrl($scope, $http, $timeout, Shared) {
	var self = this;

	self.graph = {};
	self.nodes = [];
	self.edges = [];
	
	$scope.loaderTxt = "";
	$scope.loaderSrc = Shared.moduleUri + "/static/img/loader.gif";
	$scope.loading = false;
	$scope.targetSrcName = "New source";
	$scope.targetSrcGraph = Shared.selectedSource + '-mapped';
	$scope.script = "";
	$scope.classScript = "";
	$scope.message = "";
	$scope.successfulConversion = false;
	$scope.projectLink = Shared.projectUri;
	
	$scope.propertyMappings = Shared.mappings;
	$scope.classMappings = Shared.classMappings;
	
	$scope.objects = Shared.objects;
	
	// Graph visusalization
	self.convertModelToGraph = function() {
		var obj = $scope.objects;
		
		// create everthing except object properties
		for (var i = 0 ; i < obj.length ; ++i) {
			
			var currentNodeId = "obj" + obj[i].id;
			
			// noeuds pour l'objet
			self.nodes.push({
				id: currentNodeId,
				label: obj[i].name
			});
			
			// classes
			for (var j = 0 ; j < obj[i].classes.length ; ++j) {
				
				var currentClassId = "obj" + obj[i].id + "cl" + j;
				
				self.nodes.push({
					id: currentClassId,
					label: obj[i].classes[j].substr(obj[i].classes[j].lastIndexOf('#'))
				});
				self.edges.push({
					from: currentNodeId,
					to: currentClassId,
					label: "a"
				});
			}
			
			// mappings
			for (var j = 0 ; j < obj[i].mappedProperties.length ; ++j) {
				
				var currentPropId = "obj" + obj[i].id + "mp" + j;
				
				self.nodes.push({
					id: currentPropId,
					label: "Literal",
					shape: "box"
				});
				self.edges.push({
					from: currentNodeId,
					to: currentPropId,
					label: obj[i].mappedProperties[j].targetPrefixed
				});
			}
		}
		
		// create object properties
		for (var i = 0 ; i < obj.length ; ++i) {
			
			var currentNodeId = "obj" + obj[i].id;

			for (var j = 0 ; j < obj[i].objectProperties.length ; ++j) {
				
				var targetId = "obj" + obj[i].objectProperties[j].targetId;
				self.edges.push({
					from: currentNodeId,
					to: targetId,
					label: obj[i].objectProperties[j].predicate
				});
			}
		}
		 // create a graph
		var container = document.getElementById('graph-visualization');
		var data = {
			nodes: self.nodes,
			edges: self.edges
		};
		var options = {
			width:  '100%',
			height: '400px',
			selectable: false
		};
		self.graph = new vis.Graph(container, data, options);
		
		$('.graph-frame').removeAttr("style");
		$('.graph-frame').css({height: "400px", margin: "50px", width: "100%"});
	}
	
	// Exec
	$scope.execute = function() {
		$scope.loaderTxt = "Conversion in progress...";
		$scope.loading = true;
		var script = $scope.classScript + $scope.script;
	    var url = Shared.baseUri + "/mapper/execute";
	    var data = {
	    		project: Shared.projectUri,
	    		source: Shared.selectedSource,
	    		dest_title: $scope.targetSrcName,
	    		dest_graph_uri: $scope.targetSrcGraph,
	    		script: script
	    		};
	    $http.post(url, data, {
	    	'headers': {
	    		"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
    		},
    		'transformRequest': self.transformRequest
	    })
    	.success(function(data, status, headers, config) {
	    	$scope.loading = false;
	    	$scope.message = "Conversion successful.";
	    	$scope.successfulConversion = true;
	    })
    	.error(function(data, status, headers, config) {
    		Shared.broadcastNotification({
				heading: "Execution failed",
				message: "The service returned an error while executing.",
				type: "danger"
    		});
    		$scope.loading = false;
    	});
	}
	
	$scope.hideForm = function() {
		return $scope.loading || $scope.successfulConversion;
	}
	
	self.convertMappingsToScript = function() {
		for(var i = 0 ; i < Shared.mappings.length ; ++i) {
			$scope.script += "MOVE_PREDICATE (source=<" + Shared.mappings[i].sourceUri + ">,target=<" + Shared.mappings[i].targetUri + ">);";
		}
		
		$scope.script = $scope.script.substring(0, $scope.script.length - 1);
	}
	
			// create add script
// 		for ( var i = 0 ; i < $scope.classMappings.length ; ++i ) {
// 			// proposer les mappings si plusieurs classes ?
// 			
// 			if (classesAlreadyIn.indexOf($scope.classMappings[i].targetClass) == -1) {
// 				// classe non présente, on l'ajoute
// 				$scope.classScript += "ADD_CLASS (source=<" + $scope.classMappings[i].sourceClass + ">,"
// 			 					+ "target=<" + $scope.classMappings[i].targetClass + ">);";
// 				if (classesToDelete.indexOf($scope.classMappings[i].sourceClass) == -1) {
// 					classesToDelete.push($scope.classMappings[i].sourceClass);
// 				}
// 			}
// 		}
// 		
// //		console.log("Deleting " + classesToDelete);
// 		
// 		// create delete script
// 		for ( var i = 0 ; i < classesToDelete.length ; ++i) {
// 			$scope.classScript += "DELETE_CLASS (source=<" + classesToDelete[i] + ">);";
// 		}
	
	self.transformRequest = function(data) {
		/**
		* The workhorse; converts an object to x-www-form-urlencoded serialization.
		* @param {Object} obj
		* @return {String}
		*/
		var param = function(obj) {
			var query = '';
			var name, value, fullSubName, subValue, innerObj, i;
			for(name in obj) {
				value = obj[name];
				if(value instanceof Array) {
					for(i=0; i<value.length; ++i) {
						subValue = value[i];
						fullSubName = name + '[' + i + ']';
						innerObj = {};
						innerObj[fullSubName] = subValue;
						query += param(innerObj) + '&';
					}
				}
				else if(value instanceof Object) {
					for(subName in value) {
						subValue = value[subName];
						fullSubName = name + '[' + subName + ']';
						innerObj = {};
						innerObj[fullSubName] = subValue;
						query += param(innerObj) + '&';
					}
				}
				else if(value !== undefined && value !== null) {
					query += encodeURIComponent(name) + '=' + encodeURIComponent(value) + '&';
				}
			}
			
			return query.length ? query.substr(0, query.length - 1) : query;
		};
		
		return angular.isObject(data) && String(data) !== '[object File]' ? param(data) : data;
	}
	
	$scope.getSparqlQuery = function() {
		var script = $scope.classScript + $scope.script;
	    var url = Shared.baseUri + "/mapper/sparqlQuery";
		var data = {
			project: Shared.projectUri,
			source: Shared.selectedSource,
			dest_title: $scope.targetSrcName,
			dest_graph_uri: $scope.targetSrcGraph,
			script: script
		};
	    $http.post(url, data, {
	    	'headers': {
	    		"Content-Type": "application/x-www-form-urlencoded; charset=UTF-8"
    		},
    		'transformRequest': self.transformRequest,
	    })
    	.success(function(data, status, headers, config) {
			$scope.sparqlTest = data;
	    })
    	.error(function(data, status, headers, config) {
    	});
	}
	
	//
	
	Shared.broadcastCurrentStep(4);
	
	// 
	self.convertMappingsToScript();
	self.convertModelToGraph();
// 	$timeout(self.convertModelToGraph, 1000);
}

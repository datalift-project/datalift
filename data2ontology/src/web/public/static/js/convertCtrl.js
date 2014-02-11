function ConvertCtrl($scope, $http, $timeout, Shared) {
	var self = this;

	self.graph = {};
	self.nodes = [];
	self.edges = [];
	
	self.construct = [];
	self.where = [];
	
	$scope.sources = Shared.sources;
	$scope.projectSourcesName = Shared.projectSourcesName;
	$scope.targetSrcName = Shared.targetSrcName;
	$scope.targetSrcGraph = Shared.targetSrcGraph;
	$scope.nameMsg = "";
	$scope.graphMsg = "";
	
	$scope.loaderTxt = "";
	$scope.loaderSrc = Shared.moduleUri + "/static/img/loader.gif";
	$scope.loading = false;
	$scope.message = "";
	$scope.successfulConversion = false;
	$scope.projectLink = Shared.projectUri;
	
	self.originalSparql = "";
	$scope.sparqlConstruct = "";
	$scope.nbRow = 0;
	
	$scope.propertyMappings = Shared.mappings;
	$scope.classMappings = Shared.classMappings;
	
	$scope.objects = Shared.objects;
	
	//*************************************************************************
	// Graph visusalization
	//*************************************************************************
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
					label: obj[i].classes[j].substr(obj[i].classes[j].lastIndexOf('#') + 1)
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
			
			// datatype properties
			for (var j = 0 ; j < obj[i].datatypeProperties.length ; ++j) {

				var currentPropId = "obj" + obj[i].id + "dt" + j;
				
				self.nodes.push({
					id: currentPropId,
					label: obj[i].datatypeProperties[j].value,
					shape: "box"
				});
				self.edges.push({
					from: currentNodeId,
					to: currentPropId,
					label: obj[i].datatypeProperties[j].property.name
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
					label: obj[i].objectProperties[j].predicate,
					size: 2
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
		$('.graph-frame').css({height: "400px", margin: "50px", width: "100%", position: "relative", right: "50px"});
	}
	
	//*************************************************************************
	// Sparql conversion
	//*************************************************************************
	self.generateSparql = function() {
		var obj = $scope.objects;

		for (var i = 0 ; i < obj.length ; ++i) {
			var currentResVar = "?res" + obj[i].id;
			
			// classes
			for (var j = 0 ; j < obj[i].classes.length ; ++j) {
				self.construct.push(currentResVar + " a <" + obj[i].classes[j] + "> .");
			}
			
			// mappings
			for (var j = 0 ; j < obj[i].mappedProperties.length ; ++j) {
				var mappingVar = "?match" + j + "res" + obj[i].id;

				self.construct.push(currentResVar + " <" + obj[i].mappedProperties[j].targetUri + "> " + mappingVar + " .");
				self.where.push("?res0" + " <" + obj[i].mappedProperties[j].sourceUri + "> " + mappingVar + " .");
			}
			
			// datatype properties
			for (var j = 0 ; j < obj[i].datatypeProperties.length ; ++j) {
				self.construct.push(currentResVar + ' <' + obj[i].datatypeProperties[j].property.uri + '> "' + obj[i].datatypeProperties[j].value + '" .');
			}
			
			// object properties
			for (var j = 0 ; j < obj[i].objectProperties.length ; ++j) {
				var targetVar = "?res" + obj[i].objectProperties[j].targetId;
				self.construct.push(currentResVar + " <" + obj[i].objectProperties[j].predicate + "> " + targetVar + " .");
			}
			
			// construct uri
			if (i >= 1) {
				self.where.push('BIND(URI(CONCAT(STR(?res0),"-'+ obj[i].id +'")) AS ?res' + obj[i].id +') .');
			}
		}
		$scope.sparqlConstruct = "CONSTRUCT {\n";
		for (var i = 0 ; i < self.construct.length ; ++i) {
			$scope.sparqlConstruct += "\t" + self.construct[i] + "\n";
		}
		$scope.sparqlConstruct += "}\nWHERE {\n";
		for (var i = 0 ; i < self.where.length ; ++i) {
			$scope.sparqlConstruct += "\t" + self.where[i] + "\n";
		}	
		$scope.sparqlConstruct += "}";
		
		self.originalSparql = $scope.sparqlConstruct;
		$scope.nbRow = 3 + self.construct.length + self.where.length + 1;
	}
	
	$scope.revertSparql = function() {
		$scope.sparqlConstruct = self.originalSparql;
	}
	
	//*************************************************************************
	// Execution
	//*************************************************************************
	$scope.execute = function() {
		$scope.loaderTxt = "Conversion in progress...";
		$scope.loading = true;
	    var url = Shared.moduleUri + "/execute";
	    var data = {
	    		project: Shared.projectUri,
	    		sourceGraph: Shared.selectedSource,
	    		targetName: $scope.targetSrcName,
	    		targetGraph: $scope.targetSrcGraph,
	    		query: $scope.sparqlConstruct
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
	
	//*************************************************************************
	// Form validation
	//*************************************************************************
	$scope.isTargetNameValid = function() {
 		if ( ! $scope.targetSrcName) {
 			return false;
 		}
 		if ( $scope.targetSrcName.trim() == "" ) {
 			return false;
 		}
 		if ($scope.projectSourcesName.indexOf($scope.targetSrcName.trim()) != -1) {
 			return false;
 		}
 		return true;
 	}
 	
 	$scope.nameChanged = function() {
 		if ( $scope.targetSrcName.trim() == "" ) {
 			$scope.nameMsg = "Name is empty";
 			return;
 		}
 		if ($scope.projectSourcesName.indexOf($scope.targetSrcName.trim()) != -1) {
 			$scope.nameMsg = "Name already exists";
 			return;
 		}
 		$scope.nameMsg = "";
 	}
 	
 	$scope.isTargetGraphValid = function() {
 		if ( ! $scope.targetSrcGraph) {
 			return false;
 		}
 		if ( $scope.targetSrcGraph.trim() == "" ) {
 			return false;
 		}
 		for (var i = 0; i < $scope.sources.length ; ++i) {
 			if ($scope.targetSrcGraph.trim() == $scope.sources[i].uri) {
 				return false;
 			}
 		}
 		return true;
 	}
 	
 	$scope.graphChanged = function() {
 		if ( $scope.targetSrcGraph.trim() == "" ) {
 			$scope.graphMsg = "Graph is empty";
 			return;
 		}
 		for (var i = 0; i < $scope.sources.length ; ++i) {
 			if ($scope.targetSrcGraph.trim() == $scope.sources[i].uri) {
 				$scope.graphMsg = "Graph already exists";
 				return;
 			}
 		}
 		$scope.graphMsg = "";
 	}
 	
	$scope.isFormInvalid = function() {
		return ! $scope.isTargetNameValid()
				|| ! $scope.isTargetGraphValid();
	}
	
	$scope.targetNameClass = function() {
		if ($scope.isTargetNameValid()) {
 			return "form-group has-success";
 		}
 		else {
 			return "form-group has-error";
 		}
 	}
 	
 	$scope.targetGraphClass = function() {
 		if ($scope.isTargetGraphValid()) {
 			return "form-group has-success";
 		}
 		else {
 			return "form-group has-error";
 		}
 	}

	//*************************************************************************
	// Actions to execute immediately
	//*************************************************************************
	Shared.broadcastCurrentStep(4);

	self.convertModelToGraph();
	self.generateSparql();
}

function ConvertCtrl($scope, $http, $timeout, Shared) {
	var self = this;
	
	self.url_type = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?o WHERE { graph <" + Shared.selectedSource + "> { ?s a ?o . }}&max=25";
	self.url_classPred = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?c ?p WHERE { graph <" + Shared.selectedSource + "> { ?s ?p ?o. ?s a ?c . }}&max=25";
	
	$scope.loaderTxt = "Searching corresponding types...";
	$scope.loaderSrc = Shared.moduleUri + "/static/img/loader.gif";
	$scope.loading = true;
	$scope.targetSrcName = "New source";
	$scope.targetSrcGraph = Shared.selectedSource + '-mapped';
	$scope.script = "";
	$scope.classScript = "";
	$scope.message = "";
	$scope.successfulConversion = false;
	$scope.projectLink = Shared.projectUri;
	
	$scope.predicateClassMap = {};
	$scope.propertyMappings = Shared.mappings;
	$scope.classMappings = [];
	$scope.sourceTypes = [];
	$scope.addedTypes = [];
	$scope.loadedOntologies = {};
	$scope.requestSent = 0;
	$scope.ontologiesReceived = 0;
	
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
    		alert("error!");
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
	
	self.loadAllOntologies = function() {
		
		var ontoToSearch = [];
		
		for( var i = 0 ; i < Shared.mappings.length ; ++i ) {
			console.log("mapping " + i + " : " + JSON.stringify(Shared.mappings[i]));
			if(ontoToSearch.indexOf(Shared.mappings[i].targetVocabulary) == -1) {
				ontoToSearch.push(Shared.mappings[i].targetVocabulary);
			}
		}
		
		for ( var i = 0 ; i < ontoToSearch.length ; ++i ) {
			self.loadOntology(ontoToSearch[i]);
			++$scope.requestSent;
		}
		
		// wait for results
		self.waitForResults();
	}
	
	self.waitForResults = function() {
		if ($scope.ontologiesReceived < $scope.requestSent) {
			$timeout(self.waitForResults, 1000);
		}
		else {
			// all ontologies loaded
			self.findClassesForMappings();
		}
	}
	
	// TODO IL FAUDRA FAIRE UN CHECK POUR TYPE SECONDAIRE ET LES PROPOSER À L'UTILISATEUR
	self.findClassesForMappings = function() {
		var classesToDelete = [];
		var classesAlreadyIn = [];
		
		// find mappings
		for( var i = 0 ; i < Shared.mappings.length ; ++i ) {
			self.findClassMappingsForProperty(Shared.mappings[i].targetVocabulary,
					Shared.mappings[i].targetUri);
		}
		
//		console.log($scope.classMappings.length + " class mapping(s)");
		
		// find classes already present
		for ( var i = 0 ; i < $scope.sourceTypes.length ; ++i ) {
			for ( var j = 0 ; j < $scope.addedTypes.length ; ++j ) {
				if ( $scope.sourceTypes[i] == $scope.addedTypes[j] ) {
					classesAlreadyIn.push($scope.sourceTypes[i]);
				}
			}
		}
//		console.log("Classes already present : " + classesAlreadyIn);
		
		// create add script
		for ( var i = 0 ; i < $scope.classMappings.length ; ++i ) {
			// proposer les mappings si plusieurs classes ?
			
			if (classesAlreadyIn.indexOf($scope.classMappings[i].targetClass) == -1) {
				// classe non présente, on l'ajoute
				$scope.classScript += "ADD_CLASS (source=<" + $scope.classMappings[i].sourceClass + ">,"
			 					+ "target=<" + $scope.classMappings[i].targetClass + ">);";
				if (classesToDelete.indexOf($scope.classMappings[i].sourceClass) == -1) {
					classesToDelete.push($scope.classMappings[i].sourceClass);
				}
			}
		}
		
//		console.log("Deleting " + classesToDelete);
		
		// create delete script
		for ( var i = 0 ; i < classesToDelete.length ; ++i) {
			$scope.classScript += "DELETE_CLASS (source=<" + classesToDelete[i] + ">);";
		}
		
//		console.log($scope.classScript);
		$scope.loading = false;

	}
	
	self.loadOntology = function(ontology) {
		$http.get(Shared.baseUri + "/mapper/ontology?src=" + ontology)
		.success(function(data, status, headers, config) {
			$scope.loadedOntologies[ontology] = data;
			++$scope.ontologiesReceived;
		})
		.error(function(data, status, headers, config) {
			++$scope.ontologiesReceived;
		});
	}
	
	self.findClassMappingsForProperty = function(ontology, givenPropertyUri) {
//		console.log("find class mappings for property " + givenPropertyUri + " in " + ontology);
		if ( ! $scope.loadedOntologies[ontology]) {
			console.log("unknown ontology : " + ontology);
			return;
		}
		
		var property = $scope.loadedOntologies[ontology].properties[givenPropertyUri];
		var m = self.getSpecificTargetMapping(givenPropertyUri);
		
		// the property must be in the ontology
		if ( ! property ) {
			console.log("unknown property : " + givenPropertyUri);
			return;
		}
		
		//the mapping must exists
		if (m == null) {
			console.log("no mapping found for the target : " + givenPropertyUri);
			return;
		}
		
		// we must find a source class for the mapping
		if ( ! $scope.predicateClassMap.hasOwnProperty(m.sourceUri) ) {
			console.log("no source property found inside predicateClassMap for : " + m.sourceUri);
			return;
		}

		var sourceClasses = $scope.predicateClassMap[m.sourceUri];
		// the property must contain at least 1 class
		if (sourceClasses.length < 1) {
			console.log("no class found for : " + m.sourceUri);
			return;
		}
		
		if (property.hasOwnProperty('domains')) { // si on a un domaine
//			console.log("domains for " + givenPropertyUri + " : " + JSON.stringify(property.domains));
			// on ajoute les mappings
			for ( var i = 0 ; i < property.domains.length ; ++i) {
				self.addClassMapping(m.sourceUri, sourceClasses[0], property.domains[i]);
				
				if ($scope.addedTypes.indexOf(property.domains[i]) == -1) {
					$scope.addedTypes.push(property.domains[i]);
				}
			}
		}
		else {
			console.log("no domain for property " + givenPropertyUri);
		}
	}
	
	// Get a mapping having a specific target URI
	self.getSpecificTargetMapping = function(targetUri) {
		for ( var i = 0 ; i < Shared.mappings.length ; ++i ) {
			var m = Shared.mappings[i];
			if (m.targetUri == targetUri) {
//				console.log("Mapping found for " + targetUri + " : " + JSON.stringify(m));
				return m;
			}
		}
		console.log("No mapping found for " + targetUri);
		return null;
	}
	
	// Add a class mapping if it does not exists
	self.addClassMapping = function(sourcePredicate, sourceClass, targetClass) {
//		console.log("Adding mapping with : " + sourcePredicate + " - " + sourceClass + " - " + targetClass);
		for (var i = 0 ; i < $scope.classMappings.length ; ++i) {
			var m = $scope.classMappings[i];
			if (m.sourcePredicate == sourcePredicate && m.targetClass == targetClass
					|| m.sourceClass == sourceClass && m.targetClass == targetClass) {
				// do not add the mapping if the predicate and target class are the same
				// or if the source and target are the same
//				console.log("Similar mapping already exist.");
				return;
			}
		}
		
		$scope.classMappings.push({
			'sourcePredicate': sourcePredicate,
			'sourceClass': sourceClass,
			'targetClass': targetClass
		});
	}
	
	Shared.broadcastCurrentStep(3);
	
	// 
	self.convertMappingsToScript();
	
	// Fetch classes and predicates to construct a class - predicate map
	$http.get(self.url_classPred)
	.success(function(data, status, headers, config) {
		for (  var i = 0 ; i < data.results.bindings.length ; ++i) {
			var c = data.results.bindings[i].c.value;
			var p = data.results.bindings[i].p.value;
			// ajout à la map
			if ( p != 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type') {
				if ( ! $scope.predicateClassMap[p] ) {
					$scope.predicateClassMap[p] = [];
				}
				if ( $scope.predicateClassMap[p].indexOf(c) == -1) {
						$scope.predicateClassMap[p].push(c);
				}
			}
			
			// ajout au tableau
			if ($scope.sourceTypes.indexOf(c) == -1) {
				$scope.sourceTypes.push(c); 
			}
		}

		self.loadAllOntologies();
	})
	.error(function(data, status, headers, config) {
		// TODO
		console.log("error fetching classes");
	});
}

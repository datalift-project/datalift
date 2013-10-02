function MappingProjectCtrl($scope, $http, $location, Shared) {
	var self = this;
    self.url_prop = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?p WHERE { graph <" + Shared.selectedSource + "> { ?s ?p ?o . }}&max=25";
    self.url_type = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?o WHERE { graph <" + Shared.selectedSource + "> { ?s a ?o . }}&max=25";
    self.pageSize = 5;
    
    $scope.upadatingOntology = false;
    
    $scope.ontologies = Shared.ontologies;
    $scope.loadedOntology = {};
	$scope.sourcePredicates = [];
	$scope.propertiesFound = [];
	$scope.sourceTypes = [];
	$scope.selectedOntology = {};
	$scope.mappings = [];
	$scope.vocabSummary = {};
	$scope.isAutoMapping = false;
	
	// scope method
	$scope.goToConvert = function() {
		Shared.mappings = $scope.mappings;
		$location.path("/convert/");
	}
	
	$scope.goToSelect = function() {
		$location.path("/select/");
	}
	
	$scope.hasPredicates = function() {
		return $scope.sourcePredicates.length > 0;
	}	
		
	$scope.updateOntology = function() {
		// Afficher un loader
		$scope.upadatingOntology = true;
		$http.get(Shared.baseUri + "/mapper/ontology?src=" + $scope.selectedOntology)
		.success(function(data, status, headers, config) {
			$scope.loadedOntology = data;
			// Retirer le loader
			$scope.upadatingOntology = false;
		});
	}
	
	$scope.ontologySelected = function() {
		return ! jQuery.isEmptyObject($scope.selectedOntology) 
				&& ! $scope.upadatingOntology;
	}
	
	$scope.searchOntologyProperties = function() {
		self.testClass();
		$scope.propertiesFound = [];
		$scope.propertiesFound = self.searchOntologyForProperties($scope.searchQuery);
		$scope.currentPage = 1;
	}
	
	$scope.hasResults = function() {
		return $scope.propertiesFound.length > 0;
	}
	
	$scope.autoMappingDisabled = function() {
		return $scope.isAutoMapping || $scope.sourcePredicates.length == 0;
	}

	/* Mappings control */
	$scope.addMapping = function(target) {
		if ( ! $scope.selectedPredicateId ) {
			return;
		}
		var selectedPredicate = {};
		for (var i = 0 ; i < $scope.sourcePredicates.length ; ++i) {
			if ($scope.sourcePredicates[i].name == $scope.selectedPredicateId) {
				selectedPredicate = $scope.sourcePredicates[i];
				break;
			}
		}
		// Really add the mapping
		self.addMappingToArray(selectedPredicate, target);
		
		// Change selection
		if ($scope.sourcePredicates.length > 0) {
			$scope.selectedPredicateId = $scope.sourcePredicates[0].name;
		}
		else {
			$scope.selectedPredicateId = "";
		}
	}
	
	self.addMappingToArray = function(predicate, target) {
		if (predicate != undefined) {
			$scope.mappings.push({
				'sourceId': predicate.name,
				'source': predicate.name,
				'sourceUri': predicate.uri,
				'targetUri': target.uri,
				'targetPrefixed': target.name,
				'targetVocabulary': $scope.selectedOntology
			});
			self.addToVocabularyOverview($scope.selectedOntology);
			self.removePredicate(predicate);
		}
	}
	
	$scope.removeMapping = function(mappingToRemove) {
		self.substractFromVocabularyOverview(mappingToRemove.targetVocabulary);
		self.addPredicate(mappingToRemove.source, mappingToRemove.sourceUri);
		$scope.mappings.splice($scope.mappings.indexOf(mappingToRemove), 1);
	}
	
	self.addPredicate = function(name, uri) {
		$scope.sourcePredicates.push({
			'name': name,
			'uri': uri
		});
	}
	
	self.removePredicate = function(predicateToRemove) {
		$scope.sourcePredicates.splice($scope.sourcePredicates.indexOf(predicateToRemove), 1);
	}
	
	self.addToVocabularyOverview = function(vocabulary) {
		if ($scope.vocabSummary[vocabulary] == undefined) {
			$scope.vocabSummary[vocabulary] = 1;
		}
		else {
			$scope.vocabSummary[vocabulary]++;
		}
	}
	
	self.substractFromVocabularyOverview = function(vocabulary) {
		if ($scope.vocabSummary[vocabulary] == 1) {
			delete $scope.vocabSummary[vocabulary];
		}
		else {
			$scope.vocabSummary[vocabulary]--;
		}
	}
	
	/* auto search */
	$scope.autoMapping = function() {
		$scope.isAutoMapping = true;
		var results = [];
		var mappingsToAdd = [];
		var target = {};
		for ( var i = 0 ; i < $scope.sourcePredicates.length ; ++i ) {
			results = self.searchOntologyForProperties($scope.sourcePredicates[i].name);
			if ( results.length > 0 ) {
				target = self.findBestMapping(results);
				mappingsToAdd.push({
					'predicate': $scope.sourcePredicates[i],
					'target': target
					});
			}
		}
		for ( var i = 0 ; i < mappingsToAdd.length ; ++i ) {
			self.addMappingToArray(
					mappingsToAdd[i].predicate,
					mappingsToAdd[i].target);
		}
		
		$scope.selectedPredicateId = "";
		$scope.isAutoMapping = false;
	}
	
	self.findBestMapping = function(results) {
		var score = -1;
		var bestMapping = {};
		for ( var i = 0 ; i < results.length ; ++i ) {
			if (results[i].score > score) {
				score = results[i].score;
				bestMapping = results[i];
			}
		}
		return bestMapping;
	}
	
	// self method
	self.loadOntology = function(ontoUri) {

	}
	
	self.searchOntologyForProperties = function(keyword) {
		var propertiesFound = [];
		var propertyToAdd = {};
		for (var prop in $scope.loadedOntology.properties) {
			if ($scope.loadedOntology.properties[prop].name.toLowerCase().indexOf(keyword.toLowerCase()) != -1) {
				propertyToAdd = $scope.loadedOntology.properties[prop];
				propertyToAdd.uri = prop;
				propertyToAdd.score = self.computeSearchScore(keyword, $scope.loadedOntology.properties[prop]);
				propertiesFound.push(propertyToAdd);
			}
			else {
				if ( ! $scope.loadedOntology.properties[prop].hasOwnProperty("desc")) {
					continue;
				}
				if ($scope.loadedOntology.properties[prop].desc.toLowerCase().indexOf(keyword.toLowerCase()) != -1) {
					propertyToAdd = $scope.loadedOntology.properties[prop];
					propertyToAdd.uri = prop;
					propertyToAdd.score = self.computeSearchScore(keyword, $scope.loadedOntology.properties[prop]);
					propertiesFound.push(propertyToAdd);
				}
			}
		}
		
		return propertiesFound;
	}
	
	self.computeSearchScore = function(keyword, property) {
		var score = 0;
		if ( property.hasOwnProperty("name") ) {
			if (property.name.toLowerCase().indexOf(keyword.toLowerCase()) != -1) {
				score += 1;
			}
		}
		if ( property.hasOwnProperty("desc") ) {
			if (property.desc.toLowerCase().indexOf(keyword.toLowerCase()) != -1) {
				score += 0.5;
			}
			if (property.desc.toLowerCase().indexOf("deprecated") != -1) {
				score = 0;
			}
		}
		return score;
	}
	// faire de même avec classe restreinte (domain des properties)
	
	
	self.testClass = function() {
		if ( $('#pager div ul').attr('class') != 'pagination')
			$('#pager div ul').addClass('pagination');
	}
	
	/* Pagination */
	$scope.noOfPages = function() {
		return Math.ceil($scope.propertiesFound.length / self.pageSize);
	}
	
	$scope.pageContent = function() {
		if ($scope.propertiesFound.length == 0) {
			return [];
		}
		
		var startIndex = ($scope.currentPage - 1) * self.pageSize;
		var endIndex = startIndex + self.pageSize;
		
		if (endIndex > $scope.propertiesFound.length) {
			endIndex = $scope.propertiesFound.length;
		}
		return $scope.propertiesFound.slice(startIndex, endIndex);
	}
	
	self.testClass = function() {
		if ( $('#pager div ul').attr('class') != 'pagination' )
			$('#pager div ul').addClass('pagination');
	}
	
	// Execute immediately
	Shared.broadcastCurrentStep(2);
	
	if(Shared.selectedSource == "") {
		$scope.goToSelect();
	}
	
	$http.get(self.url_prop) // prédicats
	.success(function(data, status, headers, config) {
		var rdf = new RegExp("http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		var rdfs= new RegExp("http://www.w3.org/2000/01/rdf-schema#");
		var owl = new RegExp("http://www.w3.org/2002/07/owl#");
		var prefixMapping = [];
		for ( var i = 0 ; i < data.results.bindings.length ; ++i) {
			var p = data.results.bindings[i].p.value;
			// Substitute well-known prefixes.
			var name = p.replace(rdf, "rdf:").replace(rdfs, "rdfs:")
				.replace(owl, "owl:");
			if (name == p) {
				// Not a well-known prefix. => Extract namespace
				var j = Math.max(p.lastIndexOf('#'), p.lastIndexOf('/')) + 1;
				var ns = p.substring(0, j);
				name = p.substring(j);
				// Resolve name conflicts.
				var k = prefixMapping.indexOf(ns);
				if (k == -1) {
					// Namespace not yet known.
					k = prefixMapping.push(ns) - 1;
				}
				if (k != 0) {
					name = 'ns' + k + ':' + name;
				}
//				propertyMap[name] = p;
			}
			if(name != "rdf:type") {
				self.addPredicate(name, p);
//				self.propertiesToMap.push(new Mapping('PROPERTY', self.baseUri, self.selectedSource, p, name));
			}
		}
		if ($scope.sourcePredicates.length == 0) {
			// TODO
		}
//		self.loadLovPredicates();
//		self.displaySourceSelection(false);
//		self.selectPropertyToMap(self.propertiesToMap()[0]);
	})
	.error(function(data, status, headers, config) {
		// TODO
		alert("error");
	});
	
	$http.get(self.url_type) // types
	.success(function(data, status, headers, config) {
		for (  var i = 0 ; i < data.results.bindings.length ; ++i) {
			var o = data.results.bindings[i].o.value;
			var j = Math.max(o.lastIndexOf('#'), o.lastIndexOf('/')) + 1;
			var ns = o.substring(0, j);
			var name = o.substring(j);
			$scope.sourceTypes.push(name);
			// push new mapping ?
		}
		
		if ( $scope.sourceTypes.lenghth > 0 ) {
			// selectTypeToMap($scope.sourceTypes[0]
		}
	});
	
}

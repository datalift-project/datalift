function MappingCtrl($scope, $location, $http, $timeout, Shared) {
	var self = this;
	
	//*************************************************************************
	// Static data
	//*************************************************************************
    self.url_prop = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?p WHERE { graph <" + Shared.selectedSource + "> { ?s ?p ?o . }}&max=25";
    self.url_type = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?o WHERE { graph <" + Shared.selectedSource + "> { ?s a ?o . }}&max=25";
	self.pageSize = 5;

    $scope.barLoaderSrc = Shared.moduleUri + "/static/img/bar_loader.gif";
    $scope.searchLoaderSrc = Shared.moduleUri + "/static/img/search_loader.gif";
	
	 /* 
	  * TODO rdf:test donne un vocabulaire à "null" dans l'overview
      * TODO keypress pour recherche
      */ 

	//*************************************************************************
	// Data bindings
	//*************************************************************************
	$scope.mode = "lov";
	$scope.sourcePredicates = [];
	$scope.sourceTypes = [];
	//$scope.selectedPredicateId
	//
	$scope.mappings = [];
	$scope.vocabSummary = {};
	//
	$scope.loadingPredicates = true;
	$scope.searchingLov = false;
	$scope.isAutoMapping = false;
	$scope.lovSearchResults = [];
	$scope.lovSearchCount = 0;
	$scope.numberOfPredicatesSearched = 0;
	$scope.allLovResults = [];
	//
    $scope.updatingOntology = false;
    $scope.ontologies = Shared.ontologies;
    $scope.loadedOntology = {};
	$scope.loadedOntologies = [];
	$scope.propertiesFound = [];
	$scope.selectedOntology = {};
	$scope.visibleOntologies = [];
	// tabs
	$scope.currentTab = 0;
	
	//*************************************************************************
	// Common methods
	//*************************************************************************
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
	
	$scope.autoMappingDisabled = function() {
		return $scope.isAutoMapping || $scope.sourcePredicates.length == 0;
	}
	
	$scope.selectTab = function(tab) {
		
		var oldTab = $scope.currentTab + 1;
		var newTab = tab + 1;
		
		$("#selectionTabs>li:nth-child("+ oldTab +")").removeClass("active");
		$("#selectionTabs>li:nth-child("+ newTab +")").addClass("active");
		
		$scope.currentTab = tab;
	}
	
	$scope.isTabSelected = function(tab) {
		return $scope.currentTab == tab;
	}
	
	//*************************************************************************
	// Project oriented methods
	//*************************************************************************
	self.loadAllOntologies = function() {
		for (var i = 0 ; i < Shared.ontologies.length ; ++i) {
			self.loadOntology(Shared.ontologies[i]);
		}
	}
	
	self.loadOntology = function(ontology) {
		// Afficher un loader
		$scope.updatingOntology = true;
		$http.get(Shared.baseUri + "/mapper/ontology?src=" + ontology.uri)
		.success(function(data, status, headers, config) {
			var propArray = [];
			for (var prop in data.properties) {
				if (data.properties[prop].type != "ObjectProperty") {
					propArray.push(data.properties[prop]);
				}
			}
			$scope.loadedOntologies.push({
				uri: ontology.uri,
				name: ontology.title,
				classes: data.classes,
				properties: propArray
			});
			// Retirer le loader
			$scope.updatingOntology = false;
		})
		.error(function(data, status, headers, config) {
			self.findAlternativeUriFromLov(ontology);
		});
	}
	
	$scope.updateOntology = function() {
		self.loadOntology($scope.selectedOntology);
	}
	
	self.findAlternativeUriFromLov = function(ontology) {
		$http.get(Shared.baseUri + "/lov/vocabs?uri=" + ontology.uri)
		.success(function(data, status, headers, config) {
			if (data.hasOwnProperty("lastVersionReviewed")) {
				self.loadOntologyFromLov(ontology, data.lastVersionReviewed.link);
			}
		})
		.error(function(data, status, headers, config) {
			Shared.broadcastNotification({
				heading: "Couldn't retrieve ontology",
				message: "No alternative URI has been found for " + ontology.title + ".",
				type: "warning"
    		});
			$scope.updatingOntology = false;
		});
	}
	
	self.loadOntologyFromLov = function(ontology, altUri) {
		$http.get(Shared.baseUri + "/mapper/ontology?src=" + altUri)
		.success(function(data, status, headers, config) {
			var propArray = [];
			for (var prop in data.properties) {
				if (data.properties[prop].type != "ObjectProperty") {
					propArray.push(data.properties[prop]);
				}
			}
			$scope.loadedOntologies.push({
				uri: ontology.uri,
				name: ontology.title,
				classes: data.classes,
				properties: propArray
			});
			// Retirer le loader
			$scope.updatingOntology = false;
		})
		.error(function(data, status, headers, config) {
			Shared.broadcastNotification({
				heading: "Couldn't retrieve ontology",
				message: "The onology " + ontology.title + " has not been loaded properly.",
				type: "warning"
    		});
			$scope.updatingOntology = false;
		});
	}
	
	$scope.ontologySelected = function() {
		return ! jQuery.isEmptyObject($scope.selectedOntology) 
				&& ! $scope.updatingOntology;
	}
	
	$scope.searchOntologyProperties = function() {
		self.testClass();
		$scope.propertiesFound = [];
		$scope.propertiesFound = self.searchOntologyForProperties($scope.searchQuery);
		$scope.currentPage = 1;
	}
	
	$scope.changeVisibility = function(uri) {
		if ($scope.visibleOntologies.indexOf(uri) != -1) {
			$scope.visibleOntologies.splice($scope.visibleOntologies[$scope.visibleOntologies.indexOf(uri)], 1);
		}
		else {
			$scope.visibleOntologies.push(uri);
		}
	}
	
	$scope.isVisible = function(uri) {
		return $scope.visibleOntologies.indexOf(uri) != -1;
	}
	//*************************************************************************
	// LOV oriented methods
	//*************************************************************************
	$scope.hasResults = function() {
		return $scope.lovSearchResults.length > 0 && ! $scope.searchingLov;
	}
	
	$scope.noResults = function() {
		return $scope.lovSearchResults.length == 0 && ! $scope.searchingLov;
	}
	
	$scope.getFirstVocSpace = function(result) {
		if (result.vocSpaces.length > 0) {
			return result.vocSpaces[0].label;
		}
		else {
			return "";
		}
	}
	
	$scope.searchLovClasses = function() {
		self.searchLov("http://www.w3.org/2000/01/rdf-schema%23Class");
	}

	$scope.searchLovProperties = function() {
		self.searchLov("http://www.w3.org/1999/02/22-rdf-syntax-ns%23Property");
	}

	self.searchLov = function(type) {
		$scope.bullshit = "searching...";
		$scope.searchingLov = true;
		$http.get(Shared.baseUri + '/lov/search?q='+ $scope.searchQuery
				+ "&type=" + type)
			.success(function(data, status, headers, config) {
				self.testClass();
				$scope.searchingLov = false;
				$scope.lovSearchResults = data.results;
				$scope.lovSearchCount = data.results.length;
				$scope.currentPage = 1;
			})
			.error(function(data, status, headers, config) {
				$scope.searchingLov = false;
			});
	}
	
	//*************************************************************************
	// Auto search
	//*************************************************************************
	
	//*************************************************************************
	// Mappings control
	//*************************************************************************
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
			console.log("Adding the follow mapping : " + JSON.stringify(predicate) + " - " + JSON.stringify(target));
			$scope.mappings.push({
				'sourceId': predicate.name,
				'source': predicate.name,
				'sourceUri': predicate.uri,
				'targetUri': target.uri,
				'targetPrefixed': target.uriPrefixed,
				'targetVocabulary': target.vocabulary
			});
			self.addToVocabularyOverview(target.vocabulary);
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
	
	//*************************************************************************
	// Pagination
	//*************************************************************************
	$scope.noOfPages = function() {
		return Math.ceil($scope.lovSearchCount / self.pageSize);
	}
	
	$scope.pageContent = function() {
		if ($scope.lovSearchCount == 0) {
			return [];
		}
		
		var startIndex = ($scope.currentPage - 1) * self.pageSize;
		var endIndex = startIndex + self.pageSize;
		
		if (endIndex > $scope.lovSearchCount) {
			endIndex = $scope.lovSearchCount;
		}
		
		return $scope.lovSearchResults.slice(startIndex, endIndex);
	}
	
// 	$scope.noOfPages = function() {
// 		return Math.ceil($scope.propertiesFound.length / self.pageSize);
// 	}
// 	
// 	$scope.pageContent = function() {
// 		if ($scope.propertiesFound.length == 0) {
// 			return [];
// 		}
// 		
// 		var startIndex = ($scope.currentPage - 1) * self.pageSize;
// 		var endIndex = startIndex + self.pageSize;
// 		
// 		if (endIndex > $scope.propertiesFound.length) {
// 			endIndex = $scope.propertiesFound.length;
// 		}
// 		return $scope.propertiesFound.slice(startIndex, endIndex);
// 	}
	
	self.testClass = function() {
		if ( $('#pager div ul').attr('class') != 'pagination' )
			$('#pager div ul').addClass('pagination');
	}
	
	
	//*************************************************************************
	// Actions to execute immediately
	//*************************************************************************
	Shared.broadcastCurrentStep(2);
	
	if(Shared.selectedSource == "") {
		$scope.goToSelect();
	}
	
	$scope.$watch('selectedPredicateId', function(newValue) {
		$scope.searchQuery = newValue;
	});
	
	// load project ontologies
	self.loadAllOntologies();
	
	// load source
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
			}
		}
		if ($scope.sourcePredicates.length == 0) {
			// TODO
		}
		$scope.loadingPredicates = false;
	})
	.error(function(data, status, headers, config) {
		Shared.broadcastNotification({
			heading: "Source predicate error",
			message: "The service returned an error while fetching source predicates.",
			type: "danger"
		});
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
			// TODO
		}
	})
	.error(function(data, status, headers, config) {
		Shared.broadcastNotification({
			heading: "Source type error",
			message: "The service returned an error while fetching source types.",
			type: "danger"
		});
	});
}
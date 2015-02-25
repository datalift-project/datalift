function MappingCtrl($scope, $location, $http, $timeout, Shared) {
	var self = this;
	
	//*************************************************************************
	// Static data
	//*************************************************************************
    self.url_prop = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?p WHERE { graph <" + Shared.selectedSource + "> { ?s ?p ?o . }}&max=25";
	self.pageSize = 5;

    $scope.barLoaderSrc = Shared.moduleUri + "/static/img/bar_loader.gif";
    $scope.searchLoaderSrc = Shared.moduleUri + "/static/img/search_loader.gif";
	$scope.suggestLoaderSrc = Shared.moduleUri + "/static/img/loader.gif";
	
	 /* 
	  * TODO rdf:test donne un vocabulaire à "null" dans l'overview
      * TODO keypress pour recherche
      */ 

	//*************************************************************************
	// Data bindings
	//*************************************************************************
	$scope.mode = "lov";
	$scope.sourcePredicates = [];
	//
	$scope.suggesting = false;
	$scope.suggestions = [];
	$scope.numberOfPredicatesSearched = 0;
	//
	$scope.mappings = [];
	$scope.vocabSummary = {};
	//
	$scope.vocSpaces = Shared.vocSpaces;
	$scope.vocSpaceFilter = "All";
	$scope.loadingPredicates = true;
	$scope.searchingLov = false;
	$scope.isAutoMapping = false;
	$scope.lovFilteredResults = [];
	$scope.lovSearchResults = [];
	$scope.lovSearchCount = 0;
	$scope.numberOfPredicatesSearched = 0;
	$scope.allLovResults = [];
	//
	$scope.projectPropertyType = "DatatypeProperty";
	$scope.ontologyStringFilter = "";
    $scope.updatingOntologies = false;
	$scope.ontoRequestSent = 0;
	$scope.ontoRequestReceived = 0;
    $scope.ontologies = Shared.ontologies;
    $scope.loadedOntology = {};
	$scope.loadedOntologies = [];
	$scope.propertiesFound = [];
	$scope.visibleOntologies = [];
	// tabs
	$scope.currentTab = 0;
	
	//*************************************************************************
	// Common methods
	//*************************************************************************
	self.goTo = function(path) {
		Shared.mappings = $scope.mappings;
// 		Shared.sourceData[Shared.selectedSource].mappings = $scope.mappings;
		$location.path(path);
	}
	$scope.goToRefine = function() {
		while ($scope.sourcePredicates.length > 0) {
			var predicate = $scope.sourcePredicates[0];
			var i = Math.max(predicate.uri.lastIndexOf('#'), predicate.uri.lastIndexOf('/')) + 1;
			var prefixed = "source:" + predicate.uri.substring(i);
			$scope.mappings.push({
				'sourceId': predicate.name,
				'source': predicate.name,
				'sourceUri': predicate.uri,
				'targetUri': predicate.uri,
				'targetPrefixed': prefixed,
				'targetVocabulary': Shared.selectedSource
			});
			self.removePredicate(predicate);
		}
		self.goTo("/refine/");
	}
	
	$scope.goToSelect = function() {
		self.goTo("/select/");
	}
	
	$scope.hasPredicates = function() {
		return $scope.sourcePredicates.length > 0;
	}
	
	$scope.hasPredicate = function(predicateUri) {
		for (var i = 0; i < $scope.sourcePredicates.length ; ++i) {
			if ($scope.sourcePredicates[i].uri == predicateUri) {
				return true;
			}
		}
		return false;
	}
	
	$scope.noPredicateSelected = function() {
		if ($scope.selectedPredicateId) {
			return $scope.selectedPredicateId == "";
		}
		return true;
	}
	
	$scope.autoMappingDisabled = function() {
		return $scope.isAutoMapping || $scope.sourcePredicates.length == 0;
	}
	
	$scope.selectTab = function(tab) {
		$scope.currentTab = tab;
	}
	
	$scope.isTabSelected = function(tab) {
		return $scope.currentTab == tab;
	}
	
	self.getSourceName = function() {
		for (var i = 0 ; i < Shared.sources.length ; ++i) {
			if (Shared.sources[i].uri == Shared.selectedSource) {
				return Shared.sources[i].title; 
			}
		}
		return source;
	}

	//*************************************************************************
	// Project oriented methods
	//*************************************************************************
	self.loadAllOntologies = function() {
		// Afficher un loader
		$scope.updatingOntologies = true;
		for (var i = 0 ; i < Shared.ontologies.length ; ++i) {
			++$scope.ontoRequestSent;
			self.loadOntology(Shared.ontologies[i]);
		}
	}
	
	self.loadOntology = function(ontology) {
		$http.get(Shared.moduleUri + "/ontology?src=" + ontology.uri)
		.success(function(data, status, headers, config) {
			var propArray = [];
			for (var prop in data.properties) {
				propArray.push({
					uri: prop,
					type: data.properties[prop].type,
					name: data.properties[prop].name,
					desc: data.properties[prop].desc,
					ranges: data.properties[prop].ranges,
					domains: data.properties[prop].domains
				});
			}
			$scope.loadedOntologies.push({
				uri: ontology.uri,
				name: ontology.title,
				classes: data.classes,
				properties: propArray
			});
			++$scope.ontoRequestReceived;
			Shared.loadedOntologies = $scope.loadedOntologies;
		})
		.error(function(data, status, headers, config) {
			self.findAlternativeUriFromLov(ontology);
		});
	}
	
	self.findAlternativeUriFromLov = function(ontology) {
		$http.get(Shared.baseUri + "/lov/vocabs?uri=" + ontology.uri)
		.success(function(data, status, headers, config) {
			if (data.hasOwnProperty("lastVersionReviewed")) {
				if(data.lastVersionReviewed) {
					self.loadOntologyFromLov(ontology, data.lastVersionReviewed.link);
				}
			}
		})
		.error(function(data, status, headers, config) {
			Shared.broadcastNotification({
				heading: "Couldn't retrieve ontology",
				message: "No alternative URI has been found for " + ontology.title + ".",
				type: "warning"
    		});
		});
	}
	
	self.loadOntologyFromLov = function(ontology, altUri) {
		$http.get(Shared.moduleUri + "/ontology?src=" + altUri)
		.success(function(data, status, headers, config) {
			var propArray = [];
			for (var prop in data.properties) {
				propArray.push({
					uri: prop,
					type: data.properties[prop].type,
					name: data.properties[prop].name,
					desc: data.properties[prop].desc,
					ranges: data.properties[prop].ranges,
					domains: data.properties[prop].domains
				});
			}
			$scope.loadedOntologies.push({
				uri: ontology.uri,
				name: ontology.title,
				classes: data.classes,
				properties: propArray
			});
			++$scope.ontoRequestReceived;
			Shared.loadedOntologies = $scope.loadedOntologies;
		})
		.error(function(data, status, headers, config) {
			Shared.broadcastNotification({
				heading: "Couldn't retrieve ontology",
				message: "The onology " + ontology.title + " has not been loaded properly.",
				type: "warning"
    		});
			++$scope.ontoRequestReceived;
		});
	}
	
	$scope.ontologiesLoaded = function() {
		return $scope.ontoRequestReceived >= $scope.ontoRequestSent;
	}
	
	$scope.searchOntologyProperties = function() {
		self.testClass();
		$scope.propertiesFound = [];
		$scope.propertiesFound = self.searchOntologyForProperties($scope.searchQuery);
		$scope.currentPage = 1;
	}
	
	// TODO un bug ici, parfois la mauvaise onto se ferme
	$scope.changeVisibility = function(uri) {
		var uriIndex = $scope.visibleOntologies.indexOf(uri);
		if (uriIndex != -1) {
			$scope.visibleOntologies.splice($scope.visibleOntologies[uriIndex], 1);
		}
		else {
			$scope.visibleOntologies.push(uri);
		}
	}
	
	$scope.isVisible = function(uri) {
		return $scope.visibleOntologies.indexOf(uri) != -1;
	}
	
	$scope.ontologyTreeClass = function(uri) {
		var css = "glyphicon glyphicon-";
		
		if ($scope.isVisible(uri)) {
			css += "folder-open";
		}
		else {
			css += "folder-close";
		}
		
		return css;
	}
	
	$scope.projectPropertiesFilter = function(property) {
		if (property.type != $scope.projectPropertyType) {
			return false;
		}
		else {
			if ($scope.ontologyStringFilter.trim() == "") {
				return true;
			}
			var val = $scope.ontologyStringFilter.toLowerCase();
			if(property.desc) {
				return property.name.toLowerCase().indexOf(val) != -1
					|| property.desc.toLowerCase().indexOf(val) != -1
					|| property.uri.toLowerCase().indexOf(val) != -1;
			}
			else {
				return property.name.toLowerCase().indexOf(val) != -1
					|| property.uri.toLowerCase().indexOf(val) != -1;
			}
		}
	}
	
	//*************************************************************************
	// LOV oriented methods
	//*************************************************************************
	$scope.hasResults = function() {
		return $scope.lovFilteredResults.length > 0 && ! $scope.searchingLov;
	}
	
	$scope.lovResultsText = function() {
		if ($scope.lovFilteredResults.length == 0 && ! $scope.searchingLov) {
			return "No result."
		}
		else {
			return "Results (" + $scope.lovFilteredResults.length + ")";
		}
	}
	
	self.filterByType = function(result) {
		var isDatatype = false;
		var isObject = false;
		var isProperty = false;
		
		for (var i = 0 ; i < result.types.length ; ++i) {
			if (result.types[i].uriPrefixed == "owl:DatatypeProperty") {
				isDatatype = true;
			}
			if (result.types[i].uriPrefixed == "owl:ObjectProperty") {
				isObject = true;
			}
			if (result.types[i].uriPrefixed == "rdf:Property") {
				isProperty = true;
			}
		}
		
		return isDatatype || (isProperty && ! isObject);
		
	}
	
	$scope.filterByType = function(result) {
		var isDatatype = false;
		var isObject = false;
		var isProperty = false;
		
		for (var i = 0 ; i < result.types.length ; ++i) {
			if (result.types[i].uriPrefixed == "owl:DatatypeProperty") {
				isDatatype = true;
			}
			if (result.types[i].uriPrefixed == "owl:ObjectProperty") {
				isObject = true;
			}
			if (result.types[i].uriPrefixed == "rdf:Property") {
				isProperty = true;
			}
		}
		
		return isDatatype || (isProperty && ! isObject);
		
	}
	
	$scope.filterByVocSpace = function(result) {
		if ($scope.vocSpaceFilter == undefined || $scope.vocSpaceFilter == "All") {
			return true;
		}
		for (var i = 0 ; i < result.vocSpaces.length ; ++i) {
			if (result.vocSpaces[i].label == $scope.vocSpaceFilter) {
				return true;
				break;
			}
		}
		return false;
	}
	
	self.filterLovResults = function(results) {
		var lovFilteredResults = [];
		for (var i = 0 ; i < results.length ; ++i) {
			if (self.filterByType(results[i])) {
				lovFilteredResults.push(results[i]);
			}
		}
		return lovFilteredResults;
	}
	
	$scope.filterLovResultsWithVocSpace = function() {
		$scope.lovFilteredResults = [];
		for (var i = 0 ; i < $scope.lovSearchResults.length ; ++i) {
			if ($scope.filterByType($scope.lovSearchResults[i])) {
				if ($scope.filterByVocSpace($scope.lovSearchResults[i])) {
					$scope.lovFilteredResults.push($scope.lovSearchResults[i]);
				}
			}
		}
	}
	
	$scope.getFirstVocSpace = function(result) {
		if (result.vocSpaces.length > 0) {
			return result.vocSpaces[0].label;
		}
		else {
			return "";
		}
	}
	
	$scope.setVocSpaceFilter = function(result) {
		$scope.vocSpaceFilter = $scope.getFirstVocSpace(result);
	}
	
	
	$scope.vocSpaceFiltered = function() {
		return $scope.vocSpaceFilter != "All";
	}
	
	$scope.resetVocSpaceFilter = function() {
		$scope.vocSpaceFilter = "All";
	}
	
	$scope.formatLovScore = function(result) {
		var score = result.score * 100;
		return score.toFixed(2);
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
				+ "&type=" + type + "&limit=100")
			.success(function(data, status, headers, config) {
				self.testClass();
				$scope.searchingLov = false;
				$scope.lovSearchResults = data.results;
				$scope.lovSearchCount = data.results.length;
				$scope.currentPage = 1;
				$scope.filterLovResultsWithVocSpace();
			})
			.error(function(data, status, headers, config) {
				$scope.searchingLov = false;
			});
	}
	
	//*************************************************************************
	// Auto search / Suggest
	//*************************************************************************
	self.addSuggestion = function(predicate, target) {
		if (predicate != undefined) {
			$scope.suggestions.push({
				'sourceId': predicate.name,
				'source': predicate.name,
				'sourceUri': predicate.uri,
				'targetUri': target.uri,
				'targetPrefixed': target.uriPrefixed,
				'targetVocabulary': target.vocabulary
			});
		}	
	}
	
	self.findMappings = function() {
// 		console.log("Finding mappings...");
//		console.log(JSON.stringify($scope.allLovResults));
		var sortedVocabularies = self.sortVocabularies($scope.allLovResults);
		self.createAutoMappings(sortedVocabularies);
//		self.createMappingsForEachPredicates();
		return JSON.stringify(sortedVocabularies);
	}
	
	$scope.autoMapping = function() {
		$scope.suggesting = true;
		$scope.isAutoMapping = true;
		$scope.numberOfPredicatesSearched = 0;
		$scope.allLovResults = [];
		self.loadLovPredicates();
		self.waitForResponse();
	}
	
	self.loadLovPredicates = function() {
// 		console.log("Requesting predicates");
		for (var i = 0 ; i < $scope.sourcePredicates.length ; ++i) {
			self.requestPredicate(i, $scope.allLovResults);
		}
	}
	
	self.waitForResponse = function() {
// 		console.log("numberOfPredicatesSearched : " + $scope.numberOfPredicatesSearched);
		if ($scope.numberOfPredicatesSearched < $scope.sourcePredicates.length) {
			$timeout(self.waitForResponse, 1000);
		}
		else {
			$scope.sortedVocabularies = self.findMappings();
			$scope.isAutoMapping = false;
			$scope.suggesting = false;
		}
	}
	
	self.requestPredicate = function(index, allLovResults) {
		if (index < $scope.sourcePredicates.length) {
			$http.get(Shared.baseUri + '/lov/search?q='+ $scope.sourcePredicates[index].name
					+ "&type=" + "http://www.w3.org/1999/02/22-rdf-syntax-ns%23Property")
				.success(function(data, status, headers, config) {
					allLovResults[index] = {};
					allLovResults[index].results = self.filterLovResults(data.results);
					allLovResults[index].sourceName = $scope.sourcePredicates[index].name;
					allLovResults[index].sourceUri = $scope.sourcePredicates[index].uri;
					++$scope.numberOfPredicatesSearched;
				})
				.error(function(data, status, headers, config) {
					allLovResults[index] = {};
					++$scope.numberOfPredicatesSearched;
				});
		}
	}
	
	self.sortVocabularies = function(lovResults) {
// 		console.log("Sorting vocabularies");
		var vocabList = new VocabList();
		var vocabularies = [];
		for (var i = 0 ; i < lovResults.length ; ++i) {
			vocabList.addVocabulary(lovResults[i]);
			self.vocabulariesUnion(vocabularies, lovResults[i]);
		}
		return vocabularies;
	}
	
	self.vocabulariesUnion = function(vocabularies, lovResults) {
		for (var i = 0 ; i < lovResults.results.length ; ++i) {
			var index = self.indexOfVocabulary(vocabularies, lovResults.results[i].vocabulary);
			// See if the vocabulary is already in our global list
			if (index != -1) {
				var predicateAlreadyIn = false;
				for ( var j = 0 ; j < vocabularies[index].predicates.length ; ++j ) {
					if (vocabularies[index].predicates[j].uri == lovResults.sourceUri) {
						predicateAlreadyIn = true;
						break;
					}
				}
				if ( ! predicateAlreadyIn ) {
				++vocabularies[index].count;
				vocabularies[index].score += lovResults.results[i].score;
				vocabularies[index].predicates.push({
					"name": lovResults.sourceName,
					"uri": lovResults.sourceUri,
					"targetUri": lovResults.results[i].uri,
					"targetUriPrefixed": lovResults.results[i].uriPrefixed,
					"score": lovResults.results[i].score
					});
				}
			}
			else { // new one
				vocabularies.push({
//					'uriPrefixed': lovResults.results[i].uriPrefixed,
//					'uri': lovResults.results[i].uri,
					'vocabulary': lovResults.results[i].vocabulary,
					'count': 1,
					'score': lovResults.results[i].score,
					'predicates': [{
						'name': lovResults.sourceName,
						'uri': lovResults.sourceUri,
						"targetUri": lovResults.results[i].uri,
						"targetUriPrefixed": lovResults.results[i].uriPrefixed,
						"score": lovResults.results[i].score
					}]
				});
			}
		}
	}
	
	self.indexOfVocabulary = function(vocabularyArray, vocabularyUri) {
		for (var i = 0 ; i < vocabularyArray.length ; ++i) {
			if (vocabularyArray[i].vocabulary == vocabularyUri) {
				return i;
			}
		}
		return -1;
	}
	
	self.createAutoMappings = function(sortedVocabularies) {
// 		console.log("Auto mapping starting");
		var it = 0;
		var maxIt = $scope.sourcePredicates.length * 3;
		var suggestedPredicates = 0;
		while (suggestedPredicates < $scope.sourcePredicates.length && it < maxIt) {
			var bestIndex = self.findBestVocabularyIndex(sortedVocabularies);
			if (bestIndex != -1) {
				for ( var i = 0 ; i < sortedVocabularies[bestIndex].predicates.length ; ++i ) {
					self.addSuggestion({
						"name": sortedVocabularies[bestIndex].predicates[i].name,
						"uri": sortedVocabularies[bestIndex].predicates[i].uri
					},
					{
						"uri": sortedVocabularies[bestIndex].predicates[i].targetUri,
						"uriPrefixed": sortedVocabularies[bestIndex].predicates[i].targetUriPrefixed,
						"vocabulary": sortedVocabularies[bestIndex].vocabulary
					});
					++suggestedPredicates;
				}
			
				// on copie le tableau sinon problème lors du removePredicatesFromSortedVocabularies
				var predicatesToRemove = sortedVocabularies[bestIndex].predicates.slice(0);
				self.removePredicatesFromSortedVocabularies(sortedVocabularies, predicatesToRemove);
				
			}
// 			$scope.selectedPredicateId = "";
// 			console.log("End Iteration " + it);
			++it;
		}
	}
	
	self.findBestVocabularyIndex = function(sortedVocabularies) {
		var maxCount = 0;
		var maxScore = 0;
		var bestVocabularyIndex = -1;
		for(var i = 0 ; i < sortedVocabularies.length ; ++i) {
			var vocab = sortedVocabularies[i];
			if ( vocab.count > maxCount) {
				maxCount = vocab.count;
				maxScore = vocab.score;
				bestVocabularyIndex = i;
			}
			else if ( vocab.count == maxCount ) {
				if ( vocab.score > maxScore) {
					maxScore = vocab.score;
					bestVocabularyIndex = i;
				}
			}
		}
		return bestVocabularyIndex;
		
	}
	
	self.removePredicatesFromSortedVocabularies = function(sortedVocabularies, predicatesToRemove) {
// 		console.log("Removing " + predicatesToRemove.length + " predicates");
		for (var i = 0 ; i < predicatesToRemove.length ; ++i) {
			self.removePredicateFromSortedVocabularies(sortedVocabularies, predicatesToRemove[i]);
		}
		
	}
	
	self.removePredicateFromSortedVocabularies = function(sortedVocabularies, predicate) {
		var vocabToRemove = [];
		
		for ( var i = 0 ; i < sortedVocabularies.length ; ++i ) {
			var indexesToRemove = [];
			var scoreToSubstract = 0;
			
			// look for things to remove
			for (var j = 0 ; j < sortedVocabularies[i].predicates.length ; ++j) {
				if ( sortedVocabularies[i].predicates[j].uri == predicate.uri ) {
					indexesToRemove.push(j);
					scoreToSubstract += predicate.score;
				}
			}
			
			// clean that up
			sortedVocabularies[i].score -= scoreToSubstract;
			for (var j = 0 ; j < indexesToRemove.length ; ++j) {
				sortedVocabularies[i].predicates.splice(indexesToRemove[j], 1);
				--sortedVocabularies[i].count;
			}
			
			// remember what vocabulary has become useless
			if (sortedVocabularies[i].count == 0) {
				vocabToRemove.push(i);
			}
		}
		// clean vocabularies
		for (var i = 0 ; i < vocabToRemove.length ; ++i ) {
			sortedVocabularies.splice(vocabToRemove[i], 1);
		}
	}
	
	self.findBestVocabularyForPredicate = function(predicate, sortedVocabularies) {
		var maxCount = 0;
		var maxScore = 0;
		var bestVocabulary = {};
		for(var i = 0 ; i < sortedVocabularies.length ; ++i) {
			var vocab = sortedVocabularies[i];
			if ( self.vocabularyContainsPredicate(vocab, predicate) > -1) {
				if ( vocab.count > maxCount) {
					maxCount = vocab.count;
					maxScore = vocab.score;
					bestVocabulary = vocab;
				}
				else if ( vocab.count == maxCount ) {
					if ( vocab.score > maxScore) {
						maxScore = vocab.score;
						bestVocabulary = vocab;
					}
				}
			}
		}
		return bestVocabulary;
	}
	
	self.vocabularyContainsPredicate = function(vocabulary, predicate) {
		for (var i = 0 ; i < vocabulary.predicates.length ; ++i) {
			if ( vocabulary.predicates[i].uri == predicate.uri ) return i;
		}
		return -1;
	}
	
	self.findSolutions = function(possibleMappings) {
		for (var i = 0 ; i < possibleMappings.length ; ++i) {
			
		}
	}
	
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
	
	$scope.addProjectMapping = function(ontology, property) {
		// create the target
		var target = {
			uri: property.uri,
			uriPrefixed: property.uri,
			name: property.name,
			vocabulary: ontology.uri
		};
		
		$scope.addMapping(target);
		
	}
	
	self.addMappingToArray = function(predicate, target) {
		if (predicate != undefined) {
// 			console.log("Adding the follow mapping : " + JSON.stringify(predicate) + " - " + JSON.stringify(target));
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
	
	$scope.addSuggestedMapping = function(mapping) {
		self.addMappingToArray(
			{
				'name': mapping.source,
				'uri': mapping.sourceUri
			},
			{
				'uri': mapping.targetUri,
				'uriPrefixed': mapping.targetPrefixed,
				'vocabulary': mapping.targetVocabulary
			});
		
		// Change selection
		if ($scope.sourcePredicates.length > 0) {
			$scope.selectedPredicateId = $scope.sourcePredicates[0].name;
		}
		else {
			$scope.selectedPredicateId = "";
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
		var indexToRemove = -1;
		for (var i = 0 ; i < $scope.sourcePredicates.length ; ++i) {
				if ( $scope.sourcePredicates[i].uri === predicateToRemove.uri ) {
					indexToRemove = i;
					break;
				}
		}
		if (indexToRemove != -1) {
			$scope.sourcePredicates.splice(indexToRemove, 1);
		}
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
		return Math.ceil($scope.lovFilteredResults.length / self.pageSize);
	}
	
	$scope.pageContent = function() {
		if ($scope.lovFilteredResults.length == 0) {
			return [];
		}
		
		var startIndex = ($scope.currentPage - 1) * self.pageSize;
		var endIndex = startIndex + self.pageSize;
		
		if (endIndex > $scope.lovFilteredResults.length) {
			endIndex = $scope.lovFilteredResults.length;
		}
		
		return $scope.lovFilteredResults.slice(startIndex, endIndex);
	}
	
	self.testClass = function() {
		if ( $('#pager div ul').attr('class') != 'pagination' )
			$('#pager div ul').addClass('pagination');
	}
	
	
	//*************************************************************************
	// Actions to execute immediately
	//*************************************************************************
	$scope.sourceName = self.getSourceName();
	
	Shared.broadcastCurrentStep(2);
	
	$scope.$watch('selectedPredicateId', function(newValue) {
		$scope.searchQuery = newValue;
	});
	
	$scope.$watch('vocSpaceFilter', function(newValue) {
		$scope.filterLovResultsWithVocSpace();
	});
	
	if(Shared.selectedSource == "") {
		$scope.goToSelect();
	}
	
	// Project ontologies
	if (jQuery.isEmptyObject(Shared.loadedOntologies)) {
		// load project ontologies
		self.loadAllOntologies();
	}
	else {
		$scope.loadedOntologies = Shared.loadedOntologies;
	}
	
	if (Shared.sourceData[Shared.selectedSource]) {
		$scope.suggestions = Shared.sourceData[Shared.selectedSource].suggestions;
		$scope.sourcePredicates = Shared.sourceData[Shared.selectedSource].properties;
		$scope.mappings = Shared.sourceData[Shared.selectedSource].mappings;
		$scope.loadingPredicates = false;
		$scope.selectedPredicateId = $scope.sourcePredicates[0].name;
	}
	else {
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
			Shared.sourceData[Shared.selectedSource] = {
				properties: $scope.sourcePredicates,
				suggestions: $scope.suggestions,
				mappings: $scope.mappings
			}
			$scope.selectedPredicateId = $scope.sourcePredicates[0].name;
			
			$scope.loadingPredicates = false;
			$scope.autoMapping();
		})
		.error(function(data, status, headers, config) {
			Shared.broadcastNotification({
				heading: "Source predicate error",
				message: "The service returned an error while fetching source predicates.",
				type: "danger"
			});
		});
	}
		
}

function MappingLovCtrl($scope, $location, $http, $timeout, Shared) {
	var self = this;
    self.url_prop = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?p WHERE { graph <" + Shared.selectedSource + "> { ?s ?p ?o . }}&max=25";
    self.url_type = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?o WHERE { graph <" + Shared.selectedSource + "> { ?s a ?o . }}&max=25";

    $scope.barLoaderSrc = Shared.moduleUri + "/static/img/bar_loader.gif";
    $scope.searchLoaderSrc = Shared.moduleUri + "/static/img/search_loader.gif";
    
    /* TODO : rdf:test donne un vocabulaire à "null" dans l'overview
     * keypress pour recherche lov
     *  */ 
    
	/* Data to work with */
	$scope.mode = "lov";
	$scope.sourcePredicates = [];
	$scope.sourceTypes = [];
	
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
	
	/* Scope functions */
	// Global
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
	
	// Predicates
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
	
	$scope.autoMappingDisabled = function() {
		return $scope.isAutoMapping || $scope.sourcePredicates.length == 0;
	}
	
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
	
	// Classes

	
	/* Lov search */
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
	
	
	// Pagination
	self.pageSize = 5;
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
	
	self.testClass = function() {
		if ( $('#pager div ul').attr('class') != 'pagination')
			$('#pager div ul').addClass('pagination');
	}
	
	// Auto search
	
	$scope.numberOfPredicatesSearched = 0;
	
	self.findMappings = function() {
		console.log("Finding mappings...");
//		console.log(JSON.stringify($scope.allLovResults));
		var sortedVocabularies = self.sortVocabularies($scope.allLovResults);
		self.createAutoMappings(sortedVocabularies);
//		self.createMappingsForEachPredicates();
		return JSON.stringify(sortedVocabularies);
	}
	
	$scope.autoMapping = function() {
		$scope.isAutoMapping = true;
		$scope.searchingLov = true;
		$scope.numberOfPredicatesSearched = 0;
		$scope.allLovResults = [];
		self.loadLovPredicates();
		self.waitForResponse();
	}
	
	self.loadLovPredicates = function() {
		console.log("Requesting predicates");
		for (var i = 0 ; i < $scope.sourcePredicates.length ; ++i) {
			self.requestPredicate(i, $scope.allLovResults);
		}
	}
	
	self.waitForResponse = function() {
		console.log("numberOfPredicatesSearched : " + $scope.numberOfPredicatesSearched);
		if ($scope.numberOfPredicatesSearched < $scope.sourcePredicates.length) {
			$timeout(self.waitForResponse, 1000);
		}
		else {
			$scope.sortedVocabularies = self.findMappings();
			$scope.isAutoMapping = false;
			$scope.searchingLov = false;
		}
	}
	
	self.requestPredicate = function(index, allLovResults) {
		if (index < $scope.sourcePredicates.length) {
			$http.get(Shared.baseUri + '/lov/search?q='+ $scope.sourcePredicates[index].name
					+ "&type=" + "http://www.w3.org/1999/02/22-rdf-syntax-ns%23Property")
				.success(function(data, status, headers, config) {
					allLovResults[index] = {};
					allLovResults[index].results = data.results;
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
	
//	self.loadLovPredicates = function() {
//		console.log("Aggregating predicates");
//		if ( $scope.numberOfPredicatesSearched == 0 ) {
//			self.requestPredicatesIterative(0, $scope.allLovResults);
//		}
//		else {
//			console.log("numberOfPredicatesSearched : " + $scope.numberOfPredicatesSearched);
//			if ($scope.numberOfPredicatesSearched < $scope.sourcePredicates.length) {
//				$timeout(self.loadLovPredicates, 1000);
//			}
//			else {
//				$scope.sortedVocabularies = self.findMappings();
//				$scope.isAutoMapping = false;
//			}
//		}
//	}
//	
//	self.requestPredicatesIterative = function(index, allLovResults) {
//		if (index < $scope.sourcePredicates.length) {
//			$http.get(Shared.baseUri + '/lov/search?q='+ $scope.sourcePredicates[index].name
//					+ "&type=" + "http://www.w3.org/1999/02/22-rdf-syntax-ns%23Property")
//				.success(function(data, status, headers, config) {
//					allLovResults[index] = {};
//					allLovResults[index].results = data.results;
//					allLovResults[index].sourceName = $scope.sourcePredicates[index].name;
//					allLovResults[index].sourceUri = $scope.sourcePredicates[index].uri;
//					++$scope.numberOfPredicatesSearched;
//					if ($scope.numberOfPredicatesSearched < $scope.sourcePredicates.length) {
//						self.requestPredicatesIterative(index + 1, allLovResults);
//						if ($scope.numberOfPredicatesSearched == 1) {
//							self.loadLovPredicates();
//						}
//					}
//				})
//				.error(function(data, status, headers, config) {
//					allLovResults[index] = {};
//					++$scope.numberOfPredicatesSearched;
//					if ($scope.numberOfPredicatesSearched < $scope.sourcePredicates.length) {
//						self.requestPredicatesIterative(index + 1, allLovResults);
//					}
//				});
//		}
//	}
	
	self.sortVocabularies = function(lovResults) {
		console.log("Sorting vocabularies");
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
		console.log("Auto mapping starting");
		var it = 0;
		while ( $scope.sourcePredicates.length > 0 && it < 30) {
			var bestIndex = self.findBestVocabularyIndex(sortedVocabularies);
			if (bestIndex != -1) {
				for ( var i = 0 ; i < sortedVocabularies[bestIndex].predicates.length ; ++i ) {
					self.addMappingToArray({
						"name": sortedVocabularies[bestIndex].predicates[i].name,
						"uri": sortedVocabularies[bestIndex].predicates[i].uri
					},
					{
						"uri": sortedVocabularies[bestIndex].predicates[i].targetUri,
						"uriPrefixed": sortedVocabularies[bestIndex].predicates[i].targetUriPrefixed,
						"vocabulary": sortedVocabularies[bestIndex].vocabulary
					});
				}
			}
			// on copie le tableau sinon problème lors du removePredicatesFromSortedVocabularies
			var predicatesToRemove = sortedVocabularies[bestIndex].predicates.slice(0);
			self.removePredicatesFromSortedVocabularies(sortedVocabularies, predicatesToRemove);
			$scope.selectedPredicateId = "";
			console.log("End Iteration " + it);
			console.log("Still " + $scope.sourcePredicates.length + " predicates to map.");
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
		console.log("Removing " + predicatesToRemove.length + " predicates");
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
	
	/* Actions to execute immediately */
	Shared.broadcastCurrentStep(2);
	
	if(Shared.selectedSource == "") {
		$scope.goToSelect();
	}
	
	$scope.$watch('selectedPredicateId', function(newValue) {
		$scope.searchQuery = newValue;
	});
	
	$http.get(self.url_prop)
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
		$scope.loadingPredicates = false;
//		self.displaySourceSelection(false);
//		self.selectPropertyToMap(self.propertiesToMap()[0]);
	})
	.error(function(data, status, headers, config) {
		// TODO
		alert("error");
	});
	
	$http.get(self.url_type)
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
	})
	.error(function(data, status, headers, config) {
		// TODO
		alert("error");
	});
}

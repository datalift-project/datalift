function RefineCtrl($scope, $location, $http, $timeout, Shared) {
	var self = this;
	
	//*************************************************************************
	// Static data
	//*************************************************************************
	self.url_type = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?o WHERE { graph <" + Shared.selectedSource + "> { ?s a ?o . }}&max=25";
	self.url_classPred = Shared.baseUri + "/sparql?default-graph-uri=internal&query=SELECT DISTINCT ?c ?p WHERE { graph <" + Shared.selectedSource + "> { ?s ?p ?o. ?s a ?c . }}&max=25";
	
	
	//*************************************************************************
	// Data bindings
	//*************************************************************************
	$scope.loaderTxt = "Searching corresponding types...";
	$scope.loaderSrc = Shared.moduleUri + "/static/img/loader.gif";
	$scope.loading = true;
	
	$scope.predicateClassMap = {};
	$scope.classPredicateMap = {};
	$scope.propertyMappings = Shared.mappings;
	$scope.classMappings = [];
	$scope.sourceTypes = [];
	$scope.addedTypes = [];
	$scope.loadedOntologies = {};
	$scope.requestSent = 0;
	$scope.ontologiesReceived = 0;
	
	//
	$scope.objects = [];
	$scope.objectNextId = 0;
	$scope.objectEditing = -1;
	$scope.selectedObjectProperty = {};
	$scope.visibleOntologies = [];
	// il faut gérer la valeur dans un object car on la modifie dans un autre scope.
	$scope.ontologyStringFilter = {value: ""};
	$scope.targetObject = {id: ""};
	$scope.firstObjectAdded = true;
	
	//*************************************************************************
	// Methods
	//*************************************************************************
	$scope.goToConvert = function() {
		Shared.objects = $scope.objects;
		$location.path("/convert/");
	}
	
	$scope.checkClasses = function() {
		console.log("check classes");
	}
	//*************************************************************************
	// Objects
	//*************************************************************************
	$scope.createObject = function() {
		var obj = new MappingObject($scope.objectNextId);
		$scope.objects.push(obj);
		++$scope.objectNextId;
		
		return obj;
	}
	
	$scope.removeObject = function(id) {
		var len = $scope.objects.length;
		var index = -1;
		for (var i = 0 ; i < len ; ++i) {
				if ($scope.objects[i].id == id) {
					index = i;
					break;
				}
		}
		
		$scope.objects.splice(index, 1);
	}
	
	$scope.getObject = function(id) {
		for (var i = 0 ; i < $scope.objects.length ; ++i) {
				if ($scope.objects[i].id == id) {
					return $scope.objects[i];
				}
		}
		return null;
	}
	
	$scope.isEditing = function(id) {
		return $scope.objectEditing == id;
	}
	
	$scope.setObjectPropertyPanel = function(id) {
		$scope.objectEditing = id;
		$scope.selectedObjectProperty = {};
		$scope.targetObject.id = "";
	}
	
	$scope.selectObjectProperty = function(property) {
		$scope.selectedObjectProperty = property;
	}
	
	$scope.isObjectPropertySelected = function(property) {
		return $scope.selectedObjectProperty == property;
	}
	
	$scope.addObjectProperty = function() {
		var sourceObject = $scope.getObject($scope.objectEditing);
		var targetObject = {};
		
		if ($scope.targetObject.id === "") {
			targetObject = $scope.createObject();
		}
		else {
			targetObject = $scope.getObject($scope.targetObject.id);
		}
		
		var classes = [];
		if ($scope.selectedObjectProperty.ranges) {
			classes = $scope.selectedObjectProperty.ranges;
		}
		
		for (var i = 0 ; i < classes.length ; ++i) {
			targetObject.addClass(classes[i]);
		}
		
		sourceObject.addObjectProperty({
			predicate: $scope.selectedObjectProperty.uri,
			targetId: targetObject.id
		});
		
		$scope.setObjectPropertyPanel(-1);
		
		if ($scope.firstObjectAdded) {
			$scope.firstObjectAdded = false;
			Shared.broadcastNotification({
				heading: "Tip",
				message: "You can drag and drop mapped properties from one object to another.",
				type: "info"
    		});
		}
	}
	
	$scope.moveToNewObject = function(mapping) {
		var sourceObject = $scope.getObject($scope.objectEditing);
		var targetObject = $scope.createObject();
		
		sourceObject.removeMappedProperty(mapping);
		targetObject.addMappedProperty(mapping);
		
		// TODO add and remove class
	}
	
	$scope.filterObjectProperties = function(ontology) {
		var filteredProperties = [];
		for (var prop in ontology.properties) {
			if (ontology.properties[prop].type == "ObjectProperty") {
				filteredProperties.push({
					uri: prop,
					type: ontology.properties[prop].type,
					name: ontology.properties[prop].name,
					desc: ontology.properties[prop].desc,
					ranges: ontology.properties[prop].ranges,
					domains: ontology.properties[prop].domains
				});
			}
		}
		return filteredProperties;
	}
	$scope.objectPropertiesFilter = function(property) {
		if (property.type != "ObjectProperty") {
			return false;
		}
		else {
			if ($scope.ontologyStringFilter.value.trim() == "") {
				return true;
			}
			var val = $scope.ontologyStringFilter.value.toLowerCase();
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
	
	$scope.changeVisibility = function(uri) {
		var index = $scope.visibleOntologies.indexOf(uri);
		if (index == -1) {
			$scope.visibleOntologies.push(uri);
		}
		else {
			$scope.visibleOntologies.splice(index, 1);
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
	//*************************************************************************
	// Load ontologies
	//*************************************************************************
	self.loadAllOntologies = function() {
		
		var ontoToSearch = [];
		
		// What are the ontologies used in the mappings ?
		for( var i = 0 ; i < Shared.mappings.length ; ++i ) {
// 			console.log("mapping " + i + " : " + JSON.stringify(Shared.mappings[i]));
			if(ontoToSearch.indexOf(Shared.mappings[i].targetVocabulary) == -1) {
				ontoToSearch.push(Shared.mappings[i].targetVocabulary);
			}
		}
		
		// fetch the ontologies
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
	
	self.loadOntology = function(ontology) {
		$http.get(Shared.baseUri + "/mapper/ontology?src=" + ontology)
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
			$scope.loadedOntologies[ontology] = {
				uri: ontology,
				name: data.name,
				classes: data.classes,
				properties: data.properties,
				propertiesArray: propArray
			};
			++$scope.ontologiesReceived;
		})
		.error(function(data, status, headers, config) {
			console.log("no ontology for " + ontology);
			self.findAlternativeUriFromLov(ontology);
		});
	}
	
	self.findAlternativeUriFromLov = function(ontology) {
		$http.get(Shared.baseUri + "/lov/vocabs?uri=" + ontology)
		.success(function(data, status, headers, config) {
			if (data.hasOwnProperty("lastVersionReviewed")) {
				self.loadOntologyFromLov(ontology, data.lastVersionReviewed.link);
			}
		})
		.error(function(data, status, headers, config) {
			Shared.broadcastNotification({
				heading: "Couldn't retrieve ontology",
				message: "The onology " + ontology + " has not been loaded properly.",
				type: "warning"
    		});
			++$scope.ontologiesReceived;
		});
	}
	
	self.loadOntologyFromLov = function(ontology, altUri) {
		$http.get(Shared.baseUri + "/mapper/ontology?src=" + altUri)
		.success(function(data, status, headers, config) {
			$scope.loadedOntologies[ontology] = data;
			++$scope.ontologiesReceived;
		})
		.error(function(data, status, headers, config) {
			Shared.broadcastNotification({
				heading: "Couldn't retrieve ontology",
				message: "The onology " + ontology + " has not been loaded properly.",
				type: "warning"
    		});
			++$scope.ontologiesReceived;
		});
	}
	
	//*************************************************************************
	// Classes mappings
	//*************************************************************************
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
		
//		console.log($scope.classScript);

		$scope.createObject();
		for (var i = 0 ; i < $scope.classMappings.length ; ++i) {
			if ($scope.objects[0].currentClasses.indexOf($scope.classMappings[i].sourceClass) == -1) {
				$scope.objects[0].currentClasses.push($scope.classMappings[i].sourceClass);
			}
			$scope.objects[0].addClass($scope.classMappings[i].targetClass);
		}
		
		for (var i = 0 ; i < $scope.propertyMappings.length ; ++i) {
			$scope.objects[0].addMappedProperty($scope.propertyMappings[i]);
		}
		
		$scope.loading = false;
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
			if (
				m.sourcePredicate == sourcePredicate && m.targetClass == targetClass
 					|| m.sourceClass == sourceClass && m.targetClass == targetClass
				
			) {
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
	
	//*************************************************************************
	// Actions to execute immediately
	//*************************************************************************
	Shared.broadcastCurrentStep(3);
	
	// Fetch classes and predicates to construct a class - predicate map
	$http.get(self.url_classPred)
	.success(function(data, status, headers, config) {
		for (  var i = 0 ; i < data.results.bindings.length ; ++i) {
			var c = data.results.bindings[i].c.value;
			var p = data.results.bindings[i].p.value;
			// ajout à la map
			if ( p != 'http://www.w3.org/1999/02/22-rdf-syntax-ns#type') {
				// pred - class
				if ( ! $scope.predicateClassMap[p] ) {
					$scope.predicateClassMap[p] = [];
				}
				if ( $scope.predicateClassMap[p].indexOf(c) == -1) {
						$scope.predicateClassMap[p].push(c);
				}
				// class - pred
				if ( ! $scope.classPredicateMap[c] ) {
					$scope.classPredicateMap[c] = [];
				}
				if ( $scope.classPredicateMap[c].indexOf(p) == -1) {
						$scope.classPredicateMap[c].push(p);
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

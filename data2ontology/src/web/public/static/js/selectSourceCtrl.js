function SelectSourceCtrl($scope, $location, $http, Shared) {
	var self = this;
	
	self.vocSpacesLoaded = false;
	
	$scope.sources = Shared.sources;
	$scope.projectSourcesName = Shared.projectSourcesName;
	
	$scope.goToMap = function() {
		Shared.selectedSource = $scope.selectedSource;
		Shared.targetSrcGraph = $scope.targetSrcGraph;
		Shared.targetSrcName = $scope.targetSrcName;
		$location.path("/map/");
	}
	
// 	$scope.isTargetNameValid = function() {
// 		if ( ! $scope.targetSrcName) {
// 			return false;
// 		}
// 		if ( $scope.targetSrcName.trim() == "" ) {
// 			return false;
// 		}
// 		if ($scope.projectSourcesName.indexOf($scope.targetSrcName.trim()) != -1) {
// 			return false;
// 		}
// 		return true;
// 	}
// 	
// 	$scope.nameChanged = function() {
// 		if ( $scope.targetSrcName.trim() == "" ) {
// 			$scope.nameMsg = "Name is empty";
// 			return;
// 		}
// 		if ($scope.projectSourcesName.indexOf($scope.targetSrcName.trim()) != -1) {
// 			$scope.nameMsg = "Name already exists";
// 			return;
// 		}
// 		$scope.nameMsg = "";
// 	}
// 	
// 	$scope.isTargetGraphValid = function() {
// 		if ( ! $scope.targetSrcGraph) {
// 			return false;
// 		}
// 		if ( $scope.targetSrcGraph.trim() == "" ) {
// 			return false;
// 		}
// 		for (var i = 0; i < $scope.sources.length ; ++i) {
// 			if ($scope.targetSrcGraph.trim() == $scope.sources[i].uri) {
// 				return false;
// 			}
// 		}
// 		return true;
// 	}
// 	
// 	$scope.graphChanged = function() {
// 		if ( $scope.targetSrcGraph.trim() == "" ) {
// 			$scope.graphMsg = "Graph is empty";
// 			return;
// 		}
// 		for (var i = 0; i < $scope.sources.length ; ++i) {
// 			if ($scope.targetSrcGraph.trim() == $scope.sources[i].uri) {
// 				$scope.graphMsg = "Graph already exists";
// 				return;
// 			}
// 		}
// 		$scope.graphMsg = "";
// 	}
	
	$scope.isFormInvalid = function() {
		return $scope.selectedSource == undefined
				|| ! self.vocSpacesLoaded;
	}
	
	$scope.selectSourceClass = function() {
		if ($scope.selectedSource == undefined) {
			return "form-group has-error";
		}
		else {
			return "form-group has-success";
		}
	}
	
// 	$scope.targetNameClass = function() {
// 		if ($scope.isTargetNameValid()) {
// 			return "form-group has-success";
// 		}
// 		else {
// 			return "form-group has-error";
// 		}
// 	}
// 	
// 	$scope.targetGraphClass = function() {
// 		if ($scope.isTargetGraphValid()) {
// 			return "form-group has-success";
// 		}
// 		else {
// 			return "form-group has-error";
// 		}
// 	}
	
	$scope.sourceChanged = function() {
		if ($scope.selectedSource) {
			$scope.targetSrcGraph = $scope.selectedSource + "-mapped";
			
			for (var i = 0 ; i < $scope.sources.length ; ++i) {
				if ($scope.selectedSource == $scope.sources[i].uri) {
					$scope.targetSrcName = $scope.sources[i].title + " - mapped";
					break;
				}
			}
		}
	}

	/* Actions to execute immediately */
	Shared.broadcastCurrentStep(1);
	
	$http.get(Shared.baseUri + '/lov/vocSpaces')
	.success(function(data, status, headers, config) {
		Shared.vocSpaces = data;
		self.vocSpacesLoaded = true;
	})
	.error(function(data, status, headers, config) {
		
	});
	
}

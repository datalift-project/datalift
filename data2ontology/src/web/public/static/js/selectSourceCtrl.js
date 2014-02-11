function SelectSourceCtrl($scope, $location, $http, Shared) {
	var self = this;
	
	self.vocSpacesLoaded = false;
	
	$scope.sources = Shared.sources;
	$scope.projectSourcesName = Shared.projectSourcesName;
	
	$scope.goToMap = function() {
		Shared.selectedSource = $scope.selectedSource;
		Shared.targetSrcGraph = $scope.targetSrcGraph;
		Shared.targetSrcName = $scope.targetSrcName;
		$location.path("/match/");
	}
	
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
	
	$scope.sourceChanged = function() {
		if ($scope.selectedSource) {
			$scope.targetSrcGraph = $scope.selectedSource + "-matched";
			
			for (var i = 0 ; i < $scope.sources.length ; ++i) {
				if ($scope.selectedSource == $scope.sources[i].uri) {
					$scope.targetSrcName = $scope.sources[i].title + " - matched";
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

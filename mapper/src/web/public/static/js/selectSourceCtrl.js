function SelectSourceCtrl($scope, $location, Shared) {
	var self = this;

	$scope.sources = Shared.sources;
	
	$scope.goToLov = function() {
		Shared.selectedSource = $scope.selectedSource;
		$location.path("/mapping-lov/");
	}
	
	$scope.goToProject = function() {
		Shared.selectedSource = $scope.selectedSource;
		$location.path("/mapping-project/");
	}
	
	$scope.isSourceSelected = function() {
		return $scope.selectedSource == undefined;
	}
	
	$scope.selectSourceClass = function() {
		if ($scope.selectedSource == undefined) {
			return "form-group has-error";
		}
		else {
			return "form-group has-success";
		}
	}

	/* Actions to execute immediately */
	Shared.broadcastCurrentStep(1);
	
}

function AppCtrl($scope, Shared) {

	var self = this;
	
	$scope.steps = [
		{"num": 1,
		 "name": "Select",
		 "link": "#/select-source"},
		{"num": 2,
		 "name": "Map",
		 "link": "#/mapping-lov"},
		{"num": 3,
		 "name": "Convert",
		 "link": "#/convert"}
	];
	
	$scope.$on('handleCurrentStep', function() {
		$scope.currentStep = Shared.currentStep;
	});
	
	
	$scope.getStepClass = function(stepNum) {
		if (stepNum > $scope.currentStep) {
			return "step-disabled";
		}
		else if (stepNum == $scope.currentStep) {
			return "step-active";
		}
		return "step-done";
	}

}

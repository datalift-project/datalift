function AppCtrl($scope, $location, Shared) {

	var self = this;

	// ------
	// Steps
	$scope.steps = [
		{"num": 1,
		 "name": "Select",
		 "link": "#/select"},
		{"num": 2,
		 "name": "Match",
		 "link": "#/map"},
		{"num": 3,
		 "name": "Refine",
		 "link": "#/refine"},
		{"num": 4,
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
	
	// --------------
	// Notifications
	$scope.hotNotifications = 0;
	$scope.notifications = [];
	
	$scope.$on('handleNotification', function(event, notification) {
		var notif = [];
		notif.push(notification);
		$scope.notifications = notif.concat($scope.notifications);
		++$scope.hotNotifications;
	});
	
	$scope.computeNotificationCssClass = function(notification) {
		var types = ["info", "success", "warning", "danger"];
		var cssClass = "alert alert-info";
		if (notification.hasOwnProperty("type")) {
			if (types.indexOf(notification.type) != -1) {
				cssClass += "alert alert-" + notification.type;
			}
		}
		
		return cssClass;
	}
	
	$scope.displayNotifications = function() {
		
	}
	
	$scope.removeNotification = function(notification) {
		$scope.notifications.splice($scope.notifications.indexOf(notification), 1);
		--$scope.hotNotifications;
	}

}

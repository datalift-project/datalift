function AppCtrl($scope, $location, $http, $timeout, Shared) {

	var self = this;
	self.lovUrl =  Shared.baseUri + "/lov/";
	
	$scope.lastLovUpdate = "";
	$scope.updating = false;

	// ------
	// Steps
	$scope.steps = [
		{"num": 1,
		 "name": "Select",
		 "link": "#/select"},
		{"num": 2,
		 "name": "Match",
		 "link": "#/match"},
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
	
	$scope.updateLov = function() {
		$scope.updating = true;
		$scope.lastLovUpdate = "Please wait... This could take a minute or two.";
		$http.get(self.lovUrl + "update")
		.success(function(data, status, headers, config) {
			$scope.lastLovUpdate = data.date;
			$scope.updating = false;
		})
		.error(function(data, status, headers, config) {
			$scope.updating = false;
			$scope.lastLovUpdate = "";
		});
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
	
	//
	$timeout(function() {
		$http.get(self.lovUrl + "lastLovUpdate")
		.success(function(data, status, headers, config) {
			$scope.lastLovUpdate = data.date;
		})
		.error(function(data, status, headers, config) {
		});
	});

}

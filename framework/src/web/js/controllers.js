'use strict';

/////////////////
// APP PROJECT //
/////////////////

var app = angular.module('project', []);

/*
app.directive('ngOntologyTitle', function(){
	return function(scope, elem, attrs) {
		console.log(elem);
		console.log(attrs);
	}
})
*/

app.controller('projectController', function ($scope, $http){
	
	$scope.projects = [];
	$scope.initialize = function(uri, description, title, license, owner, creation_date, modification_date)
	{
		$scope.projects.push({
			uri : uri, 
			description : description, 
			title : title, 
			license : license, 
			owner : owner, 
			creation_date : creation_date, 
			modification_date : modification_date
			});
		console.log($scope.projects);
	}
	
	$scope.deleteSource = function(projectId, sourceUri, baseUri) 
	{	
		window.location = projectId + "/source/delete?uri=" + sourceUri;
	}
	
	$scope.applicableModules = function(projectId, sourceUri)
	{
		//alert(sourceUri);
		var sourceId = sourceUri.substr(sourceUri.lastIndexOf('/') + 1);
		window.location = projectId + "/applicable?source=" + sourceId;
	}
	
	$scope.uploadOntology = function(projectId) 
	{
		var target = projectId + "/ontologyupload";
		var title = ($('#ontology-title').val());
		var source_url = ($('#ontology-cource-url').val());
		
		$.post(target, {"title": title, "source_url": source_url})
		.done(function(data, status, xhr) {
			window.location = xhr.getRespondHeader('Location');
		})
		.fail(function() {
			alert("An error occured during the Ontology upload");
		});
	}
	
	$scope.deleteOntology = function(projectId, ontoTitle)
	{
		window.location = projectId + "/ontology/" + ontoTitle + "/delete";
	}
	
});



///////////////////////////
// APP MODULE CONTROLLER //
///////////////////////////

var app = angular.module('module', []);

app.controller('moduleController', function ($scope, $location, $http) {

	$scope.modules = [];
	
	$scope.initialize = function(label, method, position, uri) 
	{
		$scope.modules.push({
			label : label, 
			method : method, 
			position : position, 
			uri : uri
			});
		
		$scope.getModulesLevel();
	}
    
	
	self.objToString = function (obj) 
	{
	    var str = '';
	    for (var p in obj) {
	        if (obj.hasOwnProperty(p)) {
	            str += obj[p];
	        }
	    }
	    return (str);
	}
	
	$scope.getModulesLevel = function() 
	{	
		$scope.modulesLevel1 = [];
		$scope.modulesLevel2 = [];
		$scope.modulesLevel3 = [];
		$scope.modulesLevel4 = [];
		
		for (var module in $scope.modules)
        {	
			var mod = $scope.modules[module];
		    if ($scope.modules.hasOwnProperty(module) && parseInt(objToString(mod.position), 10) < 1000)
		    	{
					$scope.modulesLevel1.push({label : mod.label, method : mod.method, position : mod.position, uri : mod.uri});
					$scope.moduleLevel1 = $scope.modulesLevel1[0];
		    	}
		    else if ($scope.modules.hasOwnProperty(module) && parseInt(objToString(mod.position), 10) > 999 && parseInt(objToString(mod.position), 10) < 10000)
		    	{
		    		$scope.modulesLevel2.push({label : mod.label, method : mod.method, position : mod.position, uri : mod.uri});
		    		$scope.moduleLevel2 = $scope.modulesLevel2[0];
		    	}
    		else if ($scope.modules.hasOwnProperty(module) && parseInt(objToString(mod.position), 10) > 9999 && parseInt(objToString(mod.position), 10) < 20000)
		    	{
		    		$scope.modulesLevel3.push({label : mod.label, method : mod.method, position : mod.position, uri : mod.uri});
		    		$scope.moduleLevel3 = $scope.modulesLevel3[0];
		    	}	
		    else if ($scope.modules.hasOwnProperty(module) && parseInt(objToString(mod.position), 10) > 19999 && parseInt(objToString(mod.position), 10) < 30000)
		    	{
		    		$scope.modulesLevel4.push({label : mod.label, method : mod.method, position : mod.position, uri : mod.uri});
		    		$scope.moduleLevel4 = $scope.modulesLevel4[0];
		    	}
    	}
	}
	
	$scope.moduleRedirectL1 = function(baseUri)
	{
		var target = baseUri + '/' + objToString($scope.moduleLevel1["uri"]);
		if (objToString($scope.moduleLevel1["method"]) == "GET")
			window.location = target;
		else if (objToString($scope.moduleLevel1["method"]) == "POST")
			{
				$.post(target, {"title": title, "source_url": source_url})
				.done(function(data, status, xhr) {
					window.location = xhr.getRespondHeader('Location');
				})
				.fail(function() {
					alert("An error occured during the execution of the module.");
				});
			}
	}
	
	$scope.moduleRedirectL2 = function(baseUri)
	{
		var target = baseUri + '/' + objToString($scope.moduleLevel2["uri"]);
		if (objToString($scope.moduleLevel2["method"]) == "GET")
			window.location = target;
		else if (objToString($scope.moduleLevel2["method"]) == "POST")
			{
				$.post(target, {"title": title, "source_url": source_url})
				.done(function(data, status, xhr) {
					window.location = xhr.getRespondHeader('Location');
				})
				.fail(function() {
					alert("An error occured during the execution of the module.");
				});
			}
	}
	
	$scope.moduleRedirectL3 = function(baseUri)
	{
		var target = baseUri + '/' + objToString($scope.moduleLevel3["uri"]);
		if (objToString($scope.moduleLevel3["method"]) == "GET")
			window.location = target;
		else if (objToString($scope.moduleLevel3["method"]) == "POST")
			{
				$.post(target, {"title": title, "source_url": source_url})
				.done(function(data, status, xhr) {
					window.location = xhr.getRespondHeader('Location');
				})
				.fail(function() {
					alert("An error occured during the execution of the module.");
				});
			}
	}
	
	$scope.moduleRedirectL4 = function(baseUri)
	{
		var target = baseUri + '/' + objToString($scope.moduleLevel4["uri"]);
		if (objToString($scope.moduleLevel4["method"]) == "GET")
			window.location = target;
		else if (objToString($scope.moduleLevel4["method"]) == "POST")
			{
				$.post(target, {"title": title, "source_url": source_url})
				.done(function(data, status, xhr) {
					window.location = xhr.getRespondHeader('Location');
				})
				.fail(function() {
					alert("An error occured during the execution of the module.");
				});
			}
	}
});

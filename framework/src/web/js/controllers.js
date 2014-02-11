'use strict';


/////////////////
// APP PROJECT //
/////////////////

var app = angular.module('project', []);

app.controller('projectController', function ($scope, $http){
	
	$scope.projects = allProjects;
	$scope.licenses = allLicenses;
	$scope.curProj = currentProject;
	$scope.limitToDescription = 3
	
	$scope.getRssFeed = function()
	{
		$scope.rssItems = '';
		jQuery(function() {
		
		    jQuery.getFeed({
		        url: currentProject.uri+'/feed',
		        success: function(feed) {
		        	//console.log(feed);
		        	$scope.rssFeedTitle = feed.title;
		            /*jQuery('#rss_feed').append('<h4 class="pagination-centered">'
		            + feed.title
		            + '</h4>');
		            
		            var html = '';*/
		            
		            for(var i = 0; i < feed.items.length && i < 5; i++) {
		            
		                var item = feed.items[i];
		                
		                
	                	//$scope.rssItems += item.description;
	                	//console.log($scope.rssItems);
		                
		                /*html += '<a href="'
		                + item.link
		                + '">'
		                + item.title
		                + '</a>';
		                
		                html += '<div class="updated">'
		                + item.id
		                + '</div>';
		                
		                html += '<div class="updated">'
		                    + item.description
		                    + '</div>';*/
		            }
		            
		            //jQuery('#rss_feed').append(html);*/
		            //console.log($scope.rssItems);
		        }    
		    });
		    
		});
		console.log("rssFeedTitle = "+ $scope.rssFeedTitle);
	}
	
	
	$scope.deleteSource = function(projectId, sourceUri, baseUri) 
	{	
		window.location = projectId + "/source/delete?uri=" + sourceUri;
	}
	
	$scope.applicableModules = function(projectId, sourceUri)
	{
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
	
	/*
	**$scope.testNotemodule = function()
	**{
	**	alert(toto);
	**}
	*/
});

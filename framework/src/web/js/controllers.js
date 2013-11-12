'use strict';

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
	
	
	
	$scope.deleteSource = function(projectId, sourceUri, baseUri) 
	{	
		window.location = projectId + "/source/delete?uri=" + sourceUri;
	}
	
	$scope.uploadOntology = function(projectId) 
	{
		var target = projectId + "/ontologyupload";
		var title = ($('#ontology-title').val());
		var source_url = ($('#ontology-cource-url').val());
		
		alert(target);
		alert(title);
		alert(source_url);
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

/*
function ontologyController($scope, $http) {
	
	
	$scope.uploadOntology = function(sourceUrl, tilte)
	{
		alert(sourceUrl + ' ---- ' + tilte);
	}

}
*/
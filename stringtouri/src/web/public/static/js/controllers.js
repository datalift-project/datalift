angular.module("predicateChooserApp.controllers",[])
	.controller('SelectionCtrl', function($scope,chooserInterface,$http,$location) {
		$scope.choosers = {source: {name:"source", value:{}},
						   target: {name:"target",value:{}} };
		$scope.validSelection = false;
		$scope.linkingPredicate;
		$scope.canMoveForward = false;
		
		var previewDataColumns=  
	        [
	       		{
					property: 'subject',
					label: $("#PreviewResult").attr("data-subject-label"),
					sortable: true
				},
				{
					property: 'predicate',
					label: $("#PreviewResult").attr("data-predicate-label"),
					sortable: true
				},
				{
					property: 'object',
					label: $("#PreviewResult").attr("data-object-label"),
					sortable: true
				}
			];
        var linkingDataSource = new PreviewDataSource(previewDataColumns);	

		$scope.$on("ChooserUpdate",function(obj,updatedChooserValues){
			var selectedSource = $scope.choosers.source.value;
       		var selectedTarget = $scope.choosers.target.value;
       		if(updatedChooserValues.id == "target"){
				$scope.linkingPredicate = selectedTarget.predicate;				
			}
			$scope.choosers[updatedChooserValues.id].value = updatedChooserValues.value;
			if(!($.isEmptyObject(selectedSource)) && !($.isEmptyObject(selectedTarget))){
				$scope.validSelection = selectedSource.valid && selectedTarget.valid;
			}
			$scope.canMoveForward = $scope.validSelection && isURL($scope.linkingPredicate);
		});

		$scope.$watch("newSourceForm.$valid", function(newVal){
			$scope.canMoveForward = newVal;	
		});


		$scope.enableNextButton = function(){
			$scope.canMoveForward = true;	
		}
		
		function isURL(str){
        	var pattern = new RegExp(/[-a-zA-Z0-9@:%_\+.~#?&//=]{2,256}\.[a-z]{2,4}\b(\/[-a-zA-Z0-9@:%_\+.~#?&//=]*)?/gi);
           	return pattern.test(str);
		}

		$scope.$watch("linkingPredicate",function(value){
			$scope.canMoveForward = isURL(value) && $scope.validSelection;
		});

		function getProjectUrl(){
			return $.url().param("project");
		}

		function getLinkingParams(){
			var selectedSource = $scope.choosers.source.value;
	       	var selectedTarget = $scope.choosers.target.value;
	       	return {
				"datasetSource": selectedSource.dataset,
			    "classSource": selectedSource.class,
			    "predicateSource": selectedSource.predicate,
			    "datasetTarget": selectedTarget.dataset,
			    "classTarget": selectedTarget.class,
			    "predicateTarget": selectedTarget.predicate,
			    "predicateLinking": $scope.linkingPredicate,
			    "project": getProjectUrl()
	       	} 
		}

		$scope.handleStepResult = function(){
			var currentStep = $("#moduleWizard").wizard("selectedItem").step; 
			var currentUrl = $location.absUrl();
	       	var moduleUrl = currentUrl.substring(0,currentUrl.indexOf("?"));
			switch(currentStep){
				case 1:
	       			//send ajax request
	       			var previewUrl = moduleUrl + "/preview"; 
	       			$http({
	    				url: previewUrl, 
	    				method: "GET",
	    				params: getLinkingParams()
	 				}).success(function(data){
	       				linkingDataSource.setData(data);
	       				$('#PreviewResult').datagrid({
	                		dataSource: linkingDataSource
	                    });
	                    
	                    $('#PreviewResult').datagrid("reload");
	                    $scope.canMoveForward = !linkingDataSource.isEmpty();
	                });
	            break;
            
            	case 2:
					$scope.canMoveForward = $scope.newSourceForm.$valid ; 
				break;

				case 3:
					var saveUrl = moduleUrl + "/save"; 
					var saveParams = getLinkingParams();
					//add the other parameters for saving
					saveParams.newSourceContext = $scope.targetURLNew;
					saveParams.newSourceName = $scope.sourceName;
					saveParams.newSourceDescription=$scope.sourceDescription;
					$http({
		    			url: saveUrl, 
		    			method: "POST",
		    			params: saveParams
		 			}).success(function(){
		 				window.location.replace(getProjectUrl()+"#source");
		 			});
				break;

            	
            }           	
            
       	};

      
    })
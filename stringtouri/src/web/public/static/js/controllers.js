angular.module("predicateChooserApp.controllers",[])
	.controller('SelectionCtrl', function($scope,chooserInterface,$http,$location) {
		$scope.choosers = {source: {name:"source", value:{}},
						   target: {name:"target",value:{}} };
		$scope.validSelection = false;
		$scope.linkingPredicate;
		$scope.canMoveForward = false;
		$scope.keepTargetTriples = true;

		var MAX_PREVIEW_ROWS_TO_DISPLAY = 10;
		var EXTERNAL_PROJECT_IDENTIFIER = "ext";
		
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

        Array.prototype.clone = function(){
        	return this.slice(0);
        }

		$scope.$on("ChooserUpdate",function(obj,updatedChooserValues){
			var selectedSource = $scope.choosers.source.value;
       		var selectedTarget = $scope.choosers.target.value;
       		if(updatedChooserValues.id == "target"){
				$scope.linkingPredicate = selectedTarget.predicate;				
			}
			$scope.choosers[updatedChooserValues.id].value = updatedChooserValues.value;
			if($scope.source && $scope.source.project.uri==EXTERNAL_PROJECT_IDENTIFIER){
				if(!($.isEmptyObject(selectedTarget))){
					$scope.validSelection = selectedTarget.valid && $scope.externalSourceForm.$valid ;
				}
			}else{
				if(!($.isEmptyObject(selectedSource)) && !($.isEmptyObject(selectedTarget))){
					$scope.validSelection = selectedSource.valid && selectedTarget.valid;
				}
			}
			$scope.canMoveForward = $scope.validSelection && isURL($scope.linkingPredicate);
		});

		$scope.$watch("source.project", function(newProj){
			if(newProj){
				if(newProj.uri==EXTERNAL_PROJECT_IDENTIFIER){
					$scope.validSelection = $scope.externalSourceForm.$valid && $scope.choosers.target.value.valid;
				}else{
					if(!($.isEmptyObject($scope.choosers.source.value)) && !($.isEmptyObject($scope.choosers.target.value))){
						$scope.validSelection = $scope.choosers.source.value.valid && $scope.choosers.target.value.valid;
					}
				}
				$scope.canMoveForward = $scope.validSelection && isURL($scope.linkingPredicate);
			}
		});

		$scope.$watch("newSourceForm.$valid", function(newVal){
			$scope.canMoveForward = newVal;	
		});

		$scope.$watch("externalSourceForm.$valid", function(newVal){
			$scope.validSelection = newVal && $scope.choosers.target.value.valid;
			$scope.canMoveForward = $scope.validSelection && isURL($scope.linkingPredicate);
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

		function getLinkingParams(){
			var selectedSource;
			if($scope.source.project.uri==EXTERNAL_PROJECT_IDENTIFIER){
				selectedSource = $scope.externalSource; 
			}else{
				selectedSource = $scope.choosers.source.value;
			}
			 
	       	var selectedTarget = $scope.choosers.target.value;
	       	return {
				"datasetSource": selectedSource.dataset,
			    "classSource": selectedSource.class,
			    "predicateSource": selectedSource.predicate,
			    "datasetTarget": selectedTarget.dataset,
			    "classTarget": selectedTarget.class,
			    "predicateTarget": selectedTarget.predicate,
			    "predicateLinking": $scope.linkingPredicate,
			    "project": $scope.projectUri

	       	} 
		}

		$scope.initProjectsLists = function(){
			$http.get($scope.moduleUrl + "/projects").success(function(data){
				$scope.projectTargetList = data;
				$scope.target = { project: getCurrentProject($scope.projectTargetList)};
				$scope.projectSourceList =  data.clone();
				$scope.projectSourceList.push({title: "External", uri:EXTERNAL_PROJECT_IDENTIFIER});
				$scope.source = { project: getCurrentProject($scope.projectSourceList)};
			});
		}

		getCurrentProject = function(projectsList){
			for(var i=0;i < projectsList.length;i++){
				if(projectsList[i].uri == $scope.projectUri){
					return projectsList[i];
				}
			}
			return {};
		}

		$scope.handleStepResult = function(){
			var currentStep = $("#moduleWizard").wizard("selectedItem").step; 
			var currentUrl = $location.absUrl();
			switch(currentStep){
				case 1:
	       			//send ajax request
	       			var previewUrl = $scope.moduleUrl + "/preview"; 
	       			var linkingParams = getLinkingParams();
	       			linkingParams.max = MAX_PREVIEW_ROWS_TO_DISPLAY;
	       			$http({
	    				url: previewUrl, 
	    				method: "GET",
	    				params: linkingParams
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
					$scope.sourceName = $scope.choosers.source.value.title + "-s2u";
					$scope.targetURLNew = $scope.choosers.source.value.dataset + "-s2u";
				break;

				case 3:
					var saveUrl = $scope.moduleUrl + "/save"; 
					var saveParams = getLinkingParams();
					//add the other parameters for saving
					saveParams.newSourceContext = $scope.targetURLNew;
					saveParams.newSourceName = $scope.sourceName;
					saveParams.newSourceDescription=$scope.sourceDescription;
					saveParams.copyTargetTriples = $scope.keepTargetTriples;
					$http({
		    			url: saveUrl, 
		    			method: "POST",
		    			params: saveParams
		 			}).success(function(){
		 				window.location.replace($scope.projectUri+"#source");
		 			});
				break;

            	
            }           	
            
       	};

      
    })
angular.module("SilkInterlinkerApp.controllers",[])
	.controller('InterlinkerCtrl', function($scope, $http) {

		function InterlinkedDataset(){
			this.origin = 'local',

			this.isUrl = function(str){
				 var regexp = /(http|https):\/\/(\w+:{0,1}\w*@)?(\S+)(:[0-9]+)?(\/|\/([\w#!:.?+=&%@!\-\/]))?/
                return regexp.test(str);
			},

			this.comparisons = [{
				property:"",
				transformation:"none",
				stringParam:[]
			}],

			this.isValid = function(){
				if(this.source && this.isUrl(this.source.uri)){
					for(var i = 0;i<this.comparisons.length;i++){
						if(this.comparisons[i].property=="" && !this.isUrl(this.comparisons[i].property)){
							return false;
						}
					}
					return true;
				}else{
					return false;
				}
			}
		}

		function InterlinkingSetting(){
			this.settingList = [{
				metric:"",
				threshold:"",
				weight:"",
				metricParams: []
			}];

			this.getSettingsNumber = function(){
				return this.settingList.length;
			}

			this.isValid = function(){
				for(var i=0;i<this.settingList.length;i++){
					if(this.settingList[i].metric=="" || this.settingList[i].threshold===""){
						return false;
					}
				}
				for(var i=0;i< $scope.localSourceList.length;i++){
					if($scope.localSourceList[i].uri==$scope.newSource.name || $scope.localSourceList[i].uri==$scope.newSource.url){
						$("#changeNewSourceWarning").show();
						return false;
					}
				}
				$("#changeNewSourceWarning").hide();
				return $scope.newSourceForm.$valid;
			}
				
			this.existsSourceReq = function(){
				return	$.ajax({
					url: $scope.moduleUrl + "/exists", 
					data:  
						{name: $scope.newSource.name,
						uri: $scope.newSource.uri,
						project: $scope.projectUri},
					type:"GET"
				});
			}
		}
	

		$scope.creation=new InterlinkedDataset();
		
		$scope.reference= new InterlinkedDataset();

		$scope.interlinkingSetting = new InterlinkingSetting();

		$scope.creationPredicateList = [];

		$scope.referencePredicateList = [];

		$scope.upload={format: "silk"};

		$scope.setDefaultNewSourceName = function(){
			$scope.newSource={
				name: $scope.creation.source.name +"_and_" + $scope.reference.source.name,
				url: $scope.creation.source.uri+"-silk"
			}
			$scope.$apply();
		}
		
		$scope.addComparison = function(){
			$scope.creation.comparisons.push({
				property:"",
				transformation:"none"
			});	
			$scope.reference.comparisons.push({
				property:"",
				transformation:"none"
			});
			$scope.interlinkingSetting.settingList.push({
				metric:"",
				threshold:"",
				weight:""
			});
		}

		$scope.removeComparison = function(index){
			$scope.creation.comparisons.splice(index);
			$scope.reference.comparisons.splice(index);
			$scope.interlinkingSetting.settingList.splice(index);
		}

		function assignIdToRemoteEndpoint(dataset){
			if(dataset.source && dataset.source.name==""){
				$scope.creation.source.name="remote-endpoint";
			}
		}

		function getCleanComparisonParams(comparisonParam){
			var cleanComparisons = [];
			for(var i=0;i<comparisonParam.length;i++){
				cleanComparisons.push({property: comparisonParam[i].property, 
					transformation: comparisonParam[i].transformation,
					stringParam: comparisonParam[i].stringParam});
			}
			return cleanComparisons;
		}

		//assemble the object to send to the server
		function getInterlinkedSourcesInfo(){
			assignIdToRemoteEndpoint($scope.creation);
			assignIdToRemoteEndpoint($scope.reference);
			var aggregation = $scope.interlinkingSetting.aggregation;
			if(!aggregation){
				aggregation = "max";
			} 
			return {
				sourceUrl: $scope.creation.source.uri,
				sourceQuery: $scope.creation.query,
				sourceId: $scope.creation.source.name,
				sourceComparisonParameters: getCleanComparisonParams($scope.creation.comparisons),
				isSourceLocal: $scope.creation.origin=='local',
				
				targetUrl: $scope.reference.source.uri,
				targetQuery: $scope.reference.query,
				targetId: $scope.reference.source.name,
				targetComparisonParameters: getCleanComparisonParams($scope.reference.comparisons),
				isTargetLocal: $scope.reference.origin=='local',
				
				comparisonSettings: $scope.interlinkingSetting.settingList,
				aggregationSetting: aggregation,
				newSourceUrl: $scope.newSource.url
			}
		}



		$scope.postInterlinkResources = function(){
			var createScriptDef = $.ajax({
        		contentType: 'application/json',
                url: $scope.moduleUrl + "/script",
                type: "post",
                data: JSON.stringify(getInterlinkedSourcesInfo())}); 
			$.when(createScriptDef).done(function(data, textStatus, response){
				var scriptUrl = response.getResponseHeader("Location");
				$.ajax({
					type: "get",
					url: scriptUrl,
					dataType: "xml",
					success: function(scriptContent){
						var strScript = (new XMLSerializer()).serializeToString(scriptContent);
                		var prettyScript = vkbeautify.xml(strScript);
        				$("#scriptArea").val(prettyScript);
        				$scope.cmArea = CodeMirror.fromTextArea(document.getElementById("scriptArea"), {
            				mode: {name: "xml", alignCDATA: true},
            				lineNumbers: true,
            				readOnly:true
        				});
        				$("#downloadLink").attr("href", scriptUrl);
					}
				})
			});
		}
			
			

		$scope.getPredicates = function(destination){
			var sourceUrl;
			if(destination=="creation"){
				sourceUrl = $scope.creation.source.uri;
			}else{
				sourceUrl = $scope.reference.source.uri;
			}
			var predicateWebService = $scope.moduleUrl + "/predicates";
			$http.get(predicateWebService, {params: {source: sourceUrl}}).success(function(data){
				if(destination == 'creation'){
					$scope.creationPredicateList = data;
				}else{
					$scope.referencePredicateList = data;
				}
			});
		}

		$scope.getSources = function(){
			var sourcesWebService = $scope.moduleUrl + "/sources";		
			$http({
			    url: sourcesWebService, 
			    method: "GET",
			    params: {project: $scope.projectUri}
 			}).success(function(data){	
 				$scope.localSourceList = data;
 			});
		}

		$scope.runCreatedScriptFile = function(){
			var scriptRemotePath = $("#downloadLink").attr("href");
			var scriptRelPath = scriptRemotePath.substring(scriptRemotePath.indexOf("project"));
			var silkSource = {
				project: $scope.projectUri,
				newSourceName: $scope.newSource.name,
				newSourceContext: $scope.newSource.url,
				targetContext: $scope.reference.source.uri,
				scriptFilePath: scriptRelPath
			}
			$.ajax({
        		contentType: 'application/json',
                url: $scope.moduleUrl + "/run",
                type: "post",
                data: JSON.stringify(silkSource),
                success: function(data){
                	window.location.replace($scope.projectUri + "#source");
                } 
			});
		}
		

      
    })
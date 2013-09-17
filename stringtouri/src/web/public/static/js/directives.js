angular.module("predicateChooserApp.directives",[])

    .directive("predicateChooser",function($http,chooserInterface){
        return{
            restrict: "E",
            template: "<div>"+
                "<legend ng-bind='title'></legend>"+
                "<H4 ng-bind='datasetLabel' for='datasetList'></H4>"+
                "<select ui-select2 ng-model='selectedSource'"+
                        "ng-class='selectStyle'"+
                        "data-placeholder='{{datasetPlaceholder}}'>"+
                    "<option></option>"+
                    "<option ng-repeat='source in sources' value='{{source.url}}'>{{source.title}}</option>"+

                "</select>"+
                "<br>"+
                "<p class='alert alert-info' ng-show='selectedSource.description'>{{selectedSource.description}}</p>"+

                "<br>"+

                "<H4 ng-bind='classLabel' for='classList'></H4>"+
                "<select ui-select2 "+
                        "ng-model='selectedClass'"+
                        "ng-class='selectStyle'"+
                        "data-placeholder='{{classPlaceholder}}'>"+
                    "<option></option>"+
                    "<option ng-repeat='class in classes'>{{class}}</option>"+

                "</select>"+
                "<br><br>"+

                "<H4 ng-bind='predicateLabel' for='predicateList'></H4>"+
                "<select ui-select2 "+
                        "ng-model='selectedPredicate'"+
                        "ng-class='selectStyle'"+
                        "data-placeholder='{{predicatePlaceholder}}'>"+
                    "<option></option>"+
                    "<option ng-repeat='predicate in predicates'>{{predicate}}</option>"+
                "</select>"+
            "</div>",
            scope:{
                title:"@",
                project:"@",
                module:"@",
                selectStyle:"@class",
                datasetLabel:"@",
                classLabel:"@",
                predicateLabel:"@",
                datasetPlaceholder:"@",
                classPlaceholder:"@",
                predicatePlaceholder:"@",
                id:"@"

            },
            controller:function($scope){
                $scope.$watch("selectedSource",function(sourceUrl){
                    if(sourceUrl){
                        $http.get($scope.module + "/predicates?source=" + sourceUrl).success(function(data){
                            $scope.predicates = data;

                        });
                        $http.get($scope.module + "/classes?source=" + sourceUrl).success(function(data){
                            $scope.classes = data;
                        });

                    }
                    chooserInterface.setDataset($scope.id,sourceUrl);
                });
                $scope.$watch("selectedPredicate",function(predicateUrl){
                    chooserInterface.setPredicate($scope.id,predicateUrl);
                });
                $scope.$watch("selectedClass",function(classUrl){
                    chooserInterface.setClass($scope.id,classUrl);
                });

               
            },
            link:function(scope, element, attrs){
                $http.get(attrs.module + "/sources?project="+attrs.project).success(function(data){
                    scope.sources = data;
                });
            }
        }
    });
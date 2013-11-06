angular.module("predicateChooserApp.services",[])
	.factory("chooserInterface",function($rootScope){
        return{
            bindedDirectives: {},

            setDataset :function (dirId,ds,title){
                if(this.bindedDirectives.hasOwnProperty(dirId)){
                    this.bindedDirectives[dirId].dataset = ds;
                    this.bindedDirectives[dirId].title = title;
                }else{
                    this.bindedDirectives[dirId] = {valid: false , dataset: undefined, class:undefined, predicate: undefined, title: undefined};
                }
                this.checkValidity(dirId);
            },

            setPredicate :function (dirId,pred){
                this.bindedDirectives[dirId].predicate = pred;
                this.checkValidity(dirId);
            },

            setClass :function (dirId,selClass){
                this.bindedDirectives[dirId].class = selClass;
            },

            checkValidity : function(id){
                var directive = this.bindedDirectives[id];
                directive.valid = (directive.dataset!=undefined && directive.predicate!=undefined)
                    &&  (directive.dataset!="" && directive.predicate!="");
                $rootScope.$broadcast("ChooserUpdate",{"id":id, "value": this.bindedDirectives[id]});
            }
        }
    })

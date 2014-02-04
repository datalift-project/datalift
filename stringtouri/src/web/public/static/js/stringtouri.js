/*
 * 
 * This script contains the definition of the object that will be used by the datagrid to show a preview of the linking. 
 * @author csuglia
 */

 var PreviewDataSource = function (columns) {
            this._columns = columns;
        };

        PreviewDataSource.prototype = {

            /**
             * Returns stored column metadata
             */
            columns: function () {
                return this._columns;
            },

            setData: function(data){
            	this._data = data;
            },

            isEmpty: function(){
                return $.isEmptyObject(this._data);
            },

            data: function (options, callback) {
                var startIndex = (options.pageIndex)*options.pageSize;
                var endIndex =  (options.pageIndex+1)*options.pageSize;
                if(endIndex>this._data.length) endIndex = this._data.length;
                triplesToDisplay = this._data.slice(startIndex, endIndex);
                var totalPages = Math.round(this._data.length / options.pageSize);
                if(options.sortDirection && options.sortProperty){
                    triplesToDisplay.sort(function(valA, valB){
                        if(valA[options.sortProperty]>valB[options.sortProperty] && options.sortDirection=="asc"){
                            return 1;
                        }else{
                            return -1;
                        }
                    });
                }
                callback({ data: triplesToDisplay, start: startIndex+1, end: endIndex, count: this._data.length, pages: totalPages, page: options.pageIndex+1 });
            }
        }






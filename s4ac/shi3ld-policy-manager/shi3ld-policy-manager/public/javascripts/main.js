/**
 * New node file
 */
require.config({
  paths: {
    jquery: 'lib/jquery-1.9.1-dev',
    underscore: 'lib/underscore-master/underscore',
    backbone: 'lib/backbone',
    text: 'lib/require/text',
    async: 'lib/async',
    codemirror: '../sparql/lib/codemirror',
    sparql10: '../sparql/sparql10querymode_ll1',
    sparql11: '../sparql/sparql11querymode_ll1',
    sparql11update: '../sparql/sparql11updatemode_ll1',
    flinteditor: '../sparql/flint-editor',
    bootstrap: '../bootstrap/js/bootstrap.min',
    datepicker: '../datepicker/js/bootstrap-datepicker',
    timepicker: '../bootstrap-timepicker/js/bootstrap-timepicker',
    jqueryui: 'lib/jquery-ui-1.10.3.custom/js/jquery-ui-1.10.3.custom',
    addresspicker: 'lib/jquery.ui.addresspicker',
    jqcookies: 'lib/jquery.cookie',
    paginator: 'lib/backbone.paginator',
    dateformat: 'lib/date.format',
    suggest: 'lib/magicsuggest/magicsuggest-1.3.0'
  },
  shim: {
        'underscore': {
          exports: '_'
        },
        'backbone': {
            deps: ['underscore', 'jquery'],
            exports: 'Backbone'
        },
        'sparql10': {
            deps: ['codemirror', 'jquery'],
        },
        'sparql11': {
            deps: ['codemirror', 'jquery'],
        },
        'sparql11update': {
            deps: ['codemirror', 'jquery'],
        },
        'flinteditor': {
            deps: ['codemirror', 
            	'jquery',
            	'sparql10',
            	'sparql11',
            	'sparql11update'
            ],
            exports: 'FlintEditor'
        },
        'bootstrap': {
          deps: ['jquery']
        },
        'datepicker': {
          deps: ['bootstrap']
        },
        'timepicker': {
          deps: ['bootstrap']
        },
        'jqueryui': {
          deps: ['jquery']
        },
        'addresspicker': {
          deps: ['jqueryui', 'async!http://maps.google.com/maps/api/js?sensor=false']
        },
        'jqcookies': {
          deps: ['jquery']
        },
      	'paginator': {
      	  deps: ['backbone']
      	},
    }
});
//to deal with bootstrapped data
//http://stackoverflow.com/questions/9916073/how-to-load-bootstrapped-models-in-backbone-js-while-using-amd-require-js
require(['jquery','bootstrap'], function(){
	$('#spinner').modal('show');
	//all operation in case of error before loading modules managed explicitily since
	//app view still not created
	setTimeout(function () {
		if ($('#spinner').is(':visible') == false) {
			//if modal hidden than loading has already completed (ie success or error already raised)
			return;
		}
		$('#spinner').modal('hide');
		$('i.gray-out').attr('style', 'opacity: 0.4; filter: alpha(opacity=40);background-color: #000;');
		$('#page').append(
			"<div class=\"alert alert-error alert-text-center\">\n" +
				"<h3>A network connection problem occurred<h3>\n" +
				"<a class=\"btn btn-danger\" onclick=\"window.location.reload()\">Retry</a>" +
		  	"</div>\n"
		)
	}, 10000);
});
require(['views/app'], function(AppView){
  var app_view = new AppView;
});
/**
 * view to define a new graph
 */
define([
  'underscore',
  'backbone',
  'text!templates/newGraph.html',
  'models/target',
 ], function(_, Backbone, newGraphTemplate, TargetModel){
  
	 var NewGraphView = Backbone.View.extend({
	 
	 	  template: _.template(newGraphTemplate),
	 	  
	 	  events: {
	 	  	'click #preview': 'graphPreview',
	 	  	'click #newGraphCancel': 'close',
	 	  	'click #newGraphFinish': 'addGraph',
	 	  	'keydown': 'closeOnEsc',
	 	  },
	 	  
	 	  MAX_URI_LEN: 30,
	 	  
	 	  initialize: function(options) {
		    //this.backend = options.backend;
		    //this.model = options.model;
		    this.$modal = $('#newGraph');
		    this.defaultPrefix  = options.defaultPrefix;
		    this.targets = options.targets;
		    //to distinguish the flow: ie to know if the view was called from wizard or from policyGui
		    this.targetsViewModel = options.targetsViewModel;
		    
		     _.bindAll(this, 'printModel', 'notifyError');
		    
		    this.listenTo(this.targets, 'overWriteTarget', this.addGraph);
		  },
	 	  
	      render: function () {
	      	this.$el.html(this.template({
	            backend: this.backend
	        }));
			this.$errorAlert = $('#errorAlert');
	        return this;
	      },
	      
	      init: function () {
	      	this.initialQuery = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n" +
	      						"PREFIX : <"+ this.defaultPrefix +">\n" +
	      						"SELECT * \nWHERE{\n\n}";
	
			var flintConfig = {
				"interface" : {
					"toolbar" : true,
					"menu" : true
				},
				"namespaces" : [
				]
			
			}
			var that = this;	
			this.$modal.one('shown', function () {
				$(window).resize();
				that.flintEd = new FlintEditor("queryDiv", "sparql/images", flintConfig);
				that.flintEd.getEditor().getCodeEditor().setValue(that.initialQuery);
			});
			
		    
		    $("[data-toggle-newgraph='tooltip']").tooltip();
		    this.$name = $("#inputGName");
		    this.$table = $('#previewTab');
		    this.$triplesCnt = $('#triplesCnt');
		    this.$preview = $('#preview');
		    this.$newGraphFinish = $('#newGraphFinish');
	      },
	      	  
		  notifyError: function (model, res) {
		  	this.$newGraphFinish.button('reset');
		  	this.$preview.button('reset');
		  	if (res.status == 409){
		  		this.targets.trigger('saveAlert', {
					type: "nameConflictTarget",
					targetsViewModel: this.targetsViewModel,
    			});
    			return;
		  	}
		  	this.$preview.before("<br><div id=\"errorAlert\" class=\"alert alert-error\">An error occurred. Retry later</div>");
		  },
		  
		  syntaxError: function () {
		  	this.$preview.before("<br><div id=\"errorAlert\" class=\"alert alert-error\">Syntax error in the query</div>");
		  },
	      
	      close: function () {
	      	$(window).trigger('resize', 'flint-test');
	      	this.$modal.modal('hide');
	        this.remove();
	      },
	      
	      graphPreview: function () {
	      	var that  = this; 
	      	var trimedName = this.$name.val().replace(/\s/g,"-");
	      	
	      	if (!this.nameAndSyntaxOk()){
	      		return;
	      	}
	      	var Model = Backbone.Model.extend({
	      		url:'/targets?target=' 
	      			+ encodeURIComponent(that.defaultPrefix + trimedName)
	      		    + '&triples='
	      		    + encodeURIComponent(that.flintEd.getEditor().getCodeEditor().getValue())
	      	});
	      	
	      	this.model = new Model();
	      	this.$preview.button('loading');
	      	this.model.fetch({
	      		success: this.printModel,
	      		error: this.notifyError
	      	})
	      },
	      
	      addGraph: function (options) {
	      	var query = this.flintEd.getEditor().getCodeEditor().getValue();
	      	if (!this.nameAndSyntaxOk()){
	      		return;
	      	}
	      	
	      	var uri = this.defaultPrefix + this.$name.val().replace(/\s/g,"-");
	      	
	      	var triples = query.substr(query.toUpperCase().search("WHERE"));
	      	//extract only the graph pattern without where {...} that envelops it
	      	start = triples.indexOf("{");
	      	end = triples.lastIndexOf("}");
	      	triples = triples.substring(start + 1, end);
	      	
	      	var prefixes = query.substring(0, query.toUpperCase().search("SELECT"));
	      	
	      	var target = new TargetModel({
	      		triples: triples,
	      		prefixes: prefixes,
	      		label: this.$name.val(),
	      		uri:  uri
	      	});
	      	
	      	if ((options) && (options.overwrite == true)) {
	      		target.set({newModel: false});
	      	}
	      	
	      	var that = this;
	      	//with ths.$newGraphFinish not works properly
	      	this.$newGraphFinish.button('loading');
	      	target.save(null, {
	      		success: function (model,res) {
	      			Backbone.trigger('loadingEnd');
	      			model.set({tripleCnt: res.tripleCnt})
	      			that.targets.trigger('newTarget', model);
	      			that.close();
	      		},
	      		error: this.notifyError
	      	});
	      },
	      
	      printModel: function (model, res) {
  			var html = "";
  			//Backbone.trigger('loadingEnd');
  			this.$preview.button('reset');
  			if (res.data.length > 0) {
  				html += "<tr>";
  				for(var prop in res.data[0]){
  					html += "<th>" + prop + "</th>";
  				}
  				html += "</tr>";
  				var that = this;
  				_.each(res.data, function (v) {
  					html += "<tr>";
  					for(var prop in v){
  						v[prop].length > that.MAX_URI_LEN ? (txt = '...' + v[prop].substring(v[prop].length - that.MAX_URI_LEN) ) : (txt = v[prop]);
  						html += "<td><a href=\"#\" title=\"" + v[prop] + "\"data-toggle-newgraph-tab=tooltip>" + txt + "</a></td>";
  					}
  					html += "</tr>";
  				});
  			}
  			this.$table.html(html);
  			var otherRowsCnt = (res.triplesNum - 3) > 0 ? (res.triplesNum - 3) : 0
  			if (otherRowsCnt > 1000000) {
				otherRowsCnt = "roughly " + String(Math.floor(otherRowsCnt / 1000000)) + "M";
			} else if (otherRowsCnt > 10000) {
				otherRowsCnt = "roughly " + String(Math.floor(otherRowsCnt / 1000)) + "K";
			}
  			html = res.triplesNum > 0 ? "And other " + otherRowsCnt + " rows selected" : "No rows selected"
  			this.$triplesCnt.html(html);
  			$("[data-toggle-newgraph-tab='tooltip']").tooltip();
  		 },
  		 
  		 nameAndSyntaxOk: function () {
  		 	$('.alert-error').remove();
	      	$('#errorTextNG').text("");
		  	if ((! $('#inputGName').val()) || ($('#inputGName').val().replace(/\s+/g, '') == "")) {
		  		console.log("name missing");
		  		$('#errorTextNG').text("Name is required");
		  		$('#nameCtrlGrpNG').addClass("error");
		  		$('#inputGName').on('blur', function (){
			  			if(($('#inputGName').val()) && ($('#inputGName').val().replace(/\s+/g, '') != "")) {
			  				$('#errorTextNG').text("");
			  				$('#nameCtrlGrpNG').removeClass("error");
			  			}
			  	});
		  		return false;
		  	}
		  	var tokens = $('#flint-status').text().split(" ");
		  	if (tokens[tokens.length - 1] == "invalid") {
		  		this.syntaxError();
		  		return false;
		  	}
		  	return true;
  		 },
  		 
  		 closeOnEsc: function (e) {
			if (e.keyCode == 27) {
				this.close();
			}
		}	      
	  });
	  
	  return NewGraphView;
 });
 
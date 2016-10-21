/*
 * Copyright / Copr. 2010-2013  - EURECOM -  for the DataLift project
 * Contributor(s) : Ghislain Atemezing, Raphaël Troncy
 *
 * Contact: atemezin@eurecom.fr
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.visualizer;


import java.util.LinkedList;

import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.Source.SourceType;
import javax.script.*;
import org.datalift.fwk.project.Source;
import org.datalift.fwk.project.TransformedRdfSource;
import org.openrdf.query.BindingSet;
import org.openrdf.query.MalformedQueryException;
import org.openrdf.query.QueryEvaluationException;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.TupleQueryResult;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;


/**
 * Visu module backend main class.
 * To be continued ...
 * Handles DataSets' retrieval and project compatibility check.
 *
 * author gatemezing
 */
public class VisuModel extends ModuleModel
{
    //-------------------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------------------
    
    /** Binding for the default subject var in SPARQL. */
    protected static final String SB = "s";
    
    /** Binding for the default predicate var in SPARQL. */
    protected static final String PB = "p";
    /** Binding for the default object var in SPARQL. */
    protected static final String OB = "o";
    
    /** Default WHERE SPARQL clause to retrieve all classes. */
    private static final String CLASS_WHERE = "{?" + SB + " a ?" + OB + "}";
    /** Default WHERE SPARQL clause to retrieve all predicates. */
    private static final String PREDICATE_WHERE = "{?" + SB + " ?" + PB + " ?" + OB + "}";
	
	
    //-------------------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------------------

    /**
     * Creates a new VisuModel instance.
     * @param name Name of the module.
     */
    public VisuModel(String name) {
        super(name);
    }

    //-------------------------------------------------------------------------
    // Sources management.
    //-------------------------------------------------------------------------

    /**
     * Checks if a given {@link Source} contains valid RDF-structured data
     * AND DataCube data.
     * TODO: Extend to many different type of objects
     * @param src The source to check.
     * @return True if src is {@link TransformedRdfSource}.
     */
    protected boolean isValidSource(Source src) {
        return src.getType() == SourceType.TransformedRdfSource;
    }

    /**
     * Retrieves all of the RDF DataSets from a given project's sources.
     */
    /**
     * Returns all of the URIs (as strings) from the {@link Project project}.
     * @param proj The project to use.
     * @return A LinkedList containing source file's URIs as strings.
     */
    protected final LinkedList<String> getSourcesURIs(Project proj) {
    	LinkedList<String> ret = new LinkedList<String>();
    	
    	for (Source src : proj.getSources()) {
    		if (isValidSource(src)) {
    			ret.add(src.getUri());
    		}
    	}
    	return ret;
    }
	
	 //-------------------------------------------------------------------------
    // Queries management.
    //-------------------------------------------------------------------------
    
    /**
	 * Tels if the bindings of the results are well-formed.
	 * @param tqr The result of a SPARQL query.
	 * @param bind The result one and only binding.
	 * @return True if the results contains only bind.
	 * @throws QueryEvaluationException Error while closing the result.
	 */
	protected boolean hasCorrectBindingNames(TupleQueryResult tqr, String bind) throws QueryEvaluationException {
		return tqr.getBindingNames().size() == 1 && tqr.getBindingNames().contains(bind);
	}
    
	/**
	 * Sends and evaluates a SPARQL select query on the data set, then returns
	 * the results (which must be one-column only) as a list of Strings.
	 * @param query The SPARQL query without its prefixes.
	 * @param bind The result one and only binding.
	 * @return The query's result as a list of Strings.
	 */
    protected LinkedList<String> selectQuery(String query, String bind) {
		TupleQuery tq;
		TupleQueryResult tqr;
		LinkedList<String> ret = new LinkedList<String>();
		
		LOG.debug("Processing query: \"{}\"", query);
		RepositoryConnection cnx = INTERNAL_REPO.newConnection();
		try {
			tq = cnx.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			
			if (!hasCorrectBindingNames(tqr, bind)) {
				throw new MalformedQueryException("Wrong query result bindings - " + query);
			}
			
			while (tqr.hasNext()) {
				ret.add(tqr.next().getValue(bind).stringValue());
			}
		}
		catch (MalformedQueryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (QueryEvaluationException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (RepositoryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		}
		finally {
		    try { cnx.close(); } catch (Exception e) { /* Ignore... */ }
		}
	    return ret;
	}
    
    /**
	 * Sends and evaluates a SPARQL select query on the data set, then returns
	 * the results (which must be two-column only) as a list of lists of Strings.
	 * @param query The SPARQL query without its prefixes.
	 * @param stub The predicate used in the query.
	 * @return The query's result as a list of Strings.
	 */
    protected LinkedList<LinkedList<String>> pairSelectQuery(String query, String stub) {
		TupleQuery tq;
		TupleQueryResult tqr;
		BindingSet bs;
		LinkedList<LinkedList<String>> ret = new LinkedList<LinkedList<String>>();
		
		LOG.debug("Processing query: \"{}\"", query);
		RepositoryConnection cnx = INTERNAL_REPO.newConnection();
		try {
			tq = cnx.prepareTupleQuery(QueryLanguage.SPARQL, query);
			tqr = tq.evaluate();
			LinkedList<String> tmp;
			while (tqr.hasNext()) {
				bs = tqr.next();
				tmp = new LinkedList<String>();
				tmp.add(bs.getValue(SB).stringValue());
				tmp.add(stub);
				tmp.add(bs.getValue(OB).stringValue());
				ret.add(tmp);
			}
		}
		catch (MalformedQueryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (QueryEvaluationException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		} catch (RepositoryException e) {
			LOG.fatal("Failed to process query \"{}\":", e, query);
		}
	    return ret;
	}
    
    /**
     * Writes a query given a bind to retrieve, a context and a WHERE clause.
     * @param context Context on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return A full query.
     */
    protected String writeQuery(String context, String where, String bind) {
    	String ret = "SELECT DISTINCT ?" + bind
    			+ (context.isEmpty() ? "" : " FROM <" + context + ">")
    			+ " WHERE " + where;
    	return ret;
    }
    
    /**
     * Retrieves multiple queries results based on a query pattern executed on
     * multiple contexts.
     * @param contexts Contexts on which the query will be executed.
     * @param where Constraints given by the query.
     * @param bind Binding to use to retrieve data.
     * @return Results as a LinkedList of Strings.
     */
    protected final LinkedList<String> getMultipleResults(LinkedList<String> contexts, String where, String bind) {
    	LinkedList<String> ret = new LinkedList<String>();
    	
    	for (String context : contexts) {
    		ret.addAll(selectQuery(writeQuery(context, where, bind), bind));
    	}
    	return ret;
    }

    /**
     * Retrieves all of the classes used inside given contexts.
     * @param contexts The contexts to use.
     * @return A LinkedList of all of the classes used inside the contexts.
     */
    protected final LinkedList<String> getAllClasses(LinkedList<String> contexts) {
		return getMultipleResults(contexts, CLASS_WHERE, OB);
	}
	
	/**
     * Retrieves all of the predicates used inside given contexts.
     * @param contexts The contexts to use.
     * @return A LinkedList of all of the predicates used inside the contexts.
     */
	protected final LinkedList<String> getAllPredicates(LinkedList<String> contexts) {
		return getMultipleResults(contexts, PREDICATE_WHERE, PB);
	}
    
    public void lauchSgvizler(String query, String endpoint, String charType,
                              Integer ChartHeight, Integer ChartWdith)
                                                      throws ScriptException {
        //Try to see how this work in an example 
        //String newScript ="(function(a){'"'use strict'"';var b={go:function(){this.loadLibs(),google.load('"'visualization'"','"'1.0'"',{packages:['"'annotatedtimeline","corechart","gauge","geomap","geochart","imagesparkline","map","orgchart","table","motionchart","treemap"]}),google.setOnLoadCallback(function(){b.charts.loadCharts(),b.drawFormQuery(),b.drawContainerQueries()})},loadLibs:function(){var a,c=["d3.v2.min.js","raphael-dracula.pack.min.js"];b.ui.isElement(b.ui.id.script)&&(this.option.homefolder=$("#"+b.ui.id.script).attr("src").replace(/sgvizler\.js$/,""),this.option.libfolder=this.option.homefolder+"/lib");for(a=0;a<c.length;a+=1)$.ajax(this.option.libfolder+c[a],{dataType:"script",async:!1});$("head").append('<link rel="stylesheet" href="'+this.option.homefolder+'sgvizler.chart.css" type="text/css" />')},drawFormQuery:function(){var a=new b.query(b.ui.id.chartCon),c=b.ui.getUrlParams();$.extend(a,b.option.query,{query:c.query,chart:c.chart}),b.ui.isElement(a.container)&&a.query&&($.extend(a.chartOptions,{width:c.width,height:c.height}),a.draw()),b.ui.displayUI(a)},drawContainerQueries:function(){$("["+this.ui.attr.prefix+"query]").each(function(){var a=new b.query;$.extend(a,b.option.query,b.ui.getQueryOptionAttr(this)),$.extend(a.chartOptions,b.ui.getChartOptionAttr(this)),a.draw()})},option:{},chart:{},charts:{},parser:{},ui:{}};jQuery.ajaxSetup({accepts:{xml:"application/sparql-results+xml",json:"application/sparql-results+json"}}),b.option={home:window.location.href.replace(window.location.search,""),homefolder:"",libfolder:"/lib",namespace:{rdf:"http://www.w3.org/1999/02/22-rdf-syntax-ns#",rdfs:"http://www.w3.org/2000/01/rdf-schema#",owl:"http://www.w3.org/2002/07/owl#",xsd:"http://www.w3.org/2001/XMLSchema#"},query:{},chart:{}},b.ui={id:{script:"sgvzlr_script",chartCon:"sgvzlr_gchart",queryForm:"sgvzlr_formQuery",queryTxt:"sgvzlr_cQuery",formQuery:"sgvzlr_strQuery",formWidth:"sgvzlr_strWidth",formHeight:"sgvzlr_strHeight",formChart:"sgvzlr_optChart",prefixCon:"sgvzlr_cPrefix",messageCon:"sgvzlr_cMessage"},attr:{prefix:"data-sgvizler-",prefixChart:"data-sgvizler-chart-options",valueAssign:"=",valueSplit:"|"},params:["query","chart","width","height"],displayUI:function(a){this.displayPrefixes(),this.displayChartTypesMenu(),this.displayUserInput(a)},displayPrefixes:function(){this.setElementText(this.id.prefixCon,b.query.prototype.getPrefixes())},displayUserInput:function(a){this.setElementValue(this.id.queryTxt,a.query),this.setElementValue(this.id.formChart,a.chart),this.setElementValue(this.id.formWidth,a.chartOptions.width),this.setElementValue(this.id.formHeight,a.chartOptions.height)},displayChartTypesMenu:function(){var a,c;if(this.isElement(this.id.formChart)){a=b.charts.all;for(c=0;c<a.length;c+=1)$("#"+this.id.formChart).append($("<option/>").val(a[c].id).html(a[c].id))}},displayFeedback:function(a,b){var c,d=a.container;a.container===this.id.chartCon&&this.isElement(this.id.messageCon)&&(d=this.id.messageCon);if(a.loglevel===0)c="";else if(a.loglevel===1){if(b==="LOADING")c="Loading...";else if(b==="ERROR_ENDPOINT"||b==="ERROR_UNKNOWN")c="Error."}else b==="LOADING"?c="Sending query...":b==="ERROR_ENDPOINT"?c="Error querying endpoint. Possible errors:"+this.html.ul(this.html.a(a.endpoint,"SPARQL endpoint")+" down? "+this.html.a(a.endpoint+a.endpoint_query_url+a.encodedQuery,"Check if query runs at the endpoint")+".","Malformed SPARQL query? "+this.html.a(a.validator_query_url+a.encodedQuery,"Check if it validates")+".","CORS supported and enabled? Read more about "+this.html.a("http://code.google.com/p/sgvizler/wiki/Compatibility","CORS and compatibility")+".","Is your "+this.html.a("http://code.google.com/p/sgvizler/wiki/Compatibility","browser support")+"ed?","Hmm.. it might be a bug! Please file a report to "+this.html.a("http://code.google.com/p/sgvizler/issues/","the issues")+"."):b==="ERROR_UNKNOWN"?c="Unknown error.":b==="NO_RESULTS"?c="Query returned no results.":b==="DRAWING"&&(c="Received "+a.noRows+" rows. Drawing chart...<br/>"+this.html.a(a.endpoint+a.endpoint_query_url+a.encodedQuery,"View query results","target='_blank'")+" (in new window).");this.setElementHTML(d,this.html.tag("p",c))},setElementValue:function(a,b){this.isElement(a)&&$("#"+a).val(b)},setElementText:function(a,b){this.isElement(a)&&$("#"+a).text(b)},setElementHTML:function(a,b){this.isElement(a)&&$("#"+a).html(b)},isElement:function(a){return $("#"+a).length>0},getQueryOptionAttr:function(a){var b,c={container:$(a).attr("id")},d=a.attributes;for(b=0;b<d.length;b+=1)d[b].name.lastIndexOf(this.attr.prefix,0)===0&&(c[d[b].name.substring(this.attr.prefix.length)]=d[b].value);return c},getChartOptionAttr:function(a){var c,d,e,f,g,h,i={},j=$(a).attr(b.ui.attr.prefixChart);if(typeof j!="undefined"){d=j.split(this.attr.valueSplit);for(c=0;c<d.length;c+=1){e=d[c].split(this.attr.valueAssign),f=e[0].split("."),g=i;for(h=0;h<f.length-1;h+=1)typeof g[f[h]]=="undefined"&&(g[f[h]]={}),g=g[f[h]];g[f[h]]=e[1]}}return i.width=/(\d+)/.exec($(a).css("width"))[1],i.height=/(\d+)/.exec($(a).css("height"))[1],i},getUrlParams:function(){var a={},b,c=/([^&=]+)=?([^&]*)/g,d=function(a){return decodeURIComponent(a.replace(/\+/g," "))},e=window.location.search.substring(1);while(b=c.exec(e))b[2].length>0&&this.params.indexOf(b[1])!==-1&&(a[d(b[1])]=d(b[2]));return a},resetPage:function(){document.location=b.home},submitQuery:function(){$("#"+this.id.formQuery).val($("#"+this.id.queryTxt).val()),$("#"+this.id.queryForm).submit()},html:{a:function(a,b,c){typeof c=="undefined"&&(c="");if(typeof a!="undefined"&&typeof b!="undefined")return"<a "+c+" href='"+a+"'>"+b+"</a>"},ul:function(){var a,b;if(arguments.length){b="<ul>";for(a=0;a<arguments.length;a+=1)b+="<li>"+arguments[a]+"</li>";return b+"</ul>"}},tag:function(a,b){return"<"+a+">"+b+"</"+a+">"}}},b.parser={defaultGDatatype:"string",countRowsSparqlXML:function(a){return $(a).find("sparql").find("results").find("result").length},countRowsSparqlJSON:function(a){if(typeof a.results.bindings!="undefined")return a.results.bindings.length},SparqlXML2GoogleJSON:function(a){var c,d,e=[],f=[],g=[],h=$(a).find("sparql").find("results").find("result");return c=0,$(a).find("sparql").find("head").find("variable").each(function(){var a=null,d=null,f=$(this).attr("name"),i=null,j=$(h).find('binding[name="'+f+'"]');j.length&&(i=$(j).first().children().first()[0],a=i.nodeName,d=$(i).attr("datatype")),g[c]=b.parser.getGoogleJsonDatatype(a,d),e[c]={id:f,label:f,type:g[c]},c+=1}),d=0,$(h).each(function(){var a,h,i,j,k,l=[];for(c=0;c<e.length;c+=1)a=null,h=$(this).find('binding[name="'+e[c].id+'"]'),h.length&&typeof $(h).first().children().first()!="undefined"&&$(h).first().children().first().firstChild!==null&&(i=$(h).first().children().first()[0],j=i.nodeName,k=$(i).first().text(),a=b.parser.getGoogleJsonValue(k,g[c],j)),l[c]={v:a};f[d]={c:l},d+=1}),{cols:e,rows:f}},SparqlJSON2GoogleJSON:function(a){var b,c,d,e,f,g,h,i=[],j=[],k=[],l=a.head.vars,m=a.results.bindings;for(b=0;b<l.length;b+=1){c=0,g=null,h=null;while(typeof m[c][l[b]]=="undefined"&&c+1<m.length)c+=1;typeof m[c][l[b]]!="undefined"&&(g=m[c][l[b]].type,h=m[c][l[b]].datatype),k[b]=this.getGoogleJsonDatatype(g,h),i[b]={id:l[b],label:l[b],type:k[b]}}for(c=0;c<m.length;c+=1){d=m[c],e=[];for(b=0;b<l.length;b+=1)f=null,typeof d[l[b]]!="undefined"&&typeof d[l[b]].value!="undefined"&&(f=this.getGoogleJsonValue(d[l[b]].value,k[b],d[l[b]].type)),e[b]={v:f};j[c]={c:e}}return{cols:i,rows:j}},getGoogleJsonValue:function(a,b,c){var d;return b==="number"?d=Number(a):b==="date"?d=new Date(a.substr(0,4),a.substr(5,2),a.substr(8,2)):b==="datetime"?d=new Date(a.substr(0,4),a.substr(5,2),a.substr(8,2),a.substr(11,2),a.substr(14,2),a.substr(17,2)):b==="timeofday"?d=[a.substr(0,2),a.substr(3,2),a.substr(6,2)]:(c==="uri"&&(d=this.prefixify(a)),d=a),d},getGoogleJsonDatatype:function(a,c){var d=this.defaultGDatatype,e=b.option.namespace.xsd;return typeof a!="undefined"&&(a==="typed-literal"||a==="literal")&&(c===e+"float"||c===e+"double"||c===e+"decimal"||c===e+"int"||c===e+"long"||c===e+"integer"?d="number":c===e+"boolean"?d="boolean":c===e+"date"?d="date":c===e+"dateTime"?d="datetime":c===e+"time"&&(d="timeofday")),d},prefixify:function(a){var c;for(c in b.option.namespace)if(b.option.namespace.hasOwnProperty(c)&&a.lastIndexOf(b.option.namespace[c],0)===0)return a.replace(b.option.namespace[c],c+":");return a},unprefixify:function(a){var c;for(c in b.option.namespace)if(b.option.namespace.hasOwnProperty(c)&&a.lastIndexOf(c+":",0)===0)return a.replace(c+":",b.option.namespace[c]);return a}},b.query=function(a){this.container=a,this.query="SELECT ?class (count(?instance) AS ?noOfInstances)\nWHERE{ ?instance a ?class }\nGROUP BY ?class\nORDER BY ?class",this.endpoint="http://rdf.insee.fr/sparql",this.endpoint_output="json",this.endpoint_query_url="?output=text&amp;query=",this.validator_query_url="http://www.sparql.org/query-validator?languageSyntax=SPARQL&amp;outputFormat=sparql&amp;linenumbers=true&amp;query=",this.chart="gLineChart",this.loglevel=2,this.chartOptions={width:"800",height:"400",chartArea:{left:"5%",top:"5%",width:"75%",height:"80%"},gGeoMap:{dataMode:"markers"},gMap:{dataMode:"markers"},sMap:{dataMode:"markers",showTip:!0,useMapTypeControl:!0},gSparkline:{showAxisLines:!1}}},b.query.prototype.draw=function(){var a=this,c=b.charts.getChart(this.container,this.chart);this.setChartSpecificOptions(),this.insertFrom(),this.runQuery(function(b){c.draw(new google.visualization.DataTable(a.processQueryResults(b)),a.chartOptions)})},b.query.prototype.runQuery=function(a){var c,d,e=this.endpoint_output;b.ui.displayFeedback(this,"LOADING"),this.encodedQuery=encodeURIComponent(this.getPrefixes()+this.query),this.endpoint_output!=="jsonp"&&$.browser.msie&&window.XDomainRequest?(c=new XDomainRequest,d=this.endpoint+"?query="+this.encodedQuery+"&output="+this.endpoint_output,c.open("GET",d),c.onload=function(){var b;e==="xml"?b=$.parseXML(c.responseText):b=$.parseJSON(c.responseText),a(b)},c.send()):$.get(this.endpoint,{query:this.getPrefixes()+this.query,output:this.endpoint_output==="jsonp"?"json":this.endpoint_output},function(b){a(b)},this.endpoint_output).error(function(){b.ui.displayFeedback(this,"ERROR_ENDPOINT")})},b.query.prototype.processQueryResults=function(a){this.setResultRowCount(a);if(this.noRows===null)b.ui.displayFeedback(this,"ERROR_UNKNOWN");else if(this.noRows===0)b.ui.displayFeedback(this,"NO_RESULTS");else return b.ui.displayFeedback(this,"DRAWING"),this.getGoogleJSON(a)},b.query.prototype.setResultRowCount=function(a){this.endpoint_output==="xml"?this.noRows=b.parser.countRowsSparqlXML(a):this.noRows=b.parser.countRowsSparqlJSON(a)},b.query.prototype.getGoogleJSON=function(a){return this.endpoint_output==="xml"?a=b.parser.SparqlXML2GoogleJSON(a):a=b.parser.SparqlJSON2GoogleJSON(a),a},b.query.prototype.insertFrom=function(){if(typeof this.rdf!="undefined"){var a,c=this.rdf.split(b.ui.attr.valueSplit),d="";for(a=0;a<c.length;a+=1)d+="FROM <"+c[a]+">\n";this.query=this.query.replace(/(WHERE)?(\s)*\{/,"\n"+d+"WHERE {")}},b.query.prototype.getPrefixes=function(){var a,c="";for(a in b.option.namespace)b.option.namespace.hasOwnProperty(a)&&(c+="PREFIX "+a+": <"+b.option.namespace[a]+">\n");return c},b.query.prototype.setChartSpecificOptions=function(){var a,b;for(a in this.chartOptions)if(this.chartOptions.hasOwnProperty(a)&&a===this.chart)for(b in this.chartOptions[a])this.chartOptions[a].hasOwnProperty(b)&&(this.chartOptions[b]=this.chartOptions[a][b])},b.charts={all:[],loadCharts:function(){var a=[{id:"gLineChart",func:google.visualization.LineChart},{id:"gAreaChart",func:google.visualization.AreaChart},{id:"gSteppedAreaChart",func:google.visualization.SteppedAreaChart},{id:"gPieChart",func:google.visualization.PieChart},{id:"gBubbleChart",func:google.visualization.BubbleChart},{id:"gColumnChart",func:google.visualization.ColumnChart},{id:"gBarChart",func:google.visualization.BarChart},{id:"gSparkline",func:google.visualization.ImageSparkLine},{id:"gScatterChart",func:google.visualization.ScatterChart},{id:"gCandlestickChart",func:google.visualization.CandlestickChart},{id:"gGauge",func:google.visualization.Gauge},{id:"gOrgChart",func:google.visualization.OrgChart},{id:"gTreeMap",func:google.visualization.TreeMap},{id:"gTimeline",func:google.visualization.AnnotatedTimeLine},{id:"gMotionChart",func:google.visualization.MotionChart},{id:"gGeoChart",func:google.visualization.GeoChart},{id:"gGeoMap",func:google.visualization.GeoMap},{id:"gMap",func:google.visualization.Map},{id:"gTable",func:google.visualization.Table}],c;$.merge(this.all,a);for(c in b.chart)b.chart.hasOwnProperty(c)&&this.register(b.chart[c].prototype.id,b.chart[c])},register:function(a,b){this.all.push({id:a,func:b})},getChart:function(a,b){var c,d=document.getElementById(a);for(c=0;c<this.all.length;c+=1)if(b===this.all[c].id)return new this.all[c].func(d)}},b.chart.dForceGraph=function(a){this.container=a},b.chart.dForceGraph.prototype={id:"dForceGraph",draw:function(a,b){var c=a.getNumberOfColumns(),d=a.getNumberOfRows(),e=$.extend({maxnodesize:15,minnodesize:2},b),f=d3.scale.category20(),g=b.width,h=b.height,i=function(a){return!isNaN(parseFloat(a))&&isFinite(a)},j=[],k=[],l={},m={},n=0,o,p,q,r,s,t,u,v,w,x,y,z;for(o=0;o<d;o+=1)p=a.getValue(o,0),q=a.getValue(o,1),p!==null&&$.inArray(p,j)===-1&&(j.push(p),m[p]=c>2?Math.sqrt(a.getValue(o,2)):0,l[p]=c>3?a.getValue(o,3):0,m[p]>n&&(n=m[p])),q!==null&&$.inArray(q,j)===-1&&j.push(q),p!==null&&q!==null&&k.push({source:$.inArray(p,j),target:$.inArray(q,j)});n===0&&(n=1),r=e.maxnodesize/n;for(s=0;s<j.length;s+=1)t=typeof l[j[s]]!="undefined"?l[j[s]]:1,u=i(m[j[s]])?e.minnodesize+m[j[s]]*r:e.minnodesize,j[s]={name:j[s],color:t,size:u};$(this.container).empty(),v=d3.select(this.container).append("svg:svg").attr("width",g).attr("height",h).attr("pointer-events","all").append("svg:g").call(d3.behavior.zoom().on("zoom",function(){v.attr("transform","translate("+d3.event.translate+")"+" scale("+d3.event.scale+")")})).append("svg:g"),v.append("svg:rect").attr("width",g).attr("height",h).attr("fill","white"),w=d3.layout.force().gravity(.05).distance(100).charge(-100).nodes(j).links(k).size([g,h]).start(),x=v.selectAll("line.link").data(k).enter().append("svg:line").attr("class","link").attr("x1",function(a){return a.source.x}).attr("y1",function(a){return a.source.y}).attr("x2",function(a){return a.target.x}).attr("y2",function(a){return a.target.y}),y=v.selectAll("g.node").data(j).enter().append("svg:g").attr("class","node").call(w.drag),y.append("svg:circle").style("fill",function(a){return f(a.color)}).attr("class","node").attr("r",function(a){return a.size}),y.append("svg:title").text(function(a){return a.name}),y.append("svg:text").attr("class","nodetext").attr("dx",12).attr("dy",".35em").text(function(a){return a.name}),z=0,w.on("tick",function(){z+=1,z>250&&(w.stop(),w.charge(0).linkStrength(0).linkDistance(0).gravity(0).start()),x.attr("x1",function(a){return a.source.x}).attr("y1",function(a){return a.source.y}).attr("x2",function(a){return a.target.x}).attr("y2",function(a){return a.target.y}),y.attr("transform",function(a){return"translate("+a.x+","+a.y+")"})})}},b.chart.rdGraph=function(a){this.container=a},b.chart.rdGraph.prototype={id:"rdGraph",draw:function(a,c){var d=a.getNumberOfColumns(),e=a.getNumberOfRows(),f=$.extend({noderadius:.5,nodefontsize:"10px",nodeheight:20,nodestrokewidth:"1px",nodecornerradius:"1px",nodepadding:7,nodecolor:"green",edgestroke:"blue",edgefill:"blue",edgestrokewidth:1,edgefontsize:"10px",edgeseparator:", "},c),g=new Graph,h,i,j,k,l,m,n,o,p=function(a,c){return function(d,e){return d.set().push(d.rect(e.point[0],e.point[1],e.label.length*f.nodepadding,f.nodeheight).attr({fill:a,"stroke-width":f.nodestrokewidth,r:f.nodecornerradius})).push(d.text(e.point[0]+e.label.length*f.nodepadding/2,e.point[1]+f.nodeheight/2,e.label).attr({"font-size":f.nodefontsize}).click(function(){c&&window.open(b.parser.unprefixify(c))}))}},q=function(a,b,c){g.addNode(a,{label:b,render:p(c,a)})},r={},s=[];for(j=0;j<e;j+=1)m=a.getValue(j,0),n=a.getValue(j,2),m&&q(m,a.getValue(j,1)||m,d>5?a.getValue(j,5):f.nodecolor),n&&q(n,a.getValue(j,3)||n,d>6?a.getValue(j,6):f.nodecolor),m&&n&&(o="",typeof r[m+n]!="undefined"?o=r[m+n].label:s.push(m+n),d>4&&a.getValue(j,4).length>0&&(o.length>0&&(o+=f.edgeseparator),o+=a.getValue(j,4)),r[m+n]={source:m,target:n,label:o});for(k=0;k<s.length;k+=1)l=r[s[k]],g.addEdge(l.source,l.target,{stroke:f.edgestroke,fill:f.edgefill,label:l.label,width:f.edgestrokewidth,fontsize:f.edgefontsize});h=new Graph.Layout.Spring(g),h.layout(),$(this.container).empty(),i=new Graph.Renderer.Raphael(this.container,g,f.width,f.height,{noderadius:f.nodeheight*f.noderadius}),i.draw()}},b.chart.DefList=function(a){this.container=a},b.chart.DefList.prototype={id:"sDefList",draw:function(a,b){var c,d,e,f,g=a.getNumberOfColumns(),h=a.getNumberOfRows(),i=$.extend({cellSep:" ",termPrefix:"",termPostfix:":",definitionPrefix:"",definitionPostfix:""},b),j=$(document.createElement("dl"));for(c=0;c<h;c+=1){e=i.termPrefix+a.getValue(c,0)+i.termPostfix,j.append($(document.createElement("dt")).html(e)),f=i.definitionPrefix;for(d=1;d<g;d+=1)f+=a.getValue(c,d),d+1!==g&&(f+=i.cellSep);f+=i.definitionPostfix,j.append($(document.createElement("dd")).html(f))}$(this.container).empty(),$(this.container).append(j)}},b.chart.List=function(a){this.container=a},b.chart.List.prototype={id:"sList",draw:function(a,b){var c=a.getNumberOfColumns(),d=a.getNumberOfRows(),e=$.extend({list:"ul",cellSep:", ",rowPrefix:"",rowPostfix:""},b),f=$(document.createElement(e.list)),g,h,i;for(g=0;g<d;g+=1){i=e.rowPrefix;for(h=0;h<c;h+=1)i+=a.getValue(g,h),h+1!==c&&(i+=e.cellSep);i+=e.rowPostfix,f.append($(document.createElement("li")).html(i))}$(this.container).empty(),$(this.container).append(f)}},b.chart.sMap=function(a){this.container=a},b.chart.sMap.prototype={id:"sMap",draw:function(a,c){var d,e,f,g=a.getNumberOfColumns(),h,i;if(g>3){e=a.clone();for(i=g-1;i>2;i-=1)e.removeColumn(i);for(h=0;h<a.getNumberOfRows();h+=1)f="<div class='sgvizler sgvizler-sMap'>",f+="<h1>"+a.getValue(h,2)+"</h1>",5<g&&a.getValue(h,5)!==null&&(f+="<div class='img'><img src='"+a.getValue(h,5)+"'/></div>"),3<g&&a.getValue(h,3)!==null&&(f+="<p class='text'>"+a.getValue(h,3)+"</p>"),4<g&&a.getValue(h,4)!==null&&(f+="<p class='link'><a href='"+b.parser.unprefixify(a.getValue(h,4))+"'>"+a.getValue(h,4)+"</a></p>"),f+="</div>",e.setCell(h,2,f)}else e=a;d=new google.visualization.Map(this.container),d.draw(e,c)}},b.chart.Table=function(a){this.container=a},b.chart.Table.prototype={id:"sTable",draw:function(a,b){var c=a.getNumberOfColumns(),d=a.getNumberOfRows(),e=$.extend({headings:!0},b),f=$(document.createElement("table")),g,h,i;if(e.headings){i=$(document.createElement("tr"));for(g=0;g<c;g+=1)i.append($(document.createElement("th")).html(a.getColumnLabel(g)));f.append(i)}for(h=0;h<d;h+=1){i=$(document.createElement("tr"));for(g=0;g<c;g+=1)i.append($(document.createElement("td")).html(a.getValue(h,g)));f.append(i)}$(this.container).empty(),$(this.container).append(f)}},b.chart.Text=function(a){this.container=a},b.chart.Text.prototype={id:"sText",draw:function(a,b){var c=a.getNumberOfColumns(),d=a.getNumberOfRows(),e=$.extend({cellSep:", ",cellPrefix:"",cellPostfix:"",rowPrefix:"<p>",rowPostfix:"</p>",resultsPrefix:"<div>",resultsPostfix:"</div>"},b),f=e.resultsPrefix,g,h,i;for(g=0;g<d;g+=1){i=e.rowPrefix;for(h=0;h<c;h+=1)i+=e.cellPrefix+a.getValue(g,h)+e.cellPostfix,h+1!==c&&(i+=e.cellSep);f+=i+e.rowPostfix}f+=e.resultsPostfix,$(this.container).empty(),$(this.container).html(f)}},a.sgvizler=b})(window);"
        ScriptEngineManager factory = new ScriptEngineManager();
        // create a JavaScript engine
        ScriptEngine engine = factory.getEngineByName("JavaScript");
        // evaluate JavaScript code from String
        engine.eval("print('Welcme to java world')");
    }
}


/**
 * Module dependencies.
 */

//Function that checks if configuration file is correct
//and complete, and prevent startup otherwise
function checkConfigFile() {
	console.log('Checking configuration file...');
	var i, checkSPARQLEndPointInfo;
	if (!settings.RDFInterface) {
		console.error('missing "RDFInterface" in the configuration file');
		process.exit(1);
	}
	if ((settings.RDFInterface != "sparql") 
		&& (settings.RDFInterface != "ldp") 
		&& (settings.RDFInterface != "gsp")
	) {
		console.error('bad value for "RDFinterface" in the configuration file');
		process.exit(1);
	}
	if (settings.RDFInterface != "ldp") {
		if (!settings.SPARQLEndPoint) {
			console.error('SPARQLEndPoint required for "RDFInterface" equal to sparql and gsp');
			process.exit(1);
		}
		if ((settings.SPARQLEndPoint != "fuseki") 
			&& (settings.SPARQLEndPoint != "sesame")
			&& (settings.SPARQLEndPoint != "virtuoso")
		) {
			console.error('supported SPARQLEndPoint are "fuseki", "sesame", "virtuoso" (SPARQLEndPoint must have one of these value)');
			process.exit(1);
		}
		if (!settings.SPARQLEndPointInfo) {
			console.error('SPARQLEndPointInfo required for "RDFInterface" equal to sparql and gsp"');
			process.exit(1);
		}
		
		checkSPARQLEndPointInfo = new Array (
			"host",
			"port",
			"dataset",
			"testDataset"
		);
		
		if (settings.SPARQLEndPoint == "fuseki") {
			resultContentTypeParameters = {
	        	//this is the parameter in the query string to ask for json result
	        	JSON: "output=json",
	        	//this is the format used to get turtle in the response eg fuseki serilize data in ttl when format required is text
	        	CONSTRUCT_TTL: "output=text"
	        }
			checkSPARQLEndPointInfo.push(
				"fusekiServiceQuery", 
				"fusekiServiceUpdate"
			);
		} else if (settings.SPARQLEndPoint == "sesame") {
	    	resultContentTypeParameters =  {
	        	//parameter in the query string to ask for json result
	        	JSON: "Accept=application/sparql-results%2Bjson",
	        	//format used to get turtle in the response
	        	CONSTRUCT_TTL: "Accept=application/x-turtle"
	        }
			checkSPARQLEndPointInfo.push(
				"sesameServerLocation"
			);
		}
		
		for (i = 0; i < checkSPARQLEndPointInfo.length; i ++) {
			if (!settings.SPARQLEndPointInfo[checkSPARQLEndPointInfo[i]]) {
				console.error('"' + checkSPARQLEndPointInfo[i] + '" required in SPARQLEndPointInfo');
				process.exit(1);
			} 
		}
		
		if (settings.SPARQLEndPointInfo.dataset == settings.SPARQLEndPointInfo.testDataset) {
				console.error('Test dataset and production dataset cannot be the same');
				process.exit(1);
		}
		
		if (!settings.policyNamedGraphURI) {
			console.error('"policyNamedGraphURI" required for "RDFInterface" equal to sparql and gsp');
			process.exit(1);
		}
		
		
	}
	if (!settings.defaultPrefix) {
		console.error('"defaultPrefix" required');
		process.exit(1);
	}
	if (!settings.defaultBase) {
		console.error('"defaultBase" required');
		process.exit(1);
	}
	if (!settings.accessConditionType) {
		console.error('"accessConditionType" required');
		process.exit(1);
	}
	
	console.log('Configuration file OK');
}

var express = require('express')
  , routes = require('./routes')
  , targets = require('./routes/targets')
  , keywords = require('./routes/keywords')
  , policies = require('./routes/policies')
  , testPolicies = require('./routes/test-policies')
  //, login = require('./routes/login')
  , http = require('http')
  , path = require('path')
  
 //constant to ask for query result in case of ASK and SELECT
 //query in a particular format
 FAVORITE_SPARQL_RESULT_CONTENT = "json";
 //constant to ask for query result in case of CONSTRUCT
 //query in a particular format (ie allow to choose the serialization of RDF data)
 FAVORITE_CONSTRUCT_RESULT_CONTENT = "turtle";
 
var app = express();

// all environments
app.set('port', process.env.PORT || 3000);
app.set('views', __dirname + '/views');
app.set('view engine', 'jade');
app.use(express.favicon());
app.use(express.logger('dev'));
//parse text body eventually modularize
//cfr http://stackoverflow.com/questions/12497358/handling-text-plain-in-express-3-via-connect
app.use(function(req, res, next){
	if (req.is('text/*')) {
		req.text = '';
		req.setEncoding('utf8');
		req.on('data', function(chunk){ req.text += chunk });
		req.on('end', next);
	} else {
		next();
	}
});
app.use(express.bodyParser());
app.use(express.methodOverride());
app.use(express.cookieParser());
app.use(app.router);
app.use(express.static(path.join(__dirname, 'public')));

// development only
if ('development' == app.get('env')) {
  app.use(express.errorHandler());
}

fs = require('fs');
eval(fs.readFileSync('config.js', encoding = "ascii"));
checkConfigFile();
langFileName = settings.language + ".js"
eval(fs.readFileSync(langFileName, encoding = "ascii"));

storage = eval(fs.readFileSync('resources/storage.js', encoding = "ascii"));

//if not managed explicitly express when a get on '/' occur
//the "public/index.html" file (if any) is send
app.get('/', routes.index); //to send index page with custom usr in bar by means of jade template

app.post('/policies', policies.Policies.create);
app.put('/policies',policies.Policies.create);
app.get('/policies', policies.Policies.read);
app.del('/policies', policies.Policies.del);

app.get('/targets', targets.Targets.list);
app.post('/targets', targets.Targets.add);
app.put('/targets', targets.Targets.overwrite);

app.get('/vocabularies/user-dim', keywords.list);
app.get('/vocabularies/dev-dim', keywords.list);
app.get('/vocabularies/env-dim', keywords.list);

app.post('/policy-testing/data', testPolicies.TestPolicies.initTestDataset);
app.post('/policy-testing/context', testPolicies.TestPolicies.queryViaShi3ld);

http.createServer(app).listen(app.get('port'), function() {
  console.log('Express server listening on port ' + app.get('port'));
});

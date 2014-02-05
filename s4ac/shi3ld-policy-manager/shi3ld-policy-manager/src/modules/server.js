var restify = require('restify');
var server = restify.createServer();
server.use(restify.bodyParser());



function start(route) {
	fs.readFile('../WebContent/index.html', function (err, data) {
	    if (err) {
	        throw err;
	    }
	    index = data;
	});
	
	// Set up our routes and start the server
	server.get('/', getHome);
	server.get('/policies', getPolicies);
	server.post('/policies', postPolicy);
	server.delete('/policies', deletePolicy);
	server.put('/policies', putPolicy);
	server.get('/targets', getTargets);
	server.post('/targets', postTarget);

	

	
  	http.createServer(function (request, response) {
	    var pathname = url.parse(request.url).pathname;
	    console.log("Request for " + pathname + " received.");
	    
	    route(pathname);
	    
	    response.writeHead(200, {"Content-Type": "text/html"});
	    response.write(index);
	    response.end();
	  }).listen(8888);
  	console.log("Server has started.");
}

exports.start = start;
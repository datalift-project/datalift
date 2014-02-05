/**
 * keyword route
 */
exports.list = function(req, res){
  eval(fs.readFileSync('resources/keywords.js', encoding="ascii"));
  
  console.log("url of the req: " + req.originalUrl);
  
  if (req.originalUrl == '/vocabularies/user-dim') {
  	res.send(keywords.user);
  	return;
  }
  if (req.originalUrl == '/vocabularies/env-dim') {
  	res.send(keywords.env);
  	return;
  }
  res.send(keywords.dev);
};
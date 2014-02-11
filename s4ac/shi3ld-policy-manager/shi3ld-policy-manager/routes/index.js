/*
 * GET home page.
 */
var sparqlClient = require('../lib/sparqlClient');


exports.index = function(req, res){

   var oneDayInMillisec = 24 * 60 * 60 * 1000;
   var backend, acCnt;
   
   //to distinguish sparql endpoint (sparlq or gsp) vs ldp
   if ((settings.RDFInterface == 'sparql') || (settings.RDFInterface == 'gsp'))  {
   	backend = 'sparql';
   	policyListParagraph = language.policyListSparql;
   } else {
   	backend = 'ldp';
   	policyListParagraph = language.policyListLdp;
   }
   res.cookie('backend', backend, {maxAge: oneDayInMillisec})
   //in case of http scenario with ldp and internal sparql access condition is still expressed in sparql
   res.cookie('accessConditionType', settings.accessConditionType, {maxAge: oneDayInMillisec});
   res.cookie('dateFormat', settings.dateFormat, {maxAge: oneDayInMillisec});
   res.cookie('timeFormat', settings.timeFormat, {maxAge: oneDayInMillisec});
   res.cookie('defaultPrefix', settings.defaultPrefix, {maxAge: oneDayInMillisec});
   res.cookie('defaultBase', settings.defaultBase, {maxAge: oneDayInMillisec});
   //Now assumed in file: consider if put in triple store (eg a triples in policy graphs)
   //or if a more proper storage possible
   res.cookie('acCnt', storage.acCnt, {maxAge: oneDayInMillisec});
   
   res.render('index', { 
    usr: settings.user,
   	newPolicyTooltip: language.newPolicyTooltip,
   	openPolicyTooltip: language.openPolicyTooltip,
   	savePolicyTooltip: language.savePolicyTooltip,
   	testPoliciesTooltip: language.testPoliciesTooltip,
   	policyWorkspaceTooltip: language.policyWorkspaceTooltip,
   	//policyWorkspace: language.policyWorkspace,
   	//policyEditor: language.policyEditor
   	policyPanelMsg: language.policyPanelMsg,
   	policyListParagraph: policyListParagraph
   });
   
};
Shi3ld Policy manager Demo setup

----------- Scenario -----------
Musuem triples, OpenRDF Sesame + Shi3ld-SPARQL + Shi3ld Policy Manager


----------- Contents -----------
- museum-triples.ttl
- shi3ld-policy-manager.zip
- shi3ld.war
- README.txt


----------- Requirements -----------
- Apache Tomcat 7.0.33
- Node.js 0.10.5
- OpenRDF Sesame 2.6.6


----------- OpenRDF Sesame Setup -----------
- Run Tomcat w/ deployed OpenRDF Sesame
- Access Sesame workbench (http://localhost:8080/openrdf-workbench)
- Create New repository (Repositories->"New Repository"):
	* Type: in memory
	* ID: museum
	* Title: ISWC2013 demo test dataset
- Add musuem.ttl triples to "musuem" repository. Select museum repository, Modify->add:
	* Base URI: http://museum.example.org/data/
	* use base URI as context identifier: yes
	* data format: turtle
	* RDF Data File: choose musueum.ttl
	* click on "upload"
- Create New repository for sandbox (Repositories->"New Repository"):
	* Type: in memory
	* ID: museum-sandbox
	* Title: ISWC2013 demo test dataset (sandbox)


----------- Shi3ld-SPARQL Setup -----------
- Shi3ld config.java file is embedded in shi3ld.war and sah already been pre-set with the following values:
	* sesameServer = "http://localhost:8080/openrdf-sesame"
	* baseUri = "http://museum.example.org/data/"
	* policyNamedGraph = "http://museum.example.org/policies/"
	* secureRep = "museum-sandbox"
- Deploy shi3ld.war in Tomcat


----------- Shi3ld Policy Manager Setup -----------
- Unzip shi3ld-policy-manager.zip
- config.js has been pre-set to work w/ Sesame:
	* SPARQLEndPoint: "sesame"
	* sesameServerLocation: "openrdf-sesame"
	* policyNamedGraphURI: "http://museum.example.org/policies/"
	* host: "localhost"
	* port: "8080"
    * dataset: "museum"
	* testDataset: "museum-sandbox"
- Open a terminal in shi3ld-policy-manager/ and type:
	node app.js


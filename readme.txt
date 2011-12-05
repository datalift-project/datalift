In order to run silk-sample in datalift, the following steps should be done:
1. go to datalift/silk-sample/src/java/org/datalift/samples/project, open the HandleProjectModule.java file, change the path of the files: regions-2010.rdf, nuts2008_complete.rdf and script.xml to the path of your computer environment. regions-2010.rdf and nuts2008_complete.rdf are two input RDF data sets, you can change them to other RDF data sets files. But pay attention, you also need to specify the basic uri of your data sets to variables baseURIin1 and baseURIin2. script.xml is the silk script that link the data sets. 
2. compile datalift
3. create a folder named "silk" in datalift-home/modules
4. put the silk-sample.jar in datalift-home/modules/silk
5. download sesame 2.6.0, put openrdf-sesame.war and openrdf-workbench.war into tomcat/webapps
6. restart tomcat
7. go to http://localhost:8080/datalift/workspace and create a project
8. go to http://localhost:8080/datalift/sparql and query by
SELECT * WHERE {
  ?s <http://www.w3.org/2002/07/owl#sameAs> ?o .
}
9. the link result comes.

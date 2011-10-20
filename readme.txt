In order to run silk-sample in datalift, the following steps should be done:
1. go to datalift/silk-sample/src/java/org/datalift/samples/project, open the HandleProjectModule.java file, change the path of the files: regions-2010.rdf, nuts2008_complete.rdf, insee_eurostat_without_heuristic.xml, insee_eurostat_with_heuristic_on_level.xml and result.xml to the path of your computer environment. regions-2010.rdf and nuts2008_complete.rdf are two input RDF data sets, you can change them to other RDF data sets files. Accordingly, you also should change insee_eurostat_without_heuristic.xml and insee_eurostat_with_heuristic_on_level.xml into the corresponding Silk specification file. result.xml is the output file storing the link set. So this file doesn't need to be changed.
2. compile datalift
3. create a folder named "silk" in datalift-home/modules
4. put the silk-sample.jar in datalift-home/modules/silk
5. restart tomcat
6. go to http://localhost:8080/datalift/workspace and create a project
7. The result will be produced even the message "Oops!" appears. (I will make a webpage ASAP to solve this problem)
8. go to http://localhost:8080/datalift/sparql and query by
SELECT * WHERE {
  ?s <http://www.w3.org/2002/07/owl#sameAs> ?o .
}
9. the link result comes.

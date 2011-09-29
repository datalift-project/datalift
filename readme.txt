In order to run silk-sample in datalift, the following steps should be done:
1. go to datalift/silk-sample/src/java/org/datalift/samples/project, open the HandleProjectModule.java file, change the path of the files: regions-2010.rdf, nuts2008_complete.rdf, insee_eurostat.xml and result.xml to the path to your computer environment. regions-2010.rdf and nuts2008_complete.rdf are two input RDF data sets, you can change them to other RDF data sets files. Accordingly, you also should change insee_eurostat.xml into the corresponding Silk specification file. result.xml is the output file storing the link set. So this file doesn't need to be changed.
2. put create a folder silk2.4.2 under folder datalift/core/lib/runtime, and put silk.jar file in it. The path looks like:datalift/core/lib/runtime/silk2.4.2/silk.jar
3. compile datalift
4. create a folder named "silk" in datalift-home/modules
5. put the silk-sample.jar in datalift-home/modules/silk
6. put the silk.jar in silk_2.4.2 in datalift-home/modules/silk
7. restart tomcat
8. go to http://localhost:8080/datalift/workspace and create a project
9. The result will be produced even the message "Oops!" appears. (I will make a webpage ASAP to solve this problem)
10. go to http://localhost:8080/datalift/sparql and query by
SELECT * WHERE {
  ?s <http://www.w3.org/2002/07/owl#sameAs> ?o .
}
11. the link result comes.

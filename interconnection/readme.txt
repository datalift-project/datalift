In order to run interconnection module in datalift, the following steps should be done:
1. create a project
2. go to "Sources" to upload two RDF data sets
3. go back to "Description", press button "RDF source file loading", loading the RDF data sets that you upload at 2nd step
4. go back to "Description", press button "Data publishing to public RDF store", publishing the RDF data sets that you load at 3nd step
5. go back to "Description", press button "Interconnection", upload the SILK script file (if you are using sinlge user version, please upload the file named "script_single_user_version.xml"), and fill the linkSPecId, number of Threadshold and reload or not(true or false). For example, in our case, they are "region", 1 and true respectively. Finally, press button "Run SILK!" 
6. if a page shows "OK~~" comes, go to http://localhost:8080/datalift/sparql and query by
SELECT * WHERE {
  ?s <http://www.w3.org/2002/07/owl#sameAs> ?o .
}
the link result comes.
7. or you also can create the SILK script, and run it, check the links in sparql endpoint also.

PAY ATTENTION: 
i. download sesame 2.6.4, put openrdf-sesame.war and openrdf-workbench.war into tomcat/webapps
ii. we put two RDF data sets and two SILK script files in the folder datalift/interconnection/example for users to test, one script is for git version, another script is for single user version

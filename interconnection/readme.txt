In order to run interconnection module in datalift, the following steps should be done:

1. create a project

2. go to "Sources" to add two RDF data sets that you are going to interlink, eg. insee's data set "regions-2010.rdf" and eurostat's data set "nuts2008_complete.rdf" in the example folder of the interconnection module

3. go back to "Description", press button "Import of RDF source(file or SPARQL)" to load the RDF data sets that you've added at the 2nd step

4. go back to "Description", press button "Data publishing to public RDF store" to publish the RDF data sets that you've loaded at the 3rd step 

5. go back to "Description", press button "Interconnection", then you have three choices to generate links:

i) upload the SILK script file, eg. "script.xml" for insee and eurostat data sets in the example folder of the interconnection module and press button "Run" 

ii) or you also can create a SILK script and press button "Run"  

iii) upload an ontology alignment file written in EDOAL, eg. "insee_nuts.xml" for insee and eurostat ontologies in the example folder. In this case, you should load, import and publish the ontologies of two data sets before uploading the EDOAL file. For example, the files of insee and eurostat data sets' ontologies "onto1_file.rdf" and "onto2_file.rdf" in the example folder. After that, specify the source and target data set addresses, in our example, it is datalift's sparql endpoint, which is "http://localhost:8080/datalift/sparql". Finally, press button "Run" 

6. if a page showing "OK~~ Data linking completes! " comes, go to http://localhost:8080/datalift/sparql and press the button "Links", then you can check whether there is links produced.

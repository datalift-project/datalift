S4AC Module - MAIN FEATURES

The main features of the access control module are the following:
- the module protects named graphs. This means that the protection goes from single triples (a named graph composed by one single triple) up to whole datasets (a unique named graph including all the triples contained in the dataset);
- the access policies are defined using RDF and SPARQL 1.1 ASK clause;
- the module relies on the following two lightweight vocabularies to express the access conditions:
	* S4AC: core access control concepts;
	* PRISSMA: contextual information about the user;
- if the SPARQL endpoint is not protected, i.e., there are no policies defined, then everybody can access the data. This means that the protected SPARQL endpoint is equivalent to a non protected SPARQL endpoint.

More information about the access control module can be found at http://wimmics.inria.fr/projects/shi3ld/

=======================

INSTRUCTIONS (DataLift platform full version, no wrapper)


1) Since the module constrains the access to named graphs, the user must first identify the named graphs to protect in the dataset, selecting their URIs. We provide a test dataset whose named graphs are associated to the sample policies (triples extracted from the Berlin Benchmark). The dataset contains 3 named graphs (in separate files, TriG syntax) to be loaded using the DataLift platform. Note that the access control module protects only the data published on the public RDF store (lifted repository).

In order to test the access control module, the following steps have to be performed to publish the named graphs:
STEP 1: Add each file as new source of a DataLift project.
STEP 2: Use the module "Import of RDF source" to import each uploaded source as RDF source. In the text box labelled "Named graph URI", the user has to paste the URI of the named graph contained in the uploaded source.
STEP 3: Use the module "Data publishing to public RDF store" to publish the named graph from the internal repository to the public (lifted) repository. Again, in the text box labelled "Named graph URI", the user has to paste the URI of the named graph contained in the uploaded source.

The URIs of the named graphs of the test dataset are as follows:
Berlin_1_named_graph.trig: http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromRatingSite1/Graph-reviews
Berlin_1-2_named_graph.trig: http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromRatingSite2/Graph-reviews
Berlin_1-3_named_graph.trig: http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromVendor1/Graph-2005-11-01

2) The user has to define her own access policies. A set of sample access policies is provided (file policies.rdf.xml). These sample policies protect the named graphs of the test dataset we provided. 


If the user has to associate them to other named graphs, she needs to change the file policies.rdf.xml as follows: from file policies.rdf.xml

<s4ac:appliesTo rdf:resource="http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromRatingSite1/Graph-reviews" />

the user substitutes the URI of the test named graph with the URI of the actual named graph she wants to protect.


The file containing the policies has to be stored in core/build/datalift-home/conf/


Finally, the name of the file containing the policies has to be pointed from the datalift-application.properties file as follows:

sparql.security.policy.files = ${datalift.home}/conf/policies.rdf.xml


Note that by default this line is present in datalift-application.properties. The user needs to change it only if the name of the policies' file is modified.

3) The user has to create a new repository called "secured" (e.g., using OpenRDF Workbench if DataLift is running on Sesame) to store the user's profiles (under the form of named graphs). Three sample user's profiles named graphs are provided in the file usersGraphs.txt (TriG syntax). Using the sample access policies and the sample user's profiles, the result of an access request is that uid1 is allowed to access all named graphs, uid2 cannot access any named graph, and uid3 is allowed to access only the third named graph.

4) The user has to update datalift-users.properties (core/build/datalift-home/conf/) with the following information:

user.uid1 = uid1pwd, datalift
user.uid2 = uid2pwd, datalift
user.uid3 = uid3pwd, datalift

where the username of the user is "uid1" and her password is "uid1pwd". This information must be entered for each user having a profile stored in the secured dataset. 
Note that the username uid1 is linked to the URI of the named graph containing the information about user uid1, e.g., <http://example.com/context/uid1>

To check the actual working of the access control module it is sufficient to raise a SELECT query on the (now protected) SPARQL endpoint.
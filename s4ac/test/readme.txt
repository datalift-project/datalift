S4AC Module - Instructions

1- The module constrains the access to named graphs. This means that you must first identify the named graphs in the dataset (a test dataset whose named graphs are associated to the policies may be found in file berlin-test.trig from the Berlin Benchmark).

2- You have to create a new repository called secured where you will store the user's profiles (under the form of named graphs). The secured repository has to be added into the datalift-application.properties as follows:

#secured repository
secured.repository.url = http://localhost:8080/openrdf-sesame/repositories/secured 


The s4ac sparql endpoint protects the access to the lifted repository.
To use the s4ac sparql endpoint, you have to build the s4ac module and copy dist/s4ac.jar in datalift-home/modules, as for the other modules.com'e'


3- You have to associate to each policy (expressed in RDF) which are the named graphs it is associated to.

EXAMPLE:

From file policies.rdf.xml

<s4ac:appliesTo rdf:resource="http://www4.wiwiss.fu-berlin.de/bizer/bsbm/v01/instances/dataFromRatingSite1/Graph-reviews" />

you substitute the URI of the test named graph with the named graph you want to protect.

4- The policies have to be stored in a file (e.g., policies.rdf.xml) pointed from the datalift-application.properties file as follows:

sparql.security.policy.files = ${datalift.home}/conf/policies.rdf.xml


5- You have to store the named graphs representing the users' profiles (contained in usersGraphs.txt) into the secured repository.

6- Given the users' profiles in usersGraphs.txt, you have to update the file datalift-users.properties (core/build/datalift-home/conf/)
with the following information:

user.uid1 = uid1pwd, datalift
user.uid2 = uid2pwd, datalift
user.uid3 = uid3pwd, datalift

Note that if you introduce a new user profile in the secured repository, you must add it also in this file.

The actual users' profiles and the defined policies allow uid1 to access to all named graphs, uid2 cannot access any named graph, and uid3 is
allowed to access only few named graphs. 

If the SPARQL endpoint is not protected, i.e., there are no policies defined, then everybody can access the data, i.e., the S4AC SPARQL endpoint is equivalent to a non protected SPARQL endpoint.

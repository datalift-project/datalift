This a native application wrapper for packaging and running DataLift as a standalone single-user application.
This wrapper supports building a native application for the following opérating systems:
 - Mac OS X 10.6 (Leopard) or higher, Java 6+ required
 - Windows ???
 - Linux

When run, the wrapper first starts a standalone DataLift server and then opens a new tab or window in the user's default Web browser, pointing it at the SPARQL endpoint of the DataLift server.

Please note that the standalone DataLift server is not a general purpose DataLift installation. In particular, it performs no user authentication, expecting that only the user that started the application will access it.


1. Mac OS X
-----------
The DataLift application initializes a DataLift runtime environment for the user the first time it is started.

It stores data in the following locations (<user home> denoting the user home directory):
 - <user home>/Library/Application Support/DataLift: the persistent DataLift data, as follows:
    - conf:		the DataLift (editable) configuration
    - modules:		supplementary user-specific DataLift modules
    - repositories: 	OpenRDF Sesame RDF databases (projects, lifted data...)
    - storage:		Uploaded data source files
 - <user home>/Library/Caches/DataLift: temporary files, data caches...
 - <user home>/Library/Logs/DataLift: log files

Users can install modules in addition to the default ones (project ans SPARQL endpoint) by dropping the module JARs into <user home>/Library/Application Support/DataLift/modules.

2. Windows
----------


3. Linux
--------
Yet to be done...


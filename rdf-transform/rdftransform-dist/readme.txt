Generates the final rdf-transform.jar.

- Unzips server part : ../rdftransform-server/dist/rdf-transform-server.jar
- Copies all dependencies (alibaba, jackson, etc.) in same folder
- Copies result of GWT compilation : ../rdftransform-client/target/rdf2rdf-client-1.0-SNAPSHOT (except WEB-INF) in same folder
- jar all this into a single Datalift module jar that can be copied under <datalift-home>/modules

/*
 * Copyright / Copr. 2010-2013 Atos - Public Sector France -
 * BS & Innovation for the DataLift project,
 * Contributor(s) : L. Bihanic, H. Devos, O. Ventura, M. Chetima
 *
 * Contact: dlfr-datalift@atos.net
 *
 * This software is governed by the CeCILL license under French law and
 * abiding by the rules of distribution of free software. You can use,
 * modify and/or redistribute the software under the terms of the CeCILL
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info".
 *
 * As a counterpart to the access to the source code and rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty and the software's author, the holder of the
 * economic rights, and the successive licensors have only limited
 * liability.
 *
 * In this respect, the user's attention is drawn to the risks associated
 * with loading, using, modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean that it is complicated to manipulate, and that also
 * therefore means that it is reserved for developers and experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or
 * data to be ensured and, more generally, to use and operate it in the
 * same conditions as regards security.
 *
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL license and that you accept its terms.
 */

package org.datalift.samples.project;

import java.io.File;
import java.io.FileOutputStream;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.datalift.fwk.BaseModule;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.openrdf.OpenRDFException;
import org.openrdf.model.Statement;
import org.openrdf.model.ValueFactory;
import org.openrdf.query.GraphQuery;
import org.openrdf.query.GraphQueryResult;
import org.openrdf.query.QueryLanguage;
import org.openrdf.query.TupleQuery;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.http.HTTPRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.rdfxml.RDFXMLWriter;

import de.fuberlin.wiwiss.silk.Silk;

public class HandleProjectModule extends BaseModule implements ProjectModule
{
    private final static Logger log = Logger.getLogger();

    public HandleProjectModule() {
        super("sample-silk", true);
    }

    @Override
    public UriDesc canHandle(Project p) {
        try {       	
            //store two interlinking RDF datasets
            String sesameServer = "http://localhost:8080/openrdf-sesame";
            String repositoryID = "lifted";
            
            Repository myRepositoryin = new HTTPRepository(sesameServer, repositoryID);
            ValueFactory f = myRepositoryin.getValueFactory();
            
            File filein1 = new File("E:\\datalift_2.1_1.1\\datalift\\silk-sample\\src\\java\\org\\datalift\\samples\\project\\regions-2010.rdf");
            String baseURIin1 = "http://rdf.insee.fr/geo/";
            org.openrdf.model.URI contextin1 = f.createURI(baseURIin1);	
            File filein2 = new File("E:\\datalift_2.1_1.1\\datalift\\silk-sample\\src\\java\\org\\datalift\\samples\\project\\nuts2008_complete.rdf");
            String baseURIin2 = "http://ec.europa.eu/eurostat/ramon/ontologies/geographic.rdf#";
            org.openrdf.model.URI contextin2 = f.createURI(baseURIin2);
            
            try {
                RepositoryConnection conin = myRepositoryin.getConnection();
                try {
                	conin.add(filein1, baseURIin1, RDFFormat.RDFXML, contextin1);
                	conin.add(filein2, baseURIin2, RDFFormat.RDFXML, contextin2);
                	}
                finally {
                         conin.close();
                        }
                }
            catch (OpenRDFException e) {  // handle exception                      	
                   }
                	
            File fspec = new File("E:\\datalift_2.1_1.1\\datalift\\silk-sample\\src\\java\\org\\datalift\\samples\\project\\insee_eurostat.xml");
        	Silk.executeFile(fspec, "region", 1, true);
            //System.out.println("I AM HERE: " + System.getProperty("user.dir"));                          
            //store the result in the Repository API
            File fileout = new File(System.getProperty("user.dir")+ "/insee_eurostat_accepted_links.xml");
            String baseURIout = "http://knowledgeweb.semanticweb.org/heterogeneity/alignment#";
            
            Repository myRepositoryout = new HTTPRepository(sesameServer, repositoryID);
            try {
               RepositoryConnection conout = myRepositoryout.getConnection();              
               try {
                  conout.add(fileout, baseURIout, RDFFormat.RDFXML);                 
                  //query using CONSTRUCT and form a owl:sameAs graph
                  File filelink = new File("E:\\datalift_2.1_1.1\\datalift\\silk-sample\\src\\java\\org\\datalift\\samples\\project\\result.xml");
                  FileOutputStream out = new FileOutputStream(filelink);
                  String sparqlQuery = " PREFIX owl: <http://www.w3.org/2002/07/owl#> " +
                  		               " CONSTRUCT { ?a owl:sameAs ?b } " +
                  		               " WHERE {" +
                  		               " ?d <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity1> ?a . " +
                  		               " ?d <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#entity2> ?b . " +
                  		               " ?d <http://knowledgeweb.semanticweb.org/heterogeneity/alignment#relation> \'=\'" +
                  		         	   " } ";
                  RDFXMLWriter result = new RDFXMLWriter(out);
                  GraphQuery query = conout.prepareGraphQuery(QueryLanguage.SPARQL, sparqlQuery);
                  query.evaluate(result);                                  
                  conout.add(filelink, "http://www.w3.org/1999/02/22-rdf-syntax-ns#", RDFFormat.RDFXML); 
               }
               finally {
                  conout.close();
               }
            }
            catch (OpenRDFException e) {               // handle exception                      	
            }
                 	
            return new UriDesc(this.getName() + "/cat.jpg",
                               this.getName() + ".module.label");     	
            
        }
        catch (Exception e) {
            log.fatal("Uh?", e);
            throw new RuntimeException(e);
        }
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String doGet() {
        return "Test HandleProjectModule index";
    }
}


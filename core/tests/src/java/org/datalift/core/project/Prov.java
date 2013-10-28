package org.datalift.core.project;

import static org.datalift.core.DefaultConfiguration.DATALIFT_HOME;
import static org.datalift.core.DefaultConfiguration.PRIVATE_STORAGE_PATH;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_DEFAULT_FLAG;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_URIS;
import static org.datalift.core.DefaultConfiguration.REPOSITORY_URL;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.datalift.core.DefaultConfiguration;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.CsvSource;
import org.datalift.fwk.project.Ontology;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;


// TODO: test DefaultProjectManager.newProject

public class Prov {
    private final static String RDF_STORE = "internal";

    private Properties props = new Properties();
    
    private static Logger logger = Logger.getLogger(Prov.class);
	
	@Before
	public void setUp() throws Exception {
        this.props.put(DATALIFT_HOME, "tests");
        this.props.put(REPOSITORY_URIS, RDF_STORE);
//        this.props.put(RDF_STORE + REPOSITORY_URL, "sail:///");
        this.props.put(RDF_STORE + REPOSITORY_URL, 
        		"http://localhost:9091/openrdf-sesame/repositories/tests");
        this.props.put(RDF_STORE + REPOSITORY_DEFAULT_FLAG, "true");
        this.props.put(PRIVATE_STORAGE_PATH, ".");
	}

	@After
	public void tearDown() throws Exception {

	}

	@Test
	public void testProjectSave() throws Exception {

		//-----------------------------------------------------------------
		// Configure
		//-----------------------------------------------------------------

		logger.info("Beginning Prov Test");
		
		String projectTitle = "MyProject";
		String projectDesc = "Project created to test prov";
		String csvTitle = "kiosques";
		String csvPath = "tests/files/file";

		File file = new File(csvPath);
		file.createNewFile();
		
		DefaultProjectManager pm = initProjectManager();
		
		//-----------------------------------------------------------------
		// Write
		//-----------------------------------------------------------------
		
		// Create a project, a CSV source and save it.
		URI projectURI = new URI("http://datalift.fr/proj/myproject");
		URI licenseURI = 
				new URI("http://creativecommons.org/licenses/by-nc-sa/3.0/");
		URI csvURI = new URI("http://datalift.fr/test");
		Project project = pm.newProject(projectURI, projectTitle, 
				projectDesc, licenseURI);
		CsvSource csv = pm.newCsvSource(project, csvURI, csvTitle, 
				"Kiosque: prov test", csvPath, ',');
		
		project.add(csv);
		pm.saveProject(project);

		// Create an ontology and save it.
		URI ontoURI = new URI("http://datalift.fr/onto/myontology");
		Ontology ontology = pm.newOntology(project, ontoURI, "MyOntology");
		pm.saveOntology(ontology);
		
		//-----------------------------------------------------------------
		// Read
		//-----------------------------------------------------------------
		
		// Read it
		Project project2 = pm.findProject(projectURI);
		CsvSource csv2 = (CsvSource) project2.getSource(csvURI);
		User user2 = project.getWasAttributedTo();
		List<Ontology> ontologies = pm.getOntologies(project2);
		
		System.out.println(ontologies.get(0).getTitle());
		
		assertEquals("PROV: Project: The title is wrong.", 
				project2.getTitle(), projectTitle);
		assertEquals("PROV: Project: The license is wrong.", 
				project2.getLicense(), licenseURI);
		assertEquals("PROV: Project: The description is wrong.", 
				project2.getDescription(), projectDesc);

		assertEquals("PROV: CSVSource: The csv title is wrong.", 
				csv2.getTitle(), csvTitle);
		assertEquals("PROV: CSVSource: The path is wrong.", 
				csv2.getFilePath(), csvPath);

		assertEquals("PROV: Project.User: The user is wrong.", 
				user2.getIdentifier(), null);
		assertEquals("PROV: Project.User: The actedOnBehalfOf is wrong.", 
				user2.getActedOnBehalfOf(), null);
		
		assertEquals("PROV: Ontologies owned by a project: Error.",
				ontologies.get(0).getTitle(), "MyOntology");
	}
	
	@Test
	public void testProvEvent() throws URISyntaxException {
		DefaultProjectManager pm = initProjectManager();
		ProjectCreationEvent e = new ProjectCreationEvent();
		UserImpl u = new UserImpl("John");
		URI projectURI = new URI("http://datalift.fr/proj/myproject");
		URI licenseURI = 
				new URI("http://creativecommons.org/licenses/by-nc-sa/3.0/");
		String eventURI = "http://datalift.fr/event/newProj1";
		Project p = pm.newProject(projectURI, "MyProject", 
				"Description.", licenseURI);
		
		e.setId(eventURI);
		e.setDescription("Test creation event");
		e.setParameter("none");
		e.setStartedAtTime(new Date());
		e.setEndedAtTime(new Date());
		e.setWasAssociatedWith(u);
		e.setUsed(p);
		
		pm.saveEvent(e);
	}
	
	private DefaultProjectManager initProjectManager() {
        // Load application configuration.
        DefaultConfiguration cfg = new DefaultConfiguration(this.props);
        cfg.init();
        Configuration.setDefault(cfg);
        
        // Create a project manager
		DefaultProjectManager pm = new DefaultProjectManager();
		pm.init(cfg);
		pm.postInit(cfg);
		
		return pm;
	}

	@Test
	public void testProjectRemove() throws URISyntaxException {
		URI projectURI = new URI("http://datalift.fr/proj/myproject");
		DefaultProjectManager pm = initProjectManager();
		Project project = pm.findProject(projectURI);
		
		if (project != null)
			pm.deleteProject(project);
	}
	
//	private void removeDataliftGraph() {
//		EntityManager m = f.createEntityManager();
//		Query q = m.createQuery(
//				"select distinct ?result where { " +
//				"?result a empire:Event ; dcterms:subject ??bo . }");
//	}
	
//	@Test
//	public void testProv() throws Exception {
//		DefaultProjectManager pm = initProjectManager();
//		
//		// Create a project and a CSV source
//		URI projectURI = new URI("http://projects.fr/myproject");
//		URI licenseURI = 
//				new URI("http://creativecommons.org/licenses/by-nc-sa/3.0/");
//		URI csvURI = new URI("http://datalift.fr/test");
//		Project project = pm.newProject(projectURI, "Title", 
//				"Desc.", licenseURI);
//		CsvSource csv = pm.newCsvSource(project, csvURI, "CSV Title", 
//				"Kiosque: prov test", "tests/files/file", ',');
//		
//		// Save it
//		project.add(csv);
//		pm.saveProject(project);
//	}
}

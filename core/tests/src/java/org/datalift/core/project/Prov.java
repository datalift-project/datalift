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
import java.util.Properties;

import org.apache.log4j.Logger;
import org.datalift.core.DefaultConfiguration;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.project.CsvSource;
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
		logger.info("Beginning Prov Test");
		
		String projectTitle = "MyProject";
		String projectDesc = "Project created to test prov";
		String csvTitle = "kiosques";
		String csvPath = "tests/files/file";

		File file = new File(csvPath);
		file.createNewFile();
		
		DefaultProjectManager pm = initProjectManager();
		
		
		// Create a project and a CSV source
		URI projectURI = new URI("http://datalift.fr/proj/myproject");
		URI licenseURI = 
				new URI("http://creativecommons.org/licenses/by-nc-sa/3.0/");
		URI csvURI = new URI("http://datalift.fr/test");
		Project project = pm.newProject(projectURI, projectTitle, 
				projectDesc, licenseURI);
		CsvSource csv = pm.newCsvSource(project, csvURI, csvTitle, 
				"Kiosque: prov test", csvPath, ',');
		
//		// Add user
//		UserImpl user = new UserImpl("Foo");
//		project.setWasAttributedTo(user);
		
		// Save it
		project.add(csv);
		pm.saveProject(project);
		
		// Read it
		Project project2 = pm.findProject(projectURI);
		CsvSource csv2 = (CsvSource) project2.getSource(csvURI);
		User user2 = project.getWasAttributedTo();
		
		assertEquals("The project names are different.", 
				project2.getTitle(), projectTitle);
		assertEquals("The project descriptions are different.", 
				project2.getDescription(), projectDesc);
		assertEquals("The csv titles are different.", 
				csv2.getTitle(), csvTitle);
		assertEquals("The csv file paths are different.", 
				csv2.getFilePath(), csvPath);
		assertEquals("The user is wrong.", 
				user2.getIdentifier(), null);
		assertEquals("The User.getActedOnBehalfOf is wrong.", 
				user2.getActedOnBehalfOf(), null);
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

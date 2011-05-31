package org.datalift.core.empire;

import static org.datalift.fwk.util.StringUtils.join;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.spi.PersistenceProvider;

import org.datalift.core.project.CsvSourceImpl;
import org.datalift.core.project.CustomAnnotationProvider;
import org.datalift.core.project.DbSourceImpl;
import org.datalift.core.project.OntologyImpl;
import org.datalift.core.project.ProjectImpl;
import org.datalift.core.project.RdfSourceImpl;
import org.datalift.fwk.Configuration;

import com.clarkparsia.empire.Empire;
import com.clarkparsia.empire.config.EmpireConfiguration;
import com.clarkparsia.empire.sesametwo.OpenRdfEmpireModule;

public class EmpireFactory {
	
	private static EmpireFactory 	instance = null;
	
	private EntityManagerFactory	emf = null;
	
	private final static String REPOSITORY_URL_PARSER = "/repositories/";
	
	private EmpireFactory(Configuration configuration) {
		emf = this.createEntityManagerFactory(configuration
					.getInternalRepository().url);
	}
	
	public static EmpireFactory	getInstance(Configuration configuration) {
		if (instance == null)
			instance = new EmpireFactory(configuration);
		return instance;
	}
	
	/**
	 * Creates and configures a new Empire JPA EntityManagerFactory to persist
	 * objects into the specified RDF repository.
	 * 
	 * @param repository
	 *            the RDF repository to persist objects into.
	 * 
	 * @return a configured Empire EntityManagerFactory.
	 */
	private EntityManagerFactory createEntityManagerFactory(URL repository) {
		// Build Empire configuration.
		EmpireConfiguration empireCfg = new EmpireConfiguration();
		// Configure target repository.
		Map<String, String> props = empireCfg.getGlobalConfig();
		String[] repo = repository.toString().split(REPOSITORY_URL_PARSER);
		props.put("factory", "sesame");
		props.put("url", repo[0]);
		props.put("repo", repo[1]);
		// Set persistent classes and associated (custom) annotation provider.
		empireCfg.setAnnotationProvider(CustomAnnotationProvider.class);
		props.put(CustomAnnotationProvider.ANNOTATED_CLASSES_PROP,
				join(this.getPersistentClasses(), ",").replace("class ", ""));
		// Initialize Empire.
		Empire.init(empireCfg, new OpenRdfEmpireModule());
		// Create Empire JPA persistence provider.
		PersistenceProvider provider = Empire.get().persistenceProvider();
		return provider.createEntityManagerFactory("",
				new HashMap<Object, Object>());
	}
	
	/**
	 * Returns the list of persistent classes to be handled by Empire JPA
	 * provider.
	 * 
	 * @return the list of persistent classes.
	 */
	@SuppressWarnings("unchecked")
	private Collection<Class<?>> getPersistentClasses() {
		Collection<Class<?>> classes = new LinkedList<Class<?>>();
		classes.addAll(Arrays.asList(ProjectImpl.class, CsvSourceImpl.class,
				RdfSourceImpl.class, DbSourceImpl.class, OntologyImpl.class));
		return classes;
	}
	
	public EntityManager	createEntityManager() {
		return this.emf.createEntityManager();
	}
}

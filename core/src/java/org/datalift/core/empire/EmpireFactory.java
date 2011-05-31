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
	
	private Collection<Class<?>> 	classes = null;
	
	private final static String REPOSITORY_URL_PARSER = "/repositories/";
	
	private EmpireFactory() {
		
	}
	
	public static EmpireFactory	getInstance() {
		if (instance == null)
			instance = new EmpireFactory();
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
	
	public void	init(Configuration configuration) {
		emf = this.createEntityManagerFactory(configuration
				.getInternalRepository().url);
		classes = new LinkedList<Class<?>>();
	}
	
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
				join(classes, ",").replace("class ", ""));
		// Initialize Empire.
		Empire.init(empireCfg, new OpenRdfEmpireModule());
		// Create Empire JPA persistence provider.
		PersistenceProvider provider = Empire.get().persistenceProvider();
		return provider.createEntityManagerFactory("",
				new HashMap<Object, Object>());
	}
	
	
	
	public void addPersistentClass(Class<?> cl) {
		classes.add(cl);
	}
	
	public void addPersistentClasses(Collection<Class<?>> classes) {
		this.classes.addAll(classes);
	}
	
	public EntityManager	createEntityManager() {
		return this.emf.createEntityManager();
	}
	
	public void shutdown() {
		this.emf.close();
		this.emf = null;
		instance = null;
	}
}

package org.datalift.lov;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.sparql.SparqlQueries;
import org.openrdf.query.algebra.evaluation.function.FunctionRegistry;
import org.openrdf.repository.Repository;

import com.mondeca.sesame.toolkit.functions.LevenshteinDistanceFunction;
import com.mondeca.sesame.toolkit.repository.FileDataInjector;
import com.mondeca.sesame.toolkit.repository.LocalMemoryRepositoryProvider;
import com.mondeca.sesame.toolkit.repository.RepositoryProviderException;
import com.mondeca.sesame.toolkit.repository.ThreadedProviderListener;

public class LovLoader {
	
	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------
	
	private final static Logger log = Logger.getLogger();

	/**
	 * LOV Data file.
	 */
	private File lovData;
	
	/**
	 * Repository in which lov data will be loaded.
	 */
	private Repository lovRepository;
	
	/**
	 * Latch to prevent from accessing non-loaded resources.
	 */
	private CountDownLatch loadingLatch;

	
	
	private SparqlQueries queries;

	private LabelFetcher labelFetcher;
	
	
	public LovLoader(Configuration configuration) {
		
		File privateStorage = Configuration.getDefault().getPrivateStorage();
		File lovPrivateStorage = new File(privateStorage,"lov");
		lovData = new File(lovPrivateStorage,"data");
		
		if(!lovData.exists()) {
			lovData.mkdirs();
		}
		
		List<String> files = Arrays.asList(lovData.list());
		// si le fichier lov_aggregator.rdf n'existe pas, on va le récupérer
		if( ! files.contains("lov_aggregator.rdf")) {
			// lancement du téléchargement dans un nouveau thread
			new Thread(new Runnable() {
				
				@Override
				public void run() {
					log.info("Downloading file from http://lov.okfn.org/dataset/lov/agg/lov_aggregator.rdf");
					try {
						
						long startTime = System.currentTimeMillis();
						
						// adresse pour récupérer le lov
						URL lovUrl = new URL("http://lov.okfn.org/dataset/lov/agg/lov_aggregator.rdf");
						
						// instanciation de buffers pour la lecture et l'ecriture du fichier
						BufferedReader in = new BufferedReader(new InputStreamReader(lovUrl.openStream()));
						FileWriter fw = new FileWriter(lovData.getAbsolutePath() + "/lov_aggregator.rdf");
						BufferedWriter out = new BufferedWriter(fw);

						// ecriture
						String inputLine;
						while ((inputLine = in.readLine()) != null) {
							// pretraitement pour encoder le debut des balises "xh:"
							// (sinon pb chargement repository)
							inputLine = inputLine.replaceAll("<xh:", "&lt;xh:");
							inputLine = inputLine.replaceAll("</xh:", "&lt;/xh:");
							out.write(inputLine + '\n');
						}

						// fermeture des streams
						out.flush();
						in.close();
						out.close();
						
						long estimatedTime = ((System.currentTimeMillis() - startTime) / 1000);
						
						
						log.info("File downloaded in about " + estimatedTime + " s.");
						
						// on peut charger le lov
						loadDataIntoRepository();

					} catch (IOException e) {
						log.error("Download error.");
						e.printStackTrace();
					}
					
				}

			}).start();
		}
		else {
			// on a déjà le fichier, on charge le lov
			loadDataIntoRepository();
		}
		
	}
	
	// load data of lov into a dedicated named graph
	private void loadDataIntoRepository() {

		try {
			LocalMemoryRepositoryProvider p = new LocalMemoryRepositoryProvider();
			p.setRdfsWithDirectTypeAware(true);
			System.out.println("LovModule repository");
			this.loadingLatch = new CountDownLatch(1);
			p.addListener(new ThreadedProviderListener(new FileDataInjector(lovData.getAbsolutePath()), this.loadingLatch));
			p.init();

			this.lovRepository = p.getRepository();
		} catch (RepositoryProviderException e) {
			e.printStackTrace();
		}

		this.labelFetcher = new LabelFetcher(this.queries, this.lovRepository);
		FunctionRegistry.getInstance().add(new LevenshteinDistanceFunction());

	}


}

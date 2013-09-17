package org.datalift.lov.service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.openrdf.repository.Repository;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.repository.sail.SailRepository;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.openrdf.sail.memory.MemoryStore;

/**
 * Service implementation that performs local request to get its results.
 * Handles the lov_aggregator download and updates.
 * 
 * @author freddy
 *
 */
public class OfflineLovService extends LovService {
	
	/** Private Storage file */
	private File privateStorage;
	
	/** LOV Data file */
	private File lovData;
	
	/** Repository in which lov data will be loaded */
	private Repository lovRepository;
	
	
	public OfflineLovService(File privateStorage) {
		this.privateStorage = privateStorage;
		downloadAggregator();
	}

	
	@Override
	public String search(SearchQueryParam params) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String check(CheckQueryParam params) {
		// TODO Auto-generated method stub
		return null;
	}
	
	//-------------------------------------------------------------------------
	
	private void downloadAggregator() {

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
		else { // on a déjà le fichier, on charge le lov
			// TODO : vérifier si une mise à jour est disponible
			loadDataIntoRepository();
		}
	}
	
	// load data of lov into a dedicated named graph
	private void loadDataIntoRepository() {
		
		// TODO vérifier la vitesse de chargement des données (mettre dans un autre thread ?)
		// => il faudra probablement gérer l'accès au repository avec un semaphore
			
		lovRepository = new SailRepository(new MemoryStore());
		try {
			RepositoryConnection conn = lovRepository.getConnection();
			conn.add(new File(lovData.getAbsolutePath() + "/lov_aggregator.rdf"), null, RDFFormat.RDFXML);
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//			try {
//				LocalMemoryRepositoryProvider p = new LocalMemoryRepositoryProvider();
//				p.setRdfsWithDirectTypeAware(true);
//				System.out.println("LovModule repository");
//				this.loadingLatch = new CountDownLatch(1);
//				p.addListener(new ThreadedProviderListener(new FileDataInjector(lovData.getAbsolutePath()), this.loadingLatch));
//				p.init();
//
//				this.lovRepository = p.getRepository();
//			} catch (RepositoryProviderException e) {
//				e.printStackTrace();
//			}
//
//			this.labelFetcher = new LabelFetcher(this.queries, this.lovRepository);
//			FunctionRegistry.getInstance().add(new LevenshteinDistanceFunction());
		catch (RDFParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
			
	}

}

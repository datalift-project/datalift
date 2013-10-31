package org.datalift.lov.service;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.lov.local.LovLocalService;
import org.datalift.lov.local.LovLocalVocabularyService;
import org.datalift.lov.local.LovUtil;
import org.datalift.lov.local.objects.vocab.VocabsDictionaryItem;
import org.openrdf.repository.RepositoryConnection;
import org.openrdf.repository.RepositoryException;
import org.openrdf.rio.RDFFormat;
import org.openrdf.rio.RDFParseException;
import org.semanticweb.yars.nx.Node;
import org.semanticweb.yars.nx.Triple;
import org.semanticweb.yars.nx.parser.NxParser;

/**
 * Service implementation that performs local request to get its results.
 * Handles the lov_aggregator download and updates.
 * 
 * @author freddy
 * 
 */
public class OfflineLovService extends LovService {

	private final static Logger log = Logger.getLogger(OfflineLovService.class);
	private final static String LOV_CONTEXT = "http://lov.okfn.org/datalift/local/lov_aggregator";
	private final static String DEFAULT_JSON = "{" + "\"count\": 0,"
			+ "\"offset\": 0," + "\"limit\": 15," + "\"search_query\": \"\","
			+ "\"search_type\": null," + "\"search_vocSpace\": null,"
			+ "\"search_voc\": null," + "\"facet_vocSpaces\": null,"
			+ "\"facet_types\": null," + "\"params\": null,"
			+ "\"results\": []" + "}";

	private final static String N3_AGGREGATOR = "lov_aggregator.n3";
	private final static String NQ_AGGREGATOR = "lov_aggregator.nq";
	private final static String ZIPPED_AGGREGATOR = NQ_AGGREGATOR + ".zip";

	/** Datalift configuration */
	private Configuration configuration;

	/** LOV Data file */
	private File lovData;
	
	/** Aggregator download state */
	private boolean aggregatorDownloaded;

	/** Triple store loading state */
	private boolean dataLoading;
	
	/** Aggregator in tripleStore */
	private boolean dataLoaded;

	/** Search service */
	private LovLocalService localService;
	
	/** Vocabs service */
	private LovLocalVocabularyService vocabsService;

	public OfflineLovService(Configuration configuration) {
		this.configuration = configuration;
		localService = new LovLocalService(configuration);
		vocabsService = new LovLocalVocabularyService(configuration);
		downloadAggregator();
	}

	@Override
	public String search(SearchQueryParam params) {
		if (params.getQuery().trim().isEmpty()) {
			return DEFAULT_JSON;
		}

		String type = params.getType();
		if (type.trim().isEmpty()) {
			type = null;
		}

		String vocSpace = params.getVocSpace();
		if (vocSpace.trim().isEmpty()) {
			vocSpace = null;
		}

		String voc = params.getVoc();
		if (voc.trim().isEmpty()) {
			voc = null;
		}

		return localService.search(params.getQuery(), params.getOffset(),
				params.getLimit(), type, vocSpace, voc).toJSON();
	}

	@Override
	public String check(CheckQueryParam params) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String vocabs() {
		return vocabsService.getVocabularies().toJSON();
	}
	
	public String vocabWithUri(String uri) {
		VocabsDictionaryItem item = vocabsService.getVocabularyWithUri(uri);
		if (item == null) {
			return "{}";
		}
		else {
			return item.toJSON();
		}
	}
	
	@Override
	public void checkLovData() {
		while( dataLoading ) {
			try {
				Thread.sleep(800);

			} catch (InterruptedException e) {
				log.warn("Interrupted thread.");
			}
		}
		loadDataIntoRepository();
	}
	
	public boolean isDataLoaded() {
		return dataLoaded;
	}

	// -------------------------------------------------------------------------

	private void downloadAggregator() {

		lovData = new File(configuration.getTempStorage(), "lov/data");
		if (!lovData.exists()) {
			lovData.mkdirs();
		}
		aggregatorDownloaded = false;

		List<String> files = Arrays.asList(lovData.list());
		// si le fichier lov_aggregator.rdf n'existe pas, on va le récupérer
		if ( ! files.contains("lov_aggregator.rdf")) {
			// lancement du téléchargement dans un nouveau thread
			new Thread(new Runnable() {

				@Override
				public void run() {
					log.info("Downloading file from http://lov.okfn.org/dataset/lov/agg/"
							+ ZIPPED_AGGREGATOR);
					try {

						long startTime = System.currentTimeMillis();

						// adresse pour récupérer le lov
						URL lovUrl = new URL(
								"http://lov.okfn.org/dataset/lov/agg/"
										+ ZIPPED_AGGREGATOR);

//						// instanciation de buffers pour la lecture et
//						// l'ecriture du fichier
//						BufferedReader in = new BufferedReader(
//								new InputStreamReader(lovUrl.openStream()));
//						FileWriter fw = new FileWriter(lovData
//								.getAbsolutePath() + "/" + ZIPPED_AGGREGATOR);
//						BufferedWriter out = new BufferedWriter(fw);
//
//						// ecriture
//						String inputLine;
//						while ((inputLine = in.readLine()) != null) {
//							// pretraitement pour encoder le debut des balises
//							// "xh:"
//							// (sinon pb chargement repository)
//							// inputLine = inputLine.replaceAll("<xh:",
//							// "&lt;xh:");
//							// inputLine = inputLine.replaceAll("</xh:",
//							// "&lt;/xh:");
//							out.write(inputLine + '\n');
//						}
						
				        URLConnection conn = lovUrl.openConnection();
				        conn.setDoOutput(true);
				        conn.setDoInput(true);
				        conn.setRequestProperty("content-type", "binary/data");
				        InputStream in = conn.getInputStream();
				        FileOutputStream out = new FileOutputStream(lovData
								.getAbsolutePath() + "/" + ZIPPED_AGGREGATOR);

				        byte[] b = new byte[1024];
				        int count;

				        while ((count = in.read(b)) >= 0) {
				            out.write(b, 0, count);
				        }
						// fermeture des streams
						out.flush();
						in.close();
						out.close();

						long estimatedTime = ((System.currentTimeMillis() - startTime) / 1000);

						log.info("File downloaded in about {} s.",
								estimatedTime);
						extractZippedAggregator();
						convertAggragator();
						new File(lovData
								.getAbsolutePath() + "/" + NQ_AGGREGATOR)
								.delete();
						aggregatorDownloaded = true;
						if ( ! dataLoading) {
							loadDataIntoRepository();
						}

					} catch (IOException e) {
						log.error("Download error.");
						e.printStackTrace();
					}

				}

			}).start();
		} else { // on a déjà le fichier, on charge le lov
					// TODO : vérifier si une mise à jour est disponible
			log.info("No need for download. LovAggregator is here !");
			aggregatorDownloaded = true;
			loadDataIntoRepository();
		}
	}

	// load data of lov into a dedicated named graph
	private void loadDataIntoRepository() {

		// TODO vérifier la vitesse de chargement des données (mettre dans un
		// autre thread ?)
		// => il faudra probablement gérer l'accès au repository avec un
		// semaphore

		if (!aggregatorDownloaded) {
			return;
		}

		if (dataLoaded) {
			return;
		}

		URI lovContextURI;
		RepositoryConnection conn = null;

		try {
			dataLoading = true;
			lovContextURI = new URI(LOV_CONTEXT);

			log.trace("Opening connection to internal repository.");
			conn = this.configuration.getInternalRepository().newConnection();
			if (conn.isOpen()) {
				log.trace("Connection is open.");
			}

			org.openrdf.model.URI ctx = null;
			ctx = conn.getValueFactory().createURI(lovContextURI.toString());
			log.debug("Checking context size for {}.", ctx.toString());
			long repositorySize = 0;
			repositorySize = conn.size(ctx);
			log.info("Repository size for LOV context : {}.", repositorySize);

			if (repositorySize == 0) {
				// log.info("Clearing context.");
				// conn.clear(ctx);
				
				File lov = new File(lovData.getAbsolutePath() + "/"
						+ N3_AGGREGATOR);

				log.info("Loading {} into repository.", lov.getAbsolutePath());
				long startTime = System.currentTimeMillis();
				conn.add(lov, null, RDFFormat.N3, ctx);
				long estimatedTime = ((System.currentTimeMillis() - startTime) / 1000);
				log.info(
						"Loading has been done in {} s. Offline service is set and ready.",
						estimatedTime);
			}
			dataLoaded = true;

		} catch (URISyntaxException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RepositoryException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (RDFParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			LovUtil.closeQuietly(conn);
			dataLoading = false;
		}
	}

	private boolean extractZippedAggregator() {

		log.trace("Extracting aggregator...");
		
		byte[] buffer = new byte[1024];

		try {

			String output = lovData.getAbsolutePath() + "/" + NQ_AGGREGATOR;

			// get the zip file content
			ZipInputStream zis = new ZipInputStream(new FileInputStream(
					lovData.getAbsolutePath() + "/" + ZIPPED_AGGREGATOR));
			// get the zipped file list entry
			ZipEntry ze = zis.getNextEntry();

			while (ze != null) {

				File newFile = new File(output);

				// folders should have been created when downloading
				FileOutputStream fos = new FileOutputStream(newFile);

				int len;
				while ((len = zis.read(buffer)) > 0) {
					fos.write(buffer, 0, len);
				}

				fos.close();
				ze = zis.getNextEntry();
			}

			zis.closeEntry();
			zis.close();
			
			log.info("Aggregator unzipped.");
			
			return true;

		} catch (IOException e) {
//			e.printStackTrace();
			log.error("Aggregator extraction failed.");
		}
		
		return false;

	}
	
	private void convertAggragator() {
		
		log.trace("Converting {} to {}.", NQ_AGGREGATOR, N3_AGGREGATOR);

		try {
			NxParser nxp = new NxParser(new FileInputStream(lovData.getAbsolutePath() + 
					"/" + NQ_AGGREGATOR), false);
			
			FileWriter fw = new FileWriter(lovData.getAbsolutePath() + 
					"/" + N3_AGGREGATOR);
			BufferedWriter out = new BufferedWriter(fw);
			
			int notQuad = 0;
			int notEvenTriple = 0;
			while (nxp.hasNext()) {
				Node[] ns = nxp.next();
				if (ns.length == 4) {
					out.write(new Triple(ns[0], ns[1], ns[2]).toN3());
				}
				else {
					++notQuad;
					if (ns.length == 3) {
						out.write(new Triple(ns[0], ns[1], ns[2]).toN3());
					}
					else {
						++notEvenTriple;
					}
				}
				out.newLine();
			}
			log.debug("notQuad : {} - notEvenTriple : {}", notQuad, notEvenTriple);
			out.flush();
			out.close();

			log.info("Conversion done.");
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		} 
	}

}

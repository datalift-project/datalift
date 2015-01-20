package org.datalift.lov.service;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import org.datalift.fwk.Configuration;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.rdf.BatchStatementAppender;
import org.datalift.fwk.rdf.RdfException;
import org.datalift.fwk.rdf.RdfFormat;
import org.datalift.fwk.rdf.RdfUtils;
import org.datalift.fwk.rdf.Repository;
import org.datalift.fwk.util.Env;
import org.datalift.fwk.util.io.FileUtils;
import org.datalift.fwk.util.web.Charsets;
import org.datalift.lov.local.LovLocalService;
import org.datalift.lov.local.LovLocalVocabularyService;
import org.datalift.lov.local.LovUtil;
import org.datalift.lov.local.objects.vocab.VocabsDictionaryItem;

import static org.datalift.fwk.util.PrimitiveUtils.wrap;

import org.openrdf.repository.RepositoryConnection;
import org.openrdf.rio.ParserConfig;
import org.openrdf.rio.RDFParser;
import org.openrdf.rio.RDFParser.DatatypeHandling;
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

	private final static Logger log = Logger.getLogger();
	private final static String LOV_CONTEXT = "http://lov.okfn.org/datalift/local/lov_aggregator";
	private final static String DEFAULT_JSON = "{" + "\"count\": 0,"
			+ "\"offset\": 0," + "\"limit\": 15," + "\"search_query\": \"\","
			+ "\"search_type\": null," + "\"search_vocSpace\": null,"
			+ "\"search_voc\": null," + "\"facet_vocSpaces\": null,"
			+ "\"facet_types\": null," + "\"params\": null,"
			+ "\"results\": []" + "}";

	// private final static String NQ_AGGREGATOR = "lov_aggregator.nq";
	// private final static String ZIPPED_AGGREGATOR = NQ_AGGREGATOR + ".zip";

	public final static String LOV_SRC_URL_PROPERTY = "lov.data.url";
	public final static String LOV_DATA_URL =
	            // "http://lov.okfn.org/dataset/lov/agg/" + ZIPPED_AGGREGATOR;
	            "http://lov.okfn.org/lov.nq.gz";

	private final static String BACKUP_FILE_EXT = ".bak";

	/** Aggregator download state */
	private boolean aggregatorDownloaded = false;
	/** Triple store loading state */
	private boolean dataLoading = false;
	/** Aggregator in tripleStore */
	private boolean dataLoaded = false;
	/** If an update is required*/
	private boolean updating = false;

	/** Last LOV update */
	private Date lastLovUpdate = null;

	/** Search service */
	private LovLocalService localService;
	/** Vocabs service */
	private LovLocalVocabularyService vocabsService;

	/** LOV data source URL. */
	private final URL lovSourceUrl;
	/** LOV data cache directory. */
	private File lovDataDir;
	/** The local cache of LOV data. */
	private final File lovDataFile;

	public OfflineLovService(Configuration configuration) {
		localService = new LovLocalService(configuration);
		vocabsService = new LovLocalVocabularyService(configuration);
		try {
		    lovSourceUrl = new URL(configuration.getProperty(
		                        LOV_SRC_URL_PROPERTY, LOV_DATA_URL));
		}
		catch (Exception e) {
		    throw new RuntimeException(e);
		}
		lovDataDir = new File(configuration.getTempStorage(), "lov/data");
		if (!lovDataDir.exists()) {
		    lovDataDir.mkdirs();
		}
		String lovSrcFile = new File(lovSourceUrl.getPath()).getName();
		lovDataFile = new File(lovDataDir, lovSrcFile);

		ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(new ThreadFactory() {
		    @Override
		    public Thread newThread(Runnable runnable) {
		        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
                       thread.setDaemon(true);
                       thread.setName("Lov initialization thread");
                       return thread;
		    }
		});

		Runnable runnable = new Runnable() {
		    @Override
		    public void run() {
		        if (downloadAggregator()) {
		            loadDataIntoRepository();
		        }
		    }
		};

		executor.schedule(runnable, 10, TimeUnit.SECONDS);
		executor.shutdown();
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
		} else {
			return item.toJSON();
		}
	}

	@Override
	public void checkLovData() {
		while (dataLoading) {
			try {
				Thread.sleep(800);

			} catch (InterruptedException e) {
				log.warn("Interrupted thread.");
				break;
			}
		}
		loadDataIntoRepository();
	}

	public boolean isDataLoaded() {
		return dataLoaded;
	}

	@Override
	public String vocSpaces() {
		return LovUtil.toJSON(vocabsService.getVocabularySpaces(), true);
	}

	public String getLastLovUpdate() {
		if (lastLovUpdate == null) {
			return "";
		}
		SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");
		return format.format(lastLovUpdate);
	}

	public void update() {
		File bakLov = new File(lovDataDir,
		                       lovDataFile.getName() + BACKUP_FILE_EXT);
		if (bakLov.exists()) {
			bakLov.delete();
		}
		if (lovDataFile.exists()) {
			lovDataFile.renameTo(bakLov);
		}
		String lovSrcFile = lovDataFile.getName();
		for (File f : lovDataDir.listFiles()) {
		    if (! f.getName().startsWith(lovSrcFile)) {
		        f.delete();
		    }
		}
		updating = true;
		dataLoaded = false;

		downloadAggregator();
		loadDataIntoRepository();
		updating = false;
	}

	// -------------------------------------------------------------------------

	private boolean downloadAggregator() {
		boolean ret = false;
		aggregatorDownloaded = false;

		if (! lovDataFile.exists()) {
			// LOV data file not found. => Download it.
			ExecutorService executor = Executors.newSingleThreadExecutor(new ThreadFactory() {
				@Override
				public Thread newThread(Runnable runnable) {
					Thread thread = Executors.defaultThreadFactory().newThread(runnable);
					thread.setDaemon(true);
					thread.setName("Lov downloader thread");
					return thread;
				}
			});

			Callable<Boolean> callable = new Callable<Boolean>() {
				@Override
				public Boolean call() throws Exception {
					Boolean success = Boolean.FALSE;
					log.debug("Downloading LOV data from \"{}\"...", lovSourceUrl);
					try {
						long startTime = System.currentTimeMillis();
						FileUtils.save(lovSourceUrl, lovDataFile);
						double estimatedTime = ((System.currentTimeMillis() - startTime) / 1000.0);
						log.info("Downloaded LOV data from \"{}\" in {} seconds",
						         lovSourceUrl, wrap(estimatedTime));
						aggregatorDownloaded = true;
						success = Boolean.TRUE;
					} catch (IOException e) {
						log.error("Failed downloading LOV data: {}", e, e.getMessage());
					}
					return success;
				}
			};

			Future<Boolean> future = executor.submit(callable);
			try {
			    ret = future.get().booleanValue();
			}
			catch (InterruptedException e) {
			    log.warn("LOV data download has been interrupted.");
			}
			catch (ExecutionException e) {
			    log.error("LOV data download failure: {}", e.getMessage());
			}
			finally {
			    executor.shutdown();
			}
		}
		else {
			// LOV data file already present.
			// TODO : Check if updated version is available.
			lastLovUpdate = new Date(lovDataFile.lastModified());
			log.info("No download needed: LOV data already present");
			ret = true;
			aggregatorDownloaded = true;
		}
		return ret;
	}

	// load data of LOV into a dedicated named graph
	private void loadDataIntoRepository() throws RuntimeException {
		// TODO vérifier la vitesse de chargement des données (mettre dans un
		// autre thread ?)
		// => il faudra probablement gérer l'accès au repository avec un
		// semaphore
		if ((! aggregatorDownloaded) || dataLoaded) {
			return;
		}
		dataLoading = true;

		File inFile = lovDataFile;
		boolean deleteInFile = false;
		RepositoryConnection cnx = null;
		try {
			Repository r = Configuration.getDefault()
			                            .getInternalRepository();
			cnx = r.newConnection();

			URI lovCtxUri = URI.create(LOV_CONTEXT);
			if (updating) {
				RdfUtils.clearGraph(r, lovCtxUri);
			}
			org.openrdf.model.URI ctx =
			        cnx.getValueFactory().createURI(LOV_CONTEXT);
			long repositorySize = cnx.size(ctx);
			if (repositorySize == 0L) {
				log.debug("Loading LOV data ({}) into repository...", lovDataFile);
				long startTime = System.currentTimeMillis();

				RdfFormat fmt = RdfUtils.guessRdfFormatFromExtension(lovDataFile.getName());
				if (fmt == RdfFormat.NQUADS) {
				    // Sesame 2.6.x does not support N-Quads.
				    // => Translate N-Quads into N3.
				    inFile = this.convertNQuads2N3();
				    deleteInFile = true;
				    fmt = RdfFormat.N3;
				}
				// Ignore RDF parse errors (such as ill-formatted
				// dates that sometimes occur in LOV data).
				RDFParser parser = fmt.newParser();
				parser.setParserConfig(new ParserConfig(
                                        true,       // Assume data are valid.
                                        false,      // Report all errors.
                                        false,      // Don't preserve BNode ids.
                                        DatatypeHandling.VERIFY));
				parser.setStopAtFirstError(false);
				parser.setRDFHandler(new BatchStatementAppender(cnx, ctx));
				parser.parse(FileUtils.getInputStream(inFile), "");

				double loadTime = (System.currentTimeMillis() - startTime) / 1000.0;
				log.info("LOV data successfully loaded in {} seconds. LOV offline service is ready.",
				         wrap(loadTime));
				lastLovUpdate = new Date();
			}
		}
		catch (RdfException e) {
			log.error("Error parsing LOV data: \"{}\". Please contact a LOV administrator.", e, e.getMessage());
		}
		catch (Exception e) {
			log.error("Error processing LOV data: \"{}\".", e, e.getMessage());
			throw new RuntimeException(e);
		}
		finally {
			Repository.closeQuietly(cnx);
			if (deleteInFile) {
			    inFile.delete();
			}
			dataLoading = false;
		}
		dataLoaded = true;
	}

	private File convertNQuads2N3() throws IOException {
		File tmpFile = File.createTempFile("lov-", ".n3");
		tmpFile.deleteOnExit();

		InputStream in = null;
		Writer out = null;
		try {
			final int chunkSize = Env.getFileBufferSize();
			out = new OutputStreamWriter(
			        new BufferedOutputStream(
			            new FileOutputStream(tmpFile), chunkSize),
			        Charsets.UTF_8);

			in = FileUtils.getInputStream(lovDataFile);
			NxParser nxp = new NxParser(in, false);

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
				out.write('\n');
			}
			log.trace("notQuad: {}, notEvenTriple: {}",
			                    wrap(notQuad), wrap(notEvenTriple));
			out.flush();
			out.close();
		}
		finally {
			FileUtils.closeQuietly(in);
			FileUtils.closeQuietly(out);
		}
		return tmpFile;
	}
}

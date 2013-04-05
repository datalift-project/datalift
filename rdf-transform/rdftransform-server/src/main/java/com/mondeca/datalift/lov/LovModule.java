package com.mondeca.datalift.lov;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.datalift.fwk.BaseModule;
import org.datalift.fwk.Configuration;
import org.datalift.fwk.MediaTypes;
import org.datalift.fwk.log.Logger;
import org.datalift.fwk.project.Project;
import org.datalift.fwk.project.ProjectModule;
import org.datalift.fwk.sparql.SparqlQueries;
import org.openrdf.model.Literal;
import org.openrdf.model.Resource;
import org.openrdf.model.Value;
import org.openrdf.query.Binding;
import org.openrdf.query.BindingSet;
import org.openrdf.query.TupleQueryResultHandlerException;
import org.openrdf.query.algebra.evaluation.function.FunctionRegistry;
import org.openrdf.repository.Repository;

import com.mondeca.datalift.rdf2rdf.LabelFetcher;
import com.mondeca.datalift.rdf2rdf.RDFTransformService;
import com.mondeca.sesame.toolkit.functions.LevenshteinDistanceFunction;
import com.mondeca.sesame.toolkit.query.SPARQLExecutionException;
import com.mondeca.sesame.toolkit.query.SelectSPARQLHelperBase;
import com.mondeca.sesame.toolkit.query.SesameSPARQLExecuter;
import com.mondeca.sesame.toolkit.query.builder.PagingSPARQLQueryBuilder;
import com.mondeca.sesame.toolkit.query.builder.SPARQLQueryBuilderIfc;
import com.mondeca.sesame.toolkit.query.builder.StringSPARQLQueryBuilder;
import com.mondeca.sesame.toolkit.repository.FileDataInjector;
import com.mondeca.sesame.toolkit.repository.LocalMemoryRepositoryProvider;
import com.mondeca.sesame.toolkit.repository.RepositoryProviderException;
import com.mondeca.sesame.toolkit.repository.ThreadedProviderListener;

@Path(LovModule.MODULE_NAME)
public class LovModule extends BaseModule implements ProjectModule {

	//-------------------------------------------------------------------------
	// Constants
	//-------------------------------------------------------------------------

	/** The name of this module in the DataLift configuration. */
	public final static String MODULE_NAME = "lov";

	//-------------------------------------------------------------------------
	// Class members
	//-------------------------------------------------------------------------

	private final static Logger log = Logger.getLogger();
	
	/** The SPARQL queries used by this module */
	private SparqlQueries queries;
	
	/**
	 * LOV Data file
	 */
	private File lovData;

	/**
	 * Repository in which lov data will be loaded
	 */
	private Repository lovRepository;

	private CountDownLatch loadingLatch;

	protected LabelFetcher labelFetcher;

	//-------------------------------------------------------------------------
	// Constructors
	//-------------------------------------------------------------------------

	/** Default constructor. */
	public LovModule() {
		super(MODULE_NAME);
		this.queries = new SparqlQueries(this);
	}

	//-------------------------------------------------------------------------
	// Module contract support
	//-------------------------------------------------------------------------

	/** {@inheritDoc} */
	@Override
	public void postInit(Configuration configuration) {
		super.postInit(configuration);
		System.out.println("LovModule post init phase");

		File privateStorage = Configuration.getDefault().getTempStorage();
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
		else // on a déjà le fichier, on charge le lov
			loadDataIntoRepository();
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


	//-------------------------------------------------------------------------
	// Web services
	//-------------------------------------------------------------------------

	@Override
	public UriDesc canHandle(Project p) {
		return null;
	}

	/**
	 * URLs d'appel :
	 * 
	 * http://localhost:8080/datalift/lov/vocabElements?query=latitude&type=http%3A%2F%2Fwww.w3.org%2F1999%2F02%2F22-rdf-syntax-ns%23Property
	 * http://localhost:8080/datalift/lov/vocabElements?query=latitude&type=http%3A%2F%2Fwww.w3.org%2F2002%2F07%2Fowl%23Class
	 * 
	 * @param callback
	 * @param query
	 * @param typeURI
	 * @return
	 */
	@GET
	@Path("vocabElements")
	@Produces(MediaTypes.APPLICATION_JSON)
	public Response getVocabElements(
			@QueryParam("callback") String callback,
			@QueryParam("query") String query,
			@QueryParam("type") String typeURI,
			@QueryParam("offset") String offset,
			@QueryParam("limit") String limit
			) {
		try {
			//block until the loader has set the latch to 0:
			long start = System.currentTimeMillis();
			System.out.println("Waiting for repository loading...");
			loadingLatch.await();
			System.out.println("Waited for "+(System.currentTimeMillis() - start));
		}
		catch (InterruptedException ex){
			System.err.println(ex.toString());
			Thread.currentThread().interrupt();
		}

		System.out.println("Searching on LOV with query '"+query+"' and typeURI '"+typeURI+"' with offset : "+offset+" and limit "+limit);
		SesameSPARQLExecuter executer = new SesameSPARQLExecuter(this.lovRepository);

		LovSearchHelper helper = new LovSearchHelper(
				this.queries,
				query,
				typeURI,
				this.lovRepository,
				this.labelFetcher,
				(offset == null)?null:Integer.parseInt(offset),
						(limit == null)?null:Integer.parseInt(limit)
				);

		try {
			executer.executeSelect(helper);
		} catch (SPARQLExecutionException e) {
			throw new LovModuleException(e);
		}
		JSONVocabQueryResult r = new JSONVocabQueryResult(helper.getJsonVocabElements(), helper.getJsonVocabs());

		return RDFTransformService.serializeJSON(r, callback, null);
	}

	class LovSearchHelper extends SelectSPARQLHelperBase {

		protected LabelFetcher labelFetcher;
		protected SparqlQueries queries;
		protected String searchKey;
		protected String type;
		protected Repository repository;
		protected Integer offset;
		protected Integer limit;

		protected List<JSONVocabElement> jsonVocabElements = new ArrayList<JSONVocabElement>();
		protected List<JSONVocab> jsonVocabs = new ArrayList<JSONVocab>();

		public LovSearchHelper(
				SparqlQueries queries,
				String searchKey,
				String type,
				Repository repository,
				LabelFetcher labelFetcher,
				Integer offset,
				Integer limit
				) {
			this.queries = queries;
			this.labelFetcher = labelFetcher;
			this.searchKey = searchKey;
			this.repository = repository;
			this.type = type;
			this.offset = offset;
			this.limit = limit;
		}

		@Override
		public String getSPARQL() {
			SPARQLQueryBuilderIfc builder = new StringSPARQLQueryBuilder(this.queries.get("lov.search"));
			if(this.offset != null || this.limit != null) {
				builder = new PagingSPARQLQueryBuilder(
						builder,
						this.offset,
						this.limit
						);
			}
			return builder.getSPARQL();
		}

		@Override
		public Map<String, Value> getBindings() {
			return new HashMap<String, Value>() {{
				if(type != null) { 
					put("type",repository.getValueFactory().createURI(type));
				}
				put("key",repository.getValueFactory().createLiteral(searchKey.toLowerCase()));
			}};
		}

		// ?uri ?p ?label ?directType ?vocabulary
		@Override
		public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
			String aLine = "";
			for (Binding aBinding : bs) {
				aLine += aBinding.getName()+":"+aBinding.getValue().stringValue()+"\t";
			}
			System.out.println(aLine);

			String uri = ((Resource)bs.getValue("uri")).stringValue();

			JSONVocabElement jsonVocabElement = new JSONVocabElement(uri);

			if(!this.jsonVocabElements.contains(jsonVocabElement)) {			
				jsonVocabElement.setVocabElementVocabURI(((Resource)bs.getValue("vocabulary")).stringValue());
				jsonVocabElement.setVocabElementType(((Resource)bs.getValue("directType")).stringValue());
				jsonVocabElement.setVocabElementName(LabelFetcher.concat(this.labelFetcher.getLabels(uri)));
				Literal score = (Literal)bs.getValue("inverseScore");
				if(score != null) {
					jsonVocabElement.setVocabElementScore(score.stringValue());
				}
				this.jsonVocabElements.add(jsonVocabElement);
			}
		}

		@Override
		public void endQueryResult() throws TupleQueryResultHandlerException {
			for (JSONVocabElement aVocabElement : this.jsonVocabElements) {
				if(!jsonVocabs.contains(new JSONVocab(aVocabElement.getVocabElementVocabURI()))) {
					try {
						LovVocabularyReader reader = new LovVocabularyReader(this.queries, aVocabElement.getVocabElementVocabURI(), this.repository, labelFetcher);
						new SesameSPARQLExecuter(this.repository).executeSelect(reader);
						this.jsonVocabs.add(reader.getJsonVocab());
					} catch (SPARQLExecutionException e) {
						e.printStackTrace();
					}
				}
			}
		}

		public List<JSONVocabElement> getJsonVocabElements() {
			return jsonVocabElements;
		}

		public List<JSONVocab> getJsonVocabs() {
			return jsonVocabs;
		}		
	}

	class LovVocabularyReader extends SelectSPARQLHelperBase {

		protected LabelFetcher labelFetcher;
		protected String sparql;
		protected String vocabUri;
		protected Repository repository;

		protected JSONVocab jsonVocab;

		public LovVocabularyReader(
				SparqlQueries queries,
				String vocabUri,
				Repository repository,
				LabelFetcher labelFetcher
				) {
			this.sparql = queries.get("lov.vocabulary");
			this.labelFetcher = labelFetcher;
			this.vocabUri = vocabUri;
			this.repository = repository;
		}

		@Override
		public String getSPARQL() {
			return this.sparql;
		}

		@Override
		public Map<String, Value> getBindings() {
			return new HashMap<String, Value>() {{
				put("vocabURI",repository.getValueFactory().createURI(vocabUri));
			}};
		}

		// SELECT ?vocabPrefix ?vocabURI ?nsUri
		@Override
		public void handleSolution(BindingSet bs) throws TupleQueryResultHandlerException {
			String uri = ((Resource)bs.getValue("vocabURI")).stringValue();
			Literal nsUri = (Literal)bs.getValue("nsUri");
			Literal vocabPrefix = (Literal)bs.getValue("vocabPrefix");
			Literal vocabName = (Literal)bs.getValue("vocabName");

			this.jsonVocab = new JSONVocab(uri);
			this.jsonVocab.setVocabNsp((nsUri == null)?null:nsUri.stringValue());
			this.jsonVocab.setVocabPrefix((vocabPrefix == null)?null:vocabPrefix.stringValue());
			this.jsonVocab.setVocabName((vocabName == null)?null:vocabName.stringValue());
		}

		public JSONVocab getJsonVocab() {
			return jsonVocab;
		}		
	}

}

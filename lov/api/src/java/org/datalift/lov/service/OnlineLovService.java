package org.datalift.lov.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Service implementation that performs HTTP request to get its results.
 * 
 * @author freddy
 *
 */
public class OnlineLovService extends LovService {
	
	private final static String QUERY_BASE_URL = "http://lov.okfn.org/dataset/lov/api/v1/";
	private final static String SEARCH = "search";
	private final static String CHECK = "check";
	private final static String VOCAB = "vocabs";
	private final static String DEFAULT_JSON =  "{" +
											    "\"count\": 0," +
											    "\"offset\": 0," +
											    "\"limit\": 15," +
											    "\"search_query\": \"\"," +
											    "\"search_type\": null," +
											    "\"search_vocSpace\": null," +
											    "\"search_voc\": null," +
												"\"facet_vocSpaces\": null," +
											    "\"facet_types\": null," +
											    "\"params\": null," +
											    "\"results\": []" +
												"}";

	@Override
	public String search(SearchQueryParam params) {
		if (params.getQuery().trim().isEmpty()) {
			return DEFAULT_JSON;
		}
		
		return getLovJson(SEARCH, params);
	}

	@Override
	public String check(CheckQueryParam params) {
		return getLovJson(CHECK, params);
	}
	
	@Override
	public String vocabs() {
		return getLovJson(VOCAB, new NoQueryParam());
	}
	
	private String getLovJson(String service, LovQueryParam params) {
		String line = "";
		StringBuilder builder = new StringBuilder();
		HttpURLConnection connection = null;
		
		try {
			URL url = new URL(QUERY_BASE_URL + service + params.getQueryParameters());
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			
			if (connection.getResponseCode() != 200) {
				log.error("HTTP request error. Response code is {}", connection.getResponseCode());
			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			while ((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
			reader.close();
			
		}
		catch (MalformedURLException e) {
			log.error("Not an URL");
		}
		catch (IOException e) {
			log.error(e.toString());
		}
		finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
		
		return builder.toString();
	}

	@Override
	public void checkLovData() {
		// TODO Auto-generated method stub
		
	}
		
}

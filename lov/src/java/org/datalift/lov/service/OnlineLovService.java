package org.datalift.lov.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;


public class OnlineLovService extends LovService {
	
	private final static String QUERY_BASE_URL = "http://lov.okfn.org/dataset/lov/api/v1/";
	private final static String SEARCH = "search";

	@Override
	public String search(SearchQueryParam params) {
		String line = "";
		StringBuilder builder = new StringBuilder();
		HttpURLConnection connection = null;
		
		try {
			
			URL url = new URL(QUERY_BASE_URL + SEARCH + params.toString());
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("GET");
			connection.setRequestProperty("Accept", "application/json");
			
			if(connection.getResponseCode() != 200) {
//				log.error("HTTP request error.");
			}
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					connection.getInputStream()));
			while((line = reader.readLine()) != null) {
				builder.append(line);
			}
			
			reader.close();
			
		}
		catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		finally {
			if(connection != null) {
				connection.disconnect();
			}
		}
		
		
		return builder.toString();
	}

}

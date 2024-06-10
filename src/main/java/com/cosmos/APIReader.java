package com.cosmos;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;

public class APIReader {

    public JSONObject getJsonDataFromAPI() throws IOException {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        String url = "https://cosmos-odyssey.azurewebsites.net/api/v1.0/TravelPrices";
        HttpGet request = new HttpGet(url);
        CloseableHttpResponse response = httpClient.execute(request);
        HttpEntity entity = response.getEntity();
        String jsonData = EntityUtils.toString(entity);
        httpClient.close();
        return new JSONObject(jsonData);
    }
}

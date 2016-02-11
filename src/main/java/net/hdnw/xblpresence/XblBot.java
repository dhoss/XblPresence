package net.hdnw.xblpresence;

import org.jibble.pircbot.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.commons.io.IOUtils;
import org.json.*;

public class XblBot extends PircBot {

  private String watchlistFile;
  private static final String xboxApiBaseUrl = "https://xboxapi.com/v2/";
  private static final String presenceEndpoint = "/presence";
  private String apiKey;

  public XblBot(String name, String watchlist, String key) {
    this.setName(name);
    this.watchlistFile = watchlist;
    this.apiKey = key;
  }

  public List<String> watchlist() throws IOException {
    List<String> friends = new ArrayList<>();
    try (BufferedReader br = new BufferedReader(new FileReader(this.watchlistFile))) {
      String line;
      while ((line = br.readLine()) != null) {
        friends.add(line);
      }
    }
    return friends;
  }

  public String status(String friend) throws IOException {
    String presenceUrl = xboxApiBaseUrl + URLEncoder.encode(friend, "UTF-8") + presenceEndpoint;
    String status;
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.get().setUri(presenceUrl)
                               .setHeader("X-AUTH", apiKey).build();
    CloseableHttpResponse response = client.execute(request);
    try {

      System.out.println(response.getStatusLine());
      HttpEntity entity = response.getEntity();
      StringWriter writer = new StringWriter();
      IOUtils.copy(entity.getContent(), writer, "UTF-8");
      status = nameFromJson(writer.toString());
      System.out.println("STATUS " + friend + ", " + status);
      EntityUtils.consume(entity);
    } finally {
      response.close();
    }
    return status;
  }

  public HashMap friendStatuses() throws IOException {
    List<String> friends = watchlist();
    HashMap statuses = new HashMap();
    for (String friend : friends) {
      statuses.put(friend, status(friend));
    }
    return statuses;
  }

  private String nameFromJson(String json) {
    JSONObject obj = new JSONObject(json);
    return obj.getString("state");
  }
          
}

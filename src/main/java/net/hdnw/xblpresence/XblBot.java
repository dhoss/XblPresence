package net.hdnw.xblpresence;

import org.jibble.pircbot.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URLEncoder;
import java.util.*;

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

  public HashMap statusInfo(String friend) throws IOException {
    String presenceUrl = xboxApiBaseUrl + URLEncoder.encode(friend, "UTF-8") + presenceEndpoint;
    HashMap info;
    System.out.println("URL " + presenceUrl);
    System.out.println("API key " + apiKey);
    CloseableHttpClient client = HttpClients.custom().build();
    HttpUriRequest request = RequestBuilder.get().setUri(presenceUrl)
                               .setHeader("X-AUTH", apiKey).build();
    CloseableHttpResponse response = client.execute(request);
    try {

      System.out.println(response.getStatusLine());
      HttpEntity entity = response.getEntity();
      StringWriter writer = new StringWriter();
      IOUtils.copy(entity.getContent(), writer, "UTF-8");
      System.out.println("RAW JSON FOR " + friend + ": " + writer.toString());
      info = infoFromJson(writer.toString());
      EntityUtils.consume(entity);
    } finally {
      response.close();
    }
    return info;
  }

  public HashMap friendStatuses() throws IOException {
    List<String> friends = watchlist();
    HashMap statuses = new HashMap();
    for (String friend : friends) {
      statuses.put(friend, statusInfo(friend));
    }
    return statuses;
  }

  public void displayFriendStatuses(String channel) throws IOException {
    Iterator it = friendStatuses().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();
      String name = (String)pair.getKey();
      HashMap statusInfo = (HashMap)pair.getValue();
      String status = (String)statusInfo.get("status");
      String game = (String)statusInfo.get("playing");
      String color = Colors.TEAL;
      String black = Colors.BLACK;
      if (status.equals("Offline")) {
        color = Colors.MAGENTA;
      }
      if (game == null) {
        game = "nothing";
      }
      sendMessage(channel, name + " is " + color + status + black + " playing: " + game + "\n");
      it.remove(); // avoids a ConcurrentModificationException
    }
  }

  private HashMap infoFromJson(String json) {
    System.out.println("JSON " + json);
    HashMap info = new HashMap();
    JSONObject obj = new JSONObject(json);
    info.put("status", obj.getString("state"));
    try {
      info.put("playing", obj.getJSONObject("lastSeen").getString("titleName"));
    } catch (JSONException e) {
      System.out.println("lastSeen key not present: " + e.getMessage());
    }
    return info;
  }

  public void onMessage(String channel, String sender,
                        String login, String hostname, String message) {
    if (message.equalsIgnoreCase("mobbin")) {
      try {
        displayFriendStatuses(channel);
      } catch (IOException e) {
        System.out.println("ERROR " + e.getMessage());
        sendRawLine(channel + " " + sender + ": ERROR: " + e.getMessage());
      }
    }
  }
          
}

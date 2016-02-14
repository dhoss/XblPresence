package net.hdnw.xblpresence;

import org.jibble.pircbot.*;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/*import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;*/

import com.ning.http.client.*;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
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

  public Future<Hashtable> statusInfo(String friend) throws IOException {
  //HashMap statusInfo(String friend) throws IOException {
    String presenceUrl = xboxApiBaseUrl + URLEncoder.encode(friend, "UTF-8") + presenceEndpoint;
    HashMap info;
    System.out.println("URL " + presenceUrl);
    System.out.println("API key " + apiKey);
    RequestBuilder builder = new RequestBuilder("GET");
    Request request = builder.setUrl(presenceUrl)
                             .addHeader("X-AUTH", apiKey)
                             .build();
    AsyncHttpClient client = new AsyncHttpClient();
    Future<Hashtable> f = client.executeRequest(request, new AsyncCompletionHandler<Hashtable>(){

      @Override
      public Hashtable onCompleted(Response response) throws Exception{
        // Do something with the Response
        return infoFromJson(response.getResponseBody());
      }

      @Override
      public void onThrowable(Throwable t){
        // Something wrong happened.
      }
    });


    return f;
  }

  public Map friendStatuses() throws IOException, InterruptedException, ExecutionException {
    List<String> friends = watchlist();
    Map statuses = new HashMap();
    for (String friend : friends) {
      statuses.put(friend, statusInfo(friend));
    }
    return statuses;
  }

  public void displayFriendStatuses(String channel) throws IOException, InterruptedException, ExecutionException {
    Iterator it = friendStatuses().entrySet().iterator();
    while (it.hasNext()) {
      Map.Entry pair = (Map.Entry)it.next();
      String name = (String)pair.getKey();
      Future<Hashtable> statusFuture = (Future<Hashtable>)pair.getValue();
      Hashtable statusInfo = (Hashtable)statusFuture.get();
      String status = (String)statusInfo.get("status");
      String game = (String)statusInfo.get("playing");
      String color = Colors.GREEN;
      String black = Colors.TEAL;
      String magenta = Colors.MAGENTA;
      if (status.equals("Offline")) {
        color = Colors.RED;
      }
      if (game == null) {
        game = "nothing";
      }
      //PRIVMSG #mobwatchtest :
      sendRawLine("PRIVMSG " + channel + " :" + black + name + " is " + color + status + black + " playing: " + magenta + game + "\n");
      it.remove(); // avoids a ConcurrentModificationException
    }
  }

  private Hashtable infoFromJson(String json) {
    System.out.println("JSON " + json);
    Hashtable info = new Hashtable();
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
      } catch (ExecutionException e) {
        System.out.println("EXECUTION EXCEPTION " + e.getMessage());
      } catch (InterruptedException e) {
        System.out.println("INTERRUPTED EXCEPTION " + e.getMessage());
      }
    }
  }
          
}

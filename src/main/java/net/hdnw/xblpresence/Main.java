package net.hdnw.xblpresence;

/**
 * Created by devin on 2/10/16.
 */
import org.jibble.pircbot.*;

public class Main {

  public static void main(String[] args) throws Exception {

    XblBot bot = new XblBot(System.getProperty("botName"), System.getProperty("friendsList"), System.getProperty("apiKey"));

    // Enable debugging output.
    bot.setVerbose(true);

    bot.connect(System.getProperty("server"));

    bot.joinChannel(System.getProperty("channel"), System.getProperty("key"));

  }

}
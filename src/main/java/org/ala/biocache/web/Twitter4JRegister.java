package org.ala.biocache.web;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;

import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.http.AccessToken;
import twitter4j.http.RequestToken;

/**
 * Code taken from: http://www.ibm.com/developerworks/java/library/j-tweettask/index.html
 */
public class Twitter4JRegister {

  public static void main(String args[]) throws Exception {

    Twitter twitter = new TwitterFactory().getInstance();
    BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
    
    System.out.print("Enter the consumer key:");
    String consumerKey = bufferedReader.readLine();
    
    System.out.print("Enter the consumer secret key:");
    String consumerSecretKey = bufferedReader.readLine();
    
    twitter.setOAuthConsumer(consumerKey, consumerSecretKey);
    RequestToken requestToken = twitter.getOAuthRequestToken();
    AccessToken accessToken = null;
    
    
    while (null == accessToken) {
      System.out.println("Open the following URL and grant access to your account:");
      System.out.println(requestToken.getAuthorizationURL());
      System.out.print("Enter the generated PIN:");
      String pin = bufferedReader.readLine();
      try {
        if (pin.length() > 0) {
          accessToken = twitter.getOAuthAccessToken(requestToken, pin);
        } else {
          accessToken = twitter.getOAuthAccessToken();
        }

      } catch (TwitterException e) {
        if (401 == e.getStatusCode()) {
          System.out.println("Unable to get the access token.");
        } else {
          e.printStackTrace();
        }
      }
    }
    storeAccessToken(accessToken);
    Status status = twitter.updateStatus("Client installed2");
    System.out.println("Successfully updated the status to ["+ status.getText() + "].");
    System.exit(0);
  }

  private static void storeAccessToken(AccessToken accessToken) {

    try {
      FileOutputStream fileOutputStream = new FileOutputStream("token.txt");
      ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(accessToken.getToken());
      objectOutputStream.flush();
      fileOutputStream = new FileOutputStream("tokenSecret.txt");
      objectOutputStream = new ObjectOutputStream(fileOutputStream);
      objectOutputStream.writeObject(accessToken.getTokenSecret());

      objectOutputStream.flush();
    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

  }

}
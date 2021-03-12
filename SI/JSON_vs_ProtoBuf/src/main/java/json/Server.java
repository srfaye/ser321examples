package json;

import java.net.*;
import java.io.*;

import org.json.*;

// Server just receives a string, coverts it to a JSONObject, then prints from the JSONObject
public class Server {
  public static void main (String args[]) {
    try {
      if (args.length != 1) {
        System.out.println("Argument count doesn't match...");
        System.out.println("Usage: gradle runServer -Pport=<port number>");
        System.exit(0);
      }
      int port = -1;
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException ex) {
        System.out.println("Error parsing port number: Port should be an int");
        System.exit(0);
      }

      System.out.println(port);

      ServerSocket serv = new ServerSocket(port);
      Socket clientSock;

      while (true) {
        try {
          clientSock = serv.accept(); // accept a connection
          // we are using Object streams for easy sending of Strings
          ObjectOutputStream out = new ObjectOutputStream(clientSock.getOutputStream());
          ObjectInputStream in = new ObjectInputStream(clientSock.getInputStream());
          System.out.println("A client has connected!");

          while (true) {
            // receive messages
            String reqString = (String)in.readObject(); // we expect a string so we cast it
            JSONObject req = new JSONObject(reqString); // create a JSONObject using the string constructor
            System.out.println("Message type: " + req.getString("type")); // print the fields
            System.out.println("Message: " + req.getString("message"));

          }

        } catch (Exception ex) {
          System.out.println(ex);
        }
      }

    } catch (Exception ex) {
      System.out.println(ex);
    }

  }
}

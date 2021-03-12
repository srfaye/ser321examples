package json;

import java.net.*;
import java.io.*;

import org.json.*;

// Client creates a JSONObject and sends it to a server
public class Client {
  public static void main (String args[]) {
    try {
      if (args.length != 2) {
        System.out.println("Argument count doesn't match...");
        System.out.println("Usage: gradle runClient -Phost=<host ip> -Pport=<port number>");
        System.exit(0);
      }
      int port = -1;
      try {
        port = Integer.parseInt(args[1]);
      } catch (NumberFormatException ex) {
        System.out.println("Error parsing port number: Port should be an int");
        System.exit(0);
      }
      String host = args[0];

      Socket server = new Socket(host, port);
      System.out.println("Connected to server at " + host + ":" + port);
      // we are using Object streams for easy sending of Strings
      ObjectInputStream in = new ObjectInputStream(server.getInputStream());
      ObjectOutputStream out = new ObjectOutputStream(server.getOutputStream());
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));


      while(true) {
        JSONObject json = new JSONObject(); // create JSON object
        json.put("type", "hello"); // set key "type" with value "hello"
        json.put("message", "Hello server!");
        out.writeObject(json.toString()); // convert the json to a string and send via ObjectOutputStream
        // if we needed bytes instead, we could do json.toString().getBytes()

        // block from infinite looping
        stdin.readLine();

      }

    } catch (Exception ex) {
      System.out.println(ex);
    }

  }
}

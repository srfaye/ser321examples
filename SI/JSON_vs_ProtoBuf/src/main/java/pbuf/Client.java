package pbuf;

import java.net.*;
import java.io.*;

import org.json.*;
import buffers.MessageProtos.Request;

// Client creates a ProtoBuf message and sends it to the server, then blocks
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
      // We use InputStream and OutputStream for ProtoBuf communication
      InputStream in = server.getInputStream();
      OutputStream out = server.getOutputStream();
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));


      while(true) {
        Request.Builder reqBuilder = Request.newBuilder(); // create a Request.Builder
        reqBuilder.setType(Request.RequestType.HELLO_MESSAGE) // set fields of the Request
          .setMessage("Hello server!");
        Request req = reqBuilder.build(); // build the request and assign it to a Request object

        req.writeDelimitedTo(out); // use writeDelimitedTo to send the ProtoBuf via OutputStream

        // just block from looping over and over
        stdin.readLine();

      }

    } catch (Exception ex) {
      System.out.println(ex);
    }

  }
}

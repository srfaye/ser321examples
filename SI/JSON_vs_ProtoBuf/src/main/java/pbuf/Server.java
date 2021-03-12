package pbuf;

import java.net.*;
import java.io.*;

import org.json.*;
import buffers.MessageProtos.Request;

// Server just receives a connection, receives a ProtoBuf message, prints the ProtoBuf message
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
          clientSock = serv.accept();
          // We use InputStream and OutputStream for ProtoBuf communication
          InputStream in = clientSock.getInputStream();
          OutputStream out = clientSock.getOutputStream();
          System.out.println("A client has connected!");

          while (true) {
            // receive message
            Request req = Request.parseDelimitedFrom(in); // static method to parse the Request message
            Request.RequestType requestType = req.getType(); // get the enum type
            String message = req.getMessage(); // get the string message
            System.out.println("Type: " + requestType);
            System.out.println("Mesage: " + message);
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

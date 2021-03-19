package client;

import java.net.*;
import java.io.*;

import buffers.OperationProtos.Request;
import buffers.OperationProtos.Response;

class SockBaseClient {



  public static void main (String args[]) throws Exception {
    Socket serverSock = null;
    OutputStream out = null;
    InputStream in = null;
    int i1=0, i2=0;
    int port = 8007; // default port

    if (args.length != 3) {
      System.out.println("Expected arguments: <host(String)> <port(int)> <data(json file)>");
      System.exit(1);
    }
    String host = args[0];
    try {
      port = Integer.parseInt(args[1]);
    } catch (NumberFormatException nfe) {
      System.out.println("[Port] must be integer");
      System.exit(2);
    }
    String filename = args[2];

    try {
      // connect to the server
      serverSock = new Socket(host, port);

      // write to the server
      out = serverSock.getOutputStream();
      // read from the server
      in = serverSock.getInputStream();
      BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in));

      while (true) {

        System.out.println("Enter anything to send a message or 'quit' to exit...");
        String userInput = stdin.readLine();

        // quit the client program
        if (userInput.equalsIgnoreCase("quit")) {
          // close connection
          serverSock.close();
          break;
        }

        Request.Builder reqBuilder = Request.newBuilder();
        reqBuilder.setOperationType(Request.OperationType.RACE_CONDITION_TEST);
        reqBuilder.setStr(userInput);

        Request req = reqBuilder.build();

        req.writeDelimitedTo(out); // send off to server

        // Receive the response
        Response res = Response.parseDelimitedFrom(in);
        System.out.println("Result is: " + res.getStr());

      }

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}

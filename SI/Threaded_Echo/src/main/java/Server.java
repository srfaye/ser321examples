import java.net.*;
import java.io.*;

/**
* Server class to demonstrate a simple "echo" client-server connection using sockets.
* This version is multi-threaded.
* @version March 2021
*
*/
public class Server implements Runnable {
  public static String someString;
  public static int clientCount = 0;
  public Socket clientSock = null;
  public int clientId;

  public Server(Socket sock, int clientId) {
    this.clientSock = sock;
    this.clientId = clientId;
  }

  public void run() {
    try {
      int bufLen = 1024;
      byte clientInput[] = new byte[bufLen]; // up to 1024 bytes in a message.
      PrintWriter out = new PrintWriter(clientSock.getOutputStream(), true);
      InputStream input = clientSock.getInputStream();
      System.out.println("Server connected to client " + clientId);
      int numr = input.read(clientInput, 0, bufLen);
      while (numr != -1) {
        String received = new String(clientInput, 0, numr);
        System.out.println("read from client " + clientId + ": " + received);
        out.println(received);
        numr = input.read(clientInput, 0, bufLen);
      }
      input.close();
      clientSock.close();
      System.out.println("Client Socket Closed, id: " + clientId);
    } catch (Exception ex) {
      System.out.println(ex);
    }
  }

  public static void main (String args[]) {
    try {
      if (args.length != 1) {
        System.out.println("Usage: gradle runServer -Pport=9099");
        System.exit(0);
      }
      int port = -1;
      try {
        port = Integer.parseInt(args[0]);
      } catch (NumberFormatException nfe) {
        System.out.println("[Port] must be an integer");
        System.exit(2);
      }
      ServerSocket sock = new ServerSocket(port);
      System.out.println("Server ready for connections");

      while(true) {
        try {
          System.out.println("Server waiting for a connection");
          Socket client = sock.accept(); // blocking wait
          clientCount++;
          Server runnable = new Server(client, clientCount);
          Thread thread = new Thread(runnable);
          thread.start();

        } catch (Exception ex) {
          System.out.println(ex);
        }
      }
    } catch(Exception e) {
      e.printStackTrace();
    }
  }
}
